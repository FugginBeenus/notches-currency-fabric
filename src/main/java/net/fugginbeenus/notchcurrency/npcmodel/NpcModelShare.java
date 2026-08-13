package net.fugginbeenus.notchcurrency.npcmodel;

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

/**
 * The server's side of handing custom models out and taking them in.
 *
 * <p>On join a player is told what this server has, as a name and a fingerprint each. They ask for
 * whatever they are missing, and it comes back in chunks. Nothing is pushed at anybody: a client
 * that already has a model says nothing and downloads nothing, which is what keeps joining a server
 * you play on every day free of charge.
 */
public final class NpcModelShare {

    private NpcModelShare() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcModels");

    /**
     * Tells a joining player what is on offer, and whether they may add to it.
     *
     * <p>Sent even when the list is empty, because the permission travels with it and somebody has
     * to be able to share the first model. It is a handful of bytes.
     */
    public static void greet(ServerPlayer player) {
        Map<String, String> hashes = NpcModelServerStore.hashes();

        var buf = Net.buf();
        buf.writeBoolean(Perms.isOperator(player));
        buf.writeVarInt(hashes.size());
        for (Map.Entry<String, String> model : hashes.entrySet()) {
            buf.writeUtf(model.getKey(), 64);
            buf.writeUtf(model.getValue(), 32);
        }
        Net.sendToClient(player, NotchPackets.NPC_MODEL_LIST, buf);
    }

    /** Sends one model to the player who asked for it, in pieces. */
    public static void sendModelTo(ServerPlayer player, String id) {
        byte[] blob = NpcModelServerStore.blob(id);
        if (blob == null) return; // asked for something this server does not have

        var begin = Net.buf();
        begin.writeByte(NpcModelStream.PHASE_BEGIN);
        begin.writeUtf(id, 64);
        begin.writeVarInt(blob.length);
        Net.sendToClient(player, NotchPackets.NPC_MODEL_SEND, begin);

        for (int at = 0; at < blob.length; at += NpcModelStream.CHUNK_BYTES) {
            int size = Math.min(NpcModelStream.CHUNK_BYTES, blob.length - at);
            byte[] part = new byte[size];
            System.arraycopy(blob, at, part, 0, size);

            var chunk = Net.buf();
            chunk.writeByte(NpcModelStream.PHASE_CHUNK);
            chunk.writeUtf(id, 64);
            chunk.writeByteArray(part);
            Net.sendToClient(player, NotchPackets.NPC_MODEL_SEND, chunk);
        }

        var end = Net.buf();
        end.writeByte(NpcModelStream.PHASE_END);
        end.writeUtf(id, 64);
        Net.sendToClient(player, NotchPackets.NPC_MODEL_SEND, end);
    }

    /**
     * Takes a model an operator is uploading.
     *
     * <p>The permission is checked on every packet rather than once at the start, because the start
     * is the easy one to skip.
     */
    public static void receiveUpload(ServerPlayer player, int phase, String id, byte[] part,
                                     int announcedBytes) {
        if (!Perms.isOperator(player)) return;

        String sender = player.getUUID().toString();
        switch (phase) {
            case NpcModelStream.PHASE_BEGIN -> {
                String problem = NpcModelStream.begin(sender, id, announcedBytes);
                if (problem != null) {
                    say(player, "Could not upload " + id + ": " + problem, true);
                }
            }
            case NpcModelStream.PHASE_CHUNK -> {
                String problem = NpcModelStream.chunk(sender, id, part);
                if (problem != null) say(player, "Upload of " + id + " stopped: " + problem, true);
            }
            case NpcModelStream.PHASE_END -> {
                byte[] blob = NpcModelStream.end(sender, id);
                if (blob == null) {
                    say(player, "Upload of " + id + " did not arrive in full.", true);
                    return;
                }
                String problem = NpcModelServerStore.store(player.level().getServer(), id, blob);
                if (problem != null) {
                    say(player, "Could not keep " + id + ": " + problem, true);
                    return;
                }
                LOGGER.info("{} uploaded NPC model {}", player.getName().getString(), id);
                say(player, "Shared " + id + " with this server. Players get it when they join.", false);

                // Everybody already on gets told about it now rather than on their next login.
                for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
                    greet(other);
                }
            }
            default -> { }
        }
    }

    /** A disconnecting player leaves nothing half received behind. */
    public static void forget(ServerPlayer player) {
        NpcModelStream.forget(player.getUUID().toString());
    }

    private static void say(ServerPlayer player, String line, boolean bad) {
        Msg.chat(player, Component.literal(line)
                .withStyle(bad ? ChatFormatting.RED : ChatFormatting.GREEN));
    }
}
