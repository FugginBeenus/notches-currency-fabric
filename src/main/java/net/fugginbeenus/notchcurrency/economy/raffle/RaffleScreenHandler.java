package net.fugginbeenus.notchcurrency.economy.raffle;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RaffleScreenHandler extends AbstractContainerMenu {

    public static final int P_ENABLED     = 0;
    public static final int P_POT         = 1;
    public static final int P_PRICE       = 2;
    public static final int P_YOURS       = 3;
    public static final int P_TOTAL       = 4;
    public static final int P_HAS_ITEM    = 5;
    public static final int P_LOSERS      = 6;
    public static final int P_CLAIM_COINS = 7;
    public static final int P_HAS_CLAIM   = 8;
    public static final int P_ROUND       = 9;
    public static final int P_CAN_REDEEM  = 10;
    public static final int P_COINS_POOL  = 11; // admin guaranteed coins
    private static final int PROP_COUNT    = 12;

    private final Inventory playerInv;
    private final Level world;
    private final SimpleContainer prizeInv = new SimpleContainer(1);
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean mayPlace(ItemStack s) { return false; }
        @Override public boolean mayPickup(Player p) { return false; }
    }

    public RaffleScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.RAFFLE, containerId);
        this.playerInv = inv;
        this.world = inv.player.level();
        this.addDataSlots(props);
        this.addSlot(new ReadOnlySlot(prizeInv, 0, -10000, -10000));
        refreshFromState();
    }

    public ItemStack getPrizeStack() {
        return prizeInv.getItem(0);
    }

    public int prop(int index) {
        return props.get(index);
    }

    private void refreshFromState() {
        if (!(world instanceof ServerLevel)) return;
        if (!(playerInv.player instanceof ServerPlayer sp) || sp.getServer() == null) return;

        RaffleState state = RaffleState.get(sp.getServer());
        props.set(P_ENABLED, RaffleManager.isEnabled() ? 1 : 0);
        props.set(P_POT, (int) Math.min(Integer.MAX_VALUE, state.getPot()));
        props.set(P_PRICE, (int) Math.min(Integer.MAX_VALUE, RaffleManager.getTicketPrice()));
        props.set(P_YOURS, state.getTickets(sp.getUUID()));
        props.set(P_TOTAL, state.getTotalTickets());
        props.set(P_HAS_ITEM, state.getPrizeItem().isEmpty() ? 0 : 1);
        props.set(P_LOSERS, RaffleManager.countLoserEntries(sp));
        props.set(P_CLAIM_COINS, (int) Math.min(Integer.MAX_VALUE, RaffleManager.unclaimedPrizeTotal(sp)));
        props.set(P_HAS_CLAIM, RaffleManager.hasUnclaimedWin(sp) ? 1 : 0);
        props.set(P_ROUND, (int) Math.min(Integer.MAX_VALUE, state.getCurrentRound()));
        props.set(P_CAN_REDEEM, RaffleManager.canRedeem(sp) ? 1 : 0);
        props.set(P_COINS_POOL, (int) Math.min(Integer.MAX_VALUE, state.getCoinsPool()));
        prizeInv.setItem(0, state.getPrizeItem().copy());
    }

    @Override
    public void broadcastChanges() {
        refreshFromState(); // keep pot / odds / claim status live
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return false;
        switch (id) {
            case 0 -> RaffleManager.buyTicket(sp, 1);
            case 1 -> RaffleManager.buyTicket(sp, 5);
            case 2 -> RaffleManager.buyTicket(sp, 10);
            case 3 -> RaffleManager.claim(sp);
            case 4 -> RaffleManager.redeemTicket(sp);
            default -> { return false; }
        }
        refreshFromState();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
