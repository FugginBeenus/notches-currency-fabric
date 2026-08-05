package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.util.math.RotationAxis;

/**
 * Vanilla-biped renderer for the default "humanoid" Notch NPC model. Built on the real
 * {@link PlayerEntityModel}, so it gets proper walk/idle limb animation, skin overlay layers, and
 * (via feature renderers) worn armor and held items — with preset / player-name / URL skins from
 * {@link NpcSkins}, the slim variant, and the entity's scale.
 */
public class NotchNpcBipedRenderer extends LivingEntityRenderer<NotchNpcEntity, PlayerEntityModel<NotchNpcEntity>> {

    // Two copies of the model. "live" rides the vanilla player layer, so CEM animation packs (Fresh
    // Animations / Fresh Moves via OptiFine or EMF) animate it — the life we want on nearby NPCs.
    // "frozen" rides our private layer (NpcModelLayers), which those packs don't replace; render() sends
    // an NPC there when a pack must not touch it, either because it's posed as a Statue or because it's
    // too far away to be worth animating. Without a pack installed the two layers are identical.
    private final NpcPlayerModel live;
    private final NpcPlayerModel liveSlim;
    private final NpcPlayerModel frozen;
    private final NpcPlayerModel frozenSlim;

    /** Past this, drop the skin's overlay layers (see {@link NpcPlayerModel#setOverlaysVisible}). */
    private static final double DETAIL_RANGE_SQ = 20.0 * 20.0;
    /** Past this, render on the private layer so animation packs stop animating the NPC. A CEM pack
     *  re-evaluates its animations per part per entity per frame, which is the single most expensive
     *  thing about a crowd of NPCs when one is installed — and at this range nobody can read the
     *  difference. Set beyond the overlay cut so the two changes don't pop at the same moment. */
    private static final double ANIM_RANGE_SQ = 28.0 * 28.0;
    /** Past this, skip the floating name. Vanilla caps at 64 blocks, but names are per-entity text that
     *  batches poorly, and NPCs come in crowds where vanilla mobs come alone — so we cap tighter. */
    private static final double LABEL_RANGE_SQ = 32.0 * 32.0;

    public NotchNpcBipedRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new NpcPlayerModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.live = (NpcPlayerModel) this.model; // the model handed to super() just above
        this.liveSlim = new NpcPlayerModel(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true);
        this.frozen = new NpcPlayerModel(ctx.getPart(NpcModelLayers.NPC_PLAYER), false);
        this.frozenSlim = new NpcPlayerModel(ctx.getPart(NpcModelLayers.NPC_PLAYER_SLIM), true);
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
    }

    @Override
    public void render(NotchNpcEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // Negative means "close enough to skip every cut" — that's how previews opt out (see lodApplies).
        double distSq = lodApplies() ? distanceToCameraSq(entity) : -1.0;

        // The private layer is for anything an animation pack must not touch: Statue always, and any NPC
        // far enough that its animation can't be read. Nearby non-statue NPCs stay on the vanilla player
        // layer so packs keep bringing them to life. With no pack installed the two layers are built from
        // identical model data, so this switch is invisible and costs nothing either way.
        boolean packFree = entity.getPoseAnim() == NotchNpcEntity.ANIM_STATUE
                || (distSq >= 0 && distSq >= ANIM_RANGE_SQ);
        boolean slim = entity.isSlim();
        NpcPlayerModel m = packFree
                ? (slim ? this.frozenSlim : this.frozen)
                : (slim ? this.liveSlim : this.live);
        m.setOverlaysVisible(distSq < 0 || distSq < DETAIL_RANGE_SQ);
        this.model = m;
        // Sitting/Chilling are applied inside NpcPlayerModel.setAngles (they need pivot drops, and
        // the renderer overwrites model.riding). Sneaking here; sleeping/prone via EntityPose.
        this.model.sneaking = entity.isInSneakingPose();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        // After the body, so the sign shows even on an NPC with its nameplate turned off.
        String[] sign = NpcBillboard.lines(entity);
        double y = entity.getNameOffset() + NpcBillboard.BASE_GAP;
        for (String line : sign) {
            if (!line.isBlank()) {
                matrices.push();
                matrices.translate(0.0, y, 0.0);
                //? if >=1.21 {
                /*super.renderLabelIfPresent(entity, net.minecraft.text.Text.literal(line), matrices,
                        vertexConsumers, light, tickDelta);
                *///?} else {
                super.renderLabelIfPresent(entity, net.minecraft.text.Text.literal(line), matrices,
                        vertexConsumers, light);
                //?}
                matrices.pop();
            }
            y += NpcBillboard.LINE_HEIGHT;
        }
    }

    @Override
    //? if >=1.21 {
    /*protected void setupTransforms(NotchNpcEntity entity, MatrixStack matrices, float animationProgress,
                                   float bodyYaw, float tickDelta, float scale) {
    *///?} else {
    protected void setupTransforms(NotchNpcEntity entity, MatrixStack matrices, float animationProgress,
                                   float bodyYaw, float tickDelta) {
    //?}
        if (entity.getNpcPose() == NotchNpcEntity.POSE_PRONE) {
            // Replicate vanilla's swimming/crawling transform (face-down, flat on the ground).
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(bodyYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
            matrices.translate(0.0f, -1.0f, 0.3f); // vanilla swim offset — sits the crawl on the ground
            return;
        }
        //? if >=1.21 {
        /*super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
        *///?} else {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
        //?}
    }

    @Override
    protected void scale(NotchNpcEntity entity, MatrixStack matrices, float amount) {
        // Scale only the model here (not the name label, which is rendered separately).
        float sx = entity.getScale(), sy = entity.getScaleY(), sz = entity.getScaleZ();
        if (sx != 1.0f || sy != 1.0f || sz != 1.0f) matrices.scale(sx, sy, sz);
    }

    @Override
    //? if >=1.21 {
    /*protected void renderLabelIfPresent(NotchNpcEntity entity, net.minecraft.text.Text text,
                                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                       int light, float tickDelta) {
    *///?} else {
    protected void renderLabelIfPresent(NotchNpcEntity entity, net.minecraft.text.Text text,
                                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                       int light) {
    //?}
        matrices.push();
        matrices.translate(0.0, entity.getNameOffset(), 0.0);
        //? if >=1.21 {
        /*super.renderLabelIfPresent(entity, text, matrices, vertexConsumers, light, tickDelta);
        *///?} else {
        super.renderLabelIfPresent(entity, text, matrices, vertexConsumers, light);
        //?}
        matrices.pop();
    }

    @Override
    public Identifier getTexture(NotchNpcEntity entity) {
        return NpcSkins.resolve(entity);
    }

    @Override
    protected boolean hasLabel(NotchNpcEntity entity) {
        if (!entity.hasCustomName() || !entity.isCustomNameVisible() || entity.isInvisible()) return false;
        return !lodApplies() || distanceToCameraSq(entity) < LABEL_RANGE_SQ;
    }

    double distanceToCameraSq(NotchNpcEntity entity) {
        return this.dispatcher.getSquaredDistanceToCamera(entity);
    }

    /**
     * Whether distance-based trimming should run at all. The editor, pose editor and model picker all
     * draw NPCs through this renderer while a screen is open — and the picker's previews sit at a dummy
     * position far from the camera, which would strip their overlay layers and names. The crowds we're
     * optimizing for are always the in-world case, so LOD is limited to it.
     */
    static boolean lodApplies() {
        return MinecraftClient.getInstance().currentScreen == null;
    }
}
