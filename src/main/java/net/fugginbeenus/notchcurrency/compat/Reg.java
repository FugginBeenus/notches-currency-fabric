package net.fugginbeenus.notchcurrency.compat;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.resources.ResourceLocation;

public final class Reg {

    private Reg() {}

    public static ResourceLocation id(String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(NotchCurrency.MOD_ID, path);
        *///?} else {
        return new ResourceLocation(NotchCurrency.MOD_ID, path);
        //?}
    }

    public static ResourceLocation id(String namespace, String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }

    public static ResourceLocation parse(String full) {
        //? if >=1.21 {
        /*return ResourceLocation.parse(full);
        *///?} else {
        return new ResourceLocation(full);
        //?}
    }

    public static net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProps(String path) {
        net.minecraft.world.level.block.state.BlockBehaviour.Properties props =
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
        //? if >=1.21.11 {
        /*props = props.setId(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.BLOCK, id(path)));
        *///?}
        return props;
    }

    public static net.minecraft.world.item.Item.Properties blockItemProps(String path) {
        net.minecraft.world.item.Item.Properties props = itemProps(path);
        //? if >=1.21.11 {
        /*props = props.useBlockDescriptionPrefix();
        *///?}
        return props;
    }

    public static net.minecraft.world.item.Item.Properties itemProps(String path) {
        net.minecraft.world.item.Item.Properties props = new net.minecraft.world.item.Item.Properties();
        //? if >=1.21.11 {
        /*props = props.setId(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ITEM, id(path)));
        *///?}
        return props;
    }
}
