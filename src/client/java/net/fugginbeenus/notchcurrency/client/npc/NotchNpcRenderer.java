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

/**
 * Dispatching renderer for the Notch NPC. Three model paths:
 *  - "humanoid" (default): a vanilla biped with preset/player/URL skins.
 *  - "apply" (only when the {@code apply} mod is installed): the animated GeckoLib model.
 *  - "entity:&lt;id&gt;": a disguise — renders as any vanilla/modded living entity via a cached proxy.
 * Anything that can't render (unknown/non-living type, renderer failure) falls back to the biped.
 */
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

    /** Render the NPC as another entity type. Returns false (→ caller draws the biped) on any failure. */
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

        float scale = npc.getScale();
        boolean scaled = scale > 0f && scale != 1.0f;
        if (scaled) {
            matrices.push();
            matrices.scale(scale, scale, scale);
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
            //? if >=1.21 {
            /*this.renderLabelIfPresent(npc, npc.getDisplayName(), matrices, vcp, light, tickDelta);
            *///?} else {
            this.renderLabelIfPresent(npc, npc.getDisplayName(), matrices, vcp, light);
            //?}
        }
        return true;
    }

    /**
     * Put the stand-in in step with the NPC so a disguise walks and swings like the thing it's wearing.
     *
     * <p>The stand-in never ticks — it only exists to be drawn — so none of this happens on its own.
     * Renderers read the walk cycle and the swing straight off the entity, which is why they have to
     * be set rather than left at zero. One stand-in serves every NPC of a given disguise, so this runs
     * fresh for each of them.
     */
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
