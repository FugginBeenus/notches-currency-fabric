package net.fugginbeenus.notchcurrency.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NotchNpcGeoRenderer extends GeoEntityRenderer<NotchNpcEntity> {

    public NotchNpcGeoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NotchNpcGeoModel());
        this.shadowRadius = 0.4f;
    }

    @Override
    //? if >=1.21 {
    /*public void preRender(PoseStack poseStack, NotchNpcEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
    *///?} else {
    public void preRender(PoseStack poseStack, NotchNpcEntity animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
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
