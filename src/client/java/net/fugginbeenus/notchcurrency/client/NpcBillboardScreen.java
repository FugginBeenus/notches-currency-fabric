package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.UUID;

public class NpcBillboardScreen extends Screen {

    private static final int W = 300, H = 272;
    private static final int PAD = 14;
    private static final int ROW_H = 30;
    private final UUID npcId;
    private final String[] lines = new String[NotchNpcEntity.MAX_BILLBOARD_LINES];
    private final EditBox[] fields = new EditBox[NotchNpcEntity.MAX_BILLBOARD_LINES];
    private EditBox titleField;
    private String title;
    private final String voice;
    private final int voicePitch;
    private int px, py;

    public NpcBillboardScreen(UUID npcId, String existing, String title, String voice, int voicePitch) {
        super(Component.literal("Floating text"));
        this.npcId = npcId;
        this.title = title == null ? "" : title;
        this.voice = voice == null ? "" : voice;
        this.voicePitch = voicePitch;
        String[] typed = existing == null || existing.isBlank() ? new String[0] : existing.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = i < typed.length ? typed[i] : "";
        }
    }

    private int rowY(int i) { return py + 46 + i * ROW_H; }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        for (int i = 0; i < fields.length; i++) {
            final int idx = i;
            fields[i] = new EditBox(this.font, px + PAD + 4, rowY(i) + 4,
                    W - PAD * 2 - 8, 10, Component.literal("Line " + (i + 1)));
            fields[i].setMaxLength(NotchNpcEntity.MAX_BILLBOARD_LINE_LENGTH);
            fields[i].setBordered(false);
            fields[i].setValue(lines[i]);
            fields[i].setResponder(s -> lines[idx] = s);
            addRenderableWidget(fields[i]);
        }

        titleField = new EditBox(this.font, px + PAD + 4, titleRow() + 4,
                W - PAD * 2 - 8, 10, Component.literal("Title"));
        titleField.setMaxLength(NotchNpcEntity.MAX_SUBTITLE_LENGTH);
        titleField.setBordered(false);
        titleField.setValue(title);
        titleField.setResponder(s -> title = s);
        addRenderableWidget(titleField);

        setInitialFocus(fields[0]);
    }

    private int titleRow() { return py + 178; }

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
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Floating Text", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "Text that hovers above this NPC.",
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.centerText(ctx, this.font, "Top line first. Leave a row empty to skip it.",
                px + W / 2, py + 32, NotchTheme.TEXT_MUTED, false);

        for (int i = 0; i < fields.length; i++) {
            NotchWidgets.inset(ctx, px + PAD, rowY(i), W - PAD * 2, 15, NotchTheme.DEEP);
            String preview = NotchWidgets.colorize(lines[i]);
            if (!preview.isBlank()) {
                ctx.drawString(this.font, this.font.plainSubstrByWidth(preview, W - PAD * 2 - 8),
                        px + PAD + 4, rowY(i) + 17, 0xFFFFFFFF, true);
            }
        }

        NotchWidgets.divider(ctx, px + PAD, py + 166, W - PAD * 2);
        ctx.drawString(this.font, "Title, under the name", px + PAD, titleRow() - 10,
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + PAD, titleRow(), W - PAD * 2, 16, NotchTheme.DEEP);
        if (title.isEmpty()) {
            ctx.drawString(this.font, "Blacksmith", px + PAD + 4, titleRow() + 4, 0xFF555555, false);
        } else {
            ctx.drawString(this.font, NotchWidgets.colorize(title), px + PAD + 4, titleRow() + 20,
                    0xFFFFFFFF, true);
        }

        NotchWidgets.centerText(ctx, this.font, "&-colours, %player%, %npc% and %balance% all work.",
                px + W / 2, py + H - 46, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.primaryButton(ctx, this.font, px + PAD, py + H - 32, 150, 18, "Save & Back",
                over(mouseX, mouseY, px + PAD, py + H - 32, 150, 18));
        NotchWidgets.dangerButton(ctx, this.font, px + W - PAD - 110, py + H - 32, 110, 18, "Clear sign",
                over(mouseX, mouseY, px + W - PAD - 110, py + H - 32, 110, 18));
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
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
            if (over(mx, my, px + PAD, py + H - 32, 150, 18)) {
                NotchWidgets.click();
                save(String.join("\n", lines));
                return true;
            }
            if (over(mx, my, px + W - PAD - 110, py + H - 32, 110, 18)) {
                NotchWidgets.click();
                save("");
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void save(String text) {
        NotchPacketsClient.sendNpcBillboard(npcId, text);
        NotchPacketsClient.sendNpcFlavor(npcId, title, voice, voicePitch);
        NotchPacketsClient.sendNpcEditorReopen(npcId, 0); // back to Look, where the button lives
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, fields)) return true;
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, titleField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
