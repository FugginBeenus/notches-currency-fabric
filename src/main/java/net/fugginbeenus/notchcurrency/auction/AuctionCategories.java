package net.fugginbeenus.notchcurrency.auction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 && <1.21.11 {
/*import net.minecraft.world.item.AnimalArmorItem;
*///?}
//? if <1.21.11 {
import net.minecraft.world.item.ArmorItem;
//?}
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
//? if <1.21.11 {
import net.minecraft.world.item.EnchantedBookItem;
//?}
//? if <1.21 {
import net.minecraft.world.item.HorseArmorItem;
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SpawnEggItem;
//? if <1.21.11 {
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
//?}
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.WrittenBookItem;
import java.util.Locale;

public final class AuctionCategories {

    private AuctionCategories() {}

    public static String classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "other";
        }

        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);

        if (
                //? if <1.21.11 {
                item instanceof EnchantedBookItem ||
                //?}
                item instanceof WritableBookItem ||
                item instanceof WrittenBookItem ||
                path.contains("book")) {
            return "books";
        }

        if (item instanceof SpawnEggItem || path.contains("spawn_egg")) {
            return "mobs";
        }

        //? if >=1.21.11 {
        /*net.minecraft.world.item.ItemStack probe = item.getDefaultInstance();
        if (probe.has(net.minecraft.core.component.DataComponents.TOOL)
                || probe.has(net.minecraft.core.component.DataComponents.WEAPON)
                || probe.has(net.minecraft.core.component.DataComponents.EQUIPPABLE)) {
            return "gear";
        }
        *///?} else {
        if (item instanceof ArmorItem ||
                item instanceof TieredItem ||
                item instanceof SwordItem ||
                item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof ShieldItem ||
                //? if >=1.21 {
                /*item instanceof AnimalArmorItem) {
                *///?} else {
                item instanceof HorseArmorItem) {
                //?}
            return "gear";
        }
        //?}

        if (path.contains("diamond") || path.contains("emerald") ||
                path.contains("netherite") || path.contains("ancient_debris") ||
                path.contains("ore") || path.contains("raw_") ||
                path.contains("ingot") || path.contains("nugget") ||
                path.contains("gem") || path.contains("crystal") ||
                path.contains("lapis") || path.contains("quartz") ||
                path.contains("totem")) {
            return "valuables";
        }

        if (
                namespace.contains("holiday") ||
                        (namespace.contains("mcw") && (path.contains("halloween") || path.contains("xmas") || path.contains("christmas"))) ||
                        path.contains("halloween") || path.contains("xmas") ||
                        path.contains("christmas") || path.contains("easter") ||
                        path.contains("valentine") || path.contains("pumpkin") ||
                        path.contains("candy_cane") || path.contains("present") ||
                        path.contains("gift") || path.contains("wreath") ||
                        path.contains("stocking") || path.contains("ornament")
        ) {
            return "seasonal";
        }

        if (item instanceof BlockItem ||
                path.contains("block") || path.contains("log") ||
                path.contains("planks") || path.contains("wood") ||
                path.contains("stone") || path.contains("bricks") ||
                path.contains("slab") || path.contains("stairs") ||
                path.contains("door") || path.contains("trapdoor") ||
                path.contains("fence") || path.contains("wall") ||
                path.contains("glass") || path.contains("pane") ||
                path.contains("lamp") || path.contains("lantern") ||
                path.contains("torch")) {
            return "blocks";
        }

        if (
                path.contains("chair") || path.contains("table") ||
                        path.contains("sofa") || path.contains("couch") ||
                        path.contains("bench") || path.contains("stool") ||
                        path.contains("desk") || path.contains("wardrobe") ||
                        path.contains("drawer") || path.contains("cabinet") ||
                        path.contains("counter") || path.contains("shelf") ||
                        path.contains("nightstand") || path.contains("dresser") ||
                        path.contains("coffee_table") ||
                        namespace.contains("furniture") ||
                        namespace.contains("cfm") ||
                        namespace.contains("mcwfurn")
        ) {
            return "furniture";
        }
        if (path.contains("villager")) {
            return "mobs";
        }
        return "other";
    }
}
