package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.shop.MerchantLicenseItem;
import net.fugginbeenus.notchcurrency.shop.ShopkeeperSpawnItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {

    /** Notch Coin - normal item */
    public static final Item NOTCH_COIN = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("notch_coin"),
            new Item(new Item.Settings())
    );

    /** Balloon - normal item (if you want it obtainable) */
    public static final Item BALLOON = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("balloon"),
            new Item(new Item.Settings())
    );

    /** Merchant License - use on Shopkeeper NPC to create a shop */
    public static final Item MERCHANT_LICENSE = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("merchant_license"),
            new MerchantLicenseItem(new Item.Settings())
    );

    /** Shopkeeper Spawn Egg - spawns a humanoid Shopkeeper NPC for players */
    public static final Item SHOPKEEPER_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("shopkeeper_spawn_egg"),
            new ShopkeeperSpawnItem(new Item.Settings())
    );

    /**
     * Golden Cache item reference.
     * NOTE: This does NOT register anything - it just points to the BlockItem
     * that is registered in ModBlocks. This avoids duplicate registration.
     */
    public static final Item GOLDEN_CACHE = ModBlocks.GOLDEN_CACHE_ITEM;

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