package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class DailyCrateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency");
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

    /** Said once per run, not once a week. */
    private static boolean warnedUnconfigured = false;

    private static void tick(MinecraftServer server) {
        ServerLevel world = server.overworld();
        if (world == null) return;

        BalloonConfigState cfg = BalloonConfigState.get(world);

        long totalTime = world.getGameTime(); // Total world time (doesn't reset)
        long week = totalTime / TICKS_PER_WEEK;
        long timeInWeek = totalTime % TICKS_PER_WEEK;

        // Spawn at the start of each week (during the morning window of day 1)
        if (week != lastSpawnWeek && timeInWeek >= WINDOW_START && timeInWeek <= WINDOW_END) {
            lastSpawnWeek = week;

            // Nowhere has been chosen for them, so there is nowhere to put them. Silently, because
            // a server that has never set this up does not want weekly news about it.
            if (!cfg.configured) {
                if (!warnedUnconfigured) {
                    warnedUnconfigured = true;
                    LOGGER.info("Balloon crates are idle: no area set. Use /balloon setArea to pick one.");
                }
                return;
            }

            // Clear existing balloons first to prevent stacking
            int cleared = clearExistingBalloons(world, cfg);
            if (cleared > 0) {
                LOGGER.info("Cleared {} old balloons before spawning new wave", cleared);
            }

            spawnBalloons(world, cfg);
            spawnForPlayers(world, cfg);

            if (cfg.announce) {
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("🎈 A new wave of balloon crates has appeared!"), false);
            }
        }
    }

    private static int clearExistingBalloons(ServerLevel world, BalloonConfigState cfg) {
        int cleared = 0;

        // Create a large search box around the spawn area
        // Search wider than spawn area in case balloons drifted
        int searchRadius = cfg.radius + 100;
        AABB searchBox = new AABB(
                cfg.center.getX() - searchRadius, 0, cfg.center.getZ() - searchRadius,
                cfg.center.getX() + searchRadius, 320, cfg.center.getZ() + searchRadius
        );

        // Find and remove all balloon entities
        List<BalloonEntity> balloons = world.getEntities(
                EntityTypeTest.forClass(BalloonEntity.class),
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

    private static void spawnBalloons(ServerLevel world, BalloonConfigState cfg) {
        LOGGER.info("Spawning {} balloons for week {}", cfg.perDay, lastSpawnWeek);

        for (int i = 0; i < cfg.perDay; i++) {
            int dx = world.random.nextInt(cfg.radius * 2) - cfg.radius;
            int dz = world.random.nextInt(cfg.radius * 2) - cfg.radius;

            int x = cfg.center.getX() + dx;
            int z = cfg.center.getZ() + dz;

            int yChosen = cfg.minY + world.random.nextInt(Math.max(1, (cfg.maxY - cfg.minY + 1)));
            int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            int y = Math.max(yChosen, groundY + 10);

            var balloon = new BalloonEntity(world, x + 0.5, y + 0.5, z + 0.5);
            world.addFreshEntity(balloon);
        }
    }

    /**
     * One balloon each for everybody online, alongside the wave over the area.
     *
     * <p>Part of the wave rather than a thing of its own, so it keeps the wave's timing and there is
     * no second schedule to reason about or to stop somebody farming. Off until a server turns it on.
     */
    private static void spawnForPlayers(ServerLevel world, BalloonConfigState cfg) {
        if (!cfg.perPlayer) return;

        for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
            if (player.serverLevel() != world) continue; // the wave is an overworld thing

            if (cfg.playerInAreaOnly) {
                double dx = player.getX() - cfg.center.getX();
                double dz = player.getZ() - cfg.center.getZ();
                if (dx * dx + dz * dz > (double) cfg.radius * cfg.radius) continue;
            }
            spawnNear(world, player, cfg);
        }
    }

    /** One balloon, above and a little to the side, clear of whatever the player is standing under. */
    private static void spawnNear(ServerLevel world, ServerPlayer player, BalloonConfigState cfg) {
        int spread = Math.max(1, cfg.playerSpread);
        int x = (int) player.getX() + world.random.nextInt(spread * 2 + 1) - spread;
        int z = (int) player.getZ() + world.random.nextInt(spread * 2 + 1) - spread;

        int ground = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        // Above the player and above the ground, so it is not left inside a roof or a hillside.
        int y = Math.max((int) player.getY(), ground) + Math.max(5, cfg.playerHeight);
        y = Math.min(y, world.getMaxBuildHeight() - 2);

        world.addFreshEntity(new BalloonEntity(world, x + 0.5, y + 0.5, z + 0.5));
        LOGGER.debug("Gave {} a balloon at {} {} {}", player.getName().getString(), x, y, z);
    }

    // ----- Admin helpers (write to persistent state) -----
    public static void setArea(ServerLevel world, BlockPos center, int radius) {
        var cfg = BalloonConfigState.get(world);
        cfg.center = center;
        cfg.configured = true;
        cfg.radius = Math.max(1, radius);
        cfg.setDirty();
    }
    public static void setYRange(ServerLevel world, int min, int max) {
        var cfg = BalloonConfigState.get(world);
        cfg.minY = Math.max(5, Math.min(min, max));
        cfg.maxY = Math.max(cfg.minY + 5, Math.max(min, max));
        cfg.setDirty();
    }
    public static void setCount(ServerLevel world, int perWave) {
        var cfg = BalloonConfigState.get(world);
        cfg.perDay = Math.max(0, perWave);
        cfg.setDirty();
    }
    public static void setPerPlayer(ServerLevel world, boolean enabled) {
        var cfg = BalloonConfigState.get(world);
        cfg.perPlayer = enabled;
        cfg.setDirty();
    }
    public static void setPlayerInAreaOnly(ServerLevel world, boolean areaOnly) {
        var cfg = BalloonConfigState.get(world);
        cfg.playerInAreaOnly = areaOnly;
        cfg.setDirty();
    }
    public static void setPlayerHeight(ServerLevel world, int height, int spread) {
        var cfg = BalloonConfigState.get(world);
        cfg.playerHeight = Math.max(5, height);
        cfg.playerSpread = Math.max(1, spread);
        cfg.setDirty();
    }

    public static void setAnnouncements(ServerLevel world, boolean enabled) {
        var cfg = BalloonConfigState.get(world);
        cfg.announce = enabled;
        cfg.setDirty();
    }

    public static void forceSpawn(ServerLevel world) {
        BalloonConfigState cfg = BalloonConfigState.get(world);
        int cleared = clearExistingBalloons(world, cfg);
        if (cleared > 0) {
            LOGGER.info("Force spawn: cleared {} old balloons", cleared);
        }
        spawnBalloons(world, cfg);
        spawnForPlayers(world, cfg);
    }
}