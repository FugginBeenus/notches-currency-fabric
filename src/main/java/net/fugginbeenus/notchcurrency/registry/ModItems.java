package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.item.CrateKeyItem;
import net.fugginbeenus.notchcurrency.item.NotchNpcItem;
import net.fugginbeenus.notchcurrency.item.RaffleTicketItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {

    public static final Item NOTCH_COIN = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("notch_coin"),
            new Item(new Item.Settings())
    );

    public static final Item BALLOON = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("balloon"),
            new Item(new Item.Settings())
    );

    public static final Item RAFFLE_TICKET = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("raffle_ticket"),
            new RaffleTicketItem(new Item.Settings())
    );

    public static final Item CRATE_KEY = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("crate_key"),
            new CrateKeyItem(new Item.Settings())
    );

    public static final Item COIN_TAILS = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("coin_tails"),
            new Item(new Item.Settings())
    );

    public static final Item NOTCH_NPC_ITEM = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("notch_npc"),
            new NotchNpcItem(new Item.Settings())
    );

    public static final Item ROUTE_PLANNER = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("route_planner"),
            new net.fugginbeenus.notchcurrency.item.RoutePlannerItem(new Item.Settings())
    );

    public static final Item GOLDEN_CACHE = ModBlocks.GOLDEN_CACHE_ITEM;

    private ModItems() {}

    public static void register() {
        // NO-OP. Do not register anything here.
    }
}