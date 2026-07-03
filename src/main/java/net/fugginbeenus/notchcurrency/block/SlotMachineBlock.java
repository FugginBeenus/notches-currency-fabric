package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A slot-machine gambling block. Right-click opens the slots screen. Placeholder model/texture.
 */
public class SlotMachineBlock extends Block {

    public SlotMachineBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld) {
            SlotMachineManager.openScreen(sp);
        }
        return ActionResult.CONSUME;
    }
}
