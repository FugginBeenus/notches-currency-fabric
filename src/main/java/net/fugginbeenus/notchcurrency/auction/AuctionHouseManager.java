package net.fugginbeenus.notchcurrency.auction;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Thin helper over AuctionState for now.
 * Handles turning the item in the player's hand into an AuctionListing.
 */
public final class AuctionHouseManager {

    private AuctionHouseManager() {}

    /**
     * Creates an auction listing from the item in the player's main hand.
     *
     * @param player   seller
     * @param price    price in Notch currency
     * @param category simple category string ("other", "blocks", etc.)
     * @return the created AuctionListing, or null if something failed
     */
    public static AuctionListing createListing(ServerPlayerEntity player, int price, String category) {
        if (price <= 0) {
            return null;
        }

        ItemStack hand = player.getMainHandStack();
        if (hand.isEmpty()) {
            return null;
        }

        ServerWorld world = player.getServerWorld();
        AuctionState state = AuctionState.get(world);

        // Copy stack, remove from player
        ItemStack listingStack = hand.copy();
        hand.decrement(listingStack.getCount());

        // Delegate to the new AuctionState API
        return state.addListing(world, player, listingStack, price, category);
    }
}
