package net.fugginbeenus.notchcurrency.atm;

import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class ATMBlock extends HorizontalFacingBlock {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    // Shape for the ATM (from your other class)
    private static final VoxelShape SHAPE = Block.createCuboidShape(
            1.0, 0.0, 2.0,   // minX, minY, minZ
            15.0, 16.0, 14.0 // maxX, maxY, maxZ
    );

    public ATMBlock(Settings settings) {
        super(settings);
        this.setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    // --- Blockstate / facing ---

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Face the player when placed
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state,
                                      BlockView world,
                                      BlockPos pos,
                                      ShapeContext context) {
        return SHAPE;
    }

    // --- Interaction / GUI ---

    @Override
    public ActionResult onUse(BlockState state,
                              World world,
                              BlockPos pos,
                              PlayerEntity player,
                              Hand hand,
                              BlockHitResult hit) {

        // Server: sync balance to HUD immediately
        if (player instanceof ServerPlayerEntity spe) {
            long bal = net.fugginbeenus.notchcurrency.core.BalanceStore.get(spe);
            net.fugginbeenus.notchcurrency.net.NotchPackets.sendBalance(spe, bal);
        }

        if (world.isClient) {
            // client side: hand animation & let server handle screen opening
            return ActionResult.SUCCESS;
        }

        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> createHandler(syncId, playerInv),
                Text.translatable("screen.notchcurrency.deposit")
        );

        player.openHandledScreen(factory);
        return ActionResult.CONSUME; // server handled it
    }

    private static ATMTestScreenHandler createHandler(int syncId, PlayerInventory playerInv) {
        return new ATMTestScreenHandler(syncId, playerInv);
    }
}
