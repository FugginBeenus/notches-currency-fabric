package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class NetClient {

    private NetClient() {}

    @FunctionalInterface
    public interface ClientReceiver {
        void receive(Minecraft client, FriendlyByteBuf buf);
    }

    public static void registerClientReceiver(ResourceLocation id, ClientReceiver receiver) {
        //? if >=1.21 {
        /*ClientPlayNetworking.registerGlobalReceiver(Net.channel(id), (payload, context) ->
                receiver.receive(context.client(), Net.toBuf(payload)));
        *///?} else {
        ClientPlayNetworking.registerGlobalReceiver(id,
                (client, handler, buf, responseSender) -> receiver.receive(client, buf));
        //?}
    }

    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buf) {
        //? if >=1.21 {
        /*ClientPlayNetworking.send(new Net.RawPayload(Net.channel(id), Net.toBytes(buf)));
        *///?} else {
        ClientPlayNetworking.send(id, buf);
        //?}
    }
}
