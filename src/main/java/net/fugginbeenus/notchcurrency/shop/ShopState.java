package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ShopState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final Logger LOGGER = LogManager.getLogger("NotchCurrency-ShopState");
    private static final String DATA_KEY = "notchcurrency_shops";

    // shopId -> PlayerShop
    private final Map<UUID, PlayerShop> shops = new HashMap<>();

    // ownerId -> list of shopIds (for quick lookup)
    private final Map<UUID, Set<UUID>> ownerShops = new HashMap<>();

    // npcId -> shopId (for NPC interaction lookup)
    private final Map<UUID, UUID> npcToShop = new HashMap<>();

    public ShopState() {
        super();
    }

    // --- Static Accessors ---

    public static ShopState get(ServerLevel world) {
        return get(world.getServer());
    }

    public static ShopState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not loaded");
        }

        DimensionDataStorage manager = overworld.getDataStorage();
        return StateData.getOrCreate(manager, ShopState::new, ShopState::fromNbt, DATA_KEY);
    }

    // --- Shop Management ---

    @Nullable
    public PlayerShop createShop(UUID ownerId, String ownerName, String shopName, int maxShopsPerPlayer) {
        Set<UUID> existing = ownerShops.getOrDefault(ownerId, Collections.emptySet());
        if (existing.size() >= maxShopsPerPlayer) {
            LOGGER.info("Player {} has reached shop limit ({})", ownerName, maxShopsPerPlayer);
            return null;
        }

        PlayerShop shop = new PlayerShop(ownerId, ownerName, shopName);
        shops.put(shop.getShopId(), shop);
        ownerShops.computeIfAbsent(ownerId, k -> new HashSet<>()).add(shop.getShopId());

        LOGGER.info("Created shop '{}' for player {} (ID: {})", shopName, ownerName, shop.getShopId());
        setDirty();
        return shop;
    }

    public boolean deleteShop(UUID shopId, UUID requesterId) {
        PlayerShop shop = shops.get(shopId);
        if (shop == null) return false;
        if (!shop.getOwnerId().equals(requesterId)) return false;

        shops.remove(shopId);

        Set<UUID> owned = ownerShops.get(shop.getOwnerId());
        if (owned != null) {
            owned.remove(shopId);
            if (owned.isEmpty()) {
                ownerShops.remove(shop.getOwnerId());
            }
        }

        if (shop.getLinkedNpcId() != null) {
            npcToShop.remove(shop.getLinkedNpcId());
        }

        LOGGER.info("Deleted shop '{}' (ID: {})", shop.getShopName(), shopId);
        setDirty();
        return true;
    }

    public void addShop(PlayerShop shop) {
        shops.put(shop.getShopId(), shop);
        ownerShops.computeIfAbsent(shop.getOwnerId(), k -> new HashSet<>()).add(shop.getShopId());

        if (shop.getLinkedNpcId() != null) {
            npcToShop.put(shop.getLinkedNpcId(), shop.getShopId());
        }

        LOGGER.info("Added shop '{}' for player {} (ID: {})", shop.getShopName(), shop.getOwnerName(), shop.getShopId());
        setDirty();
    }

    public void removeShop(UUID shopId) {
        PlayerShop shop = shops.remove(shopId);
        if (shop == null) return;

        Set<UUID> owned = ownerShops.get(shop.getOwnerId());
        if (owned != null) {
            owned.remove(shopId);
            if (owned.isEmpty()) {
                ownerShops.remove(shop.getOwnerId());
            }
        }

        if (shop.getLinkedNpcId() != null) {
            npcToShop.remove(shop.getLinkedNpcId());
        }

        LOGGER.info("Removed shop '{}' (ID: {})", shop.getShopName(), shopId);
        setDirty();
    }

    public void updateShopOwnership(UUID shopId, UUID newOwnerId, String newOwnerName) {
        PlayerShop shop = shops.get(shopId);
        if (shop == null) return;

        UUID oldOwnerId = shop.getOwnerId();

        // Remove from old owner's list
        Set<UUID> oldOwned = ownerShops.get(oldOwnerId);
        if (oldOwned != null) {
            oldOwned.remove(shopId);
            if (oldOwned.isEmpty()) {
                ownerShops.remove(oldOwnerId);
            }
        }

        // Transfer ownership in shop
        shop.transferOwnership(newOwnerId, newOwnerName);

        // Add to new owner's list
        ownerShops.computeIfAbsent(newOwnerId, k -> new HashSet<>()).add(shopId);

        LOGGER.info("Transferred shop '{}' from {} to {}", shop.getShopName(), oldOwnerId, newOwnerId);
        setDirty();
    }

    @Nullable
    public PlayerShop getShop(UUID shopId) {
        return shops.get(shopId);
    }

    @Nullable
    public PlayerShop getShopByNpc(UUID npcId) {
        UUID shopId = npcToShop.get(npcId);
        if (shopId == null) return null;
        return shops.get(shopId);
    }

    public List<PlayerShop> getShopsByOwner(UUID ownerId) {
        Set<UUID> shopIds = ownerShops.get(ownerId);
        if (shopIds == null) return Collections.emptyList();

        return shopIds.stream()
                .map(shops::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<PlayerShop> getAllOpenShops() {
        return shops.values().stream()
                .filter(PlayerShop::isOpen)
                .collect(Collectors.toList());
    }

    public Collection<PlayerShop> getAllShops() {
        return Collections.unmodifiableCollection(shops.values());
    }

    // --- NPC Linking ---

    public void linkNpcToShop(UUID npcId, UUID shopId) {
        PlayerShop shop = shops.get(shopId);
        if (shop == null) return;

        // Unlink from previous shop if any
        UUID previousShop = npcToShop.get(npcId);
        if (previousShop != null && !previousShop.equals(shopId)) {
            PlayerShop prev = shops.get(previousShop);
            if (prev != null && npcId.equals(prev.getLinkedNpcId())) {
                prev.setLinkedNpcId(null);
            }
        }

        // Unlink shop's previous NPC if any
        UUID previousNpc = shop.getLinkedNpcId();
        if (previousNpc != null && !previousNpc.equals(npcId)) {
            npcToShop.remove(previousNpc);
        }

        shop.setLinkedNpcId(npcId);
        npcToShop.put(npcId, shopId);

        LOGGER.info("Linked NPC {} to shop '{}'", npcId, shop.getShopName());
        setDirty();
    }

    public void unlinkNpc(UUID npcId) {
        UUID shopId = npcToShop.remove(npcId);
        if (shopId != null) {
            PlayerShop shop = shops.get(shopId);
            if (shop != null && npcId.equals(shop.getLinkedNpcId())) {
                shop.setLinkedNpcId(null);
            }
            setDirty();
        }
    }

    // --- NBT Serialization ---

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
        ListTag shopList = new ListTag();
        for (PlayerShop shop : shops.values()) {
            shopList.add(shop.toNbt());
        }
        nbt.put("Shops", shopList);

        return nbt;
    }

    public static ShopState fromNbt(CompoundTag nbt) {
        ShopState state = new ShopState();

        if (nbt.contains("Shops", Tag.TAG_LIST)) {
            ListTag shopList = nbt.getList("Shops", Tag.TAG_COMPOUND);
            for (int i = 0; i < shopList.size(); i++) {
                PlayerShop shop = PlayerShop.fromNbt(shopList.getCompound(i));
                state.shops.put(shop.getShopId(), shop);
                state.ownerShops.computeIfAbsent(shop.getOwnerId(), k -> new HashSet<>())
                        .add(shop.getShopId());

                if (shop.getLinkedNpcId() != null) {
                    state.npcToShop.put(shop.getLinkedNpcId(), shop.getShopId());
                }
            }
        }

        LOGGER.info("Loaded {} player shops from world data", state.shops.size());
        return state;
    }

    // --- Utility ---

    public void markDirtyAndSave() {
        setDirty();
    }

    /** What a sweep found. Nothing is changed unless {@code apply} was set. */
    public record OrphanSweep(int missing, int unlinked) {}

    /**
     * Finds shops whose linked NPC could not be located, and optionally unlinks them.
     *
     * <p>Two things this deliberately does not do. It does not look in one dimension: an NPC in the
     * nether is not a missing NPC. And it does not unlink on its own, because a missing NPC and an
     * NPC standing in an unloaded chunk are the same thing from here. There is no way to tell them
     * apart without loading the whole world, so the sweep reports and the operator decides.
     *
     * <p>Unlinking is not reversible from in game. Getting it wrong on a market district that
     * happened to be unloaded would cut every shop loose from its shopkeeper at once.
     */
    public OrphanSweep cleanupOrphans(MinecraftServer server, boolean apply) {
        List<UUID> missing = new java.util.ArrayList<>();

        for (var entry : npcToShop.entrySet()) {
            UUID npcId = entry.getKey();
            boolean found = false;
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(npcId) != null) { found = true; break; }
            }
            if (!found) missing.add(npcId);
        }

        if (!apply) return new OrphanSweep(missing.size(), 0);

        int unlinked = 0;
        for (UUID npcId : missing) {
            UUID shopId = npcToShop.get(npcId);
            PlayerShop shop = shopId == null ? null : shops.get(shopId);
            if (shop != null && npcId.equals(shop.getLinkedNpcId())) {
                shop.setLinkedNpcId(null);
                LOGGER.info("Unlinked NPC {} from shop '{}' (operator confirmed)", npcId, shop.getShopName());
            }
            npcToShop.remove(npcId);
            unlinked++;
        }
        if (unlinked > 0) setDirty();
        return new OrphanSweep(missing.size(), unlinked);
    }
}