package net.fugginbeenus.notchcurrency.npc.schedule;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NpcSchedule {

    public static final int MAX_ENTRIES = 16;

    private final List<ScheduleEntry> entries = new ArrayList<>();
    private boolean enabled = false;
    private boolean enforceHours = true;

    public NpcSchedule() {}

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enforceHours() {
        return enforceHours;
    }

    public void setEnforceHours(boolean enforceHours) {
        this.enforceHours = enforceHours;
    }

    public List<ScheduleEntry> entries() {
        return List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean isActive() {
        return enabled && !entries.isEmpty();
    }

    public void setEntries(List<ScheduleEntry> newEntries) {
        entries.clear();
        for (ScheduleEntry e : newEntries) {
            if (entries.size() >= MAX_ENTRIES) break;
            entries.add(e);
        }
        sort();
    }

    public void add(ScheduleEntry entry) {
        if (entries.size() >= MAX_ENTRIES) return;
        entries.add(entry);
        sort();
    }

    public void remove(int index) {
        if (index >= 0 && index < entries.size()) entries.remove(index);
    }

    public int replace(int index, ScheduleEntry entry) {
        if (index < 0 || index >= entries.size()) return index;
        entries.set(index, entry);
        sort();
        return entries.indexOf(entry);
    }

    private void sort() {
        entries.sort(Comparator.comparingInt(ScheduleEntry::time));
    }

    public int indexAt(long timeOfDay) {
        if (entries.isEmpty()) return -1;
        int tod = (int) Math.floorMod(timeOfDay, ScheduleEntry.DAY_LENGTH);
        int best = -1;
        for (int i = 0; i < entries.size(); i++) {
            ScheduleEntry e = entries.get(i);
            if (e.time() <= tod && !e.isBroken()) best = i;
        }
        if (best >= 0) return best;
        // Before the first entry of the day: yesterday's last usable entry is still running.
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (!entries.get(i).isBroken()) return i;
        }
        return -1;
    }

    @Nullable
    public ScheduleEntry activeAt(long timeOfDay) {
        int i = indexAt(timeOfDay);
        return i < 0 ? null : entries.get(i);
    }

    @Nullable
    public ScheduleEntry get(int index) {
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    public int brokenCount() {
        int n = 0;
        for (ScheduleEntry e : entries) {
            if (e.isBroken()) n++;
        }
        return n;
    }

    public int firstBroken() {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isBroken()) return i;
        }
        return -1;
    }

    public static boolean dimensionSupports(World world) {
        return world != null && !world.getDimension().hasFixedTime();
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("Enabled", enabled);
        nbt.putBoolean("EnforceHours", enforceHours);
        NbtList list = new NbtList();
        for (ScheduleEntry e : entries) list.add(e.toNbt());
        nbt.put("Entries", list);
        return nbt;
    }

    public static NpcSchedule fromNbt(NbtCompound nbt) {
        NpcSchedule schedule = new NpcSchedule();
        schedule.enabled = nbt.getBoolean("Enabled");
        schedule.enforceHours = !nbt.contains("EnforceHours") || nbt.getBoolean("EnforceHours");
        NbtList list = nbt.getList("Entries", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size() && i < MAX_ENTRIES; i++) {
            schedule.entries.add(ScheduleEntry.fromNbt(list.getCompound(i)));
        }
        schedule.sort();
        return schedule;
    }

    public static void stripAnchors(NbtCompound parent, String key) {
        if (!parent.contains(key)) return;
        NbtCompound tag = parent.getCompound(key);
        NbtList list = tag.getList("Entries", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);
            e.remove("X");
            e.remove("Y");
            e.remove("Z");
        }
    }
}
