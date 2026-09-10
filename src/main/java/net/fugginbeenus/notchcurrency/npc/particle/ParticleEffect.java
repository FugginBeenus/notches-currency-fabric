package net.fugginbeenus.notchcurrency.npc.particle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class ParticleEffect {

    public static final int MAX_LAYERS = 4;
    public static final int MAX_NAME = 32;

    private final String name;
    private final List<ParticleLayer> layers = new ArrayList<>();

    public ParticleEffect(String name) {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.length() > MAX_NAME) cleaned = cleaned.substring(0, MAX_NAME);
        this.name = cleaned;
    }

    public String name() { return name; }

    public List<ParticleLayer> layers() { return layers; }

    public int layerCount() { return layers.size(); }

    public ParticleLayer layer(int i) {
        return i >= 0 && i < layers.size() ? layers.get(i) : null;
    }

    public ParticleLayer addLayer() {
        if (layers.size() >= MAX_LAYERS) return null;
        ParticleLayer l = new ParticleLayer();
        layers.add(l);
        return l;
    }

    public boolean removeLayer(int i) {
        if (i < 0 || i >= layers.size()) return false;
        layers.remove(i);
        return true;
    }

    public boolean isEmpty() { return layers.isEmpty(); }

    public int fastestRate() {
        int best = Integer.MAX_VALUE;
        for (ParticleLayer l : layers) best = Math.min(best, l.rate());
        return best == Integer.MAX_VALUE ? 20 : best;
    }

    public ParticleEffect copyAs(String newName) {
        ParticleEffect e = new ParticleEffect(newName);
        for (ParticleLayer l : layers) e.layers.add(l.copy());
        return e;
    }

    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        t.putString("Name", name);
        ListTag list = new ListTag();
        for (ParticleLayer l : layers) list.add(l.toNbt());
        t.put("Layers", list);
        return t;
    }

    public static ParticleEffect fromNbt(CompoundTag t) {
        ParticleEffect e = new ParticleEffect(t.getString("Name"));
        ListTag list = t.getList("Layers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < MAX_LAYERS; i++) {
            e.layers.add(ParticleLayer.fromNbt(list.getCompound(i)));
        }
        return e;
    }
}
