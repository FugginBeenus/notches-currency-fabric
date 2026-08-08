package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.npc.NpcActionEditing;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.fugginbeenus.notchcurrency.npc.schedule.ScheduleEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NpcScheduleActionsScreen extends Screen {

    private static final int W = 300, H = 216;
    // Rows, kept as constants because they used to be scattered literals and the value row ended
    // up drawn straight over the buttons.
    private static final int BTN_Y = 128, VALUE_Y = 150, AMOUNT_Y = 170, SAVE_Y = 192;
    private static final int LIST_X = 12, LIST_Y = 40, LIST_W = W - 24, LIST_H = 84;
    private static final int ROW_H = 16;

    private final Screen parent;
    private final String entryLabel;
    private final List<DialogueAction> working = new ArrayList<>();
    private final Consumer<List<DialogueAction>> onSave;

    private int selected = -1;
    private int px, py;
    private EditBox valueField;
    private EditBox amountField;

    public NpcScheduleActionsScreen(Screen parent, String entryLabel,
                                    List<DialogueAction> actions, Consumer<List<DialogueAction>> onSave) {
        super(Component.literal("When it starts"));
        this.parent = parent;
        this.entryLabel = entryLabel;
        this.onSave = onSave;
        for (DialogueAction a : actions) {
            working.add(DialogueAction.fromNbt(a.toNbt())); // edit a copy: Back must really mean back
        }
        if (!working.isEmpty()) selected = 0;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;

        valueField = new EditBox(this.font, px + 60, py + VALUE_Y + 3, W - 80, 10, Component.empty());
        valueField.setMaxLength(200);
        valueField.setBordered(false);
        valueField.setResponder(s -> {
            DialogueAction a = current();
            if (a != null && NpcActionEditing.needsValue(a.type())) a.setValue(s);
        });
        addRenderableWidget(valueField);

        amountField = new EditBox(this.font, px + 60, py + AMOUNT_Y + 3, 60, 10, Component.empty());
        amountField.setMaxLength(9);
        amountField.setBordered(false);
        amountField.setResponder(s -> {
            DialogueAction a = current();
            if (a == null || !NpcActionEditing.needsAmount(a.type())) return;
            try {
                a.setAmount(s.isBlank() ? 0 : Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                // half-typed number: leave the last good value alone
            }
        });
        addRenderableWidget(amountField);

        syncFields();
    }

    @org.jetbrains.annotations.Nullable
    private DialogueAction current() {
        return selected >= 0 && selected < working.size() ? working.get(selected) : null;
    }

    private void syncFields() {
        DialogueAction a = current();
        boolean wantsValue = a != null && NpcActionEditing.needsValue(a.type());
        boolean wantsAmount = a != null && NpcActionEditing.needsAmount(a.type());
        valueField.visible = wantsValue;
        amountField.visible = wantsAmount;
        if (wantsValue && !valueField.getValue().equals(a.value())) valueField.setValue(a.value());
        if (wantsAmount && !amountField.getValue().equals(String.valueOf(a.amount()))) {
            amountField.setValue(String.valueOf(a.amount()));
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderTransparentBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "At " + entryLabel, px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "Runs once, as this part of the day begins.",
                px + W / 2, py + 24, NotchTheme.TEXT_MUTED, false);

        NotchWidgets.inset(ctx, px + LIST_X, py + LIST_Y, LIST_W, LIST_H, NotchTheme.DEEP);
        if (working.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "Nothing yet. Add one below.",
                    px + W / 2, py + LIST_Y + LIST_H / 2 - 4, NotchTheme.TEXT_MUTED, false);
        }
        for (int i = 0; i < working.size(); i++) {
            int ry = py + LIST_Y + 2 + i * ROW_H;
            boolean hover = over(mouseX, mouseY, px + LIST_X + 2, ry, LIST_W - 4, ROW_H - 2);
            String text = NpcActionEditing.describe(working.get(i));
            if (i == selected) {
                NotchWidgets.primaryButton(ctx, this.font, px + LIST_X + 2, ry, LIST_W - 4, ROW_H - 2, text, hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + LIST_X + 2, ry, LIST_W - 4, ROW_H - 2, text, hover);
            }
        }

        NotchWidgets.primaryButton(ctx, this.font, px + LIST_X, py + BTN_Y, 54, 14, "Add",
                over(mouseX, mouseY, px + LIST_X, py + BTN_Y, 54, 14));
        NotchWidgets.dangerButton(ctx, this.font, px + LIST_X + 58, py + BTN_Y, 54, 14, "Remove",
                over(mouseX, mouseY, px + LIST_X + 58, py + BTN_Y, 54, 14));
        DialogueAction sel = current();
        if (sel != null) {
            NotchWidgets.goldButton(ctx, this.font, px + LIST_X + 116, py + BTN_Y, 100, 14,
                    NpcActionEditing.actionName(sel.type()),
                    over(mouseX, mouseY, px + LIST_X + 116, py + BTN_Y, 100, 14));
        }

        if (sel != null && NpcActionEditing.needsValue(sel.type())) {
            ctx.drawString(this.font, "Text", px + 14, py + VALUE_Y + 3, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + 56, py + VALUE_Y, W - 72, 14, NotchTheme.DEEP);
            if (valueField.getValue().isEmpty()) {
                ctx.drawString(this.font, NpcActionEditing.valueHint(sel.type()),
                        px + 60, py + VALUE_Y + 3, 0xFF555555, false);
            }
        }
        if (sel != null && NpcActionEditing.needsAmount(sel.type())) {
            ctx.drawString(this.font, "Amount", px + 14, py + AMOUNT_Y + 3, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + 56, py + AMOUNT_Y, 68, 14, NotchTheme.DEEP);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + W / 2 - 70, py + SAVE_Y, 140, 16, "Save & Back",
                over(mouseX, mouseY, px + W / 2 - 70, py + SAVE_Y, 140, 16));

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
            for (int i = 0; i < working.size(); i++) {
                int ry = py + LIST_Y + 2 + i * ROW_H;
                if (over(mx, my, px + LIST_X + 2, ry, LIST_W - 4, ROW_H - 2)) {
                    NotchWidgets.tick();
                    selected = i;
                    syncFields();
                    return true;
                }
            }
            if (over(mx, my, px + LIST_X, py + BTN_Y, 54, 14)) {
                if (working.size() >= ScheduleEntry.MAX_ACTIONS) {
                    say("That's as many as one entry can run.");
                    return true;
                }
                NotchWidgets.click();
                DialogueAction a = new DialogueAction();
                a.setType(DialogueAction.Type.SAY_LINE);
                working.add(a);
                selected = working.size() - 1;
                syncFields();
                return true;
            }
            if (over(mx, my, px + LIST_X + 58, py + BTN_Y, 54, 14) && current() != null) {
                NotchWidgets.click();
                working.remove(selected);
                selected = Math.min(selected, working.size() - 1);
                syncFields();
                return true;
            }
            if (current() != null && over(mx, my, px + LIST_X + 116, py + BTN_Y, 100, 14)) {
                NotchWidgets.tick();
                NpcActionEditing.cycleType(current());
                syncFields();
                return true;
            }
            if (over(mx, my, px + W / 2 - 70, py + SAVE_Y, 140, 16)) {
                NotchWidgets.click();
                saveAndBack();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void saveAndBack() {
        List<DialogueAction> kept = new ArrayList<>();
        for (DialogueAction a : working) {
            // Drop half-finished rows rather than storing something that silently does nothing.
            if (NpcActionEditing.needsValue(a.type()) && a.value().isBlank()) continue;
            kept.add(a);
        }
        onSave.accept(kept);
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        saveAndBack();
    }

    private void say(String text) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.RED), false);
        }
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, valueField, amountField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
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
