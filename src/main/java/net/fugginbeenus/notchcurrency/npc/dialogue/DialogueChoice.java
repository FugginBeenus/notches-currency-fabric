package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * One clickable button on a dialogue node: a label, gate conditions (all must pass), actions run in
 * order, and an optional next node to jump to. If the conditions fail, the button shows locked
 * (greyed) — or is hidden entirely when {@code hideWhenLocked} is set.
 */
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

    /** True if every condition passes for this player. */
    public boolean isAvailable(ServerPlayerEntity sp, NotchNpcEntity npc) {
        for (DialogueCondition c : conditions) {
            if (!c.test(sp, npc)) return false;
        }
        return true;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Label", label);
        nbt.putString("Next", next);
        nbt.putBoolean("Hide", hideWhenLocked);
        NbtList acts = new NbtList();
        for (DialogueAction a : actions) acts.add(a.toNbt());
        nbt.put("Actions", acts);
        NbtList conds = new NbtList();
        for (DialogueCondition c : conditions) conds.add(c.toNbt());
        nbt.put("Conditions", conds);
        return nbt;
    }

    public static DialogueChoice fromNbt(NbtCompound nbt) {
        DialogueChoice c = new DialogueChoice();
        c.label = nbt.getString("Label");
        c.next = nbt.getString("Next");
        c.hideWhenLocked = nbt.getBoolean("Hide");
        NbtList acts = nbt.getList("Actions", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < acts.size(); i++) c.actions.add(DialogueAction.fromNbt(acts.getCompound(i)));
        NbtList conds = nbt.getList("Conditions", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < conds.size(); i++) c.conditions.add(DialogueCondition.fromNbt(conds.getCompound(i)));
        return c;
    }
}
