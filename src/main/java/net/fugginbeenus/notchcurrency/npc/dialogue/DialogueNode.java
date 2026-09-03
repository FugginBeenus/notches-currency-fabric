package net.fugginbeenus.notchcurrency.npc.dialogue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class DialogueNode {

    private String id;
    private String text = "";
    private final List<DialogueChoice> choices = new ArrayList<>();
    private final List<DialogueCondition> openIf = new ArrayList<>();

    public DialogueNode(String id) {
        this.id = id == null ? "node" : id;
    }

    public String id() { return id; }
    public String text() { return text; }
    public List<DialogueChoice> choices() { return choices; }

    public List<DialogueCondition> openIf() { return openIf; }

    public DialogueCondition openIf(int slot) {
        return slot >= 0 && slot < openIf.size() ? openIf.get(slot) : null;
    }

    public void setOpenIf(int slot, DialogueCondition c) {
        while (openIf.size() <= slot) openIf.add(null);
        openIf.set(slot, (c == null || c.type() == DialogueCondition.Type.NONE) ? null : c);
        while (!openIf.isEmpty() && openIf.get(openIf.size() - 1) == null) {
            openIf.remove(openIf.size() - 1);
        }
    }
    public boolean hasOpenIf() {
        for (DialogueCondition c : openIf) {
            if (c != null) return true;
        }
        return false;
    }

    public void setId(String id) { this.id = id == null ? "node" : id; }
    public void setText(String t) { this.text = t == null ? "" : t; }

    public DialogueNode withChoice(DialogueChoice c) {
        choices.add(c);
        return this;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Id", id);
        nbt.putString("Text", text);
        ListTag list = new ListTag();
        for (DialogueChoice c : choices) list.add(c.toNbt());
        nbt.put("Choices", list);
        if (hasOpenIf()) {
            ListTag conds = new ListTag();
            for (DialogueCondition c : openIf) {
                if (c != null) conds.add(c.toNbt());
            }
            nbt.put("OpenIfs", conds);
        }
        return nbt;
    }

    public static DialogueNode fromNbt(CompoundTag nbt) {
        DialogueNode n = new DialogueNode(nbt.getString("Id"));
        n.text = nbt.getString("Text");
        ListTag list = nbt.getList("Choices", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) n.choices.add(DialogueChoice.fromNbt(list.getCompound(i)));
        if (nbt.contains("OpenIfs")) {
            ListTag conds = nbt.getList("OpenIfs", Tag.TAG_COMPOUND);
            for (int i = 0; i < conds.size() && i < 2; i++) {
                n.openIf.add(DialogueCondition.fromNbt(conds.getCompound(i)));
            }
        } else if (nbt.contains("OpenIf")) {
            n.openIf.add(DialogueCondition.fromNbt(nbt.getCompound("OpenIf")));
        }
        return n;
    }
}
