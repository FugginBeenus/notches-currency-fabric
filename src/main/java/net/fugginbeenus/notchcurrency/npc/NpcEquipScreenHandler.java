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

    // Slot coordinates (client draws insets at these -1). Gear lives in the left container box;
    // the right box is the live preview; the player inventory sits below the divider. Trinket slots
    // (when the Trinkets mod is present) fill a 2-wide grid beside the armor column.
    public static final int ARMOR_X = 14, ARMOR_Y = 28;              // 4 slots, 18 apart
    public static final int HAND_X = 14, OFF_X = 86, MAIN_Y = 112, OFF_Y = 112; // side by side
    public static final int INV_X = 43, INV_Y = 158, HOTBAR_Y = 216;
    public static final int TRINKET_X = 116, TRINKET_Y = 28;         // 2 cols × up to 4 rows
    public static final int MAX_TRINKETS = 8;

    private final Inventory equip;
    @Nullable private final NotchNpcEntity npc;
    @Nullable private final java.util.UUID npcId;
    private final int trinketCount;
    private final String[] trinketLabels;

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

    /** Client constructor — the opening buf carries the NPC id so the live preview can find it. */
    public NpcEquipScreenHandler(int syncId, PlayerInventory playerInv, net.minecraft.network.PacketByteBuf buf) {
        this(syncId, playerInv, new SimpleInventory(NpcEquipmentInventory.ORDER.length), null,
                buf.readBoolean() ? buf.readUuid() : null);
    }

    /** Server constructor. */
    public NpcEquipScreenHandler(int syncId, PlayerInventory playerInv, Inventory equip, @Nullable NotchNpcEntity npc) {
        this(syncId, playerInv, equip, npc, npc != null ? npc.getUuid() : null);
    }

    private NpcEquipScreenHandler(int syncId, PlayerInventory playerInv, Inventory equip,
                                  @Nullable NotchNpcEntity npc, @Nullable java.util.UUID npcId) {
        super(ModScreenHandlers.NPC_EQUIP, syncId);
        this.equip = equip;
        this.npc = npc;
        this.npcId = npcId;

        // 0-3: armor (helmet, chest, legs, boots).
        for (int i = 0; i < 4; i++) {
            this.addSlot(new EquipSlot(equip, i, ARMOR_X, ARMOR_Y + i * 18, NpcEquipmentInventory.ORDER[i]));
        }
        // 4: main hand, 5: off hand.
        this.addSlot(new EquipSlot(equip, 4, HAND_X, MAIN_Y, EquipmentSlot.MAINHAND));
        this.addSlot(new EquipSlot(equip, 5, OFF_X, OFF_Y, EquipmentSlot.OFFHAND));

        // 6-32: player inventory; 33-41: hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }

        // 42+: trinket slots, appended last so the fixed index ranges above stay put. The specs come
        // from Trinkets' synced data, so client and server build identical slot lists; server slots
        // back straight onto the NPC's trinket inventories (persistence and sync ride the component).
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
                Inventory backing = npc != null ? NpcTrinkets.inventoryFor(npc, spec.group(), spec.slot()) : null;
                this.addSlot(new Slot(backing != null ? backing : new SimpleInventory(spec.index() + 1),
                        spec.index(), sx, sy));
                count++;
            }
        }
        this.trinketCount = count;
        this.trinketLabels = labels;
    }

    /** How many trinket slots this screen has (0 without the Trinkets mod). */
    public int trinketCount() {
        return trinketCount;
    }

    /** The slot name for trinket slot {@code i} (for the screen's hover hint). */
    public String trinketLabel(int i) {
        return trinketLabels[i];
    }

    /** The NPC this screen is editing (for the client-side live preview). */
    @Nullable
    public java.util.UUID npcId() {
        return npcId;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (index < 6 || index >= 42) {
            // NPC gear or trinket slot -> player inventory.
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
