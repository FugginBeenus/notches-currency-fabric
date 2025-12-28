package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.atm.ATMBlock;
import net.fugginbeenus.notchcurrency.block.GoldenCacheBlock;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlocks {

    // ATM
    public static final Block ATM = Registry.register(
            Registries.BLOCK,
            NotchCurrency.id("atm"),
            new ATMBlock(AbstractBlock.Settings.create()
                    .strength(2.0f)
                    .nonOpaque())
    );

    public static final Item ATM_ITEM = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("atm"),
            new BlockItem(ATM, new Item.Settings())
    );

    // Golden Cache (crate)
    public static final Block GOLDEN_CACHE = Registry.register(
            Registries.BLOCK,
            NotchCurrency.id("golden_cache"),
            new GoldenCacheBlock(
                    AbstractBlock.Settings.create()
                            .strength(2.0f)
                            .nonOpaque()
                            .luminance(state -> 12)   // <--- glowy boi
            )
    );

    public static final Item GOLDEN_CACHE_ITEM = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("golden_cache"),
            new BlockItem(GOLDEN_CACHE, new Item.Settings())
    );

    private ModBlocks() {}

    public static void register() {
        // NO-OP; static initializers above already did all the registering.
    }
}
