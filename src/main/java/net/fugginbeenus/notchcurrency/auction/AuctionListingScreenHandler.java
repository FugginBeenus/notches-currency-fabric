package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AuctionListingScreenHandler extends AbstractContainerMenu {

    public static final int INPUT_X = 80, INPUT_Y = 24;
    public static final int INV_X = 8, INV_Y = 140, HOTBAR_Y = 198;
    private final SimpleContainer input = new SimpleContainer(1);
    private final Inventory playerInv;
    public static final int P_FEE_FLAT = 0, P_FEE_PERCENT = 1, P_FEE_MAX = 2;
    private final ContainerData props = new SimpleContainerData(3);

    public AuctionListingScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.AUCTION_LISTING, containerId);
        this.playerInv = inv;
        addDataSlots(props);
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

    public static void open(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new AuctionListingScreenHandler(containerId, inv),
                Component.literal("List an Item")));
    }

    public int feeFlat() { return props.get(P_FEE_FLAT); }
    public int feePercent() { return props.get(P_FEE_PERCENT); }
    public int feeMax() { return props.get(P_FEE_MAX); }

    public long feeFor(long price) {
        long fee = feeFlat();
        if (feePercent() > 0 && price > 0) fee += (long) Math.floor(price * (feePercent() / 100.0));
        if (feeMax() > 0) fee = Math.min(fee, feeMax());
        return Math.max(0, fee);
    }

    public boolean listFromInput(ServerPlayer sp, long price, int days) {
        ItemStack item = input.getItem(0);
        if (item.isEmpty()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Put the item you want to list in the slot.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (price <= 0) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Enter a price above 0.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!(sp.level() instanceof ServerLevel world)) return false;

        long fee = AuctionConfig.listingFee(price);
        if (fee > 0) {
            if (BalanceStore.get(sp) < fee) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You need " + fee + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " for the listing fee.").withStyle(ChatFormatting.RED));
                return false;
            }
            BalanceStore.subtract(sp, fee, TransactionReason.SINK, "auction listing fee");
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        }

        ItemStack listed = item.copy();
        input.setItem(0, ItemStack.EMPTY);

        long durationTicks = 0L;
        int clampedDays = 0;
        if (days > 0) {
            clampedDays = Math.max(1, Math.min(7, days));
            durationTicks = clampedDays * 24L * 60L * 60L * 20L;
        }

        AuctionState state = AuctionState.get(world);
        state.addListing(world, sp, listed, price, AuctionCategories.classify(listed), durationTicks);

        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Listed ").withStyle(ChatFormatting.GREEN)
                .append(listed.getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" for " + price + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()
                        + (clampedDays > 0 ? " (" + clampedDays + "-day auction)." : " (buy now).")).withStyle(ChatFormatting.GREEN)));
        broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
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
