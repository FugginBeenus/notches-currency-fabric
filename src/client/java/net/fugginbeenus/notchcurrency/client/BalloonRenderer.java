package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class BalloonRenderer extends EntityRenderer<BalloonEntity> {

    // Point at the item model: assets/notchcurrency/models/item/balloon.json
    private static final ModelIdentifier BALLOON_MODEL_ID =
            new ModelIdentifier(NotchCurrency.id("balloon"), "inventory");

    public BalloonRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(BalloonEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {

        MinecraftClient mc = MinecraftClient.getInstance();
        BlockRenderManager brm = mc.getBlockRenderManager();
        BakedModel model = mc.getBakedModelManager().getModel(BALLOON_MODEL_ID);

        matrices.push();

        // Center on entity position
        matrices.translate(0.5, 0.0, 0.5);

        // Gentle spin
        float rotation = (entity.age + tickDelta) * 2.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));

        // IMPORTANT: no 1/16 scaling – your model is already in block-space units

        VertexConsumer vc = consumers.getBuffer(RenderLayer.getCutout());
        brm.getModelRenderer().render(
                matrices.peek(),
                vc,
                null,
                model,
                1.0f, 1.0f, 1.0f,
                light,
                OverlayTexture.DEFAULT_UV
        );

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    @Override
    public Identifier getTexture(BalloonEntity entity) {
        // Using the block atlas because the model pulls from notchcurrency:block/balloon
        return net.minecraft.client.texture.SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
