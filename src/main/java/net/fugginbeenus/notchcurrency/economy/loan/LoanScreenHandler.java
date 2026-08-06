package net.fugginbeenus.notchcurrency.economy.loan;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class LoanScreenHandler extends ScreenHandler {

    public static final int P_ENABLED   = 0;
    public static final int P_DEBT      = 1;
    public static final int P_MAX       = 2;
    public static final int P_INTEREST  = 3;
    public static final int P_TERM      = 4;
    public static final int P_DAYS_LEFT = 5; // negative = overdue
    private static final int PROP_COUNT = 6;

    private final PlayerInventory playerInv;
    private final World world;
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);

    public LoanScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.LOAN, syncId);
        this.playerInv = inv;
        this.world = inv.player.getWorld();
        this.addProperties(props);
        refresh();
    }

    public int prop(int i) {
        return props.get(i);
    }

    private void refresh() {
        if (!(world instanceof ServerWorld)) return;
        if (!(playerInv.player instanceof ServerPlayerEntity sp) || sp.getServer() == null) return;
        props.set(P_ENABLED, LoanManager.isEnabled() ? 1 : 0);
        props.set(P_DEBT, clamp(LoanState.get(sp.getServer()).getDebt(sp.getUuid())));
        props.set(P_MAX, clamp(LoanManager.getMaxDebt()));
        props.set(P_INTEREST, LoanManager.getInterestPercent());
        props.set(P_TERM, LoanManager.getTermDays());
        props.set(P_DAYS_LEFT, LoanManager.daysLeft(sp.getServer(), sp.getUuid()));
    }

    private static int clamp(long v) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, v));
    }

    @Override
    public void sendContentUpdates() {
        refresh();
        super.sendContentUpdates();
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
