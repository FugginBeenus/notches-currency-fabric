package net.fugginbeenus.notchcurrency.client.npcmodel;

import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelBlob;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collecting the models a server has that this game does not.
 *
 * <p>The server sends a list of names and fingerprints. Anything already on disk with a matching
 * fingerprint is skipped, so joining a server you play on daily downloads nothing at all. The rest
 * are asked for, and when the last one lands the resources are reloaded once, not once each.
 */
public final class NpcModelDownloads {

    private NpcModelDownloads() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcModels");
    private static final String SERVER = "server";

    /** What is still on its way, so the reload waits for the last one. */
    private static final Set<String> waiting = new HashSet<>();
    private static int arrived;

    /**
     * Whether this server said we may share models with it.
     *
     * <p>Told to us rather than worked out here. A client cannot know its own permission level
     * reliably, and even if it could, the answer would be a hint rather than an authority: the
     * server checks again on every packet it receives.
     */
    private static boolean mayShare;

    public static boolean mayShare() {
        return mayShare;
    }

    /** The server's list. Works out what is missing and asks for exactly that. */
    public static void onList(Map<String, String> offered, boolean allowedToShare) {
        mayShare = allowedToShare;
        waiting.clear();
        arrived = 0;

        for (Map.Entry<String, String> model : offered.entrySet()) {
            String id = model.getKey();
            if (!NpcModelBlob.validId(id)) continue;
            if (haveMatching(id, model.getValue())) continue;
            waiting.add(id);
        }

        if (waiting.isEmpty()) return;
        LOGGER.info("Fetching {} NPC model(s) from the server", waiting.size());
        for (String id : Set.copyOf(waiting)) {
            NotchPacketsClient.sendNpcModelWant(id);
        }
    }

    /**
     * Whether this game already has that exact model.
     *
     * <p>Packed and fingerprinted the same way the server does it, so the two are comparing the same
     * thing rather than trusting a stamp somebody could have edited.
     */
    private static boolean haveMatching(String id, String hash) {
        try {
            var folder = NpcModelLoader.modelsDir().resolve(id);
            if (!Files.isDirectory(folder)) return false;
            return NpcModelBlob.hash(NpcModelBlob.pack(folder)).equals(hash);
        } catch (Exception notReadable) {
            return false;
        }
    }

    /** One packet of an incoming model. */
    public static void onPiece(int phase, String id, byte[] part, int announcedBytes) {
        switch (phase) {
            case NpcModelStream.PHASE_BEGIN -> NpcModelStream.begin(SERVER, id, announcedBytes);
            case NpcModelStream.PHASE_CHUNK -> NpcModelStream.chunk(SERVER, id, part);
            case NpcModelStream.PHASE_END -> finish(id);
            default -> { }
        }
    }

    private static void finish(String id) {
        byte[] blob = NpcModelStream.end(SERVER, id);
        waiting.remove(id);

        if (blob == null) {
            LOGGER.warn("Model {} did not arrive in full", id);
        } else {
            String problem = NpcModelBlob.unpack(blob, NpcModelLoader.modelsDir(), id);
            if (problem != null) LOGGER.warn("Could not keep model {}: {}", id, problem);
            else arrived++;
        }

        // Wait for the last one. Reloading resources per model would be a hitch each time.
        if (!waiting.isEmpty()) return;
        if (arrived == 0) return;

        Minecraft client = Minecraft.getInstance();
        int count = arrived;
        arrived = 0;
        NpcModelPacks.reload(client, false);
        if (client.player != null) {
            Msg.chat(client.player, Component.literal(
                            "Got " + count + (count == 1 ? " NPC model" : " NPC models") + " from this server.")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    /** Leaving a server abandons anything half received rather than carrying it to the next one. */
    public static void reset() {
        NpcModelStream.forget(SERVER);
        waiting.clear();
        arrived = 0;
        mayShare = false;
    }
}
