package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.atm.ATMBlock;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {

    public static final Block ATM = Registry.register(
            Registries.BLOCK,
            NotchCurrency.id("atm"),
            new ATMBlock(AbstractBlock.Settings.create().strength(2.0f).nonOpaque())
    );

    public static final Item ATM_ITEM = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("atm"),
            new BlockItem(ATM, new Item.Settings())
    );

    private ModBlocks() {}

    public static void register() {
        // nothing else needed; registration already happened in the static fields
    }
}
