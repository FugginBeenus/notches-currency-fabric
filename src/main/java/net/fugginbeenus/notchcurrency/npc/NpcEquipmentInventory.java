package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * A live 6-slot {@link Inventory} view of an NPC's equipment (helmet, chest, legs, boots, main hand,
 * off hand). Writes go straight onto the entity, so the equipment GUI needs no copy/return step and
 * changes persist/sync immediately.
 */
public class NpcEquipmentInventory implements Inventory {

    public static final EquipmentSlot[] ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private final NotchNpcEntity npc;

    public NpcEquipmentInventory(NotchNpcEntity npc) {
        this.npc = npc;
    }

    @Override
    public int size() {
        return ORDER.length;
    }

    @Override
    public boolean isEmpty() {
        for (EquipmentSlot slot : ORDER) {
            if (!npc.getEquippedStack(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int index) {
        return npc.getEquippedStack(ORDER[index]);
    }

    @Override
    public ItemStack removeStack(int index, int amount) {
        ItemStack stack = npc.getEquippedStack(ORDER[index]);
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack split = stack.split(amount);
        npc.equipStack(ORDER[index], stack); // re-equip the shrunken remainder so it syncs
        return split;
    }

    @Override
    public ItemStack removeStack(int index) {
        ItemStack stack = npc.getEquippedStack(ORDER[index]);
        npc.equipStack(ORDER[index], ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setStack(int index, ItemStack stack) {
        npc.equipStack(ORDER[index], stack);
        npc.setEquipmentDropChance(ORDER[index], 1.0f); // owner's items always drop if it dies
    }

    @Override
    public void markDirty() {
        // Equipment lives on the entity; MobEntity syncs changes on its own.
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return npc.isAlive() && player.squaredDistanceTo(npc) <= 64.0;
    }

    @Override
    public void clear() {
        for (EquipmentSlot slot : ORDER) {
            npc.equipStack(slot, ItemStack.EMPTY);
        }
    }
}
