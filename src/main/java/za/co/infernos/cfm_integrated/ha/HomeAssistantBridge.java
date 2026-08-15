package za.co.infernos.cfm_integrated.ha;

import com.google.gson.JsonObject;
import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import za.co.infernos.cfm_integrated.CfmIntegrated;
import za.co.infernos.cfm_integrated.Config;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HomeAssistantBridge {
    private static final Map<String, Tracked> DEVICES = new ConcurrentHashMap<>();
    private static SimpleMqtt mqtt;
    private static MinecraftServer server;
    private static int ticks;
    private static long lastFailLog;

    private HomeAssistantBridge() {}

    private record Tracked(ResourceKey<Level> dim, BlockPos pos, String name, boolean enabled) {}

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event) {
        server = event.getServer();
        if (Config.HA_ENABLED.get()) {
            connect();
        }
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        DEVICES.clear();
        server = null;
        if (mqtt != null) {
            mqtt.close();
            mqtt = null;
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || !(chunk.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof IHomeControlDevice device) {
                DEVICES.put(key(level, device.getDevicePos()), new Tracked(
                        level.dimension(),
                        device.getDevicePos().immutable(),
                        device.getDeviceName().getString(),
                        device.isDeviceEnabled()
                ));
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || !(chunk.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkPos cp = chunk.getPos();
        DEVICES.keySet().removeIf(id -> {
            Tracked t = DEVICES.get(id);
            return t != null && t.dim().equals(level.dimension()) && new ChunkPos(t.pos()).equals(cp);
        });
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!Config.HA_ENABLED.get() || server == null) {
            return;
        }
        ticks++;
        if (mqtt == null || !mqtt.isConnected()) {
            if (ticks % 200 == 0) {
                connect();
            }
            return;
        }
        mqtt.pingIfNeeded();
        if (ticks % Math.max(10, Config.HA_PUBLISH_INTERVAL.get()) != 0) {
            return;
        }
        publishStates();
    }

    private static void connect() {
        try {
            SimpleMqtt client = new SimpleMqtt(
                    Config.HA_HOST.get(),
                    Config.HA_PORT.get(),
                    Config.HA_USERNAME.get(),
                    Config.HA_PASSWORD.get(),
                    "cfm-integrated-" + Long.toHexString(System.nanoTime())
            );
            client.connect();
            String cmdFilter = Config.HA_TOPIC_PREFIX.get() + "/+/+/set";
            client.on(Config.HA_TOPIC_PREFIX.get() + "/#", HomeAssistantBridge::onCommand);
            client.subscribe(cmdFilter);
            mqtt = client;
            publishDiscovery();
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now - lastFailLog > 30_000) {
                CfmIntegrated.LOGGER.warn("Home Assistant MQTT connect failed: {}", e.getMessage());
                lastFailLog = now;
            }
        }
    }

    private static void publishDiscovery() {
        if (mqtt == null) {
            return;
        }
        for (Map.Entry<String, Tracked> e : DEVICES.entrySet()) {
            try {
                publishDiscovery(e.getKey(), e.getValue());
            } catch (IOException ignored) {
            }
        }
    }

    private static void publishDiscovery(String id, Tracked tracked) throws IOException {
        String prefix = Config.HA_DISCOVERY_PREFIX.get();
        String topicPrefix = Config.HA_TOPIC_PREFIX.get();
        JsonObject json = new JsonObject();
        json.addProperty("name", tracked.name());
        json.addProperty("unique_id", "cfm_" + id);
        json.addProperty("state_topic", topicPrefix + "/" + id + "/state");
        json.addProperty("command_topic", topicPrefix + "/" + id + "/set");
        json.addProperty("payload_on", "ON");
        json.addProperty("payload_off", "OFF");
        json.addProperty("device_class", "switch");
        JsonObject device = new JsonObject();
        device.addProperty("identifiers", "cfm_integrated");
        device.addProperty("name", "MrCrayfish Furniture Integrated");
        device.addProperty("manufacturer", "Infernos");
        json.add("device", device);
        mqtt.publish(prefix + "/switch/" + id + "/config", json.toString(), true);
    }

    private static void publishStates() {
        if (server == null || mqtt == null) {
            return;
        }
        for (Map.Entry<String, Tracked> e : DEVICES.entrySet()) {
            Tracked tracked = e.getValue();
            ServerLevel level = server.getLevel(tracked.dim());
            if (level == null) {
                continue;
            }
            if (!(level.getBlockEntity(tracked.pos()) instanceof IHomeControlDevice device)) {
                continue;
            }
            boolean on = device.isDeviceEnabled();
            DEVICES.put(e.getKey(), new Tracked(tracked.dim(), tracked.pos(), device.getDeviceName().getString(), on));
            try {
                mqtt.publish(Config.HA_TOPIC_PREFIX.get() + "/" + e.getKey() + "/state", on ? "ON" : "OFF", true);
            } catch (IOException ignored) {
            }
        }
    }

    private static void onCommand(String topic, String payload) {
        if (server == null || !topic.endsWith("/set")) {
            return;
        }
        String prefix = Config.HA_TOPIC_PREFIX.get() + "/";
        if (!topic.startsWith(prefix)) {
            return;
        }
        String id = topic.substring(prefix.length(), topic.length() - 4);
        Tracked tracked = DEVICES.get(id);
        if (tracked == null) {
            return;
        }
        boolean enable = "ON".equalsIgnoreCase(payload.trim()) || "true".equalsIgnoreCase(payload.trim());
        server.execute(() -> {
            ServerLevel level = server.getLevel(tracked.dim());
            if (level != null && level.getBlockEntity(tracked.pos()) instanceof IHomeControlDevice device) {
                device.setDeviceState(enable);
            }
        });
    }

    private static String key(ServerLevel level, BlockPos pos) {
        String dim = level.dimension().location().getPath();
        return (dim + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ()).toLowerCase(Locale.ROOT);
    }
}
