package net.fugginbeenus.notchcurrency.core;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import io.netty.buffer.Unpooled;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BalanceData {
    private static final ConcurrentHashMap<UUID, Integer> BALANCES = new ConcurrentHashMap<>();

    private BalanceData() {}

    public static int get(ServerPlayerEntity player) {
        return BALANCES.getOrDefault(player.getUuid(), 0);
    }

    public static void set(ServerPlayerEntity player, int amount) {
        BALANCES.put(player.getUuid(), Math.max(0, amount));
        sync(player);
    }

    public static void add(ServerPlayerEntity player, int delta) {
        BALANCES.merge(player.getUuid(), Math.max(0, delta), Integer::sum);
        sync(player);
    }

    public static void sync(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(get(player));
        ServerPlayNetworking.send(player, NotchPackets.BALANCE_SYNC, buf);
    }
}
