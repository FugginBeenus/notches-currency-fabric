package net.fugginbeenus.notchcurrency.npctexture;

import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.compat.Perms;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class NpcTextureShare {

    private NpcTextureShare() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcTextures");

    public static void greet(ServerPlayer player) {
        Map<Integer, NpcTextureStore.Held> held = NpcTextureStore.all();
        var buf = Net.buf();
        buf.writeBoolean(Perms.isOperator(player));
        buf.writeVarInt(held.size());
        for (NpcTextureStore.Held h : held.values()) {
            buf.writeVarInt(h.slot());
            buf.writeUtf(h.name(), 64);
            buf.writeUtf(h.hash(), 32);
        }
        Net.sendToClient(player, NotchPackets.NPC_TEXTURE_LIST, buf);
    }

    public static void sendTo(ServerPlayer player, int slot) {
        NpcTextureStore.Held h = NpcTextureStore.get(slot);
        if (h == null) return;
        String key = slot + "-" + h.name();
        byte[] blob = h.bytes();

        var begin = Net.buf();
        begin.writeByte(NpcTextureStream.PHASE_BEGIN);
        begin.writeUtf(key, 64);
        begin.writeVarInt(blob.length);
        Net.sendToClient(player, NotchPackets.NPC_TEXTURE_SEND, begin);

        for (int at = 0; at < blob.length; at += NpcTextureStream.CHUNK_BYTES) {
            int size = Math.min(NpcTextureStream.CHUNK_BYTES, blob.length - at);
            byte[] part = new byte[size];
            System.arraycopy(blob, at, part, 0, size);
            var chunk = Net.buf();
            chunk.writeByte(NpcTextureStream.PHASE_CHUNK);
            chunk.writeUtf(key, 64);
            chunk.writeByteArray(part);
            Net.sendToClient(player, NotchPackets.NPC_TEXTURE_SEND, chunk);
        }

        var end = Net.buf();
        end.writeByte(NpcTextureStream.PHASE_END);
        end.writeUtf(key, 64);
        Net.sendToClient(player, NotchPackets.NPC_TEXTURE_SEND, end);
    }

    public static void receiveUpload(ServerPlayer player, int phase, String key, byte[] part, int announced) {
        if (!Perms.isOperator(player)) return;
        String sender = player.getUUID().toString();
        int dash = key.indexOf('-');
        int slot;
        String name;
        try {
            slot = Integer.parseInt(key.substring(0, dash));
            name = key.substring(dash + 1);
        } catch (Exception bad) {
            say(player, "Could not read that upload.", true);
            return;
        }
        switch (phase) {
            case NpcTextureStream.PHASE_BEGIN -> {
                String problem = NpcTextureStream.begin(sender, name, announced);
                if (problem != null) say(player, "Could not upload " + name + ": " + problem, true);
            }
            case NpcTextureStream.PHASE_CHUNK -> {
                String problem = NpcTextureStream.chunk(sender, name, part);
                if (problem != null) say(player, "Upload of " + name + " stopped: " + problem, true);
            }
            case NpcTextureStream.PHASE_END -> {
                byte[] blob = NpcTextureStream.end(sender, name);
                if (blob == null) {
                    say(player, "Upload of " + name + " did not arrive in full.", true);
                    return;
                }
                String problem = NpcTextureStore.store(player.level().getServer(), slot, name, blob);
                if (problem != null) {
                    say(player, "Could not keep " + name + ": " + problem, true);
                    return;
                }
                LOGGER.info("{} uploaded particle texture {} into slot {}", player.getName().getString(), name, slot);
                say(player, "Shared " + name + " in slot " + slot + ". Players get it when they next join.", false);
            }
            default -> { }
        }
    }

    public static void forget(ServerPlayer player) {
        NpcTextureStream.forget(player.getUUID().toString());
    }

    private static void say(ServerPlayer player, String line, boolean bad) {
        Msg.chat(player, Component.literal(line).withStyle(bad ? ChatFormatting.RED : ChatFormatting.GREEN));
    }
}
