package net.fugginbeenus.notchcurrency.client.npc;

import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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

public class NotchNpcRenderer extends EntityRenderer<NotchNpcEntity> {

    private final NotchNpcBipedRenderer biped;
    private final NotchNpcGeoRenderer geo;
    private final boolean applyLoaded;
    private final Map<String, Entity> proxies = new HashMap<>();

    public NotchNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.biped = new NotchNpcBipedRenderer(ctx);
        this.geo = new NotchNpcGeoRenderer(ctx);
        this.applyLoaded = FabricLoader.getInstance().isModLoaded("apply");
    }

    @Override
    public void render(NotchNpcEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light) {
        String model = entity.getModelId();
        if (entity.isInvisible()) {
            // Invisible (stats toggle or day/night rule): draw nothing, label included. The biped
            // path would hide the body on its own, but the geo/disguise paths would not.
            return;
        }
        if (applyLoaded && NotchNpcEntity.MODEL_APPLY.equals(model)) {
            geo.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }
        if (model != null && model.startsWith("entity:")
                && renderDisguise(entity, model.substring("entity:".length()), yaw, tickDelta, matrices, vertexConsumers, light)) {
            return;
        }
        biped.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

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

        float sx = npc.getScale(), sy = npc.getScaleY(), sz = npc.getScaleZ();
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
        // The disguise proxy has no name, so draw the NPC's own label (unscaled, consistent height),
        // subject to the same range cap the biped path uses so crowds don't pay for unreadable text.
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
        // The disguise's own renderer knows nothing about our sign, so draw it here too.
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
        return true;
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
            if (type != null && world != null) proxy = type.create(world);
        } catch (Exception ignored) {}
        proxies.put(typeId, proxy); // caches null too, so we don't retry a bad type every frame
        return proxy;
    }

    @Override
    public ResourceLocation getTextureLocation(NotchNpcEntity entity) {
        return biped.getTextureLocation(entity);
    }
}
