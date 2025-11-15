package net.fugginbeenus.notchcurrency.loot;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.Set;

public final class BossCurrencyInject {
    private static final Set<Identifier> BOSS_TABLES = Set.of(
            new Identifier("minecraft", "entities/wither"),
            new Identifier("minecraft", "entities/warden"),
            new Identifier("minecraft", "entities/elder_guardian")
    );
    private static final Set<Identifier> MINI_TABLES = Set.of(
            new Identifier("minecraft", "entities/evoker"),
            new Identifier("minecraft", "entities/piglin_brute")
    );

    private BossCurrencyInject() {}

    public static void init() {
        // Fabric Loot v2 signature: (resourceManager, lootManager, id, tableBuilder, source)
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, table, source) -> {
            if (BOSS_TABLES.contains(id)) {
                LootPool.Builder pool = LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.25f)) // 25%
                        .with(ItemEntry.builder(ModItems.NOTCH_COIN))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(3.0f, 7.0f)));
                table.pool(pool);
            } else if (MINI_TABLES.contains(id)) {
                LootPool.Builder pool = LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.12f)) // 12%
                        .with(ItemEntry.builder(ModItems.NOTCH_COIN))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 3.0f)));
                table.pool(pool);
            }
        });
    }
}
