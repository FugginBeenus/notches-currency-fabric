package net.fugginbeenus.notchcurrency.api;

import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleState;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NpcPresetManager;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public API for Notch Currency's NPC system.
 *
 * Two layers:
 * <ul>
 * <li><b>External entities</b>: any NPC mod (APP.ly, EasyNPC, even a villager) can be bound to an
 * economy role by UUID via {@link #assignRole}; interacting opens the matching feature. No entity-
 * level dependency needed.</li>
 * <li><b>Notch NPCs</b>: spawn the built-in, fully-customizable NPC programmatically
 * ({@link #spawnNpc}, {@link #spawnNpcFromPreset}), stamp saved presets onto it
 * ({@link #applyPreset}), or give it mod-defined behavior with {@link #registerCustomRole} +
 * {@link #setCustomRole}.</li>
 * </ul>
 */
public final class NotchNpcApi {

    private static final Map<String, NpcCustomRole> CUSTOM_ROLES = new HashMap<>();

    private NotchNpcApi() {}

    // ---- Notch NPCs: spawning & presets ----

    /**
     * Spawn a blank Notch NPC. With an owner, that player can edit it like a placed one; without,
     * it's server-owned (op-only editing): right for permanent modpack NPCs.
     */
    public static NotchNpcEntity spawnNpc(ServerWorld world, Vec3d pos, float yaw,
                                          @Nullable ServerPlayerEntity owner) {
        NotchNpcEntity npc = new NotchNpcEntity(ModEntities.NOTCH_NPC, world);
        npc.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0f);
        npc.setHeadYaw(yaw);
        npc.setBodyYaw(yaw);
        if (owner != null) {
            npc.setOwner(owner.getUuid(), owner.getName().getString());
        } else {
            npc.setOwnerType(NotchNpcEntity.OwnerType.SERVER);
        }
        npc.setCustomName(Text.literal("NPC"));
        npc.setCustomNameVisible(true);
        npc.setHome(BlockPos.ofFloored(pos));
        world.spawnEntity(npc);
        return npc;
    }

    /**
     * Spawn a Notch NPC and stamp a saved preset (config/notchcurrency/npc_presets) onto it.
     * Returns null (and spawns nothing) when the preset doesn't exist.
     */
    @Nullable
    public static NotchNpcEntity spawnNpcFromPreset(ServerWorld world, Vec3d pos, float yaw,
                                                    String presetName, @Nullable ServerPlayerEntity owner) {
        NotchNpcEntity npc = spawnNpc(world, pos, yaw, owner);
        if (!NpcPresetManager.applyPreset(npc, presetName, owner)) {
            npc.discard();
            return null;
        }
        return npc;
    }

    /** Apply a saved preset to an existing Notch NPC. The NPC keeps its owner, home and route. */
    public static boolean applyPreset(NotchNpcEntity npc, String presetName,
                                      @Nullable ServerPlayerEntity actor) {
        return NpcPresetManager.applyPreset(npc, presetName, actor);
    }

    /** The names of all saved presets. */
    public static List<String> listPresets() {
        return NpcPresetManager.list();
    }

    // ---- Notch NPCs: custom roles ----

    /**
     * Register a custom role handler (call during mod init). Assign it to NPCs with
     * {@link #setCustomRole}; the handler runs when a player right-clicks the NPC.
     */
    public static void registerCustomRole(Identifier id, NpcCustomRole handler) {
        if (id != null && handler != null) {
            CUSTOM_ROLES.put(id.toString(), handler);
        }
    }

    /** Give a Notch NPC a registered custom role. */
    public static void setCustomRole(NotchNpcEntity npc, Identifier id) {
        npc.setCustomRoleId(id == null ? "" : id.toString());
        npc.setRole(NpcRole.CUSTOM);
    }

    /** The handler registered under {@code id}, or null. Used by the role dispatcher. */
    @Nullable
    public static NpcCustomRole customRole(String id) {
        return CUSTOM_ROLES.get(id);
    }

    // ---- external entities: role binding by UUID ----

    /**
     * Bind an NPC to a role.
     * @param shopId required for {@link NpcRole#ADMIN_SHOP} (the target shop), otherwise null
     */
    public static void assignRole(MinecraftServer server, UUID npcId, NpcRole role, @Nullable UUID shopId) {
        if (server == null || npcId == null || role == null) return;
        NpcRoleState.get(server).assign(npcId, role, shopId);
    }

    /** Remove an NPC's role binding. Returns true if it had one. */
    public static boolean clearRole(MinecraftServer server, UUID npcId) {
        if (server == null || npcId == null) return false;
        return NpcRoleState.get(server).clear(npcId);
    }

    /** The NPC's current role assignment, or null if unbound. */
    @Nullable
    public static NpcRoleState.Assignment getRole(MinecraftServer server, UUID npcId) {
        if (server == null || npcId == null) return null;
        return NpcRoleState.get(server).get(npcId);
    }
}
