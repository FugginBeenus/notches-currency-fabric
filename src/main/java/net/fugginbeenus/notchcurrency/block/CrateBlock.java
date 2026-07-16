package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.crate.CrateManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A crate block of a fixed type. Right-click opens it (consuming keys); sneak-right-click shows
 * its loot odds. Faces the placer. On a successful open the lid pops up ({@code open=true}) and a
 * scheduled tick closes it again a moment later — a light vanilla "opening" animation to go with
 * the loot particles.
 */
public class CrateBlock extends Block {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;

    /** How long the lid stays up after opening (ticks). */
    private static final int OPEN_TICKS = 30;

    /** The oversized chest spills its block (~24 wide, 20 tall, 18 deep); the shape follows the
     *  model so selection/collision feel right. Wider than deep, so it swaps with facing. */
    private static final VoxelShape SHAPE_NS = Block.createCuboidShape(-4, 0, -1, 20, 20, 17);
    private static final VoxelShape SHAPE_EW = Block.createCuboidShape(-1, 0, -4, 17, 20, 20);

    private final String crateType;

    public CrateBlock(Settings settings, String crateType) {
        super(settings);
        this.crateType = crateType;
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH).with(OPEN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
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
        if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld sw) {
            if (player.isSneaking()) {
                CrateManager.showOdds(sp, crateType);
            } else {
                CrateManager.open(sp, crateType, sw, pos);
            }
        }
        return ActionResult.CONSUME;
    }

    /** Pop the lid open and schedule it to close. Called by CrateManager only when an open succeeds. */
    public static void animateOpen(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CrateBlock && !state.get(OPEN)) {
            world.setBlockState(pos, state.with(OPEN, true), Block.NOTIFY_LISTENERS);
            world.scheduleBlockTick(pos, state.getBlock(), OPEN_TICKS);
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(OPEN)) {
            world.setBlockState(pos, state.with(OPEN, false), Block.NOTIFY_LISTENERS);
        }
    }
}
