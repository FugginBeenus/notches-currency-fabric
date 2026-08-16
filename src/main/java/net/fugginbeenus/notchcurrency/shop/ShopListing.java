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
    private ItemStack itemPrice;
    private int itemPriceCount;
    private int totalSold;
    private long totalEarned;

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

        if (!itemPrice.isEmpty()) {
            nbt.put("ItemPrice", StackData.writeStack(itemPrice));
            nbt.putInt("ItemPriceCount", itemPriceCount);
        }

        nbt.putInt("TotalSold", totalSold);
        nbt.putLong("TotalEarned", totalEarned);

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

        if (nbt.contains("ItemPrice", Tag.TAG_COMPOUND)) {
            listing.itemPrice = StackData.readStack(nbt.getCompound("ItemPrice"));
            listing.itemPriceCount = nbt.getInt("ItemPriceCount");
        }

        listing.totalSold = nbt.getInt("TotalSold");
        listing.totalEarned = nbt.getLong("TotalEarned");

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