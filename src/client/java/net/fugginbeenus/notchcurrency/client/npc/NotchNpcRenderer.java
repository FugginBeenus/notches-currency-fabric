package net.fugginbeenus.notchcurrency.client.npc;

import net.minecraft.network.chat.Component;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.Minecraft;
//? if <1.21.11 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;

//? if >=1.21.11 {
/*public class NotchNpcRenderer extends EntityRenderer<NotchNpcEntity, net.minecraft.client.renderer.entity.state.AvatarRenderState> {
*///?} else {
public class NotchNpcRenderer extends EntityRenderer<NotchNpcEntity> {
//?}

    private final NotchNpcBipedRenderer biped;
    private final NotchNpcGeoRenderer geo;
    private final Map<String, Entity> proxies = new HashMap<>();

    public NotchNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.biped = new NotchNpcBipedRenderer(ctx);
        this.geo = new NotchNpcGeoRenderer(ctx);
    }

    //? if >=1.21.11 {
    /*@Override
    public net.minecraft.client.renderer.entity.state.AvatarRenderState createRenderState() {
        return new net.minecraft.client.renderer.entity.state.AvatarRenderState();
    }

    @Override
    public void extractRenderState(NotchNpcEntity entity, net.minecraft.client.renderer.entity.state.AvatarRenderState vanilla, float partialTick) {
        biped.extractRenderState(entity, vanilla, partialTick);
        NotchNpcRenderState state = NotchNpcRenderState.of(vanilla);
        String model = entity.getModelId();
        state.invisible = entity.isInvisible();
        state.useGeo = NotchNpcEntity.MODEL_APPLY.equals(model);
        state.displayName = entity.getDisplayName();
        if (state.useGeo) {
            state.showLabel = labelShows(entity);
        }
        if (!state.invisible && !state.useGeo && model != null && model.startsWith("entity:")) {
            extractDisguise(entity, state, model.substring("entity:".length()), partialTick);
        }
    }

    @Override
    public void submit(net.minecraft.client.renderer.entity.state.AvatarRenderState vanilla, PoseStack matrices,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.CameraRenderState camera) {
        NotchNpcRenderState state = NotchNpcRenderState.of(vanilla);
        // Invisible (stats toggle or day/night rule): draw nothing, label included. The biped
        // path would hide the body on its own, but the geo/disguise paths would not.
        if (state.invisible) return;
        if (state.useGeo) {
            geo.submit(vanilla, matrices, collector, camera);
            // GeckoLib's renderer draws the model and nothing else, so the nameplate and the sign
            // are ours to draw, exactly as on the disguise path.
            submitLabels(state, vanilla, matrices, collector, camera);
            return;
        }
        if (state.proxyRenderer != null && submitDisguise(state, matrices, collector, camera)) return;
        biped.submit(vanilla, matrices, collector, camera);
    }
    *///?} else {
    @Override
    public void render(NotchNpcEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light) {
        String model = entity.getModelId();
        if (entity.isInvisible()) {
            // Invisible (stats toggle or day/night rule): draw nothing, label included. The biped
            // path would hide the body on its own, but the geo/disguise paths would not.
            return;
        }
        if (NotchNpcEntity.MODEL_APPLY.equals(model)) {
            geo.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            // GeckoLib's renderer draws the model and nothing else, so the nameplate and the sign
            // are ours to draw, exactly as on the disguise path.
            renderLabels(entity, matrices, vertexConsumers, light, tickDelta);
            return;
        }
        if (model != null && model.startsWith("entity:")
                && renderDisguise(entity, model.substring("entity:".length()), yaw, tickDelta, matrices, vertexConsumers, light)) {
            return;
        }
        biped.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
    //?}

    //? if >=1.21.11 {
    /*@SuppressWarnings("unchecked")
    private void extractDisguise(NotchNpcEntity npc, NotchNpcRenderState state, String typeId, float partialTick) {
        Entity proxy = getProxy(typeId);
        if (!(proxy instanceof LivingEntity le)) return;
        poseProxy(npc, le);
        try {
            net.minecraft.client.renderer.entity.EntityRenderer<Entity, ?> r =
                    (net.minecraft.client.renderer.entity.EntityRenderer<Entity, ?>)
                            this.entityRenderDispatcher.getRenderer(le);
            if (r == null) return;
            state.proxyRenderer = r;
            state.proxyState = r.createRenderState(le, partialTick);
        } catch (Exception ignored) {
            state.proxyRenderer = null;
        }
        state.showLabel = labelShows(npc);
    }

    // Same range cap the biped path uses, so crowds do not pay for unreadable text.
    private boolean labelShows(NotchNpcEntity npc) {
        boolean inRange = !NotchNpcBipedRenderer.lodApplies()
                || this.entityRenderDispatcher.distanceToSqr(npc) < 32.0 * 32.0;
        return npc.hasCustomName() && npc.isCustomNameVisible() && inRange;
    }

    @SuppressWarnings("unchecked")
    private boolean submitDisguise(NotchNpcRenderState state, PoseStack matrices,
                                   net.minecraft.client.renderer.SubmitNodeCollector collector,
                                   net.minecraft.client.renderer.state.CameraRenderState camera) {
        float sx = state.npcScale(), sy = state.getScaleY(), sz = state.getScaleZ();
        boolean scaled = sx != 1.0f || sy != 1.0f || sz != 1.0f;
        if (scaled) {
            matrices.pushPose();
            matrices.scale(sx, sy, sz);
        }
        try {
            state.proxyRenderer.submit(state.proxyState, matrices, collector, camera);
        } catch (Exception e) {
            if (scaled) matrices.popPose();
            return false;
        }
        if (scaled) matrices.popPose();

        submitLabels(state, state.proxyState, matrices, collector, camera);
        return true;
    }

    // The nameplate and the floating sign, for the two paths that borrow someone else's renderer.
    // Neither the disguise proxy nor GeckoLib knows anything about them, and both are drawn
    // unscaled so the text sits at a consistent height whatever the NPC's size.
    private void submitLabels(NotchNpcRenderState state,
                              net.minecraft.client.renderer.entity.state.EntityRenderState anchor,
                              PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector collector,
                              net.minecraft.client.renderer.state.CameraRenderState camera) {
        if (state.showLabel) {
            matrices.pushPose();
            matrices.translate(0.0, state.getNameOffset(), 0.0);
            submitLine(anchor, state.displayName, matrices, collector, camera);
            matrices.popPose();
        }
        double signY = state.getNameOffset() + NpcBillboard.BASE_GAP;
        for (String line : state.billboard) {
            if (!line.isBlank()) {
                matrices.pushPose();
                matrices.translate(0.0, signY, 0.0);
                submitLine(anchor, net.minecraft.network.chat.Component.literal(line), matrices, collector, camera);
                matrices.popPose();
            }
            signY += NpcBillboard.LINE_HEIGHT;
        }
    }

    private void submitLine(net.minecraft.client.renderer.entity.state.EntityRenderState anchor,
                            net.minecraft.network.chat.Component text,
                            PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector collector,
                            net.minecraft.client.renderer.state.CameraRenderState camera) {
        net.fugginbeenus.notchcurrency.compat.Render.submitNameLine(
                anchor, text, matrices, collector, camera);
    }
    *///?} else {
    private boolean renderDisguise(NotchNpcEntity npc, String typeId, float yaw, float tickDelta,
                                   PoseStack matrices, MultiBufferSource vcp, int light) {
        Entity proxy = getProxy(typeId);
        if (!(proxy instanceof LivingEntity le)) return false;

        le.setYRot(npc.getYRot());
        le.setYBodyRot(npc.yBodyRot);
        le.setYHeadRot(npc.yHeadRot);
        le.yBodyRotO = npc.yBodyRotO;
        le.yHeadRotO = npc.yHeadRotO;
        le.setXRot(npc.getXRot());
        le.tickCount = npc.tickCount;
        copyAnimationState(npc, le);

        float sx = npc.npcScale(), sy = npc.getScaleY(), sz = npc.getScaleZ();
        boolean scaled = sx != 1.0f || sy != 1.0f || sz != 1.0f;
        if (scaled) {
            matrices.pushPose();
            matrices.scale(sx, sy, sz);
        }
        try {
            @SuppressWarnings("unchecked")
            EntityRenderer<Entity> r = (EntityRenderer<Entity>) this.entityRenderDispatcher.getRenderer(le);
            if (r == null) {
                if (scaled) matrices.popPose();
                return false;
            }
            r.render(le, yaw, tickDelta, matrices, vcp, light);
        } catch (Exception e) {
            if (scaled) matrices.popPose();
            return false;
        }
        if (scaled) matrices.popPose();
        renderLabels(npc, matrices, vcp, light, tickDelta);
        return true;
    }

    // The nameplate and the floating sign, for the two paths that borrow someone else's renderer.
    // Neither the disguise proxy nor GeckoLib knows anything about them, and both are drawn
    // unscaled so the text sits at a consistent height whatever the NPC's size. The range cap is
    // the one the biped path uses, so crowds do not pay for unreadable text.
    private void renderLabels(NotchNpcEntity npc, PoseStack matrices, MultiBufferSource vcp,
                              int light, float tickDelta) {
        boolean labelInRange = !NotchNpcBipedRenderer.lodApplies()
                || this.entityRenderDispatcher.distanceToSqr(npc) < 32.0 * 32.0;
        if (npc.hasCustomName() && npc.isCustomNameVisible() && labelInRange) {
            matrices.pushPose();
            matrices.translate(0.0, npc.getNameOffset(), 0.0);
            //? if >=1.21 {
            /*this.renderNameTag(npc, npc.getDisplayName(), matrices, vcp, light, tickDelta);
            *///?} else {
            this.renderNameTag(npc, npc.getDisplayName(), matrices, vcp, light);
            //?}
            matrices.popPose();
        }
        String[] sign = NpcBillboard.lines(npc);
        double signY = npc.getNameOffset() + NpcBillboard.BASE_GAP;
        for (String line : sign) {
            if (!line.isBlank()) {
                matrices.pushPose();
                matrices.translate(0.0, signY, 0.0);
                //? if >=1.21 {
                /*this.renderNameTag(npc, net.minecraft.network.chat.Component.literal(line), matrices, vcp,
                        light, tickDelta);
                *///?} else {
                this.renderNameTag(npc, net.minecraft.network.chat.Component.literal(line), matrices, vcp, light);
                //?}
                matrices.popPose();
            }
            signY += NpcBillboard.LINE_HEIGHT;
        }
    }
    //?}

    // Everything the borrowed renderer needs to believe it is drawing the NPC.
    private static void poseProxy(NotchNpcEntity npc, LivingEntity le) {
        le.setYRot(npc.getYRot());
        le.setYBodyRot(npc.yBodyRot);
        le.setYHeadRot(npc.yHeadRot);
        le.yBodyRotO = npc.yBodyRotO;
        le.yHeadRotO = npc.yHeadRotO;
        le.setXRot(npc.getXRot());
        le.tickCount = npc.tickCount;
        copyAnimationState(npc, le);
    }

    private static void copyAnimationState(NotchNpcEntity npc, LivingEntity le) {
        le.walkAnimation.setSpeed(npc.walkAnimation.speed());
        ((net.fugginbeenus.notchcurrency.mixin.LimbAnimatorAccessor) (Object) le.walkAnimation)
                .notchcurrency$setPos(npc.walkAnimation.position());
        // Both ends of the interpolation, or the limbs stutter between frames.
        ((net.fugginbeenus.notchcurrency.mixin.LimbAnimatorAccessor) (Object) le.walkAnimation)
                .notchcurrency$setPrevSpeed(npc.walkAnimation.speed(0.0f));

        le.swinging = npc.swinging;
        le.swingTime = npc.swingTime;
        le.attackAnim = npc.attackAnim;
        le.oAttackAnim = npc.oAttackAnim;
    }

    private Entity getProxy(String typeId) {
        if (proxies.containsKey(typeId)) return proxies.get(typeId);
        Entity proxy = null;
        try {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Reg.parse(typeId));
            var world = Minecraft.getInstance().level;
            //? if >=1.21.11 {
            /*if (type != null && world != null) {
                proxy = type.create(world, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            }
            *///?} else {
            if (type != null && world != null) proxy = type.create(world);
            //?}
        } catch (Exception ignored) {}
        proxies.put(typeId, proxy); // caches null too, so we don't retry a bad type every frame
        return proxy;
    }

    //? if >=1.21.11 {
    /*
    *///?} else {
    @Override
    public ResourceLocation getTextureLocation(NotchNpcEntity entity) {
        return biped.getTextureLocation(entity);
    }
    //?}
}
