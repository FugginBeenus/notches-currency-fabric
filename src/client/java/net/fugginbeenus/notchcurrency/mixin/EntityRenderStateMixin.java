package net.fugginbeenus.notchcurrency.mixin;

// Adds one field to every entity render state, holding the NPC data our renderer needs at drawing
// time. See NotchNpcStateHolder for why a plain field rather than an attachment or a subclass.
//
// Only registered from 1.21.11, where render states exist at all; the config entry is added per
// version in build.gradle, so on older versions this file compiles to nothing and is never listed.
//? if >=1.21.11 {
/*import net.fugginbeenus.notchcurrency.client.npc.NotchNpcRenderState;
import net.fugginbeenus.notchcurrency.client.npc.NotchNpcStateHolder;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements NotchNpcStateHolder {

    @Unique
    private NotchNpcRenderState notchcurrency$npcState;

    @Override
    public NotchNpcRenderState notchcurrency$getNpcState() {
        return this.notchcurrency$npcState;
    }

    @Override
    public void notchcurrency$setNpcState(NotchNpcRenderState state) {
        this.notchcurrency$npcState = state;
    }
}
*///?}
