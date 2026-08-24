package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.crate.CrateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CrateBlock extends Block implements net.minecraft.world.level.block.EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private static final int OPEN_TICKS = 130;

    private static final VoxelShape SHAPE_NS = Block.box(-4, 0, -1, 20, 20, 17);
    private static final VoxelShape SHAPE_EW = Block.box(-1, 0, -4, 17, 20, 20);

    private final String crateType;

    public String crateType() { return crateType; }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity(
                net.fugginbeenus.notchcurrency.registry.ModBlockEntities.crateTypeFor(crateType), pos, state);
    }

    public CrateBlock(Properties settings, String crateType) {
        super(settings);
        this.crateType = crateType;
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
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
        if (player instanceof ServerPlayer sp && world instanceof ServerLevel sw) {
            if (player.isShiftKeyDown()) {
                CrateManager.showOdds(sp, crateType);
            } else {
                CrateManager.open(sp, crateType, sw, pos);
            }
        }
        return InteractionResult.CONSUME;
    }

    public static void animateOpen(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CrateBlock && !state.getValue(OPEN)) {
            world.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_CLIENTS);
            world.scheduleTick(pos, state.getBlock(), OPEN_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            world.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_CLIENTS);
        }
    }
}
