package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Vanilla-biped renderer for the default "humanoid" Notch NPC model. Built on the real
 * {@link PlayerEntityModel}, so it gets proper walk/idle limb animation, skin overlay layers, and
 * (via feature renderers) worn armor and held items — with preset / player-name / URL skins from
 * {@link NpcSkins}, the slim variant, and the entity's scale.
 */
public class NotchNpcBipedRenderer extends LivingEntityRenderer<NotchNpcEntity, PlayerEntityModel<NotchNpcEntity>> {

    private final PlayerEntityModel<NotchNpcEntity> normal;
    private final PlayerEntityModel<NotchNpcEntity> slim;

    public NotchNpcBipedRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new NpcPlayerModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.normal = this.model;
        this.slim = new NpcPlayerModel(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true);
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
    }

    @Override
    public void render(NotchNpcEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        this.model = entity.isSlim() ? this.slim : this.normal;
        // Sitting/Chilling are applied inside NpcPlayerModel.setAngles (they need pivot drops, and
        // the renderer overwrites model.riding). Sneaking here; sleeping/prone via EntityPose.
        this.model.sneaking = entity.isInSneakingPose();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    protected void scale(NotchNpcEntity entity, MatrixStack matrices, float amount) {
        // Scale only the model here (not the name label, which is rendered separately).
        float s = entity.getScale();
        if (s > 0f && s != 1.0f) matrices.scale(s, s, s);
    }

    @Override
    public Identifier getTexture(NotchNpcEntity entity) {
        return NpcSkins.resolve(entity);
    }

    @Override
    protected boolean hasLabel(NotchNpcEntity entity) {
        return entity.hasCustomName() && entity.isCustomNameVisible() && !entity.isInvisible();
    }
}
