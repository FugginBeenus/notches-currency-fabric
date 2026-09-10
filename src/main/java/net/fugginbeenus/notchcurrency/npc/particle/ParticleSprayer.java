package net.fugginbeenus.notchcurrency.npc.particle;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class ParticleSprayer {

    private ParticleSprayer() {}

    public static final double WATCH_RANGE = 32.0;
    private static final double WATCH_RANGE_SQ = WATCH_RANGE * WATCH_RANGE;

    public static void tick(ServerLevel level, NotchNpcEntity npc, ParticleEffect effect) {
        if (effect == null || effect.isEmpty() || !anyoneNear(level, npc)) return;
        int time = npc.tickCount;
        for (ParticleLayer layer : effect.layers()) {
            if (!layer.solid() && time % layer.rate() != 0) continue;
            emit(level, npc, layer, time);
        }
    }

    public static void burst(ServerLevel level, NotchNpcEntity npc, ParticleEffect effect) {
        if (effect == null || effect.isEmpty() || !anyoneNear(level, npc)) return;
        for (ParticleLayer layer : effect.layers()) emit(level, npc, layer, npc.tickCount);
    }

    private static boolean anyoneNear(ServerLevel level, NotchNpcEntity npc) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(npc) <= WATCH_RANGE_SQ) return true;
        }
        return false;
    }

    private static void emit(ServerLevel level, NotchNpcEntity npc, ParticleLayer layer, int time) {
        ParticleOptions options = optionsFor(layer);
        if (options == null) return;


        Vec3 base = anchorOf(npc, layer);
        int count = layer.count();
        double phase = spinPhase(time, layer);
        double lift = riseLift(time, layer);

        double sx = layer.solid() ? 0.0 : layer.spreadX();
        double sy = layer.solid() ? 0.0 : layer.spreadY();
        double sz = layer.solid() ? 0.0 : layer.spreadZ();
        double push = layer.solid() ? 0.0 : layer.speed();

        if (layer.shape() == ParticleLayer.SHAPE_POINT) {
            level.sendParticles(options, base.x, base.y + lift, base.z, count, sx, sy, sz, push);
            return;
        }

        RandomSource rng = level.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 off = shapeOffset(layer, i, count, phase, rng);
            level.sendParticles(options, base.x + off.x, base.y + off.y + lift, base.z + off.z, 1,
                    sx, sy, sz, push);
        }
    }

    public static Vec3 shapeOffset(ParticleLayer layer, int i, int count, double phase, RandomSource rng) {
        double size = layer.size();
        switch (layer.shape()) {
            case ParticleLayer.SHAPE_RING -> {
                double a = phase + (Math.PI * 2.0 * i) / Math.max(1, count);
                return new Vec3(Math.cos(a) * size, 0, Math.sin(a) * size);
            }
            case ParticleLayer.SHAPE_BALL -> {
                if (layer.solid()) {
                    double golden = Math.PI * (3.0 - Math.sqrt(5.0));
                    double y = 1.0 - (2.0 * i) / Math.max(1, count - 1);
                    double flat = Math.sqrt(Math.max(0.0, 1.0 - y * y));
                    double a = phase + golden * i;
                    return new Vec3(Math.cos(a) * flat * size, y * size, Math.sin(a) * flat * size);
                }
                double a = rng.nextDouble() * Math.PI * 2.0;
                double z = rng.nextDouble() * 2.0 - 1.0;
                double r = Math.cbrt(rng.nextDouble()) * size;
                double flat = Math.sqrt(1.0 - z * z);
                return new Vec3(Math.cos(a) * flat * r, z * r, Math.sin(a) * flat * r);
            }
            case ParticleLayer.SHAPE_CONE -> {
                double t = layer.solid() ? (double) i / Math.max(1, count) : rng.nextDouble();
                double a = layer.solid()
                        ? phase + t * Math.PI * 6.0
                        : phase + rng.nextDouble() * Math.PI * 2.0;
                double r = size * t;
                return new Vec3(Math.cos(a) * r, size * t * 2.0, Math.sin(a) * r);
            }
            case ParticleLayer.SHAPE_COLUMN -> {
                double t = (double) i / Math.max(1, count);
                double a = layer.solid()
                        ? phase + t * Math.PI * 4.0
                        : phase + rng.nextDouble() * Math.PI * 2.0;
                double r = size * 0.15;
                return new Vec3(Math.cos(a) * r, size * t, Math.sin(a) * r);
            }
            case ParticleLayer.SHAPE_SPIRAL -> {
                double t = (double) i / Math.max(1, count);
                double a = phase + t * Math.PI * 4.0;
                double r = size * 0.5;
                return new Vec3(Math.cos(a) * r, size * t, Math.sin(a) * r);
            }
            default -> {
                return Vec3.ZERO;
            }
        }
    }

    public static Vec3 localAnchor(float bodyHeight, ParticleLayer layer) {
        float tall = Math.max(0.4f, bodyHeight);
        double side = 0.36 * (tall / 1.8);
        double localX = 0.0;
        double localY;
        switch (layer.anchor()) {
            case ParticleLayer.AT_FEET -> localY = 0.05;
            case ParticleLayer.AT_HEAD -> localY = tall;
            case ParticleLayer.AT_LEFT_HAND -> { localX = side; localY = tall * 0.72; }
            case ParticleLayer.AT_RIGHT_HAND -> { localX = -side; localY = tall * 0.72; }
            default -> localY = tall * 0.62;
        }
        return new Vec3(localX + layer.nudgeX(), localY + layer.nudgeY(), layer.nudgeZ());
    }

    public static double spinPhase(int time, ParticleLayer layer) {
        return layer.motion() == ParticleLayer.MOVE_SPIN ? time * layer.motionSpeed() * 0.08 : 0.0;
    }

    public static double riseLift(int time, ParticleLayer layer) {
        if (layer.motion() != ParticleLayer.MOVE_RISE) return 0.0;
        double span = Math.max(0.2, layer.size());
        return (time * layer.motionSpeed() * 0.03) % span;
    }

    private static Vec3 anchorOf(NotchNpcEntity npc, ParticleLayer layer) {
        Vec3 local = localAnchor(npc.getBbHeight(), layer);
        Vec3 flat = new Vec3(local.x, 0.0, local.z)
                .yRot(-npc.yBodyRot * ((float) Math.PI / 180.0f));
        return new Vec3(npc.getX() + flat.x, npc.getY() + local.y, npc.getZ() + flat.z);
    }

    public static ParticleOptions optionsFor(ParticleLayer layer) {
        String raw = layer.particle();
        ResourceLocation id = ResourceLocation.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
        if (id == null) return null;

        if (layer.takesSecondColour()) {
            return fadingDust(layer.colour(), layer.colourTo(), layer.dustSize());
        }
        if (layer.takesColour()) {
            return plainDust(layer.colour(), layer.dustSize());
        }

        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(id);
        return type instanceof SimpleParticleType simple ? simple : null;
    }

    private static ParticleOptions plainDust(int rgb, float size) {
        int colour = rgb == -1 ? 0xFFFFFF : rgb;
        //? if >=1.21.11 {
        /*return new net.minecraft.core.particles.DustParticleOptions(colour, size);
        *///?} else {
        return new net.minecraft.core.particles.DustParticleOptions(toVector(colour), size);
        //?}
    }

    private static ParticleOptions fadingDust(int from, int to, float size) {
        int a = from == -1 ? 0xFFFFFF : from;
        int b = to == -1 ? a : to;
        //? if >=1.21.11 {
        /*return new net.minecraft.core.particles.DustColorTransitionOptions(a, b, size);
        *///?} else {
        return new net.minecraft.core.particles.DustColorTransitionOptions(toVector(a), toVector(b), size);
        //?}
    }

    //? if <1.21.11 {
    private static org.joml.Vector3f toVector(int rgb) {
        return new org.joml.Vector3f(
                ((rgb >> 16) & 0xFF) / 255.0f,
                ((rgb >> 8) & 0xFF) / 255.0f,
                (rgb & 0xFF) / 255.0f);
    }
    //?}
}
