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

public final class NotchNpcApi {

    private static final Map<String, NpcCustomRole> CUSTOM_ROLES = new HashMap<>();

    private NotchNpcApi() {}

    // ---- Notch NPCs: spawning & presets ----

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

    public static boolean applyPreset(NotchNpcEntity npc, String presetName,
                                      @Nullable ServerPlayerEntity actor) {
        return NpcPresetManager.applyPreset(npc, presetName, actor);
    }

    public static List<String> listPresets() {
        return NpcPresetManager.list();
    }

    // ---- Notch NPCs: custom roles ----

    public static void registerCustomRole(Identifier id, NpcCustomRole handler) {
        if (id != null && handler != null) {
            CUSTOM_ROLES.put(id.toString(), handler);
        }
    }

    public static void setCustomRole(NotchNpcEntity npc, Identifier id) {
        npc.setCustomRoleId(id == null ? "" : id.toString());
        npc.setRole(NpcRole.CUSTOM);
    }

    @Nullable
    public static NpcCustomRole customRole(String id) {
        return CUSTOM_ROLES.get(id);
    }

    // ---- external entities: role binding by UUID ----

    public static void assignRole(MinecraftServer server, UUID npcId, NpcRole role, @Nullable UUID shopId) {
        if (server == null || npcId == null || role == null) return;
        NpcRoleState.get(server).assign(npcId, role, shopId);
    }

    public static boolean clearRole(MinecraftServer server, UUID npcId) {
        if (server == null || npcId == null) return false;
        return NpcRoleState.get(server).clear(npcId);
    }

    @Nullable
    public static NpcRoleState.Assignment getRole(MinecraftServer server, UUID npcId) {
        if (server == null || npcId == null) return null;
        return NpcRoleState.get(server).get(npcId);
    }
}
