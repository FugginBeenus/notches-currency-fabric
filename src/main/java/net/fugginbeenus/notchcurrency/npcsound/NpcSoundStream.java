package net.fugginbeenus.notchcurrency.npcsound;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NpcSoundStream {

    public static final int PHASE_BEGIN = 0, PHASE_CHUNK = 1, PHASE_END = 2;
    public static final int CHUNK_BYTES = 24 * 1024;
    private static final int MAX_OPEN_PER_SENDER = 2;
    private static final Map<String, Incoming> OPEN = new LinkedHashMap<>();

    private static final class Incoming {
        final byte[] bytes;
        int filled;

        Incoming(int size) {
            this.bytes = new byte[size];
        }
    }

    private NpcSoundStream() {}

    public static String begin(String sender, String id, int totalBytes) {
        if (!NpcSoundStore.validId(id)) return "that is not a sound name";
        if (totalBytes <= 0 || totalBytes > NpcSoundStore.MAX_BYTES) {
            return "that sound is larger than 1 MB";
        }
        int open = 0;
        for (String key : OPEN.keySet()) {
            if (key.startsWith(sender + " ")) open++;
        }
        if (open >= MAX_OPEN_PER_SENDER) {
            OPEN.keySet().removeIf(key -> key.startsWith(sender + " "));
        }
        OPEN.put(key(sender, id), new Incoming(totalBytes));
        return null;
    }

    public static String chunk(String sender, String id, byte[] part) {
        Incoming open = OPEN.get(key(sender, id));
        if (open == null) return "that sound was not announced first";
        if (open.filled + part.length > open.bytes.length) {
            OPEN.remove(key(sender, id));
            return "that sound sent more than it said it would";
        }
        System.arraycopy(part, 0, open.bytes, open.filled, part.length);
        open.filled += part.length;
        return null;
    }

    public static byte[] end(String sender, String id) {
        Incoming open = OPEN.remove(key(sender, id));
        if (open == null || open.filled != open.bytes.length) return null;
        return open.bytes;
    }

    public static void forget(String sender) {
        OPEN.keySet().removeIf(key -> key.startsWith(sender + " "));
    }

    private static String key(String sender, String id) {
        return sender + " " + id;
    }
}
