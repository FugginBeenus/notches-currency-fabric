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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The mailbox: one block that stands on the ground or hangs on a wall, and puts its flag up when
 * the owner has something waiting.
 *
 * <p>Which of the two it is comes from where it was placed rather than from a second block, so the
 * player only ever has one item and never has to pick. The flag is a blockstate rather than
 * something the renderer works out, so it costs nothing to draw and is visible from across a street.
 */
public class MailboxBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** True when hung on the side of something, false when standing on the ground. */
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    /** The flag, up when the owner has mail. */
    public static final BooleanProperty FLAG = BooleanProperty.create("flag");

    private static final VoxelShape SHAPE_FLOOR = Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0);
    private static final VoxelShape SHAPE_WALL_NORTH = Block.box(4.0, 2.0, 8.0, 12.0, 13.0, 16.0);
    private static final VoxelShape SHAPE_WALL_SOUTH = Block.box(4.0, 2.0, 0.0, 12.0, 13.0, 8.0);
    private static final VoxelShape SHAPE_WALL_WEST = Block.box(8.0, 2.0, 4.0, 16.0, 13.0, 12.0);
    private static final VoxelShape SHAPE_WALL_EAST = Block.box(0.0, 2.0, 4.0, 8.0, 13.0, 12.0);

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
        // Clicked the side of something: hang off it, front pointing away from the wall. Anything
        // else, including the top of a block, stands it on the ground facing the player.
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
        if (!state.getValue(WALL)) return SHAPE_FLOOR;
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

        // An unclaimed box belongs to whoever opens it first, which is how a player gets one at all.
        if (!box.isClaimed()) {
            box.claim(sp.getUUID(), sp.getName().getString());
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp,
                    Component.literal("This mailbox is yours now.").withStyle(ChatFormatting.GREEN));
            return InteractionResult.CONSUME;
        }

        if (box.isOwner(sp.getUUID())) {
            net.fugginbeenus.notchcurrency.mail.MailSweep.run(sp.level().getServer());
            int taken = net.fugginbeenus.notchcurrency.mail.MailManager.collectAll(sp);
            if (taken == 0) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp,
                        Component.literal("Your mailbox is empty.").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.CONSUME;
        }

        // Someone else's box. Posting to it is what the screen will offer; for now say whose it is.
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp,
                Component.literal(box.ownerName().isEmpty() ? "This mailbox belongs to someone else."
                                : "This mailbox belongs to " + box.ownerName() + ".")
                        .withStyle(ChatFormatting.YELLOW));
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (world.isClientSide
                || type != net.fugginbeenus.notchcurrency.registry.ModBlockEntities.MAILBOX) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<MailboxBlockEntity>) MailboxBlockEntity::serverTick;
    }
}
