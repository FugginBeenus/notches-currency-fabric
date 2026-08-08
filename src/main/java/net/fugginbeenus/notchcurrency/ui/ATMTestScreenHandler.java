package net.fugginbeenus.notchcurrency.ui;

import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ATMTestScreenHandler extends AbstractContainerMenu {

    // ===== Top 5 slots baseline (relative to 176x166 panel) =====
    // Vanilla-like baseline puts rows at (8, 17). Keep spacing = 18px.
    private static final int BANK_BASE_X = 8;
    private static final int BANK_BASE_Y = 17;
    private static final int BANK_SPACING = 18;

    // Pixel nudges to line up with your painted frames (adjust these!)
    public static int BANK_NUDGE_X = 38; // +right / -left
    public static int BANK_NUDGE_Y = 1;  // +down  / -up

    // ===== Player inv / hotbar (leave these unless your texture differs) =====
    private static final int PLAYER_X = 8;
    private static final int PLAYER_Y = 113;
    private static final int HOTBAR_Y = PLAYER_Y + 58;

    private final Inventory playerInv;

    private final Container bankInv = new SimpleContainer(5) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (!playerInv.player.level().isClientSide && playerInv.player instanceof ServerPlayer sp) {
                depositAllCoins(sp);
            }
        }
    };

    public ATMTestScreenHandler(int containerId, Inventory playerInv) {
        super(ModScreenHandlers.ATM, containerId);
        this.playerInv = playerInv;

        // ----- Top 5 slots (with nudges) -----
        final int rowY = BANK_BASE_Y + BANK_NUDGE_Y;
        for (int i = 0; i < 5; i++) {
            int x = BANK_BASE_X + BANK_NUDGE_X + (i * BANK_SPACING);
            this.addSlot(new CurrencySlot(bankInv, i, x, rowY));
        }

        // ----- Player inventory -----
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        PLAYER_X + col * 18,
                        PLAYER_Y + row * 18));
            }
        }

        // ----- Hotbar -----
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, PLAYER_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            if (isCurrency(stack)) {
                int count = stack.getCount();
                if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
                    slot.setByPlayer(ItemStack.EMPTY);
                    slot.setChanged();
                    depositAmount(sp, count);
                }
            }
        }
        return result;
    }

    private boolean isCurrency(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ModItems.NOTCH_COIN);
    }

    private void depositAllCoins(ServerPlayer sp) {
        int total = 0;
        for (int i = 0; i < bankInv.getContainerSize(); i++) {
            ItemStack s = bankInv.getItem(i);
            if (isCurrency(s)) {
                total += s.getCount();
                bankInv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (total > 0) {
            depositAmount(sp, total);
        }
    }

    private void depositAmount(ServerPlayer sp, int amount) {
        if (amount <= 0) return;
        CoinEconomy.depositToBalance(sp, amount);
        NotchPackets.sendBalance(sp, BalanceStore.get(sp));
    }

    public void withdraw(int amount) {
        if (amount <= 0) {
            return;
        }

        if (!playerInv.player.level().isClientSide && playerInv.player instanceof ServerPlayer sp) {
            long withdrawn = CoinEconomy.withdrawFromBalanceToInventory(sp, amount);

            if (withdrawn <= 0) {
                // optional feedback if they don't have enough balance
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You don't have enough balance to withdraw that many Notch Coins.")
                                .withStyle(ChatFormatting.RED));
            }

            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        }
    }

    private class CurrencySlot extends Slot {
        public CurrencySlot(Container inv, int index, int x, int y) {
            super(inv, index, x, y);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return isCurrency(stack);
        }
    }
}
