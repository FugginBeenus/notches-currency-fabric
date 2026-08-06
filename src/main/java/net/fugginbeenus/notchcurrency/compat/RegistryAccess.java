package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.registry.DynamicRegistryManager;
import org.jetbrains.annotations.Nullable;

public final class RegistryAccess {

    private static volatile DynamicRegistryManager serverRegistries;
    private static volatile DynamicRegistryManager clientRegistries;
    private static volatile java.util.function.BooleanSupplier clientThreadCheck;

    private RegistryAccess() {}

    public static void setClientThreadCheck(java.util.function.BooleanSupplier check) {
        clientThreadCheck = check;
    }

    public static void setServer(@Nullable DynamicRegistryManager manager) {
        serverRegistries = manager;
    }

    public static void setClient(@Nullable DynamicRegistryManager manager) {
        clientRegistries = manager;
    }

    public static DynamicRegistryManager get() {
        java.util.function.BooleanSupplier check = clientThreadCheck;
        if (check != null && check.getAsBoolean() && clientRegistries != null) {
            return clientRegistries;
        }
        DynamicRegistryManager manager = serverRegistries;
        if (manager == null) manager = clientRegistries;
        if (manager == null) {
            throw new IllegalStateException(
                    "Notch Currency: registry manager requested before a server started or a world was joined");
        }
        return manager;
    }
}
