package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for the "apply" model path — the animated APP.ly humanoid, with its themed skins
 * (resolved by {@link NotchNpcGeoModel}) and the entity's scale. Used only when the model is set to
 * APP.ly and that mod is installed; otherwise the NPC renders as the default biped.
 */
public class NotchNpcGeoRenderer extends GeoEntityRenderer<NotchNpcEntity> {

    public NotchNpcGeoRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new NotchNpcGeoModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    public void preRender(MatrixStack poseStack, NotchNpcEntity animatable, BakedGeoModel model,
                          VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        float scale = animatable.getScale();
        if (!isReRender && scale > 0f && scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
