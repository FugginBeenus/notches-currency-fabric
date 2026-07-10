package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.registry.ModItems;
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
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Backing handler for the bounty board GUI. Board offers and the viewer's taken bounties are
 * synced as read-only data-carrying display stacks (each carrying the bounty id + task/reward/
 * progress/expiry in NBT); the screen renders rows from these and sends actions by id. Refreshed
 * every tick so progress and timers stay live.
 */
public class BountyBoardScreenHandler extends ScreenHandler {

    public static final int OFFER_SLOTS = 5;
    public static final int TAKEN_SLOTS = 5;

    public static final int P_TAKE_LIMIT = 0;
    public static final int P_PAGE = 1;
    public static final int P_TOTAL_PAGES = 2;
    private static final int PROP_COUNT = 3;

    private int page = 0;

    private final PlayerInventory playerInv;
    private final World world;
    private final SimpleInventory boardInv = new SimpleInventory(OFFER_SLOTS + TAKEN_SLOTS);
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) { return false; }
        @Override public boolean canTakeItems(PlayerEntity p) { return false; }
    }

    public BountyBoardScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.BOUNTY_BOARD, syncId);
        this.playerInv = inv;
        this.world = inv.player.getWorld();
        this.addProperties(props);
        for (int i = 0; i < boardInv.size(); i++) {
            this.addSlot(new ReadOnlySlot(boardInv, i, -10000, -10000));
        }
        refresh();
    }

    public ItemStack offerStack(int i) {
        return boardInv.getStack(i);
    }

    public ItemStack takenStack(int i) {
        return boardInv.getStack(OFFER_SLOTS + i);
    }

    public int prop(int i) {
        return props.get(i);
    }

    private void refresh() {
        if (!(world instanceof ServerWorld)) return;
        if (!(playerInv.player instanceof ServerPlayerEntity sp) || sp.getServer() == null) return;
        BountyState state = BountyState.get(sp.getServer());
        long now = BountyManager.worldTime(sp.getServer());

        props.set(P_TAKE_LIMIT, BountyManager.getTakeLimit());

        // Offers the player hasn't taken yet, paginated.
        List<Bounty> avail = new ArrayList<>();
        for (Bounty b : state.allOffers()) {
            if (b.isExpired(now) || state.hasTaken(sp.getUuid(), b.getId())
                    || state.hasCompletedOffer(sp.getUuid(), b.getId())) continue;
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
            boardInv.setStack(i, idx < avail.size()
                    ? display(avail.get(idx), false, 0, avail.get(idx).getExpiresGameTime())
                    : ItemStack.EMPTY);
        }

        // The player's taken bounties.
        List<TakenBounty> mine = state.getTakenAll(sp.getUuid());
        int t = 0;
        for (TakenBounty tb : mine) {
            if (t >= TAKEN_SLOTS) break;
            boardInv.setStack(OFFER_SLOTS + t++, display(tb.bounty(), true, tb.progress(), tb.expiresGameTime()));
        }
        while (t < TAKEN_SLOTS) boardInv.setStack(OFFER_SLOTS + t++, ItemStack.EMPTY);
    }

    private static ItemStack display(Bounty b, boolean mine, int progress, long expiry) {
        ItemStack carrier = !b.getRewardItem().isEmpty() ? b.getRewardItem().copy() : new ItemStack(ModItems.NOTCH_COIN);
        NbtCompound t = carrier.getOrCreateNbt();
        t.putUuid("bid", b.getId());
        t.putString("desc", b.describe());
        t.putString("rew", b.rewardSummary());
        t.putLong("rewc", b.getRewardCoins());
        if (!b.getRewardItem().isEmpty()) {
            // The reward stack itself, so the row can draw its icon with the count.
            t.put("rews", b.getRewardItem().writeNbt(new NbtCompound()));
        }
        t.putString("rar", b.getRarity().name());
        t.putString("typ", b.getType().name());
        t.putInt("prog", progress);
        t.putInt("req", b.getRequired());
        t.putLong("exp", expiry);
        t.putBoolean("mine", mine);
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
