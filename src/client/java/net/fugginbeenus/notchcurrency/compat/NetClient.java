package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Client-side counterpart to {@link Net}: registers server→client receivers and sends client→server
 * packets. Lives in the client source set because it touches {@code ClientPlayNetworking}.
 *
 * <p>Same bridging story as {@link Net} — {@code (Identifier, PacketByteBuf)} on 1.20.1, a generic
 * {@code CustomPayload} on 1.21, contained to this file. Receivers never used the Fabric
 * {@code responseSender}/{@code networkHandler} params, so {@link ClientReceiver} is just
 * {@code (client, buf)}. Buffers keep using {@code PacketByteBufs.create()} at the call sites.
 */
public final class NetClient {

    private NetClient() {}

    /** A server→client packet handler. Runs on the network thread — hop to the main thread as needed. */
    @FunctionalInterface
    public interface ClientReceiver {
        void receive(MinecraftClient client, PacketByteBuf buf);
    }

    /** Register a receiver for a server→client channel. */
    public static void registerClientReceiver(Identifier id, ClientReceiver receiver) {
        ClientPlayNetworking.registerGlobalReceiver(id,
                (client, handler, buf, responseSender) -> receiver.receive(client, buf));
    }

    /** Send a client→server packet. */
    public static void sendToServer(Identifier id, PacketByteBuf buf) {
        ClientPlayNetworking.send(id, buf);
    }
}
