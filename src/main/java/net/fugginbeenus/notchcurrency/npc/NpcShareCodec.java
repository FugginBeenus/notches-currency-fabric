package net.fugginbeenus.notchcurrency.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public final class NpcShareCodec {

    public static final String PREFIX = "NOTCHNPC1:";
    public static final int MAX_WIRE_CHARS = 30_000;
    public static final int MAX_CODE_CHARS = 256_000;
    private static final int MAX_DECOMPRESSED_BYTES = 4 * 1024 * 1024;

    private NpcShareCodec() {}

    public static class BadCode extends Exception {
        public BadCode(String message) {
            super(message);
        }
    }

    public static String encode(CompoundTag tag) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, out);
        return PREFIX + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public static boolean looksLikeCode(@Nullable String text) {
        return text != null && text.strip().startsWith(PREFIX);
    }

    public static CompoundTag decode(@Nullable String rawText) throws BadCode {
        if (rawText == null || rawText.isBlank()) {
            throw new BadCode("There's no share code on your clipboard.");
        }
        // Codes get pasted out of chat and Discord, so whitespace and line breaks are expected.
        String text = rawText.strip().replaceAll("\\s", "");
        if (!text.startsWith(PREFIX)) {
            throw new BadCode("That doesn't look like an NPC share code.");
        }
        if (text.length() > MAX_CODE_CHARS) {
            throw new BadCode("That share code is too big to be an NPC.");
        }

        byte[] compressed;
        try {
            compressed = Base64.getDecoder().decode(text.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new BadCode("That share code is damaged. Copy it again, all of it.");
        }
        if (compressed.length == 0) {
            throw new BadCode("That share code is empty.");
        }

        // Expand once through a ceiling before handing anything to the NBT reader. Doing it in this
        // order is the point: the reader would otherwise be the thing discovering the size.
        expandCheck(compressed);

        try (ByteArrayInputStream in = new ByteArrayInputStream(compressed)) {
            //? if >=1.21 {
            /*return NbtIo.readCompressed(in, net.minecraft.nbt.NbtAccounter.create(MAX_DECOMPRESSED_BYTES));
            *///?} else {
            return NbtIo.readCompressed(in);
            //?}
        } catch (IOException e) {
            throw new BadCode("That share code is damaged. Copy it again, all of it.");
        }
    }

    private static void expandCheck(byte[] compressed) throws BadCode {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] chunk = new byte[8192];
            long total = 0;
            int read;
            while ((read = gz.read(chunk)) != -1) {
                total += read;
                if (total > MAX_DECOMPRESSED_BYTES) {
                    throw new BadCode("That share code unpacks to far too much data to be an NPC.");
                }
            }
        } catch (IOException e) {
            throw new BadCode("That share code is damaged. Copy it again, all of it.");
        }
    }
}
