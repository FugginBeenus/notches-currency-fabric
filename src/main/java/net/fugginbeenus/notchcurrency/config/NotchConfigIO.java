package net.fugginbeenus.notchcurrency.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NotchConfigIO {
    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("notchcurrency.json");

    private static NotchConfig CURRENT = null;

    private NotchConfigIO() {}

    public static NotchConfig get() {
        if (CURRENT == null) CURRENT = load();
        return CURRENT;
    }

    public static NotchConfig load() {
        try {
            if (Files.notExists(FILE)) {
                NotchConfig def = new NotchConfig();
                save(def); // write defaults
                return def;
            }
            try (Reader r = Files.newBufferedReader(FILE)) {
                NotchConfig cfg = GSON.fromJson(r, NotchConfig.class);
                if (cfg == null) cfg = new NotchConfig();
                CURRENT = cfg;
                return cfg;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config; using defaults", e);
            NotchConfig fallback = new NotchConfig();
            CURRENT = fallback;
            return fallback;
        }
    }

    public static void save(NotchConfig cfg) {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(cfg, w);
            }
            CURRENT = cfg;
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
