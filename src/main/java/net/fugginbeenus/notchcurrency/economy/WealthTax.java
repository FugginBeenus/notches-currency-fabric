package net.fugginbeenus.notchcurrency.economy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceState;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Progressive wealth tax: a periodic money sink. Each cycle it taxes only the portion of
 * a balance above the configured threshold, so the wealthy are throttled while ordinary
 * players pay nothing. Taxed coins are destroyed (logged as {@link TransactionReason#SINK}).
 *
 * Timing uses an in-memory tick accumulator, so the first cycle lands one interval after
 * server start (acceptable for a coarse daily-ish tax; tune via config).
 */
public final class WealthTax {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-WealthTax");

    private static boolean enabled = false;
    private static long threshold = 100_000L;
    private static int ratePercent = 1;
    private static long intervalTicks = 1440L * 60L * 20L;
    private static boolean announce = true;

    private static long tickAccum = 0;

    private WealthTax() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(WealthTax::tick);
    }

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.WealthTax t = cfg.wealthTax;
        enabled = t.enabled;
        threshold = Math.max(0L, t.threshold);
        ratePercent = Math.max(0, Math.min(100, t.ratePercent));
        intervalTicks = Math.max(1L, (long) t.intervalMinutes) * 60L * 20L;
        announce = t.announce;
    }

    private static void tick(MinecraftServer server) {
        if (!enabled || ratePercent <= 0) return;
        if (++tickAccum < intervalTicks) return;
        tickAccum = 0;
        collect(server);
    }

    private static void collect(MinecraftServer server) {
        Map<UUID, Long> snapshot = BalanceState.get(server).snapshot();
        long totalTaxed = 0L;
        int affected = 0;

        for (Map.Entry<UUID, Long> e : snapshot.entrySet()) {
            long bal = e.getValue();
            if (bal <= threshold) continue;

            long excess = bal - threshold;
            long tax = excess * ratePercent / 100L;
            if (tax <= 0) continue;

            // subtract = add a negative delta, tagged as a sink so it counts as destroyed
            BalanceStore.add(server, e.getKey(), -tax, TransactionReason.SINK, "wealth tax");
            totalTaxed += tax;
            affected++;

            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p != null) {
                NotchPackets.sendBalance(p, BalanceStore.get(p));
                if (announce) {
                    p.sendMessage(Text.literal("Wealth tax: ")
                            .formatted(Formatting.GRAY)
                            .append(NotchCurrency.coins(tax))
                            .append(Text.literal(" was deducted from your balance.").formatted(Formatting.GRAY)), false);
                }
            }
        }

        if (affected > 0) {
            LOGGER.info("Wealth tax collected {} coins from {} account(s)", totalTaxed, affected);
        }
    }
}
