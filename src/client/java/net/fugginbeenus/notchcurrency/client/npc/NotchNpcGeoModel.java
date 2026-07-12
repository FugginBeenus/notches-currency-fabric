package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model resolver for the Notch NPC. Phase 1 uses a single placeholder humanoid rig +
 * texture; Phase 2 will resolve model/texture through the appearance/model-provider system.
 */
public class NotchNpcGeoModel extends GeoModel<NotchNpcEntity> {

    private static final Identifier MODEL = NotchCurrency.id("geo/notch_npc.geo.json");
    private static final Identifier ANIMATION = NotchCurrency.id("animations/notch_npc.animation.json");

    @Override
    public Identifier getModelResource(NotchNpcEntity animatable) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(NotchNpcEntity animatable) {
        // For the APP.ly model, the skin value is the themed variant id.
        return NpcAppearances.texture(animatable.getSkinValue());
    }

    @Override
    public Identifier getAnimationResource(NotchNpcEntity animatable) {
        return ANIMATION;
    }
}
