package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueNode;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuickLinesScreen extends Screen {

    private static final int W = 340, H = 232;
    // 8 lines × 15px ends at y+160, safely above the edit row at y+169.
    private static final int LIST_X = 10, LIST_Y = 40, ROW_H = 15, MAX_LINES = 8;

    private final UUID npcId;
    private final List<String> lines = new ArrayList<>();
    private int selected = -1;

    private int px, py;
    private TextFieldWidget editField;

    public QuickLinesScreen(UUID npcId, DialogueTree tree) {
        super(Text.literal("Quick Lines"));
        this.npcId = npcId;
        // Seed from an existing FLAT tree only: a branching tree must not be flattened here.
        if (tree != null && tree.isFlat()) {
            for (DialogueNode n : tree.nodes().values()) {
                if (!n.text().isBlank() && lines.size() < MAX_LINES) lines.add(n.text());
            }
        }
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;

        String kept = editField == null ? "" : editField.getText();
        editField = new TextFieldWidget(this.textRenderer, px + LIST_X + 4, py + 173, W - 96, 10, Text.empty());
        editField.setMaxLength(150);
        editField.setDrawsBackground(false);
        editField.setPlaceholder(Text.literal("type a line, then Apply").formatted(Formatting.DARK_GRAY));
        editField.setText(kept);
        addDrawableChild(editField);
    }

    private int rowY(int i) { return py + LIST_Y + i * ROW_H; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderInGameBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Quick Lines", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.textRenderer,
                "The NPC says one of these at random when talked to.",
                px + W / 2, py + 20, NotchTheme.TEXT_MUTED, false);

        // Line list.
        NotchWidgets.inset(ctx, px + LIST_X - 2, py + LIST_Y - 4, W - 16, MAX_LINES * ROW_H + 8, NotchTheme.PANEL_MID);
        for (int i = 0; i < lines.size(); i++) {
            int ry = rowY(i);
            boolean hover = over(mouseX, mouseY, px + LIST_X, ry, W - 44, ROW_H - 1);
            if (i == selected) {
                NotchWidgets.primaryButton(ctx, this.textRenderer, px + LIST_X, ry, W - 44, ROW_H - 1, "", hover);
            } else {
                NotchWidgets.button(ctx, px + LIST_X, ry, W - 44, ROW_H - 1, hover, false);
            }
            String shown = this.textRenderer.trimToWidth((i + 1) + ". " + lines.get(i), W - 52);
            ctx.drawText(this.textRenderer, shown, px + LIST_X + 4, ry + 3,
                    i == selected ? NotchTheme.TEXT_LIGHT : NotchTheme.TEXT_DARK, i == selected);
            NotchWidgets.dangerButton(ctx, this.textRenderer, px + W - 30, ry, 16, ROW_H - 1, "x",
                    over(mouseX, mouseY, px + W - 30, ry, 16, ROW_H - 1));
        }
        if (lines.isEmpty()) {
            NotchWidgets.centerText(ctx, this.textRenderer, "No lines yet - type one below and Apply.",
                    px + W / 2, py + LIST_Y + 40, NotchTheme.TEXT_MUTED, false);
        }

        // Edit row: field + Apply (updates the selected line, or adds a new one).
        NotchWidgets.inset(ctx, px + LIST_X, py + 169, W - 92, 15, NotchTheme.DEEP);
        String applyLabel = selected >= 0 ? "Apply" : "+ Add";
        boolean canApply = !editField.getText().isBlank() && (selected >= 0 || lines.size() < MAX_LINES);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + W - 76, py + 169, 66, 15, applyLabel,
                canApply && over(mouseX, mouseY, px + W - 76, py + 169, 66, 15));
        NotchWidgets.centerText(ctx, this.textRenderer,
                selected >= 0 ? "Editing line " + (selected + 1) + " - click it again to deselect."
                        : lines.size() + "/" + MAX_LINES + " lines",
                px + W / 2, py + 189, NotchTheme.TEXT_MUTED, false);

        // Bottom bar.
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 10, py + H - 26, 150, 16, "Save & Back",
                over(mouseX, mouseY, px + 10, py + H - 26, 150, 16));
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 168, py + H - 26, 90, 16, "Discard",
                over(mouseX, mouseY, px + 168, py + H - 26, 90, 16));

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            for (int i = 0; i < lines.size(); i++) {
                if (over(mx, my, px + W - 30, rowY(i), 16, ROW_H - 1)) {
                    NotchWidgets.tick();
                    lines.remove(i);
                    if (selected == i) { selected = -1; editField.setText(""); }
                    else if (selected > i) selected--;
                    return true;
                }
                if (over(mx, my, px + LIST_X, rowY(i), W - 44, ROW_H - 1)) {
                    NotchWidgets.tick();
                    if (selected == i) {
                        selected = -1;
                        editField.setText("");
                    } else {
                        selected = i;
                        editField.setText(lines.get(i));
                    }
                    return true;
                }
            }
            if (over(mx, my, px + W - 76, py + 169, 66, 15)) {
                String text = editField.getText().trim();
                if (!text.isEmpty()) {
                    NotchWidgets.tick();
                    if (selected >= 0) {
                        lines.set(selected, text);
                        selected = -1;
                    } else if (lines.size() < MAX_LINES) {
                        lines.add(text);
                    }
                    editField.setText("");
                }
                return true;
            }
            if (over(mx, my, px + 10, py + H - 26, 150, 16)) {
                NotchWidgets.click();
                save();
                return true;
            }
            if (over(mx, my, px + 168, py + H - 26, 90, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 3);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void save() {
        DialogueTree tree = new DialogueTree();
        for (int i = 0; i < lines.size(); i++) {
            DialogueNode n = new DialogueNode("line_" + (i + 1));
            n.setText(lines.get(i));
            tree.put(n);
        }
        NotchPacketsClient.sendNpcStudioSave(npcId, tree.toNbt());
        if (!lines.isEmpty()) {
            NotchPacketsClient.sendNpcDialogueMode(npcId, 1); // CHAT. That's the point of quick lines
        }
        NotchPacketsClient.sendNpcEditorReopen(npcId, 3);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter applies the current line, like clicking Apply.
        if ((keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)
                && editField.isFocused() && !editField.getText().isBlank()) {
            String text = editField.getText().trim();
            if (selected >= 0) {
                lines.set(selected, text);
                selected = -1;
            } else if (lines.size() < MAX_LINES) {
                lines.add(text);
            }
            editField.setText("");
            NotchWidgets.tick();
            return true;
        }
        // Plain characters insert via charTyped only (guards against the select-all wipe).
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, editField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
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
