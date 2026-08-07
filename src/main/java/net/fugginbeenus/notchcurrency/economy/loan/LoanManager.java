package net.fugginbeenus.notchcurrency.economy.loan;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import java.util.Map;
import java.util.UUID;

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

    public static void openScreen(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new LoanScreenHandler(containerId, inv),
                Component.literal("Loans")));
    }

    // ---- borrow / repay ----

    public static void borrow(ServerPlayer player, long amount) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        if (!enabled) {
            player.displayClientMessage(Component.literal("Loans aren't available right now.").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (amount <= 0) return;

        LoanState state = LoanState.get(server);
        long debt = state.getDebt(player.getUUID());
        if (debt + amount > maxDebt) {
            player.displayClientMessage(Component.literal("That exceeds your borrowing limit of ").withStyle(ChatFormatting.RED)
                    .append(NotchCurrency.coins(maxDebt))
                    .append(Component.literal(" (you owe ").withStyle(ChatFormatting.RED))
                    .append(NotchCurrency.coins(debt))
                    .append(Component.literal(").").withStyle(ChatFormatting.RED)), false);
            return;
        }

        CurrencyApi.deposit(player, amount, TransactionReason.FAUCET, "loan borrow");
        state.borrow(player.getUUID(), amount, worldTime(server) + termTicks);
        player.displayClientMessage(Component.literal("Borrowed ").withStyle(ChatFormatting.GREEN)
                .append(NotchCurrency.coins(amount))
                .append(Component.literal(". You owe ").withStyle(ChatFormatting.GREEN))
                .append(NotchCurrency.coins(debt + amount))
                .append(Component.literal(", due in " + termDays + " days.").withStyle(ChatFormatting.GRAY)), false);
    }

    public static void repay(ServerPlayer player, long amount) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        LoanState state = LoanState.get(server);
        long debt = state.getDebt(player.getUUID());
        if (debt <= 0) {
            player.displayClientMessage(Component.literal("You have no loan to repay.").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        long want = amount <= 0 ? debt : amount;
        long pay = Math.min(Math.min(want, debt), CurrencyApi.getBalance(player));
        if (pay <= 0) {
            player.displayClientMessage(Component.literal("You don't have " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to repay with.").withStyle(ChatFormatting.RED), false);
            return;
        }
        CurrencyApi.withdraw(player, pay, TransactionReason.SINK, "loan repay");
        state.setDebt(player.getUUID(), debt - pay);
        long left = debt - pay;
        player.displayClientMessage(Component.literal("Repaid ").withStyle(ChatFormatting.GREEN)
                .append(NotchCurrency.coins(pay))
                .append(Component.literal(left > 0 ? ". Remaining debt: " : ". Loan paid off!").withStyle(ChatFormatting.GREEN))
                .append(left > 0 ? NotchCurrency.coins(left) : Component.empty()), false);
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
                    ServerPlayer online = server.getPlayerList().getPlayer(id);
                    if (online != null) {
                        CurrencyApi.withdraw(online, collect, TransactionReason.SINK, "loan auto-repay");
                        online.displayClientMessage(Component.literal("Loan auto-repaid ").withStyle(ChatFormatting.GRAY)
                                .append(NotchCurrency.coins(collect))
                                .append(Component.literal(" from your balance.").withStyle(ChatFormatting.GRAY)), false);
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
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (overdue) {
                if (!loan.lateFeeApplied && lateFeePercent > 0) {
                    loan.debt += loan.debt * lateFeePercent / 100;
                    loan.lateFeeApplied = true;
                    if (online != null) online.displayClientMessage(Component.literal("⚠ Your loan is OVERDUE - a "
                            + lateFeePercent + "% late fee was added.").withStyle(ChatFormatting.RED), false);
                }
                loan.debt += loan.debt * overdueInterestPercent / 100;
                if (online != null) online.displayClientMessage(Component.literal("Overdue loan penalty interest applied - you owe ")
                        .withStyle(ChatFormatting.RED).append(NotchCurrency.coins(loan.debt)).append(Component.literal(".").withStyle(ChatFormatting.RED)), false);
            } else {
                loan.debt += loan.debt * interestPercent / 100;
            }
            state.markDirtyPublic();
        }
    }

    // ---- helpers ----

    public static long worldTime(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        return ow == null ? 0L : ow.getGameTime();
    }

    public static int daysLeft(MinecraftServer server, UUID player) {
        LoanState.Loan l = LoanState.get(server).get(player);
        if (l == null || l.debt <= 0) return 0;
        return (int) Math.ceil((l.dueTime - worldTime(server)) / (double) TICKS_PER_DAY);
    }
}
