package net.fugginbeenus.notchcurrency.client.npcsound;

import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npcsound.NpcSoundStore;
import net.fugginbeenus.notchcurrency.npcsound.NpcSoundStream;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NpcSoundDownloads {

    private NpcSoundDownloads() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcSounds");
    private static final Map<String, byte[]> INCOMING = new LinkedHashMap<>();
    private static final Map<String, Integer> FILLED = new LinkedHashMap<>();
    private static boolean mayShare = false;
    private static int wanted = 0;
    private static final java.util.Set<String> ON_SERVER = new java.util.LinkedHashSet<>();

    public static boolean onServer(String id) {
        return ON_SERVER.contains(id);
    }

    public static boolean mayShare() {
        return mayShare;
    }

    public static void onList(Map<String, String> offered, boolean operator) {
        mayShare = operator;
        ON_SERVER.clear();
        ON_SERVER.addAll(offered.keySet());
        NpcSoundLoader.scan();
        wanted = 0;
        for (Map.Entry<String, String> sound : offered.entrySet()) {
            if (NpcSoundLoader.has(sound.getKey())) continue;
            wanted++;
            NotchPacketsClient.sendNpcSoundWant(sound.getKey());
        }
        if (wanted == 0) NpcSoundLoader.scan();
    }

    public static void onPiece(int phase, String id, byte[] part, int announcedBytes) {
        switch (phase) {
            case NpcSoundStream.PHASE_BEGIN -> {
                if (announcedBytes <= 0 || announcedBytes > NpcSoundStore.MAX_BYTES) return;
                INCOMING.put(id, new byte[announcedBytes]);
                FILLED.put(id, 0);
            }
            case NpcSoundStream.PHASE_CHUNK -> {
                byte[] buffer = INCOMING.get(id);
                Integer at = FILLED.get(id);
                if (buffer == null || at == null || at + part.length > buffer.length) {
                    INCOMING.remove(id);
                    FILLED.remove(id);
                    return;
                }
                System.arraycopy(part, 0, buffer, at, part.length);
                FILLED.put(id, at + part.length);
            }
            case NpcSoundStream.PHASE_END -> {
                byte[] buffer = INCOMING.remove(id);
                Integer at = FILLED.remove(id);
                if (buffer == null || at == null || at != buffer.length) return;
                String problem = NpcSoundLoader.save(id, buffer);
                if (problem != null) {
                    LOGGER.warn("Could not keep sound {}: {}", id, problem);
                    return;
                }
                LOGGER.info("Got sound {} from the server", id);
                ON_SERVER.add(id);
                if (--wanted <= 0) {
                    wanted = 0;
                    Minecraft client = Minecraft.getInstance();
                    net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelPacks.reload(client, false);
                }
            }
            default -> { }
        }
    }

    public static void upload(String id) {
        java.nio.file.Path file = NpcSoundLoader.fileOf(id);
        if (file == null) return;
        ON_SERVER.add(id);
        try {
            byte[] blob = java.nio.file.Files.readAllBytes(file);
            NotchPacketsClient.sendNpcSoundPush(NpcSoundStream.PHASE_BEGIN, id, new byte[0], blob.length);
            for (int at = 0; at < blob.length; at += NpcSoundStream.CHUNK_BYTES) {
                int size = Math.min(NpcSoundStream.CHUNK_BYTES, blob.length - at);
                byte[] part = new byte[size];
                System.arraycopy(blob, at, part, 0, size);
                NotchPacketsClient.sendNpcSoundPush(NpcSoundStream.PHASE_CHUNK, id, part, 0);
            }
            NotchPacketsClient.sendNpcSoundPush(NpcSoundStream.PHASE_END, id, new byte[0], 0);
        } catch (Exception e) {
            LOGGER.warn("Could not upload sound {}", id, e);
        }
    }
}
