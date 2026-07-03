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

/**
 * The custom-currency maker. Reads the admin's art from config/notchcurrency/currency/ and a name
 * from the config, and generates a plain resource pack at resourcepacks/NotchCurrencyCustom. One
 * texture (item/coin.png) drives everything — the item, the HUD icon and the chat glyph all point
 * at it — so a single PNG reskins the whole economy. The pack is rebuilt on every game start and
 * deleted when nothing is customized; the player enables it once in Options → Resource Packs.
 */
public final class CurrencyPackGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-CurrencyPack");

    public static final String PACK_DIR_NAME = "NotchCurrencyCustom";
    /** How the pack shows up in ResourcePackManager's enabled-names list. */
    public static final String PACK_PROFILE_NAME = "file/" + PACK_DIR_NAME;

    private static boolean remindedThisSession = false;

    private CurrencyPackGenerator() {}

    /** True when a generated pack exists on disk (used for the enable-it hint on world join). */
    public static boolean packExists() {
        return Files.isDirectory(target());
    }

    /**
     * If the custom pack exists but isn't enabled, nudge the player (once per session) to turn it on.
     * The pack can't be force-enabled from code reliably, so this is a friendly one-line hint.
     */
    public static void remindIfDisabled(MinecraftClient client) {
        if (remindedThisSession || !packExists() || client.player == null) return;
        boolean enabled = client.getResourcePackManager().getEnabledNames().contains(PACK_PROFILE_NAME);
        if (enabled) {
            remindedThisSession = true;
            return;
        }
        remindedThisSession = true;
        client.player.sendMessage(Text.literal("[Notch Currency] Custom coin art is ready — enable ")
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

    /** Regenerate the pack from the current config + art. Safe to call any time. */
    public static void generate() {
        try {
            Path src = sourceDir();
            Files.createDirectories(src);
            writeReadme(src);

            Path coin = src.resolve("coin.png");
            Path tails = src.resolve("coin_tails.png");
            String itemName = NotchConfigIO.get().currency.itemName.trim();

            // Always start clean — stale packs from an older name/art must not linger.
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
                        "pack_format": 15,
                        "description": "Notch Currency custom coin (generated — edit via config/notchcurrency/currency)"
                      }
                    }
                    """);

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
