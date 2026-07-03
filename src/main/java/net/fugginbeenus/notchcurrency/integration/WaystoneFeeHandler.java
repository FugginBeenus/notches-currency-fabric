package net.fugginbeenus.notchcurrency.integration;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Charges a coin fee (a SINK) when a player teleports via a Waystone. This is a SOFT integration:
 * the class references Waystones + Balm types, so it is only ever loaded/registered when the
 * Waystones mod is present (see the isModLoaded guard in NotchCurrency). A player who can't afford
 * the fee has the teleport cancelled with a message. Everything is config-driven and off by default.
 */
public final class WaystoneFeeHandler {

    private static boolean enabled = false;
    private static int fee = 50;
    private static int dimensionalFee = 200;
    private static boolean announce = true;

    private WaystoneFeeHandler() {}

    /** Read config into the local statics. Safe to call even when Waystones is absent. */
    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.waystone.enabled;
        fee = Math.max(0, cfg.waystone.fee);
        dimensionalFee = Math.max(0, cfg.waystone.dimensionalFee);
        announce = cfg.waystone.announce;
    }

    /** Hook the Waystones teleport event. Only call when the Waystones mod is loaded. */
    public static void register() {
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Pre.class, WaystoneFeeHandler::onPre);
    }

    private static void onPre(WaystoneTeleportEvent.Pre event) {
        if (!enabled) return;
        if (!(event.getContext().getEntity() instanceof ServerPlayerEntity sp)) return;

        long cost = event.getContext().isDimensionalTeleport() ? dimensionalFee : fee;
        if (cost <= 0) return;

        if (BalanceStore.get(sp) < cost) {
            event.setCanceled(true);
            sp.sendMessage(Text.literal("You need " + cost + " coins to use this waystone.")
                    .formatted(Formatting.RED), false);
            return;
        }

        BalanceStore.subtract(sp, cost, TransactionReason.SINK, "waystone fee");
        NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        if (announce) {
            sp.sendMessage(Text.literal("Paid " + cost + " coins in waystone fees.")
                    .formatted(Formatting.GRAY), false);
        }
    }
}
