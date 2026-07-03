package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

/**
 * Registers Notch Currency items to creative tabs.
 * Creates a custom "Notch Currency" tab and adds items to relevant vanilla tabs.
 */
public class ModCreativeTab {

    // Custom Notch Currency creative tab
    public static final RegistryKey<ItemGroup> NOTCH_CURRENCY_TAB_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            NotchCurrency.id("notch_currency_tab")
    );

    public static final ItemGroup NOTCH_CURRENCY_TAB = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.NOTCH_COIN))
            .displayName(Text.translatable("itemGroup.notchcurrency.notch_currency_tab"))
            .entries((context, entries) -> {
                // Currency
                entries.add(ModItems.NOTCH_COIN);

                // Blocks
                entries.add(ModBlocks.ATM_ITEM);
                entries.add(ModBlocks.GOLDEN_CACHE_ITEM);
                entries.add(ModBlocks.LEDGER_BOARD_ITEM);
                entries.add(ModBlocks.BOUNTY_BOARD_ITEM);
                entries.add(ModBlocks.crateItem("common_crate"));
                entries.add(ModBlocks.crateItem("rare_crate"));
                entries.add(ModBlocks.crateItem("epic_crate"));
                entries.add(ModItems.CRATE_KEY);
                entries.add(ModBlocks.SLOT_MACHINE_ITEM);
                entries.add(ModBlocks.COIN_FLIP_ITEM);

                // Shop items
                entries.add(ModItems.NOTCH_NPC_ITEM);

                // Misc
                entries.add(ModItems.BALLOON);
            })
            .build();

    public static void register() {
        // Register our custom creative tab
        Registry.register(Registries.ITEM_GROUP, NOTCH_CURRENCY_TAB_KEY, NOTCH_CURRENCY_TAB);

        // Also add items to relevant vanilla tabs for discoverability

        // Add ATM and Golden Cache to Functional Blocks tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(ModBlocks.ATM_ITEM);
            entries.add(ModBlocks.GOLDEN_CACHE_ITEM);
        });

        // Add Notch Coin to Ingredients tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(ModItems.NOTCH_COIN);
        });

        // Add spawn egg to Spawn Eggs tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
        });

        // Add Merchant License and Balloon to Tools tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(ModItems.BALLOON);
        });
    }
}