package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Random;

public final class GoldenCacheManager {

    /** Loot table used by the golden cache (e.g. data/notchcurrency/loot_tables/golden_cache.json). */
    public static final Identifier LOOT = new Identifier("notchcurrency", "golden_cache");

    public static boolean ANNOUNCE = true;
    private static final int NATURAL_COOLDOWN_TICKS = 20 * 60 * 60 * 4; // 4h
    private static int cooldown = 0;
    private static final Random RNG = new Random();

    // Config knobs (persisted)
    public static int GLOBAL_COOLDOWN_MIN = 60;
    public static int CURRENCY_STACKS_MIN = 1, CURRENCY_STACKS_MAX = 3;
    public static int CURRENCY_PER_STACK_MIN = 100, CURRENCY_PER_STACK_MAX = 250;

    private GoldenCacheManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GoldenCacheManager::tick);
    }

    private static void tick(MinecraftServer server) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Natural spawns are disabled for now.
        /*
        ServerWorld world = server.getOverworld();
        if (world == null || world.getPlayers().isEmpty()) return;

        var p = world.getPlayers().get(RNG.nextInt(world.getPlayers().size()));
        BlockPos placed = spawnNear(world, p.getBlockPos(), 96);
        if (placed != null) {
            cooldown = NATURAL_COOLDOWN_TICKS;
            if (ANNOUNCE) {
                server.getPlayerManager().broadcast(
                        Text.literal("✨ A Golden Cache has spawned somewhere nearby…"), false
                );
            }
        }
        */
    }

    /** Admin/testing: force a cache near a point within radius. Returns placed location or null. */
    public static BlockPos spawnNear(ServerWorld world, BlockPos center, int radius) {
        if (radius < 1) radius = 1;

        int dx = RNG.nextInt(radius * 2 + 1) - radius;
        int dz = RNG.nextInt(radius * 2 + 1) - radius;

        int x = center.getX() + dx;
        int z = center.getZ() + dz;

        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        return placeCacheBlock(world, pos) ? pos : null;
    }

    public static void applyConfig(net.fugginbeenus.notchcurrency.config.NotchConfig cfg) {
        var c = cfg.cache;
        ANNOUNCE = c.announce;
        GLOBAL_COOLDOWN_MIN = Math.max(0, c.cooldownMinutes);
        CURRENCY_STACKS_MIN = Math.max(0, c.currencyStacksMin);
        CURRENCY_STACKS_MAX = Math.max(CURRENCY_STACKS_MIN, c.currencyStacksMax);
        CURRENCY_PER_STACK_MIN = Math.max(1, c.currencyPerStackMin);
        CURRENCY_PER_STACK_MAX = Math.max(CURRENCY_PER_STACK_MIN, c.currencyPerStackMax);
    }

    public static void exportConfig(net.fugginbeenus.notchcurrency.config.NotchConfig cfg) {
        var c = cfg.cache;
        c.announce            = ANNOUNCE;
        c.cooldownMinutes     = GLOBAL_COOLDOWN_MIN;
        c.currencyStacksMin   = CURRENCY_STACKS_MIN;
        c.currencyStacksMax   = CURRENCY_STACKS_MAX;
        c.currencyPerStackMin = CURRENCY_PER_STACK_MIN;
        c.currencyPerStackMax = CURRENCY_PER_STACK_MAX;
    }

    /** Admin/testing: force a cache exactly at the given position (snapped to safe top ground). */
    public static BlockPos spawnAt(ServerWorld world, int x, int y, int z) {
        int safeY = Math.max(
                y,
                world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z)
        );
        BlockPos pos = new BlockPos(x, safeY, z);
        return placeCacheBlock(world, pos) ? pos : null;
    }

    /**
     * Places the GOLDEN_CACHE block at/above the given position.
     * GoldenCacheBlock handles loot on break.
     */
    private static boolean placeCacheBlock(ServerWorld world, BlockPos pos) {
        BlockPos place = pos;

        // Try to find a clear space (air or non-solid) up to 4 blocks upward
        for (int i = 0; i < 4; i++) {
            var stateAt = world.getBlockState(place);
            boolean clear = stateAt.isAir() || stateAt.getCollisionShape(world, place).isEmpty();
            if (clear) break;
            place = place.up();
        }

        return world.setBlockState(place, ModBlocks.GOLDEN_CACHE.getDefaultState());
    }

    public static void setAnnouncements(boolean enabled) {
        ANNOUNCE = enabled;
    }
}
