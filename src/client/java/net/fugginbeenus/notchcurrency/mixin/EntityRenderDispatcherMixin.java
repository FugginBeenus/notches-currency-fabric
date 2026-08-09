package net.fugginbeenus.notchcurrency.mixin;

// Sends an NPC's render state back to the NPC's own renderer at drawing time.
//
// From 1.21.11 the drawing pass picks a renderer from the render state rather than the entity, and
// its first question is whether the state is an AvatarRenderState. Ours is, because the NPC borrows
// the player model, so every NPC was being handed to the vanilla player renderer instead. That is
// why skins, sneaking and sleeping worked while poses, models, disguises and signs did not: those
// live in our submit, which was never called.
//
// Only states carrying NPC data are redirected, so real players are untouched.
//? if >=1.21.11 {
/*import net.fugginbeenus.notchcurrency.client.npc.NotchNpcStateHolder;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
            at = @At("HEAD"), cancellable = true)
    private void notchcurrency$npcKeepsItsOwnRenderer(
            EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> info) {
        if (state instanceof NotchNpcStateHolder holder && holder.notchcurrency$getNpcState() != null) {
            EntityRenderer<?, ?> renderer = this.renderers.get(state.entityType);
            if (renderer != null) info.setReturnValue(renderer);
        }
    }
}
*///?}
