package net.fugginbeenus.notchcurrency.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
//? if <1.21.11 {
import com.mojang.blaze3d.vertex.VertexConsumer;
//?}
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
//? if <1.21.11 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.entity.EntityRendererProvider;
//? if >=1.21.11 {
/*import software.bernie.geckolib.cache.model.BakedGeoModel;
*///?} else {
import software.bernie.geckolib.cache.object.BakedGeoModel;
//?}
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// GeckoLib 5 names the render state as a second type argument, the same shape vanilla adopted.
//? if >=1.21.11 {
/*public class NotchNpcGeoRenderer extends GeoEntityRenderer<NotchNpcEntity,
        net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {
*///?} else {
public class NotchNpcGeoRenderer extends GeoEntityRenderer<NotchNpcEntity> {
//?}

    public NotchNpcGeoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NotchNpcGeoModel());
        this.shadowRadius = 0.4f;
    }

    // 5.x replaced preRender with a scaling hook that is handed the whole pass, so the NPC's own
    // scale is read off the render state there instead of poking the pose stack directly.
    //? if >=1.21.11 {
    /*@Override
    public void scaleModelForRender(
            software.bernie.geckolib.renderer.base.RenderPassInfo<
                    net.minecraft.client.renderer.entity.state.LivingEntityRenderState> pass,
            float widthScale, float heightScale) {
        float scale = NotchNpcRenderState.of(pass.renderState()).npcScale();
        if (scale <= 0f) scale = 1.0f;
        super.scaleModelForRender(pass, widthScale * scale, heightScale * scale);
    }
    *///?} else {
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
        float scale = animatable.npcScale();
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
    //?}
}
