package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.atm.ATMBlock;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.AbstractBlock;

public final class ModBlocks {
    public static final ATMBlock ATM = new ATMBlock(AbstractBlock.Settings.create()); // ✅ uses Settings.create()

    public static void register() {
        Registry.register(Registries.BLOCK,
                new Identifier(NotchCurrency.MOD_ID, "atm"), ATM);
        Registry.register(Registries.ITEM,
                new Identifier(NotchCurrency.MOD_ID, "atm"),
                new BlockItem(ATM, new Item.Settings()));
    }
}
