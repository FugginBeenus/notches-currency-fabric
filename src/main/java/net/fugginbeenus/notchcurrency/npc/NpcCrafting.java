package net.fugginbeenus.notchcurrency.npc;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.resources.ResourceLocation;

public final class NpcCrafting {

    public static final ResourceLocation ID = NotchCurrency.id("npc_crafting");

    private NpcCrafting() {}

    public static boolean allowed() {
        String mode = NotchConfigIO.get().npcs.crafting;
        if ("EVERYWHERE".equals(mode)) return true;
        return "SINGLE_PLAYER".equals(mode)
                && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public static void register() {
        //? if >=1.21 {
        /*ResourceConditions.register(TYPE);
        *///?} else {
        ResourceConditions.register(ID, json -> allowed());
        //?}
    }

    //? if >=1.21.11 {
    /*private static final net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<Condition> TYPE =
            net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType.create(ID,
                    com.mojang.serialization.MapCodec.unit(new Condition()));

    private record Condition() implements net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition {
        @Override
        public net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<?> getType() {
            return TYPE;
        }

        @Override
        public boolean test(@org.jetbrains.annotations.Nullable net.minecraft.resources.RegistryOps.RegistryInfoLookup lookup) {
            return allowed();
        }
    }
    *///?} elif >=1.21 {
    /*private static final net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<Condition> TYPE =
            net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType.create(ID,
                    com.mojang.serialization.MapCodec.unit(new Condition()));

    private record Condition() implements net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition {
        @Override
        public net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<?> getType() {
            return TYPE;
        }

        @Override
        public boolean test(@org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider lookup) {
            return allowed();
        }
    }
    *///?}
}
