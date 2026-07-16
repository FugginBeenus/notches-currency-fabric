package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for player shop operations.
 * Handles purchases, stock management, and owner payouts.
 */
public final class PlayerShopManager {

    private static final Logger LOGGER = LogManager.getLogger("NotchCurrency-PlayerShopManager");

    // Configuration
    public static int MAX_SHOPS_PER_PLAYER = 3;
    public static double SALES_TAX_PERCENT = 0.0;  // Tax on sales (goes nowhere, just removed)
    public static int MIN_PRICE = 0;  // 0 allowed for barter-only listings
    public static int MAX_PRICE = 1_000_000;

    private PlayerShopManager() {}

    // --- Shop Creation ---

    /**
     * Creates a new shop for a player.
     */
    @Nullable
    public static PlayerShop createShop(ServerPlayerEntity player, String shopName) {
        ShopState state = ShopState.get(player.getServerWorld());

        PlayerShop shop = state.createShop(
                player.getUuid(),
                player.getName().getString(),
                shopName,
                MAX_SHOPS_PER_PLAYER
        );

        if (shop == null) {
            player.sendMessage(Text.literal("You've reached the maximum number of shops (" + MAX_SHOPS_PER_PLAYER + ")!")
                    .formatted(Formatting.RED), false);
            return null;
        }

        player.sendMessage(Text.literal("Created shop: ")
                .append(Text.literal(shopName).formatted(Formatting.GOLD))
                .formatted(Formatting.GREEN), false);

        return shop;
    }

    /**
     * Deletes a player's shop.
     */
    public static boolean deleteShop(ServerPlayerEntity player, UUID shopId) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            player.sendMessage(Text.literal("Shop not found!").formatted(Formatting.RED), false);
            return false;
        }

        if (!shop.getOwnerId().equals(player.getUuid())) {
            player.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
            return false;
        }

        // Return all stock, pending coins, and barter items via the single canonical path
        returnAllShopContents(player.getServer(), shop, player);

        state.deleteShop(shopId, player.getUuid());
        player.sendMessage(Text.literal("Shop deleted. Everything has been returned to you.")
                .formatted(Formatting.YELLOW), false);

        return true;
    }

    // --- Listing Management ---

    /**
     * Adds a new listing to a shop.
     * The item is taken from the player's inventory as initial stock.
     */
    public static boolean addListing(ServerPlayerEntity owner, UUID shopId, ItemStack item, int coinPrice) {
        ShopState state = ShopState.get(owner.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUuid())) {
            owner.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
            return false;
        }

        if (!shop.canAddListing()) {
            owner.sendMessage(Text.literal("Shop is full! Maximum " + PlayerShop.MAX_LISTINGS + " listings.")
                    .formatted(Formatting.RED), false);
            return false;
        }

        if (item == null || item.isEmpty()) {
            owner.sendMessage(Text.literal("Invalid item!").formatted(Formatting.RED), false);
            return false;
        }

        if (coinPrice < MIN_PRICE || coinPrice > MAX_PRICE) {
            owner.sendMessage(Text.literal("Price must be between " + MIN_PRICE + " and " + MAX_PRICE + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".")
                    .formatted(Formatting.RED), false);
            return false;
        }

        // Create template (1 item) and track stock from the provided count
        ItemStack template = item.copy();
        int stockAmount = template.getCount();
        template.setCount(1);

        ShopListing listing = new ShopListing(template, stockAmount, coinPrice);
        shop.addListing(listing);
        state.markDirtyAndSave();

        owner.sendMessage(Text.literal("Listed ")
                .append(Text.literal(stockAmount + "x ").formatted(Formatting.WHITE))
                .append(item.getName())
                .append(Text.literal(" for ").formatted(Formatting.GREEN))
                .append(NotchCurrency.coins(coinPrice))
                .append(Text.literal(" each").formatted(Formatting.GREEN)), false);

        return true;
    }

    /**
     * Adds stock to an existing listing.
     */
    public static boolean addStock(ServerPlayerEntity owner, UUID shopId, UUID listingId, ItemStack items) {
        ShopState state = ShopState.get(owner.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUuid())) {
            owner.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
            return false;
        }

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) {
            owner.sendMessage(Text.literal("Listing not found!").formatted(Formatting.RED), false);
            return false;
        }

        // Verify item matches
        if (!StackData.canCombine(listing.getItemForSale(), items)) {
            owner.sendMessage(Text.literal("Item doesn't match the listing!").formatted(Formatting.RED), false);
            return false;
        }

        int addAmount = items.getCount();
        listing.addStock(addAmount);
        state.markDirtyAndSave();

        owner.sendMessage(Text.literal("Added ")
                .append(Text.literal(addAmount + "x ").formatted(Formatting.WHITE))
                .append(items.getName())
                .append(Text.literal(" to stock. Total: " + listing.getStockQuantity()).formatted(Formatting.GREEN)), false);

        return true;
    }

    /**
     * Updates the price of a listing.
     */
    public static boolean updatePrice(ServerPlayerEntity owner, UUID shopId, UUID listingId, int newPrice) {
        ShopState state = ShopState.get(owner.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUuid())) {
            return false;
        }

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) {
            return false;
        }

        if (newPrice < MIN_PRICE || newPrice > MAX_PRICE) {
            owner.sendMessage(Text.literal("Price must be between " + MIN_PRICE + " and " + MAX_PRICE + ".")
                    .formatted(Formatting.RED), false);
            return false;
        }

        listing.setCoinPrice(newPrice);
        state.markDirtyAndSave();

        owner.sendMessage(Text.literal("Price updated to ")
                .append(NotchCurrency.coins(newPrice))
                .formatted(Formatting.GREEN), false);

        return true;
    }

    /**
     * Sets a barter (item) price for a listing.
     */
    public static boolean setBarterPrice(ServerPlayerEntity owner, UUID shopId, UUID listingId,
                                         ItemStack requiredItem, int requiredCount) {
        ShopState state = ShopState.get(owner.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUuid())) {
            return false;
        }

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) {
            return false;
        }

        listing.setBarterPrice(requiredItem, requiredCount);
        state.markDirtyAndSave();

        if (requiredItem == null || requiredItem.isEmpty()) {
            owner.sendMessage(Text.literal("Barter price removed.").formatted(Formatting.YELLOW), false);
        } else {
            owner.sendMessage(Text.literal("Barter price set: ")
                    .append(Text.literal(requiredCount + "x ").formatted(Formatting.WHITE))
                    .append(requiredItem.getName())
                    .formatted(Formatting.GREEN), false);
        }

        return true;
    }

    /**
     * Removes a listing and returns remaining stock to owner.
     */
    public static boolean removeListing(ServerPlayerEntity owner, UUID shopId, UUID listingId) {
        ShopState state = ShopState.get(owner.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUuid())) {
            return false;
        }

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) {
            return false;
        }

        // Return stock
        if (listing.getStockQuantity() > 0) {
            ItemStack toReturn = listing.createSaleStack(listing.getStockQuantity());
            giveItemsToPlayer(owner, toReturn);
        }

        shop.removeListing(listingId);
        state.markDirtyAndSave();

        owner.sendMessage(Text.literal("Listing removed. Stock returned.").formatted(Formatting.YELLOW), false);
        return true;
    }

    // --- Purchasing ---

    /**
     * Unified purchase method - handles BOTH coin AND barter prices (additive).
     * If a listing has both a coin price and a barter item, buyer must pay BOTH.
     */
    public static PurchaseResult purchase(ServerPlayerEntity buyer, UUID shopId, UUID listingId, int quantity) {
        MinecraftServer server = buyer.getServer();
        ShopState state = ShopState.get(server);
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) return PurchaseResult.SHOP_NOT_FOUND;
        if (!shop.isOpen() || shop.isRentPaused()) return PurchaseResult.SHOP_CLOSED;
        if (shop.getOwnerId().equals(buyer.getUuid())) return PurchaseResult.OWN_SHOP;

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) return PurchaseResult.LISTING_NOT_FOUND;
        if (quantity <= 0) return PurchaseResult.INVALID_QUANTITY;

        // Quick check (actual atomic check happens later)
        int available = listing.getStockQuantity();
        if (available < quantity) return PurchaseResult.INSUFFICIENT_STOCK;

        // Check what payment is required
        boolean needsCoins = listing.acceptsCoins() && listing.getCoinPrice() > 0;
        boolean needsBarter = listing.acceptsBarter();

        // Validate buyer has required resources
        int totalCoinCost = 0;
        int totalBarterCost = 0;
        ItemStack barterItem = ItemStack.EMPTY;

        if (needsCoins) {
            totalCoinCost = listing.getCoinPrice() * quantity;
            long buyerBalance = CurrencyApi.getBalance(buyer);
            if (buyerBalance < totalCoinCost) {
                return PurchaseResult.INSUFFICIENT_FUNDS;
            }
        }

        if (needsBarter) {
            barterItem = listing.getItemPrice();
            totalBarterCost = listing.getItemPriceCount() * quantity;
            int buyerHas = countItemsInInventory(buyer, barterItem);
            if (buyerHas < totalBarterCost) {
                return PurchaseResult.INSUFFICIENT_ITEMS;
            }
        }

        // ATOMIC: Try to remove stock first (prevents race condition with two buyers)
        if (!listing.tryRemoveStock(quantity)) {
            return PurchaseResult.INSUFFICIENT_STOCK;
        }

        // Stock removed successfully, now take payment
        if (needsCoins) {
            CurrencyApi.withdraw(buyer, totalCoinCost,
                    net.fugginbeenus.notchcurrency.economy.TransactionReason.SHOP_SALE, "shop purchase");
        }
        if (needsBarter) {
            removeItemsFromInventory(buyer, barterItem, totalBarterCost);
        }

        // Give items to buyer
        ItemStack purchased = listing.createSaleStack(quantity);
        giveItemsToPlayer(buyer, purchased);

        // Handle earnings - add to shop's pending balance (NOT directly to seller)
        ServerPlayerEntity seller = server.getPlayerManager().getPlayer(shop.getOwnerId());

        // Coin earnings (with tax) - added to shop's pending balance via recordSale
        int sellerEarnings = 0;
        if (needsCoins && totalCoinCost > 0) {
            int tax = (int) Math.floor(totalCoinCost * SALES_TAX_PERCENT / 100.0);
            sellerEarnings = totalCoinCost - tax;
            // recordSale adds to pendingBalance - owner must withdraw manually
            shop.recordSale(sellerEarnings);
        }

        // Barter items - always store for later withdrawal (better UX)
        if (needsBarter && totalBarterCost > 0) {
            ItemStack barterPayment = barterItem.copy();
            barterPayment.setCount(totalBarterCost);
            shop.addPendingBarterItem(barterPayment);
        }

        // Notify seller
        if (seller != null) {
            MutableText message = Text.literal("")
                    .append(Text.literal(buyer.getName().getString()).formatted(Formatting.AQUA))
                    .append(Text.literal(" bought ").formatted(Formatting.GREEN))
                    .append(Text.literal(quantity + "x ").formatted(Formatting.WHITE))
                    .append(purchased.getName())
                    .append(Text.literal(" for ").formatted(Formatting.GREEN));

            if (needsCoins && needsBarter) {
                message.append(NotchCurrency.coins(totalCoinCost))
                        .append(Text.literal(" + ").formatted(Formatting.WHITE))
                        .append(Text.literal(totalBarterCost + "x ").formatted(Formatting.WHITE))
                        .append(barterItem.getName());
            } else if (needsCoins) {
                message.append(NotchCurrency.coins(totalCoinCost));
            } else if (needsBarter) {
                message.append(Text.literal(totalBarterCost + "x ").formatted(Formatting.WHITE))
                        .append(barterItem.getName());
            }

            seller.sendMessage(message, false);
        }
        // (Offline owners: coins are already held in the shop's pending balance via
        //  recordSale() above, and are paid out when the owner withdraws or the shop closes.)

        // Update statistics
        listing.recordSale(quantity, totalCoinCost);
        state.markDirtyAndSave();

        // Feedback to buyer
        buyer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.2F);

        MutableText buyerMessage = Text.literal("Purchased ")
                .append(Text.literal(quantity + "x ").formatted(Formatting.WHITE))
                .append(purchased.getName())
                .append(Text.literal(" for ").formatted(Formatting.GREEN));

        if (needsCoins && needsBarter) {
            buyerMessage.append(NotchCurrency.coins(totalCoinCost))
                    .append(Text.literal(" + ").formatted(Formatting.WHITE))
                    .append(Text.literal(totalBarterCost + "x ").formatted(Formatting.WHITE))
                    .append(barterItem.getName());
        } else if (needsCoins) {
            buyerMessage.append(NotchCurrency.coins(totalCoinCost));
        } else if (needsBarter) {
            buyerMessage.append(Text.literal(totalBarterCost + "x ").formatted(Formatting.WHITE))
                    .append(barterItem.getName());
        }

        buyer.sendMessage(buyerMessage, false);

        LOGGER.info("{} purchased {}x {} from {}'s shop",
                buyer.getName().getString(), quantity,
                purchased.getName().getString(), shop.getOwnerName());

        return PurchaseResult.SUCCESS;
    }

    // --- Utility Methods ---

    private static void giveItemsToPlayer(ServerPlayerEntity player, ItemStack items) {
        if (items.isEmpty()) return;

        // Split into max stack sizes
        int remaining = items.getCount();
        while (remaining > 0) {
            int giveCount = Math.min(remaining, items.getMaxCount());
            ItemStack toGive = items.copy();
            toGive.setCount(giveCount);

            if (!player.getInventory().insertStack(toGive)) {
                player.dropItem(toGive, false);
            }
            remaining -= giveCount;
        }
    }

    private static int countItemsInInventory(ServerPlayerEntity player, ItemStack template) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (StackData.canCombine(template, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItemsFromInventory(ServerPlayerEntity player, ItemStack template, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (StackData.canCombine(template, stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
            }
        }
    }


    /**
     * Return ALL of a shop's contents to its owner: pending coin balance, pending
     * barter items, and every listing's remaining stock.
     *
     * This is the single canonical path used by /shop delete, admin delete, shopkeeper
     * death, and NPC deletion, so that coins/items can never be lost or duplicated by
     * different routes handling the pending stores differently.
     *
     * Coins are paid to the owner's account by UUID (works while offline); items go to
     * the owner's inventory only if they are currently online.
     */
    public static void returnAllShopContents(MinecraftServer server, PlayerShop shop, @Nullable ServerPlayerEntity owner) {
        // Pending coin balance is the single source of truth for shop earnings.
        long totalCurrency = shop.withdrawBalance();

        // Pending barter items.
        List<ItemStack> itemsToReturn = new java.util.ArrayList<>(shop.collectPendingBarterItems());

        // All remaining listing stock (cleared so it can never be returned twice).
        for (ShopListing listing : shop.getListings()) {
            int stock = listing.getStockQuantitySafe();
            if (stock > 0) {
                ItemStack baseItem = listing.getItemForSale();
                int maxStackSize = baseItem.getMaxCount();
                while (stock > 0) {
                    int stackSize = Math.min(stock, maxStackSize);
                    ItemStack returnStack = baseItem.copy();
                    returnStack.setCount(stackSize);
                    itemsToReturn.add(returnStack);
                    stock -= stackSize;
                }
                listing.setStock(0);
            }
        }

        // Pay coins to the owner's account (by UUID so it works while offline).
        if (totalCurrency > 0) {
            CurrencyApi.deposit(server, shop.getOwnerId(), totalCurrency,
                    net.fugginbeenus.notchcurrency.economy.TransactionReason.SHOP_PAYOUT, "shop closed/returned");
        }

        // Hand items to the owner if they're online.
        if (owner != null) {
            for (ItemStack item : itemsToReturn) {
                giveItemsToPlayer(owner, item);
            }
            if (totalCurrency > 0 || !itemsToReturn.isEmpty()) {
                owner.sendMessage(Text.literal("Returned ")
                        .append(NotchCurrency.coins(totalCurrency))
                        .append(Text.literal(" and " + itemsToReturn.size() + " item stack(s) from your shop.")
                                .formatted(Formatting.YELLOW)), false);
            }
        }

        LOGGER.info("Returned {} coins and {} item stacks from shop {} to owner {}",
                totalCurrency, itemsToReturn.size(), shop.getShopId(), shop.getOwnerName());
    }

    /**
     * Transfer shop ownership to another player.
     */
    public static boolean transferOwnership(MinecraftServer server, UUID shopId, UUID newOwnerId, String newOwnerName) {
        ShopState state = ShopState.get(server);
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            return false;
        }

        // Update ownership
        UUID oldOwnerId = shop.getOwnerId();
        String oldOwnerName = shop.getOwnerName();

        // Use reflection or add a setter - for now we'll recreate the ownership tracking
        // This is a bit hacky but works
        state.updateShopOwnership(shopId, newOwnerId, newOwnerName);
        state.markDirtyAndSave();

        LOGGER.info("Transferred shop {} from {} to {}", shopId, oldOwnerName, newOwnerName);
        return true;
    }

    // --- Result Enum ---

    public enum PurchaseResult {
        SUCCESS,
        SHOP_NOT_FOUND,
        SHOP_CLOSED,
        OWN_SHOP,
        LISTING_NOT_FOUND,
        COINS_NOT_ACCEPTED,
        BARTER_NOT_ACCEPTED,
        INVALID_QUANTITY,
        INSUFFICIENT_STOCK,
        INSUFFICIENT_FUNDS,
        INSUFFICIENT_ITEMS
    }
}