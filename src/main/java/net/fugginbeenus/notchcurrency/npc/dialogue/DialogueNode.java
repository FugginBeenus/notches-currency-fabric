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

    public DialogueNode(String id) {
        this.id = id == null ? "node" : id;
    }

    public String id() { return id; }
    public String text() { return text; }
    public List<DialogueChoice> choices() { return choices; }

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
        return nbt;
    }

    public static DialogueNode fromNbt(CompoundTag nbt) {
        DialogueNode n = new DialogueNode(nbt.getString("Id"));
        n.text = nbt.getString("Text");
        ListTag list = nbt.getList("Choices", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) n.choices.add(DialogueChoice.fromNbt(list.getCompound(i)));
        return n;
    }
}
