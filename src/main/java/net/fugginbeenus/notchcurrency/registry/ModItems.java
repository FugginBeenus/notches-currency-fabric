package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.item.CrateKeyItem;
import net.fugginbeenus.notchcurrency.item.NotchNpcItem;
import net.fugginbeenus.notchcurrency.item.RaffleTicketItem;
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

    /** Raffle Ticket - a physical receipt handed out when a player buys raffle entries. */
    public static final Item RAFFLE_TICKET = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("raffle_ticket"),
            new RaffleTicketItem(new Item.Settings())
    );

    /** Crate Key - opens crates (coin-bought sink). */
    public static final Item CRATE_KEY = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("crate_key"),
            new CrateKeyItem(new Item.Settings())
    );

    /**
     * The "tails" face of the coin, used only to draw the flip-side in the Coin Flip GUI. Not in any
     * creative tab (purely a GUI sprite). Placeholder texture reuses the coin art until real art.
     */
    public static final Item COIN_TAILS = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("coin_tails"),
            new Item(new Item.Settings())
    );

    /** The single Notch NPC item: places a blank NPC (or a packed one from the editor's Pick up). */
    public static final Item NOTCH_NPC_ITEM = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("notch_npc"),
            new NotchNpcItem(new Item.Settings())
    );

    /** Patrol route tool: bound to one NPC by the editor; right-click ground to drop waypoints. */
    public static final Item ROUTE_PLANNER = Registry.register(
            Registries.ITEM,
            NotchCurrency.id("route_planner"),
            new net.fugginbeenus.notchcurrency.item.RoutePlannerItem(new Item.Settings())
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