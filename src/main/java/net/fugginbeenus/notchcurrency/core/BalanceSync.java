package net.fugginbeenus.notchcurrency.core;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;

public final class BalanceSync {
    private BalanceSync() {}

    public static void send(ServerPlayerEntity player, int balance) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(balance);
        ServerPlayNetworking.send(player, NotchPackets.BALANCE_SYNC, buf);
    }
}
