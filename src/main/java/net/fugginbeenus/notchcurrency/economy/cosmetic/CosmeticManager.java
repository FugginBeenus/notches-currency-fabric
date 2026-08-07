package net.fugginbeenus.notchcurrency.economy.cosmetic;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public final class CosmeticManager {

    private static boolean enabled = true;

    private CosmeticManager() {}

    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.cosmetic.enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void openScreen(ServerPlayer sp, java.util.UUID npcId) {
        if (!enabled) {
            sp.displayClientMessage(Component.literal("The cosmetics shop is closed right now.").withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        if (CosmeticRegistry.count() == 0) {
            sp.displayClientMessage(Component.literal("No cosmetics are on offer yet.").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        CosmeticShopScreenHandler.open(sp, npcId);
    }

    public static void buy(ServerPlayer sp, String offerId) {
        if (!enabled) return;
        CosmeticOffer offer = CosmeticRegistry.get(offerId);
        if (offer == null) {
            sp.displayClientMessage(Component.literal("That cosmetic is no longer available.").withStyle(ChatFormatting.RED), false);
            return;
        }
        CosmeticState state = CosmeticState.get(sp.getServer());
        if (offer.oneTime() && state.owns(sp.getUUID(), offerId)) {
            sp.displayClientMessage(Component.literal("You already own that cosmetic.").withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        if (BalanceStore.get(sp) < offer.price()) {
            sp.displayClientMessage(Component.literal("You need " + offer.price() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " for that.").withStyle(ChatFormatting.RED), false);
            return;
        }

        BalanceStore.subtract(sp, offer.price(), TransactionReason.SINK, "cosmetic: " + offerId);
        NotchPackets.sendBalance(sp, BalanceStore.get(sp));

        if (offer.isCommand()) {
            String cmd = offer.command()
                    .replace("%player%", sp.getName().getString())
                    .replace("%uuid%", sp.getUUID().toString());
            // Run as the server (console) so unlock commands don't need the player to be an op.
            sp.getServer().getCommands().performPrefixedCommand(sp.getServer().createCommandSourceStack(), cmd);
        } else if (!offer.itemReward().isEmpty()) {
            ItemStack reward = offer.itemReward().copy();
            sp.getInventory().placeItemBackInInventory(reward);
        }

        if (offer.oneTime()) {
            state.markOwned(sp.getUUID(), offerId);
        }

        sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.6f, 1.4f);
        sp.displayClientMessage(Component.literal("Purchased ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(offer.name()).withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" for " + offer.price() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").withStyle(ChatFormatting.GREEN)), false);
    }
}
