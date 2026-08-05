package net.fugginbeenus.notchcurrency.mixin;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Opens up the walk-cycle position so a disguised NPC's stand-in can be put in step with it.
 *
 * <p>The stand-in entity is shared by every NPC wearing the same disguise, so its animation state has
 * to be set outright before each one is drawn rather than advanced over time, and {@code pos} is the
 * one piece with no setter. Reflection wouldn't do: field names are remapped in a built jar, while a
 * mixin accessor is remapped along with everything else.
 */
@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor {

    @Accessor("pos")
    void notchcurrency$setPos(float pos);

    @Accessor("prevSpeed")
    void notchcurrency$setPrevSpeed(float prevSpeed);
}
