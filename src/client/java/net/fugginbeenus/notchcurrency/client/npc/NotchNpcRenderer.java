package net.fugginbeenus.notchcurrency.client.npc;

import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class NotchNpcRenderer extends EntityRenderer<NotchNpcEntity> {

    private final NotchNpcBipedRenderer biped;
    private final NotchNpcGeoRenderer geo;
    private final boolean applyLoaded;
    private final Map<String, Entity> proxies = new HashMap<>();

    public NotchNpcRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.biped = new NotchNpcBipedRenderer(ctx);
        this.geo = new NotchNpcGeoRenderer(ctx);
        this.applyLoaded = FabricLoader.getInstance().isModLoaded("apply");
    }

    @Override
    public void render(NotchNpcEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
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
                                   MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        Entity proxy = getProxy(typeId);
        if (!(proxy instanceof LivingEntity le)) return false;

        le.setYaw(npc.getYaw());
        le.setBodyYaw(npc.bodyYaw);
        le.setHeadYaw(npc.headYaw);
        le.prevBodyYaw = npc.prevBodyYaw;
        le.prevHeadYaw = npc.prevHeadYaw;
        le.setPitch(npc.getPitch());
        le.age = npc.age;
        copyAnimationState(npc, le);

        float sx = npc.getScale(), sy = npc.getScaleY(), sz = npc.getScaleZ();
        boolean scaled = sx != 1.0f || sy != 1.0f || sz != 1.0f;
        if (scaled) {
            matrices.push();
            matrices.scale(sx, sy, sz);
        }
        try {
            @SuppressWarnings("unchecked")
            EntityRenderer<Entity> r = (EntityRenderer<Entity>) this.dispatcher.getRenderer(le);
            if (r == null) {
                if (scaled) matrices.pop();
                return false;
            }
            r.render(le, yaw, tickDelta, matrices, vcp, light);
        } catch (Exception e) {
            if (scaled) matrices.pop();
            return false;
        }
        if (scaled) matrices.pop();
        // The disguise proxy has no name, so draw the NPC's own label (unscaled, consistent height),
        // subject to the same range cap the biped path uses so crowds don't pay for unreadable text.
        boolean labelInRange = !NotchNpcBipedRenderer.lodApplies()
                || this.dispatcher.getSquaredDistanceToCamera(npc) < 32.0 * 32.0;
        if (npc.hasCustomName() && npc.isCustomNameVisible() && labelInRange) {
            matrices.push();
            matrices.translate(0.0, npc.getNameOffset(), 0.0);
            //? if >=1.21 {
            /*this.renderLabelIfPresent(npc, npc.getDisplayName(), matrices, vcp, light, tickDelta);
            *///?} else {
            this.renderLabelIfPresent(npc, npc.getDisplayName(), matrices, vcp, light);
            //?}
            matrices.pop();
        }
        // The disguise's own renderer knows nothing about our sign, so draw it here too.
        String[] sign = NpcBillboard.lines(npc);
        double signY = npc.getNameOffset() + NpcBillboard.BASE_GAP;
        for (String line : sign) {
            if (!line.isBlank()) {
                matrices.push();
                matrices.translate(0.0, signY, 0.0);
                //? if >=1.21 {
                /*this.renderLabelIfPresent(npc, net.minecraft.text.Text.literal(line), matrices, vcp,
                        light, tickDelta);
                *///?} else {
                this.renderLabelIfPresent(npc, net.minecraft.text.Text.literal(line), matrices, vcp, light);
                //?}
                matrices.pop();
            }
            signY += NpcBillboard.LINE_HEIGHT;
        }
        return true;
    }

    private static void copyAnimationState(NotchNpcEntity npc, LivingEntity le) {
        le.limbAnimator.setSpeed(npc.limbAnimator.getSpeed());
        ((net.fugginbeenus.notchcurrency.mixin.LimbAnimatorAccessor) (Object) le.limbAnimator)
                .notchcurrency$setPos(npc.limbAnimator.getPos());
        // Both ends of the interpolation, or the limbs stutter between frames.
        ((net.fugginbeenus.notchcurrency.mixin.LimbAnimatorAccessor) (Object) le.limbAnimator)
                .notchcurrency$setPrevSpeed(npc.limbAnimator.getSpeed(0.0f));

        le.handSwinging = npc.handSwinging;
        le.handSwingTicks = npc.handSwingTicks;
        le.handSwingProgress = npc.handSwingProgress;
        le.lastHandSwingProgress = npc.lastHandSwingProgress;
    }

    private Entity getProxy(String typeId) {
        if (proxies.containsKey(typeId)) return proxies.get(typeId);
        Entity proxy = null;
        try {
            EntityType<?> type = Registries.ENTITY_TYPE.get(Reg.parse(typeId));
            var world = MinecraftClient.getInstance().world;
            if (type != null && world != null) proxy = type.create(world);
        } catch (Exception ignored) {}
        proxies.put(typeId, proxy); // caches null too, so we don't retry a bad type every frame
        return proxy;
    }

    @Override
    public Identifier getTexture(NotchNpcEntity entity) {
        return biped.getTexture(entity);
    }
}
