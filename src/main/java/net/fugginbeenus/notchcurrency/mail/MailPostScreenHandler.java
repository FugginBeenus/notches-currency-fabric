package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MailPostScreenHandler extends AbstractContainerMenu {

    public static final int PARCEL_SLOTS = MailItem.MAX_CONTENTS;
    public static final int PARCEL_X = 34, PARCEL_Y = 62;
    private static final int INV_X = MailLayout.INV_X, INV_Y = MailLayout.INV_Y,
            HOTBAR_Y = MailLayout.HOTBAR_Y;

    private final SimpleContainer parcel = new SimpleContainer(PARCEL_SLOTS);

    public boolean parcelSlotsHidden;

    public MailPostScreenHandler(int containerId, Inventory playerInv) {
        super(ModScreenHandlers.MAIL_POST, containerId);

        for (int i = 0; i < PARCEL_SLOTS; i++) {
            addSlot(new Slot(parcel, i, PARCEL_X + i * 18, PARCEL_Y) {
                @Override
                public boolean isActive() {
                    return !parcelSlotsHidden;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    public static void open(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new MailPostScreenHandler(containerId, inv),
                Component.literal("Mailbox")));
    }

    public boolean isEmpty() {
        return parcel.isEmpty();
    }

    public java.util.List<ItemStack> takeContents() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < parcel.getContainerSize(); i++) {
            ItemStack stack = parcel.removeItemNoUpdate(i);
            if (!stack.isEmpty()) out.add(stack);
        }
        return out;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) return;
        for (int i = 0; i < parcel.getContainerSize(); i++) {
            ItemStack stack = parcel.removeItemNoUpdate(i);
            if (!stack.isEmpty() && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();
        if (index < PARCEL_SLOTS) {
            if (!moveItemStackTo(inSlot, PARCEL_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(inSlot, 0, PARCEL_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
