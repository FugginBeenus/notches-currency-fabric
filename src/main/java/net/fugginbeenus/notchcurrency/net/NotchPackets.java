package net.fugginbeenus.notchcurrency.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.function.IntConsumer;

public final class NotchPackets {
    public static final Identifier BALANCE_SYNC    = new Identifier(NotchCurrency.MOD_ID, "balance_sync");
    public static final Identifier BALANCE_REQUEST = new Identifier(NotchCurrency.MOD_ID, "balance_request");

    // Trade channels
    public static final Identifier TRADE_OPEN      = new Identifier(NotchCurrency.MOD_ID, "trade_open");
    public static final Identifier TRADE_UPDATE    = new Identifier(NotchCurrency.MOD_ID, "trade_update");   // money + ready states
    public static final Identifier TRADE_CANCEL    = new Identifier(NotchCurrency.MOD_ID, "trade_cancel");   // reason
    public static final Identifier TRADE_COMPLETE  = new Identifier(NotchCurrency.MOD_ID, "trade_complete"); // success

    public static void sendBalance(ServerPlayerEntity sp, int value) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(value);
        ServerPlayNetworking.send(sp, BALANCE_SYNC, buf);
    }

    public static void requestBalance() {
        ClientPlayNetworking.send(BALANCE_REQUEST, PacketByteBufs.empty());
    }

    public static void registerClientReceiver(IntConsumer onBalance) {
        ClientPlayNetworking.registerGlobalReceiver(BALANCE_SYNC, (client, handler, buf, responseSender) -> {
            int bal = buf.readVarInt();
            client.execute(() -> onBalance.accept(bal));
        });
    }

    private NotchPackets() {}
}
