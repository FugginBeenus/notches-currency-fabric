package net.fugginbeenus.notchcurrency.client.npc;

import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class NotchNpcBipedRenderer extends LivingEntityRenderer<NotchNpcEntity, PlayerModel<NotchNpcEntity>> {

    // Two copies of the model. "live" rides the vanilla player layer, so CEM animation packs (Fresh
    // Animations / Fresh Moves via OptiFine or EMF) animate it: the life we want on nearby NPCs.
    // "frozen" rides our private layer (NpcModelLayers), which those packs don't replace; render() sends
    // an NPC there when a pack must not touch it, either because it's posed as a Statue or because it's
    // too far away to be worth animating. Without a pack installed the two layers are identical.
    private final NpcPlayerModel live;
    private final NpcPlayerModel liveSlim;
    private final NpcPlayerModel frozen;
    private final NpcPlayerModel frozenSlim;
    private static final double DETAIL_RANGE_SQ = 20.0 * 20.0;
    private static final double ANIM_RANGE_SQ = 28.0 * 28.0;
    private static final double LABEL_RANGE_SQ = 32.0 * 32.0;

    public NotchNpcBipedRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NpcPlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.live = (NpcPlayerModel) this.model; // the model handed to super() just above
        this.liveSlim = new NpcPlayerModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.frozen = new NpcPlayerModel(ctx.bakeLayer(NpcModelLayers.NPC_PLAYER), false);
        this.frozenSlim = new NpcPlayerModel(ctx.bakeLayer(NpcModelLayers.NPC_PLAYER_SLIM), true);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public void render(NotchNpcEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light) {
        // Negative means "close enough to skip every cut". That's how previews opt out (see lodApplies).
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
        // Sitting/Chilling are applied inside NpcPlayerModel.setupAnim (they need pivot drops, and
        // the renderer overwrites model.riding). Sneaking here; sleeping/prone via EntityPose.
        this.model.crouching = entity.isCrouching();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        // After the body, so the sign shows even on an NPC with its nameplate turned off.
        String[] sign = NpcBillboard.lines(entity);
        double y = entity.getNameOffset() + NpcBillboard.BASE_GAP;
        for (String line : sign) {
            if (!line.isBlank()) {
                matrices.pushPose();
                matrices.translate(0.0, y, 0.0);
                //? if >=1.21 {
                /*super.renderNameTag(entity, net.minecraft.network.chat.Component.literal(line), matrices,
                        vertexConsumers, light, tickDelta);
                *///?} else {
                super.renderNameTag(entity, net.minecraft.network.chat.Component.literal(line), matrices,
                        vertexConsumers, light);
                //?}
                matrices.popPose();
            }
            y += NpcBillboard.LINE_HEIGHT;
        }
    }

    @Override
    //? if >=1.21 {
    /*protected void setupRotations(NotchNpcEntity entity, PoseStack matrices, float animationProgress,
                                   float yBodyRot, float tickDelta, float scale) {
    *///?} else {
    protected void setupRotations(NotchNpcEntity entity, PoseStack matrices, float animationProgress,
                                   float yBodyRot, float tickDelta) {
    //?}
        if (entity.getNpcPose() == NotchNpcEntity.POSE_PRONE) {
            // Replicate vanilla's swimming/crawling transform (face-down, flat on the ground).
            matrices.mulPose(Axis.YN.rotationDegrees(yBodyRot));
            matrices.mulPose(Axis.XP.rotationDegrees(-90.0f));
            matrices.translate(0.0f, -1.0f, 0.3f); // vanilla swim offset: sits the crawl on the ground
            return;
        }
        //? if >=1.21 {
        /*super.setupRotations(entity, matrices, animationProgress, yBodyRot, tickDelta, scale);
        *///?} else {
        super.setupRotations(entity, matrices, animationProgress, yBodyRot, tickDelta);
        //?}
    }

    @Override
    protected void scale(NotchNpcEntity entity, PoseStack matrices, float amount) {
        // Scale only the model here (not the name label, which is rendered separately).
        float sx = entity.getScale(), sy = entity.getScaleY(), sz = entity.getScaleZ();
        if (sx != 1.0f || sy != 1.0f || sz != 1.0f) matrices.scale(sx, sy, sz);
    }

    @Override
    //? if >=1.21 {
    /*protected void renderNameTag(NotchNpcEntity entity, net.minecraft.network.chat.Component text,
                                       PoseStack matrices, MultiBufferSource vertexConsumers,
                                       int light, float tickDelta) {
    *///?} else {
    protected void renderNameTag(NotchNpcEntity entity, net.minecraft.network.chat.Component text,
                                       PoseStack matrices, MultiBufferSource vertexConsumers,
                                       int light) {
    //?}
        matrices.pushPose();
        matrices.translate(0.0, entity.getNameOffset(), 0.0);
        //? if >=1.21 {
        /*super.renderNameTag(entity, text, matrices, vertexConsumers, light, tickDelta);
        *///?} else {
        super.renderNameTag(entity, text, matrices, vertexConsumers, light);
        //?}
        matrices.popPose();

        // The subtitle hangs below the name, where a job title belongs. Drawn through the same label
        // routine so it billboards, fades and backgrounds exactly like the name above it.
        String subtitle = entity.getSubtitle();
        if (!subtitle.isEmpty()) {
            matrices.pushPose();
            matrices.translate(0.0, entity.getNameOffset() - NpcBillboard.LINE_HEIGHT, 0.0);
            net.minecraft.network.chat.Component line = net.minecraft.network.chat.Component.literal(
                    net.fugginbeenus.notchcurrency.npc.NpcText.colorize(subtitle));
            //? if >=1.21 {
            /*super.renderNameTag(entity, line, matrices, vertexConsumers, light, tickDelta);
            *///?} else {
            super.renderNameTag(entity, line, matrices, vertexConsumers, light);
            //?}
            matrices.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NotchNpcEntity entity) {
        return NpcSkins.resolve(entity);
    }

    @Override
    protected boolean shouldShowName(NotchNpcEntity entity) {
        if (!entity.hasCustomName() || !entity.isCustomNameVisible() || entity.isInvisible()) return false;
        return !lodApplies() || distanceToCameraSq(entity) < LABEL_RANGE_SQ;
    }

    double distanceToCameraSq(NotchNpcEntity entity) {
        return this.entityRenderDispatcher.distanceToSqr(entity);
    }
    static boolean lodApplies() {
        return Minecraft.getInstance().screen == null;
    }
}
