package net.fugginbeenus.notchcurrency.client.particle;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.particle.ParticleEffect;
import net.fugginbeenus.notchcurrency.npc.particle.ParticleLayer;
import net.fugginbeenus.notchcurrency.npc.particle.ParticleSprayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class NpcFxRenderer {

    private NpcFxRenderer() {}

    private static final double RANGE_SQ = 32.0 * 32.0;
    private static final int BUDGET_PER_TICK = 600;
    private static final RandomSource RNG = RandomSource.create();
    private static int clock;
    private static int spentThisTick;

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.particleEngine == null) return;
        if (mc.isPaused()) return;
        clock++;
        spentThisTick = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof NotchNpcEntity npc)) continue;
            String name = npc.getParticleFx();
            if (name.isEmpty()) continue;
            if (mc.player.distanceToSqr(npc) > RANGE_SQ) continue;
            ParticleEffect fx = ParticleLibrary.get(name);
            if (fx == null || fx.isEmpty()) continue;
            play(mc, npc, fx, clock);
        }
    }

    public static void play(Minecraft mc, NotchNpcEntity npc, ParticleEffect fx, int time) {
        for (ParticleLayer layer : fx.layers()) {
            if (!layer.solid() && time % layer.rate() != 0) continue;
            emit(mc, npc, layer, time);
        }
    }

    public static void burst(Minecraft mc, NotchNpcEntity npc, ParticleEffect fx) {
        for (ParticleLayer layer : fx.layers()) emit(mc, npc, layer, clock);
    }

    private static void emit(Minecraft mc, NotchNpcEntity npc, ParticleLayer layer, int time) {
        ParticleOptions options = ParticleSprayer.optionsFor(layer);
        if (options == null) return;

        Vec3 local = ParticleSprayer.localAnchor(npc.getBbHeight(), layer);
        Vec3 flat = new Vec3(local.x, 0.0, local.z)
                .yRot(-npc.yBodyRot * ((float) Math.PI / 180.0f));
        double bx = npc.getX() + flat.x;
        double by = npc.getY() + local.y;
        double bz = npc.getZ() + flat.z;

        double phase = ParticleSprayer.spinPhase(time, layer);
        double lift = ParticleSprayer.riseLift(time, layer);
        boolean solid = layer.solid();
        double push = solid ? 0.0 : layer.speed();
        double sx = solid ? 0.0 : layer.spreadX();
        double sy = solid ? 0.0 : layer.spreadY();
        double sz = solid ? 0.0 : layer.spreadZ();

        boolean ours = layer.isOurs();
        int tint = layer.colour();
        if (tint == -1) tint = ParticleLayer.defaultTint(layer.particle());

        int count = layer.count();
        for (int i = 0; i < count; i++) {
            if (spentThisTick >= BUDGET_PER_TICK) return;
            spentThisTick++;
            if (ours) {
                TameParticle.hintIndex = i;
                TameParticle.hintTime = time;
                TameParticle.hintHold = layer.hold();
                TameParticle.hintTint = tint == ParticleLayer.RAINBOW
                        ? rainbow(i, count, time) : tint;
            }
            Vec3 off = layer.shape() == ParticleLayer.SHAPE_POINT
                    ? Vec3.ZERO
                    : ParticleSprayer.shapeOffset(layer, i, count, phase, RNG);
            double jx = sx == 0 ? 0 : RNG.nextGaussian() * sx;
            double jy = sy == 0 ? 0 : RNG.nextGaussian() * sy;
            double jz = sz == 0 ? 0 : RNG.nextGaussian() * sz;
            double vx = push == 0 ? 0 : RNG.nextGaussian() * push;
            double vy = push == 0 ? 0 : RNG.nextGaussian() * push;
            double vz = push == 0 ? 0 : RNG.nextGaussian() * push;

            Particle p = mc.particleEngine.createParticle(options,
                    bx + off.x + jx, by + off.y + lift + jy, bz + off.z + jz, vx, vy, vz);
            if (p == null) continue;
            if (layer.life() > 0) p.setLifetime(layer.life() - 1);
            if (solid) p.setParticleSpeed(0.0, 0.0, 0.0);
        }
        TameParticle.hintTint = -1;
    }

    private static int rainbow(int i, int count, int time) {
        float hue = ((float) i / Math.max(1, count) + time * 0.005f) % 1.0f;
        return net.minecraft.util.Mth.hsvToRgb(hue, 0.85f, 1.0f) & 0xFFFFFF;
    }
}
