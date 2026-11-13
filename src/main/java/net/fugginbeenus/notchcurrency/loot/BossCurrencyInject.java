package net.fugginbeenus.notchcurrency.loot;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
//? if >=26.3 {
/*import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
*///?} else {
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
//?}
import java.util.Set;

public final class BossCurrencyInject {
    private static final Set<ResourceLocation> BOSS_TABLES = Set.of(
            Reg.id("minecraft","entities/wither"),
            Reg.id("minecraft","entities/warden"),
            Reg.id("minecraft","entities/elder_guardian")
    );
    private static final Set<ResourceLocation> MINI_TABLES = Set.of(
            Reg.id("minecraft","entities/evoker"),
            Reg.id("minecraft","entities/piglin_brute")
    );

    private BossCurrencyInject() {}

    public static void init() {
        //? if >=26.1 {
        /*LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            ResourceLocation id = key.location();
        *///?} elif >=1.21 {
        /*LootTableEvents.MODIFY.register((key, table, source) -> {
            ResourceLocation id = key.location();
        *///?} else {
        // Fabric Loot v2 signature: (resourceManager, lootManager, id, tableBuilder, source)
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, table, source) -> {
        //?}
            if (BOSS_TABLES.contains(id)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.05f)) // 5%
                        .add(LootItem.lootTableItem(ModItems.NOTCH_COIN))
                        //? if >=26.3 {
                        /*.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3)));
                        *///?} else {
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f)));
                        //?}
                table.withPool(pool);
            } else if (MINI_TABLES.contains(id)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.03f)) // 3%
                        .add(LootItem.lootTableItem(ModItems.NOTCH_COIN))
                        //? if >=26.3 {
                        /*.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2)));
                        *///?} else {
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)));
                        //?}
                table.withPool(pool);
            }
        });
    }
}
