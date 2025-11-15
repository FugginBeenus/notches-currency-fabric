package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {

    /** Register the Notch Coin once via static initialization */
    public static final Item NOTCH_COIN = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("notch_coin"),
            new Item(new Item.Settings())
    );

    private ModItems() {}

    /**
     * This method intentionally does nothing.
     * Calling ModItems.register() simply forces the class to load,
     * which triggers the static initializers above.
     */
    public static void register() {
        // NO-OP. Do not register anything here.
    }
}
