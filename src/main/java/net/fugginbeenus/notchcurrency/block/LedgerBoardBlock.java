package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/**
 * "Ledger Board" — a two-block-tall leaderboard monument (door-style halves) that prints the top
 * balances to chat on use, from either half (same data as {@code /baltop}). The lower half carries
 * the whole visual (spans up past y=16); the upper is an invisible occupancy block. Faces the placer;
 * breaking either half removes both and only the lower drops.
 */
public class LedgerBoardBlock extends Block {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    private static final int TOP_LIMIT = 10;

    private static final VoxelShape LOWER_NS = Block.createCuboidShape(0.5, 0, 5, 15.5, 16, 11);
    private static final VoxelShape LOWER_EW = Block.createCuboidShape(5, 0, 0.5, 11, 16, 15.5);
    private static final VoxelShape UPPER_NS = Block.createCuboidShape(0.5, 0, 5, 15.5, 15, 11);
    private static final VoxelShape UPPER_EW = Block.createCuboidShape(5, 0, 0.5, 11, 15, 15.5);

    public LedgerBoardBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH).with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean ns = state.get(FACING).getAxis() == Direction.Axis.Z;
        return state.get(HALF) == DoubleBlockHalf.LOWER ? (ns ? LOWER_NS : LOWER_EW)
                : (ns ? UPPER_NS : UPPER_EW);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        if (pos.getY() < world.getTopY() - 1 && world.getBlockState(pos.up()).canReplace(ctx)) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
        }
        return null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighborState.isOf(this) && neighborState.get(HALF) != half
                    ? state : Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canPlaceAt(BlockState state, net.minecraft.world.WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = world.getBlockState(pos.down());
            return below.isOf(this) && below.get(HALF) == DoubleBlockHalf.LOWER;
        }
        return super.canPlaceAt(state, world, pos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && player.isCreative() && state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.down();
            BlockState belowState = world.getBlockState(below);
            if (belowState.isOf(this) && belowState.get(HALF) == DoubleBlockHalf.LOWER) {
                world.setBlockState(below, Blocks.AIR.getDefaultState(),
                        Block.NOTIFY_ALL | Block.SKIP_DROPS);
                world.syncWorldEvent(player, 2001, below, Block.getRawIdFromState(belowState));
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity sp) {
            for (Text line : EconomyLeaderboard.topLines(sp.getServer(), TOP_LIMIT)) {
                sp.sendMessage(line, false);
            }
        }
        return ActionResult.CONSUME;
    }
}
