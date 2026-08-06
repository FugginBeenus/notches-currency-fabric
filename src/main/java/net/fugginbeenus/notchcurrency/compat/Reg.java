package net.fugginbeenus.notchcurrency.compat;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.util.Identifier;

public final class Reg {

    private Reg() {}

    public static Identifier id(String path) {
        //? if >=1.21 {
        /*return Identifier.of(NotchCurrency.MOD_ID, path);
        *///?} else {
        return new Identifier(NotchCurrency.MOD_ID, path);
        //?}
    }

    public static Identifier id(String namespace, String path) {
        //? if >=1.21 {
        /*return Identifier.of(namespace, path);
        *///?} else {
        return new Identifier(namespace, path);
        //?}
    }

    public static Identifier parse(String full) {
        //? if >=1.21 {
        /*return Identifier.of(full);
        *///?} else {
        return new Identifier(full);
        //?}
    }
}
