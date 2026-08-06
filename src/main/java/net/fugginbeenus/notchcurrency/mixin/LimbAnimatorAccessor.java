package net.fugginbeenus.notchcurrency.mixin;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor {

    @Accessor("pos")
    void notchcurrency$setPos(float pos);

    @Accessor("prevSpeed")
    void notchcurrency$setPrevSpeed(float prevSpeed);
}
