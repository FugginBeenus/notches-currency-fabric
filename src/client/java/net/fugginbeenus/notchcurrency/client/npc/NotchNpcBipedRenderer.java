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
        double distSq = lodApplies() ? distanceToCameraSq(entity) : -1.0;
    //?}
        boolean packFree = entity.getPoseAnim() == NotchNpcEntity.ANIM_STATUE
                || (distSq >= 0 && distSq >= ANIM_RANGE_SQ);
        boolean slim = entity.isSlim();
        NpcPlayerModel m = packFree
                ? (slim ? this.frozenSlim : this.frozen)
                : (slim ? this.liveSlim : this.live);
        m.setOverlaysVisible(distSq < 0 || distSq < DETAIL_RANGE_SQ);
        this.model = m;
        //? if >=1.21.11 {
        /*super.submit(state, matrices, collector, camera);
        *///?} else {
        m.npcTint = entity.getTint();
        m.npcAlpha = entity.getAlpha();
        this.model.crouching = entity.isCrouching();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        //?}
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
        state.bodyOffset = entity.getBodyOffset();
        state.subtitle = entity.getSubtitle();
        state.scaleX = entity.npcScale();
        state.scaleY = entity.getScaleY();
        state.scaleZ = entity.getScaleZ();
        state.billboard = NpcBillboard.lines(entity);
        state.tint = entity.getTint();
        state.alpha = entity.getAlpha();
        state.texture = NpcSkins.resolve(entity);
        state.skinValue = entity.getSkinValue();
        vanilla.skin = new net.minecraft.world.entity.player.PlayerSkin(
                new net.minecraft.core.ClientAsset.ResourceTexture(state.texture, state.texture),
                null, null,
                state.slim ? net.minecraft.world.entity.player.PlayerModelType.SLIM
                           : net.minecraft.world.entity.player.PlayerModelType.WIDE,
                false);
        vanilla.isCrouching = entity.isCrouching();
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
        float lift = entity.getBodyOffset();
        if (lift != 0.0f) matrices.translate(0.0f, lift, 0.0f);
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
    protected int getModelTint(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        NotchNpcRenderState npc = NotchNpcRenderState.of(state);
        return blend(super.getModelTint(state), npc.tint, npc.alpha);
    }

    private static int blend(int base, int tint, float alpha) {
        if (tint == -1 && alpha >= 1.0f) return base;
        int a = (int) (((base >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, alpha)));
        int r = (base >> 16) & 0xFF;
        int g = (base >> 8) & 0xFF;
        int b = base & 0xFF;
        if (tint != -1) {
            r = r * ((tint >> 16) & 0xFF) / 255;
            g = g * ((tint >> 8) & 0xFF) / 255;
            b = b * (tint & 0xFF) / 255;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    *///?}

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
