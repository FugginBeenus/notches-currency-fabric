package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class PlayerShopManager {

    private static final Logger LOGGER = LogManager.getLogger("NotchCurrency-PlayerShopManager");

    // Configuration
    public static int MAX_SHOPS_PER_PLAYER = 3;
    public static double SALES_TAX_PERCENT = 0.0;  // Tax on sales (goes nowhere, just removed)
    public static int MIN_PRICE = 0;  // 0 allowed for barter-only listings
    public static int MAX_PRICE = 1_000_000;

    private PlayerShopManager() {}

    // --- Shop Creation ---

    @Nullable
    public static PlayerShop createShop(ServerPlayer player, String shopName) {
        ShopState state = ShopState.get(player.serverLevel());

        PlayerShop shop = state.createShop(
                player.getUUID(),
                player.getName().getString(),
                shopName,
                MAX_SHOPS_PER_PLAYER
        );

        if (shop == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You've reached the maximum number of shops (" + MAX_SHOPS_PER_PLAYER + ")!")
                    .withStyle(ChatFormatting.RED));
            return null;
        }

        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Created shop: ")
                .append(Component.literal(shopName).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GREEN));

        return shop;
    }

    public static boolean deleteShop(ServerPlayer player, UUID shopId) {
        ShopState state = ShopState.get(player.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Shop not found!").withStyle(ChatFormatting.RED));
            return false;
        }

        if (!shop.getOwnerId().equals(player.getUUID())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You don't own this shop!").withStyle(ChatFormatting.RED));
            return false;
        }

        // Return all stock, pending coins, and barter items via the single canonical path
        returnAllShopContents(player.level().getServer(), shop, player);

        state.deleteShop(shopId, player.getUUID());
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Shop deleted. Everything has been returned to you.")
                .withStyle(ChatFormatting.YELLOW));

        return true;
    }

    // --- Listing Management ---

    public static boolean addListing(ServerPlayer owner, UUID shopId, ItemStack item, int coinPrice) {
        ShopState state = ShopState.get(owner.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUUID())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("You don't own this shop!").withStyle(ChatFormatting.RED));
            return false;
        }

        if (!shop.canAddListing()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Shop is full! Maximum " + PlayerShop.MAX_LISTINGS + " listings.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (item == null || item.isEmpty()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Invalid item!").withStyle(ChatFormatting.RED));
            return false;
        }

        if (coinPrice < MIN_PRICE || coinPrice > MAX_PRICE) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Price must be between " + MIN_PRICE + " and " + MAX_PRICE + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // Create template (1 item) and track stock from the provided count
        ItemStack template = item.copy();
        int stockAmount = template.getCount();
        template.setCount(1);

        ShopListing listing = new ShopListing(template, stockAmount, coinPrice);
        shop.addListing(listing);
        state.markDirtyAndSave();

        net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Listed ")
                .append(Component.literal(stockAmount + "x ").withStyle(ChatFormatting.WHITE))
                .append(item.getHoverName())
                .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN))
                .append(NotchCurrency.coins(coinPrice))
                .append(Component.literal(" each").withStyle(ChatFormatting.GREEN)));

        return true;
    }

    public static boolean addStock(ServerPlayer owner, UUID shopId, UUID listingId, ItemStack items) {
        ShopState state = ShopState.get(owner.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUUID())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("You don't own this shop!").withStyle(ChatFormatting.RED));
            return false;
        }

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Listing not found!").withStyle(ChatFormatting.RED));
            return false;
        }

        // Verify item matches
        if (!StackData.canCombine(listing.getItemForSale(), items)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Item doesn't match the listing!").withStyle(ChatFormatting.RED));
            return false;
        }

        int addAmount = items.getCount();
        listing.addStock(addAmount);
        state.markDirtyAndSave();

        net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Added ")
                .append(Component.literal(addAmount + "x ").withStyle(ChatFormatting.WHITE))
                .append(items.getHoverName())
                .append(Component.literal(" to stock. Total: " + listing.getStockQuantity()).withStyle(ChatFormatting.GREEN)));

        return true;
    }

    public static boolean updatePrice(ServerPlayer owner, UUID shopId, UUID listingId, int newPrice) {
        ShopState state = ShopState.get(owner.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUUID())) {
            return false;
        }

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) {
            return false;
        }

        if (newPrice < MIN_PRICE || newPrice > MAX_PRICE) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Price must be between " + MIN_PRICE + " and " + MAX_PRICE + ".")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        listing.setCoinPrice(newPrice);
        state.markDirtyAndSave();

        net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Price updated to ")
                .append(NotchCurrency.coins(newPrice))
                .withStyle(ChatFormatting.GREEN));

        return true;
    }

    public static boolean setBarterPrice(ServerPlayer owner, UUID shopId, UUID listingId,
                                         ItemStack requiredItem, int requiredCount) {
        ShopState state = ShopState.get(owner.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUUID())) {
            return false;
        }

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) {
            return false;
        }

        listing.setBarterPrice(requiredItem, requiredCount);
        state.markDirtyAndSave();

        if (requiredItem == null || requiredItem.isEmpty()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Barter price removed.").withStyle(ChatFormatting.YELLOW));
        } else {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Barter price set: ")
                    .append(Component.literal(requiredCount + "x ").withStyle(ChatFormatting.WHITE))
                    .append(requiredItem.getHoverName())
                    .withStyle(ChatFormatting.GREEN));
        }

        return true;
    }

    public static boolean removeListing(ServerPlayer owner, UUID shopId, UUID listingId) {
        ShopState state = ShopState.get(owner.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(owner.getUUID())) {
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

        net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Listing removed. Stock returned.").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    // --- Purchasing ---

    public static PurchaseResult purchase(ServerPlayer buyer, UUID shopId, UUID listingId, int quantity) {
        MinecraftServer server = buyer.level().getServer();
        ShopState state = ShopState.get(server);
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) return PurchaseResult.SHOP_NOT_FOUND;
        if (!shop.isOpen() || shop.isRentPaused()) return PurchaseResult.SHOP_CLOSED;
        if (shop.getOwnerId().equals(buyer.getUUID())) return PurchaseResult.OWN_SHOP;

        ShopListing listing = shop.getListing(listingId);
        if (listing == null) return PurchaseResult.LISTING_NOT_FOUND;
        if (quantity <= 0) return PurchaseResult.INVALID_QUANTITY;

        // A listing sells the whole stack the owner put up: 32 sculk sensors for 15 coins sells all
        // 32 for 15, so `quantity` counts those bundles. Prices are per bundle; stock is in items.
        int totalItems = listing.getBundleSize() * quantity;

        // Quick check (actual atomic check happens later)
        int available = listing.getStockQuantity();
        if (available < totalItems) return PurchaseResult.INSUFFICIENT_STOCK;

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
        if (!listing.tryRemoveStock(totalItems)) {
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
        ItemStack purchased = listing.createSaleStack(totalItems);
        giveItemsToPlayer(buyer, purchased);

        // Handle earnings - add to shop's pending balance (NOT directly to seller)
        ServerPlayer seller = server.getPlayerList().getPlayer(shop.getOwnerId());

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
            MutableComponent message = Component.literal("")
                    .append(Component.literal(buyer.getName().getString()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" bought ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(totalItems + "x ").withStyle(ChatFormatting.WHITE))
                    .append(purchased.getHoverName())
                    .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN));

            if (needsCoins && needsBarter) {
                message.append(NotchCurrency.coins(totalCoinCost))
                        .append(Component.literal(" + ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(totalBarterCost + "x ").withStyle(ChatFormatting.WHITE))
                        .append(barterItem.getHoverName());
            } else if (needsCoins) {
                message.append(NotchCurrency.coins(totalCoinCost));
            } else if (needsBarter) {
                message.append(Component.literal(totalBarterCost + "x ").withStyle(ChatFormatting.WHITE))
                        .append(barterItem.getHoverName());
            }

            net.fugginbeenus.notchcurrency.compat.Msg.chat(seller, message);
        }
        // (Offline owners: coins are already held in the shop's pending balance via
        //  recordSale() above, and are paid out when the owner withdraws or the shop closes.)

        // Update statistics
        listing.recordSale(totalItems, totalCoinCost); // totalSold counts units, matching stock
        state.markDirtyAndSave();

        // Feedback to buyer
        //? if >=1.21 {
        /*buyer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
        *///?} else {
        buyer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
        //?}

        MutableComponent buyerMessage = Component.literal("Purchased ")
                .append(Component.literal(totalItems + "x ").withStyle(ChatFormatting.WHITE))
                .append(purchased.getHoverName())
                .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN));

        if (needsCoins && needsBarter) {
            buyerMessage.append(NotchCurrency.coins(totalCoinCost))
                    .append(Component.literal(" + ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(totalBarterCost + "x ").withStyle(ChatFormatting.WHITE))
                    .append(barterItem.getHoverName());
        } else if (needsCoins) {
            buyerMessage.append(NotchCurrency.coins(totalCoinCost));
        } else if (needsBarter) {
            buyerMessage.append(Component.literal(totalBarterCost + "x ").withStyle(ChatFormatting.WHITE))
                    .append(barterItem.getHoverName());
        }

        net.fugginbeenus.notchcurrency.compat.Msg.chat(buyer, buyerMessage);

        LOGGER.info("{} purchased {}x {} from {}'s shop",
                buyer.getName().getString(), totalItems,
                purchased.getHoverName().getString(), shop.getOwnerName());

        return PurchaseResult.SUCCESS;
    }

    // --- Utility Methods ---

    private static void giveItemsToPlayer(ServerPlayer player, ItemStack items) {
        if (items.isEmpty()) return;

        // Split into max stack sizes
        int remaining = items.getCount();
        while (remaining > 0) {
            int giveCount = Math.min(remaining, items.getMaxStackSize());
            ItemStack toGive = items.copy();
            toGive.setCount(giveCount);

            if (!player.getInventory().add(toGive)) {
                player.drop(toGive, false);
            }
            remaining -= giveCount;
        }
    }

    private static int countItemsInInventory(ServerPlayer player, ItemStack template) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (StackData.canCombine(template, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItemsFromInventory(ServerPlayer player, ItemStack template, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (StackData.canCombine(template, stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    public static void returnAllShopContents(MinecraftServer server, PlayerShop shop, @Nullable ServerPlayer owner) {
        // Pending coin balance is the single source of truth for shop earnings.
        long totalCurrency = shop.withdrawBalance();

        // Pending barter items.
        List<ItemStack> itemsToReturn = new java.util.ArrayList<>(shop.collectPendingBarterItems());

        // All remaining listing stock (cleared so it can never be returned twice).
        for (ShopListing listing : shop.getListings()) {
            int stock = listing.getStockQuantitySafe();
            if (stock > 0) {
                ItemStack baseItem = listing.getItemForSale();
                int maxStackSize = baseItem.getMaxStackSize();
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
                net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Returned ")
                        .append(NotchCurrency.coins(totalCurrency))
                        .append(Component.literal(" and " + itemsToReturn.size() + " item stack(s) from your shop.")
                                .withStyle(ChatFormatting.YELLOW)));
            }
        }

        LOGGER.info("Returned {} coins and {} item stacks from shop {} to owner {}",
                totalCurrency, itemsToReturn.size(), shop.getShopId(), shop.getOwnerName());
    }

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