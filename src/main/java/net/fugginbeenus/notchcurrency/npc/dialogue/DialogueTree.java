package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class DialogueTree {

    private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();
    private String startId = "";

    public boolean isEmpty() {
        return startId.isEmpty() || !nodes.containsKey(startId);
    }

    public int size() { return nodes.size(); }
    public String startId() { return startId; }
    public void setStartId(String id) { this.startId = id == null ? "" : id; }
    public Map<String, DialogueNode> nodes() { return nodes; }

    @Nullable
    public DialogueNode get(String id) {
        return id == null ? null : nodes.get(id);
    }

    @Nullable
    public DialogueNode start() {
        return nodes.get(startId);
    }

    public void put(DialogueNode node) {
        nodes.put(node.id(), node);
        if (startId.isEmpty()) startId = node.id();
    }

    public void remove(String id) {
        nodes.remove(id);
        if (startId.equals(id)) {
            startId = nodes.isEmpty() ? "" : nodes.keySet().iterator().next();
        }
    }

    public void clear() {
        nodes.clear();
        startId = "";
    }

    public boolean isFlat() {
        for (DialogueNode n : nodes.values()) {
            if (!n.choices().isEmpty()) return false;
        }
        return true;
    }

    public boolean renameNode(String oldId, String newId) {
        if (newId == null || newId.isEmpty() || nodes.containsKey(newId)) return false;
        DialogueNode node = nodes.get(oldId);
        if (node == null) return false;

        node.setId(newId);
        // Rebuild the map in iteration order so the page keeps its spot in the list.
        Map<String, DialogueNode> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, DialogueNode> e : nodes.entrySet()) {
            rebuilt.put(e.getKey().equals(oldId) ? newId : e.getKey(), e.getValue());
        }
        nodes.clear();
        nodes.putAll(rebuilt);

        if (startId.equals(oldId)) startId = newId;
        for (DialogueNode n : nodes.values()) {
            for (DialogueChoice c : n.choices()) {
                if (c.next().equals(oldId)) c.setNext(newId);
            }
        }
        return true;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Start", startId);
        NbtList list = new NbtList();
        for (DialogueNode n : nodes.values()) list.add(n.toNbt());
        nbt.put("Nodes", list);
        return nbt;
    }

    public static DialogueTree fromNbt(NbtCompound nbt) {
        DialogueTree tree = new DialogueTree();
        NbtList list = nbt.getList("Nodes", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            DialogueNode n = DialogueNode.fromNbt(list.getCompound(i));
            tree.nodes.put(n.id(), n);
        }
        tree.startId = nbt.getString("Start");
        return tree;
    }
}
