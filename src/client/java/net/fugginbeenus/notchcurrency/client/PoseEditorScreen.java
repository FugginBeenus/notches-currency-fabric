package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import java.util.UUID;

public class PoseEditorScreen extends Screen {

    private static final int W = 340, H = 226;
    private static final int PREV_X = 8, PREV_Y = 26, PREV_W = 96, PREV_H = 170;
    private static final int RX = 114; // right column start
    private static final int PART_W = 106, PART_H = 16;
    private static final int SLIDER_X = 150, SLIDER_W = 130, SLIDER_H = 12;
    private static final String[] PART_NAMES = {"Head", "Body", "Right Arm", "Left Arm", "Right Leg", "Left Leg"};
    private static final String[] AXIS_NAMES = {"Pitch", "Yaw", "Roll"};

    private final UUID npcId;
    private final int[] angles = new int[18]; // 6 parts x pitch/yaw/roll, degrees
    private int selectedPart = 0;
    private int draggingAxis = -1;
    private final int[] lastSent = new int[3];
    private NotchNpcEntity preview;

    public PoseEditorScreen(UUID npcId) {
        super(Component.literal("Pose Editor"));
        this.npcId = npcId;
    }

    @Override
    protected void init() {
        // Seed from the NPC's synced custom pose (the tracker already has it client-side).
        NotchNpcEntity npc = findNpc();
        if (npc != null && npc.getCustomPoseAngles() != null) {
            float[] current = npc.getCustomPoseAngles();
            for (int i = 0; i < 18; i++) {
                angles[i] = Math.round(current[i]);
            }
        }
    }

    private int px() { return (this.width - W) / 2; }
    private int py() { return (this.height - H) / 2; }

    private int partX(int i) { return px() + RX + (i % 2) * (PART_W + 6); }
    private int partY(int i) { return py() + 30 + (i / 2) * 18; }
    private int sliderY(int axis) { return py() + 96 + axis * 22; }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        //? if >=1.21 {
        /*renderTransparentBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        int px = px(), py = py();
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Pose Editor", px + W / 2, py + 8);

        // Live preview of the actual NPC.
        NotchWidgets.inset(ctx, px + PREV_X, py + PREV_Y, PREV_W, PREV_H, NotchTheme.DEEP);
        NotchNpcEntity npc = findNpc();
        if (npc != null) {
            float oldYaw = npc.getYRot(), oldBody = npc.yBodyRot;
            boolean wasInvisible = npc.isInvisible();
            npc.setYRot(180);
            npc.yBodyRot = 180;
            npc.setInvisible(false); // always show the NPC in its own editor preview
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx, px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H - 16, 46,
                    (px + PREV_X + PREV_W / 2f) - mouseX, (py + PREV_Y + 40f) - mouseY, npc);
            npc.setYRot(oldYaw);
            npc.yBodyRot = oldBody;
            npc.setInvisible(wasInvisible);
        }

        // Part picker.
        for (int i = 0; i < PART_NAMES.length; i++) {
            boolean hover = over(mouseX, mouseY, partX(i), partY(i), PART_W, PART_H);
            if (i == selectedPart) {
                NotchWidgets.primaryButton(ctx, this.font, partX(i), partY(i), PART_W, PART_H, PART_NAMES[i], hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, partX(i), partY(i), PART_W, PART_H, PART_NAMES[i], hover);
            }
        }

        // Sliders for the selected part.
        for (int axis = 0; axis < 3; axis++) {
            int sy = sliderY(axis);
            int deg = angles[selectedPart * 3 + axis];
            ctx.drawString(this.font, AXIS_NAMES[axis], px + RX, sy + 2, NotchTheme.TEXT_DARK, false);
            boolean hover = draggingAxis == axis
                    || over(mouseX, mouseY, px + SLIDER_X, sy, SLIDER_W, SLIDER_H);
            NotchWidgets.slider(ctx, px + SLIDER_X, sy, SLIDER_W, SLIDER_H, (deg + 180) / 360f, hover);
            ctx.drawString(this.font, deg + "°", px + SLIDER_X + SLIDER_W + 6, sy + 2, NotchTheme.TEXT_DARK, false);
        }

        NotchWidgets.neutralButton(ctx, this.font, px + RX, py + 166, 100, 15, "Reset Part",
                over(mouseX, mouseY, px + RX, py + 166, 100, 15));
        NotchWidgets.dangerButton(ctx, this.font, px + RX + 108, py + 166, 100, 15, "Reset All",
                over(mouseX, mouseY, px + RX + 108, py + 166, 100, 15));

        NotchWidgets.centerText(ctx, this.font, "Changes apply to the NPC instantly.",
                px + RX + (W - RX) / 2 - 4, py + 187, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.primaryButton(ctx, this.font, px + RX, py + 198, 208, 16, "Back to Editor",
                over(mouseX, mouseY, px + RX, py + 198, 208, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
    }

    // ---- input ----

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
            int px = px(), py = py();
            for (int i = 0; i < PART_NAMES.length; i++) {
                if (over(mx, my, partX(i), partY(i), PART_W, PART_H)) {
                    if (selectedPart != i) NotchWidgets.tick();
                    selectedPart = i;
                    return true;
                }
            }
            for (int axis = 0; axis < 3; axis++) {
                if (over(mx, my, px + SLIDER_X - 2, sliderY(axis) - 2, SLIDER_W + 4, SLIDER_H + 4)) {
                    draggingAxis = axis;
                    System.arraycopy(angles, selectedPart * 3, lastSent, 0, 3);
                    updateFromMouse(mouseX);
                    return true;
                }
            }
            if (over(mx, my, px + RX, py + 166, 100, 15)) {
                NotchWidgets.click();
                setPartAngles(0, 0, 0);
                sendPart();
                return true;
            }
            if (over(mx, my, px + RX + 108, py + 166, 100, 15)) {
                NotchWidgets.click();
                for (int i = 0; i < 18; i++) angles[i] = 0;
                NotchPacketsClient.sendNpcPosePart(npcId, -1, 0, 0, 0);
                return true;
            }
            if (over(mx, my, px + RX, py + 198, 208, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 4); // return to the NPC editor
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
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    //?}
        if (draggingAxis >= 0) {
            updateFromMouse(mouseX);
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseDragged(event, deltaX, deltaY);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //?}
        if (draggingAxis >= 0) {
            draggingAxis = -1;
            sendPart();
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseReleased(event);
        *///?} else {
        return super.mouseReleased(mouseX, mouseY, button);
        //?}
    }

    private void updateFromMouse(double mouseX) {
        float t = (float) ((mouseX - (px() + SLIDER_X + 2)) / (SLIDER_W - 10));
        int deg = Math.round(-180 + Math.max(0f, Math.min(1f, t)) * 360);
        if (Math.abs(deg) <= 3) deg = 0; // snap to neutral near the center tick
        int idx = selectedPart * 3 + draggingAxis;
        if (angles[idx] != deg) {
            angles[idx] = deg;
            // Throttle live updates: only ship when the value moved noticeably since the last send.
            if (Math.abs(deg - lastSent[draggingAxis]) >= 5) {
                lastSent[draggingAxis] = deg;
                sendPart();
            }
        }
    }

    private void setPartAngles(int x, int y, int z) {
        angles[selectedPart * 3] = x;
        angles[selectedPart * 3 + 1] = y;
        angles[selectedPart * 3 + 2] = z;
    }

    private void sendPart() {
        NotchPacketsClient.sendNpcPosePart(npcId, selectedPart,
                angles[selectedPart * 3], angles[selectedPart * 3 + 1], angles[selectedPart * 3 + 2]);
    }

    private NotchNpcEntity findNpc() {
        Minecraft c = Minecraft.getInstance();
        if (c.level == null) return null;
        if (preview != null && !preview.isRemoved()) return preview;
        for (Entity e : c.level.entitiesForRendering()) {
            if (e instanceof NotchNpcEntity n && n.getUUID().equals(npcId)) {
                preview = n;
                return n;
            }
        }
        return null;
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // The blur hook is handed the graphics now instead of the partial tick.
    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
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
