package net.fugginbeenus.notchcurrency.economy.loan;

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

public class LoanScreenHandler extends AbstractContainerMenu {

    public static final int P_ENABLED   = 0;
    public static final int P_DEBT      = 1;
    public static final int P_MAX       = 2;
    public static final int P_INTEREST  = 3;
    public static final int P_TERM      = 4;
    public static final int P_DAYS_LEFT = 5; // negative = overdue
    private static final int PROP_COUNT = 6;

    private final Inventory playerInv;
    private final Level world;
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);

    public LoanScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.LOAN, containerId);
        this.playerInv = inv;
        this.world = inv.player.level();
        this.addDataSlots(props);
        refresh();
    }

    public int prop(int i) {
        return props.get(i);
    }

    private void refresh() {
        if (!(world instanceof ServerLevel)) return;
        if (!(playerInv.player instanceof ServerPlayer sp) || sp.getServer() == null) return;
        props.set(P_ENABLED, LoanManager.isEnabled() ? 1 : 0);
        props.set(P_DEBT, clamp(LoanState.get(sp.getServer()).getDebt(sp.getUUID())));
        props.set(P_MAX, clamp(LoanManager.getMaxDebt()));
        props.set(P_INTEREST, LoanManager.getInterestPercent());
        props.set(P_TERM, LoanManager.getTermDays());
        props.set(P_DAYS_LEFT, LoanManager.daysLeft(sp.getServer(), sp.getUUID()));
    }

    private static int clamp(long v) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, v));
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
