package net.fugginbeenus.notchcurrency.npc.schedule;

import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One block of an NPC's day: from {@link #time} until whatever entry comes next.
 *
 * <p>An entry describes a <em>state</em>, never an event, which is what lets a schedule be re-derived
 * from the clock alone after the NPC has been unloaded for a week. The one exception is
 * {@link #onBegin}, which fires as the entry starts and is deliberately skipped when an NPC is simply
 * loading back in, so a shop announces opening at eight rather than every time somebody wanders into
 * the chunk.
 *
 * @param time       world time of day this entry begins, 0 to 23999
 * @param stance     what the NPC does for the duration
 * @param anchor     the spot, bed, or wander centre; null until the owner points at one
 * @param radius     how far to stray, {@link NpcStance#WANDER} only
 * @param facing     yaw to hold once in place, {@link NpcStance#STAND} only
 * @param roleOpen   whether the NPC's role can be used during this entry, subject to
 *                   {@link NpcSchedule#enforceHours()}
 * @param closedLine what to say when someone tries to use a closed role, blank for the default
 * @param label      the owner's name for this entry, purely for the editor
 * @param onBegin    actions to run on entering, capped at {@link #MAX_ACTIONS}
 */
public record ScheduleEntry(
        int time,
        NpcStance stance,
        @Nullable BlockPos anchor,
        int radius,
        float facing,
        boolean roleOpen,
        String closedLine,
        String label,
        List<DialogueAction> onBegin
) {

    public static final int MAX_ACTIONS = 5;
    public static final int MIN_RADIUS = 2, MAX_RADIUS = 32;
    public static final int DAY_LENGTH = 24000;

    /** Canonicalises every field, so nothing downstream has to re-check a hand-edited file. */
    public ScheduleEntry {
        time = Math.floorMod(time, DAY_LENGTH);
        if (stance == null) stance = NpcStance.STAND;
        radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
        facing = net.minecraft.util.math.MathHelper.wrapDegrees(facing);
        closedLine = closedLine == null ? "" : closedLine;
        label = label == null ? "" : label;
        onBegin = onBegin == null ? List.of()
                : List.copyOf(onBegin.subList(0, Math.min(onBegin.size(), MAX_ACTIONS)));
    }

    public static ScheduleEntry of(int time, NpcStance stance) {
        return new ScheduleEntry(time, stance, null, 8, 0f, true, "", "", List.of());
    }

    /**
     * Why this entry can't run, or null when it's fine. The editor shows this and the runtime skips
     * the entry rather than walking the NPC at a spot that isn't there.
     */
    @Nullable
    public String problem() {
        if (stance.needsSpot() && anchor == null) {
            return stance == NpcStance.SLEEP ? "No bed set yet." : "No spot set yet.";
        }
        return null;
    }

    public boolean isBroken() {
        return problem() != null;
    }

    /** Clock reading for the editor. Minecraft's day starts at 06:00, not midnight. */
    public String clock() {
        return formatClock(time);
    }

    public static String formatClock(int timeOfDay) {
        int hours = Math.floorMod(timeOfDay / 1000 + 6, 24);
        int minutes = (timeOfDay % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    /** Inverse of {@link #formatClock}, for the editor's time stepper. */
    public static int ticksForClock(int hours, int minutes) {
        int h = Math.floorMod(hours - 6, 24);
        return Math.floorMod(h * 1000 + minutes * 1000 / 60, DAY_LENGTH);
    }

    public ScheduleEntry withTime(int newTime) {
        return new ScheduleEntry(newTime, stance, anchor, radius, facing, roleOpen, closedLine, label, onBegin);
    }

    public ScheduleEntry withStance(NpcStance newStance) {
        return new ScheduleEntry(time, newStance, anchor, radius, facing, roleOpen, closedLine, label, onBegin);
    }

    public ScheduleEntry withAnchor(@Nullable BlockPos newAnchor, float newFacing) {
        return new ScheduleEntry(time, stance, newAnchor, radius, newFacing, roleOpen, closedLine, label, onBegin);
    }

    public ScheduleEntry withRadius(int newRadius) {
        return new ScheduleEntry(time, stance, anchor, newRadius, facing, roleOpen, closedLine, label, onBegin);
    }

    public ScheduleEntry withRoleOpen(boolean open) {
        return new ScheduleEntry(time, stance, anchor, radius, facing, open, closedLine, label, onBegin);
    }

    public ScheduleEntry withClosedLine(String line) {
        return new ScheduleEntry(time, stance, anchor, radius, facing, roleOpen, line, label, onBegin);
    }

    public ScheduleEntry withLabel(String newLabel) {
        return new ScheduleEntry(time, stance, anchor, radius, facing, roleOpen, closedLine, newLabel, onBegin);
    }

    public ScheduleEntry withActions(List<DialogueAction> actions) {
        return new ScheduleEntry(time, stance, anchor, radius, facing, roleOpen, closedLine, label, actions);
    }

    /** Drops the spot, keeping everything else. Used when an NPC travels to another world. */
    public ScheduleEntry withoutAnchor() {
        return withAnchor(null, facing);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("Time", time);
        nbt.putString("Stance", stance.name());
        if (anchor != null) {
            nbt.putInt("X", anchor.getX());
            nbt.putInt("Y", anchor.getY());
            nbt.putInt("Z", anchor.getZ());
        }
        nbt.putInt("Radius", radius);
        nbt.putFloat("Facing", facing);
        nbt.putBoolean("RoleOpen", roleOpen);
        if (!closedLine.isEmpty()) nbt.putString("ClosedLine", closedLine);
        if (!label.isEmpty()) nbt.putString("Label", label);
        if (!onBegin.isEmpty()) {
            NbtList list = new NbtList();
            for (DialogueAction a : onBegin) list.add(a.toNbt());
            nbt.put("OnBegin", list);
        }
        return nbt;
    }

    public static ScheduleEntry fromNbt(NbtCompound nbt) {
        BlockPos anchor = nbt.contains("X")
                ? new BlockPos(nbt.getInt("X"), nbt.getInt("Y"), nbt.getInt("Z"))
                : null;
        List<DialogueAction> actions = new ArrayList<>();
        NbtList list = nbt.getList("OnBegin", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size() && i < MAX_ACTIONS; i++) {
            actions.add(DialogueAction.fromNbt(list.getCompound(i)));
        }
        return new ScheduleEntry(
                nbt.getInt("Time"),
                NpcStance.byName(nbt.getString("Stance"), NpcStance.STAND),
                anchor,
                nbt.contains("Radius") ? nbt.getInt("Radius") : 8,
                nbt.getFloat("Facing"),
                !nbt.contains("RoleOpen") || nbt.getBoolean("RoleOpen"),
                nbt.getString("ClosedLine"),
                nbt.getString("Label"),
                actions);
    }
}
