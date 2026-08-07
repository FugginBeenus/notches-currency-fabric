package net.fugginbeenus.notchcurrency.compat;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class Ench {

    private Ench() {}

    public static final int COMMON = 0, UNCOMMON = 1, RARE = 2, VERY_RARE = 3;

    //? if >=1.21 {
    /*private static net.minecraft.core.Registry<Enchantment> registry() {
        return RegistryAccess.get().get(net.minecraft.core.registries.Registries.ENCHANTMENT);
    }

    private static net.minecraft.core.Holder<Enchantment> entryOf(Enchantment ench) {
        return registry().wrapAsHolder(ench);
    }
    *///?}

    public static Iterable<Enchantment> all() {
        //? if >=1.21 {
        /*return registry();
        *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT;
        //?}
    }

    public static ResourceLocation idOf(Enchantment ench) {
        //? if >=1.21 {
        /*return registry().getId(ench);
        *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(ench);
        //?}
    }

    @Nullable
    public static Enchantment byId(ResourceLocation id) {
        //? if >=1.21 {
        /*return registry().get(id);
        *///?} else {
        return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(id);
        //?}
    }

    public static boolean isCursed(Enchantment ench) {
        //? if >=1.21 {
        /*return entryOf(ench).is(net.minecraft.tags.EnchantmentTags.CURSE);
        *///?} else {
        return ench.isCurse();
        //?}
    }

    public static boolean isTreasure(Enchantment ench) {
        //? if >=1.21 {
        /*return entryOf(ench).is(net.minecraft.tags.EnchantmentTags.TREASURE);
        *///?} else {
        return ench.isTreasureOnly();
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
        return ench.canEnchant(stack);
    }

    public static boolean canCombine(Enchantment a, Enchantment b) {
        //? if >=1.21 {
        /*return Enchantment.canStoreEnchantment(entryOf(a), entryOf(b));
        *///?} else {
        return a.isCompatibleWith(b);
        //?}
    }

    public static Component name(Enchantment ench, int level) {
        //? if >=1.21 {
        /*return Enchantment.getName(entryOf(ench), level);
        *///?} else {
        return ench.getFullname(level);
        //?}
    }

    public static Map<Enchantment, Integer> get(ItemStack stack) {
        //? if >=1.21 {
        /*Map<Enchantment, Integer> map = new LinkedHashMap<>();
        net.minecraft.world.item.enchantment.ItemEnchantments component =
                EnchantmentHelper.getEnchantments(stack);
        for (net.minecraft.core.Holder<Enchantment> entry : component.keySet()) {
            map.put(entry.value(), component.getLevel(entry));
        }
        return map;
        *///?} else {
        return EnchantmentHelper.getEnchantments(stack);
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
        EnchantmentHelper.setEnchantments(map, stack);
        //?}
    }

    public static ItemStack enchantedBook(Enchantment ench, int level) {
        ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        //? if >=1.21 {
        /*EnchantmentHelper.apply(book, builder -> builder.set(entryOf(ench), level));
        *///?} else {
        net.minecraft.world.item.EnchantedBookItem.addEnchantment(book,
                new net.minecraft.world.item.enchantment.EnchantmentInstance(ench, level));
        //?}
        return book;
    }
}
