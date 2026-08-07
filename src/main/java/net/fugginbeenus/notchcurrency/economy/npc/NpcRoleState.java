package net.fugginbeenus.notchcurrency.economy.npc;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcRoleState extends SavedData {

    private static final String DATA_KEY = "notchcurrency_npc_roles";

    public record Assignment(NpcRole role, @Nullable UUID shopId) {}

    private final Map<UUID, Assignment> roles = new HashMap<>();

    public static NpcRoleState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, NpcRoleState::new, NpcRoleState::fromNbt, DATA_KEY);
    }

    public void assign(UUID npcId, NpcRole role, @Nullable UUID shopId) {
        roles.put(npcId, new Assignment(role, shopId));
        setDirty();
    }

    public boolean clear(UUID npcId) {
        boolean removed = roles.remove(npcId) != null;
        if (removed) setDirty();
        return removed;
    }

    @Nullable
    public Assignment get(UUID npcId) {
        return roles.get(npcId);
    }

    // ---- NBT ----

    @Override
    //? if >=1.21 {
    /*public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    *///?} else {
    public CompoundTag save(CompoundTag nbt) {
    //?}
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Assignment> e : roles.entrySet()) {
            CompoundTag o = new CompoundTag();
            o.putUUID("Npc", e.getKey());
            o.putString("Role", e.getValue().role().name());
            if (e.getValue().shopId() != null) o.putUUID("Shop", e.getValue().shopId());
            list.add(o);
        }
        nbt.put("Roles", list);
        return nbt;
    }

    public static NpcRoleState fromNbt(CompoundTag nbt) {
        NpcRoleState state = new NpcRoleState();
        ListTag list = nbt.getList("Roles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag o = list.getCompound(i);
            try {
                UUID npc = o.getUUID("Npc");
                NpcRole role = NpcRole.valueOf(o.getString("Role"));
                UUID shop = o.hasUUID("Shop") ? o.getUUID("Shop") : null;
                state.roles.put(npc, new Assignment(role, shop));
            } catch (IllegalArgumentException ignored) {
                // skip unknown role names
            }
        }
        return state;
    }
}
