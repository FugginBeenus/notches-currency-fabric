package net.fugginbeenus.notchcurrency.block;

import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * "Ledger Board" — a spawn-friendly block that shows the balance leaderboard on use.
 *
 * For now this prints the top balances to chat (same data as {@code /baltop}). The
 * placeholder model/texture and a future in-world rendered board can be swapped in
 * without touching this logic.
 */
public class LedgerBoardBlock extends Block {

    private static final int TOP_LIMIT = 10;

    public LedgerBoardBlock(Settings settings) {
        super(settings);
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
