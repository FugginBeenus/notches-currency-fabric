package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class GoldenCacheBlock extends Block {

    public GoldenCacheBlock(Properties settings) {
        super(settings);
    }

    @Override
    //? if >=1.21 {
    /*public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    *///?} else {
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    //?}
        if (!world.isClientSide) {
            ServerLevel sw = (ServerLevel) world;
            sw.playSound(
                    null, pos,
                    SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.BLOCKS,
                    1.0f, 1.4f
            );
            sw.playSound(
                    null, pos,
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS,
                    0.8f, 1.6f
            );

            for (int i = 0; i < 40; i++) {
                double ox = pos.getX() + 0.5 + (sw.random.nextDouble() - 0.5) * 1.5;
                double oy = pos.getY() + 0.5 + sw.random.nextDouble() * 1.5;
                double oz = pos.getZ() + 0.5 + (sw.random.nextDouble() - 0.5) * 1.5;

                double vx = (sw.random.nextDouble() - 0.5) * 0.1;
                double vy = sw.random.nextDouble() * 0.2;
                double vz = (sw.random.nextDouble() - 0.5) * 0.1;

                sw.sendParticles(ParticleTypes.END_ROD, ox, oy, oz, 1, vx, vy, vz, 0.0);

                if (i % 3 == 0) {
                    sw.sendParticles(ParticleTypes.ENCHANT, ox, oy, oz, 2, 0.0, 0.05, 0.0, 0.0);
                }
            }

            //? if >=1.21 {
            /*LootTable table = sw.getServer().reloadableRegistries().getLootTable(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, GoldenCacheManager.LOOT));
            *///?} else {
            LootTable table = sw.getServer().getLootData().getLootTable(GoldenCacheManager.LOOT);
            //?}
            LootParams ctx = new LootParams.Builder(sw)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .create(LootContextParamSets.CHEST);

            List<ItemStack> loot = table.getRandomItems(ctx);

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

                item.setDeltaMovement(vx, vy, vz);
                item.setPickUpDelay(20);
                sw.addFreshEntity(item);
            }
        }

        if (world instanceof net.minecraft.server.level.ServerLevel serverWorld) {
            GoldenCacheManager.noteOpened(serverWorld, pos);
        }

        //? if >=1.21 {
        /*return super.playerWillDestroy(world, pos, state, player);
        *///?} else {
        super.playerWillDestroy(world, pos, state, player);
        //?}
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.10f) {
            double radius = 0.4;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            double vx = 0.0;
            double vy = 0.01 + random.nextDouble() * 0.01;
            double vz = 0.0;
            world.addParticle(
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    x, y, z,
                    vx, vy, vz
            );
            if (random.nextFloat() < 0.25f) {
                double x2 = x + (random.nextDouble() - 0.5) * 0.2;
                double y2 = y + (random.nextDouble() - 0.5) * 0.2;
                double z2 = z + (random.nextDouble() - 0.5) * 0.2;

                world.addParticle(
                        net.minecraft.core.particles.ParticleTypes.GLOW,
                        x2, y2, z2,
                        0.0, 0.005, 0.0
                );
            }
        }
    }
}
