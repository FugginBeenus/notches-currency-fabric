package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;


import java.util.List;

public class GoldenCacheBlock extends Block {

    public GoldenCacheBlock(Settings settings) {
        super(settings);
    }

    @Override
    //? if >=1.21 {
    /*public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    *///?} else {
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    //?}
        if (!world.isClient) {
            ServerWorld sw = (ServerWorld) world;

            // --- 1) Magical sounds ---
            sw.playSound(
                    null, pos,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
                    SoundCategory.BLOCKS,
                    1.0f, 1.4f
            );
            sw.playSound(
                    null, pos,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.BLOCKS,
                    0.8f, 1.6f
            );

            // --- 2) Magical burst of particles ---
            for (int i = 0; i < 40; i++) {
                double ox = pos.getX() + 0.5 + (sw.random.nextDouble() - 0.5) * 1.5;
                double oy = pos.getY() + 0.5 + sw.random.nextDouble() * 1.5;
                double oz = pos.getZ() + 0.5 + (sw.random.nextDouble() - 0.5) * 1.5;

                double vx = (sw.random.nextDouble() - 0.5) * 0.1;
                double vy = sw.random.nextDouble() * 0.2;
                double vz = (sw.random.nextDouble() - 0.5) * 0.1;

                // bright spark
                sw.spawnParticles(ParticleTypes.END_ROD, ox, oy, oz, 1, vx, vy, vz, 0.0);

                // sprinkle a few enchant particles
                if (i % 3 == 0) {
                    sw.spawnParticles(ParticleTypes.ENCHANT, ox, oy, oz, 2, 0.0, 0.05, 0.0, 0.0);
                }
            }

            // --- 3) Loot from loot table (with fallback) ---
            //? if >=1.21 {
            /*LootTable table = sw.getServer().getReloadableRegistries().getLootTable(
                    net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.LOOT_TABLE, GoldenCacheManager.LOOT));
            *///?} else {
            LootTable table = sw.getServer().getLootManager().getLootTable(GoldenCacheManager.LOOT);
            //?}
            LootContextParameterSet ctx = new LootContextParameterSet.Builder(sw)
                    .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                    .add(LootContextParameters.THIS_ENTITY, player)
                    .build(LootContextTypes.CHEST);

            List<ItemStack> loot = table.generateLoot(ctx);

            // Fallback: just in case datapack is missing or empty
            if (loot.isEmpty()) {
                loot.add(new ItemStack(ModItems.NOTCH_COIN, 10));
            }

            for (ItemStack stack : loot) {
                if (stack.isEmpty()) continue;

                double cx = pos.getX() + 0.5;
                double cy = pos.getY() + 0.8;
                double cz = pos.getZ() + 0.5;

                ItemEntity item = new ItemEntity(sw, cx, cy, cz, stack.copy());

                double vx = (sw.random.nextDouble() - 0.5) * 0.2;
                double vy = 0.3 + sw.random.nextDouble() * 0.15;
                double vz = (sw.random.nextDouble() - 0.5) * 0.2;

                item.setVelocity(vx, vy, vz);
                item.setPickupDelay(20);
                sw.spawnEntity(item);
            }
        }

        // Let vanilla handle actually removing the block, etc.
        //? if >=1.21 {
        /*return super.onBreak(world, pos, state, player);
        *///?} else {
        super.onBreak(world, pos, state, player);
        //?}
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // 10% chance each tick to spawn a "firefly"
        if (random.nextFloat() < 0.10f) {
            // Spawn point around the top of the block
            double radius = 0.4;
            double angle = random.nextDouble() * Math.PI * 2.0;

            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            // Very gentle upward drift
            double vx = 0.0;
            double vy = 0.01 + random.nextDouble() * 0.01;
            double vz = 0.0;

            // Main "firefly" glow
            world.addParticle(
                    net.minecraft.particle.ParticleTypes.END_ROD,
                    x, y, z,
                    vx, vy, vz
            );

            // Tiny chance to add a second, dimmer one nearby for variety
            if (random.nextFloat() < 0.25f) {
                double x2 = x + (random.nextDouble() - 0.5) * 0.2;
                double y2 = y + (random.nextDouble() - 0.5) * 0.2;
                double z2 = z + (random.nextDouble() - 0.5) * 0.2;

                world.addParticle(
                        net.minecraft.particle.ParticleTypes.GLOW,
                        x2, y2, z2,
                        0.0, 0.005, 0.0
                );
            }
        }
    }
}
