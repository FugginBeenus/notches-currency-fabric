package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

/**
 * Move & Rotate: a compact panel docked at the bottom of the screen, so the world stays fully visible
 * so you watch the actual NPC move live while you nudge it. X/Y/Z nudge buttons (0.1 blocks, Shift =
 * 1), a yaw slider with Face Me, snap-to-block-center, and bring-to-me. Every change applies
 * instantly through the same owner-guarded path as the pose editor.
 */
public class NpcMoveScreen extends Screen {

    private static final int W = 292, H = 126;
    private static final int SLIDER_X = 40, SLIDER_W = 130, SLIDER_H = 12;

    private final UUID npcId;
    private NotchNpcEntity cached;
    private int yaw;
    private boolean draggingYaw;
    private int lastSentYaw;
    // The panel is draggable (grab the title bar) so it never has to sit over the NPC.
    private int panelX = -1, panelY = -1;
    private boolean draggingPanel;
    private int grabDx, grabDy;

    public NpcMoveScreen(UUID npcId) {
        super(Text.literal("Move & Rotate"));
        this.npcId = npcId;
    }

    @Override
    protected void init() {
        NotchNpcEntity npc = findNpc();
        if (npc != null) {
            yaw = Math.round(MathHelper.wrapDegrees(npc.getYaw()));
            lastSentYaw = yaw;
        }
        // Default to the bottom-left corner, out of the NPC's way; keep the spot across resizes.
        if (panelX < 0) {
            panelX = 8;
            panelY = this.height - H - 8;
        }
        panelX = MathHelper.clamp(panelX, 0, Math.max(0, this.width - W));
        panelY = MathHelper.clamp(panelY, 0, Math.max(0, this.height - H));
    }

    private int px() { return panelX; }
    private int py() { return panelY; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // No dimmed background: the point is watching the NPC in the world while adjusting.
        int px = px(), py = py();
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Move, Rotate & Size", px + W / 2, py + 6);
        // Grip lines at both title corners: the title bar is the drag handle.
        for (int g = 0; g < 3; g++) {
            ctx.fill(px + 6, py + 6 + g * 3, px + 26, py + 7 + g * 3, NotchTheme.PANEL_MID);
            ctx.fill(px + W - 26, py + 6 + g * 3, px + W - 6, py + 7 + g * 3, NotchTheme.PANEL_MID);
        }

        // Yaw row: slider + Face Me.
        int sy = py + 20;
        ctx.drawText(this.textRenderer, "Yaw", px + 10, sy + 2, NotchTheme.TEXT_DARK, false);
        boolean hoverYaw = draggingYaw || over(mouseX, mouseY, px + SLIDER_X, sy, SLIDER_W, SLIDER_H);
        NotchWidgets.slider(ctx, px + SLIDER_X, sy, SLIDER_W, SLIDER_H, (yaw + 180) / 360f, hoverYaw);
        ctx.drawText(this.textRenderer, yaw + "°", px + SLIDER_X + SLIDER_W + 5, sy + 2, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 226, sy - 1, 56, 14, "Face Me",
                over(mouseX, mouseY, px + 226, sy - 1, 56, 14));

        // Nudge row: X/Y/Z minus-plus pairs, then snaps.
        int ny = py + 40;
        String[] axes = {"X", "Y", "Z"};
        for (int a = 0; a < 3; a++) {
            int gx = px + 10 + a * 56;
            ctx.drawText(this.textRenderer, axes[a], gx, ny + 3, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, gx + 10, ny, 16, 14, "-",
                    over(mouseX, mouseY, gx + 10, ny, 16, 14));
            NotchWidgets.neutralButton(ctx, this.textRenderer, gx + 28, ny, 16, 14, "+",
                    over(mouseX, mouseY, gx + 28, ny, 16, 14));
        }
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 178, ny, 50, 14, "Center",
                over(mouseX, mouseY, px + 178, ny, 50, 14));
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 232, ny, 50, 14, "To Me",
                over(mouseX, mouseY, px + 232, ny, 50, 14));

        // Size row: one group per axis. Sizing lives here rather than on a tab because the whole point
        // is watching the NPC change out in the world while you do it.
        int zy = py + 60;
        NotchNpcEntity npc = findNpc();
        float[] sizes = npc == null ? new float[]{1f, 1f, 1f}
                : new float[]{npc.getScale(), npc.getScaleY(), npc.getScaleZ()};
        String[] dims = {"W", "H", "D"};
        for (int a = 0; a < 3; a++) {
            int gx = px + 8 + a * 92;
            ctx.drawText(this.textRenderer, dims[a], gx, zy + 3, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, gx + 12, zy, 14, 14, "-",
                    over(mouseX, mouseY, gx + 12, zy, 14, 14));
            NotchWidgets.centerText(ctx, this.textRenderer, String.format("%.1fx", sizes[a]),
                    gx + 46, zy + 3, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, gx + 62, zy, 14, 14, "+",
                    over(mouseX, mouseY, gx + 62, zy, 14, 14));
        }

        // Bottom row: hint + back.
        ctx.drawText(this.textRenderer, "Nudge 0.1 - hold Shift for 1 block.", px + 10, py + 84,
                NotchTheme.TEXT_MUTED, false);
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 10, py + 100, 272, 16, "Back to Editor",
                over(mouseX, mouseY, px + 10, py + 100, 272, 16));

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            int px = px(), py = py();
            int sy = py + 20;
            if (over(mx, my, px + SLIDER_X - 2, sy - 2, SLIDER_W + 4, SLIDER_H + 4)) {
                draggingYaw = true;
                updateYawFromMouse(mouseX);
                return true;
            }
            if (over(mx, my, px + 226, sy - 1, 56, 14)) {
                NotchWidgets.tick();
                faceMe();
                return true;
            }
            int ny = py + 40;
            double step = hasShiftDown() ? 1.0 : 0.1;
            for (int a = 0; a < 3; a++) {
                int gx = px + 10 + a * 56;
                if (over(mx, my, gx + 10, ny, 16, 14)) { NotchWidgets.tick(); nudge(a, -step); return true; }
                if (over(mx, my, gx + 28, ny, 16, 14)) { NotchWidgets.tick(); nudge(a, step); return true; }
            }
            if (over(mx, my, px + 178, ny, 50, 14)) {
                NotchWidgets.tick();
                centerOnBlock();
                return true;
            }
            if (over(mx, my, px + 232, ny, 50, 14)) {
                NotchWidgets.tick();
                bringToMe();
                return true;
            }
            int zy = py + 60;
            for (int a = 0; a < 3; a++) {
                int gx = px + 8 + a * 92;
                if (over(mx, my, gx + 12, zy, 14, 14)) { NotchWidgets.tick(); resize(a, -0.1f); return true; }
                if (over(mx, my, gx + 62, zy, 14, 14)) { NotchWidgets.tick(); resize(a, 0.1f); return true; }
            }
            if (over(mx, my, px + 10, py + 100, 272, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 4);
                return true;
            }
            // Grab the title bar to drag the whole panel out of the way.
            if (over(mx, my, px, py, W, 17)) {
                draggingPanel = true;
                grabDx = mx - px;
                grabDy = my - py;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingPanel) {
            panelX = MathHelper.clamp((int) mouseX - grabDx, 0, Math.max(0, this.width - W));
            panelY = MathHelper.clamp((int) mouseY - grabDy, 0, Math.max(0, this.height - H));
            return true;
        }
        if (draggingYaw) {
            updateYawFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPanel = false;
        if (draggingYaw) {
            draggingYaw = false;
            sendYaw();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateYawFromMouse(double mouseX) {
        float t = (float) ((mouseX - (px() + SLIDER_X + 2)) / (SLIDER_W - 10));
        int deg = Math.round(-180 + Math.max(0f, Math.min(1f, t)) * 360);
        if (Math.abs(deg) <= 3) deg = 0; // snap to neutral near the center tick
        if (yaw != deg) {
            yaw = deg;
            // Throttle live updates like the pose editor's sliders.
            if (Math.abs(deg - lastSentYaw) >= 5) {
                lastSentYaw = deg;
                sendYaw();
            }
        }
    }

    private void sendYaw() {
        NotchPacketsClient.sendNpcTransform(npcId, 0, 0, 0, yaw, true);
    }

    private void nudge(int axis, double step) {
        NotchPacketsClient.sendNpcTransform(npcId,
                axis == 0 ? step : 0, axis == 1 ? step : 0, axis == 2 ? step : 0, 0, false);
    }

    /** Resize one axis. The rest of the appearance is read back off the NPC so this panel doesn't
     *  need to know anything about models or skins to leave them alone. */
    private void resize(int axis, float step) {
        NotchNpcEntity npc = findNpc();
        if (npc == null) return;
        float x = npc.getScale(), y = npc.getScaleY(), z = npc.getScaleZ();
        float updated = Math.max(0.3f, Math.min(3.0f, Math.round(((axis == 0 ? x : axis == 1 ? y : z) + step) * 10f) / 10f));
        if (axis == 0) x = updated; else if (axis == 1) y = updated; else z = updated;
        NotchPacketsClient.sendNpcSetAppearance(npcId, npc.getModelId(), npc.getSkinType(),
                npc.getSkinValue(), npc.isSlim(), x, y, z, npc.getNameOffset());
    }

    /** Rotate the NPC to look at the player. */
    private void faceMe() {
        NotchNpcEntity npc = findNpc();
        MinecraftClient c = MinecraftClient.getInstance();
        if (npc == null || c.player == null) return;
        double dx = c.player.getX() - npc.getX();
        double dz = c.player.getZ() - npc.getZ();
        yaw = Math.round(MathHelper.wrapDegrees((float) (Math.toDegrees(MathHelper.atan2(dz, dx)) - 90.0)));
        lastSentYaw = yaw;
        sendYaw();
    }

    /** Snap X/Z to the centre of the block the NPC stands in. */
    private void centerOnBlock() {
        NotchNpcEntity npc = findNpc();
        if (npc == null) return;
        double dx = Math.floor(npc.getX()) + 0.5 - npc.getX();
        double dz = Math.floor(npc.getZ()) + 0.5 - npc.getZ();
        NotchPacketsClient.sendNpcTransform(npcId, dx, 0, dz, 0, false);
    }

    /** Teleport the NPC to where the player is standing. */
    private void bringToMe() {
        NotchNpcEntity npc = findNpc();
        MinecraftClient c = MinecraftClient.getInstance();
        if (npc == null || c.player == null) return;
        NotchPacketsClient.sendNpcTransform(npcId,
                c.player.getX() - npc.getX(), c.player.getY() - npc.getY(), c.player.getZ() - npc.getZ(),
                0, false);
    }

    private NotchNpcEntity findNpc() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null) return null;
        if (cached != null && !cached.isRemoved()) return cached;
        for (Entity e : c.world.getEntities()) {
            if (e instanceof NotchNpcEntity n && n.getUuid().equals(npcId)) {
                cached = n;
                return n;
            }
        }
        return null;
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean shouldPause() {
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
    public void renderBackground(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render(). This screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
