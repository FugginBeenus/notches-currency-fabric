package net.fugginbeenus.notchcurrency.economy.enchanter;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Enchanter service NPC: pay coins to repair gear, buy specific enchantment levels, or extract
 * an enchantment onto a book. Every payment is a SINK — this is one of the economy's drains, and
 * the "buy the exact enchant you want" pitch is the draw. Cost functions are deterministic and
 * shared by the client (display) and server (charge), so the number on the button is the number
 * charged.
 */
public final class EnchanterManager {

    private EnchanterManager() {}

    // Config (see NotchConfig.Enchanter; applyConfig re-reads on save).
    public static boolean enabled = true;
    public static int repairFullCost = 60;
    public static int costMultiplierPercent = 100;
    public static int extractCost = 25;
    public static boolean allowTreasure = true;

    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.enchanter.enabled;
        repairFullCost = Math.max(0, cfg.enchanter.repairFullCost);
        costMultiplierPercent = Math.max(1, cfg.enchanter.costMultiplierPercent);
        extractCost = Math.max(0, cfg.enchanter.extractCost);
        allowTreasure = cfg.enchanter.allowTreasure;
    }

    public static void openScreen(ServerPlayerEntity sp) {
        if (!enabled) {
            sp.sendMessage(Text.literal("The enchanter isn't offering services right now.")
                    .formatted(Formatting.YELLOW), false);
            return;
        }
        EnchanterScreenHandler.open(sp);
    }

    /** Coins to fully repair the stack (0 = nothing to repair). Scales with missing durability. */
    public static long repairCost(ItemStack stack, int fullCost) {
        if (stack.isEmpty() || !stack.isDamaged() || fullCost <= 0) return 0;
        return Math.max(1, Math.round(fullCost * (double) stack.getDamage() / stack.getMaxDamage()));
    }

    /** Coins for buying {@code level} of an enchant (one level step). Rarer + higher = pricier. */
    public static long upgradeCost(Enchantment ench, int level, int multiplierPercent) {
        int base = switch (ench.getRarity()) {
            case COMMON -> 15;
            case UNCOMMON -> 25;
            case RARE -> 45;
            case VERY_RARE -> 80;
        };
        long cost = (long) base * level;
        if (ench.isTreasure()) cost *= 2; // mending & friends carry a premium
        return Math.max(1, cost * multiplierPercent / 100);
    }

    /** One purchasable upgrade: the enchantment and the level being bought. */
    public record Offer(Enchantment enchantment, int level) {}

    /**
     * Every next-level the stack can take: its existing enchants below max level, plus compatible
     * new ones at level I. Curses never; treasure only when allowed. Registry-ordered, so the
     * client's rows and the server's validation always agree.
     */
    public static List<Offer> upgradeOffers(ItemStack stack, boolean treasureAllowed) {
        List<Offer> offers = new ArrayList<>();
        if (stack.isEmpty()) return offers;
        Map<Enchantment, Integer> current = EnchantmentHelper.get(stack);
        for (Enchantment ench : Registries.ENCHANTMENT) {
            if (ench.isCursed()) continue;
            if (ench.isTreasure() && !treasureAllowed) continue;
            int cur = current.getOrDefault(ench, 0);
            if (cur >= ench.getMaxLevel()) continue;
            if (cur == 0) {
                if (!ench.isAcceptableItem(stack)) continue;
                boolean clash = false;
                for (Enchantment other : current.keySet()) {
                    if (other != ench && !ench.canCombine(other)) {
                        clash = true;
                        break;
                    }
                }
                if (clash) continue;
            }
            offers.add(new Offer(ench, cur + 1));
        }
        return offers;
    }
}
