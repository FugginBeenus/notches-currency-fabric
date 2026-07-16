package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.registry.DynamicRegistryManager;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the active registry manager so {@link StackData} and {@link Ench} can resolve registry
 * entries when (de)serializing.
 *
 * <p>Why this exists: on 1.20.1 an ItemStack round-trips through NBT with no context; from 1.20.5
 * the codecs need a registry lookup, and the call sites have none. The entry points that know the
 * registries hand them here once.
 *
 * <p>Server and client are stored separately, and the server wins. In singleplayer both sides live
 * in one JVM with two distinct registry-manager instances — with a single field, the client join
 * overwrote the server's, server-side code then built stacks out of client registry entries, and
 * the first inventory sync crashed encoding them. Server-side (de)serialization must always use the
 * server's registries; the client value only serves a client on a dedicated server.
 */
public final class RegistryAccess {

    private static volatile DynamicRegistryManager serverRegistries;
    private static volatile DynamicRegistryManager clientRegistries;

    private RegistryAccess() {}

    /** Record the server's registry manager (server started / stopped). */
    public static void setServer(@Nullable DynamicRegistryManager manager) {
        serverRegistries = manager;
    }

    /** Record the client world's registry manager (world joined). */
    public static void setClient(@Nullable DynamicRegistryManager manager) {
        clientRegistries = manager;
    }

    /**
     * The registry manager to resolve against: the server's when one is running (always, on a
     * dedicated server or singleplayer's integrated server), otherwise the client world's.
     */
    public static DynamicRegistryManager get() {
        DynamicRegistryManager manager = serverRegistries;
        if (manager == null) manager = clientRegistries;
        if (manager == null) {
            throw new IllegalStateException(
                    "Notch Currency: registry manager requested before a server started or a world was joined");
        }
        return manager;
    }
}
