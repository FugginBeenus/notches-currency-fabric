package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.item.CrateKeyItem;
import net.fugginbeenus.notchcurrency.item.NotchNpcItem;
import net.fugginbeenus.notchcurrency.item.RaffleTicketItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class ModItems {

    public static final Item NOTCH_COIN = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("notch_coin"),
            new Item(net.fugginbeenus.notchcurrency.compat.Reg.itemProps("notch_coin"))
    );

    public static final Item BALLOON = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("balloon"),
            new Item(net.fugginbeenus.notchcurrency.compat.Reg.itemProps("balloon"))
    );

    public static final Item RAFFLE_TICKET = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("raffle_ticket"),
            new RaffleTicketItem(net.fugginbeenus.notchcurrency.compat.Reg.itemProps("raffle_ticket"))
    );

    public static final Item PARCEL = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("parcel"),
            new net.fugginbeenus.notchcurrency.item.ParcelItem(
                    net.fugginbeenus.notchcurrency.compat.Reg.itemProps("parcel"))
    );

    public static final Item CRATE_KEY = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("crate_key"),
            new CrateKeyItem(net.fugginbeenus.notchcurrency.compat.Reg.itemProps("crate_key"))
    );

    public static final Item COIN_TAILS = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("coin_tails"),
            new Item(net.fugginbeenus.notchcurrency.compat.Reg.itemProps("coin_tails"))
    );

    public static final Item NOTCH_NPC_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("notch_npc"),
            new NotchNpcItem(net.fugginbeenus.notchcurrency.compat.Reg.itemProps("notch_npc"))
    );

    public static final Item ROUTE_PLANNER = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("route_planner"),
            new net.fugginbeenus.notchcurrency.item.RoutePlannerItem(net.fugginbeenus.notchcurrency.compat.Reg.itemProps("route_planner"))
    );

    public static final Item HEART_CRYSTAL = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("heart_crystal"),
            new net.fugginbeenus.notchcurrency.item.HeartCrystalItem(
                    net.fugginbeenus.notchcurrency.compat.Reg.itemProps("heart_crystal"))
    );

    public static final Item GOLDEN_CACHE = ModBlocks.GOLDEN_CACHE_ITEM;

    private ModItems() {}

    public static void register() {
    }
}