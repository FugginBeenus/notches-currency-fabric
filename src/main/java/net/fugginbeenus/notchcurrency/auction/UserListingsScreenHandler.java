package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Small 2x4 inventory used for the "Your Listings" popup.
 * Purely visual for now – you can later fill this from AuctionState.
 */
public class UserListingsScreenHandler extends ScreenHandler {

    public static final int COLUMNS = 4;
    public static final int ROWS    = 2;
    public static final int SIZE    = COLUMNS * ROWS; // 8

    // Slot layout inside the popup texture (relative to popup top-left)
    private static final int SLOT_SIZE     = 18;
    private static final int START_X       = 18;   // you’ll tweak these
    private static final int START_Y       = 36;   // to line up with the plus icons
    private static final int SLOT_OFFSET_X = 4;    // extra nudge right
    private static final int SLOT_OFFSET_Y = 16;   // extra nudge down

    private final SimpleInventory inv = new SimpleInventory(SIZE);

    public UserListingsScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ModScreenHandlers.USER_AUCTIONS, syncId);

        int index = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int x = START_X + SLOT_OFFSET_X + col * SLOT_SIZE;
                int y = START_Y + SLOT_OFFSET_Y + row * SLOT_SIZE;
                this.addSlot(new Slot(inv, index++, x, y));
            }
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true; // virtual popup, no proximity checks
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        // For now: no shift-click moving in/out, just visual.
        return ItemStack.EMPTY;
    }
}
