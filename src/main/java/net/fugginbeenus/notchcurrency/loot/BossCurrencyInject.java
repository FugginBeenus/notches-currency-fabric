package net.fugginbeenus.notchcurrency.loot;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fugginbeenus.notchcurrency.compat.Reg;
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
            Reg.id("minecraft","entities/wither"),
            Reg.id("minecraft","entities/warden"),
            Reg.id("minecraft","entities/elder_guardian")
    );
    private static final Set<Identifier> MINI_TABLES = Set.of(
            Reg.id("minecraft","entities/evoker"),
            Reg.id("minecraft","entities/piglin_brute")
    );

    private BossCurrencyInject() {}

    public static void init() {
        // Fabric Loot v2 signature: (resourceManager, lootManager, id, tableBuilder, source)
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, table, source) -> {
            if (BOSS_TABLES.contains(id)) {
                LootPool.Builder pool = LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.05f)) // 5%
                        .with(ItemEntry.builder(ModItems.NOTCH_COIN))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 3.0f)));
                table.pool(pool);
            } else if (MINI_TABLES.contains(id)) {
                LootPool.Builder pool = LootPool.builder()
                        .conditionally(RandomChanceLootCondition.builder(0.03f)) // 3%
                        .with(ItemEntry.builder(ModItems.NOTCH_COIN))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f)));
                table.pool(pool);
            }
        });
    }
}
