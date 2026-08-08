package net.fugginbeenus.notchcurrency.registry;

//? if <26.2 {
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
//?}
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
                // 26.2 dropped Fabric's entity builder; vanilla's own does the same job by then.
                //? if >=26.2 {
                /*EntityType.Builder.of((EntityType.EntityFactory<BalloonEntity>) (type, world) -> new BalloonEntity(type, world), MobCategory.MISC)
                        .sized(0.9f, 1.4f)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                NotchCurrency.id("balloon")))
                *///?} elif >=1.21.11 {
                /*FabricEntityTypeBuilder.<BalloonEntity>create(MobCategory.MISC, (type, world) -> new BalloonEntity(type, world))
                        .dimensions(EntityDimensions.fixed(0.9f, 1.4f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                NotchCurrency.id("balloon")))
                *///?} else {
                FabricEntityTypeBuilder.<BalloonEntity>create(MobCategory.MISC, (type, world) -> new BalloonEntity(type, world))
                        .dimensions(EntityDimensions.fixed(0.9f, 1.4f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build()
                //?}
        );

        NOTCH_NPC = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                NotchCurrency.id("notch_npc"),
                // 26.2 dropped Fabric's entity builder; vanilla's own does the same job by then.
                //? if >=26.2 {
                /*EntityType.Builder.of((EntityType.EntityFactory<NotchNpcEntity>) NotchNpcEntity::new, MobCategory.MISC)
                        .sized(0.6f, 1.95f)
                        .clientTrackingRange(64)
                        .updateInterval(3)
                        .build(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                NotchCurrency.id("notch_npc")))
                *///?} elif >=1.21.11 {
                /*FabricEntityTypeBuilder.<NotchNpcEntity>create(MobCategory.MISC, NotchNpcEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(3)
                        .build(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                NotchCurrency.id("notch_npc")))
                *///?} else {
                FabricEntityTypeBuilder.<NotchNpcEntity>create(MobCategory.MISC, NotchNpcEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(3)
                        .build()
                //?}
        );
    }

    private ModEntities() {}
}