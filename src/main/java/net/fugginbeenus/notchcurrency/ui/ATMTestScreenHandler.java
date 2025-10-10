package net.fugginbeenus.notchcurrency.ui;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 5 input slots across the top + player inventory/hotbar.
 * Exposes a tracked "balance" property to the client for UI display.
 */
public class ATMTestScreenHandler extends ScreenHandler {

    // Indices / layout constants
    private static final int ATM_SLOTS = 5;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLS = 9;
    private static final int PLAYER_INV_SLOTS = PLAYER_INV_ROWS * PLAYER_INV_COLS; // 27
    private static final int HOTBAR_SLOTS = 9;

    // Slot pixel positions (match your background texture)
    private static final int ATM_X = 46; // you dialed this in
    private static final int ATM_Y = 18;
    private static final int SLOT_SPACING = 18;

    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = PLAYER_INV_Y + (SLOT_SPACING * 3) + 4;

    private final Inventory atmInv;
    private final ArrayPropertyDelegate props; // index 0 == balance

    public ATMTestScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ModScreenHandlers.ATM, syncId);

        this.atmInv = new SimpleInventory(ATM_SLOTS);
        this.atmInv.onOpen(playerInv.player);

        // Property delegate with one int for balance
        this.props = new ArrayPropertyDelegate(1);
        this.addProperties(this.props); // ensures client sync

        // --- ATM input row (5 slots) ---
        for (int i = 0; i < ATM_SLOTS; i++) {
            int x = ATM_X + i * SLOT_SPACING;
            addSlot(new Slot(this.atmInv, i, x, ATM_Y));
        }

        // --- Player inventory (3 rows) ---
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                int x = PLAYER_INV_X + col * SLOT_SPACING;
                int y = PLAYER_INV_Y + row * SLOT_SPACING;
                addSlot(new Slot(playerInv, col + row * 9 + 9, x, y));
            }
        }

        // --- Hotbar ---
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int x = PLAYER_INV_X + i * SLOT_SPACING;
            addSlot(new Slot(playerInv, i, x, HOTBAR_Y));
        }
    }

    // === Balance accessors (tracked & synced) ===
    public int getBalance() {
        return this.props.get(0);
    }
    public void setBalance(int value) {
        this.props.set(0, value);
    }

    /**
     * Kept for compatibility if something still calls it.
     * You can wire auto-deposit logic here later or in onContentChanged.
     */
    public void depositAll(ServerPlayerEntity player) {
        // no-op for now
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stackInSlot = slot.getStack();
            newStack = stackInSlot.copy();

            int atmStart = 0;
            int atmEnd = atmStart + ATM_SLOTS;                 // 0..4
            int invStart = atmEnd;                             // 5
            int invEnd = invStart + PLAYER_INV_SLOTS;          // 5..31
            int hotbarStart = invEnd;                          // 32
            int hotbarEnd = hotbarStart + HOTBAR_SLOTS;        // 32..40 (exclusive)

            if (index < atmEnd) {
                // Move from ATM to player (inv+hotbar)
                if (!this.insertItem(stackInSlot, invStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player/hotbar into ATM inputs
                if (!this.insertItem(stackInSlot, atmStart, atmEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return newStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.atmInv.onClose(player);
    }
}
