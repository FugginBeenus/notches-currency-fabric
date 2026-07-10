package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.NpcPresetManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

/**
 * Preset library: saved NPC setups (config/notchcurrency/npc_presets/) that can be stamped onto the
 * NPC being edited. Pick a preset and Load, or type a name and Save the current NPC. The server
 * re-sends the list after every action, so the screen always shows what's really on disk.
 */
public class NpcPresetScreen extends Screen {

    private static final int W = 300, H = 224;
    private static final int LIST_X = 14, LIST_Y = 22, LIST_W = 272, LIST_H = 116;
    private static final int ROW_H = 16, VISIBLE_ROWS = 7;

    private final UUID npcId;
    private List<String> presets;
    private int selected = -1;
    private int scroll = 0;
    private TextFieldWidget nameField;

    public NpcPresetScreen(UUID npcId, List<String> presets) {
        super(Text.literal("NPC Presets"));
        this.npcId = npcId;
        this.presets = presets;
    }

    public boolean isFor(UUID id) {
        return npcId.equals(id);
    }

    /** Fresh list from the server after a save/load/delete. */
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
        String old = nameField == null ? "" : nameField.getText();
        nameField = new TextFieldWidget(this.textRenderer, px() + 66, py() + 173, 142, 9, Text.literal("Preset name"));
        nameField.setMaxLength(32);
        nameField.setDrawsBackground(false);
        nameField.setPlaceholder(Text.literal("preset name").formatted(Formatting.DARK_GRAY));
        nameField.setText(old);
        addDrawableChild(nameField);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        int px = px(), py = py();
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "NPC Presets", px + W / 2, py + 8);

        NotchWidgets.inset(ctx, px + LIST_X, py + LIST_Y, LIST_W, LIST_H, NotchTheme.DEEP);
        if (presets.isEmpty()) {
            NotchWidgets.centerText(ctx, this.textRenderer, "No presets saved yet.",
                    px + W / 2, py + LIST_Y + LIST_H / 2 - 4, NotchTheme.TEXT_MUTED, false);
        }
        for (int v = 0; v < VISIBLE_ROWS; v++) {
            int i = scroll + v;
            if (i >= presets.size()) break;
            boolean hover = over(mouseX, mouseY, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 1);
            if (i == selected) {
                NotchWidgets.primaryButton(ctx, this.textRenderer, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 1, presets.get(i), hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.textRenderer, px + LIST_X + 2, rowY(v), LIST_W - 4, ROW_H - 1, presets.get(i), hover);
            }
        }
        if (presets.size() > VISIBLE_ROWS) {
            NotchWidgets.centerText(ctx, this.textRenderer, (scroll + 1) + "-" + Math.min(scroll + VISIBLE_ROWS, presets.size())
                    + " of " + presets.size() + " (scroll)", px + W / 2, py + LIST_Y + LIST_H + 3, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 16, py + 146, 170, 15, "Load onto this NPC",
                over(mouseX, mouseY, px + 16, py + 146, 170, 15));
        NotchWidgets.dangerButton(ctx, this.textRenderer, px + 192, py + 146, 92, 15, "Delete",
                over(mouseX, mouseY, px + 192, py + 146, 92, 15));

        NotchWidgets.divider(ctx, px + 8, py + 165, W - 16);
        ctx.drawText(this.textRenderer, "Save as:", px + 16, py + 173, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + 62, py + 170, 150, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 218, py + 170, 66, 14, "Save",
                over(mouseX, mouseY, px + 218, py + 170, 66, 14));

        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 70, py + 196, 160, 16, "Back to Editor",
                over(mouseX, mouseY, px + 70, py + 196, 160, 16));

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                String name = nameField.getText().trim();
                if (!name.isEmpty()) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendNpcPreset(npcId, NpcPresetManager.ACTION_SAVE, name);
                }
                return true;
            }
            if (over(mx, my, px + 70, py + 196, 160, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 5); // return to the NPC editor
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int maxScroll = Math.max(0, presets.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Plain characters insert via charTyped only (guards against the select-all wipe).
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, nameField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
