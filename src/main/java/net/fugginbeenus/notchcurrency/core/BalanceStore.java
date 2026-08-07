package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.economy.EconomyLedger;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class BalanceStore {

    private BalanceStore() {}

    private static BalanceState state(MinecraftServer server) {
        return BalanceState.get(server);
    }

    @FunctionalInterface
    private interface Op { long apply(BalanceState state); }

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

    public static long get(ServerPlayer sp) {
        return state(sp.getServer()).get(sp.getUUID());
    }

    public static long get(MinecraftServer server, UUID id) {
        return state(server).get(id);
    }

    /* --------- Mutations by player --------- */

    public static long set(ServerPlayer sp, long value) {
        return set(sp, value, TransactionReason.UNSPECIFIED, null);
    }

    public static long set(ServerPlayer sp, long value, TransactionReason reason, String detail) {
        return mutate(sp.getServer(), sp.getUUID(), st -> st.set(sp.getUUID(), value), reason, detail);
    }

    public static long add(ServerPlayer sp, long delta) {
        return add(sp, delta, TransactionReason.UNSPECIFIED, null);
    }

    public static long add(ServerPlayer sp, long delta, TransactionReason reason) {
        return add(sp, delta, reason, null);
    }

    public static long add(ServerPlayer sp, long delta, TransactionReason reason, String detail) {
        return mutate(sp.getServer(), sp.getUUID(), st -> st.add(sp.getUUID(), delta), reason, detail);
    }

    public static long subtract(ServerPlayer sp, long delta) {
        return subtract(sp, delta, TransactionReason.UNSPECIFIED, null);
    }

    public static long subtract(ServerPlayer sp, long delta, TransactionReason reason) {
        return subtract(sp, delta, reason, null);
    }

    public static long subtract(ServerPlayer sp, long delta, TransactionReason reason, String detail) {
        return mutate(sp.getServer(), sp.getUUID(), st -> st.subtract(sp.getUUID(), delta), reason, detail);
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
