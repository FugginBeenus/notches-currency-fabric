package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.economy.ShopRent;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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

public class ShopManageScreenHandler extends AbstractContainerMenu {

    public static final int ROWS = 6;

    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_COUNT = 2, P_OPEN = 3, P_RENT_PAUSED = 4,
            P_PEND_HI = 5, P_PEND_LO = 6, P_BARTER_COUNT = 7, P_RENT_COST = 8, P_UNPAID = 9,
            P_IS_OP = 10, P_ADMIN_MODE = 11;
    private static final int PROP_COUNT = 12;

    public static final int ACTION_RENAME = 0, ACTION_GREETING = 1, ACTION_TOGGLE_OPEN = 2,
            ACTION_EDIT_LISTING = 3, ACTION_NEW_LISTING = 4, ACTION_TOGGLE_ADMIN = 5;

    private final UUID shopId;
    private final String shopName;
    private final String greeting;
    @Nullable private final UUID npcId;
    @Nullable private final PlayerShop shop;
    private final SimpleContainer rowInv = new SimpleContainer(ROWS);
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);
    private final net.minecraft.world.entity.player.Player viewer;
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }

    public ShopManageScreenHandler(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, buf.readUUID(), buf.readUtf(64), buf.readUtf(256),
                ShopBrowseScreenHandler.readNpcId(buf), null);
    }

    public ShopManageScreenHandler(int containerId, Inventory inv, UUID shopId, String shopName,
                                   String greeting, @Nullable UUID npcId, @Nullable PlayerShop shop) {
        super(ModScreenHandlers.SHOP_MANAGE, containerId);
        this.shopId = shopId;
        this.shopName = shopName;
        this.greeting = greeting;
        this.npcId = npcId;
        this.shop = shop;
        this.viewer = inv.player;
        this.addDataSlots(props);
        for (int i = 0; i < ROWS; i++) {
            this.addSlot(new ReadOnlySlot(rowInv, i, -10000, -10000));
        }
        refresh();
    }

    public UUID shopId() { return shopId; }
    public String shopName() { return shopName; }
    public String greeting() { return greeting; }
    @Nullable public UUID npcId() { return npcId; }
    public ItemStack rowStack(int i) { return rowInv.getItem(i); }
    public int prop(int i) { return props.get(i); }

    public long pendingBalance() {
        return ((long) props.get(P_PEND_HI) << 32) | (props.get(P_PEND_LO) & 0xFFFFFFFFL);
    }

    private void refresh() {
        if (shop == null) return;
        props.set(P_OPEN, shop.isOpen() ? 1 : 0);
        props.set(P_RENT_PAUSED, shop.isRentPaused() ? 1 : 0);
        long pending = shop.getPendingBalance();
        props.set(P_PEND_HI, (int) (pending >>> 32));
        props.set(P_PEND_LO, (int) pending);
        props.set(P_BARTER_COUNT, shop.getPendingBarterCount());
        props.set(P_RENT_COST, (int) Math.min(Integer.MAX_VALUE, ShopRent.rentFor(shop.getListings().size())));
        props.set(P_UNPAID, shop.getUnpaidRentCycles());
        props.set(P_ADMIN_MODE, shop.isAdminMode() ? 1 : 0);
        boolean op = viewer instanceof net.minecraft.server.level.ServerPlayer sp
                && net.fugginbeenus.notchcurrency.compat.Perms.isOperator(sp);
        props.set(P_IS_OP, op ? 1 : 0);

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
            rowInv.setItem(i, idx < listings.size()
                    ? ShopBrowseScreenHandler.displayStack(listings.get(idx), shop.isAdminMode()) : ItemStack.EMPTY);
        }
    }

    public void handleAction(ServerPlayer sp, int action, String text, @Nullable UUID listingId) {
        if (shop == null) return;
        if (!PlayerShopManager.canManage(sp, shop)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Only the shop owner can manage this shop.").withStyle(ChatFormatting.RED));
            return;
        }
        ShopState state = ShopState.get(sp.serverLevel());
        switch (action) {
            case ACTION_TOGGLE_ADMIN -> {
                if (!net.fugginbeenus.notchcurrency.compat.Perms.isOperator(sp)) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Only a server operator can do that.").withStyle(ChatFormatting.RED));
                    return;
                }
                shop.setAdminMode(!shop.isAdminMode());
                state.markDirtyAndSave();
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, shop.isAdminMode()
                        ? Component.literal("This shop is now an admin shop. Stock never runs out and nobody is paid.").withStyle(ChatFormatting.GOLD)
                        : Component.literal("This shop is back to a normal player shop.").withStyle(ChatFormatting.GREEN));
                refresh();
            }
            case ACTION_RENAME -> {
                String name = clean(text, 32);
                if (name.isEmpty()) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Give the shop a name first.").withStyle(ChatFormatting.RED));
                    return;
                }
                shop.setShopName(name);
                state.markDirtyAndSave();
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Shop renamed to '" + name + "'.").withStyle(ChatFormatting.GREEN));
            }
            case ACTION_GREETING -> {
                shop.setShopkeeperDialog(clean(text, 128));
                state.markDirtyAndSave();
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Greeting updated.").withStyle(ChatFormatting.GREEN));
            }
            case ACTION_TOGGLE_OPEN -> {
                shop.setOpen(!shop.isOpen());
                state.markDirtyAndSave();
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(shop.isOpen() ? "Shop is now open for business."
                        : "Shop closed - nobody can buy until you reopen it.")
                        .withStyle(shop.isOpen() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            }
            case ACTION_EDIT_LISTING -> {
                if (listingId != null && shop.getListing(listingId) != null) {
                    ShopListingEditScreenHandler.open(sp, shop, listingId);
                }
            }
            case ACTION_NEW_LISTING -> {
                if (shop.getListings().size() >= PlayerShop.MAX_LISTINGS) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This shop is full (" + PlayerShop.MAX_LISTINGS + " listings).")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                ShopListingEditScreenHandler.open(sp, shop, null);
            }
        }
        refresh();
        broadcastChanges();
    }

    private static String clean(String s, int max) {
        String out = (s == null ? "" : s).replace("§", "").trim();
        return out.length() > max ? out.substring(0, max) : out;
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
