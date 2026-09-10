package net.fugginbeenus.notchcurrency.client.particle;

import net.fugginbeenus.notchcurrency.registry.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;

public final class ModParticleFactories {

    private ModParticleFactories() {}

    public static void register() {
        still(ModParticles.GLYPH);
        still(ModParticles.PORTAL);
        still(ModParticles.NOTE);
        animated(ModParticles.SPARK);
        animated(ModParticles.SPELL);
        still(ModParticles.GLOW);
        still(ModParticles.DOT);
        for (SimpleParticleType slot : ModParticles.CUSTOM) still(slot);
    }

    private static void still(SimpleParticleType type) {
        //? if >=26.1 {
        /*net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(type,
                sprites -> new TameParticle.Provider(sprites, false));
        *///?} else {
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(type,
                sprites -> new TameParticle.Provider(sprites, false));
        //?}
    }

    private static void animated(SimpleParticleType type) {
        //? if >=26.1 {
        /*net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(type,
                sprites -> new TameParticle.Provider(sprites, true));
        *///?} else {
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(type,
                sprites -> new TameParticle.Provider(sprites, true));
        //?}
    }
}
