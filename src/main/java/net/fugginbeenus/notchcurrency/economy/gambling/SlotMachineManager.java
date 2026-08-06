package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

public final class SlotMachineManager {

    private static final Random RNG = new Random();

    private static int houseEdgePercent = 22;
    private static double norm = 1.0;

    private SlotMachineManager() {}

    public static void applyConfig(NotchConfig cfg) {
        houseEdgePercent = Math.max(0, Math.min(90, cfg.gambling.slotsHouseEdgePercent));
        recomputeNorm();
    }

    private static void recomputeNorm() {
        SlotSymbol[] syms = SlotSymbol.values();
        int total = SlotSymbol.totalWeight();
        double expected = 0.0;
        for (int i = 0; i < syms.length; i++) {
            for (int j = 0; j < syms.length; j++) {
                for (int k = 0; k < syms.length; k++) {
                    double p = (syms[i].weight() / (double) total)
                            * (syms[j].weight() / (double) total)
                            * (syms[k].weight() / (double) total);
                    expected += p * rawMultiplier(i, j, k);
                }
            }
        }
        double targetRtp = (100 - houseEdgePercent) / 100.0;
        norm = expected > 0 ? targetRtp / expected : 0.0;
    }

    private static double rawMultiplier(int i, int j, int k) {
        SlotSymbol[] syms = SlotSymbol.values();
        if (i == j && j == k) return syms[i].mult3();
        if (i == j || i == k) return syms[i].mult2(); // the repeated symbol is i
        if (j == k) return syms[j].mult2();
        return 0.0;
    }

    public static int displayMult3x10(SlotSymbol s) {
        return (int) Math.round(s.mult3() * norm * 10.0);
    }

    public static void openScreen(ServerPlayerEntity sp) {
        if (!GamblingManager.isEnabled()) {
            sp.sendMessage(Text.literal("Gambling is disabled on this server.").formatted(Formatting.RED), false);
            return;
        }
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new SlotMachineScreenHandler(syncId, inv),
                Text.literal("Slot Machine")));
    }

    public record SpinResult(int r0, int r1, int r2, long payout, boolean ok) {
        static SpinResult fail() { return new SpinResult(0, 0, 0, 0, false); }
    }

    public static SpinResult spin(ServerPlayerEntity sp, long bet) {
        if (!GamblingManager.isEnabled()) {
            sp.sendMessage(Text.literal("Gambling is disabled on this server.").formatted(Formatting.RED), false);
            return SpinResult.fail();
        }
        if (!GamblingManager.betInRange(bet)) {
            sp.sendMessage(Text.literal("Bet must be between " + GamblingManager.getMinBet()
                    + " and " + GamblingManager.getMaxBet() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").formatted(Formatting.RED), false);
            return SpinResult.fail();
        }
        if (!CurrencyApi.withdraw(sp, bet, TransactionReason.SINK, "Slots bet")) {
            sp.sendMessage(Text.literal("You don't have " + bet + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to bet.").formatted(Formatting.RED), false);
            return SpinResult.fail();
        }

        int r0 = rollReel(), r1 = rollReel(), r2 = rollReel();
        double rawMult = rawMultiplier(r0, r1, r2);
        long payout = Math.round(bet * rawMult * norm);
        if (payout > 0) {
            CurrencyApi.deposit(sp, payout, TransactionReason.FAUCET, "Slots win");
        }
        // The win/loss feedback (sounds, reveal) is played client-side when the reels finish
        // settling, so the payout lands with the spectacle rather than the instant of the click.
        return new SpinResult(r0, r1, r2, payout, true);
    }

    private static int rollReel() {
        int roll = RNG.nextInt(SlotSymbol.totalWeight());
        SlotSymbol[] syms = SlotSymbol.values();
        int cum = 0;
        for (int i = 0; i < syms.length; i++) {
            cum += syms[i].weight();
            if (roll < cum) return i;
        }
        return syms.length - 1;
    }
}
