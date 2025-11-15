package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class BarrelCleanupManager {
    private BarrelCleanupManager() {}

    // Track per-dimension
    private static final Map<ServerWorld, Set<BlockPos>> TRACKED = new HashMap<>();
    private static boolean INIT = false;

    public static void init() {
        if (INIT) return;
        INIT = true;
        ServerTickEvents.END_SERVER_TICK.register(BarrelCleanupManager::tick);
    }

    public static void track(ServerWorld world, BlockPos pos) {
        TRACKED.computeIfAbsent(world, w -> new HashSet<>()).add(pos.toImmutable());
    }

    private static void tick(MinecraftServer server) {
        if (TRACKED.isEmpty()) return;

        for (var entry : new ArrayList<>(TRACKED.entrySet())) {
            ServerWorld world = entry.getKey();
            Set<BlockPos> positions = entry.getValue();
            if (positions.isEmpty()) continue;

            var toRemove = new ArrayList<BlockPos>();
            for (BlockPos pos : positions) {
                // If block changed, stop tracking
                if (!world.getBlockState(pos).isOf(Blocks.BARREL)) {
                    toRemove.add(pos);
                    continue;
                }
                var be = world.getBlockEntity(pos);
                if (!(be instanceof BarrelBlockEntity barrel)) continue;

                // If empty -> poof and remove the barrel
                if (isEmpty(barrel)) {
                    world.syncWorldEvent(2001, pos, net.minecraft.block.Block.getRawIdFromState(Blocks.BARREL.getDefaultState())); // break particles
                    world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 0.9f, 1.0f);
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
                    world.spawnParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                            10, 0.2, 0.1, 0.2, 0.02);
                    toRemove.add(pos);
                }
            }
            positions.removeAll(toRemove);
            if (positions.isEmpty()) TRACKED.remove(world);
        }
    }

    private static boolean isEmpty(BarrelBlockEntity barrel) {
        for (int i = 0; i < barrel.size(); i++) {
            if (!barrel.getStack(i).isEmpty()) return false;
        }
        return true;
    }
}
