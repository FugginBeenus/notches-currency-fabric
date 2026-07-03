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

/**
 * The "create a trade offer" screen backing handler. The OFFERED slot holds the real items that get
 * escrowed into the offer on submit; the REQUESTED slot holds a SAMPLE of the item wanted in payment
 * (its count = how many), which is copied and always returned. Price and target name come from the
 * client via TRADE_OFFER_CREATE. Anything left in the slots is returned on close.
 */
public class TradeOfferCreateScreenHandler extends ScreenHandler {

    public static final int OFFERED_X = 12, OFFERED_Y = 22;
    public static final int REQUESTED_X = 12, REQUESTED_Y = 70;
    public static final int INV_X = 8, INV_Y = 140, HOTBAR_Y = 198;

    private final PlayerInventory playerInv;
    private final SimpleInventory samples = new SimpleInventory(2); // 0 = offered (real), 1 = requested (sample)

    public TradeOfferCreateScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.TRADE_OFFER_CREATE, syncId);
        this.playerInv = inv;
        addSlot(new Slot(samples, 0, OFFERED_X, OFFERED_Y));
        addSlot(new Slot(samples, 1, REQUESTED_X, REQUESTED_Y));
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

    public ItemStack offeredStack() { return samples.getStack(0); }
    public ItemStack requestedStack() { return samples.getStack(1); }

    /** Submit the offer. The offered item is consumed; the requested item is a sample (kept). */
    public void submit(ServerPlayerEntity sp, long price, String targetName) {
        ItemStack offered = samples.getStack(0);
        ItemStack requested = samples.getStack(1);
        if (offered.isEmpty()) {
            sp.sendMessage(Text.literal("Put the item you're offering in the top slot.").formatted(Formatting.RED), false);
            return;
        }
        if (price <= 0 && requested.isEmpty()) {
            sp.sendMessage(Text.literal("Ask for coins and/or an item in return.").formatted(Formatting.RED), false);
            return;
        }
        boolean created = TradeOfferManager.createOffer(sp, offered, price, requested, targetName);
        if (created) {
            samples.setStack(0, ItemStack.EMPTY); // escrowed into the offer
            sp.closeHandledScreen();
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
            if (index <= 1) {
                if (!this.insertItem(stack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!this.insertItem(stack, 0, 2, false)) return ItemStack.EMPTY;
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
