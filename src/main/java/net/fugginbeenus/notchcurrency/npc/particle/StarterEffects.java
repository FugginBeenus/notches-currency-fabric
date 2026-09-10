package net.fugginbeenus.notchcurrency.npc.particle;

import java.util.ArrayList;
import java.util.List;

public final class StarterEffects {

    private StarterEffects() {}

    private static final int GOLD = 0xFFD24B, ORANGE = 0xFFA24B, PURPLE = 0xB07BFF, TEAL = 0x4BD8C0, DARK = 0x5A6166;

    public static List<ParticleEffect> all() {
        List<ParticleEffect> out = new ArrayList<>();
        out.add(wizard());
        out.add(ghost());
        out.add(holy());
        out.add(cursed());
        return out;
    }

    private static ParticleLayer layer(ParticleEffect fx, String particle, boolean solid, int shape, float size,
                                       int anchor, int count, int life, int rate) {
        ParticleLayer l = fx.addLayer();
        l.setParticle(particle);
        l.setSolid(solid);
        l.setShape(shape);
        l.setSize(size);
        l.setAnchor(anchor);
        l.setCount(count);
        l.setLife(life);
        l.setRate(rate);
        return l;
    }

    private static ParticleEffect wizard() {
        ParticleEffect fx = new ParticleEffect("wizard");

        ParticleLayer ring = layer(fx, "notchcurrency:glyph", true, ParticleLayer.SHAPE_RING, 1.0f,
                ParticleLayer.AT_CHEST, 18, 1, 3);
        ring.setMotion(ParticleLayer.MOVE_SPIN);
        ring.setMotionSpeed(0.4f);
        ring.setHold(8);
        ring.setColour(TEAL);

        ParticleLayer right = layer(fx, "notchcurrency:spark", false, ParticleLayer.SHAPE_BALL, 0.2f,
                ParticleLayer.AT_RIGHT_HAND, 5, 0, 8);
        right.setSpeed(0.02f);
        right.setSpread(0.0f, 0.0f, 0.0f);
        right.setHold(5);
        right.setColour(ORANGE);
        right.setNudge(0.10f, -0.65f, 0.0f);

        ParticleLayer left = layer(fx, "notchcurrency:spark", false, ParticleLayer.SHAPE_BALL, 0.2f,
                ParticleLayer.AT_LEFT_HAND, 5, 0, 8);
        left.setSpeed(0.02f);
        left.setSpread(0.0f, 0.0f, 0.0f);
        left.setHold(6);
        left.setColour(ORANGE);
        left.setNudge(0.05f, -0.65f, 0.0f);
        return fx;
    }

    private static ParticleEffect ghost() {
        ParticleEffect fx = new ParticleEffect("ghost");

        ParticleLayer wisp = layer(fx, "minecraft:sculk_soul", false, ParticleLayer.SHAPE_BALL, 0.55f,
                ParticleLayer.AT_HEAD, 1, 10, 18);
        wisp.setSpread(0.1f, 0.1f, 0.1f);
        wisp.setMotion(ParticleLayer.MOVE_RISE);
        wisp.setMotionSpeed(0.2f);
        wisp.setNudge(0.0f, 0.05f, 0.0f);

        ParticleLayer pool = layer(fx, "minecraft:smoke", true, ParticleLayer.SHAPE_RING, 0.35f,
                ParticleLayer.AT_FEET, 7, 0, 3);
        pool.setMotion(ParticleLayer.MOVE_SPIN);
        pool.setMotionSpeed(0.2f);
        pool.setNudge(0.0f, -0.05f, 0.0f);
        return fx;
    }

    private static ParticleEffect holy() {
        ParticleEffect fx = new ParticleEffect("holy");

        ParticleLayer halo = layer(fx, "notchcurrency:glow", true, ParticleLayer.SHAPE_RING, 0.32f,
                ParticleLayer.AT_HEAD, 11, 1, 3);
        halo.setMotion(ParticleLayer.MOVE_SPIN);
        halo.setMotionSpeed(0.2f);
        halo.setHold(10);
        halo.setColour(GOLD);
        halo.setDustSize(0.8f);
        halo.setNudge(0.0f, 0.10f, 0.0f);

        ParticleLayer ring = layer(fx, "minecraft:end_rod", true, ParticleLayer.SHAPE_RING, 0.75f,
                ParticleLayer.AT_FEET, 10, 0, 3);
        ring.setMotion(ParticleLayer.MOVE_SPIN);
        ring.setMotionSpeed(0.2f);
        ring.setNudge(0.0f, -0.05f, 0.0f);
        return fx;
    }

    private static ParticleEffect cursed() {
        ParticleEffect fx = new ParticleEffect("cursed");

        ParticleLayer shell = layer(fx, "minecraft:dust_color_transition", true, ParticleLayer.SHAPE_BALL, 1.25f,
                ParticleLayer.AT_CHEST, 11, 10, 3);
        shell.setMotion(ParticleLayer.MOVE_SPIN);
        shell.setMotionSpeed(0.6f);
        shell.setColour(PURPLE);
        shell.setColourTo(DARK);
        shell.setDustSize(0.7f);
        shell.setNudge(0.0f, -0.05f, 0.0f);

        ParticleLayer seep = layer(fx, "minecraft:smoke", false, ParticleLayer.SHAPE_POINT, 0.5f,
                ParticleLayer.AT_FEET, 2, 0, 8);
        seep.setSpeed(0.04f);
        seep.setSpread(0.5f, 0.5f, 0.5f);
        seep.setNudge(0.0f, -0.05f, 0.0f);

        ParticleLayer soul = layer(fx, "minecraft:sculk_soul", false, ParticleLayer.SHAPE_BALL, 1.0f,
                ParticleLayer.AT_CHEST, 1, 0, 25);
        soul.setSpeed(0.04f);
        soul.setSpread(0.3f, 0.3f, 0.3f);
        soul.setMotion(ParticleLayer.MOVE_SPIN);
        soul.setMotionSpeed(3.0f);
        soul.setNudge(-0.30f, -0.10f, 0.0f);
        return fx;
    }
}
