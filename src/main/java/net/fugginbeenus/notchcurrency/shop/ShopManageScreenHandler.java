package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.economy.ShopRent;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Backing handler for the owner-side shop hub: earnings, name/greeting, open toggle, rent status,
 * and the paginated listing list. Every action applies immediately server-side via
 * SHOP_MANAGE_ACTION — nothing is held until close (the old screen's save-on-close lost edits on
 * disconnect). Listing rows ride the same data-carrier slots as the browse screen.
 */
public class ShopManageScreenHandler extends ScreenHandler {

    public static final int ROWS = 6;

    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_COUNT = 2, P_OPEN = 3, P_RENT_PAUSED = 4,
            P_PEND_HI = 5, P_PEND_LO = 6, P_BARTER_COUNT = 7, P_RENT_COST = 8, P_UNPAID = 9;
    private static final int PROP_COUNT = 10;

    // SHOP_MANAGE_ACTION ids.
    public static final int ACTION_RENAME = 0, ACTION_GREETING = 1, ACTION_TOGGLE_OPEN = 2,
            ACTION_EDIT_LISTING = 3, ACTION_NEW_LISTING = 4;

    private final UUID shopId;
    private final String shopName;
    private final String greeting;
    @Nullable private final PlayerShop shop; // server side only
    private final SimpleInventory rowInv = new SimpleInventory(ROWS);
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) { return false; }
        @Override public boolean canTakeItems(PlayerEntity p) { return false; }
    }

    /** Client constructor: the opening buf carries the shop identity. */
    public ShopManageScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        this(syncId, inv, buf.readUuid(), buf.readString(64), buf.readString(256), null);
    }

    /** Server constructor. */
    public ShopManageScreenHandler(int syncId, PlayerInventory inv, UUID shopId, String shopName,
                                   String greeting, @Nullable PlayerShop shop) {
        super(ModScreenHandlers.SHOP_MANAGE, syncId);
        this.shopId = shopId;
        this.shopName = shopName;
        this.greeting = greeting;
        this.shop = shop;
        this.addProperties(props);
        for (int i = 0; i < ROWS; i++) {
            this.addSlot(new ReadOnlySlot(rowInv, i, -10000, -10000));
        }
        refresh();
    }

    public UUID shopId() { return shopId; }
    public String shopName() { return shopName; }
    public String greeting() { return greeting; }
    public ItemStack rowStack(int i) { return rowInv.getStack(i); }
    public int prop(int i) { return props.get(i); }

    public long pendingBalance() {
        return ((long) props.get(P_PEND_HI) << 32) | (props.get(P_PEND_LO) & 0xFFFFFFFFL);
    }

    private void refresh() {
        if (shop == null) return; // client side
        props.set(P_OPEN, shop.isOpen() ? 1 : 0);
        props.set(P_RENT_PAUSED, shop.isRentPaused() ? 1 : 0);
        long pending = shop.getPendingBalance();
        props.set(P_PEND_HI, (int) (pending >>> 32));
        props.set(P_PEND_LO, (int) pending);
        props.set(P_BARTER_COUNT, shop.getPendingBarterCount());
        props.set(P_RENT_COST, (int) Math.min(Integer.MAX_VALUE, ShopRent.rentFor(shop.getListings().size())));
        props.set(P_UNPAID, shop.getUnpaidRentCycles());

        List<ShopListing> listings = shop.getListings();
        int totalPages = Math.max(1, (listings.size() + ROWS - 1) / ROWS);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);
        props.set(P_COUNT, listings.size());

        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            rowInv.setStack(i, idx < listings.size()
                    ? ShopBrowseScreenHandler.displayStack(listings.get(idx)) : ItemStack.EMPTY);
        }
    }

    // ---- actions (server side, from the SHOP_MANAGE_ACTION packet) ----

    public void handleAction(ServerPlayerEntity sp, int action, String text, @Nullable UUID listingId) {
        if (shop == null) return;
        if (!shop.getOwnerId().equals(sp.getUuid())) {
            sp.sendMessage(Text.literal("Only the shop owner can manage this shop.").formatted(Formatting.RED), false);
            return;
        }
        ShopState state = ShopState.get(sp.getServerWorld());
        switch (action) {
            case ACTION_RENAME -> {
                String name = clean(text, 32);
                if (name.isEmpty()) {
                    sp.sendMessage(Text.literal("Give the shop a name first.").formatted(Formatting.RED), false);
                    return;
                }
                shop.setShopName(name);
                state.markDirtyAndSave();
                sp.sendMessage(Text.literal("Shop renamed to '" + name + "'.").formatted(Formatting.GREEN), false);
            }
            case ACTION_GREETING -> {
                shop.setShopkeeperDialog(clean(text, 128));
                state.markDirtyAndSave();
                sp.sendMessage(Text.literal("Greeting updated.").formatted(Formatting.GREEN), false);
            }
            case ACTION_TOGGLE_OPEN -> {
                shop.setOpen(!shop.isOpen());
                state.markDirtyAndSave();
                sp.sendMessage(Text.literal(shop.isOpen() ? "Shop is now open for business."
                        : "Shop closed — nobody can buy until you reopen it.")
                        .formatted(shop.isOpen() ? Formatting.GREEN : Formatting.YELLOW), false);
            }
            case ACTION_EDIT_LISTING -> {
                if (listingId != null && shop.getListing(listingId) != null) {
                    ShopListingEditScreenHandler.open(sp, shop, listingId);
                }
            }
            case ACTION_NEW_LISTING -> {
                if (shop.getListings().size() >= PlayerShop.MAX_LISTINGS) {
                    sp.sendMessage(Text.literal("This shop is full (" + PlayerShop.MAX_LISTINGS + " listings).")
                            .formatted(Formatting.RED), false);
                    return;
                }
                ShopListingEditScreenHandler.open(sp, shop, null);
            }
        }
        refresh();
        sendContentUpdates();
    }

    /** Strip formatting codes and trim — greetings/names are client strings. */
    private static String clean(String s, int max) {
        String out = (s == null ? "" : s).replace("§", "").trim();
        return out.length() > max ? out.substring(0, max) : out;
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
