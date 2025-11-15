package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

// add these:
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;

public final class DailyCrateManager {
    private static long lastDay = -1;

    private DailyCrateManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(DailyCrateManager::tick);
    }

    // Admin-tweakable defaults (also persisted via BalloonConfigState)
    private static BlockPos CENTER = new BlockPos(0, 80, 0);
    private static int RADIUS = 25;

    public static int MIN_Y = 110;
    public static int MAX_Y = 150;

    private static int BALLOONS_PER_DAY = 3;
    public static boolean ANNOUNCE = true;

    private static final long WINDOW_START = 1000;
    private static final long WINDOW_END   = 2000;

    public static void setPerDay(int n) { BALLOONS_PER_DAY = Math.max(0, n); }

    private static void tick(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (world == null) return;

        BalloonConfigState cfg = BalloonConfigState.get(world);

        long day  = world.getTimeOfDay() / 24000L;
        long time = world.getTimeOfDay() % 24000L;

        if (day != lastDay && time >= WINDOW_START && time <= WINDOW_END) {
            lastDay = day;
            spawnBalloons(world, cfg);
            if (cfg.announce) {
                server.getPlayerManager().broadcast(Text.literal("🎈 Balloon crates have appeared in the Market District!"), false);
            }
        }
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
        b.perDay  = BALLOONS_PER_DAY;
        b.announce = ANNOUNCE;
    }

    private static void spawnBalloons(ServerWorld world, BalloonConfigState cfg) {
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
    public static void setCount(ServerWorld world, int perDay) {
        var cfg = BalloonConfigState.get(world);
        cfg.perDay = Math.max(0, perDay);
        cfg.markDirty();
    }
    public static void setAnnouncements(ServerWorld world, boolean enabled) {
        var cfg = BalloonConfigState.get(world);
        cfg.announce = enabled;
        cfg.markDirty();
    }
}
