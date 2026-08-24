package net.fugginbeenus.notchcurrency.client;

import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PackReloads {

    private PackReloads() {}

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    public static CompletableFuture<Void> request(Minecraft client) {
        if (!RUNNING.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> done;
        try {
            done = client.reloadResourcePacks();
        } catch (RuntimeException failed) {
            RUNNING.set(false);
            throw failed;
        }
        return done.whenComplete((ok, failed) -> RUNNING.set(false));
    }
}
