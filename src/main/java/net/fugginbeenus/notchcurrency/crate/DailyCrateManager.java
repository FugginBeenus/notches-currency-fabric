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
    private static final long WINDOW_START = 1000;
    private static final long WINDOW_END   = 2000;
    private static final long TICKS_PER_WEEK = 24000L * 7L;
    private static boolean warnedUnconfigured = false;

    private static void tick(MinecraftServer server) {
        ServerLevel world = server.overworld();
        if (world == null) return;

        BalloonConfigState cfg = BalloonConfigState.get(world);

        long totalTime = world.getGameTime();
        long week = totalTime / TICKS_PER_WEEK;
        long timeInWeek = totalTime % TICKS_PER_WEEK;

        if (week != lastSpawnWeek && timeInWeek >= WINDOW_START && timeInWeek <= WINDOW_END) {
            lastSpawnWeek = week;

            if (!cfg.configured) {
                if (!warnedUnconfigured) {
                    warnedUnconfigured = true;
                    LOGGER.info("Balloon crates are idle: no area set. Use /balloon setArea to pick one.");
                }
                return;
            }

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
        int searchRadius = cfg.radius + 100;
        AABB searchBox = new AABB(
                cfg.center.getX() - searchRadius, 0, cfg.center.getZ() - searchRadius,
                cfg.center.getX() + searchRadius, 320, cfg.center.getZ() + searchRadius
        );

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

    public static void applyToWorld(net.minecraft.server.MinecraftServer server,
                                    net.fugginbeenus.notchcurrency.config.NotchConfig cfg) {
        if (server == null) return;
        ServerLevel world = server.overworld();
        if (world == null) return;

        var b = cfg.balloon;
        BalloonConfigState state = BalloonConfigState.get(world);
        state.configured = b.enabled;
        state.center = new BlockPos(b.centerX, b.centerY, b.centerZ);
        state.radius = Math.max(1, b.radius);
        state.minY = b.minY;
        state.maxY = Math.max(b.minY, b.maxY);
        state.perDay = Math.max(0, b.perDay);
        state.announce = b.announce;
        state.perPlayer = b.perPlayer;
        state.playerInAreaOnly = b.playerInAreaOnly;
        state.playerHeight = Math.max(5, b.playerHeight);
        state.playerSpread = Math.max(1, b.playerSpread);
        state.setDirty();
    }

    public static void sendTo(net.minecraft.server.level.ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) return;
        var cfg = new net.fugginbeenus.notchcurrency.config.NotchConfig();
        readFromWorld(server, cfg);

        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        BalloonConfigWire.write(buf, cfg);
        net.fugginbeenus.notchcurrency.compat.Net.sendToClient(
                player, net.fugginbeenus.notchcurrency.net.NotchPackets.BALLOON_CONFIG_SYNC, buf);
    }

    public static void readFromWorld(net.minecraft.server.MinecraftServer server,
                                     net.fugginbeenus.notchcurrency.config.NotchConfig cfg) {
        if (server == null) return;
        ServerLevel world = server.overworld();
        if (world == null) return;

        BalloonConfigState state = BalloonConfigState.get(world);
        var b = cfg.balloon;
        b.enabled = state.configured;
        b.centerX = state.center.getX();
        b.centerY = state.center.getY();
        b.centerZ = state.center.getZ();
        b.radius = state.radius;
        b.minY = state.minY;
        b.maxY = state.maxY;
        b.perDay = state.perDay;
        b.announce = state.announce;
        b.perPlayer = state.perPlayer;
        b.playerInAreaOnly = state.playerInAreaOnly;
        b.playerHeight = state.playerHeight;
        b.playerSpread = state.playerSpread;
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

    private static void spawnForPlayers(ServerLevel world, BalloonConfigState cfg) {
        if (!cfg.perPlayer) return;

        for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
            if (player.serverLevel() != world) continue;

            if (cfg.playerInAreaOnly) {
                double dx = player.getX() - cfg.center.getX();
                double dz = player.getZ() - cfg.center.getZ();
                if (dx * dx + dz * dz > (double) cfg.radius * cfg.radius) continue;
            }
            spawnNear(world, player, cfg);
        }
    }

    private static void spawnNear(ServerLevel world, ServerPlayer player, BalloonConfigState cfg) {
        int up = Math.max(5, cfg.playerHeight);
        int spread = Math.max(1, cfg.playerSpread);
        int y = Math.min((int) player.getY() + up, world.getMaxBuildHeight() - 2);

        int x = (int) player.getX() + world.random.nextInt(spread * 2 + 1) - spread;
        int z = (int) player.getZ() + world.random.nextInt(spread * 2 + 1) - spread;
        if (tryPlace(world, player, x, y, z)) return;
        tryPlace(world, player, (int) player.getX(), y, (int) player.getZ());
    }

    private static boolean tryPlace(ServerLevel world, ServerPlayer player, int x, int y, int z) {
        BalloonEntity balloon = new BalloonEntity(world, x + 0.5, y + 0.5, z + 0.5);
        if (!world.noCollision(balloon)) return false;
        world.addFreshEntity(balloon);
        LOGGER.debug("Gave {} a balloon at {} {} {}", player.getName().getString(), x, y, z);
        return true;
    }

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