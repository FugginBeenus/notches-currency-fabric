package net.fugginbeenus.notchcurrency.economy.adminshop;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** World-persistent storage for all admin shops. Stored in the overworld save. */
public class AdminShopState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_adminshops";

    private final Map<UUID, AdminShop> shops = new LinkedHashMap<>();

    public static AdminShopState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
        return mgr.getOrCreate(AdminShopState::fromNbt, AdminShopState::new, DATA_KEY);
    }

    public AdminShop create(String name) {
        AdminShop shop = new AdminShop(name);
        shops.put(shop.getId(), shop);
        markDirty();
        return shop;
    }

    public boolean remove(UUID shopId) {
        boolean removed = shops.remove(shopId) != null;
        if (removed) markDirty();
        return removed;
    }

    @Nullable
    public AdminShop get(UUID shopId) {
        return shops.get(shopId);
    }

    /** Find by exact (case-insensitive) name, else null. */
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

    /** Call every server tick; decays dynamic prices toward baseline on an interval. */
    public void tickDecay() {
        if (shops.isEmpty()) return;
        if (++decayCounter < DECAY_INTERVAL) return;
        decayCounter = 0;
        for (AdminShop s : shops.values()) s.decayAll();
        markDirty();
    }

    // ---- NBT ----

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (AdminShop s : shops.values()) list.add(s.toNbt());
        nbt.put("Shops", list);
        return nbt;
    }

    public static AdminShopState fromNbt(NbtCompound nbt) {
        AdminShopState state = new AdminShopState();
        if (nbt.contains("Shops", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("Shops", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                AdminShop s = AdminShop.fromNbt(list.getCompound(i));
                state.shops.put(s.getId(), s);
            }
        }
        return state;
    }

    /** Helper for command tab-style listing. */
    public List<String> shopNames() {
        List<String> names = new ArrayList<>();
        for (AdminShop s : shops.values()) names.add(s.getName());
        return names;
    }
}
