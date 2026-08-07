package net.fugginbeenus.notchcurrency.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface LimbAnimatorAccessor {

    @Accessor("position")
    void notchcurrency$setPos(float pos);

    @Accessor("speedOld")
    void notchcurrency$setPrevSpeed(float prevSpeed);
}
