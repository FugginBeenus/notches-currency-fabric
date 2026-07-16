package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AuctionHouseScreenHandler extends ScreenHandler {

    // layout: 9 columns × 4 rows of listing slots
    public static final int LISTING_COLUMNS = 9;
    public static final int LISTING_ROWS    = 4;
    public static final int LISTING_SIZE    = LISTING_COLUMNS * LISTING_ROWS; // 36

    // Slot positions inside the 256×256 texture (relative to GUI top-left)
    private static final int SLOT_SIZE        = 18;
    private static final int LISTING_START_X  = 9;   // left margin under “MY LISTINGS”
    private static final int LISTING_START_Y  = 28;  // just below the green header bar

    private static final int PLAYER_INV_START_X = 9;
    private static final int PLAYER_INV_START_Y = 127; // top of the lower grey inventory area

    private static final int HOTBAR_START_X = 9;
    private static final int HOTBAR_START_Y = PLAYER_INV_START_Y + 58; // standard 58-pixel offset

    // Popup inventory (My Listings overlay): 5 × 2 grid
    public static final int POPUP_COLUMNS = 5;
    public static final int POPUP_ROWS    = 2;
    public static final int POPUP_SIZE    = POPUP_COLUMNS * POPUP_ROWS;

    // --- READ-ONLY SLOT FOR AH GRID ---
    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return false;
        }
    }

    // Inventories
    private final SimpleInventory listingsInv   = new SimpleInventory(LISTING_SIZE);
    private final SimpleInventory userPopupInv  = new SimpleInventory(POPUP_SIZE);

    // For click-to-buy: which listing UUID is in each AH slot?
    private final UUID[] listingIds = new UUID[LISTING_SIZE];

    private final PlayerInventory playerInv;
    private final World world;

    // --- UI / paging / filter state used by AuctionHouseScreen ---
    private int page       = 0;
    private int totalPages = 1; // computed from auction data

    private boolean showMyListings = false;

    public enum FilterMode {
        ALL,
        BLOCKS,
        FURNITURE,
        MOBS,
        GEAR,
        SEASONAL,
        VALUABLES,
        BOOKS,
        OTHER
    }

    public enum SortMode {
        NEWEST,        // "Most recent"
        ENDING_SOON,   // "Ending soon"
        PRICE_DESC,    // "Highest price"
        PRICE_ASC,     // "Lowest price"
        NAME           // "Item name"
    }

    private FilterMode filter   = FilterMode.ALL;
    private SortMode  sortMode  = SortMode.NEWEST;

    // ---- property sync indices ----
    private static final int PROP_PAGE        = 0;
    private static final int PROP_TOTAL_PAGES = 1;
    private static final int PROP_FILTER      = 2;
    private static final int PROP_SORT        = 3;

    private final PropertyDelegate properties = new ArrayPropertyDelegate(4);

    public AuctionHouseScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ModScreenHandlers.AUCTION_HOUSE, syncId);
        this.playerInv = playerInv;
        this.world     = playerInv.player.getWorld();

        this.addProperties(properties);

        // --- Listing slots (top grid) – READ ONLY ---
        int index = 0;
        for (int row = 0; row < LISTING_ROWS; row++) {
            for (int col = 0; col < LISTING_COLUMNS; col++) {
                int x = LISTING_START_X + col * SLOT_SIZE;
                int y = LISTING_START_Y + row * SLOT_SIZE;
                this.addSlot(new ReadOnlySlot(listingsInv, index++, x, y));
            }
        }

        // --- Player inventory (3×9) ---
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = PLAYER_INV_START_X + col * SLOT_SIZE;
                int y = PLAYER_INV_START_Y + row * SLOT_SIZE;
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, x, y));
            }
        }

        // --- Hotbar (1×9) ---
        for (int col = 0; col < 9; col++) {
            int x = HOTBAR_START_X + col * SLOT_SIZE;
            int y = HOTBAR_START_Y;
            this.addSlot(new Slot(playerInv, col, x, y));
        }

        // --- Hidden popup slots (for syncing userPopupInv to client) ---
        for (int i = 0; i < POPUP_SIZE; i++) {
            this.addSlot(new ReadOnlySlot(userPopupInv, i, -10000, -10000));
        }

        // Initial load for page 0
        reload();
    }

    // Sync small bits of state to the client
    private void syncProperties() {
        // Only meaningful on the server side; client will receive these via packets.
        if (world != null && !world.isClient) {
            properties.set(PROP_PAGE, page);
            properties.set(PROP_TOTAL_PAGES, totalPages);
            properties.set(PROP_FILTER, filter.ordinal());
            properties.set(PROP_SORT, sortMode.ordinal());
        }
    }

    private boolean matchesFilter(AuctionListing listing) {
        // ALL shows everything
        if (filter == FilterMode.ALL) {
            return true;
        }

        // Re-derive category if missing or "other"
        String cat = listing.category;
        if (cat == null || cat.isEmpty() || cat.equalsIgnoreCase("other")) {
            cat = AuctionCategories.classify(listing.stack);
        }

        if (cat == null) {
            cat = "other";
        }

        cat = cat.toLowerCase(Locale.ROOT);

        return switch (filter) {
            case BLOCKS    -> cat.equals("blocks");
            case FURNITURE -> cat.equals("furniture");
            case MOBS      -> cat.equals("mobs");
            case GEAR      -> cat.equals("gear");
            case SEASONAL  -> cat.equals("seasonal");
            case VALUABLES -> cat.equals("valuables");
            case BOOKS     -> cat.equals("books");
            case OTHER     -> !cat.equals("blocks")
                    && !cat.equals("furniture")
                    && !cat.equals("mobs")
                    && !cat.equals("gear")
                    && !cat.equals("seasonal")
                    && !cat.equals("valuables")
                    && !cat.equals("books");
            case ALL       -> true;
        };
    }

    // ======== PUBLIC API FOR THE SCREEN ========

    public int getPage() {
        if (world != null && world.isClient) {
            return properties.get(PROP_PAGE);
        }
        return page;
    }

    public int getTotalPages() {
        if (world != null && world.isClient) {
            return Math.max(1, properties.get(PROP_TOTAL_PAGES));
        }
        return Math.max(1, totalPages);
    }

    private void setTotalPages(int total) {
        this.totalPages = Math.max(1, total);
    }

    public void nextPage() {
        if (world != null && world.isClient) return;
        if (page < getTotalPages() - 1) {
            page++;
            reloadPageContents();
        }
    }

    public void prevPage() {
        if (world != null && world.isClient) return;
        if (page > 0) {
            page--;
            reloadPageContents();
        }
    }

    public SimpleInventory getListingsInventory() {
        return listingsInv;
    }

    /** Called by the Reload button and when filters/sort/view change. */
    public void reload() {
        // Only the server actually rebuilds from AuctionState.
        if (!(world instanceof ServerWorld)) {
            return;
        }
        rebuildFromAuctionState();
    }

    public void toggleMyListings() {
        if (world != null && world.isClient) return;
        showMyListings = !showMyListings;
        reload();
    }

    public boolean isShowingMyListings() {
        return showMyListings;
    }

    public void cycleFilter() {
        if (world != null && world.isClient) return;

        switch (filter) {
            case ALL       -> filter = FilterMode.BLOCKS;
            case BLOCKS    -> filter = FilterMode.FURNITURE;
            case FURNITURE -> filter = FilterMode.MOBS;
            case MOBS      -> filter = FilterMode.GEAR;
            case GEAR      -> filter = FilterMode.SEASONAL;
            case SEASONAL  -> filter = FilterMode.VALUABLES;
            case VALUABLES -> filter = FilterMode.BOOKS;
            case BOOKS     -> filter = FilterMode.OTHER;
            case OTHER     -> filter = FilterMode.ALL;
        }
        reload();
    }

    public String getFilterLabel() {
        FilterMode effective = filter;
        if (world != null && world.isClient) {
            int ord = properties.get(PROP_FILTER);
            if (ord >= 0 && ord < FilterMode.values().length) {
                effective = FilterMode.values()[ord];
            }
        }

        return switch (effective) {
            case ALL       -> "All";
            case BLOCKS    -> "Blocks";
            case FURNITURE -> "Furniture";
            case MOBS      -> "Mobs";
            case GEAR      -> "Gear";
            case SEASONAL  -> "Seasonal";
            case VALUABLES -> "Valuables";
            case BOOKS     -> "Books";
            case OTHER     -> "Other";
        };
    }

    public void cycleSortMode() {
        if (world != null && world.isClient) return;
        switch (sortMode) {
            case NEWEST      -> sortMode = SortMode.ENDING_SOON;
            case ENDING_SOON -> sortMode = SortMode.PRICE_DESC;
            case PRICE_DESC  -> sortMode = SortMode.PRICE_ASC;
            case PRICE_ASC   -> sortMode = SortMode.NAME;
            case NAME        -> sortMode = SortMode.NEWEST;
        }
        reload();
    }

    public String getSortLabel() {
        SortMode effective = sortMode;
        if (world != null && world.isClient) {
            int ord = properties.get(PROP_SORT);
            if (ord >= 0 && ord < SortMode.values().length) {
                effective = SortMode.values()[ord];
            }
        }
        return switch (effective) {
            case NEWEST      -> "Most recent";
            case ENDING_SOON -> "Ending soon";
            case PRICE_DESC  -> "Highest price";
            case PRICE_ASC   -> "Lowest price";
            case NAME        -> "Item name";
        };
    }

    /** Used by the popup renderer to draw the player's own listings. */
    public SimpleInventory getUserPopupInventory() {
        return userPopupInv;
    }

    /** For click-to-buy mapping from slot → listing UUID. */
    public UUID getListingIdForSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= LISTING_SIZE) return null;
        return listingIds[slotIndex];
    }

    // ======== INTERNAL RELOAD LOGIC ========

    /** Client-side: just clear; the server will push real contents via slot sync. */
    private void clearInventoriesClientSide() {
        for (int i = 0; i < listingsInv.size(); i++) {
            listingsInv.setStack(i, ItemStack.EMPTY);
            listingIds[i] = null;
        }
        for (int i = 0; i < userPopupInv.size(); i++) {
            userPopupInv.setStack(i, ItemStack.EMPTY);
        }
    }

    /** Server-side: re-reads AuctionState, recomputes pages, fills both inventories. */
    private void rebuildFromAuctionState() {
        if (!(world instanceof ServerWorld serverWorld)) {
            clearInventoriesClientSide();
            return;
        }

        AuctionState state = AuctionState.get(serverWorld);

        // Pull the persistent list from AuctionState
        List<AuctionListing> allListings = new ArrayList<>(state.getListings());

        // Apply category filter
        List<AuctionListing> categoryFiltered = new ArrayList<>();
        for (AuctionListing l : allListings) {
            if (matchesFilter(l)) {
                categoryFiltered.add(l);
            }
        }
        allListings = categoryFiltered;

        // If showMyListings is meant to change the main grid, filter by seller here
        List<AuctionListing> mainList = allListings;
        if (showMyListings) {
            UUID me = playerInv.player.getUuid();
            mainList = new ArrayList<>();
            for (AuctionListing l : allListings) {
                if (l.sellerUuid.equals(me)) {
                    mainList.add(l);
                }
            }
        }

        // Sort according to sortMode
        Comparator<AuctionListing> cmp = switch (sortMode) {
            case PRICE_ASC   -> Comparator.comparingLong(l -> l.price);
            case PRICE_DESC  -> Comparator.comparingLong((AuctionListing l) -> l.price).reversed();
            case NEWEST      -> Comparator.comparingLong((AuctionListing l) -> l.createdGameTime).reversed();
            case ENDING_SOON -> Comparator.comparingLong(l -> l.expiresGameTime);
            case NAME        -> Comparator.comparing(l -> l.stack.getName().getString());
        };
        mainList.sort(cmp);

        // Compute total pages based on mainList
        int total = (mainList.size() + LISTING_SIZE - 1) / LISTING_SIZE;
        setTotalPages(total == 0 ? 1 : total);

        // Clamp page
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        if (page < 0) page = 0;

        // Fill listing grid for current page + UUID mapping
        for (int i = 0; i < listingsInv.size(); i++) {
            listingsInv.setStack(i, ItemStack.EMPTY);
            listingIds[i] = null;
        }

        if (!mainList.isEmpty()) {
            int start = page * LISTING_SIZE;
            for (int i = 0; i < LISTING_SIZE && (start + i) < mainList.size(); i++) {
                AuctionListing listing = mainList.get(start + i);
                listingsInv.setStack(i, makeDisplayStack(listing)); // decorated tooltip stack
                listingIds[i] = listing.id;
            }
        }

        // Fill popup with *this player's* listings (first POPUP_SIZE entries)
        UUID me = playerInv.player.getUuid();
        List<AuctionListing> mine = new ArrayList<>();
        for (AuctionListing l : allListings) {
            if (l.sellerUuid.equals(me)) {
                mine.add(l);
            }
        }
        mine.sort(cmp);

        for (int i = 0; i < userPopupInv.size(); i++) {
            userPopupInv.setStack(i, ItemStack.EMPTY);
        }
        int max = Math.min(userPopupInv.size(), mine.size());
        for (int i = 0; i < max; i++) {
            userPopupInv.setStack(i, mine.get(i).stack.copy());
        }

        // push page / total / filter / sort to client
        syncProperties();
    }

    /** Convenience: when page changes but filter/sort is already applied. */
    private void reloadPageContents() {
        reload();
    }

    // Build the display-only stack used in the AH grid (adds tooltip lore + NBT for client UI).
    private ItemStack makeDisplayStack(AuctionListing listing) {
        ItemStack base = listing.stack.copy();
        base.setCount(listing.stack.getCount());

        // Core NBT used by the client-side tooltip
        NbtCompound tag = StackData.editData(base);
        tag.putLong("nc_price", listing.price);
        tag.putString("nc_seller", listing.sellerName);
        tag.putLong("nc_created", listing.createdGameTime);
        tag.putLong("nc_expires", listing.expiresGameTime);
        tag.putUuid("nc_listing_id", listing.id);

        if (listing.highestBid > 0) {
            tag.putLong("nc_highest_bid", listing.highestBid);
            if (listing.highestBidderName != null) {
                tag.putString("nc_highest_bidder", listing.highestBidderName);
            }
        } else {
            tag.remove("nc_highest_bid");
            tag.remove("nc_highest_bidder");
        }

        StackData.commitData(base, tag);

        // Lore for vanilla hover. The lines are the same on both versions; only how they attach
        // differs — the display NBT tag on 1.20.1, the LORE component on 1.21.
        java.util.List<Text> loreLines = new java.util.ArrayList<>();
        loreLines.add(Text.literal("Price: " + listing.price + " ").formatted(Formatting.GOLD));
        if (listing.highestBid > 0) {
            loreLines.add(Text.literal("Highest bid: " + listing.highestBid + " ").formatted(Formatting.YELLOW));
        }
        loreLines.add(Text.literal("Seller: " + listing.sellerName).formatted(Formatting.GRAY));
        loreLines.add(Text.literal("Click to buy / bid").formatted(Formatting.YELLOW));

        //? if >=1.21 {
        /*base.set(net.minecraft.component.DataComponentTypes.LORE,
                new net.minecraft.component.type.LoreComponent(loreLines));
        *///?} else {
        NbtCompound display = base.getOrCreateSubNbt("display");
        NbtList lore = new NbtList();
        for (Text line : loreLines) lore.add(NbtString.of(Text.Serializer.toJson(line)));
        display.put("Lore", lore);
        //?}
        return base;
    }

    // ======== ScreenHandler boilerplate + click-to-buy ========

    @Override
    public boolean canUse(PlayerEntity player) {
        // It’s a virtual UI, no proximity checks needed
        return true;
    }

    /** Handle button clicks from the client (prev/next/filter/sort/reload). */
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity)) {
            return false;
        }

        switch (id) {
            case 0 -> prevPage();
            case 1 -> nextPage();
            case 2 -> cycleFilter();
            case 3 -> cycleSortMode();
            case 4 -> reload();
            case 5 -> net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager.openScreen((ServerPlayerEntity) player);
            case 6 -> AuctionListingScreenHandler.open((ServerPlayerEntity) player);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Shift-click behavior – AH grid is read-only. */
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        // Prevent any quick-move into / out of the listing grid
        if (index < LISTING_SIZE) {
            return ItemStack.EMPTY;
        }

        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stackInSlot = slot.getStack();
            newStack = stackInSlot.copy();

            int ahEnd      = LISTING_SIZE;         // 0..(LISTING_SIZE-1) = AH listings
            int invEnd     = ahEnd + 27;           // 27 = 3×9 inventory
            int hotbarEnd  = invEnd + 9;           // +9 = hotbar

            // Only move between player inventory and hotbar, not into AH slots
            if (index < invEnd) {
                // from main inv → hotbar
                if (!this.insertItem(stackInSlot, ahEnd + 27, hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // from hotbar → main inv
                if (!this.insertItem(stackInSlot, ahEnd, invEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Click on a listing slot (top grid)
        if (slotIndex >= 0 && slotIndex < LISTING_SIZE) {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                super.onSlotClick(slotIndex, button, actionType, player);
                return;
            }
            if (!(world instanceof ServerWorld serverWorld)) {
                super.onSlotClick(slotIndex, button, actionType, player);
                return;
            }

            UUID id = getListingIdForSlot(slotIndex);
            if (id != null) {
                AuctionState state = AuctionState.get(serverWorld);
                AuctionListing listing = state.getListing(id);
                if (listing != null) {
                    // Buy-now listing: no expiry => click to buy immediately
                    if (listing.expiresGameTime <= 0L) {
                        state.buyListing(serverPlayer, id);
                        rebuildFromAuctionState();
                    }
                    // Timed auction: DO NOT auto-buy → bid via /ah bid instead
                }
            }
            return; // don't allow normal item click behavior on AH slots
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }
}
