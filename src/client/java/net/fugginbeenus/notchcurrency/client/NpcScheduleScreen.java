package net.fugginbeenus.notchcurrency.client;

import net.minecraft.client.Minecraft;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule;
import net.fugginbeenus.notchcurrency.npc.schedule.NpcStance;
import net.fugginbeenus.notchcurrency.npc.schedule.ScheduleEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NpcScheduleScreen extends Screen {

    private static final int W = 340, H = 236;
    private static final int LIST_X = 12, LIST_Y = 40, LIST_W = 120, LIST_H = 150;
    private static final int ROW_H = 18, VISIBLE_ROWS = 8;
    private static final int PANE_X = 140, PANE_W = 188;

    private final UUID npcId;
    private final boolean dimensionOk;
    private final List<ScheduleEntry> entries = new ArrayList<>();
    private boolean enabled;
    private boolean enforceHours;
    private int selected = -1;
    private int scroll = 0;
    @org.jetbrains.annotations.Nullable private List<Component> tooltip = null;

    public NpcScheduleScreen(UUID npcId, boolean dimensionOk, NpcSchedule schedule) {
        super(Component.literal("Schedule"));
        this.npcId = npcId;
        this.dimensionOk = dimensionOk;
        this.enabled = schedule.isEnabled();
        this.enforceHours = schedule.enforceHours();
        this.entries.addAll(schedule.entries());
        if (!entries.isEmpty()) selected = 0;
    }

    public boolean isFor(UUID id) {
        return npcId.equals(id);
    }

    private int px() { return (this.width - W) / 2; }
    private int py() { return (this.height - H) / 2; }
    private int rowY(int visIdx) { return py() + LIST_Y + 2 + visIdx * ROW_H; }

    private int brokenCount() {
        int n = 0;
        for (ScheduleEntry e : entries) {
            if (e.isBroken()) n++;
        }
        return n;
    }

    private int firstBroken() {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).isBroken()) return i;
        }
        return -1;
    }

    private void sortKeepingSelection() {
        ScheduleEntry sel = selected >= 0 && selected < entries.size() ? entries.get(selected) : null;
        entries.sort(java.util.Comparator.comparingInt(ScheduleEntry::time));
        if (sel != null) selected = entries.indexOf(sel);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderInGameBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        tooltip = null;
        int px = px(), py = py();
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Daily Schedule", px + W / 2, py + 8);

        if (!dimensionOk) {
            renderNoDayHere(ctx, mouseX, mouseY, px, py);
            super.render(ctx, mouseX, mouseY, delta);
            return;
        }

        // Top row: the two switches that decide how much the schedule is allowed to do.
        toggleRow(ctx, px + 12, py + 22, 96, 40, "Schedule running", enabled, mouseX, mouseY);
        toggleRow(ctx, px + 172, py + 22, 106, 40, "Keep opening hours", enforceHours, mouseX, mouseY);

        NotchWidgets.inset(ctx, px + LIST_X, py + LIST_Y, LIST_W, LIST_H, NotchTheme.DEEP);
        if (entries.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No entries yet.",
                    px + LIST_X + LIST_W / 2, py + LIST_Y + LIST_H / 2 - 8, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "Add one below.",
                    px + LIST_X + LIST_W / 2, py + LIST_Y + LIST_H / 2 + 2, NotchTheme.TEXT_MUTED, false);
        }
        for (int v = 0; v < VISIBLE_ROWS; v++) {
            int i = scroll + v;
            if (i >= entries.size()) break;
            ScheduleEntry e = entries.get(i);
            boolean hover = over(mouseX, mouseY, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 2);
            String text = e.clock() + "  " + e.stance().label() + (e.isBroken() ? " !" : "");
            if (i == selected) {
                NotchWidgets.primaryButton(ctx, this.font, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 2, text, hover);
            } else if (e.isBroken()) {
                NotchWidgets.dangerButton(ctx, this.font, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 2, text, hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 2, text, hover);
            }
        }

        NotchWidgets.primaryButton(ctx, this.font, px + LIST_X, py + 194, 58, 14, "Add",
                over(mouseX, mouseY, px + LIST_X, py + 194, 58, 14));
        NotchWidgets.dangerButton(ctx, this.font, px + LIST_X + 62, py + 194, 58, 14, "Remove",
                over(mouseX, mouseY, px + LIST_X + 62, py + 194, 58, 14));

        renderEntryPane(ctx, mouseX, mouseY, px, py);

        int broken = brokenCount();
        if (broken > 0) {
            String msg = broken == 1 ? "1 entry needs a spot" : broken + " entries need a spot";
            ctx.drawString(this.font, msg, px + 12, py + 214, 0xFFFFAA55, false);
            NotchWidgets.primaryButton(ctx, this.font, px + 150, py + 211, 76, 14, "Fix next",
                    over(mouseX, mouseY, px + 150, py + 211, 76, 14));
        }
        NotchWidgets.primaryButton(ctx, this.font, px + 232, py + 211, 96, 14, "Save & Close",
                over(mouseX, mouseY, px + 232, py + 211, 96, 14));

        super.render(ctx, mouseX, mouseY, delta);
        if (tooltip != null) ctx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private void renderNoDayHere(GuiGraphics ctx, int mouseX, int mouseY, int px, int py) {
        NotchWidgets.centerText(ctx, this.font, "Schedules aren't available in this dimension.",
                px + W / 2, py + 90, NotchTheme.TEXT_DARK, false);
        NotchWidgets.centerText(ctx, this.font, "There's no sunrise here to follow, so a schedule",
                px + W / 2, py + 108, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.centerText(ctx, this.font, "would sit on one entry forever.",
                px + W / 2, py + 120, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.centerText(ctx, this.font, "The Moves tab has wander, patrol and follow,",
                px + W / 2, py + 140, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.centerText(ctx, this.font, "which all work fine down here.",
                px + W / 2, py + 152, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.primaryButton(ctx, this.font, px + W / 2 - 60, py + 190, 120, 16, "Back",
                over(mouseX, mouseY, px + W / 2 - 60, py + 190, 120, 16));
    }

    private void renderEntryPane(GuiGraphics ctx, int mouseX, int mouseY, int px, int py) {
        if (selected < 0 || selected >= entries.size()) {
            NotchWidgets.centerText(ctx, this.font, "Pick an entry on the left.",
                    px + PANE_X + PANE_W / 2, py + 100, NotchTheme.TEXT_MUTED, false);
            return;
        }
        ScheduleEntry e = entries.get(selected);
        int x = px + PANE_X;

        ctx.drawString(this.font, "Starts at", x, py + 44, NotchTheme.TEXT_DARK, false);
        boolean minusHover = over(mouseX, mouseY, x + 60, py + 41, 18, 14);
        boolean plusHover = over(mouseX, mouseY, x + 114, py + 41, 18, 14);
        NotchWidgets.neutralButton(ctx, this.font, x + 60, py + 41, 18, 14, "-", minusHover);
        NotchWidgets.centerText(ctx, this.font, e.clock(), x + 96, py + 44, NotchTheme.TEXT_LIGHT, false);
        NotchWidgets.neutralButton(ctx, this.font, x + 114, py + 41, 18, 14, "+", plusHover);
        if (minusHover || plusHover) {
            // The step note used to sit here as text and ran off the panel edge.
            tooltip = List.of(
                    Component.literal("Starts at").withStyle(ChatFormatting.WHITE),
                    Component.literal("Click for an hour.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Hold Shift for 15 minutes.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Minecraft's day starts at 06:00.").withStyle(ChatFormatting.DARK_GRAY));
        }

        ctx.drawString(this.font, "Does", x, py + 66, NotchTheme.TEXT_DARK, false);
        boolean stanceHover = over(mouseX, mouseY, x + 60, py + 63, 72, 14);
        NotchWidgets.primaryButton(ctx, this.font, x + 60, py + 63, 72, 14, e.stance().label(), stanceHover);
        if (stanceHover) {
            tooltip = List.of(Component.literal(e.stance().label()).withStyle(ChatFormatting.WHITE),
                    Component.literal(e.stance().hint()).withStyle(ChatFormatting.GRAY),
                    Component.literal("Click to cycle.").withStyle(ChatFormatting.DARK_GRAY));
        }

        // The spot, and the repair prompt when there isn't one.
        String problem = e.problem();
        ctx.drawString(this.font, e.stance() == NpcStance.SLEEP ? "Bed" : "Spot",
                x, py + 104, NotchTheme.TEXT_DARK, false);
        if (problem != null) {
            ctx.drawString(this.font, problem, x + 60, py + 104, 0xFFFFAA55, false);
        } else if (e.anchor() != null) {
            ctx.drawString(this.font, e.anchor().getX() + ", " + e.anchor().getY() + ", " + e.anchor().getZ(),
                    x + 60, py + 104, NotchTheme.TEXT_LIGHT, false);
        } else {
            ctx.drawString(this.font, "not needed", x + 60, py + 104, NotchTheme.TEXT_MUTED, false);
        }
        if (e.stance().needsSpot()) {
            boolean spotHover = over(mouseX, mouseY, x, py + 116, 90, 14);
            NotchWidgets.primaryButton(ctx, this.font, x, py + 116, 90, 14, "Set with tool", spotHover);
            if (spotHover) {
                tooltip = List.of(
                        Component.literal("Set with tool").withStyle(ChatFormatting.WHITE),
                        Component.literal(e.stance() == NpcStance.SLEEP
                                ? "Hands you a tool. Right-click the bed."
                                : "Hands you a tool. Right-click the spot.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Right-click the air to cancel.").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (e.stance() == NpcStance.STAND) {
            // Same slot the radius uses for Wander: only one of them is ever relevant at a time.
            ctx.drawString(this.font, "Faces", x, py + 138, NotchTheme.TEXT_DARK, false);
            boolean ccw = over(mouseX, mouseY, x + 60, py + 135, 18, 14);
            boolean cw = over(mouseX, mouseY, x + 158, py + 135, 18, 14);
            NotchWidgets.neutralButton(ctx, this.font, x + 60, py + 135, 18, 14, "<", ccw);
            NotchWidgets.centerText(ctx, this.font, e.facingLabel(), x + 118, py + 138,
                    NotchTheme.TEXT_LIGHT, false);
            NotchWidgets.neutralButton(ctx, this.font, x + 158, py + 135, 18, 14, ">", cw);
            if (ccw || cw) {
                tooltip = List.of(
                        Component.literal("Faces").withStyle(ChatFormatting.WHITE),
                        Component.literal("Which way it looks once it settles.").withStyle(ChatFormatting.GRAY),
                        Component.literal("It still turns to whoever talks to it").withStyle(ChatFormatting.GRAY),
                        Component.literal("and goes back to this afterwards.").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (e.stance() == NpcStance.WANDER) {
            ctx.drawString(this.font, "Radius", x, py + 138, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, x + 60, py + 135, 18, 14, "-",
                    over(mouseX, mouseY, x + 60, py + 135, 18, 14));
            NotchWidgets.centerText(ctx, this.font, String.valueOf(e.radius()), x + 96, py + 138,
                    NotchTheme.TEXT_LIGHT, false);
            NotchWidgets.neutralButton(ctx, this.font, x + 114, py + 135, 18, 14, "+",
                    over(mouseX, mouseY, x + 114, py + 135, 18, 14));
        }

        int acts = e.onBegin().size();
        boolean actHover = over(mouseX, mouseY, x, py + 178, 146, 14);
        NotchWidgets.goldButton(ctx, this.font, x, py + 178, 146, 14,
                acts == 0 ? "When it starts..." : "When it starts (" + acts + ")", actHover);
        if (actHover) {
            tooltip = List.of(
                    Component.literal("When it starts").withStyle(ChatFormatting.GOLD),
                    Component.literal("Say a line, hand something over, run a").withStyle(ChatFormatting.GRAY),
                    Component.literal("command. Runs once as this block begins.").withStyle(ChatFormatting.GRAY),
                    Component.literal("This is how a shop restocks at opening.").withStyle(ChatFormatting.DARK_GRAY));
        }

        toggleRow(ctx, x, py + 158, 106, 40, "Role usable now", e.roleOpen(), mouseX, mouseY);
        if (over(mouseX, mouseY, x, py + 158, 146, 14)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("Role usable now").withStyle(ChatFormatting.WHITE));
            lines.add(Component.literal(e.roleOpen()
                    ? "Players can use this NPC's role during this block."
                    : "Players are turned away during this block.").withStyle(ChatFormatting.GRAY));
            if (!enforceHours) {
                lines.add(Component.literal("Ignored: Keep opening hours is off.").withStyle(ChatFormatting.YELLOW));
            }
            tooltip = lines;
        }
    }

    private void toggleRow(GuiGraphics ctx, int x, int y, int labelW, int btnW, String label,
                           boolean on, int mouseX, int mouseY) {
        ctx.drawString(this.font, label, x, y + 3, NotchTheme.TEXT_DARK, false);
        boolean hover = over(mouseX, mouseY, x + labelW, y, btnW, 14);
        if (on) {
            NotchWidgets.primaryButton(ctx, this.font, x + labelW, y, btnW, 14, "ON", hover);
        } else {
            NotchWidgets.neutralButton(ctx, this.font, x + labelW, y, btnW, 14, "OFF", hover);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int mx = (int) mouseX, my = (int) mouseY;
        int px = px(), py = py();

        if (!dimensionOk) {
            if (over(mx, my, px + W / 2 - 60, py + 190, 120, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 1);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (over(mx, my, px + 12 + 96, py + 22, 40, 14)) {
            NotchWidgets.tick();
            enabled = !enabled;
            return true;
        }
        if (over(mx, my, px + 172 + 106, py + 22, 40, 14)) {
            NotchWidgets.tick();
            enforceHours = !enforceHours;
            return true;
        }
        for (int v = 0; v < VISIBLE_ROWS; v++) {
            int i = scroll + v;
            if (i >= entries.size()) break;
            if (over(mx, my, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 2)) {
                NotchWidgets.tick();
                selected = i;
                return true;
            }
        }
        if (over(mx, my, px + LIST_X, py + 194, 58, 14)) {
            if (entries.size() >= NpcSchedule.MAX_ENTRIES) {
                say("That's the most entries one schedule can hold.", ChatFormatting.RED);
                return true;
            }
            NotchWidgets.click();
            // A new entry lands an hour after the last one, which is usually near where it's wanted.
            int start = entries.isEmpty() ? ScheduleEntry.ticksForClock(8, 0)
                    : (entries.get(entries.size() - 1).time() + 1000) % ScheduleEntry.DAY_LENGTH;
            entries.add(ScheduleEntry.of(start, NpcStance.STAND));
            sortKeepingSelection();
            selected = entries.size() - 1;
            sortKeepingSelection();
            return true;
        }
        if (over(mx, my, px + LIST_X + 62, py + 194, 58, 14) && selected >= 0 && selected < entries.size()) {
            NotchWidgets.click();
            entries.remove(selected);
            if (selected >= entries.size()) selected = entries.size() - 1;
            return true;
        }
        if (brokenCount() > 0 && over(mx, my, px + 150, py + 211, 76, 14)) {
            NotchWidgets.click();
            int i = firstBroken();
            if (i >= 0) {
                selected = i;
                scroll = Math.max(0, Math.min(i, Math.max(0, entries.size() - VISIBLE_ROWS)));
                requestAnchorTool(i);
            }
            return true;
        }
        if (over(mx, my, px + 232, py + 211, 96, 14)) {
            NotchWidgets.click();
            save();
            NotchPacketsClient.sendNpcEditorReopen(npcId, 1);
            return true;
        }
        if (selected >= 0 && selected < entries.size() && paneClicked(mx, my, px, py)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean paneClicked(int mx, int my, int px, int py) {
        ScheduleEntry e = entries.get(selected);
        int x = px + PANE_X;
        boolean fine = hasShiftDown();

        if (over(mx, my, x + 60, py + 41, 18, 14)) {
            NotchWidgets.tick();
            entries.set(selected, e.withTime(e.time() - (fine ? 250 : 1000)));
            sortKeepingSelection();
            return true;
        }
        if (over(mx, my, x + 114, py + 41, 18, 14)) {
            NotchWidgets.tick();
            entries.set(selected, e.withTime(e.time() + (fine ? 250 : 1000)));
            sortKeepingSelection();
            return true;
        }
        if (over(mx, my, x + 60, py + 63, 72, 14)) {
            NotchWidgets.tick();
            NpcStance[] all = NpcStance.values();
            entries.set(selected, e.withStance(all[(e.stance().ordinal() + 1) % all.length]));
            return true;
        }
        if (e.stance().needsSpot() && over(mx, my, x, py + 116, 90, 14)) {
            NotchWidgets.click();
            requestAnchorTool(selected);
            return true;
        }
        if (e.stance() == NpcStance.STAND) {
            float step = fine ? 15f : 45f;
            if (over(mx, my, x + 60, py + 135, 18, 14)) {
                NotchWidgets.tick();
                entries.set(selected, e.withFacing(e.facing() - step));
                return true;
            }
            if (over(mx, my, x + 158, py + 135, 18, 14)) {
                NotchWidgets.tick();
                entries.set(selected, e.withFacing(e.facing() + step));
                return true;
            }
        }
        if (e.stance() == NpcStance.WANDER) {
            if (over(mx, my, x + 60, py + 135, 18, 14)) {
                NotchWidgets.tick();
                entries.set(selected, e.withRadius(e.radius() - (fine ? 1 : 4)));
                return true;
            }
            if (over(mx, my, x + 114, py + 135, 18, 14)) {
                NotchWidgets.tick();
                entries.set(selected, e.withRadius(e.radius() + (fine ? 1 : 4)));
                return true;
            }
        }
        if (over(mx, my, x + 106, py + 158, 40, 14)) {
            NotchWidgets.tick();
            entries.set(selected, e.withRoleOpen(!e.roleOpen()));
            return true;
        }
        if (over(mx, my, x, py + 178, 146, 14)) {
            NotchWidgets.click();
            // Hands the edited list straight back to this screen. The schedule is saved in one
            // piece, so an entry's actions have no business making their own trip to the server.
            final int editing = selected;
            if (this.minecraft != null) {
                this.minecraft.setScreen(new NpcScheduleActionsScreen(this, e.clock(), e.onBegin(),
                        updated -> {
                            if (editing >= 0 && editing < entries.size()) {
                                entries.set(editing, entries.get(editing).withActions(updated));
                            }
                        }));
            }
            return true;
        }
        return false;
    }

    private void requestAnchorTool(int entryIndex) {
        save();
        NotchPacketsClient.sendNpcScheduleTool(npcId, entryIndex);
        this.onClose();
    }

    private void save() {
        NpcSchedule out = new NpcSchedule();
        out.setEnabled(enabled);
        out.setEnforceHours(enforceHours);
        out.setEntries(entries);
        NotchPacketsClient.sendNpcScheduleSave(npcId, out.toNbt());
    }

    private void say(String text, ChatFormatting color) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(text).withStyle(color), false);
        }
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        int maxScroll = Math.max(0, entries.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount)));
        return true;
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render(). This screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
