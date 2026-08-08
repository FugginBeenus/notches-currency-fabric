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

public class NpcRoleState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

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

    // Only the older versions call this. 1.21.11 hands writeNbt to a codec instead, so there is
    // nothing on SavedData left to override there.
    //? if >=1.21.11 {
    /*
    *///?} elif >=1.21 {
    /*@Override
    public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        return writeNbt(nbt);
    }
    *///?} else {
    @Override
    public CompoundTag save(CompoundTag nbt) {
        return writeNbt(nbt);
    }
    //?}

    @Override
    public CompoundTag writeNbt(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Assignment> e : roles.entrySet()) {
            CompoundTag o = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Npc", e.getKey());
            o.putString("Role", e.getValue().role().name());
            if (e.getValue().shopId() != null) net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Shop", e.getValue().shopId());
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
                UUID npc = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(o, "Npc");
                NpcRole role = NpcRole.valueOf(o.getString("Role"));
                UUID shop = net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(o, "Shop") ? net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(o, "Shop") : null;
                state.roles.put(npc, new Assignment(role, shop));
            } catch (IllegalArgumentException ignored) {
                // skip unknown role names
            }
        }
        return state;
    }
}
