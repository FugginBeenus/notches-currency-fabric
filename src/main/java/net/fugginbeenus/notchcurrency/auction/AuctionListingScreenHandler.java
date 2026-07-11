package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Backing handler for the "list an item" screen: a single real input slot the player drops
 * the item into, plus their inventory. Price and duration are entered on the client and
 * submitted via {@link NotchPackets#AUCTION_LIST}; the server reads the slot here so the item
 * is authoritative. Anything left in the slot is returned to the player on close.
 */
public class AuctionListingScreenHandler extends ScreenHandler {

    public static final int INPUT_X = 80, INPUT_Y = 24;
    public static final int INV_X = 8, INV_Y = 140, HOTBAR_Y = 198;

    private final SimpleInventory input = new SimpleInventory(1);
    private final PlayerInventory playerInv;
    // Listing fee is price-scaled, so the client is sent the knobs and computes the live fee itself.
    public static final int P_FEE_FLAT = 0, P_FEE_PERCENT = 1, P_FEE_MAX = 2;
    private final PropertyDelegate props = new ArrayPropertyDelegate(3);

    public AuctionListingScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.AUCTION_LISTING, syncId);
        this.playerInv = inv;
        addProperties(props);
        props.set(P_FEE_FLAT, AuctionConfig.LISTING_FEE_FLAT);
        props.set(P_FEE_PERCENT, AuctionConfig.LISTING_FEE_PERCENT);
        props.set(P_FEE_MAX, AuctionConfig.LISTING_FEE_MAX);

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
                (syncId, inv, p) -> new AuctionListingScreenHandler(syncId, inv),
                Text.literal("List an Item")));
    }

    public int feeFlat() { return props.get(P_FEE_FLAT); }
    public int feePercent() { return props.get(P_FEE_PERCENT); }
    public int feeMax() { return props.get(P_FEE_MAX); }

    /** The listing fee the client should show for a typed price (mirrors AuctionConfig.listingFee). */
    public long feeFor(long price) {
        long fee = feeFlat();
        if (feePercent() > 0 && price > 0) fee += (long) Math.floor(price * (feePercent() / 100.0));
        if (feeMax() > 0) fee = Math.min(fee, feeMax());
        return Math.max(0, fee);
    }

    /** Create the listing from the slot's item at the given price/duration. */
    public boolean listFromInput(ServerPlayerEntity sp, long price, int days) {
        ItemStack item = input.getStack(0);
        if (item.isEmpty()) {
            sp.sendMessage(Text.literal("Put the item you want to list in the slot.").formatted(Formatting.RED), false);
            return false;
        }
        if (price <= 0) {
            sp.sendMessage(Text.literal("Enter a price above 0.").formatted(Formatting.RED), false);
            return false;
        }
        if (!(sp.getWorld() instanceof ServerWorld world)) return false;

        long fee = AuctionConfig.listingFee(price);
        if (fee > 0) {
            if (BalanceStore.get(sp) < fee) {
                sp.sendMessage(Text.literal("You need " + fee + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " for the listing fee.").formatted(Formatting.RED), false);
                return false;
            }
            BalanceStore.subtract(sp, fee, TransactionReason.SINK, "auction listing fee");
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        }

        ItemStack listed = item.copy();
        input.setStack(0, ItemStack.EMPTY);

        long durationTicks = 0L;
        int clampedDays = 0;
        if (days > 0) {
            clampedDays = Math.max(1, Math.min(7, days));
            durationTicks = clampedDays * 24L * 60L * 60L * 20L;
        }

        AuctionState state = AuctionState.get(world);
        state.addListing(world, sp, listed, price, AuctionCategories.classify(listed), durationTicks);

        sp.sendMessage(Text.literal("Listed ").formatted(Formatting.GREEN)
                .append(listed.getName().copy().formatted(Formatting.YELLOW))
                .append(Text.literal(" for " + price + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()
                        + (clampedDays > 0 ? " (" + clampedDays + "-day auction)." : " (buy now).")).formatted(Formatting.GREEN)), false);
        sendContentUpdates();
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Return whatever is still in the input slot so items are never lost.
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
                // input slot → inventory
                if (!this.insertItem(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // inventory → input slot (one item type at a time)
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
