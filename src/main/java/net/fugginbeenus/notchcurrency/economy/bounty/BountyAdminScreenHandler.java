package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

public class BountyAdminScreenHandler extends AbstractContainerMenu {

    public static final int DECREE_SLOTS = 4;
    public static final int DECREE_X = 9, DECREE_Y = 33;
    public static final int INV_X = 8, INV_Y = 164, HOTBAR_Y = 222;

    public static final int A_ENABLED  = 0;
    public static final int A_ACTIVE   = 1;
    public static final int A_LIMIT    = 2;
    public static final int A_DURATION = 3;
    private static final int PROP_COUNT = 4;

    private final Inventory playerInv;
    private final Level world;
    private final SimpleContainer decreeInv = new SimpleContainer(DECREE_SLOTS);
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);

    private static final class DecreeSlot extends Slot {
        DecreeSlot(Container inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) {
            return BountyPools.decreeCategory(BuiltInRegistries.ITEM.getKey(s.getItem())) != null;
        }
    }

    public BountyAdminScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.BOUNTY_ADMIN, containerId);
        this.playerInv = inv;
        this.world = inv.player.level();
        this.addDataSlots(props);

        // Prefill decree slots from the saved board decrees.
        if (inv.player instanceof ServerPlayer sp && sp.getServer() != null) {
            List<ItemStack> saved = BountyState.get(sp.getServer()).getDecrees();
            for (int i = 0; i < DECREE_SLOTS && i < saved.size(); i++) decreeInv.setItem(i, saved.get(i));
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

    public static void open(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new BountyAdminScreenHandler(containerId, inv),
                Component.literal("Bounty Setup")));
    }

    public int prop(int i) {
        return props.get(i);
    }

    private void refresh() {
        if (!(world instanceof ServerLevel)) return;
        NotchConfig.Bounty b = NotchConfigIO.get().bounty;
        props.set(A_ENABLED, b.enabled ? 1 : 0);
        props.set(A_ACTIVE, b.activeCount);
        props.set(A_LIMIT, b.takeLimit);
        props.set(A_DURATION, b.durationMinutes);
    }

    public void persistDecrees(ServerPlayer sp) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < DECREE_SLOTS; i++) {
            ItemStack s = decreeInv.getItem(i);
            if (!s.isEmpty()) items.add(s.copy());
        }
        BountyState.get(sp.getServer()).setDecrees(items);
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || !sp.hasPermissions(2)) return false;
        if (id == 0) { // regenerate now
            persistDecrees(sp);
            BountyManager.regenerate(sp.getServer());
            sp.displayClientMessage(Component.literal("Bounties regenerated."), false);
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp && sp.getServer() != null) {
            persistDecrees(sp);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Only move between the decree slots and the player inventory.
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = DECREE_SLOTS;
            if (index < DECREE_SLOTS) {
                if (!this.moveItemStackTo(stack, invStart, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, DECREE_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.hasPermissions(2);
    }
}
