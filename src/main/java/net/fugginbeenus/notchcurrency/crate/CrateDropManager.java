package net.fugginbeenus.notchcurrency.crate;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

            for (ServerLevel world : server.getAllLevels()) {
                // Copy keys to avoid CME
                List<UUID> ids = new ArrayList<>(TRACKED.keySet());
                for (UUID id : ids) {
                    Entity e = world.getEntity(id);
                    if (!(e instanceof FallingBlockEntity falling)) {
                        // Not in this world, or already gone
                        continue;
                    }

                    // Landed or placed block?
                    if (falling.onGround() || falling.verticalCollision || !falling.isAlive()) {
                        List<ItemStack> loot = TRACKED.remove(id);
                        BlockPos pos = falling.blockPosition();

                        // Visual "block break" effect (barrel)
                        world.levelEvent(2001, pos, Block.getId(Blocks.BARREL.defaultBlockState()));
                        world.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.9f, 1.0f);

                        // Ensure no barrel block stays behind
                        if (world.getBlockState(pos).is(Blocks.BARREL)) {
                            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }

                        // Little poof
                        world.sendParticles(
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
        if (falling == null || falling.getUUID() == null) return;

        // Copy stacks so nobody mutates later
        List<ItemStack> copy = new ArrayList<>();
        if (loot != null) for (ItemStack s : loot) if (s != null && !s.isEmpty()) copy.add(s.copy());

        TRACKED.put(falling.getUUID(), copy);
        // Don’t drop the barrel block item itself
        falling.dropItem = false;
    }

    private static void spill(ServerLevel world, BlockPos pos, List<ItemStack> loot) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        for (ItemStack s : loot) {
            if (!s.isEmpty()) Containers.dropItemStack(world, x, y, z, s);
        }
    }
}