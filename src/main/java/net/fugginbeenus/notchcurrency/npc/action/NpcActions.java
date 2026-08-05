package net.fugginbeenus.notchcurrency.npc.action;

import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What an NPC does when something happens to it: a short list of {@link DialogueAction}s per
 * {@link NpcTrigger}. Deliberately the same action type dialogue choices use, so anything you can do
 * from a conversation you can also do from a trigger, and there's only one thing to learn.
 *
 * <p>Stored in the NPC's config, so triggers travel with the pick-up item and with presets.
 */
public class NpcActions {

    /** Per trigger. Small on purpose: this is meant to stay readable, not become a scripting language. */
    public static final int MAX_PER_TRIGGER = 5;

    public static final int DEFAULT_RADIUS = 8;
    public static final int MIN_RADIUS = 1;
    public static final int MAX_RADIUS = 32;

    private final Map<NpcTrigger, List<DialogueAction>> byTrigger = new EnumMap<>(NpcTrigger.class);
    private int proximityRadius = DEFAULT_RADIUS;

    /** The actions for a trigger; never null, and not modifiable: go through {@link #set}. */
    public List<DialogueAction> get(NpcTrigger trigger) {
        List<DialogueAction> list = byTrigger.get(trigger);
        return list == null ? List.of() : Collections.unmodifiableList(list);
    }

    /** Whether anything is wired to this trigger. Checked before per-tick work, so keep it cheap. */
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

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        for (NpcTrigger trigger : NpcTrigger.values()) {
            List<DialogueAction> list = byTrigger.get(trigger);
            if (list == null || list.isEmpty()) continue;
            NbtList out = new NbtList();
            for (DialogueAction a : list) out.add(a.toNbt());
            nbt.put(trigger.name(), out);
        }
        nbt.putInt("ProximityRadius", proximityRadius);
        return nbt;
    }

    public static NpcActions fromNbt(NbtCompound nbt) {
        NpcActions actions = new NpcActions();
        if (nbt == null) return actions;
        for (NpcTrigger trigger : NpcTrigger.values()) {
            if (!nbt.contains(trigger.name())) continue;
            NbtList list = nbt.getList(trigger.name(), NbtElement.COMPOUND_TYPE);
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
