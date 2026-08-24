package net.fugginbeenus.notchcurrency.client.crate;

import net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CrateGeoModel extends GeoModel<CrateBlockEntity> {

    //? if >=1.21.11 {
    /*private static final ResourceLocation MODEL = NotchCurrency.id("crate");
    private static final ResourceLocation ANIMATION = NotchCurrency.id("crate");
    *///?} else {
    private static final ResourceLocation MODEL = NotchCurrency.id("geo/crate.geo.json");
    private static final ResourceLocation ANIMATION = NotchCurrency.id("animations/crate.animation.json");
    //?}

    private final ResourceLocation texture;

    public CrateGeoModel(String tier) {
        this.texture = NotchCurrency.id("textures/block/crate/" + tier + ".png");
    }

    //? if >=1.21.11 {
    /*@Override
    public ResourceLocation getModelResource(software.bernie.geckolib.renderer.base.GeoRenderState state) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(software.bernie.geckolib.renderer.base.GeoRenderState state) {
        return texture;
    }
    *///?} else {
    @Override
    public ResourceLocation getModelResource(CrateBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CrateBlockEntity animatable) {
        return texture;
    }
    //?}

    @Override
    public ResourceLocation getAnimationResource(CrateBlockEntity animatable) {
        return ANIMATION;
    }
}
