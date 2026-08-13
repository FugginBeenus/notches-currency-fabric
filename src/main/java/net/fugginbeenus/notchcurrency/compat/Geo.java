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

    /**
     * Where GeckoLib filed the NPC's animations.
     *
     * <p>5.x scans geckolib/animations and keys what it finds by the bare name. 4.x wanted the full
     * path. Kept here rather than in the model so the entity can ask what clips exist without
     * reaching into client code.
     */
    public static final String MODEL_NAME = "notch_npc";

    //? if >=1.21.11 {
    /*public static final net.minecraft.resources.ResourceLocation NPC_ANIMATIONS =
            net.fugginbeenus.notchcurrency.core.NotchCurrency.id("notch_npc");
    *///?} else {
    public static final net.minecraft.resources.ResourceLocation NPC_ANIMATIONS =
            net.fugginbeenus.notchcurrency.core.NotchCurrency.id("animations/notch_npc.animation.json");
    //?}

    /**
     * Every clip GeckoLib has loaded for the NPC, whatever provided it.
     *
     * <p>Read from the live cache rather than from a list in the code, so a resource pack that adds
     * clips to the animation file has them turn up in the editor without the mod knowing their
     * names in advance. That is the whole of the custom animation feature: the pack supplies the
     * motion, this supplies the list to pick it from.
     *
     * <p>Client-side only in practice. The cache is filled from resource packs, so on a dedicated
     * server it is simply empty, and callers treat that the same as "no custom clips".
     */
    public static java.util.List<String> clipNames() {
        return clipNames(NPC_ANIMATIONS, MODEL_NAME);
    }

    /** The clips in one animation file, by id, falling back to a name match. */
    public static java.util.List<String> clipNames(net.minecraft.resources.ResourceLocation file,
                                                   String nameHint) {
        try {
            // Three shapes, not two. 5.x replaced the flat map with a cache record, and 5.5 moved
            // the whole library from software.bernie to com.geckolib on the way to 26.
            //? if >=26.1 {
            /*var cache = com.geckolib.cache.GeckoLibResources.getBakedAnimations();
            var map = cache == null ? null : cache.cache();
            *///?} elif >=1.21.11 {
            /*var cache = software.bernie.geckolib.cache.GeckoLibResources.getBakedAnimations();
            var map = cache == null ? null : cache.cache();
            *///?} else {
            var map = software.bernie.geckolib.cache.GeckoLibCache.getBakedAnimations();
            //?}
            if (map == null || map.isEmpty()) return java.util.List.of();

            var found = map.get(file);
            if (found == null) {
                // GeckoLib has changed how it keys this file more than once, and a miss here would
                // quietly leave the picker empty. Rather than trust one spelling, take the entry
                // that names our model.
                for (var entry : map.entrySet()) {
                    if (String.valueOf(entry.getKey()).contains(nameHint)) {
                        found = entry.getValue();
                        break;
                    }
                }
            }
            if (found == null) return java.util.List.of();

            var names = new java.util.ArrayList<>(found.animations().keySet());
            java.util.Collections.sort(names);
            return names;
        } catch (Throwable notLoadedYet) {
            // Asked before the packs finished loading, or on a side that has no packs at all.
            return java.util.List.of();
        }
    }

    /**
     * Whether GeckoLib has a baked model under this id.
     *
     * <p>Only used to check whether a model written at runtime was picked up. The whole custom model
     * design rests on that working, so it is worth being able to ask rather than guess.
     */
    public static boolean hasBakedModel(net.minecraft.resources.ResourceLocation id) {
        try {
            //? if >=26.1 {
            /*var cache = com.geckolib.cache.GeckoLibResources.getBakedModels();
            var map = cache == null ? null : cache.cache();
            *///?} elif >=1.21.11 {
            /*var cache = software.bernie.geckolib.cache.GeckoLibResources.getBakedModels();
            var map = cache == null ? null : cache.cache();
            *///?} else {
            var map = software.bernie.geckolib.cache.GeckoLibCache.getBakedModels();
            //?}
            if (map == null) return false;
            if (map.containsKey(id)) return true;
            for (var key : map.keySet()) {
                if (String.valueOf(key).contains(id.getPath())) return true;
            }
            return false;
        } catch (Throwable notLoadedYet) {
            return false;
        }
    }

    /** Whether a clip is actually there, so a pack being removed does not leave an NPC stuck. */
    public static boolean hasClip(String name) {
        return name != null && !name.isEmpty() && clipNames().contains(name);
    }
}
