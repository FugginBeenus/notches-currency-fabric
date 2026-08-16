package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.block.entity.MailboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MailboxBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    public static final BooleanProperty FLAG = BooleanProperty.create("flag");
    private static final VoxelShape POST = Block.box(6.5, 0.0, 6.5, 9.5, 11.0, 9.5);
    private static final VoxelShape FLOOR_NS = Shapes.or(POST,
            Block.box(4.7, 10.5, 2.5, 11.3, 16.0, 13.5));
    private static final VoxelShape FLOOR_EW = Shapes.or(POST,
            Block.box(2.5, 10.5, 4.7, 13.5, 16.0, 11.3));

    private static final VoxelShape SHAPE_WALL_NORTH = Block.box(4.7, 5.0, 12.0, 11.3, 12.0, 16.0);
    private static final VoxelShape SHAPE_WALL_SOUTH = Block.box(4.7, 5.0, 0.0, 11.3, 12.0, 4.0);
    private static final VoxelShape SHAPE_WALL_WEST = Block.box(12.0, 5.0, 4.7, 16.0, 12.0, 11.3);
    private static final VoxelShape SHAPE_WALL_EAST = Block.box(0.0, 5.0, 4.7, 4.0, 12.0, 11.3);

    public MailboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WALL, false)
                .setValue(FLAG, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL, FLAG);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clicked = ctx.getClickedFace();
        if (clicked.getAxis().isHorizontal()) {
            return defaultBlockState()
                    .setValue(WALL, true)
                    .setValue(FACING, clicked);
        }
        return defaultBlockState()
                .setValue(WALL, false)
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (!state.getValue(WALL)) {
            return state.getValue(FACING).getAxis() == Direction.Axis.X ? FLOOR_EW : FLOOR_NS;
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_WALL_SOUTH;
            case WEST -> SHAPE_WALL_WEST;
            case EAST -> SHAPE_WALL_EAST;
            default -> SHAPE_WALL_NORTH;
        };
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MailboxBlockEntity(pos, state);
    }

    @Override
    //? if >=1.21 {
    /*protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
    *///?} else {
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
    //?}
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        if (!(world.getBlockEntity(pos) instanceof MailboxBlockEntity box)) {
            return InteractionResult.CONSUME;
        }

        if (!box.isClaimed()) {
            box.claim(sp.getUUID(), sp.getName().getString());
            net.fugginbeenus.notchcurrency.mail.MailState.get(sp.level().getServer())
                    .noteMailbox(sp.getUUID(), sp.getName().getString());
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp,
                    Component.literal("This mailbox is yours now.").withStyle(ChatFormatting.GREEN));

            int waiting = net.fugginbeenus.notchcurrency.mail.MailState
                    .get(sp.level().getServer()).count(sp.getUUID());
            if (waiting > 0) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(
                                waiting + (waiting == 1 ? " parcel was" : " parcels were")
                                        + " waiting for you.")
                        .withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.CONSUME;
        }

        if (box.isOwner(sp.getUUID())) {
            net.fugginbeenus.notchcurrency.mail.MailManager.openInbox(sp);
            return InteractionResult.CONSUME;
        }

        net.fugginbeenus.notchcurrency.mail.MailManager.openPost(sp, box.owner());
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos) {
        if (!state.getValue(WALL)) return super.canSurvive(state, world, pos);
        Direction behind = state.getValue(FACING).getOpposite();
        BlockPos wall = pos.relative(behind);
        return world.getBlockState(wall).isFaceSturdy(world, wall, behind.getOpposite());
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
                                  net.minecraft.world.level.LevelAccessor world, BlockPos pos,
                                  BlockPos neighborPos) {
    //?}
        if (state.getValue(WALL) && !canSurvive(state, world, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        //? if >=1.21.11 {
        /*return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
        *///?} else {
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
        //?}
    }

    @Override
    //? if >=1.21 {
    /*public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    *///?} else {
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
    //?}
        if (!world.isClientSide && player instanceof ServerPlayer sp
                && world.getBlockEntity(pos) instanceof MailboxBlockEntity box
                && box.isOwner(sp.getUUID())) {
            int waiting = net.fugginbeenus.notchcurrency.mail.MailState
                    .get(sp.level().getServer()).count(sp.getUUID());
            if (waiting > 0) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(
                                waiting + (waiting == 1 ? " parcel is" : " parcels are")
                                        + " still yours. Put another mailbox down to reach them.")
                        .withStyle(ChatFormatting.GREEN));
            }
        }
        //? if >=1.21 {
        /*return super.playerWillDestroy(world, pos, state, player);
        *///?} else {
        super.playerWillDestroy(world, pos, state, player);
        //?}
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (world.isClientSide
                || type != net.fugginbeenus.notchcurrency.registry.ModBlockEntities.MAILBOX) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<MailboxBlockEntity>) MailboxBlockEntity::serverTick;
    }
}
