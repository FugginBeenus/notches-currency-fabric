package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Screen handler for the player shop GUI.
 *
 * MANAGE mode uses functional slots for responsive interaction.
 * Security is enforced by validating all changes on close.
 */
public class PlayerShopScreenHandler extends ScreenHandler {

    public enum Mode {
        BROWSE,
        MANAGE
    }

    private final Mode mode;
    private final UUID shopId;
    private final PlayerShop shop;
    private final SimpleInventory shopInventory;
    private final PlayerInventory playerInventory;

    // Client-side data
    private final List<ShopListing> clientListings = new ArrayList<>();
    private String shopName = "";
    private long shopBalance = 0;

    // Client constructor
    public PlayerShopScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(ModScreenHandlers.PLAYER_SHOP, syncId);
        this.playerInventory = playerInventory;
        this.mode = buf.readEnumConstant(Mode.class);
        this.shopId = buf.readUuid();
        this.shop = null;
        this.shopName = buf.readString();

        // Read listings
        int listingCount = buf.readVarInt();
        for (int i = 0; i < listingCount; i++) {
            UUID listingId = buf.readUuid();
            ItemStack itemForSale = buf.readItemStack();
            int stockQuantity = buf.readVarInt();
            int coinPrice = buf.readVarInt();
            boolean acceptsBarter = buf.readBoolean();
            ItemStack barterItem = ItemStack.EMPTY;
            int barterCount = 0;
            if (acceptsBarter) {
                barterItem = buf.readItemStack();
                barterCount = buf.readVarInt();
            }
            clientListings.add(new ShopListing(listingId, itemForSale, stockQuantity, coinPrice, barterItem, barterCount));
        }

        if (mode == Mode.MANAGE) {
            this.shopBalance = buf.readLong();
        }

        // Create inventory for slots
        this.shopInventory = new SimpleInventory(18); // 6 rows * 3 slots
        populateInventory();
        setupSlots();
    }

    // Server constructor
    public PlayerShopScreenHandler(int syncId, PlayerInventory playerInventory,
                                   Mode mode, UUID shopId, @Nullable PlayerShop shop) {
        super(ModScreenHandlers.PLAYER_SHOP, syncId);
        this.playerInventory = playerInventory;
        this.mode = mode;
        this.shopId = shopId;
        this.shop = shop;

        if (shop != null) {
            this.shopName = shop.getShopName();
            this.shopBalance = shop.getPendingBalance();
        }

        this.shopInventory = new SimpleInventory(18);
        if (shop != null) {
            populateFromShop();
        }
        setupSlots();
    }

    private void populateInventory() {
        for (int i = 0; i < shopInventory.size(); i++) {
            shopInventory.setStack(i, ItemStack.EMPTY);
        }

        int slot = 0;
        for (ShopListing listing : clientListings) {
            if (slot >= 6) break;
            int baseIndex = slot * 3;

            // Barter slot
            if (listing.acceptsBarter()) {
                ItemStack barter = listing.getItemPrice().copy();
                barter.setCount(listing.getItemPriceCount());
                shopInventory.setStack(baseIndex, barter);
            }

            // Sale slot - preserve the count (items per purchase)
            ItemStack saleItem = listing.getItemForSale().copy();
            // Count is already set from the listing
            shopInventory.setStack(baseIndex + 1, saleItem);

            // Stock slot - shows available stock
            ItemStack stockItem = listing.getItemForSale().copy();
            stockItem.setCount(Math.min(listing.getStockQuantity(), 64));
            shopInventory.setStack(baseIndex + 2, stockItem);

            slot++;
        }
    }

    private void populateFromShop() {
        for (int i = 0; i < shopInventory.size(); i++) {
            shopInventory.setStack(i, ItemStack.EMPTY);
        }

        if (shop == null) return;

        int slot = 0;
        for (ShopListing listing : shop.getListings()) {
            if (slot >= 6) break;
            int baseIndex = slot * 3;

            if (listing.acceptsBarter()) {
                ItemStack barter = listing.getItemPrice().copy();
                barter.setCount(listing.getItemPriceCount());
                shopInventory.setStack(baseIndex, barter);
            }

            // Sale slot - preserve the count (items per purchase)
            ItemStack saleItem = listing.getItemForSale().copy();
            // Count is already set from the listing
            shopInventory.setStack(baseIndex + 1, saleItem);

            // Stock slot - shows available stock
            ItemStack stockItem = listing.getItemForSale().copy();
            stockItem.setCount(Math.min(listing.getStockQuantity(), 64));
            shopInventory.setStack(baseIndex + 2, stockItem);

            slot++;
        }
    }

    private void setupSlots() {
        if (mode == Mode.MANAGE) {
            // Shop slots - fully interactive
            final int BARTER_SLOT_X = 107;
            final int SALE_SLOT_X = 137;
            final int STOCK_SLOT_X = 168;
            final int[] ROW_Y = {13, 35, 57, 79, 101, 123};

            for (int row = 0; row < 6; row++) {
                addSlot(new Slot(shopInventory, row * 3, BARTER_SLOT_X, ROW_Y[row]));
                addSlot(new Slot(shopInventory, row * 3 + 1, SALE_SLOT_X, ROW_Y[row]));
                addSlot(new Slot(shopInventory, row * 3 + 2, STOCK_SLOT_X, ROW_Y[row]));
            }

            // Player inventory
            final int INV_X = 8;
            final int INV_Y = 153;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
                }
            }

            // Hotbar
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col, INV_X + col * 18, INV_Y + 58));
            }
        } else {
            // Browse mode - just player inventory
            final int INV_X = 8;
            final int INV_Y = 156;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col, INV_X + col * 18, INV_Y + 58));
            }
        }
    }

    public Mode getMode() { return mode; }
    public UUID getShopId() { return shopId; }
    @Nullable public PlayerShop getShop() { return shop; }
    public String getShopName() { return shopName; }
    public long getShopBalance() { return shopBalance; }
    public void setShopBalance(long balance) { this.shopBalance = balance; }

    public List<ShopListing> getListings() {
        return shop != null ? shop.getListings() : clientListings;
    }

    @Nullable
    public ShopListing getListingAtRow(int rowIndex) {
        List<ShopListing> listings = getListings();
        if (rowIndex < 0 || rowIndex >= listings.size()) return null;
        return listings.get(rowIndex);
    }

    public void refreshDisplay() {
        if (shop != null) {
            populateFromShop();
            this.shopBalance = shop.getPendingBalance();
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();

            if (mode == Mode.MANAGE) {
                int shopEnd = 18;
                int playerStart = 18;
                int playerEnd = 54;

                if (slotIndex < shopEnd) {
                    // From shop to player
                    if (!insertItem(stack, playerStart, playerEnd, true)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // From player to shop
                    if (!insertItem(stack, 0, shopEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                int hotbarStart = 27;
                int playerEnd = 36;
                if (slotIndex < hotbarStart) {
                    if (!insertItem(stack, hotbarStart, playerEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!insertItem(stack, 0, hotbarStart, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Items in shop slots stay there - they get synced via packets
        // Don't drop items, don't auto-save - client handles via SHOP_SAVE_LISTINGS
    }
}