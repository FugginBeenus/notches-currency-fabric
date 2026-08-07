package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public final class Net {

    private Net() {}

    @FunctionalInterface
    public interface ServerReceiver {
        void receive(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf);
    }

    //? if >=1.21 {
    /*public record RawPayload(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> channel, byte[] data)
            implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        @Override
        public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
            return channel;
        }
    }

    private static final Map<ResourceLocation, net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload>> CHANNELS =
            new HashMap<>();

    static net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> channel(ResourceLocation id) {
        net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> found = CHANNELS.get(id);
        if (found == null) {
            throw new IllegalStateException("Notch Currency: packet channel used before it was declared: " + id);
        }
        return found;
    }

    static FriendlyByteBuf toBuf(RawPayload payload) {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.data()));
    }

    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, FriendlyByteBuf> RAW_BUF_CODEC =
            net.minecraft.network.codec.StreamCodec.ofMember(
                    (value, buf) -> buf.writeBytes(value.slice()),
                    buf -> new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(toBytes(buf))));

    static byte[] toBytes(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    private static net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, RawPayload> codecFor(
            net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> id) {
        return net.minecraft.network.codec.StreamCodec.ofMember(
                (payload, buf) -> buf.writeBytes(payload.data()),
                buf -> new RawPayload(id, toBytes(buf)));
    }
    *///?}

    public static void declareChannels() {
        //? if >=1.21 {
        /*if (!CHANNELS.isEmpty()) return;
        for (Field field : NotchPackets.class.getFields()) {
            if (field.getType() != ResourceLocation.class || !Modifier.isStatic(field.getModifiers())) continue;
            try {
                ResourceLocation id = (ResourceLocation) field.get(null);
                net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RawPayload> payloadId =
                        new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(id);
                net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, RawPayload> codec = codecFor(payloadId);
                net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(payloadId, codec);
                net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(payloadId, codec);
                CHANNELS.put(id, payloadId);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Notch Currency: could not read packet channel " + field.getName(), e);
            }
        }
        *///?}
    }

    public static void registerServerReceiver(ResourceLocation id, ServerReceiver receiver) {
        //? if >=1.21 {
        /*ServerPlayNetworking.registerGlobalReceiver(channel(id), (payload, context) ->
                receiver.receive(context.server(), context.player(), toBuf(payload)));
        *///?} else {
        ServerPlayNetworking.registerGlobalReceiver(id,
                (server, player, handler, buf, responseSender) -> receiver.receive(server, player, buf));
        //?}
    }

    public static void sendToClient(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        //? if >=1.21 {
        /*ServerPlayNetworking.send(player, new RawPayload(channel(id), toBytes(buf)));
        *///?} else {
        ServerPlayNetworking.send(player, id, buf);
        //?}
    }
}
