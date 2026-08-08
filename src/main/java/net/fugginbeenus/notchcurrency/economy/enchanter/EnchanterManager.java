package net.fugginbeenus.notchcurrency.economy.enchanter;

import net.fugginbeenus.notchcurrency.compat.Ench;
import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public record Pricing(int common, int uncommon, int rare, int veryRare, int treasurePct, int globalPct,
                          int extractBase, int extractValuePct) {}

    public static Pricing pricing() {
        return new Pricing(costCommon, costUncommon, costRare, costVeryRare,
                treasureMultiplierPercent, costMultiplierPercent, extractCost, extractValuePercent);
    }

    public static long extractPrice(Enchantment ench, int level, Pricing p) {
        return Math.max(1, p.extractBase() + upgradeCost(ench, level, p) * p.extractValuePct() / 100);
    }

    public static void openScreen(ServerPlayer sp) {
        if (!enabled) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("The enchanter isn't offering services right now.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        EnchanterScreenHandler.open(sp);
    }

    public static long repairCost(ItemStack stack, int fullCost) {
        if (stack.isEmpty() || !stack.isDamaged() || fullCost <= 0) return 0;
        return Math.max(1, Math.round(fullCost * (double) stack.getDamageValue() / stack.getMaxDamage()));
    }

    public static long upgradeCost(Enchantment ench, int level, Pricing p) {
        int base = switch (Ench.rarityTier(ench)) {
            case Ench.COMMON -> p.common();
            case Ench.UNCOMMON -> p.uncommon();
            case Ench.RARE -> p.rare();
            default -> p.veryRare();
        };
        long cost = (long) base * level;
        if (Ench.isTreasure(ench)) cost = cost * p.treasurePct() / 100; // mending & friends carry a premium
        return Math.max(1, cost * p.globalPct() / 100);
    }

    public record Offer(Enchantment enchantment, int level) {}

    public record UncraftPlan(int consumed, List<ItemStack> returns) {}

    @org.jetbrains.annotations.Nullable
    public static UncraftPlan uncraftPlan(ItemStack stack, net.minecraft.world.level.Level world) {
        if (stack.isEmpty() || stack.isDamaged()) return null;
        // 1.21.11 took the by-type lookup and the plain result accessor off recipes. What is left is
        // the full list plus assemble(), which for an ordinary crafting recipe just hands back its
        // fixed result whatever the input. A recipe that computes its output instead returns something
        // that will not match the stack below, so it is simply skipped rather than offered wrongly.
        //? if >=26.1 {
        /*if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return null;
        for (net.minecraft.world.item.crafting.RecipeHolder<?> recipeEntry
                : serverLevel.recipeAccess().getRecipes()) {
            if (!(recipeEntry.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)) continue;
            ItemStack out = recipe.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);
        *///?} elif >=1.21.11 {
        /*if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return null;
        for (net.minecraft.world.item.crafting.RecipeHolder<?> recipeEntry
                : serverLevel.recipeAccess().getRecipes()) {
            if (!(recipeEntry.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)) continue;
            ItemStack out = recipe.assemble(
                    net.minecraft.world.item.crafting.CraftingInput.EMPTY, world.registryAccess());
        *///?} elif >=1.21 {
        /*for (net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> recipeEntry
                : world.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            net.minecraft.world.item.crafting.CraftingRecipe recipe = recipeEntry.value();
            ItemStack out = recipe.getResultItem(world.registryAccess());
        *///?} else {
        for (net.minecraft.world.item.crafting.CraftingRecipe recipe
                : world.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            ItemStack out = recipe.getResultItem(world.registryAccess());
        //?}
            if (out.isEmpty() || !out.is(stack.getItem())) continue;
            if (stack.getCount() < out.getCount()) continue;
            List<ItemStack> returns = new ArrayList<>();
            // The ingredient list moved onto the placement info, and an ingredient now reports the
            // items it accepts as holders rather than as ready-made stacks.
            //? if >=1.21.11 {
            /*net.minecraft.world.item.crafting.PlacementInfo placement = recipe.placementInfo();
            if (placement.isImpossibleToPlace()) continue;
            for (net.minecraft.world.item.crafting.Ingredient ing : placement.ingredients()) {
                if (ing.isEmpty()) continue;
                ItemStack[] options = ing.items().map(ItemStack::new).toArray(ItemStack[]::new);
            *///?} else {
            for (net.minecraft.world.item.crafting.Ingredient ing : recipe.getIngredients()) {
                if (ing.isEmpty()) continue;
                ItemStack[] options = ing.getItems();
            //?}
                if (options.length == 0 || options[0].isEmpty()) continue;
                boolean merged = false;
                for (ItemStack have : returns) {
                    if (StackData.canCombine(have, options[0])) {
                        have.grow(1);
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

    public static List<Offer> upgradeOffers(ItemStack stack, boolean treasureAllowed) {
        List<Offer> offers = new ArrayList<>();
        if (stack.isEmpty()) return offers;
        Map<Enchantment, Integer> current = Ench.get(stack);
        for (Enchantment ench : Ench.all()) {
            if (Ench.isCursed(ench)) continue;
            if (Ench.isTreasure(ench) && !treasureAllowed) continue;
            int cur = current.getOrDefault(ench, 0);
            if (cur >= Ench.maxLevel(ench)) continue;
            if (cur == 0) {
                if (!Ench.isAcceptableItem(ench, stack)) continue;
                boolean clash = false;
                for (Enchantment other : current.keySet()) {
                    if (other != ench && !Ench.canCombine(ench, other)) {
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
