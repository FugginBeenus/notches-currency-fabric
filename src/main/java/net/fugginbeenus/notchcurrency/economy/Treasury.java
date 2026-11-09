package net.fugginbeenus.notchcurrency.economy;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.core.BalanceState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Treasury {

    // A reserved account, not a player.
    public static final UUID ID = UUID.nameUUIDFromBytes("notchcurrency:treasury".getBytes(StandardCharsets.UTF_8));

    private Treasury() {}

    public static long balance(MinecraftServer server) {
        return BalanceState.get(server).get(ID);
    }

    // Runs on every balance change. Returns the reason to record.
    public static TransactionReason settle(MinecraftServer server, UUID id, long delta,
                                           TransactionReason reason, @Nullable String detail) {
        NotchConfig.Treasury cfg = NotchConfigIO.get().treasury;
        if (!cfg.enabled || delta == 0 || ID.equals(id)) return reason;

        boolean gambling = reason == TransactionReason.GAMBLING;
        if (gambling && !cfg.collectGambling) return reason;

        boolean collects = delta < 0 && (gambling || reason == TransactionReason.SINK);
        boolean pays = delta > 0 && (gambling || (cfg.fundRewards && reason == TransactionReason.FAUCET))
                && balance(server) >= delta;
        if (!collects && !pays) return reason;

        move(server, -delta, detail);
        return TransactionReason.TREASURY;
    }

    public static void collect(MinecraftServer server, long amount, @Nullable String detail) {
        if (amount > 0 && NotchConfigIO.get().treasury.enabled) move(server, amount, detail);
    }

    public static boolean pay(ServerPlayer target, long amount, String detail) {
        MinecraftServer server = target.level().getServer();
        if (amount <= 0 || balance(server) < amount) return false;
        move(server, -amount, detail);
        CurrencyApi.deposit(target, amount, TransactionReason.TREASURY, detail);
        return true;
    }

    public static long set(MinecraftServer server, long value, String detail) {
        BalanceState state = BalanceState.get(server);
        long before = state.get(ID);
        long after = state.set(ID, value);
        EconomyLedger.record(server, ID, after - before, after, TransactionReason.ADMIN, detail);
        return after;
    }

    private static void move(MinecraftServer server, long amount, @Nullable String detail) {
        long after = BalanceState.get(server).add(ID, amount);
        EconomyLedger.record(server, ID, amount, after, TransactionReason.TREASURY, detail);
    }
}
