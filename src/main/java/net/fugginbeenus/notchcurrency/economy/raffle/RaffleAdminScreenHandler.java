package net.fugginbeenus.notchcurrency.economy.raffle;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
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
import net.minecraft.world.World;

public class RaffleAdminScreenHandler extends ScreenHandler {

    public static final int A_PRICE    = 0;
    public static final int A_CUT      = 1;
    public static final int A_INTERVAL = 2; // minutes (GUI shows days)
    public static final int A_ENABLED  = 3;
    public static final int A_COINS    = 4;
    private static final int PROP_COUNT = 5;

    public static final int CUR_X = 15, CUR_Y = 23;
    public static final int INPUT_X = 41, INPUT_Y = 23;
    public static final int INV_X = 8, INV_Y = 168, HOTBAR_Y = 226;

    private final PlayerInventory playerInv;
    private final World world;
    private final SimpleInventory currentInv = new SimpleInventory(1); // read-only display
    private final SimpleInventory inputInv = new SimpleInventory(1);    // interactive template
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) { return false; }
        @Override public boolean canTakeItems(PlayerEntity p) { return false; }
    }

    public RaffleAdminScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.RAFFLE_ADMIN, syncId);
        this.playerInv = inv;
        this.world = inv.player.getWorld();
        this.addProperties(props);

        this.addSlot(new ReadOnlySlot(currentInv, 0, CUR_X, CUR_Y));    // index 0
        this.addSlot(new Slot(inputInv, 0, INPUT_X, INPUT_Y));          // index 1
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
        refresh();
    }

    public static void open(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new RaffleAdminScreenHandler(syncId, inv),
                Text.literal("Raffle Setup")));
    }

    public ItemStack getCurrentPrizeStack() {
        return currentInv.getStack(0);
    }

    public int prop(int i) {
        return props.get(i);
    }

    private void refresh() {
        if (!(world instanceof ServerWorld)) return;
        if (!(playerInv.player instanceof ServerPlayerEntity sp) || sp.getServer() == null) return;
        NotchConfig.Raffle r = NotchConfigIO.get().raffle;
        props.set(A_PRICE, (int) Math.min(Integer.MAX_VALUE, r.ticketPrice));
        props.set(A_CUT, r.houseCutPercent);
        props.set(A_INTERVAL, r.drawIntervalMinutes);
        props.set(A_ENABLED, r.enabled ? 1 : 0);
        props.set(A_COINS, (int) Math.min(Integer.MAX_VALUE, RaffleState.get(sp.getServer()).getCoinsPool()));
        currentInv.setStack(0, RaffleState.get(sp.getServer()).getPrizeItem().copy());
    }

    public void applyPrizeFromInput(ServerPlayerEntity sp) {
        ItemStack template = inputInv.getStack(0);
        if (!template.isEmpty()) {
            RaffleState state = RaffleState.get(sp.getServer());
            ItemStack previous = state.getPrizeItem().copy();
            state.setPrizeItem(template.copy());
            inputInv.setStack(0, ItemStack.EMPTY); // taken into escrow
            if (!previous.isEmpty()) sp.getInventory().offerOrDrop(previous);
        }
    }

    @Override
    public void sendContentUpdates() {
        refresh();
        super.sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        if (!sp.hasPermissionLevel(2)) return false;
        switch (id) {
            case 0 -> RaffleManager.clearPrize(sp);            // clear configured prize
            case 1 -> RaffleManager.draw(sp.getServer(), true); // draw now
            case 2 -> RaffleManager.resetAndReturn(sp); // wipe round, return escrowed prize
            default -> { return false; }
        }
        refresh();
        sendContentUpdates();
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            int invStart = 2; // after current(0) + input(1)
            int invEnd = this.slots.size();
            if (index == 1) {
                if (!this.insertItem(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (index >= invStart) {
                if (!this.insertItem(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY; // read-only display
            }
            if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return result;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!player.getWorld().isClient && !inputInv.getStack(0).isEmpty()) {
            ItemStack leftover = inputInv.removeStack(0);
            if (!player.getInventory().insertStack(leftover)) {
                player.dropItem(leftover, false);
            }
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.hasPermissionLevel(2);
    }
}
