package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Version-compat facade for server-side custom networking (registering client→server receivers and
 * sending server→client packets).
 *
 * <p>One of the mod's four compat facades for the Stonecutter port. The mod speaks in {@code (Identifier,
 * PacketByteBuf)} pairs. That's the native shape on 1.20.1; on 1.21 raw channels become typed
 * {@code CustomPayload} records, so this facade is where every id+buffer packet gets bridged onto a
 * single generic payload — the ~40 receiver bodies and their buffer read/writes stay untouched. On
 * 1.20.1 (this build) the methods are thin passthroughs, so there is no behavior change.
 *
 * <p>The receiver never needed the Fabric {@code responseSender}/{@code networkHandler} params (nothing
 * replies from inside a receiver), so {@link ServerReceiver} exposes just {@code (server, player, buf)}.
 *
 * <p>Buffers are still created with {@code PacketByteBufs.create()} at the call sites — that helper is
 * stable across versions, so it isn't wrapped here. The client-side counterpart lives in
 * {@code NetClient} (client source set).
 */
public final class Net {

    private Net() {}

    /** A client→server packet handler. Runs on the network thread — hop to the main thread as needed. */
    @FunctionalInterface
    public interface ServerReceiver {
        void receive(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf);
    }

    /** Register a receiver for a client→server channel. */
    public static void registerServerReceiver(Identifier id, ServerReceiver receiver) {
        ServerPlayNetworking.registerGlobalReceiver(id,
                (server, player, handler, buf, responseSender) -> receiver.receive(server, player, buf));
    }

    /** Send a server→client packet to one player. */
    public static void sendToClient(ServerPlayerEntity player, Identifier id, PacketByteBuf buf) {
        ServerPlayNetworking.send(player, id, buf);
    }
}
