package net.fugginbeenus.notchcurrency.npcsound;

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

public final class NpcSoundShare {

    private NpcSoundShare() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcSounds");

    public static void greet(ServerPlayer player) {
        Map<String, String> hashes = NpcSoundStore.hashes();

        var buf = Net.buf();
        buf.writeBoolean(Perms.isOperator(player));
        buf.writeVarInt(hashes.size());
        for (Map.Entry<String, String> sound : hashes.entrySet()) {
            buf.writeUtf(sound.getKey(), 64);
            buf.writeUtf(sound.getValue(), 32);
        }
        Net.sendToClient(player, NotchPackets.NPC_SOUND_LIST, buf);
    }

    public static void sendSoundTo(ServerPlayer player, String id) {
        byte[] blob = NpcSoundStore.blob(id);
        if (blob == null) return;

        var begin = Net.buf();
        begin.writeByte(NpcSoundStream.PHASE_BEGIN);
        begin.writeUtf(id, 64);
        begin.writeVarInt(blob.length);
        Net.sendToClient(player, NotchPackets.NPC_SOUND_SEND, begin);

        for (int at = 0; at < blob.length; at += NpcSoundStream.CHUNK_BYTES) {
            int size = Math.min(NpcSoundStream.CHUNK_BYTES, blob.length - at);
            byte[] part = new byte[size];
            System.arraycopy(blob, at, part, 0, size);

            var chunk = Net.buf();
            chunk.writeByte(NpcSoundStream.PHASE_CHUNK);
            chunk.writeUtf(id, 64);
            chunk.writeByteArray(part);
            Net.sendToClient(player, NotchPackets.NPC_SOUND_SEND, chunk);
        }

        var end = Net.buf();
        end.writeByte(NpcSoundStream.PHASE_END);
        end.writeUtf(id, 64);
        Net.sendToClient(player, NotchPackets.NPC_SOUND_SEND, end);
    }

    public static void receiveUpload(ServerPlayer player, int phase, String id, byte[] part,
                                     int announcedBytes) {
        if (!Perms.isOperator(player)) return;

        String sender = player.getUUID().toString();
        switch (phase) {
            case NpcSoundStream.PHASE_BEGIN -> {
                String problem = NpcSoundStream.begin(sender, id, announcedBytes);
                if (problem != null) say(player, "Could not upload " + id + ": " + problem, true);
            }
            case NpcSoundStream.PHASE_CHUNK -> {
                String problem = NpcSoundStream.chunk(sender, id, part);
                if (problem != null) say(player, "Upload of " + id + " stopped: " + problem, true);
            }
            case NpcSoundStream.PHASE_END -> {
                byte[] blob = NpcSoundStream.end(sender, id);
                if (blob == null) {
                    say(player, "Upload of " + id + " did not arrive in full.", true);
                    return;
                }
                String problem = NpcSoundStore.store(player.level().getServer(), id, blob);
                if (problem != null) {
                    say(player, "Could not keep " + id + ": " + problem, true);
                    return;
                }
                LOGGER.info("{} uploaded NPC sound {}", player.getName().getString(), id);
                say(player, "Shared " + id + ". Players already online get it when they next join.",
                        false);
            }
            default -> { }
        }
    }

    public static void forget(ServerPlayer player) {
        NpcSoundStream.forget(player.getUUID().toString());
    }

    private static void say(ServerPlayer player, String line, boolean bad) {
        Msg.chat(player, Component.literal(line)
                .withStyle(bad ? ChatFormatting.RED : ChatFormatting.GREEN));
    }
}
