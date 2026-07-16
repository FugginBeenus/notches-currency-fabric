package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * The NPC equipment screen: four armor slots (typed — only matching armor fits), main/off hand, and
 * the player's inventory. Server-side the slots are backed by {@link NpcEquipmentInventory}, so drops
 * and shift-clicks write directly onto the entity.
 */
public class NpcEquipScreenHandler extends ScreenHandler {

    // Slot coordinates (client draws insets at these -1).
    public static final int ARMOR_X = 26, ARMOR_Y = 16; // 4 slots, 18 apart
    public static final int HAND_X = 80, MAIN_Y = 34, OFF_Y = 52;
    public static final int INV_X = 8, INV_Y = 84, HOTBAR_Y = 142;

    private final Inventory equip;
    @Nullable private final NotchNpcEntity npc;

    /** Where an item wants to be equipped. 1.21 made the vanilla lookup an instance method, so this
     *  replicates the old static logic (Equipment interface, shield to offhand, else main hand). */
    private static EquipmentSlot preferredSlot(ItemStack stack) {
        //? if >=1.21 {
        /*net.minecraft.item.Equipment eq = net.minecraft.item.Equipment.fromStack(stack);
        if (eq != null) return eq.getSlotType();
        return stack.isOf(net.minecraft.item.Items.SHIELD) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        *///?} else {
        return LivingEntity.getPreferredEquipmentSlot(stack);
        //?}
    }

    /** 1.21 split the ARMOR slot type into humanoid and animal armor. */
    private static boolean isArmor(EquipmentSlot slot) {
        //? if >=1.21 {
        /*return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
        *///?} else {
        return slot.getType() == EquipmentSlot.Type.ARMOR;
        //?}
    }

    /** Client constructor. */
    public NpcEquipScreenHandler(int syncId, PlayerInventory playerInv) {
        this(syncId, playerInv, new SimpleInventory(NpcEquipmentInventory.ORDER.length), null);
    }

    /** Server constructor. */
    public NpcEquipScreenHandler(int syncId, PlayerInventory playerInv, Inventory equip, @Nullable NotchNpcEntity npc) {
        super(ModScreenHandlers.NPC_EQUIP, syncId);
        this.equip = equip;
        this.npc = npc;

        // 0-3: armor (helmet, chest, legs, boots).
        for (int i = 0; i < 4; i++) {
            this.addSlot(new EquipSlot(equip, i, ARMOR_X, ARMOR_Y + i * 18, NpcEquipmentInventory.ORDER[i]));
        }
        // 4: main hand, 5: off hand.
        this.addSlot(new EquipSlot(equip, 4, HAND_X, MAIN_Y, EquipmentSlot.MAINHAND));
        this.addSlot(new EquipSlot(equip, 5, HAND_X, OFF_Y, EquipmentSlot.OFFHAND));

        // 6-32: player inventory; 33-41: hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (index < 6) {
            // NPC slot -> player inventory.
            if (!this.insertItem(stack, 6, 42, true)) return ItemStack.EMPTY;
        } else {
            // Player inventory -> the item's preferred slot (armor to its piece, else the hands).
            EquipmentSlot preferred = preferredSlot(stack);
            int target = indexOf(preferred);
            boolean moved = target >= 0 && this.insertItem(stack, target, target + 1, false);
            if (!moved && !isArmor(preferred)) {
                moved = this.insertItem(stack, 4, 6, false); // either hand
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        return original;
    }

    private static int indexOf(EquipmentSlot slot) {
        for (int i = 0; i < NpcEquipmentInventory.ORDER.length; i++) {
            if (NpcEquipmentInventory.ORDER[i] == slot) return i;
        }
        return -1;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return npc == null || (npc.isAlive() && player.squaredDistanceTo(npc) <= 64.0);
    }

    /** A typed equipment slot: armor slots only accept their matching piece; hands accept anything. */
    private static class EquipSlot extends Slot {
        private final EquipmentSlot type;

        EquipSlot(Inventory inv, int index, int x, int y, EquipmentSlot type) {
            super(inv, index, x, y);
            this.type = type;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (!isArmor(type)) return true;
            return preferredSlot(stack) == type;
        }

        @Override
        public int getMaxItemCount() {
            return isArmor(type) ? 1 : 64;
        }
    }
}
