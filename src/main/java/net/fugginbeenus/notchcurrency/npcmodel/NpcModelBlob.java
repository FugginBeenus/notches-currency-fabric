package net.fugginbeenus.notchcurrency.npcmodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NpcModelBlob {

    private NpcModelBlob() {}

    public static final List<String> ALLOWED = List.of(
            "npc.json", "model.geo.json", "animation.json", "texture.png");

    public static final int MAX_FILE_BYTES = 4 * 1024 * 1024;
    public static final int MAX_TOTAL_BYTES = 8 * 1024 * 1024;
    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9_]{1,32}");
    }

    public static byte[] pack(Path folder) throws Exception {
        Map<String, byte[]> files = new LinkedHashMap<>();
        long total = 0;
        for (String name : ALLOWED) {
            Path file = folder.resolve(name);
            if (!Files.isRegularFile(file)) continue;
            long size = Files.size(file);
            if (size > MAX_FILE_BYTES) throw new IllegalStateException(name + " is too large to send");
            total += size;
            if (total > MAX_TOTAL_BYTES) throw new IllegalStateException("that model is too large to send");
            files.put(name, Files.readAllBytes(file));
        }
        if (!files.containsKey("model.geo.json") || !files.containsKey("texture.png")) {
            throw new IllegalStateException("that model is missing its geometry or its texture");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(out)) {
            data.writeInt(files.size());
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                data.writeUTF(file.getKey());
                data.writeInt(file.getValue().length);
                data.write(file.getValue());
            }
        }
        return out.toByteArray();
    }

    public static String unpack(byte[] blob, Path parent, String id) {
        if (!validId(id)) return "that is not a model id";
        if (blob.length > MAX_TOTAL_BYTES) return "that model is too large";

        Map<String, byte[]> files = new LinkedHashMap<>();
        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(blob))) {
            int count = data.readInt();
            if (count < 0 || count > ALLOWED.size()) return "that model has a strange number of files";
            long total = 0;
            for (int i = 0; i < count; i++) {
                String name = data.readUTF();
                int length = data.readInt();
                if (length < 0 || length > MAX_FILE_BYTES) return "a file in that model is too large";
                total += length;
                if (total > MAX_TOTAL_BYTES) return "that model is too large";
                byte[] bytes = new byte[length];
                data.readFully(bytes);
                if (ALLOWED.contains(name)) files.put(name, bytes);
            }
        } catch (Exception malformed) {
            return "that model could not be read";
        }

        if (!files.containsKey("model.geo.json") || !files.containsKey("texture.png")) {
            return "that model is missing its geometry or its texture";
        }

        try {
            Path folder = parent.resolve(id);
            Files.createDirectories(folder);
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                Files.write(folder.resolve(file.getKey()), file.getValue());
            }
            for (String name : ALLOWED) {
                if (!files.containsKey(name)) Files.deleteIfExists(folder.resolve(name));
            }
            return null;
        } catch (Exception e) {
            return "could not be written: " + e.getMessage();
        }
    }

    public static String hash(byte[] blob) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-1").digest(blob);
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8 && i < digest.length; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (Exception noSuchThing) {
            return Integer.toHexString(java.util.Arrays.hashCode(blob));
        }
    }
}
