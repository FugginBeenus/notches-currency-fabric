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

public final class GoldenCacheManager {

    public static final ResourceLocation LOOT = NotchCurrency.id("golden_cache");

    public static boolean ANNOUNCE = true;
    private static final Random RNG = new Random();

    // Config knobs (persisted)
    public static int GLOBAL_COOLDOWN_MIN = 60;
    public static int CURRENCY_STACKS_MIN = 1, CURRENCY_STACKS_MAX = 3;
    public static int CURRENCY_PER_STACK_MIN = 100, CURRENCY_PER_STACK_MAX = 250;

    private GoldenCacheManager() {}

    public static void init() {
        // Natural (timed) cache spawns are not enabled yet; caches are placed via the
        // /cache admin command and spawnNear/spawnAt. Kept as an entry point so a
        // scheduled-spawn loop can be wired up later without touching the initializer.
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
