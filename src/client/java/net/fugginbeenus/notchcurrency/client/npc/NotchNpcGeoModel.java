package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NotchNpcGeoModel extends GeoModel<NotchNpcEntity> {

    // GeckoLib 5 scans geckolib/models and geckolib/animations, and keys what it finds by the path
    // with that directory and the extension stripped off. 4.x wanted the full path under geo/.
    //? if >=1.21.11 {
    /*private static final ResourceLocation MODEL = NotchCurrency.id("notch_npc");
    *///?} else {
    private static final ResourceLocation MODEL = NotchCurrency.id("geo/notch_npc.geo.json");
    //?}

    // Shared with the clip lookup: two spellings of the same file would mean the editor listing
    // clips from one place while the NPC played them from another.
    private static final ResourceLocation ANIMATION =
            net.fugginbeenus.notchcurrency.compat.Geo.NPC_ANIMATIONS;

    // 5.x asks the model about the render state rather than the animatable, so the themed variant
    // is read off the NPC data riding on that state.
    //? if >=1.21.11 {
    /*@Override
    public ResourceLocation getModelResource(software.bernie.geckolib.renderer.base.GeoRenderState state) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(software.bernie.geckolib.renderer.base.GeoRenderState state) {
        return NpcAppearances.texture(NotchNpcRenderState.of(
                (net.minecraft.client.renderer.entity.state.EntityRenderState) state).skinValue);
    }
    *///?} else {
    @Override
    public ResourceLocation getModelResource(NotchNpcEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NotchNpcEntity animatable) {
        // For the APP.ly model, the skin value is the themed variant id.
        return NpcAppearances.texture(animatable.getSkinValue());
    }
    //?}

    @Override
    public ResourceLocation getAnimationResource(NotchNpcEntity animatable) {
        return ANIMATION;
    }
}
