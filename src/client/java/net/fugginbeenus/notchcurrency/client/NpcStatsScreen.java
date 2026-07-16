package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Stats & abilities editor: three attribute sliders (max health, walk speed, regeneration) plus the
 * toggle grid (protection, silence, glowing, doors, leash, invisibility...). EasyNPC spreads this
 * over four attribute screens; one panel covers the parts people actually use. Toggles apply
 * instantly; sliders apply on release.
 */
public class NpcStatsScreen extends Screen {

    private static final int W = 300, H = 266;
    private static final int SLIDER_X = 96, SLIDER_W = 130, SLIDER_H = 12;
    private static final String[] SLIDER_NAMES = {"Max Health", "Speed", "Regen"};
    private static final String[] TOGGLE_NAMES = {
            "Protected", "Silent", "Glowing", "Nameplate",
            "No gravity", "Opens doors", "Leashable", "Invisible", "Pushable",
            "Hostile: players", "Fights back"};
    // Explicit bit per toggle (bits 8-9 are reserved for the visibility rule, so Pushable jumps to 1024).
    private static final int[] TOGGLE_BITS = {1, 2, 4, 8, 16, 32, 64, 128, 1024, 2048, 4096};

    private final UUID npcId;
    private int statsBits;
    private int maxHealth, speedPct, regen;
    private int draggingSlider = -1;

    public NpcStatsScreen(UUID npcId, int statsBits, int maxHealth, int speedPct, int regen) {
        super(Text.literal("NPC Stats"));
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
    private int toggleX(int i) { return px() + 14 + (i % 2) * 140; }
    private int toggleY(int i) { return py() + 106 + (i / 2) * 19; }
    private int visibility() { return (statsBits >> 8) & 3; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderInGameBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        int px = px(), py = py();
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Stats & Abilities", px + W / 2, py + 8);

        for (int i = 0; i < 3; i++) {
            int sy = sliderY(i);
            ctx.drawText(this.textRenderer, SLIDER_NAMES[i], px + 14, sy + 2, NotchTheme.TEXT_DARK, false);
            boolean hover = draggingSlider == i
                    || over(mouseX, mouseY, px + SLIDER_X, sy, SLIDER_W, SLIDER_H);
            NotchWidgets.slider(ctx, px + SLIDER_X, sy, SLIDER_W, SLIDER_H, sliderT(i), hover);
            ctx.drawText(this.textRenderer, sliderReadout(i), px + SLIDER_X + SLIDER_W + 8, sy + 2,
                    NotchTheme.TEXT_DARK, false);
        }

        NotchWidgets.divider(ctx, px + 8, py + 96, W - 16);

        for (int i = 0; i < TOGGLE_NAMES.length; i++) {
            boolean on = (statsBits & TOGGLE_BITS[i]) != 0;
            boolean hover = over(mouseX, mouseY, toggleX(i), toggleY(i), 132, 15);
            if (on) NotchWidgets.primaryButton(ctx, this.textRenderer, toggleX(i), toggleY(i), 132, 15, TOGGLE_NAMES[i], hover);
            else NotchWidgets.neutralButton(ctx, this.textRenderer, toggleX(i), toggleY(i), 132, 15, TOGGLE_NAMES[i], hover);
        }

        // Day/night rule: while off-schedule the NPC is invisible and won't respond to clicks.
        ctx.drawText(this.textRenderer, "Appears", px + 14, py + 226, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + SLIDER_X, py + 223, SLIDER_W, 15,
                VIS_NAMES[visibility() % 3], over(mouseX, mouseY, px + SLIDER_X, py + 223, SLIDER_W, 15));

        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 70, py + 244, 160, 16, "Back to Editor",
                over(mouseX, mouseY, px + 70, py + 244, 160, 16));

        super.render(ctx, mouseX, mouseY, delta);
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
            case 1 -> Math.round(speedPct * 100f / 30f) + "%"; // relative to the default walk speed
            default -> regen == 0 ? "Off" : (regen / 2f) + " HP/5s";
        };
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
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
            if (over(mx, my, px + SLIDER_X, py + 223, SLIDER_W, 15)) {
                NotchWidgets.tick();
                int vis = (visibility() + 1) % 3;
                statsBits = (statsBits & ~(3 << 8)) | (vis << 8);
                NotchPacketsClient.sendNpcSetStats(npcId, statsBits);
                return true;
            }
            if (over(mx, my, px + 70, py + 244, 160, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 5); // return to the NPC editor
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSlider >= 0) {
            updateFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSlider >= 0) {
            draggingSlider = -1;
            NotchPacketsClient.sendNpcSetAttrs(npcId, maxHealth, speedPct, regen);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
    public boolean shouldPause() {
        return false;
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens — they draw crisp panels over the world.
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render() — this screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
