package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.compat.Nbt;
import net.fugginbeenus.notchcurrency.compat.Perms;
import net.fugginbeenus.notchcurrency.compat.StateData;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class NpcOwnerIndex extends SavedData
        implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private final Map<UUID, UUID> ownerOf = new HashMap<>();
    private final Map<UUID, Set<UUID>> owned = new HashMap<>();

    public static NpcOwnerIndex get(MinecraftServer server) {
        return StateData.getOrCreate(server.overworld().getDataStorage(), NpcOwnerIndex::new,
                NpcOwnerIndex::load, "notchcurrency_npc_owners");
    }

    public NpcOwnerIndex() {}

    public void track(UUID npc, @Nullable UUID owner) {
        UUID old = ownerOf.get(npc);
        if (Objects.equals(old, owner)) return;
        if (old != null) {
            Set<UUID> mine = owned.get(old);
            if (mine != null) {
                mine.remove(npc);
                if (mine.isEmpty()) owned.remove(old);
            }
        }
        if (owner == null) {
            ownerOf.remove(npc);
        } else {
            ownerOf.put(npc, owner);
            owned.computeIfAbsent(owner, k -> new HashSet<>()).add(npc);
        }
        setDirty();
    }

    public void untrack(UUID npc) {
        track(npc, null);
    }

    public int count(UUID owner) {
        Set<UUID> mine = owned.get(owner);
        return mine == null ? 0 : mine.size();
    }

    public static int max() {
        return Math.max(0, NotchConfigIO.get().npcs.maxPerPlayer);
    }

    public static boolean unlimited(ServerPlayer sp) {
        return max() <= 0 || Perms.isAdmin(sp, Perms.NPC) || Perms.has(sp, "notchcurrency.npc.nolimit", false);
    }

    public static int countOf(ServerPlayer sp) {
        MinecraftServer server = sp.level().getServer();
        return server == null ? 0 : get(server).count(sp.getUUID());
    }

    public static boolean full(ServerPlayer sp) {
        return !unlimited(sp) && countOf(sp) >= max();
    }

    public static NpcOwnerIndex load(CompoundTag nbt) {
        NpcOwnerIndex index = new NpcOwnerIndex();
        int count = nbt.getInt("count");
        for (int i = 0; i < count; i++) {
            if (!Nbt.hasUuid(nbt, "n" + i) || !Nbt.hasUuid(nbt, "o" + i)) continue;
            UUID npc = Nbt.getUuid(nbt, "n" + i), owner = Nbt.getUuid(nbt, "o" + i);
            index.ownerOf.put(npc, owner);
            index.owned.computeIfAbsent(owner, k -> new HashSet<>()).add(npc);
        }
        return index;
    }

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
        nbt.putInt("count", ownerOf.size());
        int i = 0;
        for (var entry : ownerOf.entrySet()) {
            Nbt.putUuid(nbt, "n" + i, entry.getKey());
            Nbt.putUuid(nbt, "o" + i, entry.getValue());
            i++;
        }
        return nbt;
    }
}
