package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static EntityType<BalloonEntity> BALLOON;
    public static EntityType<NotchNpcEntity> NOTCH_NPC;

    public static void register() {
        BALLOON = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                NotchCurrency.id("balloon"),
                FabricEntityTypeBuilder.<BalloonEntity>create(MobCategory.MISC,
                                (type, world) -> new BalloonEntity(type, world))
                        .dimensions(EntityDimensions.fixed(0.9f, 1.4f)) // solid hitbox
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build()
        );

        NOTCH_NPC = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                NotchCurrency.id("notch_npc"),
                FabricEntityTypeBuilder.<NotchNpcEntity>create(MobCategory.MISC, NotchNpcEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.95f)) // player-ish, humanoid geo
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(3)
                        .build()
        );
    }

    private ModEntities() {}
}