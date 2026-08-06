package net.fugginbeenus.notchcurrency.npc.schedule;

import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    public String facingLabel() {
        String[] points = {"South", "South-west", "West", "North-west",
                           "North", "North-east", "East", "South-east"};
        float deg = ((facing % 360f) + 360f) % 360f; // Math.floorMod is integers only
        int i = (int) Math.round(deg / 45.0) % 8;
        return points[i];
    }

    public ScheduleEntry withFacing(float yaw) {
        return new ScheduleEntry(time, stance, anchor, radius, yaw, roleOpen, closedLine, label, onBegin);
    }

    public String clock() {
        return formatClock(time);
    }

    public static String formatClock(int timeOfDay) {
        int hours = Math.floorMod(timeOfDay / 1000 + 6, 24);
        int minutes = (timeOfDay % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

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
