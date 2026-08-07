package net.fugginbeenus.notchcurrency.api;

import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class CurrencyApi {

    private CurrencyApi() {}

    public static long getBalance(ServerPlayer player) {
        if (player == null) return 0L;
        return BalanceStore.get(player);
    }

    public static long getBalance(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return 0L;
        return BalanceStore.get(server, playerId);
    }

    public static synchronized boolean withdraw(ServerPlayer player, long amount) {
        return withdraw(player, amount, TransactionReason.UNSPECIFIED, null);
    }

    public static synchronized boolean withdraw(ServerPlayer player, long amount,
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

    public static boolean tryWithdraw(ServerPlayer player, long amount) {
        return withdraw(player, amount);
    }

    public static void deposit(ServerPlayer player, long amount) {
        deposit(player, amount, TransactionReason.UNSPECIFIED, null);
    }

    public static void deposit(ServerPlayer player, long amount, TransactionReason reason, String detail) {
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
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            syncToClient(online);
        }
    }

    public static void setBalance(ServerPlayer player, long newBalance) {
        if (player == null) return;
        BalanceStore.set(player, Math.max(0L, newBalance));
        syncToClient(player);
    }

    private static void syncToClient(ServerPlayer player) {
        if (player != null) {
            NotchPackets.sendBalance(player, BalanceStore.get(player));
        }
    }
}
