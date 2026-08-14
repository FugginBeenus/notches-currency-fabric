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

    // Config knobs (persisted)
    public static int GLOBAL_COOLDOWN_MIN = 60;
    public static boolean NATURAL_SPAWNS = true;
    public static int NATURAL_ONE_IN = 3000;
    public static int MAX_OUTSTANDING = 1;


    public static int CURRENCY_STACKS_MIN = 1, CURRENCY_STACKS_MAX = 3;
    public static int CURRENCY_PER_STACK_MIN = 100, CURRENCY_PER_STACK_MAX = 250;

    private GoldenCacheManager() {}

    public static void init() {
        // 26 added a third argument saying whether the chunk is new. It is not needed here, since
        // the roll is worked out from the seed rather than from when the chunk appeared.
        //? if >=26.1 {
        /*net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register(
                (world, chunk, newlyGenerated) -> onChunkLoad(world, chunk));
        *///?} else {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register(
                GoldenCacheManager::onChunkLoad);
        //?}
    }

    // ChunkPos became a record at 26, so its coordinates are methods there and fields before it.
    //? if >=26.1 {
    /*private static int chunkX(net.minecraft.world.level.ChunkPos pos) { return pos.x(); }
    private static int chunkZ(net.minecraft.world.level.ChunkPos pos) { return pos.z(); }
    private static long chunkKey(net.minecraft.world.level.ChunkPos pos) { return pos.pack(); }
    *///?} else {
    private static int chunkX(net.minecraft.world.level.ChunkPos pos) { return pos.x; }
    private static int chunkZ(net.minecraft.world.level.ChunkPos pos) { return pos.z; }
    private static long chunkKey(net.minecraft.world.level.ChunkPos pos) { return pos.toLong(); }
    //?}

    /**
     * Decides, once and for all, whether a chunk holds a cache.
     *
     * <p>The answer comes from the world seed and the chunk's own coordinates, so it is the same
     * every time it is asked and never has to be written down. A given chunk in a given world either
     * is a cache chunk or is not, and always was.
     *
     * <p>That is what makes this cheap. Nearly every chunk fails the roll on one multiply and a
     * modulo and nothing else happens. Only the rare winner looks at any blocks, and only the rare
     * winner is written to the world save, so that taking the cache and coming back does not hand
     * out another.
     *
     * <p>Deliberately on load rather than on generation. Generation only ever covers new ground, and
     * a world that has been played for months has all of its terrain already: a cache would never
     * appear in any of the places people actually live.
     */
    private static void onChunkLoad(ServerLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        if (!NATURAL_SPAWNS || NATURAL_ONE_IN <= 1) return;
        if (world != world.getServer().overworld()) return;

        int cx = chunkX(chunk.getPos()), cz = chunkZ(chunk.getPos());
        if (!isCacheChunk(world.getSeed(), cx, cz)) return;

        GoldenCacheSpawnState state = GoldenCacheSpawnState.get(world);

        // Already one waiting to be found, so this chunk keeps its win rather than spending it. Not
        // claimed, so it comes up again once the one that is out there has been opened. This is what
        // stops covering ground quickly from being a way to make more caches exist.
        forgetCachesThatAreGone(world, state);
        if (state.outstandingCount() >= MAX_OUTSTANDING) return;

        long key = chunkKey(chunk.getPos());
        // Claimed only once we are actually going to try: a chunk with no oak in it should not be
        // searched again every time somebody walks past, but a chunk we never looked at should not
        // be spent either.
        if (!state.claim(key)) return;

        BlockPos spot = findSpotUnderOak(world, chunk);
        if (spot == null) return;
        if (placeCacheBlock(world, spot)) {
            state.addOutstanding(spot.asLong());
            // Never announced. Walking into one is the whole point.
            LOGGER.debug("A golden cache is hidden at {} {} {}", spot.getX(), spot.getY(), spot.getZ());
        }
    }

    /**
     * Told that one has been opened, so the next chunk to win may place another.
     *
     * <p>Called by the block itself when it is broken, which is how a cache is opened.
     */
    public static void noteOpened(ServerLevel world, BlockPos pos) {
        GoldenCacheSpawnState.get(world).clearOutstanding(pos.asLong());
    }

    /**
     * Drops any cache we are still counting that is no longer actually there.
     *
     * <p>A cache can go without anybody breaking it: an explosion, a world edit, a chunk restored
     * from a backup. Without this the count would stay full and no cache would ever appear again,
     * which is a quiet way for the feature to stop working forever.
     *
     * <p>Only positions in loaded chunks are checked, so this costs nothing and simply catches up as
     * the world is played.
     */
    private static void forgetCachesThatAreGone(ServerLevel world, GoldenCacheSpawnState state) {
        for (long key : state.outstandingPositions()) {
            BlockPos pos = BlockPos.of(key);
            if (!world.hasChunkAt(pos)) continue;
            if (!world.getBlockState(pos).is(ModBlocks.GOLDEN_CACHE)) state.clearOutstanding(key);
        }
    }

    /**
     * Whether this chunk of this world is one of the rare ones.
     *
     * <p>Mixed rather than added, so neighbouring chunks are not neighbouring answers and the caches
     * do not come out in rows.
     */
    private static boolean isCacheChunk(long worldSeed, int cx, int cz) {
        long h = worldSeed ^ (cx * 341873128712L) ^ (cz * 132897987541L);
        h *= 0x9E3779B97F4A7C15L;
        h ^= (h >>> 29);
        return Math.floorMod(h, NATURAL_ONE_IN) == 0;
    }

    /**
     * A patch of forest floor with an oak over it, somewhere in this chunk.
     *
     * <p>Walks the chunk's own columns, so nothing outside it is touched and no chunk is ever pulled
     * in to answer. Runs only for a chunk that has already won the roll.
     */
    private static BlockPos findSpotUnderOak(ServerLevel world, net.minecraft.world.level.chunk.LevelChunk chunk) {
        // Shifted rather than asked for, since the accessor moved too.
        int baseX = chunkX(chunk.getPos()) << 4, baseZ = chunkZ(chunk.getPos()) << 4;

        for (int attempt = 0; attempt < 24; attempt++) {
            int x = baseX + RNG.nextInt(16);
            int z = baseZ + RNG.nextInt(16);

            // The heightmap that ignores leaves, so this is the ground under a canopy rather than
            // the top of the tree.
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y, z);
            if (!world.getBlockState(ground).isAir()) continue;

            // Standing on the floor, not on top of the trunk we are looking for.
            var below = world.getBlockState(ground.below());
            if (below.isAir()
                    || below.is(net.minecraft.world.level.block.Blocks.OAK_LOG)
                    || below.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES)) continue;

            // Leaves first: sixteen lookups, and it rules out everywhere that is not under a tree.
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

    /**
     * A trunk within arm's reach of the spot.
     *
     * <p>Three by three by six rather than anything wider. An oak's trunk is directly under its
     * canopy, so a wider search buys nothing but lookups, and this one runs only where leaves were
     * already found overhead.
     */
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

        // Try to find a clear space (air or non-solid) up to 4 blocks upward
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
