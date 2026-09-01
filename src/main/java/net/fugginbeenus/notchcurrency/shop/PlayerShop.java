package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayerShop {

    public static final int MAX_LISTINGS = 27;
    private final UUID shopId;
    private UUID ownerId;
    private String ownerName;
    private String shopName;
    private UUID linkedNpcId;
    private String shopkeeperDialog;
    private final List<ShopListing> listings;
    private long totalRevenue;
    private long pendingBalance;
    private int totalTransactions;
    private long createdAt;
    private boolean isOpen;
    private boolean adminMode;
    private boolean rentPaused = false;
    private int unpaidRentCycles = 0;
    private final List<ItemStack> pendingBarterItems;

    public PlayerShop(UUID ownerId, String ownerName, String shopName) {
        this.shopId = UUID.randomUUID();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.shopName = shopName;
        this.linkedNpcId = null;
        this.shopkeeperDialog = "";
        this.listings = new ArrayList<>();
        this.pendingBarterItems = new ArrayList<>();
        this.totalRevenue = 0;
        this.pendingBalance = 0;
        this.totalTransactions = 0;
        this.createdAt = System.currentTimeMillis();
        this.isOpen = true;
    }

    private PlayerShop(UUID shopId, UUID ownerId) {
        this.shopId = shopId;
        this.ownerId = ownerId;
        this.ownerName = "Unknown";
        this.shopName = "Shop";
        this.linkedNpcId = null;
        this.listings = new ArrayList<>();
        this.pendingBarterItems = new ArrayList<>();
        this.totalRevenue = 0;
        this.pendingBalance = 0;
        this.totalTransactions = 0;
        this.createdAt = System.currentTimeMillis();
        this.isOpen = true;
    }

    public UUID getShopId() {
        return shopId;
    }
    public UUID getOwnerId() {
        return ownerId;
    }
    public String getOwnerName() {
        return ownerName;
    }
    public String getShopName() {
        return shopName;
    }

    @Nullable
    public UUID getLinkedNpcId() {
        return linkedNpcId;
    }
    public List<ShopListing> getListings() {
        return Collections.unmodifiableList(listings);
    }
    public long getTotalRevenue() {
        return totalRevenue;
    }
    public long getPendingBalance() {
        return pendingBalance;
    }
    public boolean spendBalance(long amount) {
        if (amount <= 0) return true;
        if (pendingBalance < amount) return false;
        pendingBalance -= amount;
        return true;
    }

    public long withdrawBalance() {
        long amount = pendingBalance;
        pendingBalance = 0;
        return amount;
    }

    public void addToPendingBalance(long amount) {
        pendingBalance += amount;
    }

    public long payFromPending(long amount) {
        long take = Math.min(Math.max(0L, amount), pendingBalance);
        pendingBalance -= take;
        return take;
    }

    public boolean isRentPaused() { return rentPaused; }
    public void setRentPaused(boolean paused) { this.rentPaused = paused; }
    public int getUnpaidRentCycles() { return unpaidRentCycles; }
    public void setUnpaidRentCycles(int n) { this.unpaidRentCycles = Math.max(0, n); }
    public int getTotalTransactions() {
        return totalTransactions;
    }
    public long getCreatedAt() {
        return createdAt;
    }
    public boolean isAdminMode() {
        return adminMode;
    }

    public void setAdminMode(boolean admin) {
        this.adminMode = admin;
    }

    public boolean isOpen() {
        return isOpen;
    }
    public void setOwnerName(String name) {
        this.ownerName = name;
    }
    public void setShopName(String name) {
        this.shopName = name != null ? name : "Shop";
    }
    public String getShopkeeperDialog() {
        return shopkeeperDialog != null ? shopkeeperDialog : "";
    }
    public void setShopkeeperDialog(String dialog) {
        this.shopkeeperDialog = dialog != null ? dialog : "";
    }
    public void setLinkedNpcId(@Nullable UUID npcId) {
        this.linkedNpcId = npcId;
    }
    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    void transferOwnership(UUID newOwnerId, String newOwnerName) {
        this.ownerId = newOwnerId;
        this.ownerName = newOwnerName;
    }

    public boolean canAddListing() {
        return listings.size() < MAX_LISTINGS;
    }

    public boolean addListing(ShopListing listing) {
        if (!canAddListing()) return false;
        listings.add(listing);
        return true;
    }

    public boolean removeListing(UUID listingId) {
        return listings.removeIf(l -> l.getId().equals(listingId));
    }

    @Nullable
    public ShopListing getListing(UUID listingId) {
        return listings.stream()
                .filter(l -> l.getId().equals(listingId))
                .findFirst()
                .orElse(null);
    }

    public List<ShopListing> getInStockListings() {
        return listings.stream()
                .filter(l -> l.isInStock(adminMode))
                .toList();
    }

    public void recordSale(int coinsEarned) {
        this.totalRevenue += coinsEarned;
        this.pendingBalance += coinsEarned;
        this.totalTransactions++;
    }

    public void addPendingBarterItem(ItemStack item) {
        if (!item.isEmpty()) {
            pendingBarterItems.add(item.copy());
        }
    }

    public List<ItemStack> collectPendingBarterItems() {
        List<ItemStack> items = new ArrayList<>(pendingBarterItems);
        pendingBarterItems.clear();
        return items;
    }

    public int getPendingBarterCount() {
        return pendingBarterItems.size();
    }
    public boolean hasPendingBarterItems() {
        return !pendingBarterItems.isEmpty();
    }


    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "ShopId", shopId);
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "OwnerId", ownerId);
        nbt.putString("OwnerName", ownerName);
        nbt.putString("ShopName", shopName);

        if (linkedNpcId != null) {
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "LinkedNpcId", linkedNpcId);
        }

        if (shopkeeperDialog != null && !shopkeeperDialog.isEmpty()) {
            nbt.putString("ShopkeeperDialog", shopkeeperDialog);
        }

        ListTag listingsNbt = new ListTag();
        for (ShopListing listing : listings) {
            listingsNbt.add(listing.toNbt());
        }
        nbt.put("Listings", listingsNbt);

        ListTag barterNbt = new ListTag();
        for (ItemStack item : pendingBarterItems) {
            barterNbt.add(StackData.writeStack(item));
        }
        nbt.put("PendingBarterItems", barterNbt);

        nbt.putLong("TotalRevenue", totalRevenue);
        nbt.putLong("PendingBalance", pendingBalance);
        nbt.putInt("TotalTransactions", totalTransactions);
        nbt.putLong("CreatedAt", createdAt);
        nbt.putBoolean("IsOpen", isOpen);
        if (adminMode) nbt.putBoolean("AdminMode", true);
        nbt.putBoolean("RentPaused", rentPaused);
        nbt.putInt("UnpaidRentCycles", unpaidRentCycles);

        return nbt;
    }

    public static PlayerShop fromNbt(CompoundTag nbt) {
        UUID shopId = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "ShopId");
        UUID ownerId = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "OwnerId");

        PlayerShop shop = new PlayerShop(shopId, ownerId);
        shop.ownerName = nbt.getString("OwnerName");
        shop.shopName = nbt.getString("ShopName");

        if (nbt.contains("LinkedNpcId")) {
            shop.linkedNpcId = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "LinkedNpcId");
        }

        if (nbt.contains("ShopkeeperDialog")) {
            shop.shopkeeperDialog = nbt.getString("ShopkeeperDialog");
        }

        if (nbt.contains("Listings", Tag.TAG_LIST)) {
            ListTag listingsNbt = nbt.getList("Listings", Tag.TAG_COMPOUND);
            for (int i = 0; i < listingsNbt.size(); i++) {
                shop.listings.add(ShopListing.fromNbt(listingsNbt.getCompound(i)));
            }
        }

        if (nbt.contains("PendingBarterItems", Tag.TAG_LIST)) {
            ListTag barterNbt = nbt.getList("PendingBarterItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < barterNbt.size(); i++) {
                ItemStack item = StackData.readStack(barterNbt.getCompound(i));
                if (!item.isEmpty()) {
                    shop.pendingBarterItems.add(item);
                }
            }
        }

        shop.totalRevenue = nbt.getLong("TotalRevenue");
        shop.pendingBalance = nbt.getLong("PendingBalance");
        shop.totalTransactions = nbt.getInt("TotalTransactions");
        shop.createdAt = nbt.getLong("CreatedAt");
        shop.isOpen = nbt.getBoolean("IsOpen");
        shop.adminMode = nbt.getBoolean("AdminMode");
        shop.rentPaused = nbt.getBoolean("RentPaused");
        shop.unpaidRentCycles = nbt.getInt("UnpaidRentCycles");

        return shop;
    }
}