package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.action.NpcActions;
import net.fugginbeenus.notchcurrency.npc.action.NpcTrigger;
import net.fugginbeenus.notchcurrency.client.npc.NpcActionEditing;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NpcActionsScreen extends Screen {

    private static final int W = 340, H = 232;
    private static final int TRIG_X = 10, TRIG_Y = 42, TRIG_W = 116, TRIG_H = 18;
    private static final int ED_X = 134, ED_W = W - ED_X - 10;
    private static final int ROW_H = 16;

    private final UUID npcId;
    private final Map<NpcTrigger, List<DialogueAction>> working = new EnumMap<>(NpcTrigger.class);
    private int proximityRadius;

    private NpcTrigger trigger = NpcTrigger.ON_INTERACT;
    private int selected = -1;

    private int px, py;
    private TextFieldWidget valueField;
    private TextFieldWidget amountField;

    public NpcActionsScreen(UUID npcId, NpcActions actions) {
        super(Text.literal("Reactions"));
        this.npcId = npcId;
        for (NpcTrigger t : NpcTrigger.values()) {
            working.put(t, new ArrayList<>(actions.get(t)));
        }
        this.proximityRadius = actions.proximityRadius();
    }

    private List<DialogueAction> rows() { return working.get(trigger); }

    private DialogueAction current() {
        List<DialogueAction> list = rows();
        return (selected >= 0 && selected < list.size()) ? list.get(selected) : null;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;

        valueField = new TextFieldWidget(this.textRenderer, px + ED_X + 46, py + 150, ED_W - 48, 10, Text.empty());
        valueField.setMaxLength(200);
        valueField.setDrawsBackground(false);
        valueField.setChangedListener(s -> {
            DialogueAction a = current();
            if (a != null && NpcActionEditing.needsValue(a.type())) a.setValue(s);
        });
        addDrawableChild(valueField);

        amountField = new TextFieldWidget(this.textRenderer, px + ED_X + 46, py + 170, 60, 10, Text.empty());
        amountField.setMaxLength(9);
        amountField.setDrawsBackground(false);
        amountField.setChangedListener(s -> {
            DialogueAction a = current();
            if (a == null || !NpcActionEditing.needsAmount(a.type())) return;
            try {
                a.setAmount(s.isBlank() ? 0 : Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
                // half-typed number: leave the last good value alone
            }
        });
        addDrawableChild(amountField);

        syncFields();
    }

    private void syncFields() {
        DialogueAction a = current();
        boolean value = a != null && NpcActionEditing.needsValue(a.type());
        boolean amount = a != null && NpcActionEditing.needsAmount(a.type());
        valueField.setVisible(value);
        valueField.setText(value ? a.value() : "");
        amountField.setVisible(amount);
        amountField.setText(amount && a.amount() > 0 ? Long.toString(a.amount()) : "");
    }

    private int trigY(int i) { return py + TRIG_Y + i * (TRIG_H + 2); }
    private int rowY(int i) { return py + 70 + i * (ROW_H + 2); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderInGameBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Reactions", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.textRenderer, "What this NPC does when something happens.",
                px + W / 2, py + 20, NotchTheme.TEXT_MUTED, false);

        // Left: the moments.
        NpcTrigger[] triggers = NpcTrigger.values();
        for (int i = 0; i < triggers.length; i++) {
            NpcTrigger t = triggers[i];
            int ty = trigY(i);
            boolean hover = over(mouseX, mouseY, px + TRIG_X, ty, TRIG_W, TRIG_H);
            int count = working.get(t).size();
            String label = count > 0 ? t.label() + " (" + count + ")" : t.label();
            label = this.textRenderer.trimToWidth(label, TRIG_W - 8);
            if (t == trigger) {
                NotchWidgets.primaryButton(ctx, this.textRenderer, px + TRIG_X, ty, TRIG_W, TRIG_H, label, hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.textRenderer, px + TRIG_X, ty, TRIG_W, TRIG_H, label, hover);
            }
        }

        // Right: what happens.
        ctx.drawText(this.textRenderer, trigger.label(), px + ED_X, py + 42, NotchTheme.TEXT_DARK, false);
        ctx.drawText(this.textRenderer,
                this.textRenderer.trimToWidth(trigger.hint(), ED_W), px + ED_X, py + 54,
                NotchTheme.TEXT_MUTED, false);

        List<DialogueAction> list = rows();
        for (int i = 0; i < list.size(); i++) {
            int ry = rowY(i);
            boolean hover = over(mouseX, mouseY, px + ED_X, ry, ED_W - 20, ROW_H);
            String label = this.textRenderer.trimToWidth(NpcActionEditing.actionName(list.get(i).type()), ED_W - 28);
            if (i == selected) {
                NotchWidgets.primaryButton(ctx, this.textRenderer, px + ED_X, ry, ED_W - 20, ROW_H, label, hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X, ry, ED_W - 20, ROW_H, label, hover);
            }
            NotchWidgets.dangerButton(ctx, this.textRenderer, px + ED_X + ED_W - 17, ry, 17, ROW_H, "x",
                    over(mouseX, mouseY, px + ED_X + ED_W - 17, ry, 17, ROW_H));
        }
        if (list.isEmpty()) {
            ctx.drawText(this.textRenderer, "Nothing yet.", px + ED_X, py + 74, NotchTheme.TEXT_MUTED, false);
        }
        if (list.size() < NpcActions.MAX_PER_TRIGGER) {
            int ay = rowY(list.size());
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X, ay, ED_W, ROW_H, "+ Add",
                    over(mouseX, mouseY, px + ED_X, ay, ED_W, ROW_H));
        }

        // Selected action's details.
        DialogueAction a = current();
        if (a != null) {
            NotchWidgets.divider(ctx, px + ED_X, py + 144, ED_W);
            if (NpcActionEditing.needsValue(a.type())) {
                String hint = switch (a.type()) {
                    case SAY_LINE -> "Says:";
                    case GIVE_ITEM -> "Item id:";
                    default -> "Command:";
                };
                ctx.drawText(this.textRenderer, hint, px + ED_X, py + 152, NotchTheme.TEXT_DARK, false);
                NotchWidgets.inset(ctx, px + ED_X + 44, py + 147, ED_W - 44, 14, NotchTheme.DEEP);
            }
            if (NpcActionEditing.needsAmount(a.type())) {
                ctx.drawText(this.textRenderer, "Amount:", px + ED_X, py + 172, NotchTheme.TEXT_DARK, false);
                NotchWidgets.inset(ctx, px + ED_X + 44, py + 167, 64, 14, NotchTheme.DEEP);
            }
            if (a.type() == DialogueAction.Type.SAY_LINE) {
                ctx.drawText(this.textRenderer, "%player% %npc% %balance% and &-colours work",
                        px + ED_X, py + 188, NotchTheme.TEXT_MUTED, false);
            }
        }

        // Proximity gets one extra control.
        if (trigger == NpcTrigger.ON_PROXIMITY) {
            ctx.drawText(this.textRenderer, "Range: " + proximityRadius + " blocks",
                    px + TRIG_X, py + H - 52, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + TRIG_X, py + H - 40, 20, 14, "-",
                    over(mouseX, mouseY, px + TRIG_X, py + H - 40, 20, 14));
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + TRIG_X + 24, py + H - 40, 20, 14, "+",
                    over(mouseX, mouseY, px + TRIG_X + 24, py + H - 40, 20, 14));
        }

        NotchWidgets.primaryButton(ctx, this.textRenderer, px + ED_X, py + H - 26, 120, 16, "Save & Close",
                over(mouseX, mouseY, px + ED_X, py + H - 26, 120, 16));
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X + 126, py + H - 26, 70, 16, "Discard",
                over(mouseX, mouseY, px + ED_X + 126, py + H - 26, 70, 16));

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;

            NpcTrigger[] triggers = NpcTrigger.values();
            for (int i = 0; i < triggers.length; i++) {
                if (over(mx, my, px + TRIG_X, trigY(i), TRIG_W, TRIG_H)) {
                    NotchWidgets.click();
                    trigger = triggers[i];
                    selected = -1;
                    syncFields();
                    return true;
                }
            }

            List<DialogueAction> list = rows();
            for (int i = 0; i < list.size(); i++) {
                if (over(mx, my, px + ED_X + ED_W - 17, rowY(i), 17, ROW_H)) {
                    NotchWidgets.tick();
                    list.remove(i);
                    selected = -1;
                    syncFields();
                    return true;
                }
                if (over(mx, my, px + ED_X, rowY(i), ED_W - 20, ROW_H)) {
                    NotchWidgets.click();
                    if (i == selected) {
                        NpcActionEditing.cycleType(list.get(i)); // clicking the selected row again cycles what it does
                    } else {
                        selected = i;
                    }
                    syncFields();
                    return true;
                }
            }
            if (list.size() < NpcActions.MAX_PER_TRIGGER
                    && over(mx, my, px + ED_X, rowY(list.size()), ED_W, ROW_H)) {
                NotchWidgets.click();
                list.add(new DialogueAction(DialogueAction.Type.SAY_LINE, "", 0));
                selected = list.size() - 1;
                syncFields();
                return true;
            }

            if (trigger == NpcTrigger.ON_PROXIMITY) {
                if (over(mx, my, px + TRIG_X, py + H - 40, 20, 14)) {
                    NotchWidgets.tick();
                    proximityRadius = Math.max(NpcActions.MIN_RADIUS, proximityRadius - 1);
                    return true;
                }
                if (over(mx, my, px + TRIG_X + 24, py + H - 40, 20, 14)) {
                    NotchWidgets.tick();
                    proximityRadius = Math.min(NpcActions.MAX_RADIUS, proximityRadius + 1);
                    return true;
                }
            }

            if (over(mx, my, px + ED_X, py + H - 26, 120, 16)) {
                NotchWidgets.click();
                save();
                return true;
            }
            if (over(mx, my, px + ED_X + 126, py + H - 26, 70, 16)) {
                NotchWidgets.click();
                this.close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void save() {
        NpcActions out = new NpcActions();
        for (NpcTrigger t : NpcTrigger.values()) {
            List<DialogueAction> kept = new ArrayList<>();
            for (DialogueAction a : working.get(t)) {
                // Drop half-finished rows rather than storing something that silently does nothing.
                if (NpcActionEditing.needsValue(a.type()) && a.value().isBlank()) continue;
                kept.add(a);
            }
            out.set(t, kept);
        }
        out.setProximityRadius(proximityRadius);
        NotchPacketsClient.sendNpcActionsSave(npcId, out.toNbt());
        this.close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, valueField, amountField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean shouldPause() { return false; }

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
