package net.fugginbeenus.notchcurrency.mixin;

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
