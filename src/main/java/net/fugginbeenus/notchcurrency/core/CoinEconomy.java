package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CoinEconomy {

    private static final Logger LOGGER = LogManager.getLogger("NotchCurrency-CoinEconomy");

    private CoinEconomy() {}

    public static void depositToBalance(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) return;

        CurrencyApi.deposit(player, amount,
                net.fugginbeenus.notchcurrency.economy.TransactionReason.ATM_DEPOSIT, "ATM deposit");
        LOGGER.info("[CoinEconomy] depositToBalance {} -> {}",
                player.getName().getString(), amount);
    }

    public static long withdrawFromBalanceToInventory(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) return 0;

        boolean ok = CurrencyApi.withdraw(player, amount);
        if (!ok) {
            LOGGER.info("[CoinEconomy] withdrawFromBalanceToInventory FAILED for {} (need {}, has {})",
                    player.getName().getString(), amount, CurrencyApi.getBalance(player));
            return 0;
        }

        // TODO: [FUTURE] Give actual coin items when withdrawing from balance
        LOGGER.info("[CoinEconomy] withdrawFromBalanceToInventory {} -> {}",
                player.getName().getString(), amount);
        return amount;
    }

    public static boolean tryCharge(ServerPlayer player, int amount, boolean silent) {
        if (player == null || amount <= 0) return true;

        boolean ok = CurrencyApi.withdraw(player, amount);
        if (!ok) {
            if (!silent) {
                net.fugginbeenus.notchcurrency.compat.Msg.actionBar(player, Component.literal("You don't have enough " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
            LOGGER.info("[CoinEconomy] tryCharge FAILED for {} (need {}, has {})",
                    player.getName().getString(), amount, CurrencyApi.getBalance(player));
        } else {
            LOGGER.info("[CoinEconomy] tryCharge OK for {} (amount {})",
                    player.getName().getString(), amount);
        }
        return ok;
    }

    public static void give(ServerPlayer player, int amount, boolean silent) {
        if (player == null || amount <= 0) return;

        // Give physical coin items
        net.minecraft.world.item.ItemStack coins = new net.minecraft.world.item.ItemStack(
                net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN,
                amount
        );

        // Insert what fits, then ALWAYS drop the remainder. (insertStack returns true when
        // it places *some*, so a partial fit used to silently discard the overflow.)
        player.getInventory().add(coins);
        while (!coins.isEmpty()) {
            int n = Math.min(coins.getCount(), coins.getMaxStackSize());
            net.minecraft.world.item.ItemStack drop = coins.copy();
            drop.setCount(n);
            player.drop(drop, false);
            coins.shrink(n);
        }

        if (!silent) {
            net.fugginbeenus.notchcurrency.compat.Msg.actionBar(player, Component.literal("You received ")
                    .withStyle(net.minecraft.ChatFormatting.GREEN)
                    .append(Component.literal(String.valueOf(amount))
                            .withStyle(net.minecraft.ChatFormatting.YELLOW))
                    .append(Component.literal(" " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".")
                            .withStyle(net.minecraft.ChatFormatting.GREEN)));
        }
        LOGGER.info("[CoinEconomy] give {} physical coins to {}", amount, player.getName().getString());
    }

    public static void giveToBalance(ServerPlayer player, int amount, boolean silent) {
        if (player == null || amount <= 0) return;

        CurrencyApi.deposit(player, amount);
        if (!silent) {
            net.fugginbeenus.notchcurrency.compat.Msg.actionBar(player, Component.literal("You received ")
                    .withStyle(net.minecraft.ChatFormatting.GREEN)
                    .append(Component.literal(String.valueOf(amount))
                            .withStyle(net.minecraft.ChatFormatting.YELLOW))
                    .append(Component.literal(" " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to your balance.")
                            .withStyle(net.minecraft.ChatFormatting.GREEN)));
        }
        LOGGER.info("[CoinEconomy] giveToBalance {} coins to {}", amount, player.getName().getString());
    }
}