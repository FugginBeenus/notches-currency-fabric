package net.fugginbeenus.notchcurrency.economy.loan;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;

/**
 * Runs loans: borrowing creates coins (up to a cap) that must be repaid with interest before the
 * loan's term expires. Interest compounds each cycle (a SINK when repaid); auto-collect pulls
 * spare balance toward the debt. If a loan goes <b>overdue</b>, a one-time late fee is added and a
 * higher penalty interest rate applies: the consequence for not paying it back in time.
 */
public final class LoanManager {

    private static final long TICKS_PER_DAY = 24L * 60L * 60L * 20L;

    private static boolean enabled = false;
    private static long maxDebt = 10_000L;
    private static int interestPercent = 5;
    private static long intervalTicks = 1440L * 60L * 20L;
    private static boolean autoCollect = true;
    private static long termTicks = 7L * TICKS_PER_DAY;
    private static int termDays = 7;
    private static int lateFeePercent = 10;
    private static int overdueInterestPercent = 20;

    private static long tickAccum = 0;

    private LoanManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(LoanManager::tick);
    }

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.Loan l = cfg.loan;
        enabled = l.enabled;
        maxDebt = Math.max(0L, l.maxDebt);
        interestPercent = Math.max(0, l.interestPercentPerCycle);
        intervalTicks = Math.max(1L, (long) l.intervalMinutes) * 60L * 20L;
        autoCollect = l.autoCollect;
        termDays = Math.max(1, l.termDays);
        termTicks = (long) termDays * TICKS_PER_DAY;
        lateFeePercent = Math.max(0, l.lateFeePercent);
        overdueInterestPercent = Math.max(0, l.overdueInterestPercent);
    }

    // ---- getters for the GUI ----

    public static boolean isEnabled() { return enabled; }
    public static long getMaxDebt() { return maxDebt; }
    public static int getInterestPercent() { return interestPercent; }
    public static int getTermDays() { return termDays; }

    public static void openScreen(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new LoanScreenHandler(syncId, inv),
                Text.literal("Loans")));
    }

    // ---- borrow / repay ----

    public static void borrow(ServerPlayerEntity player, long amount) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        if (!enabled) {
            player.sendMessage(Text.literal("Loans aren't available right now.").formatted(Formatting.RED), false);
            return;
        }
        if (amount <= 0) return;

        LoanState state = LoanState.get(server);
        long debt = state.getDebt(player.getUuid());
        if (debt + amount > maxDebt) {
            player.sendMessage(Text.literal("That exceeds your borrowing limit of ").formatted(Formatting.RED)
                    .append(NotchCurrency.coins(maxDebt))
                    .append(Text.literal(" (you owe ").formatted(Formatting.RED))
                    .append(NotchCurrency.coins(debt))
                    .append(Text.literal(").").formatted(Formatting.RED)), false);
            return;
        }

        CurrencyApi.deposit(player, amount, TransactionReason.FAUCET, "loan borrow");
        state.borrow(player.getUuid(), amount, worldTime(server) + termTicks);
        player.sendMessage(Text.literal("Borrowed ").formatted(Formatting.GREEN)
                .append(NotchCurrency.coins(amount))
                .append(Text.literal(". You owe ").formatted(Formatting.GREEN))
                .append(NotchCurrency.coins(debt + amount))
                .append(Text.literal(", due in " + termDays + " days.").formatted(Formatting.GRAY)), false);
    }

    /** Repay up to {@code amount} (or the whole debt if {@code amount <= 0}). */
    public static void repay(ServerPlayerEntity player, long amount) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        LoanState state = LoanState.get(server);
        long debt = state.getDebt(player.getUuid());
        if (debt <= 0) {
            player.sendMessage(Text.literal("You have no loan to repay.").formatted(Formatting.GRAY), false);
            return;
        }
        long want = amount <= 0 ? debt : amount;
        long pay = Math.min(Math.min(want, debt), CurrencyApi.getBalance(player));
        if (pay <= 0) {
            player.sendMessage(Text.literal("You don't have " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to repay with.").formatted(Formatting.RED), false);
            return;
        }
        CurrencyApi.withdraw(player, pay, TransactionReason.SINK, "loan repay");
        state.setDebt(player.getUuid(), debt - pay);
        long left = debt - pay;
        player.sendMessage(Text.literal("Repaid ").formatted(Formatting.GREEN)
                .append(NotchCurrency.coins(pay))
                .append(Text.literal(left > 0 ? ". Remaining debt: " : ". Loan paid off!").formatted(Formatting.GREEN))
                .append(left > 0 ? NotchCurrency.coins(left) : Text.empty()), false);
    }

    // ---- interest / auto-collection / overdue cycle ----

    private static void tick(MinecraftServer server) {
        if (!enabled) return;
        if (++tickAccum < intervalTicks) return;
        tickAccum = 0;

        long now = worldTime(server);
        LoanState state = LoanState.get(server);
        for (Map.Entry<UUID, LoanState.Loan> e : state.snapshot().entrySet()) {
            UUID id = e.getKey();
            LoanState.Loan loan = e.getValue();
            if (loan.debt <= 0) continue;

            // Pull spare balance toward the debt first.
            if (autoCollect) {
                long bal = BalanceStore.get(server, id);
                long collect = Math.min(bal, loan.debt);
                if (collect > 0) {
                    ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
                    if (online != null) {
                        CurrencyApi.withdraw(online, collect, TransactionReason.SINK, "loan auto-repay");
                        online.sendMessage(Text.literal("Loan auto-repaid ").formatted(Formatting.GRAY)
                                .append(NotchCurrency.coins(collect))
                                .append(Text.literal(" from your balance.").formatted(Formatting.GRAY)), false);
                    } else {
                        BalanceStore.add(server, id, -collect, TransactionReason.SINK, "loan auto-repay");
                    }
                    loan.debt -= collect;
                }
            }
            if (loan.debt <= 0) {
                state.setDebt(id, 0);
                continue;
            }

            boolean overdue = now >= loan.dueTime;
            ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
            if (overdue) {
                if (!loan.lateFeeApplied && lateFeePercent > 0) {
                    loan.debt += loan.debt * lateFeePercent / 100;
                    loan.lateFeeApplied = true;
                    if (online != null) online.sendMessage(Text.literal("⚠ Your loan is OVERDUE - a "
                            + lateFeePercent + "% late fee was added.").formatted(Formatting.RED), false);
                }
                loan.debt += loan.debt * overdueInterestPercent / 100;
                if (online != null) online.sendMessage(Text.literal("Overdue loan penalty interest applied - you owe ")
                        .formatted(Formatting.RED).append(NotchCurrency.coins(loan.debt)).append(Text.literal(".").formatted(Formatting.RED)), false);
            } else {
                loan.debt += loan.debt * interestPercent / 100;
            }
            state.markDirtyPublic();
        }
    }

    // ---- helpers ----

    public static long worldTime(MinecraftServer server) {
        ServerWorld ow = server.getOverworld();
        return ow == null ? 0L : ow.getTime();
    }

    /** Real-days until this player's loan is due (negative = overdue), or 0 if no loan. */
    public static int daysLeft(MinecraftServer server, UUID player) {
        LoanState.Loan l = LoanState.get(server).get(player);
        if (l == null || l.debt <= 0) return 0;
        return (int) Math.ceil((l.dueTime - worldTime(server)) / (double) TICKS_PER_DAY);
    }
}
