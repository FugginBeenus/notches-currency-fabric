package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

public final class CurrencyPackGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-CurrencyPack");

    public static final String PACK_DIR_NAME = "NotchCurrencyCustom";
    public static final String PACK_PROFILE_NAME = "file/" + PACK_DIR_NAME;

    public static final String SERVER_PACK_DIR_NAME = "NotchCurrencyServer";
    public static final String SERVER_PACK_PROFILE_NAME = "file/" + SERVER_PACK_DIR_NAME;

    private static boolean remindedThisSession = false;

    private CurrencyPackGenerator() {}

    public static boolean packExists() {
        return Files.isDirectory(target());
    }

    private static final int PACK_FORMAT =
            //? if >=1.21 {
            /*34;
            *///?} else {
            15;
            //?}

    private static java.util.Collection<String> enabledPacks(MinecraftClient client) {
        //? if >=1.21 {
        /*return client.getResourcePackManager().getEnabledIds();
        *///?} else {
        return client.getResourcePackManager().getEnabledNames();
        //?}
    }

    public static void remindIfDisabled(MinecraftClient client) {
        if (remindedThisSession || !packExists() || client.player == null) return;
        boolean enabled = enabledPacks(client).contains(PACK_PROFILE_NAME);
        if (enabled) {
            remindedThisSession = true;
            return;
        }
        remindedThisSession = true;
        client.player.sendMessage(Text.literal("[Notch Currency] Custom coin art is ready - enable ")
                .formatted(Formatting.GOLD)
                .append(Text.literal("\"NotchCurrencyCustom\"").formatted(Formatting.YELLOW))
                .append(Text.literal(" in Options → Resource Packs to see it.").formatted(Formatting.GOLD)), false);
    }

    private static Path target() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(PACK_DIR_NAME);
    }

    private static Path sourceDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("notchcurrency").resolve("currency");
    }

    public static void generate() {
        try {
            Path src = sourceDir();
            Files.createDirectories(src);
            writeReadme(src);

            Path coin = src.resolve("coin.png");
            Path tails = src.resolve("coin_tails.png");
            String itemName = NotchConfigIO.get().currency.itemName.trim();

            // Always start clean: stale packs from an older name/art must not linger.
            deleteRecursively(target());

            boolean hasArt = Files.isRegularFile(coin);
            boolean hasTails = Files.isRegularFile(tails);
            if (!hasArt && !hasTails && itemName.isEmpty()) {
                return; // nothing customized, no pack
            }

            Path assets = target().resolve("assets").resolve("notchcurrency");
            Files.createDirectories(assets.resolve("textures").resolve("item"));

            Files.writeString(target().resolve("pack.mcmeta"), """
                    {
                      "pack": {
                        "pack_format": %d,
                        "description": "Notch Currency custom coin (generated - edit via config/notchcurrency/currency)"
                      }
                    }
                    """.formatted(PACK_FORMAT));

            if (hasArt) {
                Files.copy(coin, assets.resolve("textures").resolve("item").resolve("coin.png"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            if (hasTails) {
                Files.copy(tails, assets.resolve("textures").resolve("item").resolve("coin_tails.png"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            if (!itemName.isEmpty()) {
                Files.createDirectories(assets.resolve("lang"));
                String json = "{\n  \"item.notchcurrency.notch_coin\": " + quote(itemName)
                        + ",\n  \"item.notchcurrency.coin_tails\": " + quote(itemName + " (Tails)") + "\n}\n";
                Files.writeString(assets.resolve("lang").resolve("en_us.json"), json);
            }

            LOGGER.info("Generated the custom currency pack (art: {}, tails: {}, name: '{}') at {}",
                    hasArt, hasTails, itemName, target());
        } catch (IOException e) {
            LOGGER.error("Couldn't generate the custom currency pack", e);
        }
    }

    public static void applyServerData(MinecraftClient client, String itemName, byte[] coin, byte[] tails) {
        try {
            Path pack = FabricLoader.getInstance().getGameDir().resolve("resourcepacks")
                    .resolve(SERVER_PACK_DIR_NAME);
            itemName = itemName == null ? "" : itemName.trim();
            boolean empty = itemName.isEmpty() && coin == null && tails == null;

            // Fingerprint the payload so identical re-joins don't rewrite + reload every time.
            String stamp = itemName + "|" + (coin == null ? -1 : java.util.Arrays.hashCode(coin))
                    + "|" + (tails == null ? -1 : java.util.Arrays.hashCode(tails));
            Path stampFile = pack.resolve("sync_stamp.txt");
            if (!empty && Files.isRegularFile(stampFile) && stamp.equals(Files.readString(stampFile))) {
                enableServerPack(client, false);
                return;
            }

            deleteRecursively(pack);
            if (empty) {
                boolean wasEnabled = enabledPacks(client).contains(SERVER_PACK_PROFILE_NAME);
                if (wasEnabled) {
                    client.getResourcePackManager().scanPacks();
                    client.getResourcePackManager().disable(SERVER_PACK_PROFILE_NAME);
                    client.options.refreshResourcePacks(client.getResourcePackManager());
                }
                return;
            }

            Path assets = pack.resolve("assets").resolve("notchcurrency");
            Files.createDirectories(assets.resolve("textures").resolve("item"));
            Files.writeString(pack.resolve("pack.mcmeta"), """
                    {
                      "pack": {
                        "pack_format": %d,
                        "description": "This server's coin skin (synced by Notch Currency)"
                      }
                    }
                    """.formatted(PACK_FORMAT));
            if (coin != null) {
                Files.write(assets.resolve("textures").resolve("item").resolve("coin.png"), coin);
            }
            if (tails != null) {
                Files.write(assets.resolve("textures").resolve("item").resolve("coin_tails.png"), tails);
            }
            if (!itemName.isEmpty()) {
                Files.createDirectories(assets.resolve("lang"));
                String json = "{\n  \"item.notchcurrency.notch_coin\": " + quote(itemName)
                        + ",\n  \"item.notchcurrency.coin_tails\": " + quote(itemName + " (Tails)") + "\n}\n";
                Files.writeString(assets.resolve("lang").resolve("en_us.json"), json);
            }
            Files.writeString(stampFile, stamp);

            LOGGER.info("Received the server's coin skin (art: {}, tails: {}, name: '{}')",
                    coin != null, tails != null, itemName);
            enableServerPack(client, true);
        } catch (IOException e) {
            LOGGER.error("Couldn't apply the server's coin skin", e);
        }
    }

    private static void enableServerPack(MinecraftClient client, boolean contentChanged) {
        try {
            var mgr = client.getResourcePackManager();
            mgr.scanPacks();
            if (!enabledPacks(client).contains(SERVER_PACK_PROFILE_NAME)) {
                if (mgr.enable(SERVER_PACK_PROFILE_NAME)) {
                    client.options.refreshResourcePacks(mgr);
                    client.reloadResources();
                } else if (client.player != null) {
                    client.player.sendMessage(Text.literal(
                            "[Notch Currency] This server has custom coin art - enable \"NotchCurrencyServer\" in Options → Resource Packs.")
                            .formatted(Formatting.GOLD), false);
                }
            } else if (contentChanged) {
                client.reloadResources();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't auto-enable the server coin pack", e);
        }
    }

    private static void writeReadme(Path src) throws IOException {
        Path readme = src.resolve("README.txt");
        if (Files.exists(readme)) return;
        Files.writeString(readme, """
                Custom currency art for Notch Currency
                ---------------------------------------
                Drop your coin texture here as coin.png (a square PNG, 16x16 recommended).
                Optional: coin_tails.png for the coin-flip tails side.
                The coin's display name is set in the mod's config screen (ModMenu -> Notch Currency -> Currency).

                On the next game start a resource pack called "NotchCurrencyCustom" is generated in the
                resourcepacks folder. Enable it once in Options -> Resource Packs and your art shows on
                the coin item, the balance HUD and the coin symbol in chat, everywhere at once.

                On a dedicated server, put the same files in the SERVER's config/notchcurrency/currency
                folder (and set the name in the server's config file). The skin is pushed to every player
                automatically when they join - nothing to distribute.
                """);
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
