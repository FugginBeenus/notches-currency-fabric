package net.fugginbeenus.notchcurrency.npc.particle;

import java.util.ArrayList;
import java.util.List;

public final class StarterEffects {

    private StarterEffects() {}

    private static final int GOLD = 0xFFD24B, PURPLE = 0xB07BFF, TEAL = 0x4BD8C0, DARK = 0x5A6166;

    public static List<ParticleEffect> all() {
        List<ParticleEffect> out = new ArrayList<>();
        out.add(wizard());
        out.add(ghost());
        out.add(blacksmith());
        out.add(holy());
        out.add(cursed());
        out.add(hearth());
        return out;
    }

    private static ParticleLayer add(ParticleEffect fx, String particle, int shape, int anchor) {
        ParticleLayer l = fx.addLayer();
        l.setParticle(particle);
        l.setShape(shape);
        l.setAnchor(anchor);
        return l;
    }

    private static ParticleEffect wizard() {
        ParticleEffect fx = new ParticleEffect("wizard");
        ParticleLayer ring = add(fx, "notchcurrency:glyph", ParticleLayer.SHAPE_RING, ParticleLayer.AT_CHEST);
        ring.setSolid(true);
        ring.setCount(18);
        ring.setSize(0.7f);
        ring.setMotion(ParticleLayer.MOVE_SPIN);
        ring.setMotionSpeed(2.0f);
        ring.setLife(1);

        ParticleLayer wand = add(fx, "notchcurrency:spark", ParticleLayer.SHAPE_POINT, ParticleLayer.AT_RIGHT_HAND);
        wand.setCount(1);
        wand.setRate(6);
        wand.setLife(0);
        wand.setSpread(0.03f, 0.03f, 0.03f);
        return fx;
    }

    private static ParticleEffect ghost() {
        ParticleEffect fx = new ParticleEffect("ghost");
        ParticleLayer wisp = add(fx, "minecraft:soul", ParticleLayer.SHAPE_BALL, ParticleLayer.AT_CHEST);
        wisp.setCount(1);
        wisp.setRate(4);
        wisp.setSize(0.45f);
        wisp.setMotion(ParticleLayer.MOVE_RISE);
        wisp.setMotionSpeed(0.6f);
        wisp.setLife(0);
        wisp.setSpread(0.05f, 0.05f, 0.05f);

        ParticleLayer pool = add(fx, "minecraft:smoke", ParticleLayer.SHAPE_RING, ParticleLayer.AT_FEET);
        pool.setSolid(true);
        pool.setCount(12);
        pool.setSize(0.55f);
        pool.setLife(1);
        return fx;
    }

    private static ParticleEffect blacksmith() {
        ParticleEffect fx = new ParticleEffect("blacksmith");
        ParticleLayer sparks = add(fx, "minecraft:crit", ParticleLayer.SHAPE_CONE, ParticleLayer.AT_RIGHT_HAND);
        sparks.setCount(10);
        sparks.setRate(60);
        sparks.setSize(0.3f);
        sparks.setSpeed(0.06f);
        sparks.setLife(0);

        ParticleLayer forge = add(fx, "minecraft:smoke", ParticleLayer.SHAPE_COLUMN, ParticleLayer.AT_FEET);
        forge.setCount(1);
        forge.setRate(10);
        forge.setSize(1.1f);
        forge.setLife(0);
        return fx;
    }

    private static ParticleEffect holy() {
        ParticleEffect fx = new ParticleEffect("holy");
        ParticleLayer halo = add(fx, "minecraft:dust", ParticleLayer.SHAPE_RING, ParticleLayer.AT_HEAD);
        halo.setSolid(true);
        halo.setCount(14);
        halo.setSize(0.32f);
        halo.setColour(GOLD);
        halo.setDustSize(0.8f);
        halo.setMotion(ParticleLayer.MOVE_SPIN);
        halo.setMotionSpeed(1.4f);
        halo.setLife(1);

        ParticleLayer ring = add(fx, "minecraft:end_rod", ParticleLayer.SHAPE_RING, ParticleLayer.AT_FEET);
        ring.setSolid(true);
        ring.setCount(16);
        ring.setSize(0.75f);
        ring.setMotion(ParticleLayer.MOVE_SPIN);
        ring.setMotionSpeed(0.8f);
        ring.setLife(1);
        return fx;
    }

    private static ParticleEffect cursed() {
        ParticleEffect fx = new ParticleEffect("cursed");
        ParticleLayer shell = add(fx, "minecraft:dust_color_transition",
                ParticleLayer.SHAPE_BALL, ParticleLayer.AT_CHEST);
        shell.setSolid(true);
        shell.setCount(22);
        shell.setSize(0.55f);
        shell.setColour(PURPLE);
        shell.setColourTo(DARK);
        shell.setDustSize(1.0f);
        shell.setMotion(ParticleLayer.MOVE_SPIN);
        shell.setMotionSpeed(1.6f);
        shell.setLife(1);

        ParticleLayer seep = add(fx, "minecraft:smoke", ParticleLayer.SHAPE_POINT, ParticleLayer.AT_FEET);
        seep.setCount(1);
        seep.setRate(8);
        seep.setLife(0);
        seep.setSpread(0.2f, 0.02f, 0.2f);
        return fx;
    }

    private static ParticleEffect hearth() {
        ParticleEffect fx = new ParticleEffect("hearth");
        ParticleLayer smoke = add(fx, "minecraft:campfire_cosy_smoke",
                ParticleLayer.SHAPE_COLUMN, ParticleLayer.AT_FEET);
        smoke.setCount(1);
        smoke.setRate(14);
        smoke.setSize(1.3f);
        smoke.setMotion(ParticleLayer.MOVE_RISE);
        smoke.setMotionSpeed(0.8f);
        smoke.setLife(0);

        ParticleLayer glow = add(fx, "minecraft:dust", ParticleLayer.SHAPE_RING, ParticleLayer.AT_FEET);
        glow.setSolid(true);
        glow.setCount(10);
        glow.setSize(0.4f);
        glow.setColour(TEAL);
        glow.setDustSize(0.7f);
        glow.setLife(1);
        return fx;
    }
}
