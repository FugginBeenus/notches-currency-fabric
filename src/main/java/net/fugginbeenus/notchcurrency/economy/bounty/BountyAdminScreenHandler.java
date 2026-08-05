package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Op-only bounty setup GUI (opened via {@code /bounty admin}). Edits the board config and holds
 * the board's <b>decree</b> items in dedicated slots: placing a decree gates generation to its
 * category (no decrees = all categories). Decree slots only accept registered decree items and
 * persist to {@link BountyState} on save/close.
 */
public class BountyAdminScreenHandler extends ScreenHandler {

    public static final int DECREE_SLOTS = 4;
    public static final int DECREE_X = 9, DECREE_Y = 33;
    public static final int INV_X = 8, INV_Y = 164, HOTBAR_Y = 222;

    public static final int A_ENABLED  = 0;
    public static final int A_ACTIVE   = 1;
    public static final int A_LIMIT    = 2;
    public static final int A_DURATION = 3;
    private static final int PROP_COUNT = 4;

    private final PlayerInventory playerInv;
    private final World world;
    private final SimpleInventory decreeInv = new SimpleInventory(DECREE_SLOTS);
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);

    /** Slot that only accepts items registered as decrees. */
    private static final class DecreeSlot extends Slot {
        DecreeSlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) {
            return BountyPools.decreeCategory(Registries.ITEM.getId(s.getItem())) != null;
        }
    }

    public BountyAdminScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.BOUNTY_ADMIN, syncId);
        this.playerInv = inv;
        this.world = inv.player.getWorld();
        this.addProperties(props);

        // Prefill decree slots from the saved board decrees.
        if (inv.player instanceof ServerPlayerEntity sp && sp.getServer() != null) {
            List<ItemStack> saved = BountyState.get(sp.getServer()).getDecrees();
            for (int i = 0; i < DECREE_SLOTS && i < saved.size(); i++) decreeInv.setStack(i, saved.get(i));
        }
        for (int i = 0; i < DECREE_SLOTS; i++) {
            addSlot(new DecreeSlot(decreeInv, i, DECREE_X + i * 18, DECREE_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
        refresh();
    }

    public static void open(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new BountyAdminScreenHandler(syncId, inv),
                Text.literal("Bounty Setup")));
    }

    public int prop(int i) {
        return props.get(i);
    }

    private void refresh() {
        if (!(world instanceof ServerWorld)) return;
        NotchConfig.Bounty b = NotchConfigIO.get().bounty;
        props.set(A_ENABLED, b.enabled ? 1 : 0);
        props.set(A_ACTIVE, b.activeCount);
        props.set(A_LIMIT, b.takeLimit);
        props.set(A_DURATION, b.durationMinutes);
    }

    /** Persist the decree slots back to the board state (called on save and close). */
    public void persistDecrees(ServerPlayerEntity sp) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < DECREE_SLOTS; i++) {
            ItemStack s = decreeInv.getStack(i);
            if (!s.isEmpty()) items.add(s.copy());
        }
        BountyState.get(sp.getServer()).setDecrees(items);
    }

    @Override
    public void sendContentUpdates() {
        refresh();
        super.sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity sp) || !sp.hasPermissionLevel(2)) return false;
        if (id == 0) { // regenerate now
            persistDecrees(sp);
            BountyManager.regenerate(sp.getServer());
            sp.sendMessage(Text.literal("Bounties regenerated."), false);
            return true;
        }
        return false;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (player instanceof ServerPlayerEntity sp && sp.getServer() != null) {
            persistDecrees(sp);
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        // Only move between the decree slots and the player inventory.
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            int invStart = DECREE_SLOTS;
            if (index < DECREE_SLOTS) {
                if (!this.insertItem(stack, invStart, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(stack, 0, DECREE_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.hasPermissionLevel(2);
    }
}
