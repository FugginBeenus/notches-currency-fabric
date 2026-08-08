package net.fugginbeenus.notchcurrency.economy.adminshop;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminShopState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String DATA_KEY = "notchcurrency_adminshops";

    private final Map<UUID, AdminShop> shops = new LinkedHashMap<>();

    public static AdminShopState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, AdminShopState::new, AdminShopState::fromNbt, DATA_KEY);
    }

    public AdminShop create(String name) {
        AdminShop shop = new AdminShop(name);
        shops.put(shop.getId(), shop);
        setDirty();
        return shop;
    }

    public boolean remove(UUID shopId) {
        boolean removed = shops.remove(shopId) != null;
        if (removed) setDirty();
        return removed;
    }

    @Nullable
    public AdminShop get(UUID shopId) {
        return shops.get(shopId);
    }

    @Nullable
    public AdminShop getByName(String name) {
        for (AdminShop s : shops.values()) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    public Collection<AdminShop> all() {
        return shops.values();
    }

    // Throttle dynamic-price recovery to once every ~10 seconds.
    private int decayCounter = 0;
    private static final int DECAY_INTERVAL = 200;

    public void tickDecay() {
        if (shops.isEmpty()) return;
        if (++decayCounter < DECAY_INTERVAL) return;
        decayCounter = 0;
        for (AdminShop s : shops.values()) s.decayAll();
        setDirty();
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
        for (AdminShop s : shops.values()) list.add(s.toNbt());
        nbt.put("Shops", list);
        return nbt;
    }

    public static AdminShopState fromNbt(CompoundTag nbt) {
        AdminShopState state = new AdminShopState();
        if (nbt.contains("Shops", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("Shops", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                AdminShop s = AdminShop.fromNbt(list.getCompound(i));
                state.shops.put(s.getId(), s);
            }
        }
        return state;
    }

    public List<String> shopNames() {
        List<String> names = new ArrayList<>();
        for (AdminShop s : shops.values()) names.add(s.getName());
        return names;
    }
}
