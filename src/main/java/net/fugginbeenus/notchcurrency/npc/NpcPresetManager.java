package net.fugginbeenus.notchcurrency.npc;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class NpcPresetManager {

    private static final int MAX_PRESETS = 64;

    private NpcPresetManager() {}

    // Action ids for the NPC_PRESET packet.
    public static final int ACTION_OPEN = 0, ACTION_SAVE = 1, ACTION_LOAD = 2, ACTION_DELETE = 3;

    public static void action(ServerPlayer sp, NotchNpcEntity npc, int action, String name) {
        if (!NotchNpcManager.guard(sp, npc)) return;
        switch (action) {
            case ACTION_SAVE -> save(sp, npc, name);
            case ACTION_LOAD -> load(sp, npc, name);
            case ACTION_DELETE -> delete(sp, name);
        }
        // Every action (including plain OPEN) ends with a fresh list, which opens/updates the screen.
        sendList(sp, npc);
    }

    private static void save(ServerPlayer sp, NotchNpcEntity npc, String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) {
            sp.displayClientMessage(Component.literal("Give the preset a name first.").withStyle(ChatFormatting.RED), false);
            return;
        }
        CompoundTag tag = npc.writeToItem();
        stripWorldSpecific(tag);
        try {
            File file = dir().resolve(name + ".nbt").toFile();
            if (!file.isFile() && list().size() >= MAX_PRESETS) {
                sp.displayClientMessage(Component.literal("Preset limit reached (" + MAX_PRESETS + ") - delete one first.")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
            //? if >=1.21 {
            /*NbtIo.writeCompressed(tag, file.toPath());
            *///?} else {
            NbtIo.writeCompressed(tag, file);
            //?}
            sp.displayClientMessage(Component.literal("Preset '" + name + "' saved.").withStyle(ChatFormatting.GREEN), false);
        } catch (IOException e) {
            sp.displayClientMessage(Component.literal("Couldn't save the preset: " + e.getMessage()).withStyle(ChatFormatting.RED), false);
        }
    }

    private static void load(ServerPlayer sp, NotchNpcEntity npc, String rawName) {
        applyPreset(npc, rawName, sp);
    }

    public static boolean applyPreset(NotchNpcEntity npc, String rawName,
                                      @Nullable ServerPlayer actor) {
        String name = sanitize(rawName);
        CompoundTag tag;
        try {
            File file = dir().resolve(name + ".nbt").toFile();
            if (!file.isFile()) {
                msg(actor, "No preset named '" + name + "'.", ChatFormatting.RED);
                return false;
            }
            //? if >=1.21 {
            /*tag = NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            *///?} else {
            tag = NbtIo.readCompressed(file);
            //?}
        } catch (IOException e) {
            msg(actor, "Couldn't read the preset: " + e.getMessage(), ChatFormatting.RED);
            return false;
        }
        applyTag(npc, tag, actor);
        msg(actor, "Preset '" + name + "' applied.", ChatFormatting.GREEN);
        return true;
    }

    public static void applyTag(NotchNpcEntity npc, CompoundTag tag, @Nullable ServerPlayer actor) {
        stripWorldSpecific(tag); // belt & braces for hand-edited files and pasted codes

        NpcRole role = NpcRole.NONE;
        try {
            role = NpcRole.valueOf(tag.getString("Role"));
        } catch (IllegalArgumentException ignored) {
        }
        // Unwind a linked shop before the config is overwritten so its stock isn't orphaned.
        if (npc.getRole() == NpcRole.SHOP && actor != null) {
            NotchNpcManager.removeLinkedShop(actor, npc.getUUID());
        }
        // The target NPC keeps its own identity: owner, home and route survive the load.
        UUID owner = npc.getOwner();
        String ownerName = npc.getOwnerName();
        NotchNpcEntity.OwnerType ownerType = npc.getOwnerType();
        tag.putString("Role", NpcRole.NONE.name());
        npc.readFromItem(tag);
        npc.setOwnerType(ownerType);
        npc.setOwner(owner, ownerName);
        if (role == NpcRole.SHOP && actor == null) {
            role = NpcRole.NONE;
        }
        npc.setRole(role);
        if (role == NpcRole.SHOP) {
            NotchNpcManager.ensureShopForNpc(actor.serverLevel(), npc, actor);
        }
    }

    private static void msg(@Nullable ServerPlayer actor, String text, ChatFormatting color) {
        if (actor != null) {
            actor.displayClientMessage(Component.literal(text).withStyle(color), false);
        }
    }

    private static void delete(ServerPlayer sp, String rawName) {
        String name = sanitize(rawName);
        try {
            if (Files.deleteIfExists(dir().resolve(name + ".nbt"))) {
                sp.displayClientMessage(Component.literal("Preset '" + name + "' deleted.").withStyle(ChatFormatting.GREEN), false);
            } else {
                sp.displayClientMessage(Component.literal("No preset named '" + name + "'.").withStyle(ChatFormatting.RED), false);
            }
        } catch (IOException e) {
            sp.displayClientMessage(Component.literal("Couldn't delete the preset: " + e.getMessage()).withStyle(ChatFormatting.RED), false);
        }
    }

    public static void sendList(ServerPlayer sp, NotchNpcEntity npc) {
        List<String> names = list();
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(npc.getUUID());
        buf.writeVarInt(names.size());
        for (String n : names) {
            buf.writeUtf(n);
        }
        Net.sendToClient(sp, NotchPackets.NPC_PRESET_LIST, buf);
    }

    public static List<String> list() {
        try (var files = Files.list(dir())) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".nbt"))
                    .map(n -> n.substring(0, n.length() - 4))
                    .sorted()
                    .limit(MAX_PRESETS)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Path dir() throws IOException {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("notchcurrency").resolve("npc_presets");
        Files.createDirectories(dir);
        return dir;
    }

    public static void stripWorldSpecific(CompoundTag tag) {
        tag.remove("Owner");
        tag.remove("OwnerName");
        tag.remove("OwnerType");
        tag.remove("RoleTarget");
        tag.remove("Home");
        tag.remove("Waypoints");
        tag.remove("ActionSweep");
        // The schedule itself is worth carrying (times, stances, what it says on arrival);
        // its coordinates are not. They arrive flagged as needing a spot, which is what the
        // editor's repair flow walks the new owner through.
        net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.stripAnchors(tag, "Schedule");
    }

    private static String sanitize(String name) {
        String clean = (name == null ? "" : name).trim().toLowerCase().replace(' ', '_')
                .replaceAll("[^a-z0-9_\\-]", "");
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }
}
