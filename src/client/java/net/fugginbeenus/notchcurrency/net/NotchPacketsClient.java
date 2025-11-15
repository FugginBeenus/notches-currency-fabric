package net.fugginbeenus.notchcurrency.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.util.Identifier;

import java.util.function.IntConsumer;

public final class NotchPacketsClient {
    // Reuse the same identifiers defined in the common class
    public static final Identifier BALANCE_SYNC    = NotchPackets.BALANCE_SYNC;
    public static final Identifier BALANCE_REQUEST = NotchPackets.BALANCE_REQUEST;

    public static final Identifier TRADE_OPEN     = NotchPackets.TRADE_OPEN;
    public static final Identifier TRADE_UPDATE   = NotchPackets.TRADE_UPDATE;
    public static final Identifier TRADE_CANCEL   = NotchPackets.TRADE_CANCEL;
    public static final Identifier TRADE_COMPLETE = NotchPackets.TRADE_COMPLETE;

    private NotchPacketsClient() {}

    public static void requestBalance() {
        ClientPlayNetworking.send(BALANCE_REQUEST, PacketByteBufs.empty());
    }

    public static void registerBalanceReceiver(IntConsumer onBalance) {
        ClientPlayNetworking.registerGlobalReceiver(BALANCE_SYNC, (client, handler, buf, responseSender) -> {
            int bal = buf.readVarInt();
            client.execute(() -> onBalance.accept(bal));
        });
    }
}
