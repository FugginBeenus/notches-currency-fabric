package net.fugginbeenus.notchcurrency.config;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ExampleDatapack {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-Examples");
    private static final String NAME = "notchcurrency_examples";
    private static final List<String> FOLDERS = List.of(
            "data/notchcurrency/notch_bounties",
            "data/notchcurrency/crates",
            "data/notchcurrency/cosmetics");

    public static final int DATA_FORMAT =
            //? if >=26.3 {
            /*121;
            *///?} elif >=26.2 {
            /*107;
            *///?} elif >=26.1 {
            /*101;
            *///?} elif >=1.21.11 {
            /*94;
            *///?} elif >=1.21 {
            /*48;
            *///?} else {
            15;
            //?}

    private ExampleDatapack() {}

    public static void write() {
        Path root = FabricLoader.getInstance().getConfigDir()
                .resolve("notchcurrency").resolve("examples").resolve(NAME);
        ModContainer mod = FabricLoader.getInstance().getModContainer(NotchCurrency.MOD_ID).orElse(null);
        if (mod == null) return;
        try {
            for (String folder : FOLDERS) {
                Path source = mod.findPath(folder).orElse(null);
                if (source == null) continue;
                List<Path> files;
                try (Stream<Path> walk = Files.walk(source)) {
                    files = walk.filter(Files::isRegularFile).toList();
                }
                for (Path file : files) {
                    Path target = root.resolve(folder).resolve(source.relativize(file).toString());
                    if (Files.exists(target)) continue;
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target);
                }
            }
            writeIfChanged(root.resolve("pack.mcmeta"), packMeta());
            writeIfChanged(root.resolve("README.txt"), readme());
        } catch (IOException e) {
            LOGGER.warn("Could not write the example datapack: {}", e.getMessage());
        }
    }

    private static void writeIfChanged(Path file, String text) throws IOException {
        if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).equals(text)) return;
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static String packMeta() {
        String format = DATA_FORMAT >= 82
                ? "\"min_format\": " + DATA_FORMAT + ",\n    \"max_format\": " + DATA_FORMAT
                : "\"pack_format\": " + DATA_FORMAT;
        return "{\n  \"pack\": {\n    " + format
                + ",\n    \"description\": \"Notch Currency example datapack\"\n  }\n}\n";
    }

    private static String readme() {
        String version = FabricLoader.getInstance().getModContainer("minecraft")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("this version");
        return """
                Notch Currency example datapack
                -------------------------------
                This folder is a ready-to-use datapack with the mod's default bounties,
                loot crates and cosmetics shop offers. It does nothing where it is.

                To use it:
                  1. Copy this whole folder into your world's datapacks folder, so it ends
                     up at <world>/datapacks/notchcurrency_examples/
                     On a server, the world folder is usually called "world".
                  2. Edit the JSON files inside data/.
                  3. Run /reload in game, or restart.

                How the files work:
                  - A file with the same path as one of the mod's files replaces it.
                    data/notchcurrency/notch_bounties/objectives/default.json replaces
                    the default bounty list. Make it [] to turn the defaults off.
                  - A file with a new name adds to the mod's list instead.

                pack.mcmeta is set up for Minecraft %s. If you move to another Minecraft
                version, copy this folder again.

                The mod refills this folder when it starts: a deleted file comes back,
                but files you changed are kept. The wiki's Configuration page lists
                every field.
                """.formatted(version);
    }
}
