package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.StackData;
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
 * Backing handler for the trade-offers board: offers you can accept (incoming, paginated) plus your
 * own open offers (outgoing, cancelable), synced as read-only data-carrier stacks (offered item +
 * price/requested/creator in NBT). Refreshed each tick so the board stays live as offers resolve.
 * Actions go through the TRADE_OFFER_ACTION packet by offer id.
 */
public class TradeOffersScreenHandler extends ScreenHandler {

    public static final int INCOMING = 5, OUTGOING = 5;

    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_INCOMING = 2, P_OUTGOING = 3;
    private static final int PROP_COUNT = 4;

    private final PlayerInventory playerInv;
    private final SimpleInventory inv = new SimpleInventory(INCOMING + OUTGOING);
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) { return false; }
        @Override public boolean canTakeItems(PlayerEntity p) { return false; }
    }

    public TradeOffersScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.TRADE_OFFERS, syncId);
        this.playerInv = inv;
        this.addProperties(props);
        for (int i = 0; i < this.inv.size(); i++) {
            this.addSlot(new ReadOnlySlot(this.inv, i, -10000, -10000));
        }
        refresh();
    }

    public static void open(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new TradeOffersScreenHandler(syncId, inv),
                Text.literal("Trade Offers")));
    }

    public ItemStack incomingStack(int i) { return inv.getStack(i); }
    public ItemStack outgoingStack(int i) { return inv.getStack(INCOMING + i); }
    public int prop(int i) { return props.get(i); }

    private void refresh() {
        if (!(playerInv.player instanceof ServerPlayerEntity sp) || sp.getServer() == null) return;
        TradeOfferState state = TradeOfferState.get(sp.getServer());

        List<TradeOffer> incoming = state.incomingFor(sp.getUuid(), sp.getName().getString());
        int totalPages = Math.max(1, (incoming.size() + INCOMING - 1) / INCOMING);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);
        props.set(P_INCOMING, incoming.size());

        int start = page * INCOMING;
        for (int i = 0; i < INCOMING; i++) {
            int idx = start + i;
            inv.setStack(i, idx < incoming.size() ? display(incoming.get(idx), false) : ItemStack.EMPTY);
        }

        List<TradeOffer> outgoing = state.outgoingBy(sp.getUuid());
        props.set(P_OUTGOING, outgoing.size());
        for (int i = 0; i < OUTGOING; i++) {
            inv.setStack(INCOMING + i, i < outgoing.size() ? display(outgoing.get(i), true) : ItemStack.EMPTY);
        }
    }

    private static ItemStack display(TradeOffer offer, boolean mine) {
        ItemStack carrier = offer.firstOffered().copy();
        if (carrier.isEmpty()) carrier = new ItemStack(net.minecraft.item.Items.PAPER); // coins-only offer
        NbtCompound t = StackData.editData(carrier);
        t.putUuid("nc_oid", offer.id());
        t.putLong("nc_price", offer.priceCoins());
        t.putLong("nc_gcoins", offer.offeredCoins());
        // Every stack on both sides, so the row can draw the whole exchange like trade ingredients.
        net.minecraft.nbt.NbtList gives = new net.minecraft.nbt.NbtList();
        for (ItemStack st : offer.offeredItems()) {
            gives.add(StackData.writeStack(st));
        }
        t.put("nc_gives", gives);
        net.minecraft.nbt.NbtList wants = new net.minecraft.nbt.NbtList();
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
