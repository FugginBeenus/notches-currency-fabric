package net.fugginbeenus.notchcurrency.shop;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * Handles death/removal of shopkeeper NPCs.
 * Warns owners about unclaimed items when their shopkeeper is destroyed.
 */
public class ShopkeeperDeathHandler {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ShopkeeperDeathHandler::onEntityDeath);
    }

    private static void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        // Check if this is our ShopkeeperEntity
        if (!(entity instanceof ShopkeeperEntity)) {
            return;
        }

        UUID npcId = entity.getUuid();
        ShopState state = ShopState.get(serverWorld);
        PlayerShop shop = state.getShopByNpc(npcId);

        if (shop == null) {
            // Not a linked shopkeeper
            return;
        }

        // This was a shopkeeper - warn the owner
        UUID ownerId = shop.getOwnerId();
        MinecraftServer server = serverWorld.getServer();
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);

        // Unlink the NPC from the shop (but keep the shop data for claiming)
        shop.setLinkedNpcId(null);
        state.markDirtyAndSave();

        // Calculate what's in the shop
        long pendingBalance = shop.getPendingBalance();
        int listingCount = shop.getListings().size();
        int totalStock = shop.getListings().stream()
                .mapToInt(ShopListing::getStockQuantity)
                .sum();
        boolean hasBarterItems = shop.hasPendingBarterItems();

        boolean hasAnything = pendingBalance > 0 || totalStock > 0 || hasBarterItems;

        if (owner != null) {
            // Owner is online - send them a message
            sendDeathWarning(owner, shop, pendingBalance, listingCount, totalStock, hasBarterItems);
        } else {
            // Owner is offline - they'll see it when they try to access the shop
            // The shop data is preserved, they just need to claim it
        }
    }

    private static void sendDeathWarning(ServerPlayerEntity owner, PlayerShop shop,
                                         long pendingBalance, int listingCount, int totalStock, boolean hasBarterItems) {

        owner.sendMessage(Text.literal(""), false); // Empty line
        owner.sendMessage(Text.literal("⚠ Your shopkeeper was destroyed!").formatted(Formatting.RED, Formatting.BOLD), false);

        if (pendingBalance > 0 || totalStock > 0 || hasBarterItems) {
            owner.sendMessage(Text.literal("Your shop still contains:").formatted(Formatting.YELLOW), false);

            if (pendingBalance > 0) {
                owner.sendMessage(Text.literal("  • " + pendingBalance + " coins").formatted(Formatting.GOLD), false);
            }
            if (totalStock > 0) {
                owner.sendMessage(Text.literal("  • " + totalStock + " items in stock (" + listingCount + " listings)").formatted(Formatting.AQUA), false);
            }
            if (hasBarterItems) {
                owner.sendMessage(Text.literal("  • Pending barter items").formatted(Formatting.GREEN), false);
            }

            // Create clickable command
            String claimCommand = "/notchcurrency shop claim " + shop.getShopId().toString();
            MutableText claimText = Text.literal("[Click here to claim your items]")
                    .formatted(Formatting.GREEN, Formatting.UNDERLINE)
                    .styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, claimCommand))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to run: " + claimCommand))));

            owner.sendMessage(Text.literal(""), false);
            owner.sendMessage(claimText, false);
            owner.sendMessage(Text.literal("Or use: ").formatted(Formatting.GRAY)
                    .append(Text.literal(claimCommand).formatted(Formatting.WHITE)), false);
        } else {
            owner.sendMessage(Text.literal("Your shop was empty - no items to claim.").formatted(Formatting.GRAY), false);
        }

        owner.sendMessage(Text.literal(""), false); // Empty line
    }

    /**
     * Called when a player tries to claim items from a destroyed shop
     */
    public static boolean claimShopItems(ServerPlayerEntity player, UUID shopId) {
        ServerWorld world = player.getServerWorld();
        ShopState state = ShopState.get(world);
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            player.sendMessage(Text.literal("Shop not found!").formatted(Formatting.RED), false);
            return false;
        }

        if (!shop.getOwnerId().equals(player.getUuid())) {
            player.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
            return false;
        }

        // Withdraw coins
        long coins = shop.withdrawBalance();
        if (coins > 0) {
            net.fugginbeenus.notchcurrency.api.CurrencyApi.deposit(player, (int) coins);
            player.sendMessage(Text.literal("Withdrew " + coins + " coins!").formatted(Formatting.GOLD), false);
        }

        // Give barter items
        java.util.List<net.minecraft.item.ItemStack> barterItems = shop.collectPendingBarterItems();
        int barterCount = 0;
        for (net.minecraft.item.ItemStack item : barterItems) {
            if (!item.isEmpty()) {
                barterCount += item.getCount();
                giveItemsToPlayer(player, item);
            }
        }
        if (barterCount > 0) {
            player.sendMessage(Text.literal("Returned " + barterCount + " barter items!").formatted(Formatting.AQUA), false);
        }

        // Return stock from all listings
        int stockCount = 0;
        for (ShopListing listing : shop.getListings()) {
            int stock = listing.getStockQuantity();
            if (stock > 0) {
                net.minecraft.item.ItemStack returnItems = listing.getItemForSale().copy();
                returnItems.setCount(stock);
                stockCount += stock;
                giveItemsToPlayer(player, returnItems);
                listing.removeStock(stock);
            }
        }
        if (stockCount > 0) {
            player.sendMessage(Text.literal("Returned " + stockCount + " stock items!").formatted(Formatting.GREEN), false);
        }

        // Clear shop listings and optionally delete the shop
        shop.getListings().clear();

        if (coins == 0 && barterCount == 0 && stockCount == 0) {
            player.sendMessage(Text.literal("Shop was already empty.").formatted(Formatting.GRAY), false);
        } else {
            player.sendMessage(Text.literal("All items claimed! You can create a new shopkeeper.").formatted(Formatting.GREEN), false);
        }

        // Remove the shop from state
        state.removeShop(shopId);
        state.markDirtyAndSave();

        return true;
    }

    private static void giveItemsToPlayer(ServerPlayerEntity player, net.minecraft.item.ItemStack items) {
        int remaining = items.getCount();
        while (remaining > 0) {
            int giveCount = Math.min(remaining, items.getMaxCount());
            net.minecraft.item.ItemStack toGive = items.copy();
            toGive.setCount(giveCount);
            if (!player.getInventory().insertStack(toGive)) {
                player.dropItem(toGive, false);
            }
            remaining -= giveCount;
        }
    }
}