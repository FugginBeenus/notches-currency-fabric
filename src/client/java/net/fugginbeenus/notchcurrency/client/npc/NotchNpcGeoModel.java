package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NotchNpcGeoModel extends GeoModel<NotchNpcEntity> {

    private static final ResourceLocation MODEL = NotchCurrency.id("geo/notch_npc.geo.json");
    private static final ResourceLocation ANIMATION = NotchCurrency.id("animations/notch_npc.animation.json");

    @Override
    public ResourceLocation getModelResource(NotchNpcEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NotchNpcEntity animatable) {
        // For the APP.ly model, the skin value is the themed variant id.
        return NpcAppearances.texture(animatable.getSkinValue());
    }

    @Override
    public ResourceLocation getAnimationResource(NotchNpcEntity animatable) {
        return ANIMATION;
    }
}
