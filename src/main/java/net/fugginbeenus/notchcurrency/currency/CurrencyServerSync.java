package net.fugginbeenus.notchcurrency.currency;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CurrencyServerSync {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-CurrencySync");

    private static final int MAX_TEXTURE_BYTES = 256 * 1024;

    private CurrencyServerSync() {}

    public static void send(ServerPlayer sp) {
        if (sp.level().getServer() != null
                && net.fugginbeenus.notchcurrency.compat.Profiles.isSingleplayerOwner(sp.level().getServer(), sp)) return;

        Path dir = FabricLoader.getInstance().getConfigDir().resolve("notchcurrency").resolve("currency");
        String itemName = NotchConfigIO.get().currency.itemName.trim();
        byte[] coin = readTexture(dir.resolve("coin.png"));
        byte[] tails = readTexture(dir.resolve("coin_tails.png"));

        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(itemName, 64);
        buf.writeBoolean(coin != null);
        if (coin != null) buf.writeByteArray(coin);
        buf.writeBoolean(tails != null);
        if (tails != null) buf.writeByteArray(tails);
        Net.sendToClient(sp, NotchPackets.CURRENCY_SYNC, buf);
    }

    private static byte[] readTexture(Path file) {
        try {
            if (!Files.isRegularFile(file)) return null;
            if (Files.size(file) > MAX_TEXTURE_BYTES) {
                LOGGER.warn("{} is over {} KB and won't be synced to players - shrink the PNG",
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
