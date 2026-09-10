package net.fugginbeenus.notchcurrency.economy;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.core.BalanceState;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class StartingBalance {

    private StartingBalance() {}

    public static void grant(ServerPlayer sp) {
        MinecraftServer server = sp.level().getServer();
        if (server == null) return;
        BalanceState state = BalanceState.get(server);
        if (state.has(sp.getUUID())) return;
        long amount = Math.max(0L, NotchConfigIO.get().currency.startingBalance);
        if (amount <= 0) {
            state.set(sp.getUUID(), 0L);
            return;
        }
        CurrencyApi.deposit(sp, amount, TransactionReason.FAUCET, "starting balance");
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Welcome! You start with ")
                .withStyle(ChatFormatting.GREEN)
                .append(NotchCurrency.coins(amount))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
    }
}
