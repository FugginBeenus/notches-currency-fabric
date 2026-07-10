package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * The "create a trade offer" screen backing handler, laid out like the live-trade screen: a 3×3
 * GIVE grid of real items (escrowed into the offer on submit, with optional attached coins), and a
 * REQUESTED sample slot (its count = how many; copied and always returned). Price/coins/target come
 * from the client via TRADE_OFFER_CREATE. Anything left in the slots is returned on close.
 */
public class TradeOfferCreateScreenHandler extends ScreenHandler {

    // Two-column layout (matches the live-trade screen): a 3×3 grid per side.
    public static final int GIVE_COUNT = 9, WANT_COUNT = 9;
    public static final int GIVE_X = 29, GIVE_Y = 34;   // give grid origin (real items, escrowed)
    public static final int WANT_X = 145, WANT_Y = 34;  // want grid origin (samples, always returned)
    public static final int INV_X = 32, INV_Y = 174, HOTBAR_Y = 232;

    private final PlayerInventory playerInv;
    // 0..8 = the give grid (real items), 9..17 = the want grid (samples).
    private final SimpleInventory samples = new SimpleInventory(GIVE_COUNT + WANT_COUNT);

    public TradeOfferCreateScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.TRADE_OFFER_CREATE, syncId);
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

    public static void open(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new TradeOfferCreateScreenHandler(syncId, inv),
                Text.literal("Create Trade Offer")));
    }

    public ItemStack giveStack(int i) { return samples.getStack(i); }
    public ItemStack wantStack(int i) { return samples.getStack(GIVE_COUNT + i); }

    /** Submit the offer. The give grid is consumed (escrowed); the want grid holds samples. */
    public void submit(ServerPlayerEntity sp, long price, long giveCoins, String targetName) {
        List<ItemStack> given = new ArrayList<>();
        for (int i = 0; i < GIVE_COUNT; i++) {
            if (!samples.getStack(i).isEmpty()) given.add(samples.getStack(i));
        }
        List<ItemStack> wanted = new ArrayList<>();
        for (int i = 0; i < WANT_COUNT; i++) {
            if (!samples.getStack(GIVE_COUNT + i).isEmpty()) wanted.add(samples.getStack(GIVE_COUNT + i));
        }
        if (given.isEmpty() && giveCoins <= 0) {
            sp.sendMessage(Text.literal("Put the items (and/or coins) you're offering on the GIVE side.")
                    .formatted(Formatting.RED), false);
            return;
        }
        if (price <= 0 && wanted.isEmpty()) {
            sp.sendMessage(Text.literal("Ask for coins and/or items in return.").formatted(Formatting.RED), false);
            return;
        }
        boolean created = TradeOfferManager.createOffer(sp, given, giveCoins, price, wanted, targetName);
        if (created) {
            for (int i = 0; i < GIVE_COUNT; i++) {
                samples.setStack(i, ItemStack.EMPTY); // escrowed into the offer
            }
            sp.closeHandledScreen(); // the want-grid samples come back via onClosed
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!player.getWorld().isClient) {
            for (int i = 0; i < samples.size(); i++) {
                ItemStack st = samples.removeStack(i);
                if (!st.isEmpty() && !player.getInventory().insertStack(st)) {
                    player.dropItem(st, false);
                }
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
            int sampleSlots = GIVE_COUNT + WANT_COUNT;
            if (index < sampleSlots) {
                if (!this.insertItem(stack, sampleSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // Shift-click from the inventory fills the give grid.
                if (!this.insertItem(stack, 0, GIVE_COUNT, false)) return ItemStack.EMPTY;
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
