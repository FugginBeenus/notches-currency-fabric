package net.fugginbeenus.notchcurrency.economy.cosmetic;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.nbt.CompoundTag;
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

public class CosmeticShopScreenHandler extends AbstractContainerMenu {

    // Carrier slots live OFF-screen (the row-list screen reads rowStack(i) and draws icons itself).
    public static final int VIS_ROWS = 6, PER_PAGE = VIS_ROWS;
    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_COUNT = 2;
    private static final int PROP_COUNT = 3;

    private final Inventory playerInv;
    @Nullable private final UUID npcId;
    private final SimpleContainer rowInv = new SimpleContainer(PER_PAGE);
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }

    public CosmeticShopScreenHandler(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv, buf.readBoolean() ? buf.readUUID() : null);
    }

    public CosmeticShopScreenHandler(int containerId, Inventory inv, @Nullable UUID npcId) {
        super(ModScreenHandlers.COSMETIC_SHOP, containerId);
        this.playerInv = inv;
        this.npcId = npcId;
        this.addDataSlots(props);
        for (int i = 0; i < PER_PAGE; i++) {
            this.addSlot(new ReadOnlySlot(rowInv, i, -10000, -10000));
        }
        // Player inventory (matches the code-drawn CosmeticShopScreen / ShopBrowseScreen layout).
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

    public static void open(ServerPlayer sp, @Nullable UUID npcId) {
        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(sp, Component.literal("Cosmetics"),
                (containerId, inv, p) -> new CosmeticShopScreenHandler(containerId, inv, npcId),
                buf -> {
                    buf.writeBoolean(npcId != null);
                    if (npcId != null) buf.writeUUID(npcId);
                });
    }

    public ItemStack rowStack(int i) { return rowInv.getItem(i); }
    public int prop(int i) { return props.get(i); }
    @Nullable public UUID npcId() { return npcId; }

    private void refresh() {
        if (!(playerInv.player instanceof ServerPlayer sp) || sp.level().getServer() == null) return;
        CosmeticState state = CosmeticState.get(sp.level().getServer());
        List<CosmeticOffer> offers = CosmeticRegistry.all();

        int totalPages = Math.max(1, (offers.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);
        props.set(P_COUNT, offers.size());

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx < offers.size()) {
                CosmeticOffer o = offers.get(idx);
                boolean owned = o.oneTime() && state.owns(sp.getUUID(), o.id());
                rowInv.setItem(i, display(o, owned));
            } else {
                rowInv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static ItemStack display(CosmeticOffer offer, boolean owned) {
        ItemStack carrier = offer.icon().copy();
        if (carrier.isEmpty()) return ItemStack.EMPTY;
        carrier.setCount(1);
        CompoundTag t = StackData.editData(carrier);
        t.putString("nc_cid", offer.id());
        t.putString("nc_name", offer.name());
        t.putLong("nc_price", offer.price());
        t.putBoolean("nc_owned", owned);
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
        else if (id == 1) page = page + 1; // clamped in refresh()
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
