package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelBundle;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Which geometry, texture and animations an NPC wears.
 *
 * <p>Was three constants when there was one model. Now it answers per NPC, because a custom bundle
 * writes its files under its own name and the NPC's model id says which one to reach for. An NPC on
 * the built-in model, or on a bundle that has since been deleted, falls back to the built-in files.
 */
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
        // A model with no animations of its own still has to name a file GeckoLib can find, or it
        // never draws at all. The built-in one is pointed at and simply never played from.
        if (bundle == null || bundle.idle().isEmpty()) return ANIMATION;
        //? if >=1.21.11 {
        /*return NotchCurrency.id(bundle.assetName());
        *///?} else {
        return NotchCurrency.id("animations/" + bundle.assetName() + ".animation.json");
        //?}
    }

    /**
     * Whether GeckoLib actually has what this NPC needs, right now.
     *
     * <p>False for a moment during a resource reload, and for a bundle that has been deleted. The
     * caller draws the NPC some other way until this comes back true, because asking GeckoLib for a
     * model whose animations are not loaded crashes the game rather than drawing nothing.
     */
    public static boolean ready(String modelId) {
        return net.fugginbeenus.notchcurrency.compat.Geo.hasBakedModel(modelFor(modelId))
                && net.fugginbeenus.notchcurrency.compat.Geo.hasBakedAnimations(animationFor(modelId));
    }

    private static ResourceLocation textureFor(String modelId, String skinValue) {
        NpcModelBundle bundle = NpcModelRegistry.forModelId(modelId);
        if (bundle == null) return NpcAppearances.texture(skinValue);
        return NotchCurrency.id("textures/entity/" + bundle.assetName() + ".png");
    }

    // 5.x asks the model about the render state rather than the animatable, so the themed variant
    // is read off the NPC data riding on that state.
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
        // For the APP.ly model, the skin value is the themed variant id.
        return textureFor(animatable.getModelId(), animatable.getSkinValue());
    }

    @Override
    public ResourceLocation getAnimationResource(NotchNpcEntity animatable) {
        return animationFor(animatable.getModelId());
    }
    //?}
}
