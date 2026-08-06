package net.fugginbeenus.notchcurrency.api;

import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class CurrencyApi {

    private CurrencyApi() {}

    public static long getBalance(ServerPlayerEntity player) {
        if (player == null) return 0L;
        return BalanceStore.get(player);
    }

    public static long getBalance(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return 0L;
        return BalanceStore.get(server, playerId);
    }

    public static synchronized boolean withdraw(ServerPlayerEntity player, long amount) {
        return withdraw(player, amount, TransactionReason.UNSPECIFIED, null);
    }

    public static synchronized boolean withdraw(ServerPlayerEntity player, long amount,
                                                TransactionReason reason, String detail) {
        if (player == null || amount <= 0) return false;

        long bal = getBalance(player);
        if (bal < amount) {
            return false;
        }
        BalanceStore.subtract(player, amount, reason, detail);
        syncToClient(player);
        return true;
    }

    public static boolean tryWithdraw(ServerPlayerEntity player, long amount) {
        return withdraw(player, amount);
    }

    public static void deposit(ServerPlayerEntity player, long amount) {
        deposit(player, amount, TransactionReason.UNSPECIFIED, null);
    }

    public static void deposit(ServerPlayerEntity player, long amount, TransactionReason reason, String detail) {
        if (player == null || amount <= 0) return;
        BalanceStore.add(player, amount, reason, detail);
        syncToClient(player);
    }

    public static void deposit(MinecraftServer server, UUID playerId, long amount) {
        deposit(server, playerId, amount, TransactionReason.UNSPECIFIED, null);
    }

    public static void deposit(MinecraftServer server, UUID playerId, long amount,
                               TransactionReason reason, String detail) {
        if (server == null || playerId == null || amount <= 0) return;
        BalanceStore.add(server, playerId, amount, reason, detail);

        // If player is online, sync their balance
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(playerId);
        if (online != null) {
            syncToClient(online);
        }
    }

    public static void setBalance(ServerPlayerEntity player, long newBalance) {
        if (player == null) return;
        BalanceStore.set(player, Math.max(0L, newBalance));
        syncToClient(player);
    }

    private static void syncToClient(ServerPlayerEntity player) {
        if (player != null) {
            NotchPackets.sendBalance(player, BalanceStore.get(player));
        }
    }
}
