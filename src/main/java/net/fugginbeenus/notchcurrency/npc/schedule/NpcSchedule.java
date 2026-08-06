package net.fugginbeenus.notchcurrency.npc.schedule;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An NPC's day, as a list of entries sorted by the time each one begins.
 *
 * <p><b>The schedule is a function of the clock, not a state machine.</b> There is no "current step"
 * stored anywhere and no progress to keep in sync. Ask {@link #activeAt(long)} what should be
 * happening at a given time and it answers from the entry list alone.
 *
 * <p>That is what makes an unloaded NPC free. It doesn't need to keep running to stay correct, and it
 * can't drift while nobody is watching. When its chunk loads again it asks the time, gets an entry,
 * and starts doing that: no catch-up, no replay, nothing to repair. Every awkward question about
 * pausing and resuming simply doesn't arise.
 *
 * <p>The day wraps. Before the first entry's time, the last entry of the day is still in force, which
 * is what makes an overnight sleep that starts at 21:00 carry through to the morning without needing
 * a second entry at midnight.
 */
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

    /** When false the schedule only moves the NPC around, and its role stays usable all day. */
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

    /** True once there is something to run: an NPC that fails this costs nothing per tick. */
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

    /**
     * Replace one entry. Returns where it ended up, which may not be where it was: changing an
     * entry's time re-sorts the list, and the editor needs to keep its selection on the right one.
     */
    public int replace(int index, ScheduleEntry entry) {
        if (index < 0 || index >= entries.size()) return index;
        entries.set(index, entry);
        sort();
        return entries.indexOf(entry);
    }

    private void sort() {
        entries.sort(Comparator.comparingInt(ScheduleEntry::time));
    }

    /**
     * Which entry governs the given time of day, or -1 when there is nothing to run.
     *
     * <p>Broken entries are passed over rather than applied, so a missing spot costs the NPC that
     * block of time and nothing more. It falls back to whatever the Moves tab says, which is a dull
     * outcome instead of an NPC standing in a doorway waiting for a bed that was mined.
     */
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

    /** How many entries still need a spot, for the editor's repair banner. */
    public int brokenCount() {
        int n = 0;
        for (ScheduleEntry e : entries) {
            if (e.isBroken()) n++;
        }
        return n;
    }

    /** The first entry needing attention, so "Fix next" always has somewhere to go. */
    public int firstBroken() {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isBroken()) return i;
        }
        return -1;
    }

    /**
     * Schedules need a day to follow. The Nether and the End hold at a fixed time, so an NPC there
     * would sit in whichever entry it happened to land on forever. Better to say so than to let
     * someone build a full day's routine that silently never advances.
     */
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

    /**
     * Strip the spots out of a stored schedule, keeping its shape.
     *
     * <p>Called when an NPC config leaves its world, next to the existing home and route stripping.
     * The times, stances and actions are the part worth sharing; the coordinates mean nothing
     * anywhere else and would send the NPC walking at a spot that isn't there. The entries arrive
     * marked as needing a spot, which is exactly what the repair flow is built to walk through.
     */
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
