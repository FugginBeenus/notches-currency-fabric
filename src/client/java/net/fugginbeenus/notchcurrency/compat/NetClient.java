package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class NetClient {

    private NetClient() {}

    @FunctionalInterface
    public interface ClientReceiver {
        void receive(MinecraftClient client, PacketByteBuf buf);
    }

    public static void registerClientReceiver(Identifier id, ClientReceiver receiver) {
        //? if >=1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(Net.channel(id), (payload, context) ->
                receiver.receive(context.client(), Net.toBuf(payload)));
        *///?} else {
        ClientPlayNetworking.registerGlobalReceiver(id,
                (client, handler, buf, responseSender) -> receiver.receive(client, buf));
        //?}
    }

    public static void sendToServer(Identifier id, PacketByteBuf buf) {
        //? if >=1.21 {
        /*ClientPlayNetworking.send(new Net.RawPayload(Net.channel(id), Net.toBytes(buf)));
        *///?} else {
        ClientPlayNetworking.send(id, buf);
        //?}
    }
}
