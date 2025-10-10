package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.atm.ATMBlock;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlocks {
    public static Block ATM;

    public static void register() {
        ATM = Registry.register(
                Registries.BLOCK,
                NotchCurrency.id("atm"),
                new ATMBlock(AbstractBlock.Settings.create().strength(3.0f))
        );

        Registry.register(
                Registries.ITEM,
                NotchCurrency.id("atm"),
                new BlockItem(ATM, new Item.Settings())
        );
    }

    private ModBlocks() {}
}
