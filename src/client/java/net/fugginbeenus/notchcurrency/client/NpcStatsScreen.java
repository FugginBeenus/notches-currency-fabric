package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.UUID;

public class NpcStatsScreen extends Screen {

    private static final int W = 300, H = 228;
    private static final int SLIDER_X = 96, SLIDER_W = 130, SLIDER_H = 12;
    private static final String[] SLIDER_NAMES = {"Max Health", "Speed", "Regen"};
    private static final String[] TOGGLE_NAMES = {
            "Protected", "Silent", "No gravity", "Opens doors", "Leashable", "Pushable"};
    private static final int[] TOGGLE_BITS = {1, 2, 16, 32, 64, 1024};

    private final UUID npcId;
    private int statsBits;
    private int maxHealth, speedPct, regen;
    private int draggingSlider = -1;

    public NpcStatsScreen(UUID npcId, int statsBits, int maxHealth, int speedPct, int regen) {
        super(Component.literal("NPC Stats"));
        this.npcId = npcId;
        this.statsBits = statsBits;
        this.maxHealth = Math.max(2, Math.min(100, maxHealth));
        this.speedPct = Math.max(10, Math.min(60, speedPct));
        this.regen = Math.max(0, Math.min(10, regen));
    }

    private int px() { return (this.width - W) / 2; }
    private int py() { return (this.height - H) / 2; }

    private static final String[] VIS_NAMES = {"Always", "Day only", "Night only"};

    private int sliderY(int i) { return py() + 30 + i * 22; }
    private net.fugginbeenus.notchcurrency.entity.NotchNpcEntity findNpc() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return null;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc
                    && npc.getUUID().equals(npcId)) return npc;
        }
        return null;
    }

    private boolean bossBarOn() {
        net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc = findNpc();
        return npc != null && npc.showsBossBar();
    }

    private int toggleX(int i) { return px() + 14 + (i % 2) * 140; }
    private int toggleY(int i) { return py() + 106 + (i / 2) * 19; }
    private int visibility() { return (statsBits >> 8) & 3; }

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
        NotchWidgets.title(ctx, this.font, "Stats & Abilities", px + W / 2, py + 8);

        for (int i = 0; i < 3; i++) {
            int sy = sliderY(i);
            ctx.drawString(this.font, SLIDER_NAMES[i], px + 14, sy + 2, NotchTheme.TEXT_DARK, false);
            boolean hover = draggingSlider == i
                    || over(mouseX, mouseY, px + SLIDER_X, sy, SLIDER_W, SLIDER_H);
            NotchWidgets.slider(ctx, px + SLIDER_X, sy, SLIDER_W, SLIDER_H, sliderT(i), hover);
            ctx.drawString(this.font, sliderReadout(i), px + SLIDER_X + SLIDER_W + 8, sy + 2,
                    NotchTheme.TEXT_DARK, false);
        }

        NotchWidgets.divider(ctx, px + 8, py + 96, W - 16);

        for (int i = 0; i < TOGGLE_NAMES.length; i++) {
            boolean on = (statsBits & TOGGLE_BITS[i]) != 0;
            boolean hover = over(mouseX, mouseY, toggleX(i), toggleY(i), 132, 15);
            if (on) NotchWidgets.primaryButton(ctx, this.font, toggleX(i), toggleY(i), 132, 15, TOGGLE_NAMES[i], hover);
            else NotchWidgets.neutralButton(ctx, this.font, toggleX(i), toggleY(i), 132, 15, TOGGLE_NAMES[i], hover);
        }

        boolean boss = bossBarOn();
        boolean bossHover = over(mouseX, mouseY, px + 14, py + 163, 132, 15);
        if (boss) {
            NotchWidgets.goldButton(ctx, this.font, px + 14, py + 163, 132, 15, "Boss bar", bossHover);
        } else {
            NotchWidgets.neutralButton(ctx, this.font, px + 14, py + 163, 132, 15, "Boss bar", bossHover);
        }
        ctx.drawString(this.font, "Appears", px + 14, py + 188, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + SLIDER_X, py + 185, SLIDER_W, 15,
                VIS_NAMES[visibility() % 3], over(mouseX, mouseY, px + SLIDER_X, py + 185, SLIDER_W, 15));

        if (bossHover) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Boss bar").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("A health bar at the top of the screen").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("for players within 24 blocks.").withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + 70, py + 206, 160, 16, "Back to Editor",
                over(mouseX, mouseY, px + 70, py + 206, 160, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
    }

    private float sliderT(int i) {
        return switch (i) {
            case 0 -> (maxHealth - 2) / 98f;
            case 1 -> (speedPct - 10) / 50f;
            default -> regen / 10f;
        };
    }

    private String sliderReadout(int i) {
        return switch (i) {
            case 0 -> maxHealth + " HP";
            case 1 -> Math.round(speedPct * 100f / 30f) + "%";
            default -> regen == 0 ? "Off" : (regen / 2f) + " HP/5s";
        };
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

            if (over(mx, my, px() + 14, py() + 163, 132, 15)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcLooks(npcId, 4, bossBarOn() ? 0 : 1);
                return true;
            }
            int px = px(), py = py();
            for (int i = 0; i < 3; i++) {
                if (over(mx, my, px + SLIDER_X - 2, sliderY(i) - 2, SLIDER_W + 4, SLIDER_H + 4)) {
                    draggingSlider = i;
                    updateFromMouse(mouseX);
                    return true;
                }
            }
            for (int i = 0; i < TOGGLE_NAMES.length; i++) {
                if (over(mx, my, toggleX(i), toggleY(i), 132, 15)) {
                    NotchWidgets.tick();
                    statsBits ^= TOGGLE_BITS[i];
                    NotchPacketsClient.sendNpcSetStats(npcId, statsBits);
                    return true;
                }
            }
            if (over(mx, my, px + SLIDER_X, py + 185, SLIDER_W, 15)) {
                NotchWidgets.tick();
                int vis = (visibility() + 1) % 3;
                statsBits = (statsBits & ~(3 << 8)) | (vis << 8);
                NotchPacketsClient.sendNpcSetStats(npcId, statsBits);
                return true;
            }
            if (over(mx, my, px + 70, py + 206, 160, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 5);
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
        if (draggingSlider >= 0) {
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
        if (draggingSlider >= 0) {
            draggingSlider = -1;
            NotchPacketsClient.sendNpcSetAttrs(npcId, maxHealth, speedPct, regen);
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
        t = Math.max(0f, Math.min(1f, t));
        switch (draggingSlider) {
            case 0 -> maxHealth = 2 + 2 * Math.round(t * 49); // even steps 2..100
            case 1 -> speedPct = 10 + Math.round(t * 50);
            default -> regen = Math.round(t * 10);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

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
