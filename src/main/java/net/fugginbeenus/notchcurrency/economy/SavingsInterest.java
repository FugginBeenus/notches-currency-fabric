package net.fugginbeenus.notchcurrency.economy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceState;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public final class SavingsInterest {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-SavingsInterest");

    private static boolean enabled = false;
    private static int ratePercent = 1;
    private static long intervalTicks = 1440L * 60L * 20L;
    private static long maxPerCycle = 1_000L;
    private static boolean announce = true;

    private static long tickAccum = 0;

    private SavingsInterest() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SavingsInterest::tick);
    }

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.Savings s = cfg.savings;
        enabled = s.enabled;
        ratePercent = Math.max(0, Math.min(100, s.ratePercent));
        intervalTicks = Math.max(1L, (long) s.intervalMinutes) * 60L * 20L;
        maxPerCycle = Math.max(0L, s.maxPerCycle);
        announce = s.announce;
    }

    private static void tick(MinecraftServer server) {
        if (!enabled || ratePercent <= 0) return;
        if (++tickAccum < intervalTicks) return;
        tickAccum = 0;
        pay(server);
    }

    private static void pay(MinecraftServer server) {
        Map<UUID, Long> snapshot = BalanceState.get(server).snapshot();
        long totalPaid = 0L;
        int affected = 0;
        for (Map.Entry<UUID, Long> e : snapshot.entrySet()) {
            long bal = e.getValue();
            if (bal <= 0) continue;
            long interest = bal / 100L * ratePercent + (bal % 100L) * ratePercent / 100L;
            if (maxPerCycle > 0) interest = Math.min(interest, maxPerCycle);
            if (interest <= 0) continue;
            BalanceStore.add(server, e.getKey(), interest, TransactionReason.FAUCET, "savings interest");
            totalPaid += interest;
            affected++;
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                NotchPackets.sendBalance(p, BalanceStore.get(p));
                if (announce) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Interest: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(NotchCurrency.coins(interest))
                            .append(Component.literal(" was added to your balance.").withStyle(ChatFormatting.GRAY)));
                }
            }
        }
        if (affected > 0) {
            LOGGER.info("Savings interest paid {} coins to {} account(s)", totalPaid, affected);
        }
    }
}
