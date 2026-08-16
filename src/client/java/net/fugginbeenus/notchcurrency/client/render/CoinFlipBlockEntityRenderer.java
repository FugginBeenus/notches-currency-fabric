package net.fugginbeenus.notchcurrency.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fugginbeenus.notchcurrency.block.CoinFace;
import net.fugginbeenus.notchcurrency.block.CoinFlipBlock;
import net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity;
import net.fugginbeenus.notchcurrency.compat.Render;
import net.fugginbeenus.notchcurrency.registry.ModItems;
//? if <26.1 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//? if <26.1 {
import net.minecraft.client.renderer.entity.ItemRenderer;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

//? if >=1.21.11 {
/*public class CoinFlipBlockEntityRenderer
        implements BlockEntityRenderer<CoinFlipBlockEntity, CoinFlipBlockEntityRenderer.State> {

    public static class State extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        public boolean draw;
        public float spin;
        public float arc;
        public final net.minecraft.client.renderer.item.ItemStackRenderState coin =
                new net.minecraft.client.renderer.item.ItemStackRenderState();
    }
*///?} else {
public class CoinFlipBlockEntityRenderer implements BlockEntityRenderer<CoinFlipBlockEntity> {
//?}

    private static final float TABLE_Y = 0.96f;
    private static final float PEAK = 0.7f;
    private static final float SIZE = 0.5f;
    private static final int FULL_BRIGHT = 0xF000F0;

    //? if >=1.21.11 {
    /*private final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;
    *///?} else {
    private final ItemRenderer itemRenderer;
    //?}

    public CoinFlipBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        //? if >=1.21.11 {
        /*this.itemModelResolver = ctx.itemModelResolver();
        *///?} else {
        this.itemRenderer = ctx.getItemRenderer();
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CoinFlipBlockEntity be, State out, float tickDelta,
                                   net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, out, tickDelta, cameraPos, crumbling);
        Level world = be.getLevel();
        var blockState = be.getBlockState();
        out.draw = world != null && blockState.getBlock() instanceof CoinFlipBlock;
        if (!out.draw) return;
        boolean flipping = blockState.getValue(CoinFlipBlock.FLIPPING);
        boolean tails = blockState.getValue(CoinFlipBlock.FACE) == CoinFace.TAILS;
        out.spin = 0f;
        out.arc = 0f;
        if (flipping && be.flipStartTick() >= 0) {
            float elapsed = (world.getGameTime() - be.flipStartTick()) + tickDelta;
            float t = Mth.clamp(elapsed / Math.max(1f, be.revealTicks()), 0f, 1f);
            out.arc = 4f * PEAK * t * (1f - t);   // parabola arc up + back down
            out.spin = elapsed * 52f;             // fast tumble on top of the flat rest
        }
        itemModelResolver.updateForTopItem(out.coin,
                new ItemStack(tails ? ModItems.COIN_TAILS : ModItems.NOTCH_COIN),
                net.minecraft.world.item.ItemDisplayContext.FIXED, world, null, 0);
    }

    @Override
    public void submit(State state, PoseStack matrices,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.CameraRenderState camera) {
        if (!state.draw) return;
        matrices.pushPose();
        matrices.translate(0.5, TABLE_Y + state.arc, 0.5);
        // Flat on the felt (90 degrees from the item's upright card) plus the flip spin, both around
        // X so it tumbles toward the front rather than spinning edge-on.
        matrices.mulPose(Axis.XP.rotationDegrees(90f + state.spin));
        matrices.scale(SIZE, SIZE, SIZE);
        // Full-bright: the coin floats above the block, so the block's baked light renders it black.
        state.coin.submit(matrices, collector, FULL_BRIGHT,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();
    }
    *///?} else {
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
        matrices.mulPose(Axis.XP.rotationDegrees(90f + spin));
        matrices.scale(SIZE, SIZE, SIZE);

        Render.renderFixedItem(itemRenderer, coin,
                FULL_BRIGHT, overlay,
                matrices, vertexConsumers, world, 0);
        matrices.popPose();
    }
    //?}
}
