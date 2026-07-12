package net.fugginbeenus.notchcurrency.client.render;

import net.fugginbeenus.notchcurrency.block.CoinFace;
import net.fugginbeenus.notchcurrency.block.CoinFlipBlock;
import net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

/**
 * Renders the coin-flip table's coin as the real notch coin item. Idle, it lies flat on the felt
 * showing the last-landed face; when a flip is running (per the block entity's start tick + reveal
 * duration) it arcs into the air and tumbles, then drops back to the felt as the FACE settles.
 */
public class CoinFlipBlockEntityRenderer implements BlockEntityRenderer<CoinFlipBlockEntity> {

    private static final float TABLE_Y = 0.96f;  // felt surface height (block units)
    private static final float PEAK = 0.7f;      // arc peak height (blocks)
    private static final float SIZE = 0.5f;      // coin scale

    private final ItemRenderer itemRenderer;

    public CoinFlipBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(CoinFlipBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = be.getWorld();
        if (world == null) return;
        var state = be.getCachedState();
        if (!(state.getBlock() instanceof CoinFlipBlock)) return;

        boolean flipping = state.get(CoinFlipBlock.FLIPPING);
        boolean tails = state.get(CoinFlipBlock.FACE) == CoinFace.TAILS;
        ItemStack coin = new ItemStack(tails ? ModItems.COIN_TAILS : ModItems.NOTCH_COIN);

        matrices.push();
        matrices.translate(0.5, TABLE_Y, 0.5);

        if (flipping && be.flipStartTick() >= 0) {
            float elapsed = (world.getTime() - be.flipStartTick()) + tickDelta;
            float dur = Math.max(1f, be.revealTicks());
            float t = MathHelper.clamp(elapsed / dur, 0f, 1f);
            matrices.translate(0.0, 4f * PEAK * t * (1f - t), 0.0);           // parabola arc up + back down
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(elapsed * 52f)); // fast tumble
        } else {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));  // lie flat, face up
        }
        matrices.scale(SIZE, SIZE, SIZE);

        itemRenderer.renderItem(coin, ModelTransformationMode.FIXED, light, overlay,
                matrices, vertexConsumers, world, 0);
        matrices.pop();
    }
}
