package net.fugginbeenus.notchcurrency.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class BalloonRenderer extends EntityRenderer<BalloonEntity> {

    // Point at the item model: assets/notchcurrency/models/item/balloon.json
    private static final ModelResourceLocation BALLOON_MODEL_ID =
            new ModelResourceLocation(NotchCurrency.id("balloon"), "inventory");

    public BalloonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

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

    @Override
    public ResourceLocation getTextureLocation(BalloonEntity entity) {
        // Using the block atlas because the model pulls from notchcurrency:block/balloon
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
