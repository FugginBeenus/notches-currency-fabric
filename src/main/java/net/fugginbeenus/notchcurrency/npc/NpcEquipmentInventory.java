package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NpcEquipmentInventory implements Container {

    public static final EquipmentSlot[] ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private final NotchNpcEntity npc;

    public NpcEquipmentInventory(NotchNpcEntity npc) {
        this.npc = npc;
    }

    @Override
    public int getContainerSize() {
        return ORDER.length;
    }

    @Override
    public boolean isEmpty() {
        for (EquipmentSlot slot : ORDER) {
            if (!npc.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return npc.getItemBySlot(ORDER[index]);
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        ItemStack stack = npc.getItemBySlot(ORDER[index]);
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack split = stack.split(amount);
        npc.setItemSlot(ORDER[index], stack);
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = npc.getItemBySlot(ORDER[index]);
        npc.setItemSlot(ORDER[index], ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        npc.setItemSlot(ORDER[index], stack);
        npc.setDropChance(ORDER[index], 1.0f);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return npc.isAlive() && player.distanceToSqr(npc) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (EquipmentSlot slot : ORDER) {
            npc.setItemSlot(slot, ItemStack.EMPTY);
        }
    }
}
