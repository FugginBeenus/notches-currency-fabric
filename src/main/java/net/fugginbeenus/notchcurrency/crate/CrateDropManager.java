package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class CrateDropManager {
    private CrateDropManager() {}

    private static final Map<UUID, List<ItemStack>> TRACKED = new HashMap<>();
    private static boolean INIT = false;

    public static void init() {
        if (INIT) return;
        INIT = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Early exit if nothing is tracked
            if (TRACKED.isEmpty()) {
                return;
            }

            for (ServerWorld world : server.getWorlds()) {
                // Copy keys to avoid CME
                List<UUID> ids = new ArrayList<>(TRACKED.keySet());
                for (UUID id : ids) {
                    Entity e = world.getEntity(id);
                    if (!(e instanceof FallingBlockEntity falling)) {
                        // Not in this world, or already gone
                        continue;
                    }

                    // Landed or placed block?
                    if (falling.isOnGround() || falling.verticalCollision || !falling.isAlive()) {
                        List<ItemStack> loot = TRACKED.remove(id);
                        BlockPos pos = falling.getBlockPos();

                        // Visual "block break" effect (barrel)
                        world.syncWorldEvent(2001, pos, Block.getRawIdFromState(Blocks.BARREL.getDefaultState()));
                        world.playSound(null, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 0.9f, 1.0f);

                        // Ensure no barrel block stays behind
                        if (world.getBlockState(pos).isOf(Blocks.BARREL)) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                        }

                        // Little poof
                        world.spawnParticles(
                                ParticleTypes.POOF,
                                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                                10, 0.2, 0.1, 0.2, 0.02
                        );

                        // Spill loot (if any)
                        if (loot != null && !loot.isEmpty()) {
                            spill(world, pos, loot);
                        }

                        // Remove the falling entity
                        falling.discard();
                    }
                }
            }
        });
    }

    public static void track(FallingBlockEntity falling, List<ItemStack> loot) {
        if (falling == null || falling.getUuid() == null) return;

        // Copy stacks so nobody mutates later
        List<ItemStack> copy = new ArrayList<>();
        if (loot != null) for (ItemStack s : loot) if (s != null && !s.isEmpty()) copy.add(s.copy());

        TRACKED.put(falling.getUuid(), copy);
        // Don’t drop the barrel block item itself
        falling.dropItem = false;
    }

    private static void spill(ServerWorld world, BlockPos pos, List<ItemStack> loot) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        for (ItemStack s : loot) {
            if (!s.isEmpty()) ItemScatterer.spawn(world, x, y, z, s);
        }
    }
}