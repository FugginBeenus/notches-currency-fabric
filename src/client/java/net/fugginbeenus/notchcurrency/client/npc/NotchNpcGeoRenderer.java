package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NotchNpcGeoRenderer extends GeoEntityRenderer<NotchNpcEntity> {

    public NotchNpcGeoRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new NotchNpcGeoModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    //? if >=1.21 {
    /*public void preRender(MatrixStack poseStack, NotchNpcEntity animatable, BakedGeoModel model,
                          VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
    *///?} else {
    public void preRender(MatrixStack poseStack, NotchNpcEntity animatable, BakedGeoModel model,
                          VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
    //?}
        float scale = animatable.getScale();
        if (!isReRender && scale > 0f && scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }
        //? if >=1.21 {
        /*super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);
        *///?} else {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        //?}
    }
}
