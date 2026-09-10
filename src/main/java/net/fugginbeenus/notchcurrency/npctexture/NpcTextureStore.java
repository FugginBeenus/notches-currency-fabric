package net.fugginbeenus.notchcurrency.npctexture;

import net.fugginbeenus.notchcurrency.registry.ModParticles;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class NpcTextureStore {

    private NpcTextureStore() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcTextures");
    public static final int SLOTS = ModParticles.CUSTOM_SLOTS;
    public static final int MAX_BYTES = 256 * 1024;

    public record Held(int slot, String name, byte[] bytes, String hash) {}

    private static final Map<Integer, Held> HELD = new LinkedHashMap<>();

    public static boolean validName(String name) {
        return name != null && name.matches("[a-z0-9_]{1,32}");
    }

    public static boolean validSlot(int slot) {
        return slot >= 1 && slot <= SLOTS;
    }

    public static boolean looksLikePng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    public static String hash(byte[] blob) {
        try {
            byte[] out = java.security.MessageDigest.getInstance("SHA-1").digest(blob);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", out[i]));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(blob));
        }
    }

    public static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("notchcurrency").resolve("npc_particles");
    }

    public static void load(MinecraftServer server) {
        HELD.clear();
        Path root = dir(server);
        try {
            Files.createDirectories(root);
            try (Stream<Path> files = Files.list(root)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".png"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .forEach(file -> {
                            String stem = file.getFileName().toString();
                            stem = stem.substring(0, stem.length() - 4);
                            int dash = stem.indexOf('-');
                            if (dash < 1) return;
                            int slot;
                            try {
                                slot = Integer.parseInt(stem.substring(0, dash));
                            } catch (NumberFormatException bad) {
                                return;
                            }
                            String name = stem.substring(dash + 1);
                            if (!validSlot(slot) || !validName(name)) return;
                            try {
                                if (Files.size(file) > MAX_BYTES) return;
                                byte[] bytes = Files.readAllBytes(file);
                                if (!looksLikePng(bytes)) return;
                                HELD.put(slot, new Held(slot, name, bytes, hash(bytes)));
                            } catch (Exception e) {
                                LOGGER.warn("Skipped particle texture {}: {}", stem, e.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            LOGGER.error("Could not read the server's particle textures", e);
        }
        if (!HELD.isEmpty()) LOGGER.info("Holding {} custom particle texture(s)", HELD.size());
    }

    public static String store(MinecraftServer server, int slot, String name, byte[] bytes) {
        if (!validSlot(slot)) return "there are only " + SLOTS + " slots";
        if (!validName(name)) return "a name may only use lowercase letters, numbers and underscores";
        if (bytes.length > MAX_BYTES) return "that picture is larger than 256 KB";
        if (!looksLikePng(bytes)) return "that file is not a PNG";
        try {
            Path root = dir(server);
            Files.createDirectories(root);
            Held old = HELD.get(slot);
            if (old != null) Files.deleteIfExists(root.resolve(slot + "-" + old.name() + ".png"));
            Files.write(root.resolve(slot + "-" + name + ".png"), bytes);
        } catch (Exception e) {
            return "could not save it: " + e.getMessage();
        }
        HELD.put(slot, new Held(slot, name, bytes, hash(bytes)));
        return null;
    }

    public static String remove(MinecraftServer server, int slot) {
        Held old = HELD.remove(slot);
        if (old == null) return "slot " + slot + " is already empty";
        try {
            Files.deleteIfExists(dir(server).resolve(slot + "-" + old.name() + ".png"));
        } catch (Exception e) {
            return "could not remove the file: " + e.getMessage();
        }
        return null;
    }

    public static Map<Integer, Held> all() {
        return new LinkedHashMap<>(HELD);
    }

    public static Held get(int slot) {
        return HELD.get(slot);
    }
}
