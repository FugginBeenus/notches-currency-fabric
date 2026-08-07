package net.fugginbeenus.notchcurrency.npc.action;

import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class NpcActions {

    public static final int MAX_PER_TRIGGER = 5;

    public static final int DEFAULT_RADIUS = 8;
    public static final int MIN_RADIUS = 1;
    public static final int MAX_RADIUS = 32;

    private final Map<NpcTrigger, List<DialogueAction>> byTrigger = new EnumMap<>(NpcTrigger.class);
    private int proximityRadius = DEFAULT_RADIUS;

    public List<DialogueAction> get(NpcTrigger trigger) {
        List<DialogueAction> list = byTrigger.get(trigger);
        return list == null ? List.of() : Collections.unmodifiableList(list);
    }

    public boolean has(NpcTrigger trigger) {
        List<DialogueAction> list = byTrigger.get(trigger);
        return list != null && !list.isEmpty();
    }

    public void set(NpcTrigger trigger, List<DialogueAction> actions) {
        if (actions == null || actions.isEmpty()) {
            byTrigger.remove(trigger);
            return;
        }
        List<DialogueAction> copy = new ArrayList<>(actions.subList(0, Math.min(actions.size(), MAX_PER_TRIGGER)));
        byTrigger.put(trigger, copy);
    }

    public int proximityRadius() { return proximityRadius; }

    public void setProximityRadius(int radius) {
        this.proximityRadius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
    }

    public boolean isEmpty() {
        for (NpcTrigger t : NpcTrigger.values()) {
            if (has(t)) return false;
        }
        return true;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        for (NpcTrigger trigger : NpcTrigger.values()) {
            List<DialogueAction> list = byTrigger.get(trigger);
            if (list == null || list.isEmpty()) continue;
            ListTag out = new ListTag();
            for (DialogueAction a : list) out.add(a.toNbt());
            nbt.put(trigger.name(), out);
        }
        nbt.putInt("ProximityRadius", proximityRadius);
        return nbt;
    }

    public static NpcActions fromNbt(CompoundTag nbt) {
        NpcActions actions = new NpcActions();
        if (nbt == null) return actions;
        for (NpcTrigger trigger : NpcTrigger.values()) {
            if (!nbt.contains(trigger.name())) continue;
            ListTag list = nbt.getList(trigger.name(), Tag.TAG_COMPOUND);
            List<DialogueAction> parsed = new ArrayList<>();
            for (int i = 0; i < list.size() && i < MAX_PER_TRIGGER; i++) {
                parsed.add(DialogueAction.fromNbt(list.getCompound(i)));
            }
            actions.set(trigger, parsed);
        }
        if (nbt.contains("ProximityRadius")) actions.setProximityRadius(nbt.getInt("ProximityRadius"));
        return actions;
    }
}
