package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class TradeOfferCreateScreenHandler extends AbstractContainerMenu {

    // Two-column layout (matches the live-trade screen): a 3×3 grid per side.
    public static final int GIVE_COUNT = 9, WANT_COUNT = 9;
    public static final int GIVE_X = 29, GIVE_Y = 34;   // give grid origin (real items, escrowed)
    public static final int WANT_X = 145, WANT_Y = 34;  // want grid origin (samples, always returned)
    public static final int INV_X = 32, INV_Y = 174, HOTBAR_Y = 232;

    private final Inventory playerInv;
    // 0..8 = the give grid (real items), 9..17 = the want grid (samples).
    private final SimpleContainer samples = new SimpleContainer(GIVE_COUNT + WANT_COUNT);

    public TradeOfferCreateScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.TRADE_OFFER_CREATE, containerId);
        this.playerInv = inv;
        for (int i = 0; i < GIVE_COUNT; i++) {
            addSlot(new Slot(samples, i, GIVE_X + (i % 3) * 18, GIVE_Y + (i / 3) * 18));
        }
        for (int i = 0; i < WANT_COUNT; i++) {
            addSlot(new Slot(samples, GIVE_COUNT + i, WANT_X + (i % 3) * 18, WANT_Y + (i / 3) * 18));
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
                (containerId, inv, p) -> new TradeOfferCreateScreenHandler(containerId, inv),
                Component.literal("Create Trade Offer")));
    }

    public ItemStack giveStack(int i) { return samples.getItem(i); }
    public ItemStack wantStack(int i) { return samples.getItem(GIVE_COUNT + i); }

    public void submit(ServerPlayer sp, long price, long giveCoins, String targetName) {
        List<ItemStack> given = new ArrayList<>();
        for (int i = 0; i < GIVE_COUNT; i++) {
            if (!samples.getItem(i).isEmpty()) given.add(samples.getItem(i));
        }
        List<ItemStack> wanted = new ArrayList<>();
        for (int i = 0; i < WANT_COUNT; i++) {
            if (!samples.getItem(GIVE_COUNT + i).isEmpty()) wanted.add(samples.getItem(GIVE_COUNT + i));
        }
        if (given.isEmpty() && giveCoins <= 0) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Put the items (and/or " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ") you're offering on the GIVE side.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (price <= 0 && wanted.isEmpty()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Ask for " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " and/or items in return.").withStyle(ChatFormatting.RED));
            return;
        }
        boolean created = TradeOfferManager.createOffer(sp, given, giveCoins, price, wanted, targetName);
        if (created) {
            for (int i = 0; i < GIVE_COUNT; i++) {
                samples.setItem(i, ItemStack.EMPTY); // escrowed into the offer
            }
            sp.closeContainer(); // the want-grid samples come back via onClosed
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            for (int i = 0; i < samples.getContainerSize(); i++) {
                ItemStack st = samples.removeItemNoUpdate(i);
                if (!st.isEmpty() && !player.getInventory().add(st)) {
                    player.drop(st, false);
                }
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
            int sampleSlots = GIVE_COUNT + WANT_COUNT;
            if (index < sampleSlots) {
                if (!this.moveItemStackTo(stack, sampleSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // Shift-click from the inventory fills the give grid.
                if (!this.moveItemStackTo(stack, 0, GIVE_COUNT, false)) return ItemStack.EMPTY;
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
