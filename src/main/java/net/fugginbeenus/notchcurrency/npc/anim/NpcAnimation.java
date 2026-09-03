package net.fugginbeenus.notchcurrency.npc.anim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class NpcAnimation {

    public static final int PARTS = 6;
    public static final int SLOTS = PARTS * 3;
    public static final int WIDE = SLOTS * 2;
    public static final int MAX_FRAMES = 16;

    public static class Frame {
        public final int[] angles = new int[SLOTS];
        public final int[] offsets = new int[SLOTS];
        public long set = 0L;
        public int holdTicks = 10;

        public Frame() {}

        public Frame(int[] rot, int[] move, int hold) {
            if (rot != null) System.arraycopy(rot, 0, angles, 0, Math.min(SLOTS, rot.length));
            if (move != null) System.arraycopy(move, 0, offsets, 0, Math.min(SLOTS, move.length));
            this.holdTicks = Math.max(1, hold);
        }

        public boolean has(int slot) { return (set & (1L << slot)) != 0L; }

        public void mark(int slot) { set |= (1L << slot); }

        public void clear(int slot) { set &= ~(1L << slot); }

        public int value(int slot) {
            return slot < SLOTS ? angles[slot] : offsets[slot - SLOTS];
        }

        public void put(int slot, int v) {
            if (slot < SLOTS) angles[slot] = v; else offsets[slot - SLOTS] = v;
            mark(slot);
        }

        public Frame copy() {
            Frame f = new Frame(angles, offsets, holdTicks);
            f.set = this.set;
            return f;
        }
    }

    private String name;
    private boolean loop = true;
    private boolean smooth = true;
    private int speedPercent = 100;
    private int lengthTicks = 40;
    private final List<Frame> frames = new ArrayList<>();

    public NpcAnimation(String name) {
        this.name = name == null ? "" : name;
    }

    public String name() { return name; }
    public void setName(String n) { this.name = n == null ? "" : n; }
    public boolean loop() { return loop; }
    public void setLoop(boolean on) { this.loop = on; }
    public boolean smooth() { return smooth; }
    public void setSmooth(boolean on) { this.smooth = on; }
    public int speedPercent() { return speedPercent; }
    public void setSpeedPercent(int p) { this.speedPercent = Math.max(25, Math.min(400, p)); }
    public int lengthTicks() { return lengthTicks; }
    public void setLengthTicks(int t) { this.lengthTicks = Math.max(4, Math.min(600, t)); }
    public List<Frame> frames() { return frames; }

    public int totalTicks() { return lengthTicks; }

    public float segmentTicks() {
        return lengthTicks / (float) Math.max(1, frames.size());
    }

    public String summary() {
        return frames.size() + " frame" + (frames.size() == 1 ? "" : "s")
                + ", " + String.format("%.1f", lengthTicks / 20.0f) + "s"
                + (loop ? ", loops" : "");
    }

    public float[] sample(float ticks) {
        if (frames.isEmpty()) return null;
        float scaled = ticks * (speedPercent / 100.0f);
        int total = totalTicks();
        float t = loop ? ((scaled % total) + total) % total : Math.min(scaled, total - 0.001f);
        float span = segmentTicks();
        float[] out = new float[WIDE];
        for (int slot = 0; slot < WIDE; slot++) out[slot] = channel(slot, t, span, total);
        return out;
    }

    private float channel(int slot, float t, float span, int total) {
        int n = frames.size();
        int at = Math.min(n - 1, (int) (t / span));

        int prev = -1;
        for (int step = 0; step < n; step++) {
            int i = at - step;
            if (i < 0) {
                if (!loop) break;
                i += n;
            }
            if (frames.get(i).has(slot)) { prev = i; break; }
        }
        int next = -1;
        for (int step = 1; step <= n; step++) {
            int i = at + step;
            if (i >= n) {
                if (!loop) break;
                i -= n;
            }
            if (frames.get(i).has(slot)) { next = i; break; }
        }

        if (prev < 0 && next < 0) return 0f;
        if (prev < 0) return frames.get(next).value(slot);
        if (next < 0 || !smooth || prev == next) return frames.get(prev).value(slot);

        float prevT = prev * span;
        if (prevT > t) prevT -= total;
        float nextT = next * span;
        if (nextT <= prevT) nextT += total;
        float gap = nextT - prevT;
        if (gap <= 0.001f) return frames.get(prev).value(slot);
        float mix = Math.max(0f, Math.min(1f, (t - prevT) / gap));
        int a = frames.get(prev).value(slot), b = frames.get(next).value(slot);
        return a + (b - a) * mix;
    }

    public static Frame frameOf(float[] wide) {
        Frame f = new Frame();
        if (wide == null) return f;
        for (int i = 0; i < SLOTS && i < wide.length; i++) f.put(i, Math.round(wide[i]));
        for (int i = 0; i < SLOTS && SLOTS + i < wide.length; i++) {
            f.put(SLOTS + i, Math.round(wide[SLOTS + i]));
        }
        return f;
    }

    public static float[] flat(Frame f) {
        float[] out = new float[WIDE];
        for (int i = 0; i < SLOTS; i++) {
            out[i] = f.angles[i];
            out[SLOTS + i] = f.offsets[i];
        }
        return out;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Name", name);
        nbt.putBoolean("Loop", loop);
        nbt.putBoolean("Smooth", smooth);
        nbt.putInt("Speed", speedPercent);
        nbt.putInt("Length", lengthTicks);
        ListTag list = new ListTag();
        for (Frame f : frames) {
            CompoundTag o = new CompoundTag();
            o.putIntArray("A", f.angles);
            o.putIntArray("O", f.offsets);
            o.putInt("T", f.holdTicks);
            o.putLong("S", f.set);
            list.add(o);
        }
        nbt.put("Frames", list);
        return nbt;
    }

    public static NpcAnimation fromNbt(CompoundTag nbt) {
        NpcAnimation a = new NpcAnimation(nbt.getString("Name"));
        a.loop = nbt.getBoolean("Loop");
        a.smooth = !nbt.contains("Smooth") || nbt.getBoolean("Smooth");
        a.speedPercent = nbt.contains("Speed") ? Math.max(25, Math.min(400, nbt.getInt("Speed"))) : 100;
        if (nbt.contains("Length")) a.setLengthTicks(nbt.getInt("Length"));
        ListTag list = nbt.getList("Frames", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < MAX_FRAMES; i++) {
            CompoundTag o = list.getCompound(i);
            Frame f = new Frame(
                    net.fugginbeenus.notchcurrency.compat.Nbt.intArray(o, "A"),
                    net.fugginbeenus.notchcurrency.compat.Nbt.intArray(o, "O"),
                    o.getInt("T"));
            f.set = o.contains("S") ? o.getLong("S") : -1L;
            a.frames.add(f);
        }
        return a;
    }
}
