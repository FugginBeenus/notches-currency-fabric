package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class ShopListing {

    private final UUID id;
    private ItemStack itemForSale;
    private int stockQuantity;
    private int coinPrice;
    private int shopPaysPrice;
    private boolean dynamicPricing;
    private double stockIndex;
    private ItemStack itemPrice;
    private int itemPriceCount;
    private int totalSold;
    private long totalEarned;
    private Restock.Mode restockMode = Restock.Mode.OFF;
    private int restockTo = 0;
    private long lastRestockPeriod = 0L;
    private int perPlayerLimit = 0;
    private final java.util.Map<UUID, Integer> boughtBy = new java.util.HashMap<>();

    public ShopListing(ItemStack itemForSale, int stockQuantity, int coinPrice) {
        this.id = UUID.randomUUID();
        this.itemForSale = itemForSale.copy();
        this.stockQuantity = stockQuantity;
        this.coinPrice = Math.max(0, coinPrice);
        this.itemPrice = ItemStack.EMPTY;
        this.itemPriceCount = 0;
        this.totalSold = 0;
        this.totalEarned = 0;
    }

    public ShopListing(UUID id, ItemStack itemForSale, int stockQuantity, int coinPrice,
                       ItemStack barterItem, int barterCount) {
        this.id = id;
        this.itemForSale = itemForSale.copy();
        this.stockQuantity = stockQuantity;
        this.coinPrice = Math.max(0, coinPrice);
        this.itemPrice = barterItem == null ? ItemStack.EMPTY : barterItem.copy();
        this.itemPriceCount = Math.max(0, barterCount);
        this.totalSold = 0;
        this.totalEarned = 0;
    }

    private ShopListing(UUID id) {
        this.id = id;
        this.itemForSale = ItemStack.EMPTY;
        this.stockQuantity = 0;
        this.coinPrice = 0;
        this.itemPrice = ItemStack.EMPTY;
        this.itemPriceCount = 0;
        this.totalSold = 0;
        this.totalEarned = 0;
    }

    public UUID getId() {
        return id;
    }
    public ItemStack getItemForSale() {
        return itemForSale;
    }
    public void setItemForSale(ItemStack item) {
        this.itemForSale = item.copy();
    }
    public int getStockQuantity() {
        return stockQuantity;
    }
    public int getCoinPrice() {
        return coinPrice;
    }
    public boolean acceptsCoins() {
        return coinPrice > 0;
    }
    public int getShopPaysPrice() {
        return shopPaysPrice;
    }
    public void setShopPaysPrice(int price) {
        this.shopPaysPrice = Math.max(0, price);
    }
    public boolean buysFromPlayers() {
        return ShopRules.sellToShops && shopPaysPrice > 0;
    }
    public boolean isDynamicPricing() {
        return dynamicPricing;
    }
    public void setDynamicPricing(boolean on) {
        this.dynamicPricing = on;
        if (!on) this.stockIndex = 0.0;
    }
    public double priceMultiplier() {
        return (dynamicPricing && ShopRules.dynamicPricing) ? DynamicPrice.multiplier(stockIndex) : 1.0;
    }
    public int currentCoinPrice() {
        if (!dynamicPricing || !ShopRules.dynamicPricing || coinPrice <= 0) return coinPrice;
        return (int) Math.max(1, Math.round(coinPrice * priceMultiplier()));
    }
    public int currentShopPays() {
        if (!dynamicPricing || !ShopRules.dynamicPricing || shopPaysPrice <= 0) return shopPaysPrice;
        return (int) Math.max(0, Math.round(shopPaysPrice * priceMultiplier()));
    }
    public synchronized void recordDemand(int units) {
        if (dynamicPricing) stockIndex -= units;
    }
    public synchronized void recordSupply(int units) {
        if (dynamicPricing) stockIndex += units;
    }
    public synchronized void decayPrice() {
        if (dynamicPricing) stockIndex = DynamicPrice.decayed(stockIndex);
    }
    public ItemStack getItemPrice() {
        return itemPrice;
    }
    public int getItemPriceCount() {
        return itemPriceCount;
    }
    public boolean acceptsBarter() {
        return !itemPrice.isEmpty() && itemPriceCount > 0;
    }
    public int getTotalSold() {
        return totalSold;
    }
    public long getTotalEarned() {
        return totalEarned;
    }
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isInStock(boolean infinite) {
        return infinite || stockQuantity > 0;
    }

    public Restock.Mode getRestockMode() { return restockMode; }
    public void setRestockMode(Restock.Mode mode) { this.restockMode = mode == null ? Restock.Mode.OFF : mode; }
    public int getRestockTo() { return restockTo; }
    public void setRestockTo(int amount) { this.restockTo = Math.max(0, amount); }

    public void setBuyLimitFrom(int buyLimit, int sellLimit, Restock.Mode mode) {
        this.perPlayerLimit = Math.max(0, Math.max(buyLimit, sellLimit));
        this.restockMode = mode == null ? Restock.Mode.OFF : mode;
    }

    public int getPerPlayerLimit() { return perPlayerLimit; }
    public void setPerPlayerLimit(int limit) { this.perPlayerLimit = Math.max(0, limit); }

    public synchronized int boughtBy(UUID player) {
        return boughtBy.getOrDefault(player, 0);
    }

    public synchronized int remainingFor(UUID player) {
        if (!ShopRules.buyLimits || perPlayerLimit <= 0) return Integer.MAX_VALUE;
        return Math.max(0, perPlayerLimit - boughtBy.getOrDefault(player, 0));
    }

    public synchronized void recordPurchase(UUID player, int quantity) {
        if (perPlayerLimit <= 0 || quantity <= 0) return;
        boughtBy.merge(player, quantity, Integer::sum);
    }

    public synchronized boolean maybeRestock(net.minecraft.server.level.ServerLevel level) {
        if (!ShopRules.restock || restockMode == Restock.Mode.OFF) return false;
        long now = Restock.periodOf(restockMode, level);
        if (now == lastRestockPeriod) return false;
        lastRestockPeriod = now;
        boolean changed = !boughtBy.isEmpty();
        boughtBy.clear();
        if (restockTo > 0 && stockQuantity < restockTo) {
            stockQuantity = restockTo;
            changed = true;
        }
        return changed;
    }

    public void setCoinPrice(int price) {
        this.coinPrice = Math.max(0, price);
    }

    public void setBarterPrice(ItemStack item, int count) {
        this.itemPrice = item == null ? ItemStack.EMPTY : item.copy();
        this.itemPriceCount = Math.max(0, count);
        if (this.itemPrice.isEmpty()) {
            this.itemPriceCount = 0;
        }
    }

    public synchronized void addStock(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative stock");
        }
        this.stockQuantity = Math.max(0, this.stockQuantity + amount);
    }

    public synchronized boolean tryRemoveStock(int amount) {
        if (amount <= 0) {
            return false;
        }
        if (this.stockQuantity < amount) {
            return false;
        }
        this.stockQuantity -= amount;
        return true;
    }

    public synchronized void removeStock(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot remove negative stock");
        }
        this.stockQuantity = Math.max(0, this.stockQuantity - amount);
    }

    public synchronized void setStock(int amount) {
        this.stockQuantity = Math.max(0, amount);
    }

    public synchronized int getStockQuantitySafe() {
        return this.stockQuantity;
    }

    public synchronized void recordSale(int quantity, int coinsEarned) {
        if (quantity <= 0 || coinsEarned < 0) {
            return;
        }
        this.totalSold += quantity;
        this.totalEarned += coinsEarned;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "Id", id);
        nbt.put("Item", StackData.writeStack(itemForSale));
        nbt.putInt("Stock", stockQuantity);
        nbt.putInt("CoinPrice", coinPrice);
        if (shopPaysPrice > 0) nbt.putInt("ShopPays", shopPaysPrice);
        if (dynamicPricing) {
            nbt.putBoolean("Dynamic", true);
            nbt.putDouble("StockIndex", stockIndex);
        }

        if (!itemPrice.isEmpty()) {
            nbt.put("ItemPrice", StackData.writeStack(itemPrice));
            nbt.putInt("ItemPriceCount", itemPriceCount);
        }

        nbt.putInt("TotalSold", totalSold);
        nbt.putLong("TotalEarned", totalEarned);
        if (restockMode != Restock.Mode.OFF) {
            nbt.putString("RestockMode", restockMode.name());
            nbt.putInt("RestockTo", restockTo);
            nbt.putLong("RestockPeriod", lastRestockPeriod);
        }
        if (perPlayerLimit > 0) {
            nbt.putInt("PerPlayerLimit", perPlayerLimit);
            net.minecraft.nbt.ListTag bought = new net.minecraft.nbt.ListTag();
            for (java.util.Map.Entry<UUID, Integer> e : boughtBy.entrySet()) {
                CompoundTag row = new CompoundTag();
                net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(row, "P", e.getKey());
                row.putInt("N", e.getValue());
                bought.add(row);
            }
            nbt.put("BoughtBy", bought);
        }

        return nbt;
    }

    public static ShopListing fromNbt(CompoundTag nbt) {
        UUID id = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "Id");
        ShopListing listing = new ShopListing(id);

        if (nbt.contains("Item", Tag.TAG_COMPOUND)) {
            listing.itemForSale = StackData.readStack(nbt.getCompound("Item"));
        }

        listing.stockQuantity = nbt.getInt("Stock");
        listing.coinPrice = nbt.getInt("CoinPrice");
        listing.shopPaysPrice = nbt.getInt("ShopPays");
        listing.dynamicPricing = nbt.getBoolean("Dynamic");
        listing.stockIndex = nbt.getDouble("StockIndex");

        if (nbt.contains("ItemPrice", Tag.TAG_COMPOUND)) {
            listing.itemPrice = StackData.readStack(nbt.getCompound("ItemPrice"));
            listing.itemPriceCount = nbt.getInt("ItemPriceCount");
        }

        listing.totalSold = nbt.getInt("TotalSold");
        listing.totalEarned = nbt.getLong("TotalEarned");
        listing.restockMode = Restock.Mode.byName(nbt.getString("RestockMode"));
        listing.restockTo = nbt.getInt("RestockTo");
        listing.lastRestockPeriod = nbt.getLong("RestockPeriod");
        listing.perPlayerLimit = nbt.getInt("PerPlayerLimit");
        if (nbt.contains("BoughtBy", Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag bought = nbt.getList("BoughtBy", Tag.TAG_COMPOUND);
            for (int i = 0; i < bought.size(); i++) {
                CompoundTag row = bought.getCompound(i);
                if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(row, "P")) continue;
                listing.boughtBy.put(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(row, "P"), row.getInt("N"));
            }
        }

        return listing;
    }

    public int getBundleSize() {
        return Math.max(1, itemForSale.getCount());
    }

    public ItemStack createSaleStack(int totalItems) {
        ItemStack stack = itemForSale.copy();
        stack.setCount(Math.max(0, totalItems));
        return stack;
    }
}