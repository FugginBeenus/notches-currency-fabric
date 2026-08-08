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

    /**
     * Block settings that already know what the block will be registered as.
     *
     * <p>From 1.21.11 a block reads its own registry key out of its settings while it is being
     * constructed, and throws if it is not there. That happens before the register call that used to
     * supply the name, so the name has to be handed over up front.
     */
    public static net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProps(String path) {
        net.minecraft.world.level.block.state.BlockBehaviour.Properties props =
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
        //? if >=1.21.11 {
        /*props = props.setId(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.BLOCK, id(path)));
        *///?}
        return props;
    }

    /**
     * Settings for the item form of a block.
     *
     * <p>A BlockItem used to borrow its translation key from the block it places. From 1.21.11 the key
     * follows the id set here, which is an item id, so without this every block item would look for
     * an {@code item.} entry and show its raw id instead of the {@code block.} name in the lang file.
     */
    public static net.minecraft.world.item.Item.Properties blockItemProps(String path) {
        net.minecraft.world.item.Item.Properties props = itemProps(path);
        //? if >=1.21.11 {
        /*props = props.useBlockDescriptionPrefix();
        *///?}
        return props;
    }

    /** Item settings, for the same reason. */
    public static net.minecraft.world.item.Item.Properties itemProps(String path) {
        net.minecraft.world.item.Item.Properties props = new net.minecraft.world.item.Item.Properties();
        //? if >=1.21.11 {
        /*props = props.setId(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ITEM, id(path)));
        *///?}
        return props;
    }
}
