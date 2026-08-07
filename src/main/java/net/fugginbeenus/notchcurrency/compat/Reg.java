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
        /*return ResourceLocation.fromNamespaceAndPath(full);
        *///?} else {
        return new ResourceLocation(full);
        //?}
    }
}
