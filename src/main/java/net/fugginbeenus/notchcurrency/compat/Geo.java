package net.fugginbeenus.notchcurrency.compat;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
//? if >=1.21 {
/*import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.RawAnimation;
*///?} else {
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.RawAnimation;
//?}
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The GeckoLib calls that can be hidden behind one door.
 *
 * <p>Only the utility surface lives here, and that is a real limit rather than an oversight. GeckoLib
 * is used through inheritance: the entity implements GeoEntity, the renderer extends
 * GeoEntityRenderer, and both override methods whose signatures change between versions. Java has no
 * type aliases, so no facade can stand in front of an extends clause, an import, or an override.
 * Those stay as Stonecutter branches, which is the right tool for them.
 *
 * <p>What that leaves is still worth having. Between 4.4 and 4.8 the animation classes moved package,
 * and 5.x moves three more and deletes AnimationState outright. Every version bump so far has been a
 * package move rather than a behaviour change, so the calls themselves are stable even when their
 * imports are not, and collecting them here means one file to edit instead of four.
 */
public final class Geo {

    private Geo() {}

    /**
     * Start GeckoLib, where that is still something a mod has to do.
     *
     * <p>4.4 needs the explicit call. 4.8 and later initialise themselves and do not expose it at all,
     * so this is a no-op there rather than a call that would not compile.
     */
    public static void init() {
        //? if <1.21
        software.bernie.geckolib.GeckoLib.initialize();
    }

    /** The per-instance animation cache an animatable has to hold onto. */
    public static AnimatableInstanceCache cache(NotchNpcEntity animatable) {
        return GeckoLibUtil.createInstanceCache(animatable);
    }

    /**
     * A single animation played on a loop, which is the only shape this mod asks for.
     *
     * <p>Cached per name, because the caller is an animation predicate: it runs every frame for every
     * visible NPC, and building a fresh RawAnimation there would allocate once per NPC per frame. The
     * field this replaced was a static built once, and a facade is no excuse to make it worse.
     */
    public static RawAnimation loop(String animationName) {
        return LOOPS.computeIfAbsent(animationName, name -> RawAnimation.begin().thenLoop(name));
    }

    private static final java.util.Map<String, RawAnimation> LOOPS = new java.util.concurrent.ConcurrentHashMap<>();
}
