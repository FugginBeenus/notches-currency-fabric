package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class NpcEquipScreenHandler extends AbstractContainerMenu {

    public static final int ARMOR_X = 14, ARMOR_Y = 28;
    public static final int HAND_X = 14, OFF_X = 86, MAIN_Y = 112, OFF_Y = 112;
    public static final int INV_X = 43, INV_Y = 158, HOTBAR_Y = 216;
    public static final int TRINKET_X = 116, TRINKET_Y = 28;
    public static final int MAX_TRINKETS = 8;

    private final Container equip;
    @Nullable private final NotchNpcEntity npc;
    @Nullable private final java.util.UUID npcId;
    private final int trinketCount;
    private final String[] trinketLabels;

    private static EquipmentSlot preferredSlot(ItemStack stack) {
        //? if >=1.21.11 {
        /*net.minecraft.world.item.equipment.Equippable eq =
                stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (eq != null) return eq.slot();
        return stack.is(net.minecraft.world.item.Items.SHIELD) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        *///?} elif >=1.21 {
        /*net.minecraft.world.item.Equipable eq = net.minecraft.world.item.Equipable.get(stack);
        if (eq != null) return eq.getEquipmentSlot();
        return stack.is(net.minecraft.world.item.Items.SHIELD) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
        *///?} else {
        return LivingEntity.getEquipmentSlotForItem(stack);
        //?}
    }

    private static boolean isArmor(EquipmentSlot slot) {
        //? if >=1.21 {
        /*return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
        *///?} else {
        return slot.getType() == EquipmentSlot.Type.ARMOR;
        //?}
    }

    public NpcEquipScreenHandler(int containerId, Inventory playerInv, net.minecraft.network.FriendlyByteBuf buf) {
        this(containerId, playerInv, new SimpleContainer(NpcEquipmentInventory.ORDER.length), null,
                buf.readBoolean() ? buf.readUUID() : null);
    }

    public NpcEquipScreenHandler(int containerId, Inventory playerInv, Container equip, @Nullable NotchNpcEntity npc) {
        this(containerId, playerInv, equip, npc, npc != null ? npc.getUUID() : null);
    }

    private NpcEquipScreenHandler(int containerId, Inventory playerInv, Container equip,
                                  @Nullable NotchNpcEntity npc, @Nullable java.util.UUID npcId) {
        super(ModScreenHandlers.NPC_EQUIP, containerId);
        this.equip = equip;
        this.npc = npc;
        this.npcId = npcId;

        for (int i = 0; i < 4; i++) {
            this.addSlot(new EquipSlot(equip, i, ARMOR_X, ARMOR_Y + i * 18, NpcEquipmentInventory.ORDER[i]));
        }

        this.addSlot(new EquipSlot(equip, 4, HAND_X, MAIN_Y, EquipmentSlot.MAINHAND));
        this.addSlot(new EquipSlot(equip, 5, OFF_X, OFF_Y, EquipmentSlot.OFFHAND));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }

        int count = 0;
        String[] labels = new String[0];
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            var specs = NpcTrinkets.slotSpecs(
                    net.fugginbeenus.notchcurrency.registry.ModEntities.NOTCH_NPC, MAX_TRINKETS);
            labels = new String[specs.size()];
            for (int i = 0; i < specs.size(); i++) {
                var spec = specs.get(i);
                labels[i] = spec.slot();
                int sx = TRINKET_X + (i % 2) * 18;
                int sy = TRINKET_Y + (i / 2) * 18;
                Container backing = npc != null ? NpcTrinkets.inventoryFor(npc, spec.group(), spec.slot()) : null;
                this.addSlot(new Slot(backing != null ? backing : new SimpleContainer(spec.index() + 1),
                        spec.index(), sx, sy));
                count++;
            }
        }
        this.trinketCount = count;
        this.trinketLabels = labels;
    }

    public int trinketCount() {
        return trinketCount;
    }

    public String trinketLabel(int i) {
        return trinketLabels[i];
    }

    @Nullable
    public java.util.UUID npcId() {
        return npcId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 6 || index >= 42) {
            if (!this.moveItemStackTo(stack, 6, 42, true)) return ItemStack.EMPTY;
        } else {

            EquipmentSlot preferred = preferredSlot(stack);
            int target = indexOf(preferred);
            boolean moved = target >= 0 && this.moveItemStackTo(stack, target, target + 1, false);
            if (!moved && !isArmor(preferred)) {
                moved = this.moveItemStackTo(stack, 4, 6, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    private static int indexOf(EquipmentSlot slot) {
        for (int i = 0; i < NpcEquipmentInventory.ORDER.length; i++) {
            if (NpcEquipmentInventory.ORDER[i] == slot) return i;
        }
        return -1;
    }

    @Override
    public boolean stillValid(Player player) {
        return npc == null || (npc.isAlive() && player.distanceToSqr(npc) <= 64.0);
    }

    private static class EquipSlot extends Slot {
        private final EquipmentSlot type;

        EquipSlot(Container inv, int index, int x, int y, EquipmentSlot type) {
            super(inv, index, x, y);
            this.type = type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isArmor(type)) return true;
            return preferredSlot(stack) == type;
        }

        @Override
        public int getMaxStackSize() {
            return isArmor(type) ? 1 : 64;
        }
    }
}
