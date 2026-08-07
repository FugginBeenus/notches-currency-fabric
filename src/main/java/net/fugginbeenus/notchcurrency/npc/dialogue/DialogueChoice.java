package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;

public class DialogueChoice {

    private String label = "";
    private String next = "";
    private boolean hideWhenLocked = false;
    private final List<DialogueAction> actions = new ArrayList<>();
    private final List<DialogueCondition> conditions = new ArrayList<>();

    public DialogueChoice() {}

    public DialogueChoice(String label, String next) {
        this.label = label == null ? "" : label;
        this.next = next == null ? "" : next;
    }

    public String label() { return label; }
    public String next() { return next; }
    public boolean hideWhenLocked() { return hideWhenLocked; }
    public List<DialogueAction> actions() { return actions; }
    public List<DialogueCondition> conditions() { return conditions; }

    public void setLabel(String l) { this.label = l == null ? "" : l; }
    public void setNext(String n) { this.next = n == null ? "" : n; }
    public void setHideWhenLocked(boolean h) { this.hideWhenLocked = h; }

    public DialogueChoice withAction(DialogueAction a) {
        actions.add(a);
        return this;
    }

    public DialogueChoice withCondition(DialogueCondition c) {
        conditions.add(c);
        return this;
    }

    public boolean isAvailable(ServerPlayer sp, NotchNpcEntity npc) {
        for (DialogueCondition c : conditions) {
            if (!c.test(sp, npc)) return false;
        }
        return true;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Label", label);
        nbt.putString("Next", next);
        nbt.putBoolean("Hide", hideWhenLocked);
        ListTag acts = new ListTag();
        for (DialogueAction a : actions) acts.add(a.toNbt());
        nbt.put("Actions", acts);
        ListTag conds = new ListTag();
        for (DialogueCondition c : conditions) conds.add(c.toNbt());
        nbt.put("Conditions", conds);
        return nbt;
    }

    public static DialogueChoice fromNbt(CompoundTag nbt) {
        DialogueChoice c = new DialogueChoice();
        c.label = nbt.getString("Label");
        c.next = nbt.getString("Next");
        c.hideWhenLocked = nbt.getBoolean("Hide");
        ListTag acts = nbt.getList("Actions", Tag.TAG_COMPOUND);
        for (int i = 0; i < acts.size(); i++) c.actions.add(DialogueAction.fromNbt(acts.getCompound(i)));
        ListTag conds = nbt.getList("Conditions", Tag.TAG_COMPOUND);
        for (int i = 0; i < conds.size(); i++) c.conditions.add(DialogueCondition.fromNbt(conds.getCompound(i)));
        return c;
    }
}
