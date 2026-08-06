package net.fugginbeenus.notchcurrency.npc;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Turns an NPC config into a line of text and back, so an NPC can leave the world it was built in.
 *
 * <p>Presets already write files, but they land in the <em>server's</em> config folder, which nobody
 * on a multiplayer server can reach. A share code is text: copy it to the clipboard, paste it in
 * Discord, drop it in a file, mail it. The same string is also exactly what a {@code .npc} file
 * contains, so one format covers both without a second encoder.
 *
 * <p>Format is {@code NOTCHNPC1:<base64 of the gzipped NBT>}. The version in the prefix is there so a
 * later format can be recognised and rejected with a clear message instead of failing as corrupt.
 *
 * <p><b>Decoding treats its input as hostile.</b> A share code arrives from another player, which
 * makes it the one place in the mod where a stranger hands the server bytes to parse. Two limits
 * apply: the text length is checked before any decoding, and the gzip stream is expanded through a
 * hard ceiling first, so a small code that inflates to gigabytes is refused rather than parsed.
 * Callers still have to strip ownership and re-sweep actions. This class only promises well-formed
 * NBT of a sane size, never that the contents are safe to trust.
 */
public final class NpcShareCodec {

    public static final String PREFIX = "NOTCHNPC1:";

    /**
     * What a code may be to travel in a packet. Vanilla refuses a client-to-server custom payload
     * over 32767 bytes and disconnects the sender, so this is a hard ceiling, not a preference. The
     * codes are base64, one byte per character, and the rest of the packet is a uuid and two small
     * numbers, which leaves comfortable room under the limit.
     */
    public static final int MAX_WIRE_CHARS = 30_000;

    /**
     * What a code may be at all. Files never cross the network, so a genuinely enormous NPC can
     * still be moved as a {@code .npc} file even when it is too big to paste.
     */
    public static final int MAX_CODE_CHARS = 256_000;
    /** Ceiling on the expanded NBT. Above this the code is a zip bomb, not an NPC. */
    private static final int MAX_DECOMPRESSED_BYTES = 4 * 1024 * 1024;

    private NpcShareCodec() {}

    /** Thrown with a message meant to be shown to the player as-is. */
    public static class BadCode extends Exception {
        public BadCode(String message) {
            super(message);
        }
    }

    public static String encode(NbtCompound tag) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, out);
        return PREFIX + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** True for anything that looks like one of our codes, so callers can tell "wrong text" from
     *  "broken code" before they bother the server with it. */
    public static boolean looksLikeCode(@Nullable String text) {
        return text != null && text.strip().startsWith(PREFIX);
    }

    public static NbtCompound decode(@Nullable String rawText) throws BadCode {
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
            /*return NbtIo.readCompressed(in, net.minecraft.nbt.NbtSizeTracker.of(MAX_DECOMPRESSED_BYTES));
            *///?} else {
            return NbtIo.readCompressed(in);
            //?}
        } catch (IOException e) {
            throw new BadCode("That share code is damaged. Copy it again, all of it.");
        }
    }

    /** Read the gzip stream to its end (or to the ceiling) without keeping any of it. */
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
