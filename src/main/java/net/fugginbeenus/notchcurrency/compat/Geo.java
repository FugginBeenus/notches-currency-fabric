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

public final class Geo {

    private Geo() {}
    public static void init() {
        //? if <1.21
        software.bernie.geckolib.GeckoLib.initialize();
    }

    public static AnimatableInstanceCache cache(NotchNpcEntity animatable) {
        return GeckoLibUtil.createInstanceCache(animatable);
    }
    public static RawAnimation loop(String animationName) {
        return LOOPS.computeIfAbsent(animationName, name -> RawAnimation.begin().thenLoop(name));
    }

    private static final java.util.Map<String, RawAnimation> LOOPS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final String MODEL_NAME = "notch_npc";

    //? if >=1.21.11 {
    /*public static final net.minecraft.resources.ResourceLocation NPC_ANIMATIONS =
            net.fugginbeenus.notchcurrency.core.NotchCurrency.id("notch_npc");
    *///?} else {
    public static final net.minecraft.resources.ResourceLocation NPC_ANIMATIONS =
            net.fugginbeenus.notchcurrency.core.NotchCurrency.id("animations/notch_npc.animation.json");
    //?}

    public static java.util.List<String> clipNames() {
        return clipNames(NPC_ANIMATIONS, MODEL_NAME);
    }

    /** The clips in one animation file, by id, falling back to a name match. */
    public static java.util.List<String> clipNames(net.minecraft.resources.ResourceLocation file,
                                                   String nameHint) {
        try {
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
            return java.util.List.of();
        }
    }

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

    public static boolean hasBakedAnimations(net.minecraft.resources.ResourceLocation id) {
        try {
            //? if >=26.1 {
            /*var cache = com.geckolib.cache.GeckoLibResources.getBakedAnimations();
            var map = cache == null ? null : cache.cache();
            *///?} elif >=1.21.11 {
            /*var cache = software.bernie.geckolib.cache.GeckoLibResources.getBakedAnimations();
            var map = cache == null ? null : cache.cache();
            *///?} else {
            var map = software.bernie.geckolib.cache.GeckoLibCache.getBakedAnimations();
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

    public static boolean hasClip(String name) {
        return name != null && !name.isEmpty() && clipNames().contains(name);
    }
}
