package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CoinFlipScreenHandler extends AbstractContainerMenu {

    public static final int P_ENABLED = 0;
    public static final int P_MIN     = 1;
    public static final int P_MAX     = 2;
    public static final int P_BAL     = 3;
    public static final int P_PAYOUT  = 4;
    private static final int PROP_COUNT = 5;

    private final Inventory playerInv;
    private final Level world;
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);

    public CoinFlipScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.COIN_FLIP, containerId);
        this.playerInv = inv;
        this.world = inv.player.level();
        this.addDataSlots(props);
        refresh();
    }

    public int prop(int i) { return props.get(i); }

    private void refresh() {
        if (!(world instanceof ServerLevel)) return;
        if (!(playerInv.player instanceof ServerPlayer sp)) return;
        props.set(P_ENABLED, GamblingManager.isEnabled() ? 1 : 0);
        props.set(P_MIN, (int) Math.min(Integer.MAX_VALUE, GamblingManager.getMinBet()));
        props.set(P_MAX, (int) Math.min(Integer.MAX_VALUE, GamblingManager.getMaxBet()));
        props.set(P_BAL, (int) Math.min(Integer.MAX_VALUE, Math.max(0, CurrencyApi.getBalance(sp))));
        props.set(P_PAYOUT, CoinFlipManager.getPayoutPercent());
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
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
