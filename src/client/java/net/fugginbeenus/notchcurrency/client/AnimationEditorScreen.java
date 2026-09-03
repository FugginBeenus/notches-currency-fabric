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

    private static final int W = 360, H = 250;
    private static final String[] PART_NAMES = {"Head", "Body", "R Arm", "L Arm", "R Leg", "L Leg"};
    private static final String[] AXIS = {"X", "Y", "Z"};

    private static final int LX = 10, LW = 118;
    private static final int PREV_X = 134, PREV_Y = 44, PREV_W = 96, PREV_H = 132;
    private static final int RX = 238, RW = 112;
    private static final int SLIDER_W = 88;
    private static final int TL_X = 10;

    private final String name;
    private final NpcAnimation anim;
    private int frame = 0, part = 0;
    private boolean pivotMode, playing;
    private float playTicks;
    private int dragging = -1;
    private int px, py;

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
    }

    private NpcAnimation.Frame current() {
        if (frame >= anim.frames().size()) frame = anim.frames().size() - 1;
        if (frame < 0) frame = 0;
        return anim.frames().get(frame);
    }

    private int slotOf(int axis) {
        return (pivotMode ? NpcAnimation.SLOTS : 0) + part * 3 + axis;
    }

    private int limit() { return pivotMode ? 24 : 180; }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private NotchNpcEntity mannequin;

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

    private int sliderY(int axis) { return py + 116 + axis * 20; }

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

        ctx.drawString(this.font, "Speed " + anim.speedPercent() + "%", px + LX, py + 30,
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + LX, py + 40, 18, 14, "-",
                over(mouseX, mouseY, px + LX, py + 40, 18, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 22, py + 40, 18, 14, "+",
                over(mouseX, mouseY, px + LX + 22, py + 40, 18, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 46, py + 40, LW - 46, 14,
                anim.smooth() ? "Smooth" : "Snap",
                over(mouseX, mouseY, px + LX + 46, py + 40, LW - 46, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX, py + 58, LW, 14,
                anim.loop() ? "Loops" : "Plays once",
                over(mouseX, mouseY, px + LX, py + 58, LW, 14));

        NotchWidgets.divider(ctx, px + LX, py + 78, LW);
        ctx.drawString(this.font, "Keyframe " + (frame + 1) + " of " + anim.frames().size(),
                px + LX, py + 84, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + LX, py + 94, 26, 14, "<",
                over(mouseX, mouseY, px + LX, py + 94, 26, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 30, py + 94, 26, 14, ">",
                over(mouseX, mouseY, px + LX + 30, py + 94, 26, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 62, py + 94, 26, 14, "<<",
                over(mouseX, mouseY, px + LX + 62, py + 94, 26, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 92, py + 94, 26, 14, ">>",
                over(mouseX, mouseY, px + LX + 92, py + 94, 26, 14));

        ctx.drawString(this.font, "Length " + String.format("%.1f", anim.lengthTicks() / 20.0f) + "s",
                px + LX, py + 114, NotchTheme.TEXT_DARK, false);
        if (over(mouseX, mouseY, px + LX, py + 112, LW, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Length").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("How long the whole animation runs, up to 30s.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Keyframes spread evenly across it.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Hold shift for smaller steps.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
        }
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 62, py + 112, 26, 14, "-",
                over(mouseX, mouseY, px + LX + 62, py + 112, 26, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 92, py + 112, 26, 14, "+",
                over(mouseX, mouseY, px + LX + 92, py + 112, 26, 14));

        NotchWidgets.neutralButton(ctx, this.font, px + LX, py + 132, 56, 14, "Add",
                over(mouseX, mouseY, px + LX, py + 132, 56, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + LX + 62, py + 132, 56, 14, "Copy",
                over(mouseX, mouseY, px + LX + 62, py + 132, 56, 14));
        NotchWidgets.dangerButton(ctx, this.font, px + LX, py + 150, LW, 14, "Remove keyframe",
                over(mouseX, mouseY, px + LX, py + 150, LW, 14));
        NotchWidgets.primaryButton(ctx, this.font, px + LX, py + 170, LW, 16,
                playing ? "Stop" : "Play", over(mouseX, mouseY, px + LX, py + 170, LW, 16));

        NotchWidgets.inset(ctx, px + PREV_X, py + PREV_Y, PREV_W, PREV_H, NotchTheme.DEEP);
        NotchNpcEntity npc = preview();
        if (npc != null) {
            float[] shot = anim.sample(playing ? playTicks : frame * anim.segmentTicks());
            NpcAnimation still = new NpcAnimation(net.fugginbeenus.notchcurrency.client.npc
                    .AnimationLibrary.PREVIEW);
            still.frames().add(NpcAnimation.frameOf(shot));
            net.fugginbeenus.notchcurrency.client.npc.AnimationLibrary.setPreview(still);
            npc.setIdleAnimation(net.fugginbeenus.notchcurrency.client.npc.AnimationLibrary.PREVIEW);
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx,
                    px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H - 14, 44, 0f, 0f, npc);
        }

        for (int i = 0; i < 6; i++) {
            int bx = px + RX + (i % 2) * 58;
            int by = py + 30 + (i / 2) * 18;
            boolean hover = over(mouseX, mouseY, bx, by, 54, 16);
            if (i == part) {
                NotchWidgets.primaryButton(ctx, this.font, bx, by, 54, 16, PART_NAMES[i], hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, bx, by, 54, 16, PART_NAMES[i], hover);
            }
        }
        ctx.drawString(this.font, pivotMode ? "Move the part" : "Turn the part",
                px + RX, py + 90, NotchTheme.TEXT_MUTED, false);
        boolean rotHover = over(mouseX, mouseY, px + RX, py + 100, 54, 14);
        boolean pivHover = over(mouseX, mouseY, px + RX + 58, py + 100, 54, 14);
        if (pivotMode) {
            NotchWidgets.neutralButton(ctx, this.font, px + RX, py + 100, 54, 14, "Rotate", rotHover);
            NotchWidgets.primaryButton(ctx, this.font, px + RX + 58, py + 100, 54, 14, "Move", pivHover);
        } else {
            NotchWidgets.primaryButton(ctx, this.font, px + RX, py + 100, 54, 14, "Rotate", rotHover);
            NotchWidgets.neutralButton(ctx, this.font, px + RX + 58, py + 100, 54, 14, "Move", pivHover);
        }

        NpcAnimation.Frame f = current();
        float[] shown = anim.sample(frame * anim.segmentTicks());
        for (int axis = 0; axis < 3; axis++) {
            int ry = sliderY(axis);
            int slot = slotOf(axis);
            boolean held = f.has(slot);
            int value = held ? f.value(slot)
                    : (shown == null ? 0 : Math.round(shown[slot]));
            ctx.drawString(this.font, AXIS[axis], px + RX, ry + 3,
                    held ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);
            NotchWidgets.inset(ctx, px + RX + 12, ry, SLIDER_W, 14, NotchTheme.DEEP);
            float pct = (value + limit()) / (float) (limit() * 2);
            int knob = px + RX + 12 + Math.round(pct * (SLIDER_W - 6));
            NotchWidgets.pill(ctx, knob, ry + 1, 6, 12);
            NotchWidgets.centerText(ctx, this.font,
                    (held ? "" : "~") + value + (pivotMode ? "" : " deg"),
                    px + RX + 12 + SLIDER_W / 2, ry + 3,
                    held ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);
        }
        NotchWidgets.dangerButton(ctx, this.font, px + RX, py + 180, RW, 14, "Reset this part",
                over(mouseX, mouseY, px + RX, py + 180, RW, 14));
        if (over(mouseX, mouseY, px + RX, py + 180, RW, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Reset this part").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("Wipes this part on every keyframe, turn and").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("move, so it stands still for the whole thing.").withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }

        NotchWidgets.divider(ctx, px + 8, py + H - 44, W - 16);
        ctx.drawString(this.font, anim.summary(), px + 10, py + H - 52, NotchTheme.TEXT_MUTED, false);
        int tx = px + TL_X, tw = W - TL_X * 2;
        NotchWidgets.inset(ctx, tx, py + H - 40, tw, 14, NotchTheme.DEEP);
        int cells = anim.frames().size();
        for (int i = 0; i < cells; i++) {
            int cx = tx + 2 + Math.round(i / (float) cells * (tw - 4));
            int cw = Math.max(3, (tw - 4) / cells - 1);
            ctx.fill(cx, py + H - 38, cx + cw, py + H - 28,
                    i == frame ? 0xFF5FBF63 : 0xFF8A8A8A);
        }
        float head = playing
                ? (anim.totalTicks() <= 0 ? 0f
                        : (playTicks * (anim.speedPercent() / 100f)) % anim.totalTicks() / anim.totalTicks())
                : (cells <= 0 ? 0f : (frame + 0.5f) / cells);
        int hx = tx + 2 + Math.round(head * (tw - 4));
        ctx.fill(hx - 1, py + H - 41, hx + 1, py + H - 25, 0xFFFFFFFF);

        NotchWidgets.primaryButton(ctx, this.font, px + 60, py + H - 24, 110, 16, "Save & Back",
                over(mouseX, mouseY, px + 60, py + H - 24, 110, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + 190, py + H - 24, 110, 16, "Back",
                over(mouseX, mouseY, px + 190, py + H - 24, 110, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
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
            for (int axis = 0; axis < 3; axis++) {
                if (over(mx, my, px + RX + 12, sliderY(axis), SLIDER_W, 14)) {
                    dragging = axis;
                    setFromMouse(axis, mouseX);
                    return true;
                }
            }
            if (over(mx, my, px + LX, py + 40, 18, 14)) {
                NotchWidgets.click();
                anim.setSpeedPercent(anim.speedPercent() - 25);
                return true;
            }
            if (over(mx, my, px + LX + 22, py + 40, 18, 14)) {
                NotchWidgets.click();
                anim.setSpeedPercent(anim.speedPercent() + 25);
                return true;
            }
            if (over(mx, my, px + LX + 46, py + 40, LW - 46, 14)) {
                NotchWidgets.click();
                anim.setSmooth(!anim.smooth());
                return true;
            }
            if (over(mx, my, px + LX, py + 58, LW, 14)) {
                NotchWidgets.click();
                anim.setLoop(!anim.loop());
                return true;
            }
            if (over(mx, my, px + LX, py + 94, 26, 14)) {
                NotchWidgets.click();
                frame = Math.max(0, frame - 1);
                return true;
            }
            if (over(mx, my, px + LX + 30, py + 94, 26, 14)) {
                NotchWidgets.click();
                frame = Math.min(anim.frames().size() - 1, frame + 1);
                return true;
            }
            if (over(mx, my, px + LX + 62, py + 94, 26, 14) && frame > 0) {
                NotchWidgets.click();
                anim.frames().add(frame - 1, anim.frames().remove(frame));
                frame--;
                return true;
            }
            if (over(mx, my, px + LX + 92, py + 94, 26, 14) && frame < anim.frames().size() - 1) {
                NotchWidgets.click();
                anim.frames().add(frame + 1, anim.frames().remove(frame));
                frame++;
                return true;
            }
            if (over(mx, my, px + LX + 62, py + 112, 26, 14)) {
                NotchWidgets.click();
                anim.setLengthTicks(anim.lengthTicks()
                        - (net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 2 : 10));
                return true;
            }
            if (over(mx, my, px + LX + 92, py + 112, 26, 14)) {
                NotchWidgets.click();
                anim.setLengthTicks(anim.lengthTicks()
                        + (net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 2 : 10));
                return true;
            }
            if (over(mx, my, px + LX, py + 132, 56, 14)
                    && anim.frames().size() < NpcAnimation.MAX_FRAMES) {
                NotchWidgets.click();
                anim.frames().add(frame + 1, new NpcAnimation.Frame());
                frame++;
                return true;
            }
            if (over(mx, my, px + LX + 62, py + 132, 56, 14)
                    && anim.frames().size() < NpcAnimation.MAX_FRAMES) {
                NotchWidgets.click();
                anim.frames().add(frame + 1, current().copy());
                frame++;
                return true;
            }
            if (over(mx, my, px + LX, py + 150, LW, 14) && anim.frames().size() > 1) {
                NotchWidgets.click();
                anim.frames().remove(frame);
                if (frame >= anim.frames().size()) frame = anim.frames().size() - 1;
                return true;
            }
            if (over(mx, my, px + LX, py + 170, LW, 16)) {
                NotchWidgets.click();
                playing = !playing;
                playTicks = 0f;
                return true;
            }
            for (int i = 0; i < 6; i++) {
                if (over(mx, my, px + RX + (i % 2) * 58, py + 30 + (i / 2) * 18, 54, 16)) {
                    NotchWidgets.click();
                    part = i;
                    return true;
                }
            }
            if (over(mx, my, px + RX, py + 100, 54, 14)) {
                NotchWidgets.click();
                pivotMode = false;
                return true;
            }
            if (over(mx, my, px + RX + 58, py + 100, 54, 14)) {
                NotchWidgets.click();
                pivotMode = true;
                return true;
            }
            if (over(mx, my, px + RX, py + 180, RW, 14)) {
                NotchWidgets.click();
                for (NpcAnimation.Frame f : anim.frames()) {
                    for (int axis = 0; axis < 3; axis++) {
                        int rot = part * 3 + axis;
                        f.clear(rot);
                        f.clear(NpcAnimation.SLOTS + rot);
                        f.angles[rot] = 0;
                        f.offsets[rot] = 0;
                    }
                }
                return true;
            }
            int tx = px + TL_X, tw = W - TL_X * 2;
            if (over(mx, my, tx, py + H - 41, tw, 16) && !anim.frames().isEmpty()) {
                NotchWidgets.click();
                float pct = Math.max(0f, Math.min(0.999f, (mx - (tx + 2)) / (float) (tw - 4)));
                frame = (int) (pct * anim.frames().size());
                playing = false;
                return true;
            }
            if (over(mx, my, px + 60, py + H - 24, 110, 16)) {
                NotchWidgets.click();
                anim.setName(name);
                NotchPacketsClient.sendAnimSave(anim.toNbt());
                NotchPacketsClient.sendAnimDesign();
                return true;
            }
            if (over(mx, my, px + 190, py + H - 24, 110, 16)) {
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

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x();
    *///?} else {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
    //?}
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

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        dragging = -1;
        return super.mouseReleased(event);
    }
    *///?} else {
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    //?}

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
