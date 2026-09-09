package net.fugginbeenus.notchcurrency.client.npcsound;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class NpcSoundLoader {

    private NpcSoundLoader() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcSounds");
    public static final long MAX_FILE_BYTES = 1024L * 1024;
    private static final Map<String, Path> FOUND = new LinkedHashMap<>();

    public static Path soundsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("notchcurrency").resolve("npc_sounds");
    }

    public static Path importDir() {
        return soundsDir().resolve("_import");
    }

    public static List<String> names() {
        return new ArrayList<>(FOUND.keySet());
    }

    public static int count() {
        return FOUND.size();
    }

    public static boolean has(String id) {
        return FOUND.containsKey(id);
    }

    public static Path fileOf(String id) {
        return FOUND.get(id);
    }

    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9_]{1,48}");
    }

    public static void scan() {
        FOUND.clear();
        try {
            Files.createDirectories(importDir());
            writeReadme();
            try (Stream<Path> files = Files.list(soundsDir())) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".ogg"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(file -> {
                            String name = file.getFileName().toString();
                            String id = name.substring(0, name.length() - 4);
                            if (!validId(id)) return;
                            try {
                                if (Files.size(file) > MAX_FILE_BYTES) return;
                            } catch (Exception unreadable) {
                                return;
                            }
                            FOUND.put(id, file);
                        });
            }
        } catch (Exception e) {
            LOGGER.error("Could not read the sounds folder", e);
        }
    }

    public static List<Path> imported() {
        List<Path> out = new ArrayList<>();
        try {
            Files.createDirectories(importDir());
            try (Stream<Path> files = Files.list(importDir())) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".ogg"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(out::add);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static String adopt(Path file, String id) {
        if (!validId(id)) return "a sound name may only use lowercase letters, numbers and underscores";
        try {
            if (Files.size(file) > MAX_FILE_BYTES) return "that file is larger than 1 MB";
            byte[] head = new byte[4];
            try (var in = Files.newInputStream(file)) {
                if (in.read(head) != 4 || head[0] != 'O' || head[1] != 'g'
                        || head[2] != 'g' || head[3] != 'S') {
                    return "that file is not an .ogg sound";
                }
            }
            Files.createDirectories(soundsDir());
            Files.copy(file, soundsDir().resolve(id + ".ogg"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(file);
            scan();
            return null;
        } catch (Exception e) {
            return "could not read it: " + e.getMessage();
        }
    }

    public static String save(String id, byte[] blob) {
        if (!validId(id)) return "that is not a sound name";
        try {
            Files.createDirectories(soundsDir());
            Files.write(soundsDir().resolve(id + ".ogg"), blob);
            scan();
            return null;
        } catch (Exception e) {
            return "could not save it: " + e.getMessage();
        }
    }

    public static String delete(String id) {
        if (!validId(id)) return "that is not a sound name";
        try {
            Files.deleteIfExists(soundsDir().resolve(id + ".ogg"));
            scan();
            return null;
        } catch (Exception e) {
            return "could not remove it: " + e.getMessage();
        }
    }

    public static void writeInto(Path assets) throws Exception {
        if (FOUND.isEmpty()) return;
        Path dir = assets.resolve("sounds");
        Files.createDirectories(dir);
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, Path> sound : FOUND.entrySet()) {
            Files.copy(sound.getValue(), dir.resolve(sound.getKey() + ".ogg"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (!first) json.append(",\n");
            first = false;
            json.append("  \"").append(sound.getKey()).append("\": {\n")
                    .append("    \"category\": \"neutral\",\n")
                    .append("    \"sounds\": [\"notchcurrency:").append(sound.getKey()).append("\"]\n")
                    .append("  }");
        }
        json.append("\n}\n");
        Files.writeString(assets.resolve("sounds.json"), json.toString(), StandardCharsets.UTF_8);
    }

    public static String stampPart() {
        StringBuilder sig = new StringBuilder();
        for (Map.Entry<String, Path> sound : FOUND.entrySet()) {
            try {
                sig.append(sound.getKey()).append(':')
                        .append(Files.size(sound.getValue())).append(':')
                        .append(Files.getLastModifiedTime(sound.getValue()).toMillis()).append('\n');
            } catch (Exception unreadable) {
                sig.append(sound.getKey()).append(":?\n");
            }
        }
        return sig.toString();
    }

    private static void writeReadme() throws Exception {
        Path readme = importDir().resolve("README.txt");
        if (Files.isRegularFile(readme)) return;
        Files.writeString(readme, String.join("\n",
                "Drop .ogg sound files in this folder.",
                "",
                "Then open an NPC, go to Talk, press Sounds, then Custom.",
                "Give each file a short name using lowercase letters, numbers and underscores.",
                "",
                "A sound then plays as notchcurrency:<name>, for example notchcurrency:hammer.",
                "Each file must be an .ogg and under 1 MB.",
                "",
                "On a server an operator uploads them once and every player gets them on join.",
                ""), StandardCharsets.UTF_8);
    }
}
