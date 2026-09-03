package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.npc.anim.NpcAnimation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnimationLibrary {

    private AnimationLibrary() {}

    private static final Map<String, NpcAnimation> KNOWN = new LinkedHashMap<>();

    public static void load(CompoundTag payload) {
        KNOWN.clear();
        if (payload == null) return;
        ListTag list = payload.getList("Animations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            NpcAnimation a = NpcAnimation.fromNbt(list.getCompound(i));
            if (!a.name().isBlank()) KNOWN.put(a.name().toLowerCase(), a);
        }
    }

    public static final String PREVIEW = "__preview";

    public static void setPreview(NpcAnimation anim) {
        if (anim == null) KNOWN.remove(PREVIEW);
        else KNOWN.put(PREVIEW, anim);
    }

    public static NpcAnimation get(String name) {
        return name == null || name.isBlank() ? null : KNOWN.get(name.toLowerCase());
    }

    public static List<NpcAnimation> all() {
        List<NpcAnimation> out = new ArrayList<>();
        for (Map.Entry<String, NpcAnimation> e : KNOWN.entrySet()) {
            if (!PREVIEW.equals(e.getKey())) out.add(e.getValue());
        }
        return out;
    }

    public static String next(String current) {
        List<String> options = new ArrayList<>();
        options.add("");
        for (NpcAnimation a : KNOWN.values()) options.add(a.name());
        if (options.size() == 1) return current == null ? "" : current;
        int at = options.indexOf(current == null ? "" : current.trim());
        return options.get((at < 0 ? 0 : at + 1) % options.size());
    }
}
