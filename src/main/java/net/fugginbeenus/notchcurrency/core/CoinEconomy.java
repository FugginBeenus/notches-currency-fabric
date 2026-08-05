package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bridge / compatibility layer used by:
 * - ATMTestScreenHandler
 * - NpcShopLogic
 * - Any other mods that want a simple CoinEconomy API.
 */
public final class CoinEconomy {

    private static final Logger LOGGER = LogManager.getLogger("NotchCurrency-CoinEconomy");

    private CoinEconomy() {}

    // ------------------------------------------------------------------------
    // ATM METHODS (already referenced in your code)
    // ------------------------------------------------------------------------

    /** ATM → deposit money into balance (no items). */
    public static void depositToBalance(ServerPlayerEntity player, int amount) {
        if (player == null || amount <= 0) return;

        CurrencyApi.deposit(player, amount,
                net.fugginbeenus.notchcurrency.economy.TransactionReason.ATM_DEPOSIT, "ATM deposit");
        LOGGER.info("[CoinEconomy] depositToBalance {} -> {}",
                player.getName().getString(), amount);
    }

    /**
     * ATM → withdraw from balance and (optionally) give physical coins.
     *
     * For now we just withdraw from the virtual balance and return how much
     * was actually withdrawn. You can later add logic to spawn / add coin items.
     */
    public static long withdrawFromBalanceToInventory(ServerPlayerEntity player, int amount) {
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

    // ------------------------------------------------------------------------
    // GENERIC ECONOMY HELPERS
    // ------------------------------------------------------------------------

    /**
     * Try to charge a player some amount.
     * @param silent if false, also sends a chat toast on failure.
     */
    public static boolean tryCharge(ServerPlayerEntity player, int amount, boolean silent) {
        if (player == null || amount <= 0) return true;

        boolean ok = CurrencyApi.withdraw(player, amount);
        if (!ok) {
            if (!silent) {
                player.sendMessage(Text.literal("§cYou don't have enough " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "."), true);
            }
            LOGGER.info("[CoinEconomy] tryCharge FAILED for {} (need {}, has {})",
                    player.getName().getString(), amount, CurrencyApi.getBalance(player));
        } else {
            LOGGER.info("[CoinEconomy] tryCharge OK for {} (amount {})",
                    player.getName().getString(), amount);
        }
        return ok;
    }

    /**
     * Give physical coins to a player's inventory.
     * @param silent if false, sends a message to the player.
     */
    public static void give(ServerPlayerEntity player, int amount, boolean silent) {
        if (player == null || amount <= 0) return;

        // Give physical coin items
        net.minecraft.item.ItemStack coins = new net.minecraft.item.ItemStack(
                net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN,
                amount
        );

        // Insert what fits, then ALWAYS drop the remainder. (insertStack returns true when
        // it places *some*, so a partial fit used to silently discard the overflow.)
        player.getInventory().insertStack(coins);
        while (!coins.isEmpty()) {
            int n = Math.min(coins.getCount(), coins.getMaxCount());
            net.minecraft.item.ItemStack drop = coins.copy();
            drop.setCount(n);
            player.dropItem(drop, false);
            coins.decrement(n);
        }

        if (!silent) {
            player.sendMessage(
                    Text.literal("§aYou received §e" + amount + "§a " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "."),
                    true
            );
        }
        LOGGER.info("[CoinEconomy] give {} physical coins to {}", amount, player.getName().getString());
    }

    /**
     * Give coins to a player's virtual balance (not physical items).
     * Use this for payouts that should go to balance, not inventory.
     */
    public static void giveToBalance(ServerPlayerEntity player, int amount, boolean silent) {
        if (player == null || amount <= 0) return;

        CurrencyApi.deposit(player, amount);
        if (!silent) {
            player.sendMessage(
                    Text.literal("§aYou received §e" + amount + "§a " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to your balance."),
                    true
            );
        }
        LOGGER.info("[CoinEconomy] giveToBalance {} coins to {}", amount, player.getName().getString());
    }
}