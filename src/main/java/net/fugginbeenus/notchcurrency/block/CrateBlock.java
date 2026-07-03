package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.crate.CrateManager;
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
 * A crate block of a fixed type. Right-click opens it (consuming keys); sneak-right-click shows
 * its loot odds. Placeholder model/texture for now.
 */
public class CrateBlock extends Block {

    private final String crateType;

    public CrateBlock(Settings settings, String crateType) {
        super(settings);
        this.crateType = crateType;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
            if (player.isSneaking()) {
                CrateManager.showOdds(sp, crateType);
            } else {
                CrateManager.open(sp, crateType, sw, pos);
            }
        }
        return ActionResult.CONSUME;
    }
}
