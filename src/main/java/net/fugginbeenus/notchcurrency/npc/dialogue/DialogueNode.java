package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

/**
 * One page of dialogue: an id, what the NPC says (plain text, so {@code %player%}/{@code %npc%} are
 * substituted at display time), and the choice buttons underneath.
 */
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

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Id", id);
        nbt.putString("Text", text);
        NbtList list = new NbtList();
        for (DialogueChoice c : choices) list.add(c.toNbt());
        nbt.put("Choices", list);
        return nbt;
    }

    public static DialogueNode fromNbt(NbtCompound nbt) {
        DialogueNode n = new DialogueNode(nbt.getString("Id"));
        n.text = nbt.getString("Text");
        NbtList list = nbt.getList("Choices", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) n.choices.add(DialogueChoice.fromNbt(list.getCompound(i)));
        return n;
    }
}
