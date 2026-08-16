package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ShopBrowseScreenHandler extends AbstractContainerMenu {

    public static final int VIS_ROWS = 6, PER_PAGE = VIS_ROWS;

    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_COUNT = 2, P_STATUS = 3;
    public static final int STATUS_CLOSED = 0, STATUS_OPEN = 1, STATUS_RENT_PAUSED = 2;

    private final UUID shopId;
    private final String shopName;
    private final String greeting;
    @Nullable private final UUID npcId;
    @Nullable private final PlayerShop shop;
    private final SimpleContainer rowInv = new SimpleContainer(PER_PAGE);
    private final ContainerData props = new SimpleContainerData(4);
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }

    static UUID readNpcId(FriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUUID() : null;
    }

    public ShopBrowseScreenHandler(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, buf.readUUID(), buf.readUtf(64), buf.readUtf(256), readNpcId(buf), null);
    }

    public ShopBrowseScreenHandler(int containerId, Inventory inv, UUID shopId, String shopName,
                                   String greeting, @Nullable UUID npcId, @Nullable PlayerShop shop) {
        super(ModScreenHandlers.SHOP_BROWSE, containerId);
        this.shopId = shopId;
        this.shopName = shopName;
        this.greeting = greeting;
        this.npcId = npcId;
        this.shop = shop;
        this.addDataSlots(props);
        for (int i = 0; i < PER_PAGE; i++) {
            this.addSlot(new ReadOnlySlot(rowInv, i, -10000, -10000));
        }
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
    public ItemStack rowStack(int i) { return rowInv.getItem(i); }
    public int prop(int i) { return props.get(i); }

    private void refresh() {
        if (shop == null) return;
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
            rowInv.setItem(i, idx < listings.size() ? displayStack(listings.get(idx)) : ItemStack.EMPTY);
        }
    }

    static ItemStack displayStack(ShopListing listing) {
        ItemStack carrier = listing.getItemForSale().copy();
        if (carrier.isEmpty()) return ItemStack.EMPTY;
        CompoundTag t = StackData.editData(carrier);
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(t, "nc_lid", listing.getId());
        t.putInt("nc_price", listing.getCoinPrice());
        t.putString("nc_bname", listing.acceptsBarter() ? listing.getItemPrice().getHoverName().getString() : "");
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
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer)) return false;
        if (id == 0) page = Math.max(0, page - 1);
        else if (id == 1) page = page + 1;
        else return false;
        refresh();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
