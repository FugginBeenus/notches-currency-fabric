package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.UUID;

public class ShopListing {

    private final UUID id;
    private ItemStack itemForSale;      // The item being sold (template)
    private int stockQuantity;          // How many are in stock
    private int coinPrice;              // Price in coins (0 = not for coin sale)
    private ItemStack itemPrice;        // Item required for barter (EMPTY = not for barter)
    private int itemPriceCount;         // How many of itemPrice needed per purchase
    private int totalSold;              // Statistics: total units sold
    private long totalEarned;           // Statistics: total coins earned

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

    // --- Getters ---

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

    // --- Setters ---

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
            return; // Invalid sale
        }
        this.totalSold += quantity;
        this.totalEarned += coinsEarned;
    }

    // --- NBT Serialization ---

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("Id", id);
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

    public static ShopListing fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("Id");
        ShopListing listing = new ShopListing(id);

        if (nbt.contains("Item", NbtElement.COMPOUND_TYPE)) {
            listing.itemForSale = StackData.readStack(nbt.getCompound("Item"));
        }

        listing.stockQuantity = nbt.getInt("Stock");
        listing.coinPrice = nbt.getInt("CoinPrice");

        if (nbt.contains("ItemPrice", NbtElement.COMPOUND_TYPE)) {
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