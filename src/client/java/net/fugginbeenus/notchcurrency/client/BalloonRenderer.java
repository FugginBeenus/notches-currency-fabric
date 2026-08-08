package net.fugginbeenus.notchcurrency.client;

import com.mojang.blaze3d.vertex.PoseStack;
//? if <1.21.11 {
import com.mojang.blaze3d.vertex.VertexConsumer;
//?}
import com.mojang.math.Axis;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.minecraft.client.Minecraft;
//? if <1.21.11 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
//? if <1.21.11 {
import net.minecraft.client.renderer.RenderType;
//?}
//? if <1.21.11 {
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
//?}
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if <1.21.11 {
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
//?}
import net.minecraft.resources.ResourceLocation;

//? if >=1.21.11 {
/*public class BalloonRenderer extends EntityRenderer<BalloonEntity, BalloonRenderer.State> {

    // The balloon is drawn from its item model, so its state is one item plus the spin.
    public static class State extends net.minecraft.client.renderer.entity.state.EntityRenderState {
        public final net.minecraft.client.renderer.item.ItemStackRenderState item =
                new net.minecraft.client.renderer.item.ItemStackRenderState();
        public float spin;
    }
*///?} else {
public class BalloonRenderer extends EntityRenderer<BalloonEntity> {
//?}

    //? if <1.21.11 {
    // Point at the item model: assets/notchcurrency/models/item/balloon.json
    private static final ModelResourceLocation BALLOON_MODEL_ID =
            new ModelResourceLocation(NotchCurrency.id("balloon"), "inventory");
    //?}

    //? if >=1.21.11 {
    /*private final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;
    *///?}

    public BalloonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        //? if >=1.21.11 {
        /*this.itemModelResolver = ctx.getItemModelResolver();
        *///?}
    }

    // Nothing is drawn on the spot from 1.21.11. The balloon's model is resolved into an item
    // render state while the entity is here, and submitted for the drawing pass.
    //? if >=1.21.11 {
    /*@Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BalloonEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.spin = (entity.tickCount + partialTick) * 2.0f;
        itemModelResolver.updateForNonLiving(state.item,
                new net.minecraft.world.item.ItemStack(
                        net.fugginbeenus.notchcurrency.registry.ModItems.BALLOON),
                net.minecraft.world.item.ItemDisplayContext.FIXED, entity);
    }

    @Override
    public void submit(State state, PoseStack matrices,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.CameraRenderState camera) {
        matrices.pushPose();
        // Centre on the entity position, then the gentle spin. The model is already in block-space
        // units, so it is deliberately not scaled down the way an inventory item would be.
        matrices.translate(0.5, 0.0, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(state.spin));
        state.item.submit(matrices, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();
        super.submit(state, matrices, collector, camera);
    }
    *///?} else {
    @Override
    public void render(BalloonEntity entity, float yaw, float tickDelta,
                       PoseStack matrices, MultiBufferSource consumers, int light) {

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher brm = mc.getBlockRenderer();
        BakedModel model = mc.getModelManager().getModel(BALLOON_MODEL_ID);

        matrices.pushPose();

        // Center on entity position
        matrices.translate(0.5, 0.0, 0.5);

        // Gentle spin
        float rotation = (entity.tickCount + tickDelta) * 2.0f;
        matrices.mulPose(Axis.YP.rotationDegrees(rotation));

        // IMPORTANT: no 1/16 scaling – your model is already in block-space units

        VertexConsumer vc = consumers.getBuffer(RenderType.cutout());
        brm.getModelRenderer().renderModel(
                matrices.last(),
                vc,
                null,
                model,
                1.0f, 1.0f, 1.0f,
                light,
                OverlayTexture.NO_OVERLAY
        );

        matrices.popPose();

        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    //?}

    //? if <1.21.11 {
    @Override
    public ResourceLocation getTextureLocation(BalloonEntity entity) {
        // Using the block atlas because the model pulls from notchcurrency:block/balloon
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
    //?}
}
