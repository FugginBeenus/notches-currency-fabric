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
    public static int NATURAL_ONE_IN = 100;
    public static int NATURAL_INTERVAL_SECONDS = 300;
    public static int NATURAL_RADIUS = 72;

    /** Game time of the last one that turned up on its own, so the cooldown means something. */
    private static long lastNatural = Long.MIN_VALUE;
    public static int CURRENCY_STACKS_MIN = 1, CURRENCY_STACKS_MAX = 3;
    public static int CURRENCY_PER_STACK_MIN = 100, CURRENCY_PER_STACK_MAX = 250;

    private GoldenCacheManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GoldenCacheManager::tickNatural);
    }

    /**
     * The only way a cache turns up without an admin putting it there.
     *
     * <p>Every so often, each player gets one roll. Win it and a cache is tucked under an oak
     * somewhere near them, out of sight, for somebody to walk into later. The odds are meant to be
     * poor: this is a thing you find twice a year and remember, not a thing you farm.
     *
     * <p>Near a player rather than sprinkled through the world at generation, because a server that
     * has been running for months has all its terrain already, and a worldgen feature would never
     * put a single one in any of it.
     */
    private static void tickNatural(MinecraftServer server) {
        if (!NATURAL_SPAWNS || NATURAL_ONE_IN <= 0) return;

        ServerLevel overworld = server.overworld();
        if (overworld == null) return;

        long now = overworld.getGameTime();
        int everyTicks = Math.max(20, NATURAL_INTERVAL_SECONDS * 20);
        if (now % everyTicks != 0) return;

        // One anywhere on the server per cooldown, however many people are online. Otherwise a busy
        // server would find them at a completely different rate to a quiet one.
        long cooldown = (long) Math.max(0, GLOBAL_COOLDOWN_MIN) * 60L * 20L;
        if (lastNatural != Long.MIN_VALUE && now >= lastNatural && now - lastNatural < cooldown) return;

        for (var player : server.getPlayerList().getPlayers()) {
            if (player.serverLevel() != overworld) continue;
            if (RNG.nextInt(NATURAL_ONE_IN) != 0) continue;

            BlockPos spot = findSpotUnderOak(overworld, player.blockPosition(), Math.max(16, NATURAL_RADIUS));
            if (spot == null) continue;
            if (!placeCacheBlock(overworld, spot)) continue;

            lastNatural = now;
            // Nothing is announced. The whole point is walking into one.
            LOGGER.debug("A golden cache appeared at {} {} {}", spot.getX(), spot.getY(), spot.getZ());
            return;
        }
    }

    /**
     * A patch of forest floor with an oak over it, or null if none was found nearby.
     *
     * <p>Both parts are checked: leaves overhead and a trunk within a few blocks. Leaves alone would
     * accept a lone block somebody placed on a roof, and a trunk alone would accept standing beside
     * a tree rather than under it.
     */
    private static BlockPos findSpotUnderOak(ServerLevel world, BlockPos near, int radius) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = near.getX() + RNG.nextInt(radius * 2 + 1) - radius;
            int z = near.getZ() + RNG.nextInt(radius * 2 + 1) - radius;

            // Before anything else. Asking a world about a block in a chunk it does not have loaded
            // is what would make this expensive, and it is the one cost here worth avoiding.
            if (!world.hasChunkAt(new BlockPos(x, 64, z))) continue;

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

            // Leaves first: it is sixteen lookups and it rules out everywhere that is not under a
            // tree, which is nearly everywhere. The trunk check only runs on what survives it.
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
        NATURAL_INTERVAL_SECONDS = Math.max(1, c.naturalIntervalSeconds);
        NATURAL_RADIUS = Math.max(16, c.naturalRadius);
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
        c.naturalIntervalSeconds = NATURAL_INTERVAL_SECONDS;
        c.naturalRadius          = NATURAL_RADIUS;
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
