package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.registry.DynamicRegistryManager;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the active registry manager so {@link StackData} can serialize whole ItemStacks.
 *
 * <p>Why this exists: on 1.20.1 an ItemStack round-trips through {@code ItemStack.fromNbt}/
 * {@code writeNbt} with no context. From 1.20.5 the same job goes through {@code ItemStack.CODEC},
 * which needs a registry lookup to resolve item ids and component types — and the call sites
 * (shop/auction/trade save data, client screens decoding a carried stack) have no registry handy.
 * Rather than thread a registry parameter through the whole codebase, the two entry points that
 * legitimately know it hand it here once.
 *
 * <p>Populated from {@code NotchCurrency} when the server starts and from {@code ClientInit} when the
 * client joins a world. Unused on 1.20.1 — kept version-agnostic so callers don't need a gate.
 */
public final class RegistryAccess {

    private static volatile DynamicRegistryManager registries;

    private RegistryAccess() {}

    /** Record the registry manager for the world/server we're attached to. */
    public static void set(@Nullable DynamicRegistryManager manager) {
        registries = manager;
    }

    /**
     * The active registry manager. Only ever called from stack (de)serialization, which cannot
     * happen before a server has started or a client has joined a world.
     */
    public static DynamicRegistryManager get() {
        DynamicRegistryManager manager = registries;
        if (manager == null) {
            throw new IllegalStateException(
                    "Notch Currency: registry manager requested before a server started or a world was joined");
        }
        return manager;
    }
}
