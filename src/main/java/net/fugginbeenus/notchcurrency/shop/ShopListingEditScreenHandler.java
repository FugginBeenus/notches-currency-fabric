package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ShopListingEditScreenHandler extends AbstractContainerMenu {

    public static final int SALE_X = 12, SALE_Y = 24;
    public static final int BARTER_X = 12, BARTER_Y = 72;
    public static final int STOCK_X = 12, STOCK_Y = 110;
    public static final int INV_X = 24, INV_Y = 158, HOTBAR_Y = 216;
    public static final int SLOT_SALE = 0, SLOT_BARTER = 1, SLOT_STOCK = 2, SLOT_COUNT = 3;
    public static final int P_STOCK = 0, P_PRICE = 1, P_HAS_LISTING = 2;
    private static final int PROP_COUNT = 3;
    public static final int ACTION_SAVE = 0, ACTION_DEPOSIT = 1, ACTION_RETURN_STOCK = 2,
            ACTION_DELETE = 3, ACTION_BACK = 4, ACTION_CLEAR_BARTER = 5;

    private final Inventory playerInv;
    private final SimpleContainer samples = new SimpleContainer(SLOT_COUNT);
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);
    private final String currentSaleDesc;
    private final String currentBarterDesc;

    @Nullable private final PlayerShop shop;
    @Nullable private UUID listingId;

    public ShopListingEditScreenHandler(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, buf.readBoolean(), buf.readUtf(64), buf.readUtf(64),
                buf.readVarInt(), buf.readVarInt(), null, null);
    }

    public ShopListingEditScreenHandler(int containerId, Inventory inv, boolean hasListing,
                                        String saleDesc, String barterDesc, int price, int stock,
                                        @Nullable PlayerShop shop, @Nullable UUID listingId) {
        super(ModScreenHandlers.SHOP_LISTING_EDIT, containerId);
        this.playerInv = inv;
        this.shop = shop;
        this.listingId = hasListing ? listingId : null;
        this.currentSaleDesc = saleDesc;
        this.currentBarterDesc = barterDesc;
        this.addDataSlots(props);
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

    public static void open(ServerPlayer sp, PlayerShop shop, @Nullable UUID listingId) {
        ShopListing listing = listingId == null ? null : shop.getListing(listingId);
        boolean has = listing != null;
        String saleDesc = has ? listing.getItemForSale().getCount() + "×"
                + listing.getItemForSale().getHoverName().getString() : "";
        String barterDesc = has && listing.acceptsBarter() ? listing.getItemPriceCount() + "×"
                + listing.getItemPrice().getHoverName().getString() : "";
        int price = has ? listing.getCoinPrice() : 0;
        int stock = has ? listing.getStockQuantitySafe() : 0;
        UUID id = has ? listing.getId() : null;

        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(sp, Component.literal(has ? "Edit Listing" : "New Listing"),
                (containerId, inv, p) -> new ShopListingEditScreenHandler(containerId, inv, has, saleDesc, barterDesc,
                        price, stock, shop, id),
                buf -> {
                    buf.writeBoolean(has);
                    buf.writeUtf(saleDesc);
                    buf.writeUtf(barterDesc);
                    buf.writeVarInt(price);
                    buf.writeVarInt(stock);
                });
    }

    public boolean hasListing() { return props.get(P_HAS_LISTING) != 0; }
    public int stockProp() { return props.get(P_STOCK); }
    public int priceProp() { return props.get(P_PRICE); }
    public String currentSaleDesc() { return currentSaleDesc; }
    public String currentBarterDesc() { return currentBarterDesc; }
    public ItemStack saleSample() { return samples.getItem(SLOT_SALE); }
    public ItemStack barterSample() { return samples.getItem(SLOT_BARTER); }
    public ItemStack stockSample() { return samples.getItem(SLOT_STOCK); }

    @Nullable
    private ShopListing listing() {
        return (shop != null && listingId != null) ? shop.getListing(listingId) : null;
    }

    public void handleAction(ServerPlayer sp, int action, int price) {
        if (shop == null) return;
        if (!shop.getOwnerId().equals(sp.getUUID())) return;
        ShopState state = ShopState.get(sp.serverLevel());
        switch (action) {
            case ACTION_SAVE -> save(sp, price, state);
            case ACTION_DEPOSIT -> deposit(sp, state);
            case ACTION_RETURN_STOCK -> returnStock(sp, state);
            case ACTION_DELETE -> {
                if (listingId != null && PlayerShopManager.removeListing(sp, shop.getShopId(), listingId)) {
                    listingId = null;
                    props.set(P_HAS_LISTING, 0);
                    props.set(P_STOCK, 0);
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Listing removed - its stock is back in your inventory.")
                            .withStyle(ChatFormatting.GREEN));
                    NpcShopLogic.openShopManager(sp, shop.getShopId());
                }
            }
            case ACTION_BACK -> NpcShopLogic.openShopManager(sp, shop.getShopId());
            case ACTION_CLEAR_BARTER -> {
                ShopListing l = listing();
                if (l != null) {
                    l.setBarterPrice(ItemStack.EMPTY, 0);
                    state.markDirtyAndSave();
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Barter price removed.").withStyle(ChatFormatting.GREEN));
                }
            }
        }
        broadcastChanges();
    }

    private void save(ServerPlayer sp, int price, ShopState state) {
        price = Math.max(0, Math.min(PlayerShopManager.MAX_PRICE, price));
        ItemStack sale = samples.getItem(0);
        ItemStack barter = samples.getItem(1);
        ShopListing l = listing();

        if (l == null) {
            if (sale.isEmpty()) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Put a sample of the item you're selling in the top slot.")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            if (price <= 0 && barter.isEmpty()) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Set a coin price and/or a barter item.").withStyle(ChatFormatting.RED));
                return;
            }
            if (shop.getListings().size() >= PlayerShop.MAX_LISTINGS) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This shop is full.").withStyle(ChatFormatting.RED));
                return;
            }
            ShopListing created = new ShopListing(sale.copy(), 0, price);
            if (!barter.isEmpty()) created.setBarterPrice(barter.copy(), barter.getCount());
            shop.addListing(created);
            listingId = created.getId();
            props.set(P_HAS_LISTING, 1);
            state.markDirtyAndSave();
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Listing created - drop stock into the bin.").withStyle(ChatFormatting.GREEN));
            return;
        }

        if (!sale.isEmpty() && !StackData.canCombine(sale, l.getItemForSale())) {
            int old = l.getStockQuantitySafe();
            if (old > 0) {
                give(sp, l.getItemForSale(), old);
                l.setStock(0);
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Returned " + old + " old stock (the item changed).")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        if (!sale.isEmpty()) l.setItemForSale(sale.copy());
        if (!barter.isEmpty()) l.setBarterPrice(barter.copy(), barter.getCount());
        if (price <= 0 && !l.acceptsBarter() && barter.isEmpty()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("A listing needs a coin price and/or a barter item.").withStyle(ChatFormatting.RED));
            return;
        }
        l.setCoinPrice(price);
        props.set(P_PRICE, price);
        state.markDirtyAndSave();
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Listing saved.").withStyle(ChatFormatting.GREEN));
    }

    private void deposit(ServerPlayer sp, ShopState state) {
        ShopListing l = listing();
        if (l == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Save the listing first, then deposit stock.").withStyle(ChatFormatting.RED));
            return;
        }
        int moved = 0;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack st = sp.getInventory().getItem(i);
            if (!st.isEmpty() && StackData.canCombine(st, l.getItemForSale())) {
                moved += st.getCount();
                sp.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        if (moved == 0) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You aren't carrying any matching items.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        l.addStock(moved);
        state.markDirtyAndSave();
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Deposited " + moved + " into stock.").withStyle(ChatFormatting.GREEN));
    }

    private void returnStock(ServerPlayer sp, ShopState state) {
        ShopListing l = listing();
        if (l == null) return;
        int stock = l.getStockQuantitySafe();
        if (stock <= 0) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("There's no stock to take back.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        l.setStock(0);
        give(sp, l.getItemForSale(), stock);
        state.markDirtyAndSave();
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Returned " + stock + " stock to your inventory.").withStyle(ChatFormatting.GREEN));
    }

    private static void give(ServerPlayer sp, ItemStack template, int count) {
        while (count > 0) {
            ItemStack chunk = template.copy();
            chunk.setCount(Math.min(count, template.getMaxStackSize()));
            count -= chunk.getCount();
            sp.getInventory().placeItemBackInInventory(chunk);
        }
    }

    @Override
    public void broadcastChanges() {
        ShopListing l = listing();
        if (l != null && playerInv.player instanceof ServerPlayer sp && !sp.level().isClientSide) {
            ItemStack intake = samples.getItem(SLOT_STOCK);
            if (!intake.isEmpty() && StackData.canCombine(intake, l.getItemForSale())) {
                int moved = intake.getCount();
                l.addStock(moved);
                samples.setItem(SLOT_STOCK, ItemStack.EMPTY);
                ShopState.get(sp.serverLevel()).markDirtyAndSave();
                net.fugginbeenus.notchcurrency.compat.Msg.actionBar(sp, Component.literal("Added " + moved + " to stock.").withStyle(ChatFormatting.GREEN));
            }
        }
        if (l != null) {
            props.set(P_STOCK, l.getStockQuantitySafe());
        }
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            for (int i = 0; i < samples.getContainerSize(); i++) {
                ItemStack st = samples.removeItemNoUpdate(i);
                if (!st.isEmpty() && !player.getInventory().add(st)) {
                    player.drop(st, false);
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, SLOT_STOCK, SLOT_STOCK + 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
