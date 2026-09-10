package net.fugginbeenus.notchcurrency.client.npctexture;

import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.npctexture.NpcTextureStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class NpcTextureLoader {

    private NpcTextureLoader() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcTextures");
    public static final int SLOTS = NpcTextureStore.SLOTS;

    public record Local(int slot, String name, Path file) {}

    private static final Map<Integer, Local> FOUND = new TreeMap<>();

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("notchcurrency").resolve("npc_particles");
    }

    public static Path importDir() {
        return dir().resolve("_import");
    }

    public static Local slot(int slot) { return FOUND.get(slot); }

    public static int count() { return FOUND.size(); }

    public static String nameOf(int slot) {
        Local l = FOUND.get(slot);
        return l == null ? null : l.name();
    }

    public static int freeSlot() {
        for (int i = 1; i <= SLOTS; i++) if (!FOUND.containsKey(i)) return i;
        return -1;
    }

    public static void scan() {
        FOUND.clear();
        net.fugginbeenus.notchcurrency.client.particle.ParticleThumbs.forget();
        try {
            Files.createDirectories(importDir());
            writeReadme();
            try (Stream<Path> files = Files.list(dir())) {
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
                            if (!NpcTextureStore.validSlot(slot) || !NpcTextureStore.validName(name)) return;
                            FOUND.put(slot, new Local(slot, name, file));
                        });
            }
        } catch (Exception e) {
            LOGGER.error("Could not read the particle textures folder", e);
        }
    }

    public static List<Path> imported() {
        List<Path> out = new ArrayList<>();
        try {
            Files.createDirectories(importDir());
            try (Stream<Path> files = Files.list(importDir())) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .forEach(out::add);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static String problemWith(Path file) {
        try {
            if (Files.size(file) > NpcTextureStore.MAX_BYTES) return "that picture is larger than 256 KB";
            try (var in = Files.newInputStream(file);
                 com.mojang.blaze3d.platform.NativeImage img = com.mojang.blaze3d.platform.NativeImage.read(in)) {
                int w = img.getWidth(), h = img.getHeight();
                if (w < 2 || w > 64) return "the picture should be between 2 and 64 pixels wide";
                if (h % w != 0) return "for an animation, the height must be a multiple of the width";
                if (h / w > 64) return "that is too many frames";
            }
            return null;
        } catch (Exception e) {
            return "that file could not be read as a PNG";
        }
    }

    public static String adopt(Path file, int slot, String name) {
        if (!NpcTextureStore.validSlot(slot)) return "there are only " + SLOTS + " slots";
        if (!NpcTextureStore.validName(name)) return "a name may only use lowercase letters, numbers and underscores";
        String problem = problemWith(file);
        if (problem != null) return problem;
        try {
            Files.createDirectories(dir());
            Local old = FOUND.get(slot);
            if (old != null) Files.deleteIfExists(old.file());
            Files.copy(file, dir().resolve(slot + "-" + name + ".png"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(file);
            scan();
            return null;
        } catch (Exception e) {
            return "could not copy it: " + e.getMessage();
        }
    }

    public static String save(int slot, String name, byte[] bytes) {
        if (!NpcTextureStore.validSlot(slot) || !NpcTextureStore.validName(name)) return "bad slot or name";
        try {
            Files.createDirectories(dir());
            Local old = FOUND.get(slot);
            if (old != null) Files.deleteIfExists(old.file());
            Files.write(dir().resolve(slot + "-" + name + ".png"), bytes);
            scan();
            return null;
        } catch (Exception e) {
            return "could not save it: " + e.getMessage();
        }
    }

    public static String delete(int slot) {
        Local old = FOUND.get(slot);
        if (old == null) return "that slot is already empty";
        try {
            Files.deleteIfExists(old.file());
            scan();
            return null;
        } catch (Exception e) {
            return "could not remove it: " + e.getMessage();
        }
    }

    public static void writeInto(Path assets) throws Exception {
        if (FOUND.isEmpty()) return;
        Path dir = assets.resolve("textures").resolve("particle");
        Files.createDirectories(dir);
        for (Local l : FOUND.values()) {
            Files.copy(l.file(), dir.resolve("custom_" + l.slot() + ".png"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            int frames = frameCount(l.file());
            Path meta = dir.resolve("custom_" + l.slot() + ".png.mcmeta");
            if (frames > 1) {
                Files.writeString(meta, "{\"animation\":{\"frametime\":2}}\n", StandardCharsets.UTF_8);
            } else {
                Files.deleteIfExists(meta);
            }
        }
    }

    private static int frameCount(Path file) {
        try (var in = Files.newInputStream(file);
             com.mojang.blaze3d.platform.NativeImage img = com.mojang.blaze3d.platform.NativeImage.read(in)) {
            return Math.max(1, img.getHeight() / Math.max(1, img.getWidth()));
        } catch (Exception e) {
            return 1;
        }
    }

    public static String stampPart() {
        StringBuilder sig = new StringBuilder();
        for (Local l : FOUND.values()) {
            try {
                sig.append(l.slot()).append('-').append(l.name()).append(':')
                        .append(Files.size(l.file())).append(':')
                        .append(Files.getLastModifiedTime(l.file()).toMillis()).append('\n');
            } catch (Exception unreadable) {
                sig.append(l.slot()).append(":?\n");
            }
        }
        return sig.toString();
    }

    private static void writeReadme() throws Exception {
        Path readme = importDir().resolve("README.txt");
        if (Files.isRegularFile(readme)) return;
        Files.writeString(readme, String.join("\n",
                "Drop PNG pictures in this folder to use them as particles.",
                "",
                "Then open an NPC, Manage, Particles, Textures. Name each one and pick a slot.",
                "",
                "A picture should be square, between 2 and 64 pixels wide. 8 or 16 is normal.",
                "For an animation, stack the frames top to bottom in one tall picture.",
                "",
                "It then shows up in the particle picker under Notch, by the name you gave it.",
                "There are 8 slots. On a server an operator uploads once and every player gets them on join.",
                ""), StandardCharsets.UTF_8);
    }
}
