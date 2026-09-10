package net.fugginbeenus.notchcurrency.client.particle;

import net.fugginbeenus.notchcurrency.npc.particle.ParticleEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParticleLibrary {

    private ParticleLibrary() {}

    private static final Map<String, ParticleEffect> KNOWN = new LinkedHashMap<>();

    public static void load(CompoundTag payload) {
        KNOWN.clear();
        if (payload == null) return;
        ListTag list = payload.getList("Effects", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ParticleEffect e = ParticleEffect.fromNbt(list.getCompound(i));
            if (!e.name().isBlank()) KNOWN.put(e.name().toLowerCase(), e);
        }
    }

    public static ParticleEffect get(String name) {
        return name == null || name.isBlank() ? null : KNOWN.get(name.toLowerCase());
    }

    public static List<ParticleEffect> all() {
        return new ArrayList<>(KNOWN.values());
    }

    public static int count() { return KNOWN.size(); }

    public static String next(String current) {
        List<String> options = new ArrayList<>();
        options.add("");
        for (ParticleEffect e : KNOWN.values()) options.add(e.name());
        if (options.size() == 1) return current == null ? "" : current;
        int at = options.indexOf(current == null ? "" : current.trim());
        return options.get((at < 0 ? 0 : at + 1) % options.size());
    }
}
