package net.fugginbeenus.notchcurrency.economy.raffle;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Backing handler for the raffle screen. Carries no interactive slots: just one read-only
 * display slot for the prize item and a {@link PropertyDelegate} of live numbers (pot, your
 * entries, odds inputs, claim status). The values are refreshed from {@link RaffleState}
 * every tick so the pot and odds stay live as other players enter and draws happen.
 *
 * Buttons drive {@link RaffleManager} actions; nothing here trusts client input beyond the
 * fixed button ids.
 */
public class RaffleScreenHandler extends ScreenHandler {

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

    private final PlayerInventory playerInv;
    private final World world;
    private final SimpleInventory prizeInv = new SimpleInventory(1);
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) { return false; }
        @Override public boolean canTakeItems(PlayerEntity p) { return false; }
    }

    public RaffleScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.RAFFLE, syncId);
        this.playerInv = inv;
        this.world = inv.player.getWorld();
        this.addProperties(props);
        this.addSlot(new ReadOnlySlot(prizeInv, 0, -10000, -10000));
        refreshFromState();
    }

    public ItemStack getPrizeStack() {
        return prizeInv.getStack(0);
    }

    public int prop(int index) {
        return props.get(index);
    }

    private void refreshFromState() {
        if (!(world instanceof ServerWorld)) return;
        if (!(playerInv.player instanceof ServerPlayerEntity sp) || sp.getServer() == null) return;

        RaffleState state = RaffleState.get(sp.getServer());
        props.set(P_ENABLED, RaffleManager.isEnabled() ? 1 : 0);
        props.set(P_POT, (int) Math.min(Integer.MAX_VALUE, state.getPot()));
        props.set(P_PRICE, (int) Math.min(Integer.MAX_VALUE, RaffleManager.getTicketPrice()));
        props.set(P_YOURS, state.getTickets(sp.getUuid()));
        props.set(P_TOTAL, state.getTotalTickets());
        props.set(P_HAS_ITEM, state.getPrizeItem().isEmpty() ? 0 : 1);
        props.set(P_LOSERS, RaffleManager.countLoserEntries(sp));
        props.set(P_CLAIM_COINS, (int) Math.min(Integer.MAX_VALUE, RaffleManager.unclaimedPrizeTotal(sp)));
        props.set(P_HAS_CLAIM, RaffleManager.hasUnclaimedWin(sp) ? 1 : 0);
        props.set(P_ROUND, (int) Math.min(Integer.MAX_VALUE, state.getCurrentRound()));
        props.set(P_CAN_REDEEM, RaffleManager.canRedeem(sp) ? 1 : 0);
        props.set(P_COINS_POOL, (int) Math.min(Integer.MAX_VALUE, state.getCoinsPool()));
        prizeInv.setStack(0, state.getPrizeItem().copy());
    }

    @Override
    public void sendContentUpdates() {
        refreshFromState(); // keep pot / odds / claim status live
        super.sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        switch (id) {
            case 0 -> RaffleManager.buyTicket(sp, 1);
            case 1 -> RaffleManager.buyTicket(sp, 5);
            case 2 -> RaffleManager.buyTicket(sp, 10);
            case 3 -> RaffleManager.claim(sp);
            case 4 -> RaffleManager.redeemTicket(sp);
            default -> { return false; }
        }
        refreshFromState();
        sendContentUpdates();
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
