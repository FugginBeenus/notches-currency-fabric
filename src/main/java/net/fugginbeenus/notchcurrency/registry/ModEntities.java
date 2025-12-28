package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static EntityType<BalloonEntity> BALLOON;
    public static EntityType<ShopkeeperEntity> SHOPKEEPER;

    public static void register() {
        BALLOON = Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(NotchCurrency.MOD_ID, "balloon"),
                FabricEntityTypeBuilder.<BalloonEntity>create(SpawnGroup.MISC,
                                (type, world) -> new BalloonEntity(type, world))
                        .dimensions(EntityDimensions.fixed(0.9f, 1.4f)) // solid hitbox
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build()
        );

        SHOPKEEPER = Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(NotchCurrency.MOD_ID, "shopkeeper"),
                FabricEntityTypeBuilder.<ShopkeeperEntity>create(SpawnGroup.MISC, ShopkeeperEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)) // Player-sized
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(3)
                        .build()
        );
    }

    private ModEntities() {}
}