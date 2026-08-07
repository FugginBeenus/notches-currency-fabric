package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class SlotMachineBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(1, 0, 1, 15, 3, 15),
            Block.box(2, 3, 2, 14, 13, 14),
            Block.box(3, 13, 3, 13, 15.5, 13));

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter world, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    public SlotMachineBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    /*protected InteractionResult onUse(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
    *///?} else {
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                              Player player, InteractionHand hand, BlockHitResult hit) {
    //?}
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && world instanceof ServerLevel) {
            SlotMachineManager.openScreen(sp);
        }
        return InteractionResult.CONSUME;
    }
}
