package net.fugginbeenus.notchcurrency.economy.raffle;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RaffleAdminScreenHandler extends AbstractContainerMenu {

    public static final int A_PRICE    = 0;
    public static final int A_CUT      = 1;
    public static final int A_INTERVAL = 2; // minutes (GUI shows days)
    public static final int A_ENABLED  = 3;
    public static final int A_COINS    = 4;
    private static final int PROP_COUNT = 5;

    public static final int CUR_X = 15, CUR_Y = 23;
    public static final int INPUT_X = 41, INPUT_Y = 23;
    public static final int INV_X = 8, INV_Y = 168, HOTBAR_Y = 226;

    private final Inventory playerInv;
    private final Level world;
    private final SimpleContainer currentInv = new SimpleContainer(1); // read-only display
    private final SimpleContainer inputInv = new SimpleContainer(1);    // interactive template
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }

    public RaffleAdminScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.RAFFLE_ADMIN, containerId);
        this.playerInv = inv;
        this.world = inv.player.level();
        this.addDataSlots(props);

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

    public static void open(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new RaffleAdminScreenHandler(containerId, inv),
                Component.literal("Raffle Setup")));
    }

    public ItemStack getCurrentPrizeStack() {
        return currentInv.getItem(0);
    }

    public int prop(int i) {
        return props.get(i);
    }

    private void refresh() {
        if (!(world instanceof ServerLevel)) return;
        if (!(playerInv.player instanceof ServerPlayer sp) || sp.level().getServer() == null) return;
        NotchConfig.Raffle r = NotchConfigIO.get().raffle;
        props.set(A_PRICE, (int) Math.min(Integer.MAX_VALUE, r.ticketPrice));
        props.set(A_CUT, r.houseCutPercent);
        props.set(A_INTERVAL, r.drawIntervalMinutes);
        props.set(A_ENABLED, r.enabled ? 1 : 0);
        props.set(A_COINS, (int) Math.min(Integer.MAX_VALUE, RaffleState.get(sp.level().getServer()).getCoinsPool()));
        currentInv.setItem(0, RaffleState.get(sp.level().getServer()).getPrizeItem().copy());
    }

    public void applyPrizeFromInput(ServerPlayer sp) {
        ItemStack template = inputInv.getItem(0);
        if (!template.isEmpty()) {
            RaffleState state = RaffleState.get(sp.level().getServer());
            ItemStack previous = state.getPrizeItem().copy();
            state.setPrizeItem(template.copy());
            inputInv.setItem(0, ItemStack.EMPTY); // taken into escrow
            if (!previous.isEmpty()) sp.getInventory().placeItemBackInInventory(previous);
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (!net.fugginbeenus.notchcurrency.compat.Perms.isOperator(sp)) return false;
        switch (id) {
            case 0 -> RaffleManager.clearPrize(sp);            // clear configured prize
            case 1 -> RaffleManager.draw(sp.level().getServer(), true); // draw now
            case 2 -> RaffleManager.resetAndReturn(sp); // wipe round, return escrowed prize
            default -> { return false; }
        }
        refresh();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = 2; // after current(0) + input(1)
            int invEnd = this.slots.size();
            if (index == 1) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (index >= invStart) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY; // read-only display
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && !inputInv.getItem(0).isEmpty()) {
            ItemStack leftover = inputInv.removeItemNoUpdate(0);
            if (!player.getInventory().add(leftover)) {
                player.drop(leftover, false);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return net.fugginbeenus.notchcurrency.compat.Perms.isOperator(player);
    }
}
