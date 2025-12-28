package net.fugginbeenus.notchcurrency.shop;

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

        // Return any stocked items to the owner
        for (ShopListing listing : shop.getListings()) {
            if (listing.getStockQuantity() > 0) {
                ItemStack toReturn = listing.createSaleStack(listing.getStockQuantity());
                giveItemsToPlayer(player, toReturn);
            }
        }

        // Collect any pending earnings
        int pending = shop.collectPendingEarnings();
        if (pending > 0) {
            CurrencyApi.deposit(player, pending);
            player.sendMessage(Text.literal("Collected ")
                    .append(NotchCurrency.coins(pending))
                    .append(" in pending sales!")
                    .formatted(Formatting.GREEN), false);
        }

        state.deleteShop(shopId, player.getUuid());
        player.sendMessage(Text.literal("Shop deleted. All stock has been returned to you.")
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
            owner.sendMessage(Text.literal("Price must be between " + MIN_PRICE + " and " + MAX_PRICE + " coins.")
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
        if (!ItemStack.canCombine(listing.getItemForSale(), items)) {
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
     * Attempt to purchase from a shop using coins.
     * Uses atomic operations to prevent race conditions and duping.
     */
    public static synchronized PurchaseResult purchaseWithCoins(ServerPlayerEntity buyer, UUID shopId, UUID listingId, int quantity) {
        MinecraftServer server = buyer.getServer();
        ShopState state = ShopState.get(server);
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            return PurchaseResult.SHOP_NOT_FOUND;
        }

        if (!shop.isOpen()) {
            return PurchaseResult.SHOP_CLOSED;
        }

        // Can't buy from your own shop
        if (shop.getOwnerId().equals(buyer.getUuid())) {
            return PurchaseResult.OWN_SHOP;
        }

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) {
            return PurchaseResult.LISTING_NOT_FOUND;
        }

        if (!listing.acceptsCoins()) {
            return PurchaseResult.COINS_NOT_ACCEPTED;
        }

        if (quantity <= 0 || quantity > 64) { // Add max quantity limit
            return PurchaseResult.INVALID_QUANTITY;
        }

        // Use atomic tryRemoveStock to prevent race conditions
        // This checks AND removes in one synchronized operation
        if (!listing.tryRemoveStock(quantity)) {
            return PurchaseResult.INSUFFICIENT_STOCK;
        }

        // Stock is now reserved - continue with purchase
        int totalCost = listing.getCoinPrice() * quantity;
        int buyerBalance = CurrencyApi.getBalance(buyer);

        if (buyerBalance < totalCost) {
            // Refund the stock since we can't complete the purchase
            listing.addStock(quantity);
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // Execute the purchase - stock already removed above
        // 1. Charge buyer
        if (!CurrencyApi.tryWithdraw(buyer, totalCost)) {
            // Failed to charge - refund stock
            listing.addStock(quantity);
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // 2. Stock already removed above via tryRemoveStock

        // 3. Give items to buyer
        ItemStack purchased = listing.createSaleStack(quantity);
        giveItemsToPlayer(buyer, purchased);

        // 4. Pay the seller (minus tax)
        int tax = (int) Math.floor(totalCost * SALES_TAX_PERCENT / 100.0);
        int sellerEarnings = totalCost - tax;

        ServerPlayerEntity seller = server.getPlayerManager().getPlayer(shop.getOwnerId());
        if (seller != null) {
            // Seller is online - pay directly
            CurrencyApi.deposit(seller, sellerEarnings);
            seller.sendMessage(Text.literal("")
                    .append(Text.literal(buyer.getName().getString()).formatted(Formatting.AQUA))
                    .append(Text.literal(" bought ").formatted(Formatting.GREEN))
                    .append(Text.literal(quantity + "x ").formatted(Formatting.WHITE))
                    .append(purchased.getName())
                    .append(Text.literal(" from your shop for ").formatted(Formatting.GREEN))
                    .append(NotchCurrency.coins(totalCost)), false);
        } else {
            // Seller is offline - queue earnings and deposit to their balance
            shop.addPendingSale(buyer.getName().getString(), listing.getItemForSale(), quantity, sellerEarnings);
            CurrencyApi.deposit(server, shop.getOwnerId(), sellerEarnings);
        }

        // 5. Update statistics
        listing.recordSale(quantity, totalCost);
        shop.recordSale(sellerEarnings);
        state.markDirtyAndSave();

        // 6. Feedback to buyer - coin/purchase sound
        buyer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.2F);
        buyer.playSound(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5F, 1.0F);
        buyer.sendMessage(Text.literal("Purchased ")
                .append(Text.literal(quantity + "x ").formatted(Formatting.WHITE))
                .append(purchased.getName())
                .append(Text.literal(" for ").formatted(Formatting.GREEN))
                .append(NotchCurrency.coins(totalCost)), false);

        LOGGER.info("{} purchased {}x {} from {}'s shop for {} coins",
                buyer.getName().getString(), quantity,
                purchased.getName().getString(), shop.getOwnerName(), totalCost);

        // 7. Low stock warning to seller
        int remainingStock = listing.getStockQuantitySafe();
        if (remainingStock > 0 && remainingStock <= LOW_STOCK_THRESHOLD) {
            notifyLowStock(server, shop, listing, remainingStock);
        } else if (remainingStock == 0) {
            notifyOutOfStock(server, shop, listing);
        }

        return PurchaseResult.SUCCESS;
    }

    // Low stock threshold - warn when stock falls to this level or below
    private static final int LOW_STOCK_THRESHOLD = 5;

    /**
     * Notify shop owner of low stock.
     */
    private static void notifyLowStock(MinecraftServer server, PlayerShop shop, ShopListing listing, int remaining) {
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(shop.getOwnerId());
        if (owner != null) {
            // Warning sound - note block pling
            owner.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1.0F, 0.8F);
            owner.sendMessage(Text.literal("⚠ Low stock warning: ").formatted(Formatting.YELLOW)
                    .append(listing.getItemForSale().getName())
                    .append(Text.literal(" only has " + remaining + " left!").formatted(Formatting.YELLOW)), false);
        }
    }

    /**
     * Notify shop owner that an item is out of stock.
     */
    private static void notifyOutOfStock(MinecraftServer server, PlayerShop shop, ShopListing listing) {
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(shop.getOwnerId());
        if (owner != null) {
            // Urgent warning sound - anvil land (thunk)
            owner.playSound(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.5F, 1.5F);
            owner.sendMessage(Text.literal("❌ Out of stock: ").formatted(Formatting.RED)
                    .append(listing.getItemForSale().getName())
                    .append(Text.literal(" is now sold out!").formatted(Formatting.RED)), false);
        }
    }

    /**
     * Attempt to purchase from a shop using barter (items).
     */
    public static PurchaseResult purchaseWithBarter(ServerPlayerEntity buyer, UUID shopId, UUID listingId, int quantity) {
        MinecraftServer server = buyer.getServer();
        ShopState state = ShopState.get(server);
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) return PurchaseResult.SHOP_NOT_FOUND;
        if (!shop.isOpen()) return PurchaseResult.SHOP_CLOSED;
        if (shop.getOwnerId().equals(buyer.getUuid())) return PurchaseResult.OWN_SHOP;

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) return PurchaseResult.LISTING_NOT_FOUND;
        if (!listing.acceptsBarter()) return PurchaseResult.BARTER_NOT_ACCEPTED;
        if (quantity <= 0) return PurchaseResult.INVALID_QUANTITY;

        int available = listing.getStockQuantity();
        if (available < quantity) return PurchaseResult.INSUFFICIENT_STOCK;

        // Check if buyer has the required items
        ItemStack required = listing.getItemPrice();
        int requiredTotal = listing.getItemPriceCount() * quantity;

        int buyerHas = countItemsInInventory(buyer, required);
        if (buyerHas < requiredTotal) {
            return PurchaseResult.INSUFFICIENT_ITEMS;
        }

        // Execute the barter
        // 1. Take items from buyer
        removeItemsFromInventory(buyer, required, requiredTotal);

        // 2. Reduce stock
        listing.removeStock(quantity);

        // 3. Give purchased items to buyer
        ItemStack purchased = listing.createSaleStack(quantity);
        giveItemsToPlayer(buyer, purchased);

        // 4. Give bartered items to seller
        ItemStack barterItems = required.copy();
        barterItems.setCount(requiredTotal);

        ServerPlayerEntity seller = server.getPlayerManager().getPlayer(shop.getOwnerId());
        if (seller != null) {
            giveItemsToPlayer(seller, barterItems);
            seller.sendMessage(Text.literal("")
                    .append(Text.literal(buyer.getName().getString()).formatted(Formatting.AQUA))
                    .append(Text.literal(" traded ").formatted(Formatting.GREEN))
                    .append(Text.literal(requiredTotal + "x ").formatted(Formatting.WHITE))
                    .append(required.getName())
                    .append(Text.literal(" for ").formatted(Formatting.GREEN))
                    .append(Text.literal(quantity + "x ").formatted(Formatting.WHITE))
                    .append(purchased.getName()), false);
        } else {
            // TODO: [FUTURE] Queue unsold items for offline player collection
            // For now, items are lost if seller is offline during barter
            // Could use a mailbox system similar to auction house
        }

        // 5. Update statistics
        listing.recordSale(quantity, 0);
        shop.recordSale(0);
        state.markDirtyAndSave();

        // 6. Feedback
        buyer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.2F);
        buyer.sendMessage(Text.literal("Traded ")
                .append(Text.literal(requiredTotal + "x ").formatted(Formatting.WHITE))
                .append(required.getName())
                .append(Text.literal(" for ").formatted(Formatting.GREEN))
                .append(Text.literal(quantity + "x ").formatted(Formatting.WHITE))
                .append(purchased.getName()), false);

        return PurchaseResult.SUCCESS;
    }

    /**
     * Unified purchase method - handles BOTH coin AND barter prices (additive).
     * If a listing has both a coin price and a barter item, buyer must pay BOTH.
     */
    public static PurchaseResult purchase(ServerPlayerEntity buyer, UUID shopId, UUID listingId, int quantity) {
        MinecraftServer server = buyer.getServer();
        ShopState state = ShopState.get(server);
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) return PurchaseResult.SHOP_NOT_FOUND;
        if (!shop.isOpen()) return PurchaseResult.SHOP_CLOSED;
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
            int buyerBalance = CurrencyApi.getBalance(buyer);
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
            CurrencyApi.withdraw(buyer, totalCoinCost);
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
        } else if (needsCoins) {
            shop.addPendingSale(buyer.getName().getString(), listing.getItemForSale(), quantity,
                    totalCoinCost - (int) Math.floor(totalCoinCost * SALES_TAX_PERCENT / 100.0));
        }

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
            if (ItemStack.canCombine(template, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItemsFromInventory(ServerPlayerEntity player, ItemStack template, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (ItemStack.canCombine(template, stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
            }
        }
    }

    /**
     * Collect pending sales earnings for a player.
     */
    public static int collectPendingEarnings(ServerPlayerEntity owner, UUID shopId) {
        ShopState state = ShopState.get(owner.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUuid())) {
            return 0;
        }

        int collected = shop.collectPendingEarnings();
        if (collected > 0) {
            state.markDirtyAndSave();
            owner.sendMessage(Text.literal("Collected ")
                    .append(NotchCurrency.coins(collected))
                    .append(Text.literal(" from pending sales!").formatted(Formatting.GREEN)), false);
        }
        return collected;
    }

    /**
     * Return all shop contents (items and currency) to the owner.
     * Used when admin deletes a shop or for cleanup.
     */
    public static void returnAllShopContents(MinecraftServer server, PlayerShop shop, ServerPlayerEntity owner) {
        int totalCurrency = 0;
        List<ItemStack> itemsToReturn = new java.util.ArrayList<>();

        // Collect pending balance
        totalCurrency += (int) shop.getPendingBalance();

        // Collect pending sale earnings
        totalCurrency += shop.collectPendingEarnings();

        // Collect pending barter items
        itemsToReturn.addAll(shop.collectPendingBarterItems());

        // Collect all stock from listings
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
            }
        }

        // Give currency to owner's balance
        if (totalCurrency > 0) {
            net.fugginbeenus.notchcurrency.api.CurrencyApi.deposit(server, shop.getOwnerId(), totalCurrency);
        }

        // Give items to owner if online, otherwise they're lost (could queue for later)
        if (owner != null) {
            for (ItemStack item : itemsToReturn) {
                if (!item.isEmpty()) {
                    if (!owner.getInventory().insertStack(item.copy())) {
                        owner.dropItem(item.copy(), false);
                    }
                }
            }

            if (totalCurrency > 0 || !itemsToReturn.isEmpty()) {
                owner.sendMessage(Text.literal("Your shop was closed. Returned: " + totalCurrency + " coins and " +
                        itemsToReturn.size() + " item stacks.").formatted(Formatting.YELLOW), false);
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