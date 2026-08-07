package net.fugginbeenus.notchcurrency.compat;

import org.jetbrains.annotations.Nullable;

public final class RegistryAccess {

    private static volatile net.minecraft.core.RegistryAccess serverRegistries;
    private static volatile net.minecraft.core.RegistryAccess clientRegistries;
    private static volatile java.util.function.BooleanSupplier clientThreadCheck;

    private RegistryAccess() {}

    public static void setClientThreadCheck(java.util.function.BooleanSupplier check) {
        clientThreadCheck = check;
    }

    public static void setServer(@Nullable net.minecraft.core.RegistryAccess manager) {
        serverRegistries = manager;
    }

    public static void setClient(@Nullable net.minecraft.core.RegistryAccess manager) {
        clientRegistries = manager;
    }

    public static net.minecraft.core.RegistryAccess get() {
        java.util.function.BooleanSupplier check = clientThreadCheck;
        if (check != null && check.getAsBoolean() && clientRegistries != null) {
            return clientRegistries;
        }
        net.minecraft.core.RegistryAccess manager = serverRegistries;
        if (manager == null) manager = clientRegistries;
        if (manager == null) {
            throw new IllegalStateException(
                    "Notch Currency: registry manager requested before a server started or a world was joined");
        }
        return manager;
    }
}
