package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BountyBoardBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape LOWER_NS = Shapes.or(
            Block.box(2, 0, 6.5, 4, 16, 9.5),
            Block.box(12, 0, 6.5, 14, 16, 9.5),
            Block.box(1, 6, 6.75, 15, 16, 9.25));
    private static final VoxelShape LOWER_EW = Shapes.or(
            Block.box(6.5, 0, 2, 9.5, 16, 4),
            Block.box(6.5, 0, 12, 9.5, 16, 14),
            Block.box(6.75, 6, 1, 9.25, 16, 15));
    private static final VoxelShape UPPER_NS = Block.box(0.5, 0, 6.25, 15.5, 16, 9.75);
    private static final VoxelShape UPPER_EW = Block.box(6.25, 0, 0.5, 9.75, 16, 15.5);

    public BountyBoardBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        boolean ns = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? (ns ? LOWER_NS : LOWER_EW)
                : (ns ? UPPER_NS : UPPER_EW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level world = ctx.getLevel();
        if (pos.getY() < world.getMaxBuildHeight() - 1 && world.getBlockState(pos.above()).canBeReplaced(ctx)) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide && world instanceof ServerLevel sw) {
            BountyManager.ensurePopulated(sw.getServer());
        }
    }

    //? if >=1.21.11 {
    /*@Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader world,
                                     net.minecraft.world.level.ScheduledTickAccess tickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, net.minecraft.util.RandomSource random) {
    *///?} else {
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                                LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
    //?}
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighborState.is(this) && neighborState.getValue(HALF) != half
                    ? state : Blocks.AIR.defaultBlockState();
        }
        //? if >=1.21.11 {
        /*return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
        *///?} else {
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
        //?}
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = world.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return super.canSurvive(state, world, pos);
    }

    @Override
    //? if >=1.21 {
    /*public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    *///?} else {
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    //?}
        if (!world.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState belowState = world.getBlockState(below);
            if (belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                world.setBlock(below, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                world.levelEvent(player, 2001, below, Block.getId(belowState));
            }
        }
        //? if >=1.21 {
        /*return super.playerWillDestroy(world, pos, state, player);
        *///?} else {
        super.playerWillDestroy(world, pos, state, player);
        //?}
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    //? if >=1.21 {
    /*protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
    *///?} else {
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                              Player player, InteractionHand hand, BlockHitResult hit) {
    //?}
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            BountyManager.openScreen(sp);
        }
        return InteractionResult.CONSUME;
    }
}
