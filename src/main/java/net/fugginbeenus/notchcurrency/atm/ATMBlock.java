package net.fugginbeenus.notchcurrency.atm;

import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ATMBlock extends Block {

    public ATMBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state,
                              World world,
                              BlockPos pos,
                              PlayerEntity player,
                              Hand hand,
                              BlockHitResult hit) {

        if (world.isClient) {
            // client side: return success so the hand animates immediately
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
        // This ctor matches the one we wrote in ATMTestScreenHandler
        return new ATMTestScreenHandler(syncId, playerInv);
    }
}
