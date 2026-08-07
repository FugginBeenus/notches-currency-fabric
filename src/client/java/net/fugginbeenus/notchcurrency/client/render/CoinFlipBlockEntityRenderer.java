package net.fugginbeenus.notchcurrency.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fugginbeenus.notchcurrency.block.CoinFace;
import net.fugginbeenus.notchcurrency.block.CoinFlipBlock;
import net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity;
import net.fugginbeenus.notchcurrency.compat.Render;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CoinFlipBlockEntityRenderer implements BlockEntityRenderer<CoinFlipBlockEntity> {

    private static final float TABLE_Y = 0.96f;  // felt surface height (block units)
    private static final float PEAK = 0.7f;      // arc peak height (blocks)
    private static final float SIZE = 0.5f;      // coin scale

    private final ItemRenderer itemRenderer;

    public CoinFlipBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(CoinFlipBlockEntity be, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = be.getLevel();
        if (world == null) return;
        var state = be.getBlockState();
        if (!(state.getBlock() instanceof CoinFlipBlock)) return;

        boolean flipping = state.getValue(CoinFlipBlock.FLIPPING);
        boolean tails = state.getValue(CoinFlipBlock.FACE) == CoinFace.TAILS;
        ItemStack coin = new ItemStack(tails ? ModItems.COIN_TAILS : ModItems.NOTCH_COIN);

        float spin = 0f;
        float arc = 0f;
        if (flipping && be.flipStartTick() >= 0) {
            float elapsed = (world.getGameTime() - be.flipStartTick()) + tickDelta;
            float t = Mth.clamp(elapsed / Math.max(1f, be.revealTicks()), 0f, 1f);
            arc = 4f * PEAK * t * (1f - t);   // parabola arc up + back down
            spin = elapsed * 52f;              // fast tumble on top of the flat rest
        }

        matrices.pushPose();
        matrices.translate(0.5, TABLE_Y + arc, 0.5);
        // Flat on the felt (90° from the item's upright card) + the flip spin, both around X so it
        // tumbles toward the front rather than spinning edge-on.
        matrices.mulPose(Axis.XP.rotationDegrees(90f + spin));
        matrices.scale(SIZE, SIZE, SIZE);

        // Full-bright: the coin floats above the block, so the block's baked light renders it black.
        Render.renderFixedItem(itemRenderer, coin,
                LightTexture.FULL_BRIGHT, overlay,
                matrices, vertexConsumers, world, 0);
        matrices.popPose();
    }
}
