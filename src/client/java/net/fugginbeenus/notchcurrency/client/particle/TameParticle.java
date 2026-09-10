package net.fugginbeenus.notchcurrency.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

//? if >=1.21.11 {
/*public class TameParticle extends net.minecraft.client.particle.SingleQuadParticle {
*///?} else {
public class TameParticle extends net.minecraft.client.particle.TextureSheetParticle {
//?}

    public static int hintTint = -1;
    public static int hintIndex = 0;
    public static int hintTime = 0;
    public static int hintHold = 4;

    private final SpriteSet sprites;
    private final boolean animated;
    private final int index;
    private final int hold;
    private int clock;

    private static RandomSource seeded(int index, int time, int hold) {
        return RandomSource.create(index * 7919L + (time / Math.max(1, hold)) * 104729L);
    }

    //? if >=1.21.11 {
    /*protected TameParticle(ClientLevel level, double x, double y, double z,
                           double vx, double vy, double vz, SpriteSet sprites, boolean animated) {
        super(level, x, y, z, animated
                ? sprites.get(hintTime % Math.max(1, hintHold), Math.max(1, hintHold))
                : sprites.get(seeded(hintIndex, hintTime, hintHold)));
    *///?} else {
    protected TameParticle(ClientLevel level, double x, double y, double z,
                           double vx, double vy, double vz, SpriteSet sprites, boolean animated) {
        super(level, x, y, z);
        setSprite(animated
                ? sprites.get(hintTime % Math.max(1, hintHold), Math.max(1, hintHold))
                : sprites.get(seeded(hintIndex, hintTime, hintHold)));
    //?}
        this.sprites = sprites;
        this.animated = animated;
        this.index = hintIndex;
        this.hold = hintHold;
        this.clock = hintTime;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = 20;
        this.quadSize = 0.1f;
        if (hintTint != -1) {
            this.rCol = ((hintTint >> 16) & 0xFF) / 255.0f;
            this.gCol = ((hintTint >> 8) & 0xFF) / 255.0f;
            this.bCol = (hintTint & 0xFF) / 255.0f;
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        clock++;
        if (animated) {
            setSprite(sprites.get(clock % Math.max(1, hold), Math.max(1, hold)));
        } else if (clock % Math.max(1, hold) == 0) {
            setSprite(sprites.get(seeded(index, clock, hold)));
        }
    }

    //? if >=1.21.11 {
    /*@Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }
    *///?} else {
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    //?}

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean animated;

        public Provider(SpriteSet sprites, boolean animated) {
            this.sprites = sprites;
            this.animated = animated;
        }

        //? if >=1.21.11 {
        /*@Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource random) {
            return new TameParticle(level, x, y, z, vx, vy, vz, sprites, animated);
        }
        *///?} else {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new TameParticle(level, x, y, z, vx, vy, vz, sprites, animated);
        }
        //?}
    }
}
