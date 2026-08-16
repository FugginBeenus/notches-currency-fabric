package net.fugginbeenus.notchcurrency.npc;

import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NpcShareManager {

    private NpcShareManager() {}

    public static final int ACTION_COPY = 0, ACTION_PASTE = 1, ACTION_SAVE_FILE = 2, ACTION_LOAD_FILE = 3;

    public static void action(ServerPlayer sp, NotchNpcEntity npc, int action, String payload) {
        if (!NotchNpcManager.guard(sp, npc)) return;
        switch (action) {
            case ACTION_COPY -> copy(sp, npc);
            case ACTION_PASTE -> paste(sp, npc, payload);
            case ACTION_SAVE_FILE -> saveFile(sp, npc, payload);
            case ACTION_LOAD_FILE -> loadFile(sp, npc, payload);
        }
    }

    private static void copy(ServerPlayer sp, NotchNpcEntity npc) {
        String code = exportCode(npc);
        if (code == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Couldn't package that NPC.").withStyle(ChatFormatting.RED));
            return;
        }
        if (code.length() > NpcShareCodec.MAX_WIRE_CHARS) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This NPC is too detailed to copy as a code. Use 'To file' instead.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUtf(code, NpcShareCodec.MAX_WIRE_CHARS);
        Net.sendToClient(sp, NotchPackets.NPC_SHARE_CODE, buf);
    }

    private static void paste(ServerPlayer sp, NotchNpcEntity npc, String code) {
        CompoundTag tag;
        try {
            tag = NpcShareCodec.decode(code);
        } catch (NpcShareCodec.BadCode e) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(e.getMessage()).withStyle(ChatFormatting.RED));
            return;
        }
        NpcPresetManager.applyTag(npc, tag, sp);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("NPC imported.").withStyle(ChatFormatting.GREEN));
    }

    private static void saveFile(ServerPlayer sp, NotchNpcEntity npc, String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Give the file a name first.").withStyle(ChatFormatting.RED));
            return;
        }
        String code = exportCode(npc);
        if (code == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Couldn't package that NPC.").withStyle(ChatFormatting.RED));
            return;
        }
        try {
            Path file = dir().resolve(name + ".npc");
            Files.writeString(file, code + System.lineSeparator(), StandardCharsets.UTF_8);
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Exported to npc_share/" + name + ".npc").withStyle(ChatFormatting.GREEN));
        } catch (IOException e) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Couldn't write the file: " + e.getMessage()).withStyle(ChatFormatting.RED));
        }
    }

    private static void loadFile(ServerPlayer sp, NotchNpcEntity npc, String rawName) {
        String name = sanitize(rawName);
        String code;
        try {
            Path file = dir().resolve(name + ".npc");
            if (!Files.isRegularFile(file)) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("No file named '" + name + ".npc' in npc_share.")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            if (Files.size(file) > NpcShareCodec.MAX_CODE_CHARS) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("That file is too big to be an NPC.").withStyle(ChatFormatting.RED));
                return;
            }
            code = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Couldn't read the file: " + e.getMessage()).withStyle(ChatFormatting.RED));
            return;
        }
        paste(sp, npc, code);
    }

    private static String exportCode(NotchNpcEntity npc) {
        CompoundTag tag = npc.writeToItem();
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
