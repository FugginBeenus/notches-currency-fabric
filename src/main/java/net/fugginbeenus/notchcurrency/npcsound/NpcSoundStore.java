package net.fugginbeenus.notchcurrency.npcsound;

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

public final class NpcSoundStore {

    private NpcSoundStore() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcSounds");
    public static final int MAX_SOUNDS = 64;
    public static final int MAX_BYTES = 1024 * 1024;
    private static final Map<String, byte[]> BLOBS = new LinkedHashMap<>();
    private static final Map<String, String> HASHES = new LinkedHashMap<>();

    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9_]{1,48}");
    }

    public static String hash(byte[] blob) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] out = md.digest(blob);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", out[i]));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(java.util.Arrays.hashCode(blob));
        }
    }

    public static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("notchcurrency").resolve("npc_sounds");
    }

    public static void load(MinecraftServer server) {
        BLOBS.clear();
        HASHES.clear();
        Path root = dir(server);
        try {
            Files.createDirectories(root);
            try (Stream<Path> files = Files.list(root)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".ogg"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(file -> {
                            String name = file.getFileName().toString();
                            String id = name.substring(0, name.length() - 4);
                            if (!validId(id)) return;
                            if (BLOBS.size() >= MAX_SOUNDS) return;
                            try {
                                if (Files.size(file) > MAX_BYTES) {
                                    LOGGER.warn("Skipped server sound {}: larger than 1 MB", id);
                                    return;
                                }
                                byte[] blob = Files.readAllBytes(file);
                                BLOBS.put(id, blob);
                                HASHES.put(id, hash(blob));
                            } catch (Exception e) {
                                LOGGER.warn("Skipped server sound {}: {}", id, e.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            LOGGER.error("Could not read the server's NPC sounds", e);
        }
        if (!BLOBS.isEmpty()) LOGGER.info("Holding {} NPC sound(s) for players", BLOBS.size());
    }

    public static String store(MinecraftServer server, String id, byte[] blob) {
        if (!validId(id)) return "a sound name may only use lowercase letters, numbers and underscores";
        if (blob.length > MAX_BYTES) return "that sound is larger than 1 MB";
        if (!looksLikeOgg(blob)) return "that file is not an .ogg sound";
        if (!BLOBS.containsKey(id) && BLOBS.size() >= MAX_SOUNDS) {
            return "this server is already holding " + MAX_SOUNDS + " sounds";
        }
        try {
            Path root = dir(server);
            Files.createDirectories(root);
            Files.write(root.resolve(id + ".ogg"), blob);
        } catch (Exception e) {
            return "could not save it: " + e.getMessage();
        }
        BLOBS.put(id, blob);
        HASHES.put(id, hash(blob));
        return null;
    }

    public static boolean looksLikeOgg(byte[] blob) {
        return blob.length > 4 && blob[0] == 'O' && blob[1] == 'g' && blob[2] == 'g' && blob[3] == 'S';
    }

    public static String remove(MinecraftServer server, String id) {
        if (!validId(id)) return "that is not a sound name";
        if (!BLOBS.containsKey(id)) return "this server does not have a sound called " + id;
        BLOBS.remove(id);
        HASHES.remove(id);
        try {
            Files.deleteIfExists(dir(server).resolve(id + ".ogg"));
        } catch (Exception e) {
            return "could not remove the file: " + e.getMessage();
        }
        return null;
    }

    public static Map<String, String> hashes() {
        return new LinkedHashMap<>(HASHES);
    }

    public static byte[] blob(String id) {
        return BLOBS.get(id);
    }

    public static boolean has(String id) {
        return BLOBS.containsKey(id);
    }

    public static boolean isEmpty() {
        return BLOBS.isEmpty();
    }
}
