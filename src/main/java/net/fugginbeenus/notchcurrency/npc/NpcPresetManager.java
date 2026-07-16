package net.fugginbeenus.notchcurrency.npc;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Named NPC presets: a full NPC setup saved as a compressed NBT file under
 * config/notchcurrency/npc_presets/, loadable onto any NPC in any world — build your NPCs once in a
 * test world, stamp them into the real one. Presets are templates: ownership and world-specific bits
 * (shop link, home, patrol route) are stripped, so the target NPC keeps its own owner and location.
 * This is EasyNPC's preset system minus the default/local/world/custom storage maze — one folder.
 */
public final class NpcPresetManager {

    private static final int MAX_PRESETS = 64;

    private NpcPresetManager() {}

    // Action ids for the NPC_PRESET packet.
    public static final int ACTION_OPEN = 0, ACTION_SAVE = 1, ACTION_LOAD = 2, ACTION_DELETE = 3;

    public static void action(ServerPlayerEntity sp, NotchNpcEntity npc, int action, String name) {
        if (!NotchNpcManager.guard(sp, npc)) return;
        switch (action) {
            case ACTION_SAVE -> save(sp, npc, name);
            case ACTION_LOAD -> load(sp, npc, name);
            case ACTION_DELETE -> delete(sp, name);
        }
        // Every action (including plain OPEN) ends with a fresh list, which opens/updates the screen.
        sendList(sp, npc);
    }

    private static void save(ServerPlayerEntity sp, NotchNpcEntity npc, String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) {
            sp.sendMessage(Text.literal("Give the preset a name first.").formatted(Formatting.RED), false);
            return;
        }
        NbtCompound tag = npc.writeToItem();
        stripWorldSpecific(tag);
        try {
            File file = dir().resolve(name + ".nbt").toFile();
            if (!file.isFile() && list().size() >= MAX_PRESETS) {
                sp.sendMessage(Text.literal("Preset limit reached (" + MAX_PRESETS + ") — delete one first.")
                        .formatted(Formatting.RED), false);
                return;
            }
            //? if >=1.21 {
            /*NbtIo.writeCompressed(tag, file.toPath());
            *///?} else {
            NbtIo.writeCompressed(tag, file);
            //?}
            sp.sendMessage(Text.literal("Preset '" + name + "' saved.").formatted(Formatting.GREEN), false);
        } catch (IOException e) {
            sp.sendMessage(Text.literal("Couldn't save the preset: " + e.getMessage()).formatted(Formatting.RED), false);
        }
    }

    private static void load(ServerPlayerEntity sp, NotchNpcEntity npc, String rawName) {
        applyPreset(npc, rawName, sp);
    }

    /**
     * Core preset apply, also usable without a player (API / commands). Returns false when the
     * preset is missing or unreadable. Without an actor, SHOP-role handling is skipped (a player
     * shop needs a player to own it) — the NPC lands role-less instead.
     */
    public static boolean applyPreset(NotchNpcEntity npc, String rawName,
                                      @Nullable ServerPlayerEntity actor) {
        String name = sanitize(rawName);
        NbtCompound tag;
        try {
            File file = dir().resolve(name + ".nbt").toFile();
            if (!file.isFile()) {
                msg(actor, "No preset named '" + name + "'.", Formatting.RED);
                return false;
            }
            //? if >=1.21 {
            /*tag = NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
            *///?} else {
            tag = NbtIo.readCompressed(file);
            //?}
        } catch (IOException e) {
            msg(actor, "Couldn't read the preset: " + e.getMessage(), Formatting.RED);
            return false;
        }
        stripWorldSpecific(tag); // belt & braces for hand-edited files

        NpcRole role = NpcRole.NONE;
        try {
            role = NpcRole.valueOf(tag.getString("Role"));
        } catch (IllegalArgumentException ignored) {
        }
        // Unwind a linked shop before the config is overwritten so its stock isn't orphaned.
        if (npc.getRole() == NpcRole.SHOP && actor != null) {
            NotchNpcManager.removeLinkedShop(actor, npc.getUuid());
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
            NotchNpcManager.ensureShopForNpc(actor.getServerWorld(), npc, actor);
        }
        msg(actor, "Preset '" + name + "' applied.", Formatting.GREEN);
        return true;
    }

    private static void msg(@Nullable ServerPlayerEntity actor, String text, Formatting color) {
        if (actor != null) {
            actor.sendMessage(Text.literal(text).formatted(color), false);
        }
    }

    private static void delete(ServerPlayerEntity sp, String rawName) {
        String name = sanitize(rawName);
        try {
            if (Files.deleteIfExists(dir().resolve(name + ".nbt"))) {
                sp.sendMessage(Text.literal("Preset '" + name + "' deleted.").formatted(Formatting.GREEN), false);
            } else {
                sp.sendMessage(Text.literal("No preset named '" + name + "'.").formatted(Formatting.RED), false);
            }
        } catch (IOException e) {
            sp.sendMessage(Text.literal("Couldn't delete the preset: " + e.getMessage()).formatted(Formatting.RED), false);
        }
    }

    public static void sendList(ServerPlayerEntity sp, NotchNpcEntity npc) {
        List<String> names = list();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npc.getUuid());
        buf.writeVarInt(names.size());
        for (String n : names) {
            buf.writeString(n);
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

    /** Presets carry no ownership or world-specific data. */
    private static void stripWorldSpecific(NbtCompound tag) {
        tag.remove("Owner");
        tag.remove("OwnerName");
        tag.remove("OwnerType");
        tag.remove("RoleTarget");
        tag.remove("Home");
        tag.remove("Waypoints");
    }

    /** File-safe preset name: lowercase, spaces to underscores, [a-z0-9_-] only, max 32 chars. */
    private static String sanitize(String name) {
        String clean = (name == null ? "" : name).trim().toLowerCase().replace(' ', '_')
                .replaceAll("[^a-z0-9_\\-]", "");
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }
}
