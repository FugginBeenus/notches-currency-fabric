package net.fugginbeenus.notchcurrency.mixin;

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
