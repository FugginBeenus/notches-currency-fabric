package net.fugginbeenus.notchcurrency.economy.npc;

import net.fugginbeenus.notchcurrency.compat.StateData;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcRoleState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_npc_roles";

    public record Assignment(NpcRole role, @Nullable UUID shopId) {}

    private final Map<UUID, Assignment> roles = new HashMap<>();

    public static NpcRoleState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
        return StateData.getOrCreate(mgr, NpcRoleState::new, NpcRoleState::fromNbt, DATA_KEY);
    }

    public void assign(UUID npcId, NpcRole role, @Nullable UUID shopId) {
        roles.put(npcId, new Assignment(role, shopId));
        markDirty();
    }

    public boolean clear(UUID npcId) {
        boolean removed = roles.remove(npcId) != null;
        if (removed) markDirty();
        return removed;
    }

    @Nullable
    public Assignment get(UUID npcId) {
        return roles.get(npcId);
    }

    // ---- NBT ----

    @Override
    //? if >=1.21 {
    /*public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound nbt) {
    //?}
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Assignment> e : roles.entrySet()) {
            NbtCompound o = new NbtCompound();
            o.putUuid("Npc", e.getKey());
            o.putString("Role", e.getValue().role().name());
            if (e.getValue().shopId() != null) o.putUuid("Shop", e.getValue().shopId());
            list.add(o);
        }
        nbt.put("Roles", list);
        return nbt;
    }

    public static NpcRoleState fromNbt(NbtCompound nbt) {
        NpcRoleState state = new NpcRoleState();
        NbtList list = nbt.getList("Roles", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound o = list.getCompound(i);
            try {
                UUID npc = o.getUuid("Npc");
                NpcRole role = NpcRole.valueOf(o.getString("Role"));
                UUID shop = o.containsUuid("Shop") ? o.getUuid("Shop") : null;
                state.roles.put(npc, new Assignment(role, shop));
            } catch (IllegalArgumentException ignored) {
                // skip unknown role names
            }
        }
        return state;
    }
}
