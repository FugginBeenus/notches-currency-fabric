package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelBundle;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NotchNpcGeoModel extends GeoModel<NotchNpcEntity> {
    //? if >=1.21.11 {
    /*private static final ResourceLocation MODEL = NotchCurrency.id("notch_npc");
    *///?} else {
    private static final ResourceLocation MODEL = NotchCurrency.id("geo/notch_npc.geo.json");
    //?}

    private static final ResourceLocation ANIMATION =
            net.fugginbeenus.notchcurrency.compat.Geo.NPC_ANIMATIONS;

    private static ResourceLocation modelFor(String modelId) {
        NpcModelBundle bundle = NpcModelRegistry.forModelId(modelId);
        if (bundle == null) return MODEL;
        //? if >=1.21.11 {
        /*return NotchCurrency.id(bundle.assetName());
        *///?} else {
        return NotchCurrency.id("geo/" + bundle.assetName() + ".geo.json");
        //?}
    }

    private static ResourceLocation animationFor(String modelId) {
        NpcModelBundle bundle = NpcModelRegistry.forModelId(modelId);
        if (bundle == null || bundle.idle().isEmpty()) return ANIMATION;
        //? if >=1.21.11 {
        /*return NotchCurrency.id(bundle.assetName());
        *///?} else {
        return NotchCurrency.id("animations/" + bundle.assetName() + ".animation.json");
        //?}
    }

    public static boolean ready(String modelId) {
        return net.fugginbeenus.notchcurrency.compat.Geo.hasBakedModel(modelFor(modelId))
                && net.fugginbeenus.notchcurrency.compat.Geo.hasBakedAnimations(animationFor(modelId));
    }

    private static ResourceLocation textureFor(String modelId, String skinValue) {
        NpcModelBundle bundle = NpcModelRegistry.forModelId(modelId);
        if (bundle == null) return NpcAppearances.texture(skinValue);
        return NotchCurrency.id("textures/entity/" + bundle.assetName() + ".png");
    }
    //? if >=1.21.11 {
    /*@Override
    public ResourceLocation getModelResource(software.bernie.geckolib.renderer.base.GeoRenderState state) {
        return modelFor(NotchNpcRenderState.of(
                (net.minecraft.client.renderer.entity.state.EntityRenderState) state).modelId);
    }

    @Override
    public ResourceLocation getTextureResource(software.bernie.geckolib.renderer.base.GeoRenderState state) {
        NotchNpcRenderState npc = NotchNpcRenderState.of(
                (net.minecraft.client.renderer.entity.state.EntityRenderState) state);
        return textureFor(npc.modelId, npc.skinValue);
    }

    @Override
    public ResourceLocation getAnimationResource(NotchNpcEntity animatable) {
        return animationFor(animatable.getModelId());
    }
    *///?} else {
    @Override
    public ResourceLocation getModelResource(NotchNpcEntity animatable) {
        return modelFor(animatable.getModelId());
    }

    @Override
    public ResourceLocation getTextureResource(NotchNpcEntity animatable) {
        return textureFor(animatable.getModelId(), animatable.getSkinValue());
    }

    @Override
    public ResourceLocation getAnimationResource(NotchNpcEntity animatable) {
        return animationFor(animatable.getModelId());
    }
    //?}
}
