package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity;
import net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
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

public class CoinFlipBlock extends Block implements BlockEntityProvider {

    public static final BooleanProperty FLIPPING = BooleanProperty.of("flipping");
    public static final EnumProperty<CoinFace> FACE = EnumProperty.of("face", CoinFace.class);

    public CoinFlipBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FLIPPING, false)
                .with(FACE, CoinFace.HEADS));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CoinFlipBlockEntity(pos, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FLIPPING, FACE);
    }

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(-3, 0, -3, 2, 12, 2),
            Block.createCuboidShape(14, 0, -3, 19, 12, 2),
            Block.createCuboidShape(-3, 0, 14, 2, 12, 19),
            Block.createCuboidShape(14, 0, 14, 19, 12, 19),
            Block.createCuboidShape(-2, 9, -2, 18, 15, 18),
            Block.createCuboidShape(3, 15, 3, 13, 24, 13));

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    //? if >=1.21 {
    /*protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
    *///?} else {
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
    //?}
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
