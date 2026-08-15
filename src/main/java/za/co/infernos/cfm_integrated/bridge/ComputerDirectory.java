package za.co.infernos.cfm_integrated.bridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
import za.co.infernos.cfm_integrated.CfmIntegrated;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Stable pairing codes + positions for furniture computers.
 * Codes are random (not player UUID/name). Website claim uses these.
 */
public final class ComputerDirectory extends SavedData {
    private static final String DATA_NAME = CfmIntegrated.MOD_ID + "_computers";
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

    public record Entry(String key, String dimension, int x, int y, int z, String pairingCode) {}

    private final Map<String, Entry> entries = new HashMap<>();
    private static @Nullable ComputerDirectory INSTANCE;

    public static void attach(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        INSTANCE = storage.computeIfAbsent(new Factory<>(ComputerDirectory::new, ComputerDirectory::load), DATA_NAME);
    }

    public static ComputerDirectory get() {
        if (INSTANCE == null) {
            INSTANCE = new ComputerDirectory();
        }
        return INSTANCE;
    }

    public static void detach() {
        INSTANCE = null;
    }

    public static String keyOf(ResourceKey<Level> dim, BlockPos pos) {
        ResourceLocation loc = dim.location();
        return loc.getNamespace() + ":" + loc.getPath() + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    public Entry discover(ServerLevel level, BlockPos pos) {
        String key = keyOf(level.dimension(), pos);
        Entry existing = entries.get(key);
        if (existing != null) {
            return existing;
        }
        String code = newCode();
        Entry created = new Entry(
                key,
                level.dimension().location().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                code
        );
        entries.put(key, created);
        setDirty();
        return created;
    }

    public Collection<Entry> all() {
        return List.copyOf(entries.values());
    }

    public static String formatDisplay(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("-", "").toUpperCase(Locale.ROOT);
        if (s.length() == 8) {
            return s.substring(0, 4) + "-" + s.substring(4);
        }
        return s;
    }

    private String newCode() {
        Set<String> used = new HashSet<>();
        for (Entry e : entries.values()) {
            used.add(e.pairingCode());
        }
        String code;
        int guard = 0;
        do {
            char[] out = new char[8];
            for (int i = 0; i < out.length; i++) {
                out[i] = ALPHABET[RNG.nextInt(ALPHABET.length)];
            }
            code = new String(out);
            guard++;
        } while (used.contains(code) && guard < 64);
        return code;
    }

    public ComputerDirectory() {}

    public static ComputerDirectory load(CompoundTag tag, HolderLookup.Provider provider) {
        ComputerDirectory data = new ComputerDirectory();
        ListTag list = tag.getList("computers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            String key = row.getString("k");
            if (key.isBlank()) {
                continue;
            }
            data.entries.put(key, new Entry(
                    key,
                    row.getString("d"),
                    row.getInt("x"),
                    row.getInt("y"),
                    row.getInt("z"),
                    row.getString("c")
            ));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag row = new CompoundTag();
            row.putString("k", e.key());
            row.putString("d", e.dimension());
            row.putInt("x", e.x());
            row.putInt("y", e.y());
            row.putInt("z", e.z());
            row.putString("c", e.pairingCode());
            list.add(row);
        }
        tag.put("computers", list);
        return tag;
    }

    public List<Entry> snapshot() {
        return new ArrayList<>(entries.values());
    }
}
