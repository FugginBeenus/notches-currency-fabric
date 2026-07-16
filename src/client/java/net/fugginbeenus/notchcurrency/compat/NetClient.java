package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Client-side counterpart to {@link Net}: registers server→client receivers and sends client→server
 * packets. Lives in the client source set because it touches {@code ClientPlayNetworking}.
 *
 * <p>Same bridging story as {@link Net} — {@code (Identifier, PacketByteBuf)} on 1.20.1, one generic
 * {@code CustomPayload} on 1.21, contained to these two files. The channel table and payload type
 * live in {@link Net}; {@link Net#declareChannels()} must have run first.
 */
public final class NetClient {

    private NetClient() {}

    /** A server→client packet handler. */
    @FunctionalInterface
    public interface ClientReceiver {
        void receive(MinecraftClient client, PacketByteBuf buf);
    }

    /** Register a receiver for a server→client channel. */
    public static void registerClientReceiver(Identifier id, ClientReceiver receiver) {
        //? if >=1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(Net.channel(id), (payload, context) ->
                receiver.receive(context.client(), Net.toBuf(payload)));
        *///?} else {
        ClientPlayNetworking.registerGlobalReceiver(id,
                (client, handler, buf, responseSender) -> receiver.receive(client, buf));
        //?}
    }

    /** Send a client→server packet. */
    public static void sendToServer(Identifier id, PacketByteBuf buf) {
        //? if >=1.21 {
        /*ClientPlayNetworking.send(new Net.RawPayload(Net.channel(id), Net.toBytes(buf)));
        *///?} else {
        ClientPlayNetworking.send(id, buf);
        //?}
    }
}
