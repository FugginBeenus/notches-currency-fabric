package net.fugginbeenus.notchcurrency.auction;

import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;

/**
 * Helper for classifying auction listings into simple categories for the
 * star-button filters. Inspired by vanilla creative tabs.
 *
 * Categories:
 *  - "blocks"
 *  - "furniture"
 *  - "mobs"
 *  - "gear"
 *  - "seasonal"
 *  - "valuables"
 *  - "books"
 *  - "other"
 */
public final class AuctionCategories {

    private AuctionCategories() {}

    public static String classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "other";
        }

        Item item = stack.getItem();
        Identifier id = Registries.ITEM.getId(item);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);

        // ===== BOOKS =====
        if (item instanceof EnchantedBookItem ||
                item instanceof WritableBookItem ||
                item instanceof WrittenBookItem ||
                path.contains("book")) {
            return "books";
        }

        // ===== MOBS =====
        if (item instanceof SpawnEggItem || path.contains("spawn_egg")) {
            return "mobs";
        }

        // ===== GEAR =====
        if (item instanceof ArmorItem ||
                item instanceof ToolItem ||
                item instanceof SwordItem ||
                item instanceof BowItem ||
                item instanceof CrossbowItem ||
                item instanceof ShieldItem ||
                item instanceof HorseArmorItem) {
            return "gear";
        }

        // ===== VALUABLES (ore/gem/ingot style stuff) =====
        if (path.contains("diamond") || path.contains("emerald") ||
                path.contains("netherite") || path.contains("ancient_debris") ||
                path.contains("ore") || path.contains("raw_") ||
                path.contains("ingot") || path.contains("nugget") ||
                path.contains("gem") || path.contains("crystal") ||
                path.contains("lapis") || path.contains("quartz") ||
                path.contains("totem")) {
            return "valuables";
        }

        // ===== SEASONAL =====
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

        // ===== BASE CATEGORY FOR BUILDING STUFF: BLOCKS =====
        // Anything that's a BlockItem or looks very block-like in name.
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
            // You might later special-case some of these into other cats,
            // but for now treating them all as "blocks" is safe.
            return "blocks";
        }

        // ===== FURNITURE (non-block items that sound like furniture) =====
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

        // ===== EXTRA MOBS: villager-related =====
        if (path.contains("villager")) {
            return "mobs";
        }

        // ===== FALLBACK =====
        return "other";
    }
}
