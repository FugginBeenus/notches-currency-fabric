package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.anim.NpcAnimation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class AnimationEditorScreen extends Screen {

    private static final int W = 380, H = 308;
    private static final String[] PART_NAMES = {"Head", "Body", "R Arm", "L Arm", "R Leg", "L Leg"};
    private static final String[] AXIS = {"X", "Y", "Z"};

    private static final int LX = 10, LW = 112;
    private static final int PREV_X = 128, PREV_Y = 40, PREV_W = 106, PREV_H = 150;
    private static final int RX = 242, RW = 128, SLIDER_W = 96;

    private static final int TL_X = 10, TL_TOP = 200, LANE_H = 10;
    private static final int SNAP = 2;

    private final String name;
    private final NpcAnimation anim;
    private int key = 0, part = 0;
    private boolean pivotMode, playing, onion = true;
    private float playTicks;
    private int dragging = -1, dragKey = -1;
    private int limbPart = -1, limbFromX, limbFromY, limbBaseA, limbBaseB;
    private int px, py;
    private net.minecraft.client.gui.components.EditBox lengthField;

    public AnimationEditorScreen(String name, CompoundTag existing) {
        super(Component.literal("Animation"));
        this.name = name == null ? "" : name;
        this.anim = existing != null ? NpcAnimation.fromNbt(existing) : new NpcAnimation(this.name);
        this.anim.setName(this.name);
        if (this.anim.frames().isEmpty()) this.anim.frames().add(new NpcAnimation.Frame());
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        lengthField = new net.minecraft.client.gui.components.EditBox(
                this.font, px + LX + 47, py + 43, 30, 10, Component.empty());
        lengthField.setMaxLength(6);
        lengthField.setBordered(false);
        lengthField.setValue(String.format("%.1f", anim.lengthTicks() / 20.0f));
        lengthField.setResponder(v -> {
            try {
                anim.setLengthTicks(Math.round(Float.parseFloat(v.trim()) * 20f));
            } catch (NumberFormatException notANumber) {
                // leave the length alone while they are still typing
            }
        });
        addRenderableWidget(lengthField);
    }

    private NpcAnimation.Frame current() {
        if (key >= anim.frames().size()) key = anim.frames().size() - 1;
        if (key < 0) key = 0;
        return anim.frames().get(key);
    }

    private int slotOf(int axis) { return (pivotMode ? NpcAnimation.SLOTS : 0) + part * 3 + axis; }

    private int limit() { return pivotMode ? 24 : 180; }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int sliderY(int axis) { return py + 118 + axis * 20; }

    private int tlW() { return W - TL_X * 2; }

    private int timeToX(float ticks) {
        return px + TL_X + Math.round(ticks / Math.max(1, anim.lengthTicks()) * (tlW() - 4));
    }

    private int xToTime(int mx) {
        float pct = (mx - (px + TL_X)) / (float) Math.max(1, tlW() - 4);
        int raw = Math.round(Math.max(0f, Math.min(1f, pct)) * anim.lengthTicks());
        if (net.fugginbeenus.notchcurrency.compat.Render.shiftDown()) return raw;
        return Math.round(raw / (float) SNAP) * SNAP;
    }

    private void syncLength() {
        if (lengthField != null) {
            lengthField.setValue(String.format("%.1f", anim.lengthTicks() / 20.0f));
        }
    }

    private NotchNpcEntity mannequin, ghostDoll;

    private NotchNpcEntity build() {
        var mc = this.minecraft;
        NotchNpcEntity e = new NotchNpcEntity(
                net.fugginbeenus.notchcurrency.registry.ModEntities.NOTCH_NPC, mc.level);
        e.setId(-1);
        e.setYRot(180);
        e.yRotO = 180;
        e.yBodyRot = e.yBodyRotO = 180;
        e.yHeadRot = e.yHeadRotO = 180;
        e.setNpcPose(NotchNpcEntity.POSE_STANDING);
        e.setPoseAnim(NotchNpcEntity.ANIM_STATUE);
        e.setCustomName(null);
        return e;
    }

    private NotchNpcEntity ghostDoll() {
        var mc = this.minecraft;
        if (mc == null || mc.level == null) return null;
        if (ghostDoll == null || ghostDoll.isRemoved()) ghostDoll = build();
        return ghostDoll;
    }

    private NotchNpcEntity preview() {
        var mc = this.minecraft;
        if (mc == null || mc.level == null) return null;
        if (mannequin == null || mannequin.isRemoved()) {
            mannequin = new NotchNpcEntity(
                    net.fugginbeenus.notchcurrency.registry.ModEntities.NOTCH_NPC, mc.level);
            mannequin.setId(-1);
            mannequin.setYRot(180);
            mannequin.yRotO = 180;
            mannequin.yBodyRot = mannequin.yBodyRotO = 180;
            mannequin.yHeadRot = mannequin.yHeadRotO = 180;
            mannequin.setNpcPose(NotchNpcEntity.POSE_STANDING);
            mannequin.setPoseAnim(NotchNpcEntity.ANIM_STATUE);
            mannequin.setCustomName(null);
        }
        return mannequin;
    }

    private void drawPose(GuiGraphics ctx, NotchNpcEntity npc, float[] shot,
                          float alpha, boolean ghost) {
        var lib = net.fugginbeenus.notchcurrency.client.npc.AnimationLibrary.class;
        String slot = ghost
                ? net.fugginbeenus.notchcurrency.client.npc.AnimationLibrary.GHOST
                : net.fugginbeenus.notchcurrency.client.npc.AnimationLibrary.PREVIEW;
        NpcAnimation still = new NpcAnimation(slot);
        still.frames().add(NpcAnimation.frameOf(shot));
        if (ghost) net.fugginbeenus.notchcurrency.client.npc.AnimationLibrary.setGhost(still);
        else net.fugginbeenus.notchcurrency.client.npc.AnimationLibrary.setPreview(still);
        npc.setIdleAnimation(slot);
        npc.setAlpha(alpha);
        net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx,
                px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H - 12, 42, 0f, 0f, npc);
    }

    private int limbAt(int mx, int my) {
        int cx = px + PREV_X + PREV_W / 2;
        int feet = py + PREV_Y + PREV_H - 12;
        if (over(mx, my, cx - 9, feet - 66, 18, 16)) return 0;
        if (over(mx, my, cx - 10, feet - 50, 20, 24)) return 1;
        if (over(mx, my, cx - 20, feet - 50, 10, 24)) return 2;
        if (over(mx, my, cx + 10, feet - 50, 10, 24)) return 3;
        if (over(mx, my, cx - 10, feet - 26, 10, 26)) return 4;
        if (over(mx, my, cx, feet - 26, 10, 26)) return 5;
        return -1;
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        if (playing) playTicks += delta;

        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, name.isBlank() ? "Animation" : name, px + W / 2, py + 8);
        NotchWidgets.divider(ctx, px + 8, py + 22, W - 16);

        ctx.drawString(this.font, "Length", px + LX, py + 30, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + LX, py + 40, 18, 14, "-",
                over(mouseX, mouseY, px + LX, py + 40, 18, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 22, py + 40, 18, 14, "+",
                over(mouseX, mouseY, px + LX + 22, py + 40, 18, 14));
        NotchWidgets.inset(ctx, px + LX + 44, py + 40, 36, 14, NotchTheme.DEEP);
        ctx.drawString(this.font, "s", px + LX + 82, py + 44, NotchTheme.TEXT_MUTED, false);
        ctx.drawString(this.font, "Speed " + anim.speedPercent() + "%", px + LX, py + 58,
                NotchTheme.TEXT_MUTED, false);
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 70, py + 56, 18, 14, "-",
                over(mouseX, mouseY, px + LX + 70, py + 56, 18, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 92, py + 56, 18, 14, "+",
                over(mouseX, mouseY, px + LX + 92, py + 56, 18, 14));

        NotchWidgets.neutralButton(ctx, this.font, px + LX, py + 74, 54, 14,
                anim.loop() ? "Loops" : "Once", over(mouseX, mouseY, px + LX, py + 74, 54, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 58, py + 74, 54, 14,
                onion ? "Ghost on" : "Ghost off", over(mouseX, mouseY, px + LX + 58, py + 74, 54, 14));

        NotchWidgets.divider(ctx, px + LX, py + 94, LW);
        ctx.drawString(this.font, "Key " + (key + 1) + " at "
                        + String.format("%.2f", current().time / 20.0f) + "s",
                px + LX, py + 100, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + LX, py + 110, LW, 14,
                NpcAnimation.easeName(current().ease),
                over(mouseX, mouseY, px + LX, py + 110, LW, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX, py + 128, 54, 14, "Copy",
                over(mouseX, mouseY, px + LX, py + 128, 54, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 58, py + 128, 54, 14, "Mirror",
                over(mouseX, mouseY, px + LX + 58, py + 128, 54, 14));
        NotchWidgets.dangerButton(ctx, this.font, px + LX, py + 146, LW, 14, "Delete key",
                over(mouseX, mouseY, px + LX, py + 146, LW, 14));
        NotchWidgets.primaryButton(ctx, this.font, px + LX, py + 166, LW, 16,
                playing ? "Stop" : "Play", over(mouseX, mouseY, px + LX, py + 166, LW, 16));

        NotchWidgets.inset(ctx, px + PREV_X, py + PREV_Y, PREV_W, PREV_H, NotchTheme.DEEP);
        NotchNpcEntity npc = preview();
        if (npc != null) {
            float at = playing ? playTicks : current().time;
            if (onion && !playing && anim.frames().size() > 1) {
                NotchNpcEntity doll = ghostDoll();
                int back = (key - 1 + anim.frames().size()) % anim.frames().size();
                float[] ghost = anim.sample(anim.frames().get(back).time);
                if (doll != null && ghost != null) drawPose(ctx, doll, ghost, 0.3f, true);
            }
            float[] shot = anim.sample(at);
            if (shot != null) drawPose(ctx, npc, shot, 1.0f, false);
            if (!playing) {
                int hover = limbAt(mouseX, mouseY);
                if (hover >= 0) {
                    ctx.drawString(this.font, PART_NAMES[hover], px + PREV_X + 4, py + PREV_Y + 4,
                            NotchTheme.TEXT_MUTED, false);
                }
            }
        }

        for (int i = 0; i < 6; i++) {
            int bx = px + RX + (i % 2) * 66;
            int by = py + 30 + (i / 2) * 18;
            boolean hover = over(mouseX, mouseY, bx, by, 62, 16);
            if (i == part) {
                NotchWidgets.primaryButton(ctx, this.font, bx, by, 62, 16, PART_NAMES[i], hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, bx, by, 62, 16, PART_NAMES[i], hover);
            }
        }
        ctx.drawString(this.font, pivotMode ? "Move the part" : "Turn the part",
                px + RX, py + 90, NotchTheme.TEXT_MUTED, false);
        boolean rotHover = over(mouseX, mouseY, px + RX, py + 100, 62, 14);
        boolean pivHover = over(mouseX, mouseY, px + RX + 66, py + 100, 62, 14);
        if (pivotMode) {
            NotchWidgets.neutralButton(ctx, this.font, px + RX, py + 100, 62, 14, "Rotate", rotHover);
            NotchWidgets.primaryButton(ctx, this.font, px + RX + 66, py + 100, 62, 14, "Move", pivHover);
        } else {
            NotchWidgets.primaryButton(ctx, this.font, px + RX, py + 100, 62, 14, "Rotate", rotHover);
            NotchWidgets.neutralButton(ctx, this.font, px + RX + 66, py + 100, 62, 14, "Move", pivHover);
        }

        NpcAnimation.Frame f = current();
        float[] shown = anim.sample(f.time);
        for (int axis = 0; axis < 3; axis++) {
            int ry = sliderY(axis);
            int slot = slotOf(axis);
            boolean held = f.has(slot);
            int value = held ? f.value(slot) : (shown == null ? 0 : Math.round(shown[slot]));
            ctx.drawString(this.font, AXIS[axis], px + RX, ry + 3,
                    held ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);
            NotchWidgets.inset(ctx, px + RX + 12, ry, SLIDER_W, 14, NotchTheme.DEEP);
            float pct = (value + limit()) / (float) (limit() * 2);
            NotchWidgets.pill(ctx, px + RX + 12 + Math.round(pct * (SLIDER_W - 6)), ry + 1, 6, 12);
            NotchWidgets.centerText(ctx, this.font, (held ? "" : "~") + value,
                    px + RX + 12 + SLIDER_W / 2, ry + 3,
                    held ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);
        }
        NotchWidgets.dangerButton(ctx, this.font, px + RX, py + 180, RW, 14, "Clear this part",
                over(mouseX, mouseY, px + RX, py + 180, RW, 14));
        if (over(mouseX, mouseY, px + RX, py + 180, RW, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Clears this part on this keyframe only.")
                            .withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }

        drawSheet(ctx, mouseX, mouseY);

        NotchWidgets.primaryButton(ctx, this.font, px + 70, py + H - 24, 110, 16, "Save & Back",
                over(mouseX, mouseY, px + 70, py + H - 24, 110, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + 200, py + H - 24, 110, 16, "Back",
                over(mouseX, mouseY, px + 200, py + H - 24, 110, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
    }

    private boolean laneHasKey(NpcAnimation.Frame fr, int row) {
        for (int axis = 0; axis < 3; axis++) {
            if (fr.has(row * 3 + axis) || fr.has(NpcAnimation.SLOTS + row * 3 + axis)) return true;
        }
        return false;
    }

    private void drawSheet(GuiGraphics ctx, int mouseX, int mouseY) {
        int tw = tlW();
        NotchWidgets.inset(ctx, px + TL_X, py + TL_TOP - 2, tw, 6 * LANE_H + 12, NotchTheme.DEEP);

        int step = SNAP;
        float perTick = (tw - 4) / (float) Math.max(1, anim.lengthTicks());
        while (step * perTick < 4f && step < 200) step *= 2;
        for (int t = 0; t <= anim.lengthTicks(); t += step) {
            int gx = timeToX(t);
            boolean second = t % 20 == 0;
            ctx.fill(gx, py + TL_TOP - 1, gx + 1, py + TL_TOP + 6 * LANE_H + 9,
                    second ? 0x40FFFFFF : 0x18FFFFFF);
        }

        for (int row = 0; row < 6; row++) {
            int ry = py + TL_TOP + row * LANE_H;
            ctx.drawString(this.font, PART_NAMES[row].substring(0, 1),
                    px + TL_X + 2, ry + 1, NotchTheme.TEXT_MUTED, false);
            ctx.fill(px + TL_X + 10, ry + 4, px + TL_X + tw - 2, ry + 5, 0x33FFFFFF);
            for (int i = 0; i < anim.frames().size(); i++) {
                NpcAnimation.Frame fr = anim.frames().get(i);
                if (!laneHasKey(fr, row)) continue;
                int kx = timeToX(fr.time);
                ctx.fill(kx - 2, ry + 1, kx + 2, ry + 7, i == key ? 0xFF5FBF63 : 0xFFB9BEB0);
            }
        }

        int handleY = py + TL_TOP + 6 * LANE_H + 1;
        for (int i = 0; i < anim.frames().size(); i++) {
            int kx = timeToX(anim.frames().get(i).time);
            boolean hot = over(mouseX, mouseY, kx - 4, handleY, 8, 8);
            ctx.fill(kx - 3, handleY, kx + 3, handleY + 7,
                    i == key ? 0xFF5FBF63 : (hot ? 0xFFE8EBE0 : 0xFF8A8F80));
        }

        float head = playing
                ? (playTicks * (anim.speedPercent() / 100f)) % Math.max(1, anim.lengthTicks())
                : current().time;
        int hx = timeToX(head);
        ctx.fill(hx, py + TL_TOP - 2, hx + 1, py + TL_TOP + 6 * LANE_H + 10, 0xFFFFFFFF);
    }

    private void setFromMouse(int axis, double mouseX) {
        float pct = (float) ((mouseX - (px + RX + 12)) / (SLIDER_W - 6));
        pct = Math.max(0f, Math.min(1f, pct));
        current().put(slotOf(axis), Math.round(pct * limit() * 2) - limit());
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //?}
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;

            int handleY = py + TL_TOP + 6 * LANE_H + 1;
            for (int i = 0; i < anim.frames().size(); i++) {
                int kx = timeToX(anim.frames().get(i).time);
                if (over(mx, my, kx - 4, handleY, 8, 8)) {
                    key = i;
                    dragKey = i;
                    playing = false;
                    return true;
                }
            }
            for (int row = 0; row < 6; row++) {
                int ry = py + TL_TOP + row * LANE_H;
                if (my < ry || my >= ry + LANE_H) continue;
                for (int i = 0; i < anim.frames().size(); i++) {
                    NpcAnimation.Frame fr = anim.frames().get(i);
                    if (!laneHasKey(fr, row)) continue;
                    if (!over(mx, my, timeToX(fr.time) - 4, ry, 8, LANE_H)) continue;
                    NotchWidgets.click();
                    key = i;
                    part = row;
                    playing = false;
                    return true;
                }
            }
            if (over(mx, my, px + TL_X, py + TL_TOP - 2, tlW(), 6 * LANE_H + 12)) {
                int made = anim.addAt(xToTime(mx));
                if (made >= 0) {
                    NotchWidgets.click();
                    key = made;
                    playing = false;
                }
                return true;
            }

            int limb = limbAt(mx, my);
            if (limb >= 0 && !playing) {
                NotchWidgets.click();
                part = limb;
                limbPart = limb;
                limbFromX = mx;
                limbFromY = my;
                limbBaseA = current().value(slotOf(0));
                limbBaseB = current().value(slotOf(1));
                return true;
            }

            for (int axis = 0; axis < 3; axis++) {
                if (over(mx, my, px + RX + 12, sliderY(axis), SLIDER_W, 14)) {
                    dragging = axis;
                    setFromMouse(axis, mouseX);
                    return true;
                }
            }
            if (over(mx, my, px + LX, py + 40, 18, 14)) {
                NotchWidgets.click();
                anim.setLengthTicks(anim.lengthTicks()
                        - (net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 2 : 10));
                syncLength();
                return true;
            }
            if (over(mx, my, px + LX + 22, py + 40, 18, 14)) {
                NotchWidgets.click();
                anim.setLengthTicks(anim.lengthTicks()
                        + (net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 2 : 10));
                syncLength();
                return true;
            }
            if (over(mx, my, px + LX + 70, py + 56, 18, 14)) {
                NotchWidgets.click();
                anim.setSpeedPercent(anim.speedPercent() - 25);
                return true;
            }
            if (over(mx, my, px + LX + 92, py + 56, 18, 14)) {
                NotchWidgets.click();
                anim.setSpeedPercent(anim.speedPercent() + 25);
                return true;
            }
            if (over(mx, my, px + LX, py + 74, 54, 14)) {
                NotchWidgets.click();
                anim.setLoop(!anim.loop());
                return true;
            }
            if (over(mx, my, px + LX + 58, py + 74, 54, 14)) {
                NotchWidgets.click();
                onion = !onion;
                return true;
            }
            if (over(mx, my, px + LX, py + 110, LW, 14)) {
                NotchWidgets.click();
                current().ease = (current().ease + 1) % 4;
                return true;
            }
            if (over(mx, my, px + LX, py + 128, 54, 14)
                    && anim.frames().size() < NpcAnimation.MAX_FRAMES) {
                NotchWidgets.click();
                NpcAnimation.Frame copy = current().copy();
                copy.time = Math.min(anim.lengthTicks(), current().time + 5);
                anim.frames().add(copy);
                anim.sort();
                key = anim.frames().indexOf(copy);
                return true;
            }
            if (over(mx, my, px + LX + 58, py + 128, 54, 14)) {
                NotchWidgets.click();
                mirror(current());
                return true;
            }
            if (over(mx, my, px + LX, py + 146, LW, 14) && anim.frames().size() > 1) {
                NotchWidgets.click();
                anim.frames().remove(key);
                if (key >= anim.frames().size()) key = anim.frames().size() - 1;
                return true;
            }
            if (over(mx, my, px + LX, py + 166, LW, 16)) {
                NotchWidgets.click();
                playing = !playing;
                playTicks = 0f;
                return true;
            }
            for (int i = 0; i < 6; i++) {
                if (over(mx, my, px + RX + (i % 2) * 66, py + 30 + (i / 2) * 18, 62, 16)) {
                    NotchWidgets.click();
                    part = i;
                    return true;
                }
            }
            if (over(mx, my, px + RX, py + 100, 62, 14)) {
                NotchWidgets.click();
                pivotMode = false;
                return true;
            }
            if (over(mx, my, px + RX + 66, py + 100, 62, 14)) {
                NotchWidgets.click();
                pivotMode = true;
                return true;
            }
            if (over(mx, my, px + RX, py + 180, RW, 14)) {
                NotchWidgets.click();
                NpcAnimation.Frame here = current();
                for (int axis = 0; axis < 3; axis++) {
                    int rot = part * 3 + axis;
                    here.clear(rot);
                    here.clear(NpcAnimation.SLOTS + rot);
                    here.angles[rot] = 0;
                    here.offsets[rot] = 0;
                }
                return true;
            }
            if (over(mx, my, px + 70, py + H - 24, 110, 16)) {
                NotchWidgets.click();
                anim.setName(name);
                anim.sort();
                NotchPacketsClient.sendAnimSave(anim.toNbt());
                NotchPacketsClient.sendAnimDesign();
                return true;
            }
            if (over(mx, my, px + 200, py + H - 24, 110, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendAnimDesign();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void mirror(NpcAnimation.Frame f) {
        for (int block = 0; block < 2; block++) {
            int base = block * NpcAnimation.SLOTS;
            swapPart(f, base, 2, 3);
            swapPart(f, base, 4, 5);
        }
        for (int block = 0; block < 2; block++) {
            int base = block * NpcAnimation.SLOTS;
            for (int p = 0; p < 6; p++) {
                int yaw = base + p * 3 + 1, roll = base + p * 3 + 2;
                if (f.has(yaw)) f.put(yaw, -f.value(yaw));
                if (f.has(roll)) f.put(roll, -f.value(roll));
            }
        }
    }

    private void swapPart(NpcAnimation.Frame f, int base, int a, int b) {
        for (int axis = 0; axis < 3; axis++) {
            int sa = base + a * 3 + axis, sb = base + b * 3 + axis;
            boolean ha = f.has(sa), hb = f.has(sb);
            int va = f.value(sa), vb = f.value(sb);
            if (hb) f.put(sa, vb); else f.clear(sa);
            if (ha) f.put(sb, va); else f.clear(sb);
        }
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x(), mouseY = event.y();
    *///?} else {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
    //?}
        if (dragKey >= 0 && dragKey < anim.frames().size()) {
            anim.frames().get(dragKey).time = xToTime((int) mouseX);
            NpcAnimation.Frame held = anim.frames().get(dragKey);
            anim.sort();
            key = anim.frames().indexOf(held);
            dragKey = key;
            return true;
        }
        if (limbPart >= 0) {
            int sx = slotOf(0), sy = slotOf(1);
            current().put(sx, clampLimb(limbBaseA + (int) (mouseY - limbFromY) * 2));
            current().put(sy, clampLimb(limbBaseB + (int) (mouseX - limbFromX) * 2));
            return true;
        }
        if (dragging >= 0) {
            setFromMouse(dragging, mouseX);
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseDragged(event, dx, dy);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        //?}
    }

    private int clampLimb(int v) {
        int lim = limit();
        return Math.max(-lim, Math.min(lim, v));
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        dragging = -1;
        dragKey = -1;
        limbPart = -1;
        return super.mouseReleased(event);
    }
    *///?} else {
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = -1;
        dragKey = -1;
        limbPart = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    //?}

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        boolean typing = lengthField != null && lengthField.isFocused();
        if (!typing && (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT)) {
            int by = keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT ? -1 : 1;
            if (net.fugginbeenus.notchcurrency.compat.Render.shiftDown()) by *= SNAP;
            NpcAnimation.Frame held = current();
            held.time = Math.max(0, Math.min(anim.lengthTicks(), held.time + by));
            anim.sort();
            key = anim.frames().indexOf(held);
            playing = false;
            return true;
        }
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    @Override
    public boolean isPauseScreen() { return false; }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    }
    *///?}
}
