package net.fugginbeenus.notchcurrency.economy.cosmetic;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Backing handler for the cosmetics shop. Offers are synced as read-only data-carrier display
 * stacks (icon + name/price/owned in NBT), refreshed each tick so the "owned" state updates right
 * after a purchase. Paginated so any number of cosmetics is browsable. Purchases go through the
 * COSMETIC_BUY packet (carries the offer id) → CosmeticManager.buy.
 */
public class CosmeticShopScreenHandler extends ScreenHandler {

    public static final int ROWS = 6;
    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_COUNT = 2;
    private static final int PROP_COUNT = 3;

    private final PlayerInventory playerInv;
    private final SimpleInventory rowInv = new SimpleInventory(ROWS);
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) { return false; }
        @Override public boolean canTakeItems(PlayerEntity p) { return false; }
    }

    public CosmeticShopScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.COSMETIC_SHOP, syncId);
        this.playerInv = inv;
        this.addProperties(props);
        for (int i = 0; i < ROWS; i++) {
            this.addSlot(new ReadOnlySlot(rowInv, i, -10000, -10000));
        }
        refresh();
    }

    public static void open(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new CosmeticShopScreenHandler(syncId, inv),
                Text.literal("Cosmetics")));
    }

    public ItemStack rowStack(int i) { return rowInv.getStack(i); }
    public int prop(int i) { return props.get(i); }

    private void refresh() {
        if (!(playerInv.player instanceof ServerPlayerEntity sp) || sp.getServer() == null) return;
        CosmeticState state = CosmeticState.get(sp.getServer());
        List<CosmeticOffer> offers = CosmeticRegistry.all();

        int totalPages = Math.max(1, (offers.size() + ROWS - 1) / ROWS);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);
        props.set(P_COUNT, offers.size());

        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx < offers.size()) {
                CosmeticOffer o = offers.get(idx);
                boolean owned = o.oneTime() && state.owns(sp.getUuid(), o.id());
                rowInv.setStack(i, display(o, owned));
            } else {
                rowInv.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static ItemStack display(CosmeticOffer offer, boolean owned) {
        ItemStack carrier = offer.icon().copy();
        if (carrier.isEmpty()) return ItemStack.EMPTY;
        carrier.setCount(1);
        NbtCompound t = carrier.getOrCreateNbt();
        t.putString("nc_cid", offer.id());
        t.putString("nc_name", offer.name());
        t.putLong("nc_price", offer.price());
        t.putBoolean("nc_owned", owned);
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
