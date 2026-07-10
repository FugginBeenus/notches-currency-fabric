package net.fugginbeenus.notchcurrency.economy.cosmetic;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The cosmetics shop: buy cosmetics for coins (a SINK). A cosmetic is generic — buying it either
 * gives an item or runs an unlock command, so the shop can sell cosmetics from any mod. One-time
 * cosmetics are recorded per player so they can't be bought twice.
 */
public final class CosmeticManager {

    private static boolean enabled = true;

    private CosmeticManager() {}

    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.cosmetic.enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void openScreen(ServerPlayerEntity sp, java.util.UUID npcId) {
        if (!enabled) {
            sp.sendMessage(Text.literal("The cosmetics shop is closed right now.").formatted(Formatting.YELLOW), false);
            return;
        }
        if (CosmeticRegistry.count() == 0) {
            sp.sendMessage(Text.literal("No cosmetics are on offer yet.").formatted(Formatting.GRAY), false);
            return;
        }
        CosmeticShopScreenHandler.open(sp, npcId);
    }

    /** Attempt to buy the offer with the given id for the player. */
    public static void buy(ServerPlayerEntity sp, String offerId) {
        if (!enabled) return;
        CosmeticOffer offer = CosmeticRegistry.get(offerId);
        if (offer == null) {
            sp.sendMessage(Text.literal("That cosmetic is no longer available.").formatted(Formatting.RED), false);
            return;
        }
        CosmeticState state = CosmeticState.get(sp.getServer());
        if (offer.oneTime() && state.owns(sp.getUuid(), offerId)) {
            sp.sendMessage(Text.literal("You already own that cosmetic.").formatted(Formatting.YELLOW), false);
            return;
        }
        if (BalanceStore.get(sp) < offer.price()) {
            sp.sendMessage(Text.literal("You need " + offer.price() + " coins for that.").formatted(Formatting.RED), false);
            return;
        }

        BalanceStore.subtract(sp, offer.price(), TransactionReason.SINK, "cosmetic: " + offerId);
        NotchPackets.sendBalance(sp, BalanceStore.get(sp));

        if (offer.isCommand()) {
            String cmd = offer.command()
                    .replace("%player%", sp.getName().getString())
                    .replace("%uuid%", sp.getUuid().toString());
            // Run as the server (console) so unlock commands don't need the player to be an op.
            sp.getServer().getCommandManager().executeWithPrefix(sp.getServer().getCommandSource(), cmd);
        } else if (!offer.itemReward().isEmpty()) {
            ItemStack reward = offer.itemReward().copy();
            sp.getInventory().offerOrDrop(reward);
        }

        if (offer.oneTime()) {
            state.markOwned(sp.getUuid(), offerId);
        }

        sp.getWorld().playSound(null, sp.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 0.6f, 1.4f);
        sp.sendMessage(Text.literal("Purchased ").formatted(Formatting.GREEN)
                .append(Text.literal(offer.name()).formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal(" for " + offer.price() + " coins.").formatted(Formatting.GREEN)), false);
    }
}
