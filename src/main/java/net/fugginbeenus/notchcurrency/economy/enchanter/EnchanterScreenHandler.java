package net.fugginbeenus.notchcurrency.economy.enchanter;

import net.fugginbeenus.notchcurrency.compat.Ench;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Backing handler for the Enchanter screen: one real input slot for the item being worked on, plus
 * the player inventory. Actions (repair / buy enchant level / extract to book) arrive via
 * {@link NotchPackets#ENCHANTER_ACTION}; the server recomputes offers and costs from the slot, so
 * the client can't invent prices. Costs are synced as properties so the display always matches the
 * server's config. Anything left in the slot is returned on close.
 */
public class EnchanterScreenHandler extends ScreenHandler {

    public static final int INPUT_X = 12, INPUT_Y = 22;
    public static final int INV_X = 47, INV_Y = 156, HOTBAR_Y = 214;

    // Property indices.
    public static final int P_REPAIR_COST = 0, P_MULTIPLIER = 1, P_EXTRACT_COST = 2, P_TREASURE = 3,
            P_UNCRAFT_COST = 4, P_COST_COMMON = 5, P_COST_UNCOMMON = 6, P_COST_RARE = 7,
            P_COST_VERY_RARE = 8, P_TREASURE_MULT = 9, P_EXTRACT_VALUE_PCT = 10;

    // Packet action ids.
    public static final int ACTION_REPAIR = 0, ACTION_UPGRADE = 1, ACTION_EXTRACT = 2, ACTION_UNCRAFT = 3;

    private final SimpleInventory input = new SimpleInventory(1);
    private final PlayerInventory playerInv;
    private final PropertyDelegate props = new ArrayPropertyDelegate(11);

    public EnchanterScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.ENCHANTER, syncId);
        this.playerInv = inv;
        addProperties(props);
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

    /** Open this screen for the player. */
    public static void open(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new EnchanterScreenHandler(syncId, inv),
                Text.literal("Enchanter")));
    }

    public ItemStack inputStack() {
        return this.slots.get(0).getStack();
    }

    public int repairCostProp() { return props.get(P_REPAIR_COST); }
    public int multiplierProp() { return props.get(P_MULTIPLIER); }
    public int extractCostProp() { return props.get(P_EXTRACT_COST); }
    public boolean treasureAllowedProp() { return props.get(P_TREASURE) != 0; }
    public int uncraftCostProp() { return props.get(P_UNCRAFT_COST); }

    /** The synced price knobs, so the client's card prices always match the server's config. */
    public EnchanterManager.Pricing pricing() {
        return new EnchanterManager.Pricing(props.get(P_COST_COMMON), props.get(P_COST_UNCOMMON),
                props.get(P_COST_RARE), props.get(P_COST_VERY_RARE), props.get(P_TREASURE_MULT),
                props.get(P_MULTIPLIER), props.get(P_EXTRACT_COST), props.get(P_EXTRACT_VALUE_PCT));
    }

    @Override
    public void sendContentUpdates() {
        // Keep the repair price live as the slot contents change.
        props.set(P_REPAIR_COST, (int) EnchanterManager.repairCost(input.getStack(0), EnchanterManager.repairFullCost));
        super.sendContentUpdates();
    }

    // ---- actions (server side, from the ENCHANTER_ACTION packet) ----

    public void handleAction(ServerPlayerEntity sp, int action, String enchId) {
        if (!EnchanterManager.enabled) return;
        ItemStack stack = input.getStack(0);
        if (stack.isEmpty()) {
            sp.sendMessage(Text.literal("Put an item in the slot first.").formatted(Formatting.RED), false);
            return;
        }
        switch (action) {
            case ACTION_REPAIR -> repair(sp, stack);
            case ACTION_UPGRADE -> upgrade(sp, stack, enchId);
            case ACTION_EXTRACT -> extract(sp, stack, enchId);
            case ACTION_UNCRAFT -> uncraft(sp, stack);
        }
    }

    /** Break the item back into its crafting ingredients for a fee (one craft's worth per click). */
    private void uncraft(ServerPlayerEntity sp, ItemStack stack) {
        EnchanterManager.UncraftPlan plan = EnchanterManager.uncraftPlan(stack, sp.getWorld());
        if (plan == null) {
            String why = stack.isDamaged() ? "Repair it first — worn gear can't be salvaged for full parts."
                    : "That item has no crafting recipe to reverse.";
            sp.sendMessage(Text.literal(why).formatted(Formatting.YELLOW), false);
            return;
        }
        long cost = EnchanterManager.uncraftCost;
        if (!charge(sp, cost, "enchanter uncraft")) return;
        stack.decrement(plan.consumed());
        input.markDirty();
        for (ItemStack ret : plan.returns()) {
            sp.getInventory().offerOrDrop(ret.copy());
        }
        sendContentUpdates();
        sp.getWorld().playSound(null, sp.getBlockPos(), SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.7f, 0.8f);
        sp.sendMessage(Text.literal("Uncrafted into its parts for " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").formatted(Formatting.GREEN), false);
    }

    private void repair(ServerPlayerEntity sp, ItemStack stack) {
        long cost = EnchanterManager.repairCost(stack, EnchanterManager.repairFullCost);
        if (cost <= 0) {
            sp.sendMessage(Text.literal("That item doesn't need repairs.").formatted(Formatting.YELLOW), false);
            return;
        }
        if (!charge(sp, cost, "enchanter repair")) return;
        stack.setDamage(0);
        input.markDirty();
        sendContentUpdates();
        sp.getWorld().playSound(null, sp.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.6f, 1.2f);
        sp.sendMessage(Text.literal("Repaired ").formatted(Formatting.GREEN)
                .append(stack.getName().copy().formatted(Formatting.YELLOW))
                .append(Text.literal(" for " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").formatted(Formatting.GREEN)), false);
    }

    private void upgrade(ServerPlayerEntity sp, ItemStack stack, String enchId) {
        Enchantment ench = enchantFromId(enchId);
        if (ench == null) return;
        // Re-derive the offer server-side — the client can only pick from what's legitimately offered.
        int level = -1;
        for (EnchanterManager.Offer offer : EnchanterManager.upgradeOffers(stack, EnchanterManager.allowTreasure)) {
            if (offer.enchantment() == ench) {
                level = offer.level();
                break;
            }
        }
        if (level < 0) {
            sp.sendMessage(Text.literal("That enchantment can't go on this item.").formatted(Formatting.RED), false);
            return;
        }
        long cost = EnchanterManager.upgradeCost(ench, level, EnchanterManager.pricing());
        if (!charge(sp, cost, "enchanter upgrade")) return;
        Map<Enchantment, Integer> map = Ench.get(stack);
        map.put(ench, level);
        Ench.set(map, stack);
        input.markDirty();
        sendContentUpdates();
        sp.getWorld().playSound(null, sp.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.8f, 1.0f);
        sp.sendMessage(Text.literal("Applied ").formatted(Formatting.GREEN)
                .append(Ench.name(ench, level).copy().formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal(" for " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").formatted(Formatting.GREEN)), false);
    }

    private void extract(ServerPlayerEntity sp, ItemStack stack, String enchId) {
        Enchantment ench = enchantFromId(enchId);
        if (ench == null) return;
        Map<Enchantment, Integer> map = Ench.get(stack);
        Integer level = map.get(ench);
        if (level == null) {
            sp.sendMessage(Text.literal("That enchantment isn't on this item.").formatted(Formatting.RED), false);
            return;
        }
        long cost = EnchanterManager.extractPrice(ench, level, EnchanterManager.pricing());
        if (!charge(sp, cost, "enchanter extract")) return;
        map.remove(ench);
        Ench.set(map, stack);
        input.markDirty();
        ItemStack book = Ench.enchantedBook(ench, level);
        sp.getInventory().offerOrDrop(book);
        sendContentUpdates();
        sp.getWorld().playSound(null, sp.getBlockPos(), SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.7f, 1.1f);
        sp.sendMessage(Text.literal("Extracted ").formatted(Formatting.GREEN)
                .append(Ench.name(ench, level).copy().formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal(" onto a book.").formatted(Formatting.GREEN)), false);
    }

    @org.jetbrains.annotations.Nullable
    private Enchantment enchantFromId(String enchId) {
        Identifier id = Identifier.tryParse(enchId);
        return id == null ? null : Ench.byId(id);
    }

    private boolean charge(ServerPlayerEntity sp, long cost, String detail) {
        if (cost <= 0) return true;
        if (BalanceStore.get(sp) < cost) {
            sp.sendMessage(Text.literal("You need " + cost + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " for that.").formatted(Formatting.RED), false);
            return false;
        }
        BalanceStore.subtract(sp, cost, TransactionReason.SINK, detail);
        NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Return whatever is still in the slot so items are never lost.
        if (!player.getWorld().isClient && !input.getStack(0).isEmpty()) {
            ItemStack leftover = input.removeStack(0);
            if (!player.getInventory().insertStack(leftover)) {
                player.dropItem(leftover, false);
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index == 0) {
                if (!this.insertItem(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(stack, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
