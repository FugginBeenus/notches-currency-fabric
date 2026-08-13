package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTab {

    // Custom Notch Currency creative tab
    public static final ResourceKey<CreativeModeTab> NOTCH_CURRENCY_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            NotchCurrency.id("notch_currency_tab")
    );

    public static final CreativeModeTab NOTCH_CURRENCY_TAB = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.NOTCH_COIN))
            .title(Component.translatable("itemGroup.notchcurrency.notch_currency_tab"))
            .displayItems((context, entries) -> {
                // Currency
                entries.accept(ModItems.NOTCH_COIN);

                // Blocks
                entries.accept(ModBlocks.ATM_ITEM);
                entries.accept(ModBlocks.GOLDEN_CACHE_ITEM);
                entries.accept(ModBlocks.LEDGER_BOARD_ITEM);
                entries.accept(ModBlocks.BOUNTY_BOARD_ITEM);
                entries.accept(ModBlocks.MAILBOX_ITEM);
                entries.accept(ModBlocks.crateItem("common_crate"));
                entries.accept(ModBlocks.crateItem("rare_crate"));
                entries.accept(ModBlocks.crateItem("epic_crate"));
                entries.accept(ModItems.CRATE_KEY);
                entries.accept(ModBlocks.SLOT_MACHINE_ITEM);
                entries.accept(ModBlocks.COIN_FLIP_ITEM);

                // Shop items
                entries.accept(ModItems.NOTCH_NPC_ITEM);

                // Misc
                entries.accept(ModItems.BALLOON);
            })
            .build();

    public static void register() {
        // Register our custom creative tab
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, NOTCH_CURRENCY_TAB_KEY, NOTCH_CURRENCY_TAB);

        // Also add items to relevant vanilla tabs for discoverability

        // Add ATM and Golden Cache to Functional Blocks tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(ModBlocks.ATM_ITEM);
            entries.accept(ModBlocks.GOLDEN_CACHE_ITEM);
        });

        // Add Notch Coin to Ingredients tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(ModItems.NOTCH_COIN);
        });

        // Add spawn egg to Spawn Eggs tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
        });

        // Add Merchant License and Balloon to Tools tab
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(ModItems.BALLOON);
        });
    }
}