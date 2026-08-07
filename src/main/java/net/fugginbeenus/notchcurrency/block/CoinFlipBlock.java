package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity;
import net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoinFlipBlock extends Block implements EntityBlock {

    public static final BooleanProperty FLIPPING = BooleanProperty.create("flipping");
    public static final EnumProperty<CoinFace> FACE = EnumProperty.create("face", CoinFace.class);

    public CoinFlipBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(FLIPPING, false)
                .setValue(FACE, CoinFace.HEADS));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoinFlipBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLIPPING, FACE);
    }

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(-3, 0, -3, 2, 12, 2),
            Block.box(14, 0, -3, 19, 12, 2),
            Block.box(-3, 0, 14, 2, 12, 19),
            Block.box(14, 0, 14, 19, 12, 19),
            Block.box(-2, 9, -2, 18, 15, 18),
            Block.box(3, 15, 3, 13, 24, 13));

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
            if (state.getValue(FLIPPING)) {
                CoinFlipManager.notifyBusy(sp);
            } else {
                CoinFlipManager.openScreen(sp, pos);
            }
        }
        return InteractionResult.CONSUME;
    }
}
