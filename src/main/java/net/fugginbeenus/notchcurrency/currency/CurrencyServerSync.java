package net.fugginbeenus.notchcurrency.currency;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pushes the server's custom-currency skin to every joining player, so the admin sets the coin art
 * + name once (config/notchcurrency/currency/ + the config's currency.itemName) and the whole
 * server sees it — no hand-distributed resource pack. The client writes the payload into a local
 * "NotchCurrencyServer" pack and auto-enables it. An all-empty payload tells the client to clear
 * any pack left over from a previously customized server.
 *
 * The host of a singleplayer/LAN world is skipped — their own local pack generator already ran.
 */
public final class CurrencyServerSync {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-CurrencySync");

    /** Keep each texture comfortably under the 1 MB S2C payload limit. */
    private static final int MAX_TEXTURE_BYTES = 256 * 1024;

    private CurrencyServerSync() {}

    /** Called on player join. Sends the server's coin skin (or an explicit "nothing customized"). */
    public static void send(ServerPlayerEntity sp) {
        if (sp.getServer() != null && sp.getServer().isHost(sp.getGameProfile())) return;

        Path dir = FabricLoader.getInstance().getConfigDir().resolve("notchcurrency").resolve("currency");
        String itemName = NotchConfigIO.get().currency.itemName.trim();
        byte[] coin = readTexture(dir.resolve("coin.png"));
        byte[] tails = readTexture(dir.resolve("coin_tails.png"));

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(itemName, 64);
        buf.writeBoolean(coin != null);
        if (coin != null) buf.writeByteArray(coin);
        buf.writeBoolean(tails != null);
        if (tails != null) buf.writeByteArray(tails);
        ServerPlayNetworking.send(sp, NotchPackets.CURRENCY_SYNC, buf);
    }

    private static byte[] readTexture(Path file) {
        try {
            if (!Files.isRegularFile(file)) return null;
            if (Files.size(file) > MAX_TEXTURE_BYTES) {
                LOGGER.warn("{} is over {} KB and won't be synced to players — shrink the PNG",
                        file.getFileName(), MAX_TEXTURE_BYTES / 1024);
                return null;
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            LOGGER.error("Couldn't read {} for currency sync", file, e);
            return null;
        }
    }
}
