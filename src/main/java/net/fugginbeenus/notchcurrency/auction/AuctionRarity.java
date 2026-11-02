package net.fugginbeenus.notchcurrency.auction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import java.util.Map;

public final class AuctionRarity {

    private static final Map<String, Rarity> VANILLA = Map.ofEntries(
            Map.entry("elytra", Rarity.EPIC),
            Map.entry("dragon_egg", Rarity.EPIC),
            Map.entry("nether_star", Rarity.EPIC),
            Map.entry("beacon", Rarity.EPIC),
            Map.entry("enchanted_golden_apple", Rarity.EPIC),
            Map.entry("trident", Rarity.EPIC),
            Map.entry("mace", Rarity.EPIC),
            Map.entry("heavy_core", Rarity.EPIC),
            Map.entry("ancient_debris", Rarity.RARE),
            Map.entry("netherite_scrap", Rarity.RARE),
            Map.entry("netherite_upgrade_smithing_template", Rarity.RARE),
            Map.entry("totem_of_undying", Rarity.RARE),
            Map.entry("heart_of_the_sea", Rarity.RARE),
            Map.entry("conduit", Rarity.RARE),
            Map.entry("shulker_shell", Rarity.RARE),
            Map.entry("echo_shard", Rarity.RARE),
            Map.entry("dragon_head", Rarity.RARE),
            Map.entry("end_crystal", Rarity.RARE),
            Map.entry("wither_skeleton_skull", Rarity.RARE),
            Map.entry("golden_apple", Rarity.RARE),
            Map.entry("emerald", Rarity.UNCOMMON),
            Map.entry("emerald_block", Rarity.UNCOMMON),
            Map.entry("name_tag", Rarity.UNCOMMON),
            Map.entry("saddle", Rarity.UNCOMMON));

    private AuctionRarity() {}

    public static Rarity of(ItemStack stack) {
        Rarity base = baseOf(stack);
        return stack.isEnchanted() ? oneUp(base) : base;
    }

    private static Rarity baseOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getNamespace().equals("minecraft")) {
            String path = id.getPath();
            Rarity listed = VANILLA.get(path);
            if (listed != null) return listed;
            if (path.startsWith("netherite_")) return Rarity.EPIC;
            if (path.startsWith("diamond") || path.endsWith("shulker_box")) return Rarity.RARE;
        }
        return new ItemStack(stack.getItem()).getRarity();
    }

    private static Rarity oneUp(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Rarity.UNCOMMON;
            case UNCOMMON -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }
}
