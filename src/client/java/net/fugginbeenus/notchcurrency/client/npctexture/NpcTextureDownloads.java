package net.fugginbeenus.notchcurrency.client.npctexture;

import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npctexture.NpcTextureStore;
import net.fugginbeenus.notchcurrency.npctexture.NpcTextureStream;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NpcTextureDownloads {

    private NpcTextureDownloads() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcTextures");
    private static final Map<String, byte[]> INCOMING = new LinkedHashMap<>();
    private static final Map<String, Integer> FILLED = new LinkedHashMap<>();
    private static final Map<Integer, String> ON_SERVER = new LinkedHashMap<>();
    private static boolean mayShare;
    private static int wanted;

    public static boolean mayShare() { return mayShare; }

    public static boolean onServer(int slot) { return ON_SERVER.containsKey(slot); }

    public static void onList(Map<Integer, String[]> offered, boolean operator) {
        mayShare = operator;
        ON_SERVER.clear();
        NpcTextureLoader.scan();
        wanted = 0;
        for (Map.Entry<Integer, String[]> e : offered.entrySet()) {
            int slot = e.getKey();
            String name = e.getValue()[0];
            ON_SERVER.put(slot, name);
            NpcTextureLoader.Local have = NpcTextureLoader.slot(slot);
            if (have != null && have.name().equals(name)) continue;
            wanted++;
            NotchPacketsClient.sendNpcTextureWant(slot);
        }
    }

    public static void onPiece(int phase, String key, byte[] part, int announced) {
        switch (phase) {
            case NpcTextureStream.PHASE_BEGIN -> {
                if (announced <= 0 || announced > NpcTextureStore.MAX_BYTES) return;
                INCOMING.put(key, new byte[announced]);
                FILLED.put(key, 0);
            }
            case NpcTextureStream.PHASE_CHUNK -> {
                byte[] buffer = INCOMING.get(key);
                Integer at = FILLED.get(key);
                if (buffer == null || at == null || at + part.length > buffer.length) {
                    INCOMING.remove(key);
                    FILLED.remove(key);
                    return;
                }
                System.arraycopy(part, 0, buffer, at, part.length);
                FILLED.put(key, at + part.length);
            }
            case NpcTextureStream.PHASE_END -> {
                byte[] buffer = INCOMING.remove(key);
                Integer at = FILLED.remove(key);
                if (buffer == null || at == null || at != buffer.length) return;
                int dash = key.indexOf('-');
                int slot;
                String name;
                try {
                    slot = Integer.parseInt(key.substring(0, dash));
                    name = key.substring(dash + 1);
                } catch (Exception bad) {
                    return;
                }
                String problem = NpcTextureLoader.save(slot, name, buffer);
                if (problem != null) {
                    LOGGER.warn("Could not keep particle texture {}: {}", key, problem);
                    return;
                }
                LOGGER.info("Got particle texture {} from the server", key);
                if (--wanted <= 0) {
                    wanted = 0;
                    net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelPacks.reload(Minecraft.getInstance(), false);
                }
            }
            default -> { }
        }
    }

    public static void upload(int slot) {
        NpcTextureLoader.Local l = NpcTextureLoader.slot(slot);
        if (l == null) return;
        String key = slot + "-" + l.name();
        try {
            byte[] blob = java.nio.file.Files.readAllBytes(l.file());
            ON_SERVER.put(slot, l.name());
            NotchPacketsClient.sendNpcTexturePush(NpcTextureStream.PHASE_BEGIN, key, new byte[0], blob.length);
            for (int at = 0; at < blob.length; at += NpcTextureStream.CHUNK_BYTES) {
                int size = Math.min(NpcTextureStream.CHUNK_BYTES, blob.length - at);
                byte[] part = new byte[size];
                System.arraycopy(blob, at, part, 0, size);
                NotchPacketsClient.sendNpcTexturePush(NpcTextureStream.PHASE_CHUNK, key, part, 0);
            }
            NotchPacketsClient.sendNpcTexturePush(NpcTextureStream.PHASE_END, key, new byte[0], 0);
        } catch (Exception e) {
            LOGGER.warn("Could not upload particle texture {}", key, e);
        }
    }
}
