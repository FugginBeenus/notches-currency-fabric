package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
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

public class CoinFlipScreenHandler extends ScreenHandler {

    public static final int P_ENABLED = 0;
    public static final int P_MIN     = 1;
    public static final int P_MAX     = 2;
    public static final int P_BAL     = 3;
    public static final int P_PAYOUT  = 4; // payout percent on a win
    private static final int PROP_COUNT = 5;

    private final PlayerInventory playerInv;
    private final World world;
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);

    public CoinFlipScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.COIN_FLIP, syncId);
        this.playerInv = inv;
        this.world = inv.player.getWorld();
        this.addProperties(props);
        refresh();
    }

    public int prop(int i) { return props.get(i); }

    private void refresh() {
        if (!(world instanceof ServerWorld)) return;
        if (!(playerInv.player instanceof ServerPlayerEntity sp)) return;
        props.set(P_ENABLED, GamblingManager.isEnabled() ? 1 : 0);
        props.set(P_MIN, (int) Math.min(Integer.MAX_VALUE, GamblingManager.getMinBet()));
        props.set(P_MAX, (int) Math.min(Integer.MAX_VALUE, GamblingManager.getMaxBet()));
        props.set(P_BAL, (int) Math.min(Integer.MAX_VALUE, Math.max(0, CurrencyApi.getBalance(sp))));
        props.set(P_PAYOUT, CoinFlipManager.getPayoutPercent());
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
