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

    // Layout (relative to 176x166 GUI panel)
    private static final int BANK_X = 8;
    private static final int BANK_Y = 17;
    private static final int PLAYER_X = 8;
    private static final int PLAYER_Y = 84;
    private static final int HOTBAR_Y = PLAYER_Y + 58;

    private final PlayerInventory playerInv;
    private final PropertyDelegate props = new ArrayPropertyDelegate(1);

    // Top 5 slots inventory
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

        // Sync property (balance) to client
        this.addProperties(props);

        // Seed client with current balance on open (server only)
        if (!playerInv.player.getWorld().isClient && playerInv.player instanceof ServerPlayerEntity sp) {
            props.set(0, BalanceStore.get(sp));
        }

        // ----- Top 5 slots -----
        for (int i = 0; i < 5; i++) {
            this.addSlot(new CurrencySlot(bankInv, i, BANK_X + i * 18, BANK_Y));
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

    /** Server updates the property; Fabric syncs it to the client screen. */
    public void setSyncedBalance(int value) {
        if (!playerInv.player.getWorld().isClient) {
            props.set(0, value);
        }
    }

    /** Screen reads this on both sides */
    public int getSyncedBalance() {
        return props.get(0);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    /** Shift-click: if it's currency, consume it into the balance immediately. */
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

    // -------------------
    // Deposit mechanics
    // -------------------

    private boolean isCurrency(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(ModItems.NOTCH_COIN);
    }

    /** Called when the user places/removes items in the top 5 slots (server-side). */
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

    /** Add to player balance, update ATM property, and ping HUD immediately. */
    private void depositAmount(ServerPlayerEntity sp, int amount) {
        int newBal = BalanceStore.add(sp, amount); // returns new total
        setSyncedBalance(newBal);                  // updates ATM screen property
        NotchPackets.sendBalance(sp, newBal);      // updates HUD now
    }

    // Only allow the coin in the top 5 slots
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
