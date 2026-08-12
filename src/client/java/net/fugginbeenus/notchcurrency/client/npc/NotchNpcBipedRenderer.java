package net.fugginbeenus.notchcurrency.client.npc;

import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
//? if >=1.21.11 {
/*import net.minecraft.client.model.player.PlayerModel;
*///?} else {
import net.minecraft.client.model.PlayerModel;
//?}
import net.minecraft.client.model.geom.ModelLayers;
//? if <1.21.11 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

//? if >=1.21.11 {
/*public class NotchNpcBipedRenderer extends LivingEntityRenderer<NotchNpcEntity, net.minecraft.client.renderer.entity.state.AvatarRenderState, PlayerModel> {
*///?} else {
public class NotchNpcBipedRenderer extends LivingEntityRenderer<NotchNpcEntity, PlayerModel<NotchNpcEntity>> {
//?}

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
        //? if >=1.21.11 {
        /*this.addLayer(new HumanoidArmorLayer<>(this,
                net.minecraft.client.renderer.entity.ArmorModelSet.bake(
                        ModelLayers.PLAYER_ARMOR, ctx.getModelSet(),
                        HumanoidModel<net.minecraft.client.renderer.entity.state.AvatarRenderState>::new),
                ctx.getEquipmentRenderer()));
        *///?} else {
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        //?}
        //? if >=1.21.11 {
        /*this.addLayer(new ItemInHandLayer<>(this));
        *///?} else {
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        //?}
    }

    // Nothing is drawn on the spot from 1.21.11: work is submitted and drawn later in one pass.
    // The entity is gone by then, so everything this needs was copied into the state beforehand.
    //? if >=1.21.11 {
    /*@Override
    public void submit(net.minecraft.client.renderer.entity.state.AvatarRenderState state, PoseStack matrices,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.CameraRenderState camera) {
        NotchNpcRenderState entity = NotchNpcRenderState.of(state);
        double distSq = lodApplies() ? state.distanceToCameraSq : -1.0;
    *///?} else {
    @Override
    public void render(NotchNpcEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light) {
        // Negative means "close enough to skip every cut". That's how previews opt out (see lodApplies).
        double distSq = lodApplies() ? distanceToCameraSq(entity) : -1.0;
    //?}

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
        //? if >=1.21.11 {
        /*super.submit(state, matrices, collector, camera);
        *///?} else {
        this.model.crouching = entity.isCrouching();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        //?}

        // After the body, so the sign shows even on an NPC with its nameplate turned off.
        //? if >=1.21.11 {
        /*String[] sign = entity.billboard;
        *///?} else {
        String[] sign = NpcBillboard.lines(entity);
        //?}
        double y = entity.getNameOffset() + NpcBillboard.BASE_GAP;
        for (String line : sign) {
            if (!line.isBlank()) {
                matrices.pushPose();
                matrices.translate(0.0, y, 0.0);
                //? if >=26.2 {
                /*collector.submitNameTag(matrices, state.nameTagAttachment, 0,
                        net.minecraft.network.chat.Component.literal(line), !state.isDiscrete,
                        state.lightCoords, camera);
                *///?} elif >=1.21.11 {
                /*collector.submitNameTag(matrices, state.nameTagAttachment, 0,
                        net.minecraft.network.chat.Component.literal(line), !state.isDiscrete,
                        state.lightCoords, state.distanceToCameraSq, camera);
                *///?} elif >=1.21 {
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

    // Everything the drawing pass will need, read off the entity while it is still in hand.
    //? if >=1.21.11 {
    /*@Override
    public net.minecraft.client.renderer.entity.state.AvatarRenderState createRenderState() {
        return new net.minecraft.client.renderer.entity.state.AvatarRenderState();
    }

    @Override
    public void extractRenderState(NotchNpcEntity entity, net.minecraft.client.renderer.entity.state.AvatarRenderState vanilla, float partialTick) {
        super.extractRenderState(entity, vanilla, partialTick);
        NotchNpcRenderState state = new NotchNpcRenderState();
        NotchNpcRenderState.attachTo(vanilla, state);
        state.poseAnim = entity.getPoseAnim();
        state.npcPose = entity.getNpcPose();
        state.customPoseAngles = entity.getCustomPoseAngles();
        state.clientSwingStartAge = entity.clientSwingStartAge;
        state.slim = entity.isSlim();
        state.talkBubble = entity.showsTalkBubble();
        state.ageInTicks = entity.tickCount + partialTick;
        state.bodyHeight = entity.getBbHeight();
        state.nameOffset = entity.getNameOffset();
        state.subtitle = entity.getSubtitle();
        state.scaleX = entity.npcScale();
        state.scaleY = entity.getScaleY();
        state.scaleZ = entity.getScaleZ();
        state.billboard = NpcBillboard.lines(entity);
        state.texture = NpcSkins.resolve(entity);
        state.skinValue = entity.getSkinValue();
        vanilla.skin = new net.minecraft.world.entity.player.PlayerSkin(
                new net.minecraft.core.ClientAsset.ResourceTexture(state.texture, state.texture),
                null, null,
                state.slim ? net.minecraft.world.entity.player.PlayerModelType.SLIM
                           : net.minecraft.world.entity.player.PlayerModelType.WIDE,
                false);
        vanilla.isCrouching = entity.isCrouching();
        // The second skin layer is driven off these flags now: PlayerModel.setupAnim assigns each
        // overlay part's visibility from the state every frame, so anything set on the model itself
        // is overwritten. Left unset they default to false and the whole outer layer disappears.
        // Same distance rule setOverlaysVisible uses, so near NPCs keep their jackets and far ones
        // do not pay for them.
        boolean overlays = !lodApplies() || vanilla.distanceToCameraSq < DETAIL_RANGE_SQ;
        vanilla.showHat = overlays;
        vanilla.showJacket = overlays;
        vanilla.showLeftSleeve = overlays;
        vanilla.showRightSleeve = overlays;
        vanilla.showLeftPants = overlays;
        vanilla.showRightPants = overlays;
    }
    *///?}

    //? if >=1.21.11 {
    /*@Override
    protected void setupRotations(net.minecraft.client.renderer.entity.state.AvatarRenderState state, PoseStack matrices,
                                  float yBodyRot, float scale) {
        NotchNpcRenderState entity = NotchNpcRenderState.of(state);
    *///?} elif >=1.21 {
    /*@Override
    protected void setupRotations(NotchNpcEntity entity, PoseStack matrices, float animationProgress,
                                   float yBodyRot, float tickDelta, float scale) {
    *///?} else {
    @Override
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
        //? if >=1.21.11 {
        /*super.setupRotations(state, matrices, yBodyRot, scale);
        *///?} elif >=1.21 {
        /*super.setupRotations(entity, matrices, animationProgress, yBodyRot, tickDelta, scale);
        *///?} else {
        super.setupRotations(entity, matrices, animationProgress, yBodyRot, tickDelta);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    protected void scale(net.minecraft.client.renderer.entity.state.AvatarRenderState state, PoseStack matrices) {
        NotchNpcRenderState entity = NotchNpcRenderState.of(state);
    *///?} else {
    @Override
    protected void scale(NotchNpcEntity entity, PoseStack matrices, float amount) {
    //?}
        // Scale only the model here (not the name label, which is rendered separately).
        float sx = entity.npcScale(), sy = entity.getScaleY(), sz = entity.getScaleZ();
        if (sx != 1.0f || sy != 1.0f || sz != 1.0f) matrices.scale(sx, sy, sz);
    }

    //? if >=26.1 {
    /*@Override
    protected void submitNameDisplay(net.minecraft.client.renderer.entity.state.AvatarRenderState state, PoseStack matrices,
                                     net.minecraft.client.renderer.SubmitNodeCollector collector,
                                     net.minecraft.client.renderer.state.CameraRenderState camera) {
    *///?} elif >=1.21.11 {
    /*@Override
    protected void submitNameTag(net.minecraft.client.renderer.entity.state.AvatarRenderState state, PoseStack matrices,
                                 net.minecraft.client.renderer.SubmitNodeCollector collector,
                                 net.minecraft.client.renderer.state.CameraRenderState camera) {
    *///?} elif >=1.21 {
    /*@Override
    protected void renderNameTag(NotchNpcEntity entity, net.minecraft.network.chat.Component text,
                                       PoseStack matrices, MultiBufferSource vertexConsumers,
                                       int light, float tickDelta) {
    *///?} else {
    @Override
    protected void renderNameTag(NotchNpcEntity entity, net.minecraft.network.chat.Component text,
                                       PoseStack matrices, MultiBufferSource vertexConsumers,
                                       int light) {
    //?}
        //? if >=1.21.11 {
        /*NotchNpcRenderState entity = NotchNpcRenderState.of(state);
        *///?}
        matrices.pushPose();
        matrices.translate(0.0, entity.getNameOffset(), 0.0);
        //? if >=26.1 {
        /*super.submitNameDisplay(state, matrices, collector, camera);
        *///?} elif >=1.21.11 {
        /*super.submitNameTag(state, matrices, collector, camera);
        *///?} elif >=1.21 {
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
            //? if >=26.2 {
            /*collector.submitNameTag(matrices, state.nameTagAttachment, 0, line,
                    !state.isDiscrete, state.lightCoords, camera);
            *///?} elif >=1.21.11 {
            /*collector.submitNameTag(matrices, state.nameTagAttachment, 0, line,
                    !state.isDiscrete, state.lightCoords, state.distanceToCameraSq, camera);
            *///?} elif >=1.21 {
            /*super.renderNameTag(entity, line, matrices, vertexConsumers, light, tickDelta);
            *///?} else {
            super.renderNameTag(entity, line, matrices, vertexConsumers, light);
            //?}
            matrices.popPose();
        }
    }

    //? if >=1.21.11 {
    /*@Override
    public ResourceLocation getTextureLocation(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        return NotchNpcRenderState.of(state).texture;
    }
    *///?} else {
    @Override
    public ResourceLocation getTextureLocation(NotchNpcEntity entity) {
        return NpcSkins.resolve(entity);
    }
    //?}

    //? if >=1.21.11 {
    /*@Override
    protected boolean shouldShowName(NotchNpcEntity entity, double distSq) {
        if (!entity.hasCustomName() || !entity.isCustomNameVisible() || entity.isInvisible()) return false;
        return !lodApplies() || distSq < LABEL_RANGE_SQ;
    }
    *///?} else {
    @Override
    protected boolean shouldShowName(NotchNpcEntity entity) {
        if (!entity.hasCustomName() || !entity.isCustomNameVisible() || entity.isInvisible()) return false;
        return !lodApplies() || distanceToCameraSq(entity) < LABEL_RANGE_SQ;
    }
    //?}

    double distanceToCameraSq(NotchNpcEntity entity) {
        return this.entityRenderDispatcher.distanceToSqr(entity);
    }
    static boolean lodApplies() {
        return net.fugginbeenus.notchcurrency.compat.Render.currentScreen() == null;
    }
}
