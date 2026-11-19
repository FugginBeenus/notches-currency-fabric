package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.compat.Perms;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class NpcEditLocks {

    public static final int NAME = 1, SKIN = 2, SIZE = 4, EFFECTS = 8, PLATE = 16,
            MOVEMENT = 32, FIGHTING = 64,
            DIALOGUE = 128, SOUNDS = 256,
            POSES = 512, MOVE = 1024,
            STATS = 2048, EQUIPMENT = 4096, REACTIONS = 8192, PRESETS = 16384;

    public static final int LOOK_PARTS = NAME | SKIN | SIZE | EFFECTS | PLATE;
    public static final int MOVES_PARTS = MOVEMENT | FIGHTING;
    public static final int TALK_PARTS = DIALOGUE | SOUNDS;
    public static final int POSE_PARTS = POSES | MOVE;

    private static final String[] NODES = {"name", "skin", "size", "effects", "plate", "movement", "fighting",
            "dialogue", "sounds", "poses", "move", "stats", "equipment", "reactions", "presets"};

    public static final int STATS_EFFECT_BITS = 4 | 128;
    public static final int STATS_PLATE_BITS = 8 | 2048;
    public static final int MOVES_FIGHT_BITS = 4 | 8 | 16 | 32 | 64;

    private NpcEditLocks() {}

    private static boolean fallback(int part) {
        NotchConfig.Npcs c = NotchConfigIO.get().npcs;
        return switch (part) {
            case NAME -> c.editName;
            case SKIN -> c.editSkin;
            case SIZE -> c.editSize;
            case EFFECTS -> c.editEffects;
            case PLATE -> c.editPlate;
            case MOVEMENT -> c.editMovement;
            case FIGHTING -> c.editFighting;
            case DIALOGUE -> c.editDialogue;
            case SOUNDS -> c.editSounds;
            case POSES -> c.editPoses;
            case MOVE -> c.editMove;
            case STATS -> c.editStats;
            case EQUIPMENT -> c.editEquipment;
            case REACTIONS -> c.editReactions;
            case PRESETS -> c.editPresets;
            default -> false;
        };
    }

    public static boolean may(ServerPlayer sp, int part) {
        if (Perms.isAdmin(sp, Perms.NPC)) return true;
        int index = Integer.numberOfTrailingZeros(part);
        if (index >= NODES.length) return false;
        return Perms.has(sp, "notchcurrency.npc.edit." + NODES[index], fallback(part));
    }

    public static boolean check(ServerPlayer sp, int part) {
        if (may(sp, part)) return true;
        refuse(sp);
        return false;
    }

    public static void refuse(ServerPlayer sp) {
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(
                "An admin locked this part of the NPC editor.").withStyle(ChatFormatting.RED));
    }

    public static boolean mayPick(ServerPlayer sp, NpcRole role) {
        if (Perms.isAdmin(sp, Perms.NPC)) return true;
        if (role == NpcRole.ADMIN_SHOP || role == NpcRole.CUSTOM) return false;
        String mode = NotchConfigIO.get().npcs.roles;
        boolean fallback = "NONE".equals(mode) ? false
                : "SHOP".equals(mode) ? (role == NpcRole.SHOP || role == NpcRole.NONE)
                : true;
        return Perms.has(sp, "notchcurrency.npc.role." + role.name().toLowerCase(Locale.ROOT), fallback);
    }

    public static int lockedBits(ServerPlayer sp) {
        int bits = 0;
        for (int i = 0; i < NODES.length; i++) {
            if (!may(sp, 1 << i)) bits |= 1 << i;
        }
        return bits;
    }

    public static long allowedRoles(ServerPlayer sp) {
        long mask = 0;
        for (NpcRole role : NpcRole.values()) {
            if (mayPick(sp, role)) mask |= 1L << role.ordinal();
        }
        return mask;
    }

    public static boolean mayReplaceAll(ServerPlayer sp) {
        if (Perms.isAdmin(sp, Perms.NPC)) return true;
        if (lockedBits(sp) != 0) return false;
        for (NpcRole role : NpcRole.values()) {
            if (role == NpcRole.ADMIN_SHOP || role == NpcRole.CUSTOM) continue;
            if (!mayPick(sp, role)) return false;
        }
        return true;
    }

    public static boolean checkReplaceAll(ServerPlayer sp) {
        if (mayReplaceAll(sp)) return true;
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(
                "Presets and share codes change the whole NPC, and an admin locked part of the editor.")
                .withStyle(ChatFormatting.RED));
        return false;
    }
}
