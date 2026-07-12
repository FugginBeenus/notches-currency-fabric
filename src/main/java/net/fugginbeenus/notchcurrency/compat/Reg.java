package net.fugginbeenus.notchcurrency.compat;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.util.Identifier;

/**
 * Version-compat facade for constructing {@link Identifier}s (and, later, other registry glue).
 *
 * <p>One of the mod's four compat facades for the Stonecutter multi-version port. Minecraft 1.21
 * makes the {@code new Identifier(...)} constructors non-public in favour of the {@code Identifier.of(...)}
 * factories, so every id in the codebase is funnelled through here — on 1.21 only these three method
 * bodies change. On 1.20.1 (this build) they are the plain constructors, so there is no behavior
 * change.
 *
 * <p>Mod-namespaced ids should prefer {@link NotchCurrency#id(String)} (which delegates here); the
 * two-arg and parse forms are for vanilla/other namespaces and for parsing user- or data-supplied
 * strings.
 */
public final class Reg {

    private Reg() {}

    /** A {@code notchcurrency:}-namespaced id. The single construction point for the mod's own ids. */
    public static Identifier id(String path) {
        return new Identifier(NotchCurrency.MOD_ID, path);
    }

    /** An id in an explicit namespace (e.g. {@code minecraft:entities/wither}). */
    public static Identifier id(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    /** Parse a full {@code "namespace:path"} string (bare paths default to the {@code minecraft} namespace). */
    public static Identifier parse(String full) {
        return new Identifier(full);
    }
}
