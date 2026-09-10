package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

public final class ModParticles {

    private ModParticles() {}

    public static final int CUSTOM_SLOTS = 8;

    public static SimpleParticleType GLYPH, PORTAL, NOTE, SPARK, SPELL, GLOW, DOT;
    public static final List<SimpleParticleType> CUSTOM = new ArrayList<>();

    public static void register() {
        GLYPH = simple("glyph");
        PORTAL = simple("portal");
        NOTE = simple("note");
        SPARK = simple("spark");
        SPELL = simple("spell");
        GLOW = simple("glow");
        DOT = simple("dot");
        for (int i = 1; i <= CUSTOM_SLOTS; i++) CUSTOM.add(simple("custom_" + i));
    }

    private static SimpleParticleType simple(String path) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, NotchCurrency.id(path),
                FabricParticleTypes.simple());
    }
}
