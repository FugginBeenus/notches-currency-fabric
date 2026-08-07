package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

public class BountyBoardScreenHandler extends AbstractContainerMenu {

    public static final int OFFER_SLOTS = 5;
    public static final int TAKEN_SLOTS = 5;

    public static final int P_TAKE_LIMIT = 0;
    public static final int P_PAGE = 1;
    public static final int P_TOTAL_PAGES = 2;
    private static final int PROP_COUNT = 3;

    private int page = 0;

    private final Inventory playerInv;
    private final Level world;
    private final SimpleContainer boardInv = new SimpleContainer(OFFER_SLOTS + TAKEN_SLOTS);
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }

    public BountyBoardScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.BOUNTY_BOARD, containerId);
        this.playerInv = inv;
        this.world = inv.player.level();
        this.addDataSlots(props);
        for (int i = 0; i < boardInv.getContainerSize(); i++) {
            this.addSlot(new ReadOnlySlot(boardInv, i, -10000, -10000));
        }
        refresh();
    }

    public ItemStack offerStack(int i) {
        return boardInv.getItem(i);
    }

    public ItemStack takenStack(int i) {
        return boardInv.getItem(OFFER_SLOTS + i);
    }

    public int prop(int i) {
        return props.get(i);
    }

    private void refresh() {
        if (!(world instanceof ServerLevel)) return;
        if (!(playerInv.player instanceof ServerPlayer sp) || sp.getServer() == null) return;
        BountyState state = BountyState.get(sp.getServer());
        long now = BountyManager.worldTime(sp.getServer());

        props.set(P_TAKE_LIMIT, BountyManager.getTakeLimit());

        // Offers the player hasn't taken yet, paginated.
        List<Bounty> avail = new ArrayList<>();
        for (Bounty b : state.allOffers()) {
            if (b.isExpired(now) || state.hasTaken(sp.getUUID(), b.getId())
                    || state.hasCompletedOffer(sp.getUUID(), b.getId())) continue;
            avail.add(b);
        }
        int totalPages = Math.max(1, (avail.size() + OFFER_SLOTS - 1) / OFFER_SLOTS);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);

        int start = page * OFFER_SLOTS;
        for (int i = 0; i < OFFER_SLOTS; i++) {
            int idx = start + i;
            boardInv.setItem(i, idx < avail.size()
                    ? display(avail.get(idx), false, 0, avail.get(idx).getExpiresGameTime())
                    : ItemStack.EMPTY);
        }

        // The player's taken bounties.
        List<TakenBounty> mine = state.getTakenAll(sp.getUUID());
        int t = 0;
        for (TakenBounty tb : mine) {
            if (t >= TAKEN_SLOTS) break;
            boardInv.setItem(OFFER_SLOTS + t++, display(tb.bounty(), true, tb.progress(), tb.expiresGameTime()));
        }
        while (t < TAKEN_SLOTS) boardInv.setItem(OFFER_SLOTS + t++, ItemStack.EMPTY);
    }

    private static ItemStack display(Bounty b, boolean mine, int progress, long expiry) {
        ItemStack carrier = !b.getRewardItem().isEmpty() ? b.getRewardItem().copy() : new ItemStack(ModItems.NOTCH_COIN);
        CompoundTag t = StackData.editData(carrier);
        t.putUUID("bid", b.getId());
        t.putString("desc", b.describe());
        t.putString("rew", b.rewardSummary());
        t.putLong("rewc", b.getRewardCoins());
        if (!b.getRewardItem().isEmpty()) {
            // The reward stack itself, so the row can draw its icon with the count.
            t.put("rews", StackData.writeStack(b.getRewardItem()));
        }
        t.putString("rar", b.getRarity().name());
        t.putString("typ", b.getType().name());
        t.putInt("prog", progress);
        t.putInt("req", b.getRequired());
        t.putLong("exp", expiry);
        t.putBoolean("mine", mine);
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
