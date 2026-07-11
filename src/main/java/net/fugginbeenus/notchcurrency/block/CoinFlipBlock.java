package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A coin-flip gambling block. Right-click opens the betting screen (pick a side + wager); after the
 * player commits, the block enters {@link #FLIPPING} for a short spin and then settles on a
 * {@link #FACE}. Those two blockstates are the animation hook — a future model/animated texture can
 * read them without a block entity. While flipping, the block is "busy" and rejects new bets.
 */
public class CoinFlipBlock extends Block {

    public static final BooleanProperty FLIPPING = BooleanProperty.of("flipping");
    public static final EnumProperty<CoinFace> FACE = EnumProperty.of("face", CoinFace.class);

    public CoinFlipBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FLIPPING, false)
                .with(FACE, CoinFace.HEADS));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FLIPPING, FACE);
    }

    /** Fits the model (foot + column + tabletop + standing coin) so the shape hugs the podium. */
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(3, 0, 3, 13, 2, 13),
            Block.createCuboidShape(5, 2, 5, 11, 7, 11),
            Block.createCuboidShape(1, 7, 1, 15, 9, 15),
            Block.createCuboidShape(5.5, 9, 6.5, 10.5, 14, 9.5));

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld) {
            if (state.get(FLIPPING)) {
                CoinFlipManager.notifyBusy(sp);
            } else {
                CoinFlipManager.openScreen(sp, pos);
            }
        }
        return ActionResult.CONSUME;
    }
}
