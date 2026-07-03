package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * "Bounty Board" — a placeable block that shows the auto-generated bounties on use (the same
 * board reachable from a BOUNTY-role NPC). Placeholder model/texture for now.
 */
public class BountyBoardBlock extends Block {

    public BountyBoardBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && world instanceof ServerWorld sw) {
            BountyManager.ensurePopulated(sw.getServer()); // start generating right away
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity sp) {
            BountyManager.openScreen(sp);
        }
        return ActionResult.CONSUME;
    }
}
