package net.fugginbeenus.notchcurrency.api;

import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Public API for currency operations.
 *
 * Delegates all operations to BalanceStore which persists to world data.
 * External mods can safely use this API for currency integration.
 *
 * Amounts are {@code long}; callers may still pass {@code int} (it widens).
 */
public final class CurrencyApi {

    private CurrencyApi() {}

    /** Get the player's coin balance (0 if none). */
    public static long getBalance(ServerPlayerEntity player) {
        if (player == null) return 0L;
        return BalanceStore.get(player);
    }

    /** Get balance by UUID (requires server reference). */
    public static long getBalance(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return 0L;
        return BalanceStore.get(server, playerId);
    }

    /**
     * Try to withdraw coins from a player atomically.
     * @return true if successful, false if not enough balance.
     */
    public static synchronized boolean withdraw(ServerPlayerEntity player, long amount) {
        return withdraw(player, amount, TransactionReason.UNSPECIFIED, null);
    }

    /** Withdraw with an audit-log reason/detail. */
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

    /**
     * Alias for withdraw - tries to withdraw and returns success status.
     * More explicit naming for atomic operations.
     */
    public static boolean tryWithdraw(ServerPlayerEntity player, long amount) {
        return withdraw(player, amount);
    }

    /** Deposit coins to a player's account. */
    public static void deposit(ServerPlayerEntity player, long amount) {
        deposit(player, amount, TransactionReason.UNSPECIFIED, null);
    }

    /** Deposit with an audit-log reason/detail. */
    public static void deposit(ServerPlayerEntity player, long amount, TransactionReason reason, String detail) {
        if (player == null || amount <= 0) return;
        BalanceStore.add(player, amount, reason, detail);
        syncToClient(player);
    }

    /**
     * Deposit coins to an offline player's account by UUID.
     * Useful for shop sales when the owner is offline.
     */
    public static void deposit(MinecraftServer server, UUID playerId, long amount) {
        deposit(server, playerId, amount, TransactionReason.UNSPECIFIED, null);
    }

    /** Deposit by UUID with an audit-log reason/detail. */
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

    /** Set a player's balance directly. */
    public static void setBalance(ServerPlayerEntity player, long newBalance) {
        if (player == null) return;
        BalanceStore.set(player, Math.max(0L, newBalance));
        syncToClient(player);
    }

    /** Sync balance to client HUD. */
    private static void syncToClient(ServerPlayerEntity player) {
        if (player != null) {
            NotchPackets.sendBalance(player, BalanceStore.get(player));
        }
    }
}
