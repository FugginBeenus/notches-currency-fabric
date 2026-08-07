package net.fugginbeenus.notchcurrency.economy.enchanter;

import net.fugginbeenus.notchcurrency.compat.Ench;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.Map;

public class EnchanterScreenHandler extends AbstractContainerMenu {

    public static final int INPUT_X = 12, INPUT_Y = 22;
    public static final int INV_X = 47, INV_Y = 156, HOTBAR_Y = 214;

    // Property indices.
    public static final int P_REPAIR_COST = 0, P_MULTIPLIER = 1, P_EXTRACT_COST = 2, P_TREASURE = 3,
            P_UNCRAFT_COST = 4, P_COST_COMMON = 5, P_COST_UNCOMMON = 6, P_COST_RARE = 7,
            P_COST_VERY_RARE = 8, P_TREASURE_MULT = 9, P_EXTRACT_VALUE_PCT = 10;

    // Packet action ids.
    public static final int ACTION_REPAIR = 0, ACTION_UPGRADE = 1, ACTION_EXTRACT = 2, ACTION_UNCRAFT = 3;

    private final SimpleContainer input = new SimpleContainer(1);
    private final Inventory playerInv;
    private final ContainerData props = new SimpleContainerData(11);

    public EnchanterScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.ENCHANTER, containerId);
        this.playerInv = inv;
        addDataSlots(props);
        props.set(P_MULTIPLIER, EnchanterManager.costMultiplierPercent);
        props.set(P_EXTRACT_COST, EnchanterManager.extractCost);
        props.set(P_TREASURE, EnchanterManager.allowTreasure ? 1 : 0);
        props.set(P_UNCRAFT_COST, EnchanterManager.uncraftCost);
        props.set(P_COST_COMMON, EnchanterManager.costCommon);
        props.set(P_COST_UNCOMMON, EnchanterManager.costUncommon);
        props.set(P_COST_RARE, EnchanterManager.costRare);
        props.set(P_COST_VERY_RARE, EnchanterManager.costVeryRare);
        props.set(P_TREASURE_MULT, EnchanterManager.treasureMultiplierPercent);
        props.set(P_EXTRACT_VALUE_PCT, EnchanterManager.extractValuePercent);

        addSlot(new Slot(input, 0, INPUT_X, INPUT_Y));

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
                (containerId, inv, p) -> new EnchanterScreenHandler(containerId, inv),
                Component.literal("Enchanter")));
    }

    public ItemStack inputStack() {
        return this.slots.get(0).getItem();
    }

    public int repairCostProp() { return props.get(P_REPAIR_COST); }
    public int multiplierProp() { return props.get(P_MULTIPLIER); }
    public int extractCostProp() { return props.get(P_EXTRACT_COST); }
    public boolean treasureAllowedProp() { return props.get(P_TREASURE) != 0; }
    public int uncraftCostProp() { return props.get(P_UNCRAFT_COST); }

    public EnchanterManager.Pricing pricing() {
        return new EnchanterManager.Pricing(props.get(P_COST_COMMON), props.get(P_COST_UNCOMMON),
                props.get(P_COST_RARE), props.get(P_COST_VERY_RARE), props.get(P_TREASURE_MULT),
                props.get(P_MULTIPLIER), props.get(P_EXTRACT_COST), props.get(P_EXTRACT_VALUE_PCT));
    }

    @Override
    public void broadcastChanges() {
        // Keep the repair price live as the slot contents change.
        props.set(P_REPAIR_COST, (int) EnchanterManager.repairCost(input.getItem(0), EnchanterManager.repairFullCost));
        super.broadcastChanges();
    }

    // ---- actions (server side, from the ENCHANTER_ACTION packet) ----

    public void handleAction(ServerPlayer sp, int action, String enchId) {
        if (!EnchanterManager.enabled) return;
        ItemStack stack = input.getItem(0);
        if (stack.isEmpty()) {
            sp.displayClientMessage(Component.literal("Put an item in the slot first.").withStyle(ChatFormatting.RED), false);
            return;
        }
        switch (action) {
            case ACTION_REPAIR -> repair(sp, stack);
            case ACTION_UPGRADE -> upgrade(sp, stack, enchId);
            case ACTION_EXTRACT -> extract(sp, stack, enchId);
            case ACTION_UNCRAFT -> uncraft(sp, stack);
        }
    }

    private void uncraft(ServerPlayer sp, ItemStack stack) {
        EnchanterManager.UncraftPlan plan = EnchanterManager.uncraftPlan(stack, sp.level());
        if (plan == null) {
            String why = stack.isDamaged() ? "Repair it first - worn gear can't be salvaged for full parts."
                    : "That item has no crafting recipe to reverse.";
            sp.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        long cost = EnchanterManager.uncraftCost;
        if (!charge(sp, cost, "enchanter uncraft")) return;
        stack.shrink(plan.consumed());
        input.setChanged();
        for (ItemStack ret : plan.returns()) {
            sp.getInventory().placeItemBackInInventory(ret.copy());
        }
        broadcastChanges();
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7f, 0.8f);
        sp.displayClientMessage(Component.literal("Uncrafted into its parts for " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").withStyle(ChatFormatting.GREEN), false);
    }

    private void repair(ServerPlayer sp, ItemStack stack) {
        long cost = EnchanterManager.repairCost(stack, EnchanterManager.repairFullCost);
        if (cost <= 0) {
            sp.displayClientMessage(Component.literal("That item doesn't need repairs.").withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        if (!charge(sp, cost, "enchanter repair")) return;
        stack.setDamageValue(0);
        input.setChanged();
        broadcastChanges();
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        sp.displayClientMessage(Component.literal("Repaired ").withStyle(ChatFormatting.GREEN)
                .append(stack.getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" for " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").withStyle(ChatFormatting.GREEN)), false);
    }

    private void upgrade(ServerPlayer sp, ItemStack stack, String enchId) {
        Enchantment ench = enchantFromId(enchId);
        if (ench == null) return;
        // Re-derive the offer server-side: the client can only pick from what's legitimately offered.
        int level = -1;
        for (EnchanterManager.Offer offer : EnchanterManager.upgradeOffers(stack, EnchanterManager.allowTreasure)) {
            if (offer.enchantment() == ench) {
                level = offer.level();
                break;
            }
        }
        if (level < 0) {
            sp.displayClientMessage(Component.literal("That enchantment can't go on this item.").withStyle(ChatFormatting.RED), false);
            return;
        }
        long cost = EnchanterManager.upgradeCost(ench, level, EnchanterManager.pricing());
        if (!charge(sp, cost, "enchanter upgrade")) return;
        Map<Enchantment, Integer> map = Ench.get(stack);
        map.put(ench, level);
        Ench.set(map, stack);
        input.setChanged();
        broadcastChanges();
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
        sp.displayClientMessage(Component.literal("Applied ").withStyle(ChatFormatting.GREEN)
                .append(Ench.name(ench, level).copy().withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" for " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").withStyle(ChatFormatting.GREEN)), false);
    }

    private void extract(ServerPlayer sp, ItemStack stack, String enchId) {
        Enchantment ench = enchantFromId(enchId);
        if (ench == null) return;
        Map<Enchantment, Integer> map = Ench.get(stack);
        Integer level = map.get(ench);
        if (level == null) {
            sp.displayClientMessage(Component.literal("That enchantment isn't on this item.").withStyle(ChatFormatting.RED), false);
            return;
        }
        long cost = EnchanterManager.extractPrice(ench, level, EnchanterManager.pricing());
        if (!charge(sp, cost, "enchanter extract")) return;
        map.remove(ench);
        Ench.set(map, stack);
        input.setChanged();
        ItemStack book = Ench.enchantedBook(ench, level);
        sp.getInventory().placeItemBackInInventory(book);
        broadcastChanges();
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7f, 1.1f);
        sp.displayClientMessage(Component.literal("Extracted ").withStyle(ChatFormatting.GREEN)
                .append(Ench.name(ench, level).copy().withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" onto a book.").withStyle(ChatFormatting.GREEN)), false);
    }

    @org.jetbrains.annotations.Nullable
    private Enchantment enchantFromId(String enchId) {
        ResourceLocation id = ResourceLocation.tryParse(enchId);
        return id == null ? null : Ench.byId(id);
    }

    private boolean charge(ServerPlayer sp, long cost, String detail) {
        if (cost <= 0) return true;
        if (BalanceStore.get(sp) < cost) {
            sp.displayClientMessage(Component.literal("You need " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " for that.").withStyle(ChatFormatting.RED), false);
            return false;
        }
        BalanceStore.subtract(sp, cost, TransactionReason.SINK, detail);
        NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Return whatever is still in the slot so items are never lost.
        if (!player.level().isClientSide && !input.getItem(0).isEmpty()) {
            ItemStack leftover = input.removeItemNoUpdate(0);
            if (!player.getInventory().add(leftover)) {
                player.drop(leftover, false);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
