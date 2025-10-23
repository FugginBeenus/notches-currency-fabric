package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item NOTCH_COIN = new Item(new Item.Settings());

    private ModItems() {}

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(NotchCurrency.MOD_ID, "notch_coin"), NOTCH_COIN);
    }
}
