package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class TradeOffersScreenHandler extends AbstractContainerMenu {

    public static final int INCOMING = 5, OUTGOING = 5;

    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_INCOMING = 2, P_OUTGOING = 3;
    private static final int PROP_COUNT = 4;

    private final Inventory playerInv;
    private final SimpleContainer inv = new SimpleContainer(INCOMING + OUTGOING);
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }

    public TradeOffersScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.TRADE_OFFERS, containerId);
        this.playerInv = inv;
        this.addDataSlots(props);
        for (int i = 0; i < this.inv.getContainerSize(); i++) {
            this.addSlot(new ReadOnlySlot(this.inv, i, -10000, -10000));
        }
        refresh();
    }

    public static void open(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new TradeOffersScreenHandler(containerId, inv),
                Component.literal("Trade Offers")));
    }

    public ItemStack incomingStack(int i) { return inv.getItem(i); }
    public ItemStack outgoingStack(int i) { return inv.getItem(INCOMING + i); }
    public int prop(int i) { return props.get(i); }

    private void refresh() {
        if (!(playerInv.player instanceof ServerPlayer sp) || sp.getServer() == null) return;
        TradeOfferState state = TradeOfferState.get(sp.getServer());

        List<TradeOffer> incoming = state.incomingFor(sp.getUUID(), sp.getName().getString());
        int totalPages = Math.max(1, (incoming.size() + INCOMING - 1) / INCOMING);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);
        props.set(P_INCOMING, incoming.size());

        int start = page * INCOMING;
        for (int i = 0; i < INCOMING; i++) {
            int idx = start + i;
            inv.setItem(i, idx < incoming.size() ? display(incoming.get(idx), false) : ItemStack.EMPTY);
        }

        List<TradeOffer> outgoing = state.outgoingBy(sp.getUUID());
        props.set(P_OUTGOING, outgoing.size());
        for (int i = 0; i < OUTGOING; i++) {
            inv.setItem(INCOMING + i, i < outgoing.size() ? display(outgoing.get(i), true) : ItemStack.EMPTY);
        }
    }

    private static ItemStack display(TradeOffer offer, boolean mine) {
        ItemStack carrier = offer.firstOffered().copy();
        if (carrier.isEmpty()) carrier = new ItemStack(net.minecraft.world.item.Items.PAPER); // coins-only offer
        CompoundTag t = StackData.editData(carrier);
        t.putUUID("nc_oid", offer.id());
        t.putLong("nc_price", offer.priceCoins());
        t.putLong("nc_gcoins", offer.offeredCoins());
        // Every stack on both sides, so the row can draw the whole exchange like trade ingredients.
        net.minecraft.nbt.ListTag gives = new net.minecraft.nbt.ListTag();
        for (ItemStack st : offer.offeredItems()) {
            gives.add(StackData.writeStack(st));
        }
        t.put("nc_gives", gives);
        net.minecraft.nbt.ListTag wants = new net.minecraft.nbt.ListTag();
        for (ItemStack st : offer.requestedItems()) {
            wants.add(StackData.writeStack(st));
        }
        t.put("nc_wants", wants);
        t.putString("nc_from", offer.creatorName());
        t.putString("nc_target", offer.isOpen() ? "" : offer.targetName());
        t.putBoolean("nc_mine", mine);
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
