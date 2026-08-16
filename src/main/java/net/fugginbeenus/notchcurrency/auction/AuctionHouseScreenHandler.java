package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AuctionHouseScreenHandler extends AbstractContainerMenu {
    public static final int LISTING_COLUMNS = 9;
    public static final int LISTING_ROWS    = 4;
    public static final int LISTING_SIZE    = LISTING_COLUMNS * LISTING_ROWS;
    private static final int SLOT_SIZE        = 18;
    private static final int LISTING_START_X  = 9;
    private static final int LISTING_START_Y  = 28;
    private static final int PLAYER_INV_START_X = 9;
    private static final int PLAYER_INV_START_Y = 127;
    private static final int HOTBAR_START_X = 9;
    private static final int HOTBAR_START_Y = PLAYER_INV_START_Y + 58;
    public static final int POPUP_COLUMNS = 5;
    public static final int POPUP_ROWS    = 2;
    public static final int POPUP_SIZE    = POPUP_COLUMNS * POPUP_ROWS;

    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(Container inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private final SimpleContainer listingsInv   = new SimpleContainer(LISTING_SIZE);
    private final SimpleContainer userPopupInv  = new SimpleContainer(POPUP_SIZE);
    private final UUID[] listingIds = new UUID[LISTING_SIZE];
    private final Inventory playerInv;
    private final Level world;
    private int page       = 0;
    private int totalPages = 1;

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
        NEWEST,
        ENDING_SOON,
        PRICE_DESC,
        PRICE_ASC,
        NAME
    }

    private FilterMode filter   = FilterMode.ALL;
    private SortMode  sortMode  = SortMode.NEWEST;
    private static final int PROP_PAGE        = 0;
    private static final int PROP_TOTAL_PAGES = 1;
    private static final int PROP_FILTER      = 2;
    private static final int PROP_SORT        = 3;

    private final ContainerData properties = new SimpleContainerData(4);

    public AuctionHouseScreenHandler(int containerId, Inventory playerInv) {
        super(ModScreenHandlers.AUCTION_HOUSE, containerId);
        this.playerInv = playerInv;
        this.world     = playerInv.player.level();

        this.addDataSlots(properties);

        int index = 0;
        for (int row = 0; row < LISTING_ROWS; row++) {
            for (int col = 0; col < LISTING_COLUMNS; col++) {
                int x = LISTING_START_X + col * SLOT_SIZE;
                int y = LISTING_START_Y + row * SLOT_SIZE;
                this.addSlot(new ReadOnlySlot(listingsInv, index++, x, y));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = PLAYER_INV_START_X + col * SLOT_SIZE;
                int y = PLAYER_INV_START_Y + row * SLOT_SIZE;
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, x, y));
            }
        }

        for (int col = 0; col < 9; col++) {
            int x = HOTBAR_START_X + col * SLOT_SIZE;
            int y = HOTBAR_START_Y;
            this.addSlot(new Slot(playerInv, col, x, y));
        }

        for (int i = 0; i < POPUP_SIZE; i++) {
            this.addSlot(new ReadOnlySlot(userPopupInv, i, -10000, -10000));
        }

        reload();
    }

    private void syncProperties() {
        if (world != null && !world.isClientSide) {
            properties.set(PROP_PAGE, page);
            properties.set(PROP_TOTAL_PAGES, totalPages);
            properties.set(PROP_FILTER, filter.ordinal());
            properties.set(PROP_SORT, sortMode.ordinal());
        }
    }

    private boolean matchesFilter(AuctionListing listing) {
        if (filter == FilterMode.ALL) {
            return true;
        }

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

    public int getPage() {
        if (world != null && world.isClientSide) {
            return properties.get(PROP_PAGE);
        }
        return page;
    }

    public int getTotalPages() {
        if (world != null && world.isClientSide) {
            return Math.max(1, properties.get(PROP_TOTAL_PAGES));
        }
        return Math.max(1, totalPages);
    }

    private void setTotalPages(int total) {
        this.totalPages = Math.max(1, total);
    }

    public void nextPage() {
        if (world != null && world.isClientSide) return;
        if (page < getTotalPages() - 1) {
            page++;
            reloadPageContents();
        }
    }

    public void prevPage() {
        if (world != null && world.isClientSide) return;
        if (page > 0) {
            page--;
            reloadPageContents();
        }
    }

    public SimpleContainer getListingsInventory() {
        return listingsInv;
    }

    public void reload() {
        if (!(world instanceof ServerLevel)) {
            return;
        }
        rebuildFromAuctionState();
    }

    public void toggleMyListings() {
        if (world != null && world.isClientSide) return;
        showMyListings = !showMyListings;
        reload();
    }

    public boolean isShowingMyListings() {
        return showMyListings;
    }

    public void cycleFilter() {
        if (world != null && world.isClientSide) return;

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
        if (world != null && world.isClientSide) {
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
        if (world != null && world.isClientSide) return;
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
        if (world != null && world.isClientSide) {
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

    public SimpleContainer getUserPopupInventory() {
        return userPopupInv;
    }

    public UUID getListingIdForSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= LISTING_SIZE) return null;
        return listingIds[slotIndex];
    }

    private void clearInventoriesClientSide() {
        for (int i = 0; i < listingsInv.getContainerSize(); i++) {
            listingsInv.setItem(i, ItemStack.EMPTY);
            listingIds[i] = null;
        }
        for (int i = 0; i < userPopupInv.getContainerSize(); i++) {
            userPopupInv.setItem(i, ItemStack.EMPTY);
        }
    }

    private void rebuildFromAuctionState() {
        if (!(world instanceof ServerLevel serverWorld)) {
            clearInventoriesClientSide();
            return;
        }

        AuctionState state = AuctionState.get(serverWorld);
        List<AuctionListing> allListings = new ArrayList<>(state.getListings());
        List<AuctionListing> categoryFiltered = new ArrayList<>();
        for (AuctionListing l : allListings) {
            if (matchesFilter(l)) {
                categoryFiltered.add(l);
            }
        }
        allListings = categoryFiltered;
        List<AuctionListing> mainList = allListings;
        if (showMyListings) {
            UUID me = playerInv.player.getUUID();
            mainList = new ArrayList<>();
            for (AuctionListing l : allListings) {
                if (l.sellerUuid.equals(me)) {
                    mainList.add(l);
                }
            }
        }

        Comparator<AuctionListing> cmp = switch (sortMode) {
            case PRICE_ASC   -> Comparator.comparingLong(l -> l.price);
            case PRICE_DESC  -> Comparator.comparingLong((AuctionListing l) -> l.price).reversed();
            case NEWEST      -> Comparator.comparingLong((AuctionListing l) -> l.createdGameTime).reversed();
            case ENDING_SOON -> Comparator.comparingLong(l -> l.expiresGameTime);
            case NAME        -> Comparator.comparing(l -> l.stack.getHoverName().getString());
        };
        mainList.sort(cmp);

        int total = (mainList.size() + LISTING_SIZE - 1) / LISTING_SIZE;
        setTotalPages(total == 0 ? 1 : total);

        if (page >= totalPages) {
            page = totalPages - 1;
        }
        if (page < 0) page = 0;

        for (int i = 0; i < listingsInv.getContainerSize(); i++) {
            listingsInv.setItem(i, ItemStack.EMPTY);
            listingIds[i] = null;
        }

        if (!mainList.isEmpty()) {
            int start = page * LISTING_SIZE;
            for (int i = 0; i < LISTING_SIZE && (start + i) < mainList.size(); i++) {
                AuctionListing listing = mainList.get(start + i);
                listingsInv.setItem(i, makeDisplayStack(listing));
                listingIds[i] = listing.id;
            }
        }

        UUID me = playerInv.player.getUUID();
        List<AuctionListing> mine = new ArrayList<>();
        for (AuctionListing l : allListings) {
            if (l.sellerUuid.equals(me)) {
                mine.add(l);
            }
        }
        mine.sort(cmp);

        for (int i = 0; i < userPopupInv.getContainerSize(); i++) {
            userPopupInv.setItem(i, ItemStack.EMPTY);
        }
        int max = Math.min(userPopupInv.getContainerSize(), mine.size());
        for (int i = 0; i < max; i++) {
            userPopupInv.setItem(i, mine.get(i).stack.copy());
        }

        syncProperties();
    }

    private void reloadPageContents() {
        reload();
    }

    private ItemStack makeDisplayStack(AuctionListing listing) {
        ItemStack base = listing.stack.copy();
        base.setCount(listing.stack.getCount());
        CompoundTag tag = StackData.editData(base);
        tag.putLong("nc_price", listing.price);
        tag.putString("nc_seller", listing.sellerName);
        tag.putLong("nc_created", listing.createdGameTime);
        tag.putLong("nc_expires", listing.expiresGameTime);
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(tag, "nc_listing_id", listing.id);

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

        java.util.List<Component> loreLines = new java.util.ArrayList<>();
        loreLines.add(Component.literal("Price: " + listing.price + " ").withStyle(ChatFormatting.GOLD));
        if (listing.highestBid > 0) {
            loreLines.add(Component.literal("Highest bid: " + listing.highestBid + " ").withStyle(ChatFormatting.YELLOW));
        }
        loreLines.add(Component.literal("Seller: " + listing.sellerName).withStyle(ChatFormatting.GRAY));
        loreLines.add(Component.literal("Click to buy / bid").withStyle(ChatFormatting.YELLOW));

        //? if >=1.21 {
        /*base.set(net.minecraft.core.component.DataComponents.LORE,
                new net.minecraft.world.item.component.ItemLore(loreLines));
        *///?} else {
        CompoundTag display = base.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        for (Component line : loreLines) lore.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        display.put("Lore", lore);
        //?}
        return base;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        switch (id) {
            case 0 -> prevPage();
            case 1 -> nextPage();
            case 2 -> cycleFilter();
            case 3 -> cycleSortMode();
            case 4 -> reload();
            case 5 -> net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager.openScreen((ServerPlayer) player);
            case 6 -> AuctionListingScreenHandler.open((ServerPlayer) player);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < LISTING_SIZE) {
            return ItemStack.EMPTY;
        }

        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            newStack = stackInSlot.copy();

            int ahEnd      = LISTING_SIZE;
            int invEnd     = ahEnd + 27;
            int hotbarEnd  = invEnd + 9;

            if (index < invEnd) {
                if (!this.moveItemStackTo(stackInSlot, ahEnd + 27, hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stackInSlot, ahEnd, invEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < LISTING_SIZE) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                super.clicked(slotIndex, button, actionType, player);
                return;
            }
            if (!(world instanceof ServerLevel serverWorld)) {
                super.clicked(slotIndex, button, actionType, player);
                return;
            }

            UUID id = getListingIdForSlot(slotIndex);
            if (id != null) {
                AuctionState state = AuctionState.get(serverWorld);
                AuctionListing listing = state.getListing(id);
                if (listing != null) {
                    if (listing.expiresGameTime <= 0L) {
                        state.buyListing(serverPlayer, id);
                        rebuildFromAuctionState();
                    }
                }
            }
            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }
}
