package net.fugginbeenus.notchcurrency.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a player-owned shop.
 *
 * A shop can be linked to a ShopkeeperEntity for the storefront,
 * but the shop data and logic are managed entirely by NotchCurrency.
 */
public class PlayerShop {

    public static final int MAX_LISTINGS = 27;  // 3 rows of 9, like a chest

    private final UUID shopId;
    private UUID ownerId;                 // Can be changed by admin transfer
    private String ownerName;           // Cached for display
    private String shopName;            // Custom shop name
    private UUID linkedNpcId;           // Optional: linked ShopkeeperEntity UUID
    private String shopkeeperDialog;    // NPC greeting/dialog text

    private final List<ShopListing> listings;
    private long totalRevenue;          // Lifetime coins earned
    private long pendingBalance;        // Coins waiting to be withdrawn
    private int totalTransactions;      // Lifetime sales count
    private long createdAt;             // Timestamp
    private boolean isOpen;             // Whether shop accepts purchases

    // Rent state: a rent-paused shop is still "open" but won't sell until rent is paid.
    private boolean rentPaused = false;
    private int unpaidRentCycles = 0;

    // Pending barter items for offline collection.
    // Coin earnings are tracked solely by pendingBalance (single source of truth).
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

    // --- Getters ---

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

    /**
     * Withdraw the pending balance. Returns the amount withdrawn.
     */
    public long withdrawBalance() {
        long amount = pendingBalance;
        pendingBalance = 0;
        return amount;
    }

    /**
     * Add earnings to the pending balance (called when a sale is made).
     */
    public void addToPendingBalance(long amount) {
        pendingBalance += amount;
    }

    /** Deduct up to {@code amount} from the pending balance; returns what was actually taken. */
    public long payFromPending(long amount) {
        long take = Math.min(Math.max(0L, amount), pendingBalance);
        pendingBalance -= take;
        return take;
    }

    // --- Rent ---

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

    public boolean isOpen() {
        return isOpen;
    }

    // --- Setters ---

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

    /**
     * Transfer ownership of the shop (admin only).
     * Package-private to restrict access.
     */
    void transferOwnership(UUID newOwnerId, String newOwnerName) {
        this.ownerId = newOwnerId;
        this.ownerName = newOwnerName;
    }

    // --- Listing Management ---

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
                .filter(ShopListing::isInStock)
                .toList();
    }

    // --- Sales Recording ---

    public void recordSale(int coinsEarned) {
        this.totalRevenue += coinsEarned;
        this.pendingBalance += coinsEarned;  // Add to pending balance for withdrawal
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

    /** How many barter stacks are waiting to be collected (non-destructive, for GUI display). */
    public int getPendingBarterCount() {
        return pendingBarterItems.size();
    }

    public boolean hasPendingBarterItems() {
        return !pendingBarterItems.isEmpty();
    }

    // --- NBT Serialization ---

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putUuid("ShopId", shopId);
        nbt.putUuid("OwnerId", ownerId);
        nbt.putString("OwnerName", ownerName);
        nbt.putString("ShopName", shopName);

        if (linkedNpcId != null) {
            nbt.putUuid("LinkedNpcId", linkedNpcId);
        }

        if (shopkeeperDialog != null && !shopkeeperDialog.isEmpty()) {
            nbt.putString("ShopkeeperDialog", shopkeeperDialog);
        }

        NbtList listingsNbt = new NbtList();
        for (ShopListing listing : listings) {
            listingsNbt.add(listing.toNbt());
        }
        nbt.put("Listings", listingsNbt);

        // Save pending barter items
        NbtList barterNbt = new NbtList();
        for (ItemStack item : pendingBarterItems) {
            barterNbt.add(item.writeNbt(new NbtCompound()));
        }
        nbt.put("PendingBarterItems", barterNbt);

        nbt.putLong("TotalRevenue", totalRevenue);
        nbt.putLong("PendingBalance", pendingBalance);
        nbt.putInt("TotalTransactions", totalTransactions);
        nbt.putLong("CreatedAt", createdAt);
        nbt.putBoolean("IsOpen", isOpen);
        nbt.putBoolean("RentPaused", rentPaused);
        nbt.putInt("UnpaidRentCycles", unpaidRentCycles);

        return nbt;
    }

    public static PlayerShop fromNbt(NbtCompound nbt) {
        UUID shopId = nbt.getUuid("ShopId");
        UUID ownerId = nbt.getUuid("OwnerId");

        PlayerShop shop = new PlayerShop(shopId, ownerId);
        shop.ownerName = nbt.getString("OwnerName");
        shop.shopName = nbt.getString("ShopName");

        if (nbt.contains("LinkedNpcId")) {
            shop.linkedNpcId = nbt.getUuid("LinkedNpcId");
        }

        if (nbt.contains("ShopkeeperDialog")) {
            shop.shopkeeperDialog = nbt.getString("ShopkeeperDialog");
        }

        if (nbt.contains("Listings", NbtElement.LIST_TYPE)) {
            NbtList listingsNbt = nbt.getList("Listings", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < listingsNbt.size(); i++) {
                shop.listings.add(ShopListing.fromNbt(listingsNbt.getCompound(i)));
            }
        }

        // Load pending barter items
        if (nbt.contains("PendingBarterItems", NbtElement.LIST_TYPE)) {
            NbtList barterNbt = nbt.getList("PendingBarterItems", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < barterNbt.size(); i++) {
                ItemStack item = ItemStack.fromNbt(barterNbt.getCompound(i));
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
        shop.rentPaused = nbt.getBoolean("RentPaused");
        shop.unpaidRentCycles = nbt.getInt("UnpaidRentCycles");

        return shop;
    }
}