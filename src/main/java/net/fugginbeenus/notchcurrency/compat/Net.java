package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public final class Net {

    private Net() {}

    @FunctionalInterface
    public interface ServerReceiver {
        void receive(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf);
    }

    //? if >=1.21 {
    /*public record RawPayload(net.minecraft.network.packet.CustomPayload.Id<RawPayload> channel, byte[] data)
            implements net.minecraft.network.packet.CustomPayload {
        @Override
        public net.minecraft.network.packet.CustomPayload.Id<? extends net.minecraft.network.packet.CustomPayload> getId() {
            return channel;
        }
    }

    private static final Map<Identifier, net.minecraft.network.packet.CustomPayload.Id<RawPayload>> CHANNELS =
            new HashMap<>();

    static net.minecraft.network.packet.CustomPayload.Id<RawPayload> channel(Identifier id) {
        net.minecraft.network.packet.CustomPayload.Id<RawPayload> found = CHANNELS.get(id);
        if (found == null) {
            throw new IllegalStateException("Notch Currency: packet channel used before it was declared: " + id);
        }
        return found;
    }

    static PacketByteBuf toBuf(RawPayload payload) {
        return new PacketByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.data()));
    }

    public static final net.minecraft.network.codec.PacketCodec<PacketByteBuf, PacketByteBuf> RAW_BUF_CODEC =
            net.minecraft.network.codec.PacketCodec.of(
                    (value, buf) -> buf.writeBytes(value.slice()),
                    buf -> new PacketByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(toBytes(buf))));

    static byte[] toBytes(PacketByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    private static net.minecraft.network.codec.PacketCodec<PacketByteBuf, RawPayload> codecFor(
            net.minecraft.network.packet.CustomPayload.Id<RawPayload> id) {
        return net.minecraft.network.codec.PacketCodec.of(
                (payload, buf) -> buf.writeBytes(payload.data()),
                buf -> new RawPayload(id, toBytes(buf)));
    }
    *///?}

    public static void declareChannels() {
        //? if >=1.21 {
        /*if (!CHANNELS.isEmpty()) return;
        for (Field field : NotchPackets.class.getFields()) {
            if (field.getType() != Identifier.class || !Modifier.isStatic(field.getModifiers())) continue;
            try {
                Identifier id = (Identifier) field.get(null);
                net.minecraft.network.packet.CustomPayload.Id<RawPayload> payloadId =
                        new net.minecraft.network.packet.CustomPayload.Id<>(id);
                net.minecraft.network.codec.PacketCodec<PacketByteBuf, RawPayload> codec = codecFor(payloadId);
                net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(payloadId, codec);
                net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(payloadId, codec);
                CHANNELS.put(id, payloadId);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Notch Currency: could not read packet channel " + field.getName(), e);
            }
        }
        *///?}
    }

    public static void registerServerReceiver(Identifier id, ServerReceiver receiver) {
        //? if >=1.21 {
        /*ServerPlayNetworking.registerGlobalReceiver(channel(id), (payload, context) ->
                receiver.receive(context.server(), context.player(), toBuf(payload)));
        *///?} else {
        ServerPlayNetworking.registerGlobalReceiver(id,
                (server, player, handler, buf, responseSender) -> receiver.receive(server, player, buf));
        //?}
    }

    public static void sendToClient(ServerPlayerEntity player, Identifier id, PacketByteBuf buf) {
        //? if >=1.21 {
        /*ServerPlayNetworking.send(player, new RawPayload(channel(id), toBytes(buf)));
        *///?} else {
        ServerPlayNetworking.send(player, id, buf);
        //?}
    }
}
