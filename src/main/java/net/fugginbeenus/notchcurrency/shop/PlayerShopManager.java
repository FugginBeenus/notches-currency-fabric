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
    public static int MAX_SHOPS_PER_PLAYER = 3;
    public static double SALES_TAX_PERCENT = 0.0;
    public static int MIN_PRICE = 0;
    public static int MAX_PRICE = 1_000_000;

    private PlayerShopManager() {}

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

        returnAllShopContents(player.level().getServer(), shop, player);

        state.deleteShop(shopId, player.getUUID());
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Shop deleted. Everything has been returned to you.")
                .withStyle(ChatFormatting.YELLOW));

        return true;
    }

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

        if (listing.getStockQuantity() > 0) {
            ItemStack toReturn = listing.createSaleStack(listing.getStockQuantity());
            giveItemsToPlayer(owner, toReturn);
        }

        shop.removeListing(listingId);
        state.markDirtyAndSave();

        net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("Listing removed. Stock returned.").withStyle(ChatFormatting.YELLOW));
        return true;
    }

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

        int totalItems = listing.getBundleSize() * quantity;
        int available = listing.getStockQuantity();
        if (available < totalItems) return PurchaseResult.INSUFFICIENT_STOCK;

        boolean needsCoins = listing.acceptsCoins() && listing.getCoinPrice() > 0;
        boolean needsBarter = listing.acceptsBarter();

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

        if (!listing.tryRemoveStock(totalItems)) {
            return PurchaseResult.INSUFFICIENT_STOCK;
        }

        if (needsCoins) {
            CurrencyApi.withdraw(buyer, totalCoinCost,
                    net.fugginbeenus.notchcurrency.economy.TransactionReason.SHOP_SALE, "shop purchase");
        }
        if (needsBarter) {
            removeItemsFromInventory(buyer, barterItem, totalBarterCost);
        }

        ItemStack purchased = listing.createSaleStack(totalItems);
        giveItemsToPlayer(buyer, purchased);
        ServerPlayer seller = server.getPlayerList().getPlayer(shop.getOwnerId());

        int sellerEarnings = 0;
        if (needsCoins && totalCoinCost > 0) {
            int tax = (int) Math.floor(totalCoinCost * SALES_TAX_PERCENT / 100.0);
            sellerEarnings = totalCoinCost - tax;
            shop.recordSale(sellerEarnings);
        }

        if (needsBarter && totalBarterCost > 0) {
            ItemStack barterPayment = barterItem.copy();
            barterPayment.setCount(totalBarterCost);
            shop.addPendingBarterItem(barterPayment);
        }

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

        listing.recordSale(totalItems, totalCoinCost);
        state.markDirtyAndSave();

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

    private static void giveItemsToPlayer(ServerPlayer player, ItemStack items) {
        if (items.isEmpty()) return;
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
        long totalCurrency = shop.withdrawBalance();
        List<ItemStack> itemsToReturn = new java.util.ArrayList<>(shop.collectPendingBarterItems());
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

        if (totalCurrency > 0) {
            CurrencyApi.deposit(server, shop.getOwnerId(), totalCurrency,
                    net.fugginbeenus.notchcurrency.economy.TransactionReason.SHOP_PAYOUT, "shop closed/returned");
        }

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

        UUID oldOwnerId = shop.getOwnerId();
        String oldOwnerName = shop.getOwnerName();

        state.updateShopOwnership(shopId, newOwnerId, newOwnerName);
        state.markDirtyAndSave();

        LOGGER.info("Transferred shop {} from {} to {}", shopId, oldOwnerName, newOwnerName);
        return true;
    }

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