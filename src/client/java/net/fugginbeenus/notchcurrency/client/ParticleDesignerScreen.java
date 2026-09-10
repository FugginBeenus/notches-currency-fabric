package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.particle.ParticleLibrary;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.particle.ParticleEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ParticleDesignerScreen extends Screen {

    private static final int W = 320, H = 246;
    private static final int ROW_H = 26, ROWS = 5;

    public static java.util.UUID cameFromNpc = null;

    private int px, py, scroll;
    private EditBox newNameField;
    private java.util.List<Component> tooltipLines;

    public ParticleDesignerScreen() {
        super(Component.literal("Particle Effects"));
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        newNameField = new EditBox(this.font, px + 13, py + H - 45, 170, 10, Component.empty());
        newNameField.setMaxLength(ParticleEffect.MAX_NAME);
        newNameField.setBordered(false);
        newNameField.setHint(Component.literal("new effect name").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(newNameField);
    }

    private net.fugginbeenus.notchcurrency.entity.NotchNpcEntity findNpc() {
        if (cameFromNpc == null) return null;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return null;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc
                    && npc.getUUID().equals(cameFromNpc)) return npc;
        }
        return null;
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String summaryOf(ParticleEffect e) {
        if (e.isEmpty()) return "empty";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < e.layerCount(); i++) {
            if (i > 0) sb.append(" + ");
            String p = e.layer(i).particle();
            sb.append(p.startsWith("minecraft:") ? p.substring(10) : p);
        }
        return sb.toString();
    }

    private String fit(String text, int room) {
        if (this.font.width(text) <= room) return text;
        return this.font.plainSubstrByWidth(text, room - this.font.width("...")) + "...";
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Particle Effects", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "Build a look here, then give it to any NPC.",
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.divider(ctx, px + 8, py + 34, W - 16);

        List<ParticleEffect> list = ParticleLibrary.all();
        if (list.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No effects yet. Name one below.",
                    px + W / 2, py + 80, NotchTheme.TEXT_MUTED, false);
        }
        int shown = Math.min(ROWS, Math.max(0, list.size() - scroll));
        for (int i = 0; i < shown; i++) {
            ParticleEffect e = list.get(i + scroll);
            int ry = py + 42 + i * ROW_H;
            boolean hover = over(mouseX, mouseY, px + 12, ry, W - 46, 22);
            NotchWidgets.neutralButton(ctx, this.font, px + 12, ry, W - 46, 22, "", hover);
            ctx.drawString(this.font, fit(e.name(), W - 62), px + 18, ry + 3,
                    NotchTheme.TEXT_DARK, false);
            ctx.drawString(this.font, fit(summaryOf(e), W - 62), px + 18, ry + 13,
                    NotchTheme.TEXT_MUTED, false);
            NotchWidgets.dangerButton(ctx, this.font, px + W - 32, ry + 3, 20, 16, "x",
                    over(mouseX, mouseY, px + W - 32, ry + 3, 20, 16));
        }

        if (list.size() > ROWS) {
            int sy = py + 42 + ROWS * ROW_H;
            NotchWidgets.neutralButton(ctx, this.font, px + 12, sy, 20, 12, "v",
                    over(mouseX, mouseY, px + 12, sy, 20, 12));
            NotchWidgets.neutralButton(ctx, this.font, px + 36, sy, 20, 12, "^",
                    over(mouseX, mouseY, px + 36, sy, 20, 12));
        }

        NotchWidgets.divider(ctx, px + 8, py + H - 54, W - 16);
        NotchWidgets.inset(ctx, px + 10, py + H - 49, 176, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.font, px + 192, py + H - 49, 116, 14, "+ New effect",
                over(mouseX, mouseY, px + 192, py + H - 49, 116, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + W - 84, py + 42 + ROWS * ROW_H, 72, 12, "Textures...",
                over(mouseX, mouseY, px + W - 84, py + 42 + ROWS * ROW_H, 72, 12));
        var worn = findNpc();
        if (worn != null) {
            String on = worn.getParticleFx().isBlank() ? "None" : worn.getParticleFx();
            ctx.drawString(this.font, "This NPC wears:", px + 12, py + H - 22,
                    NotchTheme.TEXT_DARK, false);
            boolean wornHover = over(mouseX, mouseY, px + 96, py + H - 26, 110, 14);
            NotchWidgets.neutralButton(ctx, this.font, px + 96, py + H - 26, 110, 14,
                    fit(on, 100), wornHover);
            if (wornHover) {
                tooltipLines = java.util.List.of(
                        Component.literal("Standing effect").withStyle(ChatFormatting.WHITE),
                        Component.literal("Runs all the time on this NPC.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Click to cycle through your effects.").withStyle(ChatFormatting.DARK_GRAY));
            }
            NotchWidgets.neutralButton(ctx, this.font, px + W - 62, py + H - 26, 50, 14, "Back",
                    over(mouseX, mouseY, px + W - 62, py + H - 26, 50, 14));
        } else {
            NotchWidgets.neutralButton(ctx, this.font, px + W / 2 - 50, py + H - 26, 100, 16, "Back",
                    over(mouseX, mouseY, px + W / 2 - 50, py + H - 26, 100, 16));
        }

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        if (tooltipLines != null) {
            ctx.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
            tooltipLines = null;
        }
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
            List<ParticleEffect> list = ParticleLibrary.all();
            int shown = Math.min(ROWS, Math.max(0, list.size() - scroll));
            for (int i = 0; i < shown; i++) {
                int ry = py + 42 + i * ROW_H;
                ParticleEffect e = list.get(i + scroll);
                if (over(mx, my, px + W - 32, ry + 3, 20, 16)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendParticleDelete(e.name());
                    return true;
                }
                if (over(mx, my, px + 12, ry, W - 46, 22)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendParticleOpen(e.name());
                    return true;
                }
            }
            if (list.size() > ROWS) {
                int sy = py + 42 + ROWS * ROW_H;
                if (over(mx, my, px + 12, sy, 20, 12)) {
                    NotchWidgets.click();
                    scroll = Math.min(Math.max(0, list.size() - ROWS), scroll + 1);
                    return true;
                }
                if (over(mx, my, px + 36, sy, 20, 12)) {
                    NotchWidgets.click();
                    scroll = Math.max(0, scroll - 1);
                    return true;
                }
            }
            if (over(mx, my, px + W - 84, py + 42 + ROWS * ROW_H, 72, 12)) {
                NotchWidgets.click();
                final ParticleDesignerScreen self = this;
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new net.fugginbeenus.notchcurrency.client.npctexture.NpcTextureManageScreen(
                                () -> net.minecraft.client.Minecraft.getInstance().setScreen(self)));
                return true;
            }
            if (over(mx, my, px + 192, py + H - 49, 116, 14)) {
                String name = newNameField.getValue().trim();
                if (!name.isBlank()) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendParticleOpen(name);
                }
                return true;
            }
            var worn = findNpc();
            if (worn != null && over(mx, my, px + 96, py + H - 26, 110, 14)) {
                NotchWidgets.click();
                String next = net.fugginbeenus.notchcurrency.client.particle.ParticleLibrary
                        .next(worn.getParticleFx());
                worn.setParticleFx(next);
                NotchPacketsClient.sendNpcSetFx(cameFromNpc, next);
                return true;
            }
            boolean backHit = worn != null
                    ? over(mx, my, px + W - 62, py + H - 26, 50, 14)
                    : over(mx, my, px + W / 2 - 50, py + H - 26, 100, 16);
            if (backHit) {
                NotchWidgets.click();
                if (cameFromNpc != null) {
                    java.util.UUID back = cameFromNpc;
                    cameFromNpc = null;
                    NotchPacketsClient.sendNpcEditorReopen(back, 5);
                } else {
                    this.onClose();
                }
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
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
