package net.fugginbeenus.notchcurrency.npc.particle;

import net.minecraft.nbt.CompoundTag;

public class ParticleLayer {

    public static final int SHAPE_POINT = 0, SHAPE_RING = 1, SHAPE_BALL = 2,
            SHAPE_CONE = 3, SHAPE_COLUMN = 4, SHAPE_SPIRAL = 5;
    public static final String[] SHAPE_NAMES = {"Point", "Ring", "Ball", "Cone", "Column", "Spiral"};

    public static final int AT_FEET = 0, AT_CHEST = 1, AT_HEAD = 2, AT_LEFT_HAND = 3, AT_RIGHT_HAND = 4;
    public static final String[] ANCHOR_NAMES = {"Feet", "Chest", "Head", "Left hand", "Right hand"};

    public static final int MOVE_STILL = 0, MOVE_SPIN = 1, MOVE_RISE = 2;
    public static final String[] MOTION_NAMES = {"Still", "Spin", "Rise"};

    public static final int MAX_COUNT = 200;
    public static final int MAX_SOLID_COUNT = 48;
    private static final java.util.Set<String> WANDERERS = java.util.Set.of(
            "minecraft:enchant", "minecraft:nautilus", "minecraft:portal",
            "minecraft:reverse_portal", "minecraft:note", "minecraft:elder_guardian",
            "minecraft:sculk_charge_pop", "minecraft:shriek");

    public static boolean goesItsOwnWay(String particle) {
        if (particle == null) return false;
        String id = particle.contains(":") ? particle : "minecraft:" + particle;
        return WANDERERS.contains(id.trim().toLowerCase());
    }

    public boolean goesItsOwnWay() { return goesItsOwnWay(particle); }

    private static final java.util.Map<String, String> TAME_TWINS = java.util.Map.of(
            "minecraft:enchant", "notchcurrency:glyph",
            "minecraft:nautilus", "notchcurrency:glyph",
            "minecraft:portal", "notchcurrency:portal",
            "minecraft:reverse_portal", "notchcurrency:portal",
            "minecraft:note", "notchcurrency:note",
            "minecraft:end_rod", "notchcurrency:spark",
            "minecraft:witch", "notchcurrency:spell",
            "minecraft:glow", "notchcurrency:glow");

    public static String tameTwin(String particle) {
        if (particle == null) return null;
        String id = particle.contains(":") ? particle : "minecraft:" + particle;
        return TAME_TWINS.get(id.trim().toLowerCase());
    }

    public String tameTwin() { return tameTwin(particle); }

    public static final int RAINBOW = -2;

    public static boolean isOurs(String particle) {
        return particle != null && particle.trim().toLowerCase().startsWith("notchcurrency:");
    }

    public boolean isOurs() { return isOurs(particle); }

    public static int defaultTint(String particle) {
        String id = particle == null ? "" : particle.trim().toLowerCase();
        if (id.equals("notchcurrency:portal")) return 0xB07BFF;
        return -1;
    }

    public boolean takesTint() {
        return takesColour() || isOurs();
    }

    public boolean takesHold() {
        return isOurs();
    }

    public static final String DUST = "minecraft:dust";
    public static final String DUST_FADE = "minecraft:dust_color_transition";

    private String particle = "minecraft:flame";
    private int colour = -1;
    private int colourTo = -1;
    private float dustSize = 1.0f;
    private int count = 16;
    private int rate = 3;
    private int shape = SHAPE_POINT;
    private float size = 0.5f;
    private int anchor = AT_CHEST;
    private float nudgeX, nudgeY, nudgeZ;
    private float speed = 0.0f;
    private float spreadX = 0.1f, spreadY = 0.1f, spreadZ = 0.1f;
    private boolean solid = true;
    private int life = 1;
    private int hold = 4;
    private int motion = MOVE_STILL;
    private float motionSpeed = 1.0f;

    public String particle() { return particle; }
    public void setParticle(String id) { this.particle = id == null || id.isBlank() ? "minecraft:flame" : id.trim(); }

    public boolean takesColour() {
        String id = particle.startsWith("minecraft:") ? particle : "minecraft:" + particle;
        return id.equals(DUST) || id.equals(DUST_FADE);
    }

    public boolean takesSecondColour() {
        String id = particle.startsWith("minecraft:") ? particle : "minecraft:" + particle;
        return id.equals(DUST_FADE);
    }

    public int colour() { return colour; }
    public void setColour(int rgb) { this.colour = rgb; }

    public int colourTo() { return colourTo; }
    public void setColourTo(int rgb) { this.colourTo = rgb; }

    public float dustSize() { return dustSize; }
    public void setDustSize(float v) { this.dustSize = clampF(v, 0.1f, 4.0f); }

    public boolean solid() { return solid; }
    public void setSolid(boolean v) {
        this.solid = v;
        if (v) this.count = Math.min(this.count, MAX_SOLID_COUNT);
    }

    public int life() { return life; }
    public void setLife(int ticks) { this.life = Math.max(0, Math.min(200, ticks)); }

    public int hold() { return hold; }
    public void setHold(int ticks) { this.hold = Math.max(1, Math.min(100, ticks)); }

    public int count() { return count; }
    public void setCount(int v) {
        this.count = Math.max(1, Math.min(solid ? MAX_SOLID_COUNT : MAX_COUNT, v));
    }

    public int rate() { return rate; }
    public void setRate(int ticks) { this.rate = Math.max(1, Math.min(600, ticks)); }

    public int shape() { return shape; }
    public void setShape(int v) { this.shape = Math.floorMod(v, SHAPE_NAMES.length); }

    public float size() { return size; }
    public void setSize(float v) { this.size = clampF(v, 0.0f, 8.0f); }

    public int anchor() { return anchor; }
    public void setAnchor(int v) { this.anchor = Math.floorMod(v, ANCHOR_NAMES.length); }

    public float nudgeX() { return nudgeX; }
    public float nudgeY() { return nudgeY; }
    public float nudgeZ() { return nudgeZ; }
    public void setNudge(float x, float y, float z) {
        this.nudgeX = clampF(x, -4.0f, 4.0f);
        this.nudgeY = clampF(y, -4.0f, 4.0f);
        this.nudgeZ = clampF(z, -4.0f, 4.0f);
    }

    public float speed() { return speed; }
    public void setSpeed(float v) { this.speed = clampF(v, 0.0f, 2.0f); }

    public float spreadX() { return spreadX; }
    public float spreadY() { return spreadY; }
    public float spreadZ() { return spreadZ; }
    public void setSpread(float x, float y, float z) {
        this.spreadX = clampF(x, 0.0f, 4.0f);
        this.spreadY = clampF(y, 0.0f, 4.0f);
        this.spreadZ = clampF(z, 0.0f, 4.0f);
    }

    public int motion() { return motion; }
    public void setMotion(int v) { this.motion = Math.floorMod(v, MOTION_NAMES.length); }

    public float motionSpeed() { return motionSpeed; }
    public void setMotionSpeed(float v) { this.motionSpeed = clampF(v, 0.0f, 8.0f); }

    private static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public ParticleLayer copy() {
        ParticleLayer c = new ParticleLayer();
        c.particle = particle;
        c.colour = colour;
        c.colourTo = colourTo;
        c.dustSize = dustSize;
        c.count = count;
        c.rate = rate;
        c.shape = shape;
        c.size = size;
        c.anchor = anchor;
        c.nudgeX = nudgeX; c.nudgeY = nudgeY; c.nudgeZ = nudgeZ;
        c.speed = speed;
        c.spreadX = spreadX; c.spreadY = spreadY; c.spreadZ = spreadZ;
        c.solid = solid;
        c.life = life;
        c.hold = hold;
        c.motion = motion;
        c.motionSpeed = motionSpeed;
        return c;
    }

    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        t.putString("P", particle);
        if (colour != -1) t.putInt("C", colour);
        if (colourTo != -1) t.putInt("C2", colourTo);
        if (dustSize != 1.0f) t.putFloat("DS", dustSize);
        t.putInt("N", count);
        t.putInt("R", rate);
        t.putInt("SH", shape);
        t.putFloat("SZ", size);
        t.putInt("A", anchor);
        if (nudgeX != 0) t.putFloat("NX", nudgeX);
        if (nudgeY != 0) t.putFloat("NY", nudgeY);
        if (nudgeZ != 0) t.putFloat("NZ", nudgeZ);
        t.putFloat("SP", speed);
        t.putFloat("DX", spreadX);
        t.putFloat("DY", spreadY);
        t.putFloat("DZ", spreadZ);
        t.putBoolean("SO", solid);
        t.putInt("LF", life);
        if (hold != 4) t.putInt("HD", hold);
        t.putInt("M", motion);
        t.putFloat("MS", motionSpeed);
        return t;
    }

    public static ParticleLayer fromNbt(CompoundTag t) {
        ParticleLayer l = new ParticleLayer();
        l.setParticle(t.getString("P"));
        l.colour = t.contains("C") ? t.getInt("C") : -1;
        l.colourTo = t.contains("C2") ? t.getInt("C2") : -1;
        l.setDustSize(t.contains("DS") ? t.getFloat("DS") : 1.0f);
        l.setCount(t.contains("N") ? t.getInt("N") : 16);
        l.setRate(t.contains("R") ? t.getInt("R") : 3);
        l.setShape(t.getInt("SH"));
        l.setSize(t.contains("SZ") ? t.getFloat("SZ") : 0.5f);
        l.setAnchor(t.getInt("A"));
        l.setNudge(t.getFloat("NX"), t.getFloat("NY"), t.getFloat("NZ"));
        l.setSpeed(t.getFloat("SP"));
        l.setSpread(
                t.contains("DX") ? t.getFloat("DX") : 0.1f,
                t.contains("DY") ? t.getFloat("DY") : 0.1f,
                t.contains("DZ") ? t.getFloat("DZ") : 0.1f);
        l.setSolid(!t.contains("SO") || t.getBoolean("SO"));
        l.setLife(t.contains("LF") ? t.getInt("LF") : 1);
        l.setHold(t.contains("HD") ? t.getInt("HD") : 4);
        l.setMotion(t.getInt("M"));
        l.setMotionSpeed(t.contains("MS") ? t.getFloat("MS") : 1.0f);
        return l;
    }
}
