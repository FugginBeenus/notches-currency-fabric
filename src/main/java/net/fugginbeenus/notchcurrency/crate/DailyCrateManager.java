package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import java.util.List;

public final class DailyCrateManager {
    private static long lastSpawnWeek = -1;

    private DailyCrateManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(DailyCrateManager::tick);
    }

    // Admin-tweakable defaults (also persisted via BalloonConfigState)
    private static BlockPos CENTER = new BlockPos(0, 80, 0);
    private static int RADIUS = 25;

    public static int MIN_Y = 110;
    public static int MAX_Y = 150;

    private static int BALLOONS_PER_WAVE = 3;
    public static boolean ANNOUNCE = true;

    // Spawn window (morning of first day of week)
    private static final long WINDOW_START = 1000;
    private static final long WINDOW_END   = 2000;

    // Minecraft week = 7 days
    private static final long TICKS_PER_WEEK = 24000L * 7L;

    public static void setPerDay(int n) { BALLOONS_PER_WAVE = Math.max(0, n); }

    private static void tick(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (world == null) return;

        BalloonConfigState cfg = BalloonConfigState.get(world);

        long totalTime = world.getTime(); // Total world time (doesn't reset)
        long week = totalTime / TICKS_PER_WEEK;
        long timeInWeek = totalTime % TICKS_PER_WEEK;

        // Spawn at the start of each week (during the morning window of day 1)
        if (week != lastSpawnWeek && timeInWeek >= WINDOW_START && timeInWeek <= WINDOW_END) {
            lastSpawnWeek = week;

            // Clear existing balloons first to prevent stacking
            int cleared = clearExistingBalloons(world, cfg);
            if (cleared > 0) {
                System.out.println("[NotchCurrency] Cleared " + cleared + " old balloons before spawning new wave");
            }

            spawnBalloons(world, cfg);

            if (cfg.announce) {
                server.getPlayerManager().broadcast(
                        Text.literal("🎈 A new wave of balloon crates has appeared!"), false);
            }
        }
    }

    /**
     * Remove all existing balloon entities in the spawn area (and beyond)
     * Returns count of removed balloons
     */
    private static int clearExistingBalloons(ServerWorld world, BalloonConfigState cfg) {
        int cleared = 0;

        // Create a large search box around the spawn area
        // Search wider than spawn area in case balloons drifted
        int searchRadius = cfg.radius + 100;
        Box searchBox = new Box(
                cfg.center.getX() - searchRadius, 0, cfg.center.getZ() - searchRadius,
                cfg.center.getX() + searchRadius, 320, cfg.center.getZ() + searchRadius
        );

        // Find and remove all balloon entities
        List<BalloonEntity> balloons = world.getEntitiesByType(
                TypeFilter.instanceOf(BalloonEntity.class),
                searchBox,
                entity -> true
        );

        for (BalloonEntity balloon : balloons) {
            balloon.discard();
            cleared++;
        }

        return cleared;
    }

    // apply config from file
    public static void applyConfig(net.fugginbeenus.notchcurrency.config.NotchConfig cfg) {
        var b = cfg.balloon;
        CENTER = new BlockPos(b.centerX, b.centerY, b.centerZ);
        RADIUS = Math.max(1, b.radius);
        MIN_Y  = b.minY;
        MAX_Y  = b.maxY;
        setPerDay(b.perDay);
        ANNOUNCE = b.announce;
    }

    // push current runtime values back into cfg (so command changes can be saved)
    public static void exportConfig(net.fugginbeenus.notchcurrency.config.NotchConfig cfg) {
        var b = cfg.balloon;
        b.centerX = CENTER.getX();
        b.centerY = CENTER.getY();
        b.centerZ = CENTER.getZ();
        b.radius  = RADIUS;
        b.minY    = MIN_Y;
        b.maxY    = MAX_Y;
        b.perDay  = BALLOONS_PER_WAVE;
        b.announce = ANNOUNCE;
    }

    private static void spawnBalloons(ServerWorld world, BalloonConfigState cfg) {
        System.out.println("[NotchCurrency] Spawning " + cfg.perDay + " balloons for week " + lastSpawnWeek);

        for (int i = 0; i < cfg.perDay; i++) {
            int dx = world.random.nextInt(cfg.radius * 2) - cfg.radius;
            int dz = world.random.nextInt(cfg.radius * 2) - cfg.radius;

            int x = cfg.center.getX() + dx;
            int z = cfg.center.getZ() + dz;

            int yChosen = cfg.minY + world.random.nextInt(Math.max(1, (cfg.maxY - cfg.minY + 1)));
            int groundY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            int y = Math.max(yChosen, groundY + 10);

            var balloon = new BalloonEntity(world, x + 0.5, y + 0.5, z + 0.5);
            world.spawnEntity(balloon);
        }
    }

    // ----- Admin helpers (write to persistent state) -----
    public static void setArea(ServerWorld world, BlockPos center, int radius) {
        var cfg = BalloonConfigState.get(world);
        cfg.center = center;
        cfg.radius = Math.max(1, radius);
        cfg.markDirty();
    }
    public static void setYRange(ServerWorld world, int min, int max) {
        var cfg = BalloonConfigState.get(world);
        cfg.minY = Math.max(5, Math.min(min, max));
        cfg.maxY = Math.max(cfg.minY + 5, Math.max(min, max));
        cfg.markDirty();
    }
    public static void setCount(ServerWorld world, int perWave) {
        var cfg = BalloonConfigState.get(world);
        cfg.perDay = Math.max(0, perWave);
        cfg.markDirty();
    }
    public static void setAnnouncements(ServerWorld world, boolean enabled) {
        var cfg = BalloonConfigState.get(world);
        cfg.announce = enabled;
        cfg.markDirty();
    }

    /**
     * Force spawn a new wave of balloons (for admin use)
     * Clears existing balloons first
     */
    public static void forceSpawn(ServerWorld world) {
        BalloonConfigState cfg = BalloonConfigState.get(world);
        int cleared = clearExistingBalloons(world, cfg);
        if (cleared > 0) {
            System.out.println("[NotchCurrency] Force spawn: cleared " + cleared + " old balloons");
        }
        spawnBalloons(world, cfg);
    }
}