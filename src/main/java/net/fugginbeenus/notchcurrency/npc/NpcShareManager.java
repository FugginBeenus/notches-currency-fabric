package net.fugginbeenus.notchcurrency.npc;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NpcShareManager {

    private NpcShareManager() {}

    public static final int ACTION_COPY = 0, ACTION_PASTE = 1, ACTION_SAVE_FILE = 2, ACTION_LOAD_FILE = 3;

    public static void action(ServerPlayerEntity sp, NotchNpcEntity npc, int action, String payload) {
        if (!NotchNpcManager.guard(sp, npc)) return;
        switch (action) {
            case ACTION_COPY -> copy(sp, npc);
            case ACTION_PASTE -> paste(sp, npc, payload);
            case ACTION_SAVE_FILE -> saveFile(sp, npc, payload);
            case ACTION_LOAD_FILE -> loadFile(sp, npc, payload);
        }
    }

    private static void copy(ServerPlayerEntity sp, NotchNpcEntity npc) {
        String code = exportCode(npc);
        if (code == null) {
            sp.sendMessage(Text.literal("Couldn't package that NPC.").formatted(Formatting.RED), false);
            return;
        }
        // A code longer than a packet allows could be sent out (server to client is far roomier) but
        // never pasted back, so handing one over would just be a trap. The file route has no such
        // limit and produces the same text.
        if (code.length() > NpcShareCodec.MAX_WIRE_CHARS) {
            sp.sendMessage(Text.literal("This NPC is too detailed to copy as a code. Use 'To file' instead.")
                    .formatted(Formatting.RED), false);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(code, NpcShareCodec.MAX_WIRE_CHARS);
        Net.sendToClient(sp, NotchPackets.NPC_SHARE_CODE, buf);
    }

    private static void paste(ServerPlayerEntity sp, NotchNpcEntity npc, String code) {
        NbtCompound tag;
        try {
            tag = NpcShareCodec.decode(code);
        } catch (NpcShareCodec.BadCode e) {
            sp.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED), false);
            return;
        }
        NpcPresetManager.applyTag(npc, tag, sp);
        sp.sendMessage(Text.literal("NPC imported.").formatted(Formatting.GREEN), false);
    }

    private static void saveFile(ServerPlayerEntity sp, NotchNpcEntity npc, String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) {
            sp.sendMessage(Text.literal("Give the file a name first.").formatted(Formatting.RED), false);
            return;
        }
        String code = exportCode(npc);
        if (code == null) {
            sp.sendMessage(Text.literal("Couldn't package that NPC.").formatted(Formatting.RED), false);
            return;
        }
        try {
            Path file = dir().resolve(name + ".npc");
            Files.writeString(file, code + System.lineSeparator(), StandardCharsets.UTF_8);
            sp.sendMessage(Text.literal("Exported to npc_share/" + name + ".npc").formatted(Formatting.GREEN), false);
        } catch (IOException e) {
            sp.sendMessage(Text.literal("Couldn't write the file: " + e.getMessage()).formatted(Formatting.RED), false);
        }
    }

    private static void loadFile(ServerPlayerEntity sp, NotchNpcEntity npc, String rawName) {
        String name = sanitize(rawName);
        String code;
        try {
            Path file = dir().resolve(name + ".npc");
            if (!Files.isRegularFile(file)) {
                sp.sendMessage(Text.literal("No file named '" + name + ".npc' in npc_share.")
                        .formatted(Formatting.RED), false);
                return;
            }
            if (Files.size(file) > NpcShareCodec.MAX_CODE_CHARS) {
                sp.sendMessage(Text.literal("That file is too big to be an NPC.").formatted(Formatting.RED), false);
                return;
            }
            code = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            sp.sendMessage(Text.literal("Couldn't read the file: " + e.getMessage()).formatted(Formatting.RED), false);
            return;
        }
        paste(sp, npc, code);
    }

    private static String exportCode(NotchNpcEntity npc) {
        NbtCompound tag = npc.writeToItem();
        NpcPresetManager.stripWorldSpecific(tag);
        try {
            return NpcShareCodec.encode(tag);
        } catch (IOException e) {
            return null;
        }
    }

    private static Path dir() throws IOException {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("notchcurrency").resolve("npc_share");
        Files.createDirectories(dir);
        return dir;
    }

    private static String sanitize(String name) {
        String clean = (name == null ? "" : name).trim().toLowerCase().replace(' ', '_')
                .replaceAll("[^a-z0-9_\\-]", "");
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }
}
