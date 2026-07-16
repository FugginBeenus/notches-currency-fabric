package net.fugginbeenus.notchcurrency.economy.enchanter;

import net.fugginbeenus.notchcurrency.compat.StackData;

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
    public static int uncraftCost = 30;
    public static int costCommon = 15, costUncommon = 25, costRare = 45, costVeryRare = 80;
    public static int treasureMultiplierPercent = 200;
    public static int extractValuePercent = 100;

    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.enchanter.enabled;
        repairFullCost = Math.max(0, cfg.enchanter.repairFullCost);
        costMultiplierPercent = Math.max(1, cfg.enchanter.costMultiplierPercent);
        extractCost = Math.max(0, cfg.enchanter.extractCost);
        allowTreasure = cfg.enchanter.allowTreasure;
        uncraftCost = Math.max(0, cfg.enchanter.uncraftCost);
        costCommon = Math.max(1, cfg.enchanter.costCommon);
        costUncommon = Math.max(1, cfg.enchanter.costUncommon);
        costRare = Math.max(1, cfg.enchanter.costRare);
        costVeryRare = Math.max(1, cfg.enchanter.costVeryRare);
        treasureMultiplierPercent = Math.max(100, cfg.enchanter.treasureMultiplierPercent);
        extractValuePercent = Math.max(0, cfg.enchanter.extractValuePercent);
    }

    /** The price knobs bundled up, so the client screen prices with the SERVER's synced values. */
    public record Pricing(int common, int uncommon, int rare, int veryRare, int treasurePct, int globalPct,
                          int extractBase, int extractValuePct) {}

    /** The server's live pricing (from config). The handler syncs it to clients as properties. */
    public static Pricing pricing() {
        return new Pricing(costCommon, costUncommon, costRare, costVeryRare,
                treasureMultiplierPercent, costMultiplierPercent, extractCost, extractValuePercent);
    }

    /** Coins to pull an enchant onto a book: a flat handling fee plus a share of the enchant's own
     *  purchase price — so extraction can never undercut what the enchant is worth (no book farms). */
    public static long extractPrice(Enchantment ench, int level, Pricing p) {
        return Math.max(1, p.extractBase() + upgradeCost(ench, level, p) * p.extractValuePct() / 100);
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
    public static long upgradeCost(Enchantment ench, int level, Pricing p) {
        int base = switch (ench.getRarity()) {
            case COMMON -> p.common();
            case UNCOMMON -> p.uncommon();
            case RARE -> p.rare();
            case VERY_RARE -> p.veryRare();
        };
        long cost = (long) base * level;
        if (ench.isTreasure()) cost = cost * p.treasurePct() / 100; // mending & friends carry a premium
        return Math.max(1, cost * p.globalPct() / 100);
    }

    /** One purchasable upgrade: the enchantment and the level being bought. */
    public record Offer(Enchantment enchantment, int level) {}

    /** An uncraft quote: how many of the item one craft consumes, and what comes back. */
    public record UncraftPlan(int consumed, List<ItemStack> returns) {}

    /**
     * Work out what the item breaks back into: the first crafting recipe whose output is this item.
     * Same code runs client-side (preview) and server-side (validation), so the display matches the
     * result. Null when there's no recipe, the stack is too small for one craft, or the item is
     * damaged (no salvaging worn-out gear for full materials).
     */
    @org.jetbrains.annotations.Nullable
    public static UncraftPlan uncraftPlan(ItemStack stack, net.minecraft.world.World world) {
        if (stack.isEmpty() || stack.isDamaged()) return null;
        for (net.minecraft.recipe.CraftingRecipe recipe
                : world.getRecipeManager().listAllOfType(net.minecraft.recipe.RecipeType.CRAFTING)) {
            ItemStack out = recipe.getOutput(world.getRegistryManager());
            if (out.isEmpty() || !out.isOf(stack.getItem())) continue;
            if (stack.getCount() < out.getCount()) continue;
            List<ItemStack> returns = new ArrayList<>();
            for (net.minecraft.recipe.Ingredient ing : recipe.getIngredients()) {
                if (ing.isEmpty()) continue;
                ItemStack[] options = ing.getMatchingStacks();
                if (options.length == 0 || options[0].isEmpty()) continue;
                boolean merged = false;
                for (ItemStack have : returns) {
                    if (StackData.canCombine(have, options[0])) {
                        have.increment(1);
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    ItemStack one = options[0].copy();
                    one.setCount(1);
                    returns.add(one);
                }
            }
            if (returns.isEmpty()) continue; // special recipes (fireworks etc.) expose no ingredients
            return new UncraftPlan(out.getCount(), returns);
        }
        return null;
    }

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
