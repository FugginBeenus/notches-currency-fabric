package net.fugginbeenus.notchcurrency.integration;

// Balm dropped its api package and moved the entry point up a level for its 1.21.11 build.
//? if >=1.21.11 {
/*import net.blay09.mods.balm.Balm;
*///?} else {
import net.blay09.mods.balm.api.Balm;
//?}
//? if >=1.21 {
/*import net.blay09.mods.waystones.api.event.WaystoneTeleportEvent;
*///?} else {
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
//?}
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class WaystoneFeeHandler {

    private static boolean enabled = false;
    private static int fee = 50;
    private static int dimensionalFee = 200;
    private static boolean announce = true;

    private WaystoneFeeHandler() {}

    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.waystone.enabled;
        fee = Math.max(0, cfg.waystone.fee);
        dimensionalFee = Math.max(0, cfg.waystone.dimensionalFee);
        announce = cfg.waystone.announce;
    }

    public static void register() {
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Pre.class, WaystoneFeeHandler::onPre);

        // Fees to each joining client, so the waystone selection menu can price its destinations.
        // Join-only: fee changes mid-session reach players on their next login.
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> sendFees(handler.getPlayer()));
    }

    private static void sendFees(ServerPlayer sp) {
        var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeBoolean(enabled);
        buf.writeVarInt(fee);
        buf.writeVarInt(dimensionalFee);
        net.fugginbeenus.notchcurrency.compat.Net.sendToClient(sp, NotchPackets.WAYSTONE_FEE_SYNC, buf);
    }

    private static void onPre(WaystoneTeleportEvent.Pre event) {
        if (!enabled) return;
        if (!(event.getContext().getEntity() instanceof ServerPlayer sp)) return;

        long cost = event.getContext().isDimensionalTeleport() ? dimensionalFee : fee;
        if (cost <= 0) return;

        if (BalanceStore.get(sp) < cost) {
            event.setCanceled(true);
            sp.displayClientMessage(Component.literal("You need " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to use this waystone.")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        BalanceStore.subtract(sp, cost, TransactionReason.SINK, "waystone fee");
        NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        if (announce) {
            sp.displayClientMessage(Component.literal("Paid " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " in waystone fees.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
    }
}
