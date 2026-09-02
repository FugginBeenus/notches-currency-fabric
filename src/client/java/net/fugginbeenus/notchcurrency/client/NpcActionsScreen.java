package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.action.NpcActions;
import net.fugginbeenus.notchcurrency.npc.action.NpcTrigger;
import net.fugginbeenus.notchcurrency.client.npc.NpcActionEditing;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NpcActionsScreen extends Screen {

    private static final int W = 340, H = 252;
    private static final int TRIG_X = 10, TRIG_Y = 42, TRIG_W = 116, TRIG_H = 18;
    private static final int ED_X = 134, ED_W = W - ED_X - 10;
    private static final int ROW_H = 16;

    private final UUID npcId;
    private final Map<NpcTrigger, List<DialogueAction>> working = new EnumMap<>(NpcTrigger.class);
    private int proximityRadius;
    private int npcCooldown;

    private NpcTrigger trigger = NpcTrigger.ON_INTERACT;
    private int selected = -1;

    private int px, py;
    private EditBox valueField;
    private EditBox amountField;
    private EditBox soundField;
    private EditBox filterField;
    private String npcFilter = "";

    public NpcActionsScreen(UUID npcId, NpcActions actions) {
        super(Component.literal("Reactions"));
        this.npcId = npcId;
        for (NpcTrigger t : NpcTrigger.values()) {
            working.put(t, new ArrayList<>(actions.get(t)));
        }
        this.proximityRadius = actions.proximityRadius();
        this.npcCooldown = actions.npcCooldownSeconds();
        this.npcFilter = actions.npcNameFilter();
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

        valueField = new EditBox(this.font, px + ED_X + 46, py + 150, ED_W - 48, 10, Component.empty());
        valueField.setMaxLength(200);
        valueField.setBordered(false);
        valueField.setResponder(s -> {
            DialogueAction a = current();
            if (a != null && NpcActionEditing.needsValue(a.type())) a.setValue(s);
        });
        addRenderableWidget(valueField);

        amountField = new EditBox(this.font, px + ED_X + 46, py + 170, 60, 10, Component.empty());
        amountField.setMaxLength(9);
        amountField.setBordered(false);
        amountField.setResponder(s -> {
            DialogueAction a = current();
            if (a == null || !NpcActionEditing.needsAmount(a.type())) return;
            try {
                a.setAmount(s.isBlank() ? 0 : Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(amountField);

        soundField = new EditBox(this.font, px + ED_X + 46, py + 191, 92, 10, Component.empty());
        soundField.setMaxLength(80);
        soundField.setBordered(false);
        soundField.setHint(Component.literal("sound id")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        soundField.setResponder(t -> {
            DialogueAction a = current();
            if (a != null && a.type() == DialogueAction.Type.SAY_LINE) a.setSound(t);
        });
        addRenderableWidget(soundField);

        filterField = new EditBox(this.font, px + TRIG_X + 3, filterY() + 12, 72, 10, Component.empty());
        filterField.setMaxLength(64);
        filterField.setBordered(false);
        filterField.setHint(Component.literal("any NPC")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        filterField.setResponder(t -> npcFilter = t.trim());
        filterField.setValue(npcFilter);
        addRenderableWidget(filterField);

        syncFields();
    }

    private void syncFields() {
        DialogueAction a = current();
        boolean value = a != null && NpcActionEditing.needsValue(a.type());
        boolean amount = a != null && NpcActionEditing.needsAmount(a.type());
        valueField.setVisible(value);
        valueField.setWidth(a != null && a.type() == DialogueAction.Type.GIVE_EFFECT
                ? ED_W - 100 : ED_W - 48);
        valueField.setValue(value ? a.value() : "");
        amountField.setVisible(amount);
        amountField.setValue(amount && a.amount() > 0 ? Long.toString(a.amount()) : "");
        boolean line = a != null && a.type() == DialogueAction.Type.SAY_LINE;
        soundField.setVisible(line);
        soundField.setValue(line ? a.sound() : "");
        if (filterField != null) filterField.setVisible(trigger == NpcTrigger.ON_NPC_NEAR);
    }

    private String fit(String text, int room) {
        if (this.font.width(text) <= room) return text;
        String cut = this.font.plainSubstrByWidth(text, room - this.font.width("..."));
        return cut + "...";
    }

    private int trigY(int i) { return py + TRIG_Y + i * (TRIG_H + 2); }

    private int settingsTop() { return trigY(NpcTrigger.values().length - 1) + TRIG_H + 6; }
    private int setRowY(int i) { return settingsTop() + i * 28; }
    private int cooldownY() { return setRowY(0); }
    private int rangeY() { return trigger == NpcTrigger.ON_NPC_NEAR ? setRowY(1) : setRowY(0); }
    private int filterY() { return setRowY(2); }

    private void cycleFilterName() {
        java.util.List<String> names = new ArrayList<>();
        names.add("");
        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            for (net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc
                    : this.minecraft.level.getEntitiesOfClass(
                            net.fugginbeenus.notchcurrency.entity.NotchNpcEntity.class,
                            this.minecraft.player.getBoundingBox().inflate(48.0))) {
                String n = net.fugginbeenus.notchcurrency.npc.NpcText.npcName(npc);
                if (!n.isBlank() && !names.contains(n)) names.add(n);
            }
        }
        int at = names.indexOf(npcFilter);
        npcFilter = names.get((at < 0 ? 0 : at + 1) % names.size());
        filterField.setValue(npcFilter);
    }
    private int rowY(int i) { return py + 70 + i * (ROW_H + 2); }

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
        NotchWidgets.title(ctx, this.font, "Reactions", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "What this NPC does when something happens.",
                px + W / 2, py + 20, NotchTheme.TEXT_MUTED, false);

        NpcTrigger[] triggers = NpcTrigger.values();
        for (int i = 0; i < triggers.length; i++) {
            NpcTrigger t = triggers[i];
            int ty = trigY(i);
            boolean hover = over(mouseX, mouseY, px + TRIG_X, ty, TRIG_W, TRIG_H);
            int count = working.get(t).size();
            String label = count > 0 ? t.label() + " (" + count + ")" : t.label();
            label = fit(label, TRIG_W - 8);
            if (t == trigger) {
                NotchWidgets.primaryButton(ctx, this.font, px + TRIG_X, ty, TRIG_W, TRIG_H, label, hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + TRIG_X, ty, TRIG_W, TRIG_H, label, hover);
            }
        }

        ctx.drawString(this.font, trigger.label(), px + ED_X, py + 42, NotchTheme.TEXT_DARK, false);
        ctx.drawString(this.font, fit(trigger.hint(), ED_W), px + ED_X, py + 54,
                NotchTheme.TEXT_MUTED, false);

        List<DialogueAction> list = rows();
        for (int i = 0; i < list.size(); i++) {
            int ry = rowY(i);
            boolean hover = over(mouseX, mouseY, px + ED_X, ry, ED_W - 20, ROW_H);
            String label = this.font.plainSubstrByWidth(NpcActionEditing.actionName(list.get(i).type()), ED_W - 28);
            if (i == selected) {
                NotchWidgets.primaryButton(ctx, this.font, px + ED_X, ry, ED_W - 20, ROW_H, label, hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + ED_X, ry, ED_W - 20, ROW_H, label, hover);
            }
            NotchWidgets.dangerButton(ctx, this.font, px + ED_X + ED_W - 17, ry, 17, ROW_H, "x",
                    over(mouseX, mouseY, px + ED_X + ED_W - 17, ry, 17, ROW_H));
        }
        if (list.isEmpty()) {
            ctx.drawString(this.font, "Nothing yet.", px + ED_X, py + 74, NotchTheme.TEXT_MUTED, false);
        }
        if (list.size() < NpcActions.MAX_PER_TRIGGER) {
            int ay = rowY(list.size());
            NotchWidgets.neutralButton(ctx, this.font, px + ED_X, ay, ED_W, ROW_H, "+ Add",
                    over(mouseX, mouseY, px + ED_X, ay, ED_W, ROW_H));
        }

        DialogueAction a = current();
        if (a != null) {
            NotchWidgets.divider(ctx, px + ED_X, py + 144, ED_W);
            if (NpcActionEditing.needsValue(a.type())) {
                String hint = switch (a.type()) {
                    case SAY_LINE -> "Says:";
                    case GIVE_ITEM -> "Item id:";
                    case GIVE_EFFECT -> "Effect:";
                    default -> "Command:";
                };
                boolean effect = a.type() == DialogueAction.Type.GIVE_EFFECT;
                ctx.drawString(this.font, hint, px + ED_X, py + 152, NotchTheme.TEXT_DARK, false);
                NotchWidgets.inset(ctx, px + ED_X + 44, py + 147,
                        effect ? ED_W - 96 : ED_W - 44, 14, NotchTheme.DEEP);
                if (effect) {
                    NotchWidgets.neutralButton(ctx, this.font, px + ED_X + ED_W - 48, py + 147, 48, 14,
                            "Pick", over(mouseX, mouseY, px + ED_X + ED_W - 48, py + 147, 48, 14));
                }
            }
            if (NpcActionEditing.needsAmount(a.type())) {
                String unit = switch (a.type()) {
                    case HEAL_PLAYER -> "Hearts:";
                    case GIVE_EFFECT -> "Seconds:";
                    default -> "Amount:";
                };
                ctx.drawString(this.font, unit, px + ED_X, py + 172, NotchTheme.TEXT_DARK, false);
                NotchWidgets.inset(ctx, px + ED_X + 44, py + 167, 64, 14, NotchTheme.DEEP);
            }
            if (a.type() == DialogueAction.Type.SAY_LINE) {
                ctx.drawString(this.font, "Sound:", px + ED_X, py + 193, NotchTheme.TEXT_DARK, false);
                NotchWidgets.inset(ctx, px + ED_X + 44, py + 188, 100, 14, NotchTheme.DEEP);
                NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 148, py + 188, 48, 14, "Pick",
                        over(mouseX, mouseY, px + ED_X + 148, py + 188, 48, 14));

                boolean quiet = a.hideText();
                boolean hov = over(mouseX, mouseY, px + ED_X, py + 206, 90, 14);
                if (quiet) {
                    NotchWidgets.goldButton(ctx, this.font, px + ED_X, py + 206, 90, 14, "No text", hov);
                } else {
                    NotchWidgets.neutralButton(ctx, this.font, px + ED_X, py + 206, 90, 14, "Text on", hov);
                }
                ctx.drawString(this.font, fit(NpcSoundPicks.nameFor(a.sound()), 96),
                        px + ED_X + 100, py + 210, NotchTheme.TEXT_MUTED, false);
            }
        }

        if (trigger == NpcTrigger.ON_NPC_NEAR) {
            ctx.drawString(this.font, "Every " + npcCooldown + "s", px + TRIG_X, cooldownY(),
                    NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, px + TRIG_X, cooldownY() + 10, 20, 14, "-",
                    over(mouseX, mouseY, px + TRIG_X, cooldownY() + 10, 20, 14));
            NotchWidgets.neutralButton(ctx, this.font, px + TRIG_X + 24, cooldownY() + 10, 20, 14, "+",
                    over(mouseX, mouseY, px + TRIG_X + 24, cooldownY() + 10, 20, 14));
        }

        if (trigger == NpcTrigger.ON_PROXIMITY || trigger == NpcTrigger.ON_NPC_NEAR) {
            ctx.drawString(this.font, "Range: " + proximityRadius + " blocks",
                    px + TRIG_X, rangeY(), NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, px + TRIG_X, rangeY() + 10, 20, 14, "-",
                    over(mouseX, mouseY, px + TRIG_X, rangeY() + 10, 20, 14));
            NotchWidgets.neutralButton(ctx, this.font, px + TRIG_X + 24, rangeY() + 10, 20, 14, "+",
                    over(mouseX, mouseY, px + TRIG_X + 24, rangeY() + 10, 20, 14));
        }

        if (trigger == NpcTrigger.ON_NPC_NEAR) {
            ctx.drawString(this.font, "Only this NPC:", px + TRIG_X, filterY(),
                    NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + TRIG_X, filterY() + 10, 76, 14, NotchTheme.DEEP);
            NotchWidgets.neutralButton(ctx, this.font, px + TRIG_X + 80, filterY() + 10, 36, 14,
                    "Pick", over(mouseX, mouseY, px + TRIG_X + 80, filterY() + 10, 36, 14));
        }

        NotchWidgets.primaryButton(ctx, this.font, px + ED_X, py + H - 26, 120, 16, "Save & Close",
                over(mouseX, mouseY, px + ED_X, py + H - 26, 120, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 126, py + H - 26, 70, 16, "Discard",
                over(mouseX, mouseY, px + ED_X + 126, py + H - 26, 70, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}

        drawHints(ctx, mouseX, mouseY);
    }

    private void drawHints(GuiGraphics ctx, int mouseX, int mouseY) {
        if (over(mouseX, mouseY, px + ED_X, py + 50, ED_W, 12)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal(trigger.label()).withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal(trigger.hint()).withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
            return;
        }

        NpcTrigger[] all = NpcTrigger.values();
        for (int i = 0; i < all.length; i++) {
            if (!over(mouseX, mouseY, px + TRIG_X, trigY(i), TRIG_W, TRIG_H)) continue;
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal(all[i].label()).withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal(all[i].hint()).withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
            return;
        }

        if (trigger == NpcTrigger.ON_NPC_NEAR
                && (over(mouseX, mouseY, px + TRIG_X + 80, filterY() + 10, 36, 14)
                    || over(mouseX, mouseY, px + TRIG_X, filterY() + 10, 76, 14))) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Only this NPC").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("Leave it empty and any nearby NPC sets this off.")
                            .withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Type a name, or press Pick to step through").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("the NPCs loaded around you.").withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
            return;
        }

        DialogueAction a = current();
        if (a == null || a.type() != DialogueAction.Type.SAY_LINE) return;

        if (over(mouseX, mouseY, px + ED_X + 44, py + 147, ED_W - 44, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("What the NPC says").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("%player% %npc% %balance% get swapped in.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("&-codes colour the text.").withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
            return;
        }
        if (over(mouseX, mouseY, px + ED_X + 148, py + 188, 48, 14)
                || over(mouseX, mouseY, px + ED_X + 44, py + 188, 100, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal(NpcSoundPicks.nameFor(a.sound())).withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal(a.sound().isEmpty() ? "No sound plays." : a.sound())
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY),
                    Component.literal("Pick steps through the common ones.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Or type any sound id yourself.").withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
            return;
        }
        if (over(mouseX, mouseY, px + ED_X, py + 206, 90, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal(a.hideText() ? "Sound only" : "Text and sound").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("Turn text off for a grunt or a laugh.").withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
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

            DialogueAction line = current();
            if (line != null && line.type() == DialogueAction.Type.SAY_LINE) {
                if (over(mx, my, px + ED_X, py + 206, 90, 14)) {
                    NotchWidgets.click();
                    line.setHideText(!line.hideText());
                    return true;
                }
                if (over(mx, my, px + ED_X + 148, py + 188, 48, 14)) {
                    NotchWidgets.click();
                    line.setSound(NpcSoundPicks.next(line.sound()));
                    soundField.setValue(line.sound());
                    return true;
                }
            }

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
                        NpcActionEditing.cycleType(list.get(i));
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

            if (trigger == NpcTrigger.ON_NPC_NEAR) {
                if (over(mx, my, px + TRIG_X, cooldownY() + 10, 20, 14)) {
                    NotchWidgets.tick();
                    npcCooldown = Math.max(1, npcCooldown - 5);
                    return true;
                }
                if (over(mx, my, px + TRIG_X + 24, cooldownY() + 10, 20, 14)) {
                    NotchWidgets.tick();
                    npcCooldown = Math.min(600, npcCooldown + 5);
                    return true;
                }
            }
            if (trigger == NpcTrigger.ON_PROXIMITY || trigger == NpcTrigger.ON_NPC_NEAR) {
                if (over(mx, my, px + TRIG_X, rangeY() + 10, 20, 14)) {
                    NotchWidgets.tick();
                    proximityRadius = Math.max(NpcActions.MIN_RADIUS, proximityRadius - 1);
                    return true;
                }
                if (over(mx, my, px + TRIG_X + 24, rangeY() + 10, 20, 14)) {
                    NotchWidgets.tick();
                    proximityRadius = Math.min(NpcActions.MAX_RADIUS, proximityRadius + 1);
                    return true;
                }
            }
            DialogueAction pick = current();
            if (pick != null && pick.type() == DialogueAction.Type.GIVE_EFFECT
                    && over(mx, my, px + ED_X + ED_W - 48, py + 147, 48, 14)) {
                NotchWidgets.tick();
                pick.setValue(NpcEffectPicks.next(pick.value()));
                valueField.setValue(pick.value());
                return true;
            }
            if (trigger == NpcTrigger.ON_NPC_NEAR
                    && over(mx, my, px + TRIG_X + 80, filterY() + 10, 36, 14)) {
                NotchWidgets.tick();
                cycleFilterName();
                return true;
            }

            if (over(mx, my, px + ED_X, py + H - 26, 120, 16)) {
                NotchWidgets.click();
                save();
                return true;
            }
            if (over(mx, my, px + ED_X + 126, py + H - 26, 70, 16)) {
                NotchWidgets.click();
                this.onClose();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void save() {
        NpcActions out = new NpcActions();
        for (NpcTrigger t : NpcTrigger.values()) {
            List<DialogueAction> kept = new ArrayList<>();
            for (DialogueAction a : working.get(t)) {
                if (NpcActionEditing.needsValue(a.type()) && a.value().isBlank()) continue;
                kept.add(a);
            }
            out.set(t, kept);
        }
        out.setProximityRadius(proximityRadius);
        out.setNpcCooldownSeconds(npcCooldown);
        out.setNpcNameFilter(npcFilter);
        NotchPacketsClient.sendNpcActionsSave(npcId, out.toNbt());
        this.onClose();
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
