package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A coin-flip gambling block. Right-click opens the betting screen (pick a side + wager); after the
 * player commits, the block enters {@link #FLIPPING} for a short spin and then settles on a
 * {@link #FACE}. Those two blockstates are the animation hook — a future model/animated texture can
 * read them without a block entity. While flipping, the block is "busy" and rejects new bets.
 */
public class CoinFlipBlock extends Block {

    public static final BooleanProperty FLIPPING = BooleanProperty.of("flipping");
    public static final EnumProperty<CoinFace> FACE = EnumProperty.of("face", CoinFace.class);

    public CoinFlipBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FLIPPING, false)
                .with(FACE, CoinFace.HEADS));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FLIPPING, FACE);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity sp && world instanceof ServerWorld) {
            if (state.get(FLIPPING)) {
                CoinFlipManager.notifyBusy(sp);
            } else {
                CoinFlipManager.openScreen(sp, pos);
            }
        }
        return ActionResult.CONSUME;
    }
}
