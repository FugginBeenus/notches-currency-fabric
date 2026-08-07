package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UserListingsScreenHandler extends AbstractContainerMenu {

    public static final int COLUMNS = 4;
    public static final int ROWS    = 2;
    public static final int SIZE    = COLUMNS * ROWS; // 8

    // Slot layout inside the popup texture (relative to popup top-left)
    private static final int SLOT_SIZE     = 18;
    private static final int START_X       = 18;   // you’ll tweak these
    private static final int START_Y       = 36;   // to line up with the plus icons
    private static final int SLOT_OFFSET_X = 4;    // extra nudge right
    private static final int SLOT_OFFSET_Y = 16;   // extra nudge down

    private final SimpleContainer inv = new SimpleContainer(SIZE);

    public UserListingsScreenHandler(int containerId, Inventory playerInv) {
        super(ModScreenHandlers.USER_AUCTIONS, containerId);

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
    public boolean stillValid(Player player) {
        return true; // virtual popup, no proximity checks
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // For now: no shift-click moving in/out, just visual.
        return ItemStack.EMPTY;
    }
}
