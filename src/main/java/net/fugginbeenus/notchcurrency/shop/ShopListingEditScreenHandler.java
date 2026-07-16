package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Backing handler for the listing editor. The SALE and BARTER slots hold SAMPLES — copied into the
 * listing on save and always handed back on close; nothing is consumed or created by them, so there
 * is no dupe or loss path. The STOCK bin is a real intake slot: matching stacks dropped in are
 * pulled into the listing's stock count each tick (and cleared); anything that doesn't match just
 * sits there and returns on close. Every action applies immediately server-side (SHOP_EDIT_ACTION).
 */
public class ShopListingEditScreenHandler extends ScreenHandler {

    public static final int SALE_X = 12, SALE_Y = 24;
    public static final int BARTER_X = 12, BARTER_Y = 72;
    public static final int STOCK_X = 12, STOCK_Y = 110;
    public static final int INV_X = 24, INV_Y = 158, HOTBAR_Y = 216;

    // Sample/intake slot indices.
    public static final int SLOT_SALE = 0, SLOT_BARTER = 1, SLOT_STOCK = 2, SLOT_COUNT = 3;

    public static final int P_STOCK = 0, P_PRICE = 1, P_HAS_LISTING = 2;
    private static final int PROP_COUNT = 3;

    // SHOP_EDIT_ACTION ids.
    public static final int ACTION_SAVE = 0, ACTION_DEPOSIT = 1, ACTION_RETURN_STOCK = 2,
            ACTION_DELETE = 3, ACTION_BACK = 4, ACTION_CLEAR_BARTER = 5;

    private final PlayerInventory playerInv;
    private final SimpleInventory samples = new SimpleInventory(SLOT_COUNT);
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);

    // Initial display seed (from the opening buf on the client).
    private final String currentSaleDesc;
    private final String currentBarterDesc;

    @Nullable private final PlayerShop shop; // server side only
    @Nullable private UUID listingId;        // null until first save on a new listing

    /** Client constructor. */
    public ShopListingEditScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        this(syncId, inv, buf.readBoolean(), buf.readString(64), buf.readString(64),
                buf.readVarInt(), buf.readVarInt(), null, null);
    }

    /** Server constructor. */
    public ShopListingEditScreenHandler(int syncId, PlayerInventory inv, boolean hasListing,
                                        String saleDesc, String barterDesc, int price, int stock,
                                        @Nullable PlayerShop shop, @Nullable UUID listingId) {
        super(ModScreenHandlers.SHOP_LISTING_EDIT, syncId);
        this.playerInv = inv;
        this.shop = shop;
        this.listingId = hasListing ? listingId : null;
        this.currentSaleDesc = saleDesc;
        this.currentBarterDesc = barterDesc;
        this.addProperties(props);
        props.set(P_HAS_LISTING, hasListing ? 1 : 0);
        props.set(P_PRICE, price);
        props.set(P_STOCK, stock);

        addSlot(new Slot(samples, SLOT_SALE, SALE_X, SALE_Y));
        addSlot(new Slot(samples, SLOT_BARTER, BARTER_X, BARTER_Y));
        addSlot(new Slot(samples, SLOT_STOCK, STOCK_X, STOCK_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    /** Open the editor for a listing (null id = create a new one). */
    public static void open(ServerPlayerEntity sp, PlayerShop shop, @Nullable UUID listingId) {
        ShopListing listing = listingId == null ? null : shop.getListing(listingId);
        boolean has = listing != null;
        String saleDesc = has ? listing.getItemForSale().getCount() + "×"
                + listing.getItemForSale().getName().getString() : "";
        String barterDesc = has && listing.acceptsBarter() ? listing.getItemPriceCount() + "×"
                + listing.getItemPrice().getName().getString() : "";
        int price = has ? listing.getCoinPrice() : 0;
        int stock = has ? listing.getStockQuantitySafe() : 0;
        UUID id = has ? listing.getId() : null;

        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(sp, Text.literal(has ? "Edit Listing" : "New Listing"),
                (syncId, inv, p) -> new ShopListingEditScreenHandler(syncId, inv, has, saleDesc, barterDesc,
                        price, stock, shop, id),
                buf -> {
                    buf.writeBoolean(has);
                    buf.writeString(saleDesc);
                    buf.writeString(barterDesc);
                    buf.writeVarInt(price);
                    buf.writeVarInt(stock);
                });
    }

    public boolean hasListing() { return props.get(P_HAS_LISTING) != 0; }
    public int stockProp() { return props.get(P_STOCK); }
    public int priceProp() { return props.get(P_PRICE); }
    public String currentSaleDesc() { return currentSaleDesc; }
    public String currentBarterDesc() { return currentBarterDesc; }
    public ItemStack saleSample() { return samples.getStack(SLOT_SALE); }
    public ItemStack barterSample() { return samples.getStack(SLOT_BARTER); }
    public ItemStack stockSample() { return samples.getStack(SLOT_STOCK); }

    @Nullable
    private ShopListing listing() {
        return (shop != null && listingId != null) ? shop.getListing(listingId) : null;
    }

    // ---- actions (server side, from the SHOP_EDIT_ACTION packet) ----

    public void handleAction(ServerPlayerEntity sp, int action, int price) {
        if (shop == null) return;
        if (!shop.getOwnerId().equals(sp.getUuid())) return;
        ShopState state = ShopState.get(sp.getServerWorld());
        switch (action) {
            case ACTION_SAVE -> save(sp, price, state);
            case ACTION_DEPOSIT -> deposit(sp, state);
            case ACTION_RETURN_STOCK -> returnStock(sp, state);
            case ACTION_DELETE -> {
                if (listingId != null && PlayerShopManager.removeListing(sp, shop.getShopId(), listingId)) {
                    listingId = null;
                    props.set(P_HAS_LISTING, 0);
                    props.set(P_STOCK, 0);
                    sp.sendMessage(Text.literal("Listing removed — its stock is back in your inventory.")
                            .formatted(Formatting.GREEN), false);
                    NpcShopLogic.openShopManager(sp, shop.getShopId());
                }
            }
            case ACTION_BACK -> NpcShopLogic.openShopManager(sp, shop.getShopId());
            case ACTION_CLEAR_BARTER -> {
                ShopListing l = listing();
                if (l != null) {
                    l.setBarterPrice(ItemStack.EMPTY, 0);
                    state.markDirtyAndSave();
                    sp.sendMessage(Text.literal("Barter price removed.").formatted(Formatting.GREEN), false);
                }
            }
        }
        sendContentUpdates();
    }

    private void save(ServerPlayerEntity sp, int price, ShopState state) {
        price = Math.max(0, Math.min(PlayerShopManager.MAX_PRICE, price));
        ItemStack sale = samples.getStack(0);
        ItemStack barter = samples.getStack(1);
        ShopListing l = listing();

        if (l == null) {
            // Creating: need a sale sample and at least one pricing mode.
            if (sale.isEmpty()) {
                sp.sendMessage(Text.literal("Put a sample of the item you're selling in the top slot.")
                        .formatted(Formatting.RED), false);
                return;
            }
            if (price <= 0 && barter.isEmpty()) {
                sp.sendMessage(Text.literal("Set a coin price and/or a barter item.").formatted(Formatting.RED), false);
                return;
            }
            if (shop.getListings().size() >= PlayerShop.MAX_LISTINGS) {
                sp.sendMessage(Text.literal("This shop is full.").formatted(Formatting.RED), false);
                return;
            }
            ShopListing created = new ShopListing(sale.copy(), 0, price);
            if (!barter.isEmpty()) created.setBarterPrice(barter.copy(), barter.getCount());
            shop.addListing(created);
            listingId = created.getId();
            props.set(P_HAS_LISTING, 1);
            state.markDirtyAndSave();
            sp.sendMessage(Text.literal("Listing created — drop stock into the bin.").formatted(Formatting.GREEN), false);
            return;
        }

        // Editing an existing listing.
        if (!sale.isEmpty() && !StackData.canCombine(sale, l.getItemForSale())) {
            // Swapping the sale item: hand back the old item's stock first so nothing strands.
            int old = l.getStockQuantitySafe();
            if (old > 0) {
                give(sp, l.getItemForSale(), old);
                l.setStock(0);
                sp.sendMessage(Text.literal("Returned " + old + " old stock (the item changed).")
                        .formatted(Formatting.YELLOW), false);
            }
        }
        if (!sale.isEmpty()) l.setItemForSale(sale.copy());
        if (!barter.isEmpty()) l.setBarterPrice(barter.copy(), barter.getCount());
        if (price <= 0 && !l.acceptsBarter() && barter.isEmpty()) {
            sp.sendMessage(Text.literal("A listing needs a coin price and/or a barter item.").formatted(Formatting.RED), false);
            return;
        }
        l.setCoinPrice(price);
        props.set(P_PRICE, price);
        state.markDirtyAndSave();
        sp.sendMessage(Text.literal("Listing saved.").formatted(Formatting.GREEN), false);
    }

    private void deposit(ServerPlayerEntity sp, ShopState state) {
        ShopListing l = listing();
        if (l == null) {
            sp.sendMessage(Text.literal("Save the listing first, then deposit stock.").formatted(Formatting.RED), false);
            return;
        }
        int moved = 0;
        for (int i = 0; i < sp.getInventory().size(); i++) {
            ItemStack st = sp.getInventory().getStack(i);
            if (!st.isEmpty() && StackData.canCombine(st, l.getItemForSale())) {
                moved += st.getCount();
                sp.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
        if (moved == 0) {
            sp.sendMessage(Text.literal("You aren't carrying any matching items.").formatted(Formatting.YELLOW), false);
            return;
        }
        l.addStock(moved);
        state.markDirtyAndSave();
        sp.sendMessage(Text.literal("Deposited " + moved + " into stock.").formatted(Formatting.GREEN), false);
    }

    private void returnStock(ServerPlayerEntity sp, ShopState state) {
        ShopListing l = listing();
        if (l == null) return;
        int stock = l.getStockQuantitySafe();
        if (stock <= 0) {
            sp.sendMessage(Text.literal("There's no stock to take back.").formatted(Formatting.YELLOW), false);
            return;
        }
        l.setStock(0);
        give(sp, l.getItemForSale(), stock);
        state.markDirtyAndSave();
        sp.sendMessage(Text.literal("Returned " + stock + " stock to your inventory.").formatted(Formatting.GREEN), false);
    }

    private static void give(ServerPlayerEntity sp, ItemStack template, int count) {
        while (count > 0) {
            ItemStack chunk = template.copy();
            chunk.setCount(Math.min(count, template.getMaxCount()));
            count -= chunk.getCount();
            sp.getInventory().offerOrDrop(chunk);
        }
    }

    @Override
    public void sendContentUpdates() {
        ShopListing l = listing();
        // Pull any matching stack sitting in the stock bin into the listing's stock, then clear it.
        if (l != null && playerInv.player instanceof ServerPlayerEntity sp && !sp.getWorld().isClient) {
            ItemStack intake = samples.getStack(SLOT_STOCK);
            if (!intake.isEmpty() && StackData.canCombine(intake, l.getItemForSale())) {
                int moved = intake.getCount();
                l.addStock(moved);
                samples.setStack(SLOT_STOCK, ItemStack.EMPTY);
                ShopState.get(sp.getServerWorld()).markDirtyAndSave();
                sp.sendMessage(Text.literal("Added " + moved + " to stock.").formatted(Formatting.GREEN), true);
            }
        }
        if (l != null) {
            props.set(P_STOCK, l.getStockQuantitySafe());
        }
        super.sendContentUpdates();
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Samples always go back — they were never part of the listing.
        if (!player.getWorld().isClient) {
            for (int i = 0; i < samples.size(); i++) {
                ItemStack st = samples.removeStack(i);
                if (!st.isEmpty() && !player.getInventory().insertStack(st)) {
                    player.dropItem(st, false);
                }
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < SLOT_COUNT) {
                // A sample/intake slot → back to the player inventory.
                if (!this.insertItem(stack, SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // Player inventory → the stock bin (shift-click deposits stock).
                if (!this.insertItem(stack, SLOT_STOCK, SLOT_STOCK + 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
