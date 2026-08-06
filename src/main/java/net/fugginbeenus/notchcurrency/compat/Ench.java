package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Ench {

    private Ench() {}

    public static final int COMMON = 0, UNCOMMON = 1, RARE = 2, VERY_RARE = 3;

    //? if >=1.21 {
    /*private static net.minecraft.registry.Registry<Enchantment> registry() {
        return RegistryAccess.get().get(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
    }

    private static net.minecraft.registry.entry.RegistryEntry<Enchantment> entryOf(Enchantment ench) {
        return registry().getEntry(ench);
    }
    *///?}

    public static Iterable<Enchantment> all() {
        //? if >=1.21 {
        /*return registry();
        *///?} else {
        return net.minecraft.registry.Registries.ENCHANTMENT;
        //?}
    }

    public static Identifier idOf(Enchantment ench) {
        //? if >=1.21 {
        /*return registry().getId(ench);
        *///?} else {
        return net.minecraft.registry.Registries.ENCHANTMENT.getId(ench);
        //?}
    }

    @Nullable
    public static Enchantment byId(Identifier id) {
        //? if >=1.21 {
        /*return registry().get(id);
        *///?} else {
        return net.minecraft.registry.Registries.ENCHANTMENT.get(id);
        //?}
    }

    public static boolean isCursed(Enchantment ench) {
        //? if >=1.21 {
        /*return entryOf(ench).isIn(net.minecraft.registry.tag.EnchantmentTags.CURSE);
        *///?} else {
        return ench.isCursed();
        //?}
    }

    public static boolean isTreasure(Enchantment ench) {
        //? if >=1.21 {
        /*return entryOf(ench).isIn(net.minecraft.registry.tag.EnchantmentTags.TREASURE);
        *///?} else {
        return ench.isTreasure();
        //?}
    }

    public static int rarityTier(Enchantment ench) {
        //? if >=1.21 {
        /*int weight = ench.getWeight();
        if (weight >= 10) return COMMON;
        if (weight >= 5) return UNCOMMON;
        if (weight >= 2) return RARE;
        return VERY_RARE;
        *///?} else {
        return switch (ench.getRarity()) {
            case COMMON -> COMMON;
            case UNCOMMON -> UNCOMMON;
            case RARE -> RARE;
            case VERY_RARE -> VERY_RARE;
        };
        //?}
    }

    public static int maxLevel(Enchantment ench) {
        return ench.getMaxLevel();
    }

    public static boolean isAcceptableItem(Enchantment ench, ItemStack stack) {
        return ench.isAcceptableItem(stack);
    }

    public static boolean canCombine(Enchantment a, Enchantment b) {
        //? if >=1.21 {
        /*return Enchantment.canBeCombined(entryOf(a), entryOf(b));
        *///?} else {
        return a.canCombine(b);
        //?}
    }

    public static Text name(Enchantment ench, int level) {
        //? if >=1.21 {
        /*return Enchantment.getName(entryOf(ench), level);
        *///?} else {
        return ench.getName(level);
        //?}
    }

    public static Map<Enchantment, Integer> get(ItemStack stack) {
        //? if >=1.21 {
        /*Map<Enchantment, Integer> map = new LinkedHashMap<>();
        net.minecraft.component.type.ItemEnchantmentsComponent component =
                EnchantmentHelper.getEnchantments(stack);
        for (net.minecraft.registry.entry.RegistryEntry<Enchantment> entry : component.getEnchantments()) {
            map.put(entry.value(), component.getLevel(entry));
        }
        return map;
        *///?} else {
        return EnchantmentHelper.get(stack);
        //?}
    }

    public static void set(Map<Enchantment, Integer> map, ItemStack stack) {
        //? if >=1.21 {
        /*EnchantmentHelper.apply(stack, builder -> {
            builder.remove(e -> true);
            for (Map.Entry<Enchantment, Integer> e : map.entrySet()) {
                builder.set(entryOf(e.getKey()), e.getValue());
            }
        });
        *///?} else {
        EnchantmentHelper.set(map, stack);
        //?}
    }

    public static ItemStack enchantedBook(Enchantment ench, int level) {
        ItemStack book = new ItemStack(net.minecraft.item.Items.ENCHANTED_BOOK);
        //? if >=1.21 {
        /*EnchantmentHelper.apply(book, builder -> builder.set(entryOf(ench), level));
        *///?} else {
        net.minecraft.item.EnchantedBookItem.addEnchantment(book,
                new net.minecraft.enchantment.EnchantmentLevelEntry(ench, level));
        //?}
        return book;
    }
}
