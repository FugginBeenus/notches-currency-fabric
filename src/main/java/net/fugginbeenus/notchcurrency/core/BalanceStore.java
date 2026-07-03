package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.economy.EconomyLedger;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Thin facade used by the rest of the mod. Delegates to the world-persistent
 * {@link BalanceState} and records every change to the {@link EconomyLedger}.
 *
 * Balances are {@code long}. Mutators come in a plain form (reason defaults to
 * {@link TransactionReason#UNSPECIFIED}) and a reason-tagged form; callers should
 * prefer the tagged form so the audit log can distinguish faucets from sinks.
 */
public final class BalanceStore {

    private BalanceStore() {}

    private static BalanceState state(MinecraftServer server) {
        return BalanceState.get(server);
    }

    @FunctionalInterface
    private interface Op { long apply(BalanceState state); }

    /** Apply a mutation, then log the actual (clamped) delta to the ledger. */
    private static long mutate(MinecraftServer server, UUID id, Op op,
                               TransactionReason reason, String detail) {
        BalanceState st = state(server);
        long before = st.get(id);
        long after = op.apply(st);
        EconomyLedger.record(server, id, after - before, after, reason, detail);
        net.fugginbeenus.notchcurrency.economy.ReceiptState.record(server, id, after - before, after, reason, detail);
        return after;
    }

    /* --------- Reads --------- */

    public static long get(ServerPlayerEntity sp) {
        return state(sp.getServer()).get(sp.getUuid());
    }

    public static long get(MinecraftServer server, UUID id) {
        return state(server).get(id);
    }

    /* --------- Mutations by player --------- */

    public static long set(ServerPlayerEntity sp, long value) {
        return set(sp, value, TransactionReason.UNSPECIFIED, null);
    }

    public static long set(ServerPlayerEntity sp, long value, TransactionReason reason, String detail) {
        return mutate(sp.getServer(), sp.getUuid(), st -> st.set(sp.getUuid(), value), reason, detail);
    }

    public static long add(ServerPlayerEntity sp, long delta) {
        return add(sp, delta, TransactionReason.UNSPECIFIED, null);
    }

    public static long add(ServerPlayerEntity sp, long delta, TransactionReason reason) {
        return add(sp, delta, reason, null);
    }

    public static long add(ServerPlayerEntity sp, long delta, TransactionReason reason, String detail) {
        return mutate(sp.getServer(), sp.getUuid(), st -> st.add(sp.getUuid(), delta), reason, detail);
    }

    public static long subtract(ServerPlayerEntity sp, long delta) {
        return subtract(sp, delta, TransactionReason.UNSPECIFIED, null);
    }

    public static long subtract(ServerPlayerEntity sp, long delta, TransactionReason reason) {
        return subtract(sp, delta, reason, null);
    }

    public static long subtract(ServerPlayerEntity sp, long delta, TransactionReason reason, String detail) {
        return mutate(sp.getServer(), sp.getUuid(), st -> st.subtract(sp.getUuid(), delta), reason, detail);
    }

    /* --------- Mutations by UUID (e.g. offline players) --------- */

    public static long set(MinecraftServer server, UUID id, long value) {
        return mutate(server, id, st -> st.set(id, value), TransactionReason.UNSPECIFIED, null);
    }

    public static long add(MinecraftServer server, UUID id, long delta) {
        return add(server, id, delta, TransactionReason.UNSPECIFIED, null);
    }

    public static long add(MinecraftServer server, UUID id, long delta, TransactionReason reason, String detail) {
        return mutate(server, id, st -> st.add(id, delta), reason, detail);
    }

    public static long subtract(MinecraftServer server, UUID id, long delta) {
        return mutate(server, id, st -> st.subtract(id, delta), TransactionReason.UNSPECIFIED, null);
    }
}
