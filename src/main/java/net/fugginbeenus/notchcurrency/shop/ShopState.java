package net.fugginbeenus.notchcurrency.shop;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Persistent state for all player shops in the world.
 * Saved to data/notchcurrency_shops.dat
 */
public class ShopState extends PersistentState {

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

    public static ShopState get(ServerWorld world) {
        return get(world.getServer());
    }

    public static ShopState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not loaded");
        }

        PersistentStateManager manager = overworld.getPersistentStateManager();
        return manager.getOrCreate(
                ShopState::fromNbt,
                ShopState::new,
                DATA_KEY
        );
    }

    // --- Shop Management ---

    /**
     * Creates a new shop for a player.
     * @return The created shop, or null if the player has reached their shop limit
     */
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
        markDirty();
        return shop;
    }

    /**
     * Deletes a shop. Only the owner can delete their shop.
     * @return true if deleted, false if not found or not owned
     */
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
        markDirty();
        return true;
    }

    /**
     * Add an existing shop object to the state.
     * Used when creating shops through the Merchant License.
     */
    public void addShop(PlayerShop shop) {
        shops.put(shop.getShopId(), shop);
        ownerShops.computeIfAbsent(shop.getOwnerId(), k -> new HashSet<>()).add(shop.getShopId());

        if (shop.getLinkedNpcId() != null) {
            npcToShop.put(shop.getLinkedNpcId(), shop.getShopId());
        }

        LOGGER.info("Added shop '{}' for player {} (ID: {})", shop.getShopName(), shop.getOwnerName(), shop.getShopId());
        markDirty();
    }

    /**
     * Remove a shop from the state (internal use, no ownership check).
     * Used when claiming items from a destroyed shopkeeper.
     */
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
        markDirty();
    }

    /**
     * Update shop ownership (admin transfer).
     */
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
        markDirty();
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
        markDirty();
    }

    public void unlinkNpc(UUID npcId) {
        UUID shopId = npcToShop.remove(npcId);
        if (shopId != null) {
            PlayerShop shop = shops.get(shopId);
            if (shop != null && npcId.equals(shop.getLinkedNpcId())) {
                shop.setLinkedNpcId(null);
            }
            markDirty();
        }
    }

    // --- NBT Serialization ---

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList shopList = new NbtList();
        for (PlayerShop shop : shops.values()) {
            shopList.add(shop.toNbt());
        }
        nbt.put("Shops", shopList);

        return nbt;
    }

    public static ShopState fromNbt(NbtCompound nbt) {
        ShopState state = new ShopState();

        if (nbt.contains("Shops", NbtElement.LIST_TYPE)) {
            NbtList shopList = nbt.getList("Shops", NbtElement.COMPOUND_TYPE);
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
        markDirty();
    }

    /**
     * Clean up orphaned shop data.
     * Call this periodically or on server start.
     * @param world The server world to check for NPCs
     * @return Number of orphaned links cleaned up
     */
    public int cleanupOrphans(net.minecraft.server.world.ServerWorld world) {
        int cleaned = 0;
        List<UUID> orphanedNpcs = new java.util.ArrayList<>();

        // Find NPC links where the NPC no longer exists
        for (var entry : npcToShop.entrySet()) {
            UUID npcId = entry.getKey();
            UUID shopId = entry.getValue();

            // Check if NPC exists in world
            if (world.getEntity(npcId) == null) {
                orphanedNpcs.add(npcId);

                // Mark the shop as unlinked
                PlayerShop shop = shops.get(shopId);
                if (shop != null && npcId.equals(shop.getLinkedNpcId())) {
                    shop.setLinkedNpcId(null);
                    LOGGER.info("Unlinked orphaned NPC {} from shop '{}'", npcId, shop.getShopName());
                }
                cleaned++;
            }
        }

        // Remove orphaned NPC mappings
        for (UUID npcId : orphanedNpcs) {
            npcToShop.remove(npcId);
        }

        if (cleaned > 0) {
            markDirty();
            LOGGER.info("Cleaned up {} orphaned NPC links", cleaned);
        }

        return cleaned;
    }
}