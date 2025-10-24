package net.fugginbeenus.notchcurrency.ui;

import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

public class ATMTestScreenHandler extends ScreenHandler {

    // ===== Top 5 slots baseline (relative to 176x166 panel) =====
    // Vanilla-like baseline puts rows at (8, 17). Keep spacing = 18px.
    private static final int BANK_BASE_X = 8;
    private static final int BANK_BASE_Y = 17;
    private static final int BANK_SPACING = 18;

    // Pixel nudges to line up with your painted frames (adjust these!)
    public static int BANK_NUDGE_X = 38; // +right / -left
    public static int BANK_NUDGE_Y = 1; // +down  / -up

    // ===== Player inv / hotbar (leave these unless your texture differs) =====
    private static final int PLAYER_X = 8;
    private static final int PLAYER_Y = 84;
    private static final int HOTBAR_Y = PLAYER_Y + 58;

    private final PlayerInventory playerInv;
    private final PropertyDelegate props = new ArrayPropertyDelegate(1);

    private final Inventory bankInv = new SimpleInventory(5) {
        @Override
        public void markDirty() {
            super.markDirty();
            if (!playerInv.player.getWorld().isClient && playerInv.player instanceof ServerPlayerEntity sp) {
                depositAllCoins(sp);
            }
        }
    };

    public ATMTestScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ModScreenHandlers.ATM, syncId);
        this.playerInv = playerInv;

        this.addProperties(props);

        if (!playerInv.player.getWorld().isClient && playerInv.player instanceof ServerPlayerEntity sp) {
            props.set(0, BalanceStore.get(sp));
        }

        // ----- Top 5 slots (with nudges) -----
        final int rowY = BANK_BASE_Y + BANK_NUDGE_Y;
        for (int i = 0; i < 5; i++) {
            int x = BANK_BASE_X + BANK_NUDGE_X + (i * BANK_SPACING);
            this.addSlot(new CurrencySlot(bankInv, i, x, rowY));
        }

        // ----- Player inventory -----
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        PLAYER_X + col * 18,
                        PLAYER_Y + row * 18));
            }
        }

        // ----- Hotbar -----
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, PLAYER_X + col * 18, HOTBAR_Y));
        }
    }

    public void setSyncedBalance(int value) {
        if (!playerInv.player.getWorld().isClient) {
            props.set(0, value);
        }
    }

    public int getSyncedBalance() {
        return props.get(0);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            if (isCurrency(stack)) {
                int count = stack.getCount();
                if (!player.getWorld().isClient && player instanceof ServerPlayerEntity sp) {
                    slot.setStack(ItemStack.EMPTY);
                    slot.markDirty();
                    depositAmount(sp, count);
                }
            }
        }
        return result;
    }

    private boolean isCurrency(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(ModItems.NOTCH_COIN);
    }

    private void depositAllCoins(ServerPlayerEntity sp) {
        int total = 0;
        for (int i = 0; i < bankInv.size(); i++) {
            ItemStack s = bankInv.getStack(i);
            if (isCurrency(s)) {
                total += s.getCount();
                bankInv.setStack(i, ItemStack.EMPTY);
            }
        }
        if (total > 0) {
            depositAmount(sp, total);
        }
    }

    private void depositAmount(ServerPlayerEntity sp, int amount) {
        int newBal = BalanceStore.add(sp, amount);
        setSyncedBalance(newBal);
        NotchPackets.sendBalance(sp, newBal);
    }

    private class CurrencySlot extends Slot {
        public CurrencySlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }
        @Override
        public boolean canInsert(ItemStack stack) {
            return isCurrency(stack);
        }
    }
}
