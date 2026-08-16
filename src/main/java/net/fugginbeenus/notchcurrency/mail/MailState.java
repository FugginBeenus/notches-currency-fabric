package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.compat.Nbt;
import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MailState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String DATA_KEY = "notchcurrency_mail";
    public static final int MAX_PER_PLAYER = 200;

    private final Map<UUID, List<MailItem>> boxes = new LinkedHashMap<>();
    private final Map<UUID, String> knownBoxes = new LinkedHashMap<>();

    public static MailState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return StateData.getOrCreate(storage, MailState::new, MailState::fromNbt, DATA_KEY);
    }

    public boolean post(UUID recipient, MailItem item, long gameTime) {
        if (item.isEmpty()) return true;
        List<MailItem> box = boxes.computeIfAbsent(recipient, key -> new ArrayList<>());
        if (box.size() >= MAX_PER_PLAYER) return false;
        box.add(item.at(gameTime));
        setDirty();
        return true;
    }

    public void noteMailbox(UUID owner, String name) {
        if (owner == null || name == null || name.isEmpty()) return;
        if (name.equals(knownBoxes.get(owner))) return;
        knownBoxes.put(owner, name);
        setDirty();
    }

    public Map<UUID, String> knownMailboxes() {
        return Map.copyOf(knownBoxes);
    }

    public List<MailItem> inbox(UUID player) {
        List<MailItem> box = boxes.get(player);
        return box == null ? List.of() : List.copyOf(box);
    }

    public int count(UUID player) {
        List<MailItem> box = boxes.get(player);
        return box == null ? 0 : box.size();
    }

    public boolean isFull(UUID player) {
        return count(player) >= MAX_PER_PLAYER;
    }

    public MailItem take(UUID player, UUID itemId) {
        List<MailItem> box = boxes.get(player);
        if (box == null) return null;
        for (int i = 0; i < box.size(); i++) {
            if (box.get(i).id().equals(itemId)) {
                MailItem taken = box.remove(i);
                if (box.isEmpty()) boxes.remove(player);
                setDirty();
                return taken;
            }
        }
        return null;
    }

    public void putBack(UUID player, MailItem item) {
        if (item.isEmpty()) return;
        boxes.computeIfAbsent(player, key -> new ArrayList<>()).add(item);
        setDirty();
    }


    private static MailState fromNbt(CompoundTag nbt) {
        MailState state = new MailState();
        ListTag list = nbt.getList("Boxes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!Nbt.hasUuid(entry, "Player")) continue;
            UUID player = Nbt.getUuid(entry, "Player");
            List<MailItem> box = new ArrayList<>();
            ListTag items = entry.getList("Items", Tag.TAG_COMPOUND);
            for (int j = 0; j < items.size(); j++) {
                MailItem item = MailItem.fromNbt(items.getCompound(j));
                if (!item.isEmpty()) box.add(item);
            }
            if (!box.isEmpty()) state.boxes.put(player, box);
        }
        ListTag known = nbt.getList("Known", Tag.TAG_COMPOUND);
        for (int i = 0; i < known.size(); i++) {
            CompoundTag entry = known.getCompound(i);
            if (Nbt.hasUuid(entry, "Player")) {
                state.knownBoxes.put(Nbt.getUuid(entry, "Player"), entry.getString("Name"));
            }
        }
        return state;
    }

    @Override
    public CompoundTag writeNbt(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, List<MailItem>> entry : boxes.entrySet()) {
            CompoundTag box = new CompoundTag();
            Nbt.putUuid(box, "Player", entry.getKey());
            ListTag items = new ListTag();
            for (MailItem item : entry.getValue()) items.add(item.toNbt());
            box.put("Items", items);
            list.add(box);
        }
        nbt.put("Boxes", list);

        ListTag known = new ListTag();
        for (Map.Entry<UUID, String> entry : knownBoxes.entrySet()) {
            CompoundTag one = new CompoundTag();
            Nbt.putUuid(one, "Player", entry.getKey());
            one.putString("Name", entry.getValue());
            known.add(one);
        }
        nbt.put("Known", known);
        return nbt;
    }

    //? if >=1.21.11 {
    /*
    *///?} elif >=1.21 {
    /*@Override
    public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        return writeNbt(nbt);
    }
    *///?} else {
    @Override
    public CompoundTag save(CompoundTag nbt) {
        return writeNbt(nbt);
    }
    //?}
}
