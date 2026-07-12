package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Backing handler for the buyer-side shop browser. Listings are synced as read-only data-carrier
 * display stacks (bounty-board pattern: the sale item with listing id / price / barter / stock in
 * NBT), refreshed every tick so stock stays live while people shop. Pagination unlocks all 27
 * listings — the old screen showed only the first 6. Purchases go through the existing
 * SHOP_PURCHASE packet and PlayerShopManager.purchase().
 */
public class ShopBrowseScreenHandler extends ScreenHandler {

    // Carrier slots live OFF-screen (the row-list screen reads rowStack(i) and draws icons itself).
    public static final int VIS_ROWS = 6, PER_PAGE = VIS_ROWS;

    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_COUNT = 2, P_STATUS = 3;
    public static final int STATUS_CLOSED = 0, STATUS_OPEN = 1, STATUS_RENT_PAUSED = 2;

    private final UUID shopId;
    private final String shopName;
    private final String greeting;
    @Nullable private final UUID npcId; // linked NPC, for the client-side preview
    @Nullable private final PlayerShop shop; // server side only
    private final SimpleInventory rowInv = new SimpleInventory(PER_PAGE);
    private final PropertyDelegate props = new ArrayPropertyDelegate(4);
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) { return false; }
        @Override public boolean canTakeItems(PlayerEntity p) { return false; }
    }

    /** Read the optional linked-NPC uuid appended by NpcShopLogic (used in the client ctor's this()). */
    static UUID readNpcId(PacketByteBuf buf) {
        return buf.readBoolean() ? buf.readUuid() : null;
    }

    /** Client constructor: the opening buf carries the shop identity. */
    public ShopBrowseScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        this(syncId, inv, buf.readUuid(), buf.readString(64), buf.readString(256), readNpcId(buf), null);
    }

    /** Server constructor: holds the live shop for per-tick refresh. */
    public ShopBrowseScreenHandler(int syncId, PlayerInventory inv, UUID shopId, String shopName,
                                   String greeting, @Nullable UUID npcId, @Nullable PlayerShop shop) {
        super(ModScreenHandlers.SHOP_BROWSE, syncId);
        this.shopId = shopId;
        this.shopName = shopName;
        this.greeting = greeting;
        this.npcId = npcId;
        this.shop = shop;
        this.addProperties(props);
        for (int i = 0; i < PER_PAGE; i++) {
            this.addSlot(new ReadOnlySlot(rowInv, i, -10000, -10000));
        }
        // Player inventory (vanilla trade layout — coords match the code-drawn ShopBrowseScreen).
        final int invX = 43, invY = 158;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, invX + col * 18, invY + 58));
        }
        refresh();
    }

    public UUID shopId() { return shopId; }
    public String shopName() { return shopName; }
    public String greeting() { return greeting; }
    @Nullable public UUID npcId() { return npcId; }
    public ItemStack rowStack(int i) { return rowInv.getStack(i); }
    public int prop(int i) { return props.get(i); }

    private void refresh() {
        if (shop == null) return; // client side
        props.set(P_STATUS, !shop.isOpen() ? STATUS_CLOSED
                : shop.isRentPaused() ? STATUS_RENT_PAUSED : STATUS_OPEN);

        List<ShopListing> listings = shop.getListings();
        int totalPages = Math.max(1, (listings.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);
        props.set(P_COUNT, listings.size());

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            rowInv.setStack(i, idx < listings.size() ? displayStack(listings.get(idx)) : ItemStack.EMPTY);
        }
    }

    /** The sale item, carrying everything a row needs to render and buy. Shared with the manage hub.
     *  Keeps the per-sale count so the row icon shows vanilla stacking numbers, and embeds the full
     *  barter stack so the row can draw its icon like a villager trade ingredient. */
    static ItemStack displayStack(ShopListing listing) {
        ItemStack carrier = listing.getItemForSale().copy();
        if (carrier.isEmpty()) return ItemStack.EMPTY;
        NbtCompound t = StackData.editData(carrier);
        t.putUuid("nc_lid", listing.getId());
        t.putInt("nc_price", listing.getCoinPrice());
        t.putString("nc_bname", listing.acceptsBarter() ? listing.getItemPrice().getName().getString() : "");
        t.putInt("nc_bcount", listing.acceptsBarter() ? listing.getItemPriceCount() : 0);
        t.putInt("nc_stock", listing.getStockQuantitySafe());
        if (listing.acceptsBarter()) {
            ItemStack bs = listing.getItemPrice().copy();
            bs.setCount(Math.max(1, Math.min(64, listing.getItemPriceCount())));
            t.put("nc_bstack", StackData.writeStack(bs));
        }
        StackData.commitData(carrier, t);
        return carrier;
    }

    @Override
    public void sendContentUpdates() {
        refresh();
        super.sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity)) return false;
        if (id == 0) page = Math.max(0, page - 1);
        else if (id == 1) page = page + 1; // clamped in refresh()
        else return false;
        refresh();
        sendContentUpdates();
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
