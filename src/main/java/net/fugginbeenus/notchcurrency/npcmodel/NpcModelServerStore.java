package net.fugginbeenus.notchcurrency.npcmodel;

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

public final class NpcModelServerStore {

    private NpcModelServerStore() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcModels");
    public static final int MAX_MODELS = 64;
    private static final Map<String, byte[]> BLOBS = new LinkedHashMap<>();
    private static final Map<String, String> HASHES = new LinkedHashMap<>();

    public static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("notchcurrency").resolve("npc_models");
    }

    public static void load(MinecraftServer server) {
        BLOBS.clear();
        HASHES.clear();
        Path root = dir(server);
        try {
            Files.createDirectories(root);
            try (Stream<Path> folders = Files.list(root)) {
                folders.filter(Files::isDirectory)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(folder -> {
                            String id = folder.getFileName().toString();
                            if (!NpcModelBlob.validId(id)) return;
                            if (BLOBS.size() >= MAX_MODELS) return;
                            try {
                                byte[] blob = NpcModelBlob.pack(folder);
                                BLOBS.put(id, blob);
                                HASHES.put(id, NpcModelBlob.hash(blob));
                            } catch (Exception e) {
                                LOGGER.warn("Skipped server NPC model {}: {}", id, e.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            LOGGER.error("Could not read the server's NPC models", e);
        }
        if (!BLOBS.isEmpty()) LOGGER.info("Holding {} NPC model(s) for players", BLOBS.size());
    }

    public static String store(MinecraftServer server, String id, byte[] blob) {
        if (!NpcModelBlob.validId(id)) return "that is not a model id";
        if (!BLOBS.containsKey(id) && BLOBS.size() >= MAX_MODELS) {
            return "this server is already holding " + MAX_MODELS + " models";
        }
        String problem = NpcModelBlob.unpack(blob, dir(server), id);
        if (problem != null) return problem;
        BLOBS.put(id, blob);
        HASHES.put(id, NpcModelBlob.hash(blob));
        return null;
    }

    public static String remove(MinecraftServer server, String id) {
        if (!NpcModelBlob.validId(id)) return "that is not a model id";
        if (!BLOBS.containsKey(id)) return "this server does not have a model called " + id;
        BLOBS.remove(id);
        HASHES.remove(id);
        try {
            Path folder = dir(server).resolve(id);
            try (Stream<Path> walk = Files.walk(folder)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
            }
        } catch (Exception e) {
            return "could not remove the files: " + e.getMessage();
        }
        return null;
    }

    public static Map<String, String> hashes() {
        return new LinkedHashMap<>(HASHES);
    }
    public static byte[] blob(String id) {
        return BLOBS.get(id);
    }
    public static boolean isEmpty() {
        return BLOBS.isEmpty();
    }
}
