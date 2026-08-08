package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.NpcPresetManager;
import net.fugginbeenus.notchcurrency.npc.NpcShareCodec;
import net.fugginbeenus.notchcurrency.npc.NpcShareManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.UUID;

public class NpcPresetScreen extends Screen {

    private static final int W = 300, H = 252;

    // Share row. Presets stay on this server; these four move an NPC off it entirely.
    private static final int SHARE_Y = 204, SHARE_H = 15;
    private static final int COPY_X = 16, COPY_W = 78;
    private static final int PASTE_X = 98, PASTE_W = 78;
    private static final int TOFILE_X = 180, TOFILE_W = 50;
    private static final int FROMFILE_X = 234, FROMFILE_W = 50;
    private static final int BACK_Y = 226;
    private static final int LIST_X = 14, LIST_Y = 22, LIST_W = 272, LIST_H = 116;
    private static final int ROW_H = 16, VISIBLE_ROWS = 7;

    private final UUID npcId;
    private List<String> presets;
    private int selected = -1;
    private int scroll = 0;
    private EditBox nameField;

    public NpcPresetScreen(UUID npcId, List<String> presets) {
        super(Component.literal("NPC Presets"));
        this.npcId = npcId;
        this.presets = presets;
    }

    public boolean isFor(UUID id) {
        return npcId.equals(id);
    }

    public void setPresets(List<String> names) {
        this.presets = names;
        if (selected >= names.size()) selected = -1;
        int maxScroll = Math.max(0, names.size() - VISIBLE_ROWS);
        if (scroll > maxScroll) scroll = maxScroll;
    }

    private int px() { return (this.width - W) / 2; }
    private int py() { return (this.height - H) / 2; }

    private int rowY(int visIdx) { return py() + LIST_Y + 2 + visIdx * ROW_H; }

    @Override
    protected void init() {
        String old = nameField == null ? "" : nameField.getValue();
        nameField = new EditBox(this.font, px() + 66, py() + 173, 142, 9, Component.literal("Preset name"));
        nameField.setMaxLength(32);
        nameField.setBordered(false);
        nameField.setHint(Component.literal("preset name").withStyle(ChatFormatting.DARK_GRAY));
        nameField.setValue(old);
        addRenderableWidget(nameField);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderTransparentBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        int px = px(), py = py();
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "NPC Presets", px + W / 2, py + 8);

        NotchWidgets.inset(ctx, px + LIST_X, py + LIST_Y, LIST_W, LIST_H, NotchTheme.DEEP);
        if (presets.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No presets saved yet.",
                    px + W / 2, py + LIST_Y + LIST_H / 2 - 4, NotchTheme.TEXT_MUTED, false);
        }
        for (int v = 0; v < VISIBLE_ROWS; v++) {
            int i = scroll + v;
            if (i >= presets.size()) break;
            boolean hover = over(mouseX, mouseY, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 1);
            if (i == selected) {
                NotchWidgets.primaryButton(ctx, this.font, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 1, presets.get(i), hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 1, presets.get(i), hover);
            }
        }
        if (presets.size() > VISIBLE_ROWS) {
            NotchWidgets.centerText(ctx, this.font, (scroll + 1) + "-" + Math.min(scroll + VISIBLE_ROWS, presets.size())
                    + " of " + presets.size() + " (scroll)", px + W / 2, py + LIST_Y + LIST_H + 3, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + 16, py + 146, 170, 15, "Load onto this NPC",
                over(mouseX, mouseY, px + 16, py + 146, 170, 15));
        NotchWidgets.dangerButton(ctx, this.font, px + 192, py + 146, 92, 15, "Delete",
                over(mouseX, mouseY, px + 192, py + 146, 92, 15));

        NotchWidgets.divider(ctx, px + 8, py + 165, W - 16);
        ctx.drawString(this.font, "Save as:", px + 16, py + 173, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + 62, py + 170, 150, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.font, px + 218, py + 170, 66, 14, "Save",
                over(mouseX, mouseY, px + 218, py + 170, 66, 14));

        NotchWidgets.divider(ctx, px + 8, py + 189, W - 16);
        ctx.drawString(this.font, "Share across worlds (file uses the name above)",
                px + 16, py + 194, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.primaryButton(ctx, this.font, px + COPY_X, py + SHARE_Y, COPY_W, SHARE_H, "Copy code",
                over(mouseX, mouseY, px + COPY_X, py + SHARE_Y, COPY_W, SHARE_H));
        NotchWidgets.primaryButton(ctx, this.font, px + PASTE_X, py + SHARE_Y, PASTE_W, SHARE_H, "Paste code",
                over(mouseX, mouseY, px + PASTE_X, py + SHARE_Y, PASTE_W, SHARE_H));
        NotchWidgets.neutralButton(ctx, this.font, px + TOFILE_X, py + SHARE_Y, TOFILE_W, SHARE_H, "To file",
                over(mouseX, mouseY, px + TOFILE_X, py + SHARE_Y, TOFILE_W, SHARE_H));
        NotchWidgets.neutralButton(ctx, this.font, px + FROMFILE_X, py + SHARE_Y, FROMFILE_W, SHARE_H, "From file",
                over(mouseX, mouseY, px + FROMFILE_X, py + SHARE_Y, FROMFILE_W, SHARE_H));

        NotchWidgets.primaryButton(ctx, this.font, px + 70, py + BACK_Y, 160, 16, "Back to Editor",
                over(mouseX, mouseY, px + 70, py + BACK_Y, 160, 16));

        super.render(ctx, mouseX, mouseY, delta);
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
            int px = px(), py = py();
            for (int v = 0; v < VISIBLE_ROWS; v++) {
                int i = scroll + v;
                if (i >= presets.size()) break;
                if (over(mx, my, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 1)) {
                    NotchWidgets.tick();
                    selected = (selected == i) ? -1 : i;
                    return true;
                }
            }
            if (over(mx, my, px + 16, py + 146, 170, 15) && selected >= 0) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcPreset(npcId, NpcPresetManager.ACTION_LOAD, presets.get(selected));
                NotchPacketsClient.sendNpcEditorReopen(npcId, 5); // back to the editor, preview updated
                return true;
            }
            if (over(mx, my, px + 192, py + 146, 92, 15) && selected >= 0) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcPreset(npcId, NpcPresetManager.ACTION_DELETE, presets.get(selected));
                selected = -1;
                return true;
            }
            if (over(mx, my, px + 218, py + 170, 66, 14)) {
                String name = nameField.getValue().trim();
                if (!name.isEmpty()) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendNpcPreset(npcId, NpcPresetManager.ACTION_SAVE, name);
                }
                return true;
            }
            if (over(mx, my, px + COPY_X, py + SHARE_Y, COPY_W, SHARE_H)) {
                NotchWidgets.click();
                // The server builds the code and sends it back; the clipboard write happens there.
                NotchPacketsClient.sendNpcShare(npcId, NpcShareManager.ACTION_COPY, "");
                return true;
            }
            if (over(mx, my, px + PASTE_X, py + SHARE_Y, PASTE_W, SHARE_H)) {
                NotchWidgets.click();
                String code = this.minecraft == null ? "" : this.minecraft.keyboardHandler.getClipboard();
                // Checked here too so an unrelated clipboard says so instantly instead of after a
                // round trip. The server still validates: this is convenience, not the gate.
                if (!NpcShareCodec.looksLikeCode(code)) {
                    say("There's no NPC share code on your clipboard.", ChatFormatting.RED);
                    return true;
                }
                // Writing an over-long string to the buffer throws, and the packet itself would be
                // refused by the connection. Say so instead, and point at the route that has room.
                if (code.strip().length() > NpcShareCodec.MAX_WIRE_CHARS) {
                    say("That code is too big to paste. Save it as a .npc file and use 'From file'.",
                            ChatFormatting.RED);
                    return true;
                }
                NotchPacketsClient.sendNpcShare(npcId, NpcShareManager.ACTION_PASTE, code);
                NotchPacketsClient.sendNpcEditorReopen(npcId, 5); // back to the editor, preview updated
                return true;
            }
            if (over(mx, my, px + TOFILE_X, py + SHARE_Y, TOFILE_W, SHARE_H)) {
                String name = nameField.getValue().trim();
                if (name.isEmpty()) {
                    say("Type a name in the box above first.", ChatFormatting.RED);
                    return true;
                }
                NotchWidgets.click();
                NotchPacketsClient.sendNpcShare(npcId, NpcShareManager.ACTION_SAVE_FILE, name);
                return true;
            }
            if (over(mx, my, px + FROMFILE_X, py + SHARE_Y, FROMFILE_W, SHARE_H)) {
                String name = nameField.getValue().trim();
                if (name.isEmpty()) {
                    say("Type the file's name in the box above first.", ChatFormatting.RED);
                    return true;
                }
                NotchWidgets.click();
                NotchPacketsClient.sendNpcShare(npcId, NpcShareManager.ACTION_LOAD_FILE, name);
                NotchPacketsClient.sendNpcEditorReopen(npcId, 5);
                return true;
            }
            if (over(mx, my, px + 70, py + BACK_Y, 160, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 5); // return to the NPC editor
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
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        int maxScroll = Math.max(0, presets.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount)));
        return true;
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        // Plain characters insert via charTyped only (guards against the select-all wipe).
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, nameField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private void say(String text, ChatFormatting color) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(text).withStyle(color), false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=1.21 {
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
