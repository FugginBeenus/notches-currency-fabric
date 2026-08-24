package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GoldenCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-Cache");
    public static final ResourceLocation LOOT = NotchCurrency.id("golden_cache");
    public static boolean ANNOUNCE = true;
    private static final Random RNG = new Random();
    private static final java.util.Set<net.minecraft.world.level.ChunkPos> PENDING =
            new java.util.LinkedHashSet<>();
    public static int GLOBAL_COOLDOWN_MIN = 60;
    public static boolean NATURAL_SPAWNS = true;
    public static int NATURAL_ONE_IN = 3000;
    public static int MAX_OUTSTANDING = 1;


    public static int CURRENCY_STACKS_MIN = 1, CURRENCY_STACKS_MAX = 3;
    public static int CURRENCY_PER_STACK_MIN = 100, CURRENCY_PER_STACK_MAX = 250;

    private GoldenCacheManager() {}

    public static void init() {
        //? if >=26.1 {
        /*net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register(
                (world, chunk, newlyGenerated) -> onChunkLoad(world, chunk));
        *///?} else {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register(
                GoldenCacheManager::onChunkLoad);
        //?}
        ServerTickEvents.END_SERVER_TICK.register(GoldenCacheManager::runPending);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
                stopped -> { synchronized (PENDING) { PENDING.clear(); } });
    }

    //? if >=26.1 {
    /*private static int chunkX(net.minecraft.world.level.ChunkPos pos) { return pos.x(); }
    private static int chunkZ(net.minecraft.world.level.ChunkPos pos) { return pos.z(); }
    private static long chunkKey(net.minecraft.world.level.ChunkPos pos) { return pos.pack(); }
    *///?} else {
    private static int chunkX(net.minecraft.world.level.ChunkPos pos) { return pos.x; }
    private static int chunkZ(net.minecraft.world.level.ChunkPos pos) { return pos.z; }
    private static long chunkKey(net.minecraft.world.level.ChunkPos pos) { return pos.toLong(); }
    //?}

    private static void onChunkLoad(ServerLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        if (!NATURAL_SPAWNS || NATURAL_ONE_IN <= 1) return;
        if (world != world.getServer().overworld()) return;

        int cx = chunkX(chunk.getPos()), cz = chunkZ(chunk.getPos());
        if (!isCacheChunk(world.getSeed(), cx, cz)) return;

        synchronized (PENDING) {
            PENDING.add(chunk.getPos());
        }
    }

    private static void runPending(MinecraftServer server) {
        java.util.List<net.minecraft.world.level.ChunkPos> due;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            due = new java.util.ArrayList<>(PENDING);
            PENDING.clear();
        }

        ServerLevel world = server.overworld();
        for (net.minecraft.world.level.ChunkPos pos : due) {
            tryHideCache(world, pos);
        }
    }

    private static void tryHideCache(ServerLevel world, net.minecraft.world.level.ChunkPos pos) {
        int baseX = chunkX(pos) << 4, baseZ = chunkZ(pos) << 4;
        if (!world.hasChunkAt(new BlockPos(baseX, 0, baseZ))) return;

        GoldenCacheSpawnState state = GoldenCacheSpawnState.get(world);

        forgetCachesThatAreGone(world, state);
        if (state.outstandingCount() >= MAX_OUTSTANDING) return;

        if (!state.claim(chunkKey(pos))) return;

        BlockPos spot = findSpotUnderOak(world, baseX, baseZ);
        if (spot == null) return;
        if (placeCacheBlock(world, spot)) {
            state.addOutstanding(spot.asLong());
            LOGGER.debug("A golden cache is hidden at {} {} {}", spot.getX(), spot.getY(), spot.getZ());
        }
    }

    public static void noteOpened(ServerLevel world, BlockPos pos) {
        GoldenCacheSpawnState.get(world).clearOutstanding(pos.asLong());
    }

    private static void forgetCachesThatAreGone(ServerLevel world, GoldenCacheSpawnState state) {
        for (long key : state.outstandingPositions()) {
            BlockPos pos = BlockPos.of(key);
            if (!world.hasChunkAt(pos)) continue;
            if (!world.getBlockState(pos).is(ModBlocks.GOLDEN_CACHE)) state.clearOutstanding(key);
        }
    }

    private static boolean isCacheChunk(long worldSeed, int cx, int cz) {
        long h = worldSeed ^ (cx * 341873128712L) ^ (cz * 132897987541L);
        h *= 0x9E3779B97F4A7C15L;
        h ^= (h >>> 29);
        return Math.floorMod(h, NATURAL_ONE_IN) == 0;
    }

    private static BlockPos findSpotUnderOak(ServerLevel world, int baseX, int baseZ) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = baseX + RNG.nextInt(16);
            int z = baseZ + RNG.nextInt(16);
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y, z);
            if (!world.getBlockState(ground).isAir()) continue;

            var below = world.getBlockState(ground.below());
            if (below.isAir()
                    || below.is(net.minecraft.world.level.block.Blocks.OAK_LOG)
                    || below.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES)) continue;

            if (hasOakAbove(world, ground) && hasOakTrunkNear(world, ground)) return ground;
        }
        return null;
    }

    private static boolean hasOakAbove(ServerLevel world, BlockPos ground) {
        for (int dy = 1; dy <= 16; dy++) {
            if (world.getBlockState(ground.above(dy)).is(net.minecraft.world.level.block.Blocks.OAK_LEAVES)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOakTrunkNear(ServerLevel world, BlockPos ground) {
        for (int dy = 0; dy <= 5; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (world.getBlockState(ground.offset(dx, dy, dz))
                            .is(net.minecraft.world.level.block.Blocks.OAK_LOG)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static BlockPos spawnNear(ServerLevel world, BlockPos center, int radius) {
        if (radius < 1) radius = 1;

        int dx = RNG.nextInt(radius * 2 + 1) - radius;
        int dz = RNG.nextInt(radius * 2 + 1) - radius;

        int x = center.getX() + dx;
        int z = center.getZ() + dz;

        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        return placeCacheBlock(world, pos) ? pos : null;
    }

    public static void applyConfig(net.fugginbeenus.notchcurrency.config.NotchConfig cfg) {
        var c = cfg.cache;
        ANNOUNCE = c.announce;
        GLOBAL_COOLDOWN_MIN = Math.max(0, c.cooldownMinutes);
        NATURAL_SPAWNS = c.naturalSpawns;
        NATURAL_ONE_IN = Math.max(1, c.naturalOneIn);
        MAX_OUTSTANDING = Math.max(1, c.maxOutstanding);
        CURRENCY_STACKS_MIN = Math.max(0, c.currencyStacksMin);
        CURRENCY_STACKS_MAX = Math.max(CURRENCY_STACKS_MIN, c.currencyStacksMax);
        CURRENCY_PER_STACK_MIN = Math.max(1, c.currencyPerStackMin);
        CURRENCY_PER_STACK_MAX = Math.max(CURRENCY_PER_STACK_MIN, c.currencyPerStackMax);
    }

    public static void exportConfig(net.fugginbeenus.notchcurrency.config.NotchConfig cfg) {
        var c = cfg.cache;
        c.announce            = ANNOUNCE;
        c.cooldownMinutes     = GLOBAL_COOLDOWN_MIN;
        c.naturalSpawns          = NATURAL_SPAWNS;
        c.naturalOneIn           = NATURAL_ONE_IN;
        c.maxOutstanding         = MAX_OUTSTANDING;
        c.currencyStacksMin   = CURRENCY_STACKS_MIN;
        c.currencyStacksMax   = CURRENCY_STACKS_MAX;
        c.currencyPerStackMin = CURRENCY_PER_STACK_MIN;
        c.currencyPerStackMax = CURRENCY_PER_STACK_MAX;
    }

    public static BlockPos spawnAt(ServerLevel world, int x, int y, int z) {
        int safeY = Math.max(
                y,
                world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
        );
        BlockPos pos = new BlockPos(x, safeY, z);
        return placeCacheBlock(world, pos) ? pos : null;
    }

    private static boolean placeCacheBlock(ServerLevel world, BlockPos pos) {
        BlockPos place = pos;

        for (int i = 0; i < 4; i++) {
            var stateAt = world.getBlockState(place);
            boolean clear = stateAt.isAir() || stateAt.getCollisionShape(world, place).isEmpty();
            if (clear) break;
            place = place.above();
        }

        return world.setBlockAndUpdate(place, ModBlocks.GOLDEN_CACHE.defaultBlockState());
    }

    public static void setAnnouncements(boolean enabled) {
        ANNOUNCE = enabled;
    }
}
