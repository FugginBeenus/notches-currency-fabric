package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import java.util.*;

public final class BarrelCleanupManager {
    private BarrelCleanupManager() {}
    private static final Map<ServerLevel, Set<BlockPos>> TRACKED = new HashMap<>();
    private static boolean INIT = false;

    public static void init() {
        if (INIT) return;
        INIT = true;
        ServerTickEvents.END_SERVER_TICK.register(BarrelCleanupManager::tick);
    }

    public static void track(ServerLevel world, BlockPos pos) {
        TRACKED.computeIfAbsent(world, w -> new HashSet<>()).add(pos.immutable());
    }

    private static void tick(MinecraftServer server) {
        if (TRACKED.isEmpty()) return;

        for (var entry : new ArrayList<>(TRACKED.entrySet())) {
            ServerLevel world = entry.getKey();
            Set<BlockPos> positions = entry.getValue();
            if (positions.isEmpty()) continue;

            var toRemove = new ArrayList<BlockPos>();
            for (BlockPos pos : positions) {
                if (!world.getBlockState(pos).is(Blocks.BARREL)) {
                    toRemove.add(pos);
                    continue;
                }
                var be = world.getBlockEntity(pos);
                if (!(be instanceof BarrelBlockEntity barrel)) continue;
                if (isEmpty(barrel)) {
                    world.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(Blocks.BARREL.defaultBlockState()));
                    world.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.9f, 1.0f);
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    world.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                            10, 0.2, 0.1, 0.2, 0.02);
                    toRemove.add(pos);
                }
            }
            positions.removeAll(toRemove);
            if (positions.isEmpty()) TRACKED.remove(world);
        }
    }

    private static boolean isEmpty(BarrelBlockEntity barrel) {
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            if (!barrel.getItem(i).isEmpty()) return false;
        }
        return true;
    }
}
