package net.fugginbeenus.notchcurrency.api;

import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleState;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NpcPresetManager;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NotchNpcApi {

    private static final Map<String, NpcCustomRole> CUSTOM_ROLES = new HashMap<>();

    private NotchNpcApi() {}

    // ---- Notch NPCs: spawning & presets ----

    public static NotchNpcEntity spawnNpc(ServerLevel world, Vec3 pos, float yaw,
                                          @Nullable ServerPlayer owner) {
        NotchNpcEntity npc = new NotchNpcEntity(ModEntities.NOTCH_NPC, world);
        npc.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        if (owner != null) {
            npc.setOwner(owner.getUUID(), owner.getName().getString());
        } else {
            npc.setOwnerType(NotchNpcEntity.OwnerType.SERVER);
        }
        npc.setCustomName(Component.literal("NPC"));
        npc.setCustomNameVisible(true);
        npc.setHome(BlockPos.containing(pos));
        world.addFreshEntity(npc);
        return npc;
    }

    @Nullable
    public static NotchNpcEntity spawnNpcFromPreset(ServerLevel world, Vec3 pos, float yaw,
                                                    String presetName, @Nullable ServerPlayer owner) {
        NotchNpcEntity npc = spawnNpc(world, pos, yaw, owner);
        if (!NpcPresetManager.applyPreset(npc, presetName, owner)) {
            npc.discard();
            return null;
        }
        return npc;
    }

    public static boolean applyPreset(NotchNpcEntity npc, String presetName,
                                      @Nullable ServerPlayer actor) {
        return NpcPresetManager.applyPreset(npc, presetName, actor);
    }

    public static List<String> listPresets() {
        return NpcPresetManager.list();
    }

    // ---- Notch NPCs: custom roles ----

    public static void registerCustomRole(ResourceLocation id, NpcCustomRole handler) {
        if (id != null && handler != null) {
            CUSTOM_ROLES.put(id.toString(), handler);
        }
    }

    public static void setCustomRole(NotchNpcEntity npc, ResourceLocation id) {
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
