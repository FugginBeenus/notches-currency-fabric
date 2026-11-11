package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BountyPostScreenHandler extends AbstractContainerMenu {

    public static final int SAMPLE_X = 100, SAMPLE_Y = 42, REWARD_X = 146, REWARD_Y = 66;
    public static final int INV_X = 20, INV_Y = 134, HOTBAR_Y = 192;

    private final SimpleContainer held = new SimpleContainer(2);
    private final net.minecraft.world.inventory.ContainerData flags = new net.minecraft.world.inventory.SimpleContainerData(1);
    private boolean fetchMode;

    public BountyPostScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.BOUNTY_POST, containerId);
        addDataSlots(flags);
        if (inv.player instanceof ServerPlayer sp && BountyManager.pvpAllowed(sp)) flags.set(0, 1);
        addSlot(new Slot(held, 0, SAMPLE_X, SAMPLE_Y) {
            @Override
            public boolean isActive() {
                return fetchMode;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(held, 1, REWARD_X, REWARD_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    public boolean pvpAllowed() {
        return flags.get(0) == 1;
    }

    public void setFetchMode(boolean on) {
        this.fetchMode = on;
    }

    public ItemStack sample() {
        return held.getItem(0);
    }

    public ItemStack reward() {
        return held.getItem(1);
    }

    public void clearReward() {
        held.setItem(1, ItemStack.EMPTY);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !(player instanceof ServerPlayer sp)) return false;
        BountyManager.openScreen(sp);
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            for (int i = 0; i < held.getContainerSize(); i++) {
                ItemStack st = held.removeItemNoUpdate(i);
                if (!st.isEmpty() && !player.getInventory().add(st)) player.drop(st, false);
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
            if (index < 2) {
                if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
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
