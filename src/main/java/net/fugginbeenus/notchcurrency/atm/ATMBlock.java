package net.fugginbeenus.notchcurrency.atm;

import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ATMBlock extends HorizontalDirectionalBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(
            1.0, 0.0, 2.0,
            15.0, 16.0, 14.0
    );

    public ATMBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state,
                                      BlockGetter world,
                                      BlockPos pos,
                                      CollisionContext context) {
        return SHAPE;
    }

    //? if >=1.21 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.HorizontalDirectionalBlock> codec() {
        return simpleCodec(ATMBlock::new);
    }
    *///?}

    @Override
    //? if >=1.21 {
    /*protected InteractionResult useWithoutItem(BlockState state,
                                 Level world,
                                 BlockPos pos,
                                 Player player,
                                 BlockHitResult hit) {
    *///?} else {
    public InteractionResult use(BlockState state,
                              Level world,
                              BlockPos pos,
                              Player player,
                              InteractionHand hand,
                              BlockHitResult hit) {
    //?}

        if (player instanceof ServerPlayer spe) {
            long bal = net.fugginbeenus.notchcurrency.core.BalanceStore.get(spe);
            net.fugginbeenus.notchcurrency.net.NotchPackets.sendBalance(spe, bal);
        }

        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        MenuProvider factory = new SimpleMenuProvider(
                (containerId, playerInv, p) -> createHandler(containerId, playerInv),
                Component.translatable("screen.notchcurrency.deposit")
        );

        player.openMenu(factory);
        return InteractionResult.CONSUME;
    }

    private static ATMTestScreenHandler createHandler(int containerId, Inventory playerInv) {
        return new ATMTestScreenHandler(containerId, playerInv);
    }
}
