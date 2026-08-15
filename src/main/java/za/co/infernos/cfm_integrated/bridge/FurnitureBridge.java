package za.co.infernos.cfm_integrated.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mrcrayfish.furniture.refurbished.blockentity.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import za.co.infernos.cfm_integrated.CfmIntegrated;
import za.co.infernos.cfm_integrated.Config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Furniture computers heartbeat onto the existing MekAcc server (same token).
 * Player clients do not need to be online — only the dedicated server.
 */
public final class FurnitureBridge {
    private static final TicketType<ChunkPos> COMPUTER_TICKET = TicketType.create(
            "cfm_integrated_computer",
            Comparator.comparingLong(ChunkPos::toLong)
    );

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cfm-integrated-bridge");
        t.setDaemon(true);
        return t;
    });

    private static @Nullable MinecraftServer SERVER;
    private static int tick;
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean(false);

    private FurnitureBridge() {}

    public static boolean isEnabled() {
        return Config.BRIDGE_ENABLED.get()
                && Config.BRIDGE_URL.get() != null
                && !Config.BRIDGE_URL.get().isBlank()
                && Config.BRIDGE_TOKEN.get() != null
                && !Config.BRIDGE_TOKEN.get().isBlank();
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        SERVER = event.getServer();
        tick = 0;
        ComputerDirectory.attach(SERVER);
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        SERVER = null;
        ComputerDirectory.detach();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || !(chunk.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ComputerBlockEntity computer) {
                ComputerDirectory.get().discover(level, computer.getBlockPos());
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!isEnabled() || SERVER == null) {
            return;
        }
        tick++;
        int period = Math.max(15, Config.BRIDGE_HEARTBEAT_SECONDS.get()) * 20;
        if (tick % period == 0) {
            heartbeatAsync();
        }
        if (tick % 100 == 0) {
            pollCommandsAsync();
        }
    }

    public static void heartbeatAsync() {
        if (!isEnabled() || !IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }
        final String body;
        try {
            body = buildHeartbeatJson();
        } catch (Throwable t) {
            IN_FLIGHT.set(false);
            CfmIntegrated.LOGGER.warn("Furniture heartbeat build failed: {}", t.toString());
            return;
        }
        final String base = trimSlash(Config.BRIDGE_URL.get());
        final String token = Config.BRIDGE_TOKEN.get().trim();
        EXEC.execute(() -> {
            try {
                postJson(base + "/api/v1/mekacc/furniture/heartbeat", body, token);
                pollCommandsSync(base, token);
            } catch (Exception e) {
                CfmIntegrated.LOGGER.warn("Furniture bridge failed: {}", e.toString());
            } finally {
                IN_FLIGHT.set(false);
            }
        });
    }

    public static void pollCommandsAsync() {
        if (!isEnabled() || SERVER == null || !IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }
        final String base = trimSlash(Config.BRIDGE_URL.get());
        final String token = Config.BRIDGE_TOKEN.get().trim();
        EXEC.execute(() -> {
            try {
                pollCommandsSync(base, token);
            } catch (Exception e) {
                CfmIntegrated.LOGGER.debug("Furniture command poll failed: {}", e.toString());
            } finally {
                IN_FLIGHT.set(false);
            }
        });
    }

    private static String buildHeartbeatJson() {
        JsonObject root = new JsonObject();
        root.addProperty("mod", CfmIntegrated.MOD_ID);
        root.addProperty("modVersion", "1.0.2");
        root.addProperty("ts", System.currentTimeMillis());
        if (SERVER != null) {
            root.addProperty("motd", SERVER.getMotd());
            root.addProperty("players", SERVER.getPlayerCount());
            root.addProperty("maxPlayers", SERVER.getMaxPlayers());
        }
        JsonArray computers = new JsonArray();
        if (SERVER != null) {
            for (ComputerDirectory.Entry entry : ComputerDirectory.get().snapshot()) {
                JsonObject row = new JsonObject();
                row.addProperty("computerKey", entry.key());
                row.addProperty("pairingCode", entry.pairingCode());
                row.addProperty("pairingCodeDisplay", ComputerDirectory.formatDisplay(entry.pairingCode()));
                row.addProperty("dimension", entry.dimension());
                row.addProperty("pos", entry.x() + " " + entry.y() + " " + entry.z());
                ServerLevel level = resolveLevel(entry.dimension());
                boolean loaded = false;
                if (level != null) {
                    BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
                    ensureChunk(level, pos);
                    if (level.getBlockEntity(pos) instanceof ComputerBlockEntity computer) {
                        loaded = true;
                        row.add("devices", FurnitureDevices.snapshot(computer));
                    }
                }
                row.addProperty("loaded", loaded);
                computers.add(row);
            }
        }
        root.add("computers", computers);
        return GSON.toJson(root);
    }

    private static void pollCommandsSync(String base, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/api/v1/mekacc/furniture/commands"))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "cfm_integrated/1.0.2")
                .GET();
        if (!token.isEmpty()) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            return;
        }
        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray cmds = root.has("commands") && root.get("commands").isJsonArray()
                ? root.getAsJsonArray("commands")
                : new JsonArray();
        List<JsonObject> toRun = new ArrayList<>();
        for (JsonElement el : cmds) {
            if (el.isJsonObject()) {
                toRun.add(el.getAsJsonObject());
            }
        }
        if (toRun.isEmpty() || SERVER == null) {
            return;
        }
        SERVER.execute(() -> {
            for (JsonObject cmd : toRun) {
                String id = str(cmd, "id");
                try {
                    boolean ok = runCommand(cmd);
                    ack(base, token, id, ok, ok ? "ok" : "failed");
                } catch (Exception e) {
                    ack(base, token, id, false, e.getMessage());
                }
            }
        });
    }

    private static boolean runCommand(JsonObject cmd) {
        JsonObject payload = cmd.has("payload") && cmd.get("payload").isJsonObject()
                ? cmd.getAsJsonObject("payload")
                : new JsonObject();
        String key = str(payload, "computerKey");
        String posRaw = str(payload, "devicePos");
        if (key.isBlank() || posRaw.isBlank() || SERVER == null) {
            return false;
        }
        boolean enabled = payload.has("enabled") && payload.get("enabled").getAsBoolean();
        ComputerDirectory.Entry match = null;
        for (ComputerDirectory.Entry e : ComputerDirectory.get().snapshot()) {
            if (e.key().equals(key)) {
                match = e;
                break;
            }
        }
        if (match == null) {
            return false;
        }
        ServerLevel level = resolveLevel(match.dimension());
        if (level == null) {
            return false;
        }
        BlockPos computerPos = new BlockPos(match.x(), match.y(), match.z());
        ensureChunk(level, computerPos);
        if (!(level.getBlockEntity(computerPos) instanceof ComputerBlockEntity computer)) {
            return false;
        }
        String[] parts = posRaw.trim().split("\\s+");
        if (parts.length != 3) {
            return false;
        }
        BlockPos devicePos = new BlockPos(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        );
        return FurnitureDevices.set(computer, devicePos, enabled);
    }

    private static void ack(String base, String token, String id, boolean ok, String result) {
        if (id.isBlank()) {
            return;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("ok", ok);
            body.addProperty("result", result == null ? "" : result);
            postJson(base + "/api/v1/mekacc/commands/" + id + "/ack", GSON.toJson(body), token);
        } catch (Exception ignored) {
        }
    }

    private static void postJson(String url, String body, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .header("User-Agent", "cfm_integrated/1.0.2")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + resp.statusCode());
        }
    }

    private static @Nullable ServerLevel resolveLevel(String dim) {
        if (SERVER == null || dim == null || dim.isBlank()) {
            return null;
        }
        ResourceLocation loc = ResourceLocation.tryParse(dim);
        if (loc == null) {
            return null;
        }
        return SERVER.getLevel(ResourceKey.create(Registries.DIMENSION, loc));
    }

    private static void ensureChunk(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        level.getChunkSource().addRegionTicket(COMPUTER_TICKET, chunk, 2, chunk);
        level.getChunk(chunk.x, chunk.z);
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String s = url.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }
}
