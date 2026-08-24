package net.fugginbeenus.notchcurrency.client.npcmodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelBundle;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class NpcModelLoader {

    private NpcModelLoader() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcModels");

    public static final String PACK_DIR_NAME = "NotchCurrencyModels";
    public static final String PACK_PROFILE_NAME = "file/" + PACK_DIR_NAME;
    private static final long MAX_FILE_BYTES = 4L * 1024 * 1024;
    private static final List<String> PROBLEMS = new ArrayList<>();

    public static Path modelsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("notchcurrency").resolve("npc_models");
    }

    public static Path importDir() {
        return modelsDir().resolve("_import");
    }

    private static Path packDir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(PACK_DIR_NAME);
    }

    public static List<String> problems() {
        return List.copyOf(PROBLEMS);
    }
    public static boolean loadAll() {
        PROBLEMS.clear();
        List<NpcModelBundle> found = new ArrayList<>();
        try {
            Files.createDirectories(importDir());
            writeReadme();

            List<Path> folders = new ArrayList<>();
            try (Stream<Path> entries = Files.list(modelsDir())) {
                entries.filter(Files::isDirectory)
                        .filter(dir -> !dir.getFileName().toString().startsWith("_"))
                        .sorted(Comparator.comparing(dir -> dir.getFileName().toString()))
                        .forEach(folders::add);
            }

            String stamp = stampOf(folders);
            if (stamp.equals(writtenStamp())) {
                for (Path folder : folders) {
                    NpcModelBundle bundle = readOnly(folder);
                    if (bundle != null) found.add(bundle);
                }
                NpcModelRegistry.replaceAll(found);
                return false;
            }

            boolean hadPack = Files.isDirectory(packDir());
            deleteRecursively(packDir());
            if (folders.isEmpty()) {
                NpcModelRegistry.replaceAll(List.of());
                return hadPack;
            }

            writePackMeta();
            for (Path folder : folders) {
                NpcModelBundle bundle = readAndWrite(folder);
                if (bundle != null) found.add(bundle);
            }
            Files.writeString(packDir().resolve(".stamp"), stamp);
        } catch (Exception e) {
            LOGGER.error("Could not load custom NPC models", e);
            PROBLEMS.add("Could not read the models folder: " + e.getMessage());
        }

        NpcModelRegistry.replaceAll(found);
        LOGGER.info("Loaded {} custom NPC model(s), {} skipped", found.size(), PROBLEMS.size());
        return true;
    }

    public static String delete(String id) {
        if (id == null || !id.matches("[a-z0-9_]+")) return "that is not a model id";
        Path folder = modelsDir().resolve(id);
        if (!Files.isDirectory(folder)) return "there is no model called " + id;
        try {
            // Belt and braces against a caller ever handing this something clever.
            if (!folder.toAbsolutePath().normalize().startsWith(modelsDir().toAbsolutePath().normalize())) {
                return "that is not inside the models folder";
            }
            deleteRecursively(folder);
            return Files.isDirectory(folder) ? "some files could not be removed" : null;
        } catch (Exception e) {
            return "could not remove it: " + e.getMessage();
        }
    }

    private static String stampOf(List<Path> folders) {
        StringBuilder sig = new StringBuilder();
        for (Path folder : folders) {
            try (Stream<Path> files = Files.walk(folder)) {
                files.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(file -> {
                            try {
                                sig.append(file).append(':')
                                        .append(Files.size(file)).append(':')
                                        .append(Files.getLastModifiedTime(file).toMillis()).append('\n');
                            } catch (Exception unreadable) {
                                sig.append(file).append(":?\n");
                            }
                        });
            } catch (Exception unreadable) {
                sig.append(folder).append(":?\n");
            }
        }
        return Integer.toHexString(sig.toString().hashCode()) + "-" + sig.length()
                + "-" + net.fugginbeenus.notchcurrency.client.PackMeta.FORMAT;
    }

    private static String writtenStamp() {
        try {
            Path stamp = packDir().resolve(".stamp");
            return Files.isRegularFile(stamp) ? Files.readString(stamp).strip() : "";
        } catch (Exception unreadable) {
            return "";
        }
    }

    private static NpcModelBundle readOnly(Path folder) {
        String id = folder.getFileName().toString();
        try {
            Path animFile = firstExisting(folder, "animation.json", "animations.json",
                    id + ".animation.json");
            List<String> clips = animFile == null ? List.of() : clipsIn(animFile);
            return buildBundle(id, readManifest(folder), clips);
        } catch (Exception unreadable) {
            return null;
        }
    }

    private static NpcModelBundle readAndWrite(Path folder) {
        String id = folder.getFileName().toString();
        try {
            if (!id.matches("[a-z0-9_]+")) {
                return skip(id, "the folder name may only use lowercase letters, numbers and underscores");
            }

            Path geoFile = firstExisting(folder, "model.geo.json", id + ".geo.json");
            Path animFile = firstExisting(folder, "animation.json", "animations.json",
                    id + ".animation.json");
            Path texFile = firstExisting(folder, "texture.png", id + ".png");
            if (geoFile == null) return skip(id, "no model.geo.json in the folder");
            if (texFile == null) return skip(id, "no texture.png in the folder");

            for (Path file : new Path[]{geoFile, animFile, texFile}) {
                if (file != null && Files.size(file) > MAX_FILE_BYTES) {
                    return skip(id, file.getFileName() + " is larger than 4 MB");
                }
            }

            String problem = problemWith(geoFile, texFile);
            if (problem != null) return skip(id, problem);
            String geoText = Files.readString(geoFile, StandardCharsets.UTF_8);

            List<String> clips = animFile == null ? List.of() : clipsIn(animFile);
            JsonObject manifest = readManifest(folder);
            NpcModelBundle bundle = buildBundle(id, manifest, clips);

            for (String clip : allClips(bundle)) {
                if (!clips.contains(clip)) {
                    return skip(id, "npc.json asks for a clip called \"" + clip
                            + "\", which is not in the animation file");
                }
            }

            writeAssets(bundle, geoText, animFile, texFile);
            return bundle;
        } catch (Exception e) {
            return skip(id, "could not be read: " + e.getMessage());
        }
    }

    public static String problemWith(Path geoFile, Path texFile) {
        JsonObject geo;
        try {
            geo = JsonParser.parseString(
                    Files.readString(geoFile, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception unreadable) {
            return "that model file could not be read as JSON";
        }
        if (!geo.has("minecraft:geometry")) {
            return "that is not a GeckoLib model. In Blockbench use File, Export, "
                    + "GeckoLib Animated Model";
        }

        int texW, texH;
        try (InputStream in = Files.newInputStream(texFile);
             com.mojang.blaze3d.platform.NativeImage image =
                     com.mojang.blaze3d.platform.NativeImage.read(in)) {
            texW = image.getWidth();
            texH = image.getHeight();
        } catch (Exception notAnImage) {
            return "that texture could not be read as a PNG";
        }

        int[] wanted = declaredTextureSize(geo);
        if (wanted != null && (texW != wanted[0] || texH != wanted[1])) {
            return "this model expects a " + wanted[0] + " by " + wanted[1]
                    + " texture, but that image is " + texW + " by " + texH;
        }
        return null;
    }

    public static List<String> clipsIn(Path animFile) {
        try {
            JsonObject root = JsonParser.parseString(
                    Files.readString(animFile, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("animations")) return List.of();
            List<String> names = new ArrayList<>(root.getAsJsonObject("animations").keySet());
            java.util.Collections.sort(names);
            return names;
        } catch (Exception unreadable) {
            return List.of();
        }
    }

    private static int[] declaredTextureSize(JsonObject geo) {
        try {
            JsonElement first = geo.getAsJsonArray("minecraft:geometry").get(0);
            JsonObject desc = first.getAsJsonObject().getAsJsonObject("description");
            if (!desc.has("texture_width") || !desc.has("texture_height")) return null;
            return new int[]{desc.get("texture_width").getAsInt(), desc.get("texture_height").getAsInt()};
        } catch (Exception notThere) {
            return null;
        }
    }

    private static JsonObject readManifest(Path folder) {
        Path file = folder.resolve("npc.json");
        if (!Files.isRegularFile(file)) return new JsonObject();
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception unreadable) {
            return new JsonObject();
        }
    }

    private static NpcModelBundle buildBundle(String id, JsonObject manifest, List<String> clips) {
        JsonObject clipsJson = manifest.has("clips") ? manifest.getAsJsonObject("clips") : new JsonObject();
        String idle = str(clipsJson, "idle", guess(clips, "idle"));
        String walk = str(clipsJson, "walk", guess(clips, "walk"));

        List<String> special = new ArrayList<>();
        if (clipsJson.has("special")) {
            for (JsonElement e : clipsJson.getAsJsonArray("special")) special.add(e.getAsString());
        }

        return new NpcModelBundle(
                id,
                str(manifest, "name", id),
                str(manifest, "author", ""),
                num(manifest, "scale", 1.0f),
                manifest.has("hitbox") ? num(manifest.getAsJsonObject("hitbox"), "width", 0f) : 0f,
                manifest.has("hitbox") ? num(manifest.getAsJsonObject("hitbox"), "height", 0f) : 0f,
                idle, walk, special);
    }

    private static String guess(List<String> clips, String role) {
        for (String clip : clips) {
            if (clip.equals(role) || clip.endsWith("." + role)) return clip;
        }
        return "";
    }

    private static List<String> allClips(NpcModelBundle bundle) {
        List<String> out = new ArrayList<>(bundle.special());
        if (!bundle.idle().isEmpty()) out.add(bundle.idle());
        if (!bundle.walk().isEmpty()) out.add(bundle.walk());
        return out;
    }

    private static void writeAssets(NpcModelBundle bundle, String geoText, Path animFile, Path texFile)
            throws Exception {
        Path assets = packDir().resolve("assets").resolve("notchcurrency");
        String asset = bundle.assetName();

        Files.createDirectories(assets.resolve("geckolib").resolve("models"));
        Files.createDirectories(assets.resolve("geckolib").resolve("animations"));
        Files.createDirectories(assets.resolve("geo"));
        Files.createDirectories(assets.resolve("animations"));
        Files.createDirectories(assets.resolve("textures").resolve("entity"));

        Files.writeString(assets.resolve("geckolib").resolve("models").resolve(asset + ".geo.json"), geoText);
        Files.writeString(assets.resolve("geo").resolve(asset + ".geo.json"), geoText);
        if (animFile != null) {
            byte[] anim = Files.readAllBytes(animFile);
            Files.write(assets.resolve("geckolib").resolve("animations").resolve(asset + ".json"), anim);
            Files.write(assets.resolve("animations").resolve(asset + ".animation.json"), anim);
        }
        Files.copy(texFile, assets.resolve("textures").resolve("entity").resolve(asset + ".png"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writePackMeta() throws Exception {
        Files.createDirectories(packDir());
        Files.writeString(packDir().resolve("pack.mcmeta"),
                net.fugginbeenus.notchcurrency.client.PackMeta.json(
                        "Notch Currency NPC models (generated - edit via config/notchcurrency/npc_models)"));
    }

    private static void writeReadme() throws Exception {
        Path readme = modelsDir().resolve("README.txt");
        if (Files.exists(readme)) return;
        Files.writeString(readme, """
                Custom NPC models for Notch Currency
                ------------------------------------
                One folder per model. The folder name is the model's id, and may only use
                lowercase letters, numbers and underscores.

                  npc_models/
                    town_guard/
                      model.geo.json     from Blockbench: File > Export > GeckoLib Animated Model
                      animation.json     from Blockbench, if the model is animated
                      texture.png
                      npc.json           optional, see below

                npc.json is optional. Without it the mod uses the folder name and looks for
                clips called "idle" and "walk". With it you can be explicit:

                  {
                    "name": "Town Guard",
                    "author": "you",
                    "scale": 1.0,
                    "hitbox": { "width": 0.6, "height": 1.95 },
                    "clips": {
                      "idle": "animation.guard.idle",
                      "walk": "animation.guard.walk",
                      "special": ["animation.guard.wave"]
                    }
                  }

                Drop working files in _import if you like; that folder is ignored.

                Common problems the mod will tell you about:
                  - Exported as Java Block/Entity instead of GeckoLib Animated Model
                  - texture.png is a different size than the model expects
                  - npc.json names a clip the animation file does not have
                """);
    }

    private static Path firstExisting(Path folder, String... names) {
        for (String name : names) {
            Path candidate = folder.resolve(name);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }

    private static NpcModelBundle skip(String id, String why) {
        String line = id + ": " + why;
        PROBLEMS.add(line);
        LOGGER.warn("Skipped NPC model {}", line);
        return null;
    }

    private static String str(JsonObject json, String key, String fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : fallback;
    }

    private static float num(JsonObject json, String key, float fallback) {
        try {
            return json.has(key) ? json.get(key).getAsFloat() : fallback;
        } catch (Exception notANumber) {
            return fallback;
        }
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }
}
