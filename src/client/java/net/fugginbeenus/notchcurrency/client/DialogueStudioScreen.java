package net.fugginbeenus.notchcurrency.client;

import net.minecraft.client.Minecraft;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueNode;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DialogueStudioScreen extends Screen {

    private static final int W = 400, H = 260;
    // Left pane: page list.
    private static final int LIST_X = 8, LIST_Y = 40, LIST_W = 100, ROW_H = 14, LIST_ROWS = 12;
    // Right pane.
    private static final int ED_X = 116, ED_W = 276;
    private static final int MAX_NODES = 24, MAX_CHOICES = 5;

    private final UUID npcId;
    private final DialogueTree tree;

    private int px, py;
    private String selectedId = "";
    private int choiceIdx = -1;  // -1 = editing the page; >=0 = editing that choice
    private int actionIdx = 0;   // which of the choice's two action slots is being edited
    private int condIdx = 0;     // which of the two condition slots
    private int listScroll = 0;
    private String statusMsg = "";
    private long statusUntil = 0;

    private net.minecraft.client.gui.components.MultiLineEditBox nodeTextBox; // multiline "Says" editor
    private EditBox renameField;
    private EditBox choiceLabelField;
    private EditBox actionValueField, actionAmountField;
    private EditBox condValueField, condAmountField;

    public DialogueStudioScreen(UUID npcId, DialogueTree tree) {
        super(Component.literal("Dialogue Studio"));
        this.npcId = npcId;
        this.tree = tree;
        this.selectedId = tree.startId();
        if (selectedId.isEmpty() && !tree.nodes().isEmpty()) {
            this.selectedId = tree.nodes().keySet().iterator().next();
        }
    }

    // ---- setup ----

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;

        // Multiline "Says" editor: type freely, it wraps and scrolls (finally).
        nodeTextBox = new net.minecraft.client.gui.components.MultiLineEditBox(this.font,
                px + ED_X, py + 54, ED_W - 4, 56,
                Component.literal("What the NPC says...").withStyle(ChatFormatting.DARK_GRAY), Component.empty());
        nodeTextBox.setCharacterLimit(500);
        nodeTextBox.setValueListener(s -> {
            DialogueNode n = node();
            if (n != null && choiceIdx < 0) n.setText(s);
        });
        addRenderableWidget(nodeTextBox);

        renameField = field(px + ED_X + 30, py + 26, 88, 24);
        renameField.setFilter(s -> s.chars().allMatch(ch -> ch == '_' || Character.isLetterOrDigit(ch)));

        choiceLabelField = field(px + ED_X + 38, py + 56, ED_W - 40, 48);
        choiceLabelField.setResponder(s -> {
            DialogueChoice c = choice();
            if (c != null) c.setLabel(s);
        });

        actionValueField = field(px + ED_X + 60, py + 108, ED_W - 62, 200);
        actionValueField.setResponder(s -> {
            DialogueAction a = action(actionIdx, false);
            if (a != null) a.setValue(s);
        });

        actionAmountField = field(px + ED_X + 60, py + 126, 96, 9);
        actionAmountField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        actionAmountField.setResponder(s -> {
            DialogueAction a = action(actionIdx, false);
            if (a != null) a.setAmount(parse(s));
        });

        condValueField = field(px + ED_X + 60, py + 162, ED_W - 62, 200);
        condValueField.setResponder(s -> {
            DialogueCondition c = condition(condIdx, false);
            if (c != null) c.setValue(s);
        });

        condAmountField = field(px + ED_X + 60, py + 180, 96, 9);
        condAmountField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        condAmountField.setResponder(s -> {
            DialogueCondition c = condition(condIdx, false);
            if (c != null) c.setAmount(parse(s));
        });

        refreshFields();
    }

    @Override
    public void tick() {
        super.tick();
        //? if <1.21 {
        if (nodeTextBox != null) nodeTextBox.tick(); // cursor blink
        //?}
    }

    private EditBox field(int x, int y, int w, int maxLen) {
        EditBox f = new EditBox(this.font, x + 2, y + 3, w - 4, 9, Component.empty());
        f.setMaxLength(maxLen);
        f.setBordered(false);
        addRenderableWidget(f);
        return f;
    }

    private static long parse(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // ---- model accessors ----

    private DialogueNode node() {
        return tree.get(selectedId);
    }

    private DialogueChoice choice() {
        DialogueNode n = node();
        if (n == null || choiceIdx < 0 || choiceIdx >= n.choices().size()) return null;
        return n.choices().get(choiceIdx);
    }

    private DialogueAction action(int idx, boolean create) {
        DialogueChoice c = choice();
        if (c == null) return null;
        if (c.actions().size() <= idx) {
            if (!create) return null;
            while (c.actions().size() <= idx) c.actions().add(new DialogueAction());
        }
        return c.actions().get(idx);
    }

    private DialogueCondition condition(int idx, boolean create) {
        DialogueChoice c = choice();
        if (c == null) return null;
        if (c.conditions().size() <= idx) {
            if (!create) return null;
            while (c.conditions().size() <= idx) {
                c.conditions().add(new DialogueCondition(DialogueCondition.Type.NONE, "", 0));
            }
        }
        return c.conditions().get(idx);
    }

    private boolean actionSlotUsed(int idx) {
        DialogueAction a = action(idx, false);
        return a != null && a.type() != DialogueAction.Type.NONE;
    }

    private boolean condSlotUsed(int idx) {
        DialogueCondition c = condition(idx, false);
        return c != null && c.type() != DialogueCondition.Type.NONE;
    }

    private void refreshFields() {
        DialogueNode n = node();
        DialogueChoice c = choice();
        boolean nodeMode = (choiceIdx < 0);

        nodeTextBox.visible = nodeMode && n != null;
        if (nodeMode && n != null && !nodeTextBox.getValue().equals(n.text())) nodeTextBox.setValue(n.text());

        renameField.setVisible(nodeMode && n != null);
        if (nodeMode && n != null) renameField.setValue(n.id());

        choiceLabelField.setVisible(!nodeMode && c != null);
        if (!nodeMode && c != null) choiceLabelField.setValue(c.label());

        DialogueAction a = action(actionIdx, false);
        DialogueAction.Type at = a == null ? DialogueAction.Type.NONE : a.type();
        boolean valVisible = !nodeMode && c != null
                && (at == DialogueAction.Type.SAY_LINE || at == DialogueAction.Type.GIVE_ITEM
                || at == DialogueAction.Type.RUN_COMMAND
                || at == DialogueAction.Type.RUN_COMMAND_AS_PLAYER);
        boolean amtVisible = !nodeMode && c != null
                && (at == DialogueAction.Type.PAY_COINS || at == DialogueAction.Type.CHARGE_COINS
                || at == DialogueAction.Type.GIVE_ITEM);
        actionValueField.setVisible(valVisible);
        if (valVisible) actionValueField.setValue(a.value());
        actionAmountField.setVisible(amtVisible);
        if (amtVisible) actionAmountField.setValue(a.amount() > 0 ? Long.toString(a.amount()) : "");

        DialogueCondition cd = condition(condIdx, false);
        boolean condReal = cd != null && cd.type() != DialogueCondition.Type.NONE;
        boolean cvVisible = !nodeMode && c != null && condReal
                && (cd.type() == DialogueCondition.Type.HAS_ITEM
                || cd.type() == DialogueCondition.Type.IS_FACTION);
        boolean caVisible = !nodeMode && c != null && condReal
                && (cd.type() == DialogueCondition.Type.HAS_COINS || cd.type() == DialogueCondition.Type.HAS_ITEM);
        condValueField.setVisible(cvVisible);
        if (cvVisible) condValueField.setValue(cd.value());
        condAmountField.setVisible(caVisible);
        if (caVisible) condAmountField.setValue(cd.amount() > 0 ? Long.toString(cd.amount()) : "");
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderTransparentBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Dialogue Studio", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "Pages are what the NPC says. Choices are the buttons players click.",
                px + W / 2, py + 18, NotchTheme.TEXT_MUTED, false);

        drawPageList(ctx, mouseX, mouseY);
        if (choiceIdx < 0) {
            drawNodeEditor(ctx, mouseX, mouseY);
        } else {
            drawChoiceEditor(ctx, mouseX, mouseY);
        }

        // Bottom bar.
        NotchWidgets.primaryButton(ctx, this.font, px + ED_X, py + H - 24, 104, 16, "Save & Back",
                over(mouseX, mouseY, px + ED_X, py + H - 24, 104, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 108, py + H - 24, 78, 16, "Preview",
                !tree.isEmpty() && over(mouseX, mouseY, px + ED_X + 108, py + H - 24, 78, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 190, py + H - 24, 70, 16, "Discard",
                over(mouseX, mouseY, px + ED_X + 190, py + H - 24, 70, 16));

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawPageList(GuiGraphics ctx, int mx, int my) {
        ctx.drawString(this.font, "Pages:", px + LIST_X, py + 28, NotchTheme.TEXT_DARK, false);
        List<String> ids = new ArrayList<>(tree.nodes().keySet());
        int maxScroll = Math.max(0, ids.size() - LIST_ROWS);
        if (listScroll > maxScroll) listScroll = maxScroll;
        for (int i = 0; i < LIST_ROWS; i++) {
            int idx = listScroll + i;
            if (idx >= ids.size()) break;
            String id = ids.get(idx);
            int ry = py + LIST_Y + i * ROW_H;
            boolean hover = over(mx, my, px + LIST_X, ry, LIST_W, ROW_H - 1);
            if (id.equals(selectedId)) {
                NotchWidgets.primaryButton(ctx, this.font, px + LIST_X, ry, LIST_W, ROW_H - 1, pageLabel(id), hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + LIST_X, ry, LIST_W, ROW_H - 1, pageLabel(id), hover);
            }
        }
        if (tree.size() < MAX_NODES) {
            NotchWidgets.neutralButton(ctx, this.font, px + LIST_X, py + H - 42, LIST_W, 14, "+ Add Page",
                    over(mx, my, px + LIST_X, py + H - 42, LIST_W, 14));
        }
        NotchWidgets.centerText(ctx, this.font, tree.size() + "/" + MAX_NODES,
                px + LIST_X + LIST_W / 2, py + H - 24, NotchTheme.TEXT_MUTED, false);
    }

    private String pageLabel(String id) {
        String label = id.equals(tree.startId()) ? "[S] " + id : id;
        return this.font.width(label) <= LIST_W - 8 ? label
                : this.font.plainSubstrByWidth(label, LIST_W - 14) + "..";
    }

    private void drawNodeEditor(GuiGraphics ctx, int mx, int my) {
        DialogueNode n = node();
        if (n == null) {
            NotchWidgets.centerText(ctx, this.font, "Add a page to get started.",
                    px + ED_X + ED_W / 2, py + 90, NotchTheme.TEXT_MUTED, false);
            return;
        }
        boolean isStart = n.id().equals(tree.startId());

        // Header row: editable page id + rename, start marker, delete.
        ctx.drawString(this.font, "Id:", px + ED_X, py + 30,
                isStart ? NotchTheme.TEXT_GOLD : NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + ED_X + 26, py + 26, 94, 13, NotchTheme.DEEP);
        NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 124, py + 26, 50, 13, "Rename",
                over(mx, my, px + ED_X + 124, py + 26, 50, 13));
        if (!isStart) {
            NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 178, py + 26, 52, 13, "Start",
                    over(mx, my, px + ED_X + 178, py + 26, 52, 13));
        } else {
            NotchWidgets.centerText(ctx, this.font, "[start]", px + ED_X + 204, py + 29,
                    NotchTheme.TEXT_GOLD, false);
        }
        NotchWidgets.dangerButton(ctx, this.font, px + ED_X + 234, py + 26, 40, 13, "Del",
                over(mx, my, px + ED_X + 234, py + 26, 40, 13));

        // Says (the EditBoxWidget draws itself at py+54, h=56) + status/hint line.
        ctx.drawString(this.font, "Says:", px + ED_X, py + 44, NotchTheme.TEXT_DARK, false);
        if (!statusMsg.isEmpty() && System.currentTimeMillis() < statusUntil) {
            ctx.drawString(this.font, statusMsg, px + ED_X + 36, py + 44, NotchTheme.TEXT_RED, false);
        }
        ctx.drawString(this.font, "Choices:", px + ED_X, py + 118, NotchTheme.TEXT_DARK, false);
        ctx.drawString(this.font, "(click one to edit it)", px + ED_X + 50, py + 118, NotchTheme.TEXT_MUTED, false);
        List<DialogueChoice> choices = n.choices();
        for (int i = 0; i < choices.size(); i++) {
            int ry = py + 129 + i * 17;
            DialogueChoice c = choices.get(i);
            String label = c.label().isEmpty() ? "(unnamed)" : c.label();
            String target = c.next().isEmpty() ? "end" : c.next();
            String row = label + "  ->  " + target;
            if (this.font.width(row) > 220) {
                row = this.font.plainSubstrByWidth(row, 214) + "..";
            }
            NotchWidgets.neutralButton(ctx, this.font, px + ED_X, ry, 240, 15, row,
                    over(mx, my, px + ED_X, ry, 240, 15));
            NotchWidgets.dangerButton(ctx, this.font, px + ED_X + 246, ry, 16, 15, "x",
                    over(mx, my, px + ED_X + 246, ry, 16, 15));
        }
        if (choices.size() < MAX_CHOICES) {
            int ry = py + 129 + choices.size() * 17;
            NotchWidgets.neutralButton(ctx, this.font, px + ED_X, ry, 262, 15, "+ Add Choice",
                    over(mx, my, px + ED_X, ry, 262, 15));
        }
    }

    private void drawChoiceEditor(GuiGraphics ctx, int mx, int my) {
        DialogueChoice c = choice();
        if (c == null) {
            choiceIdx = -1;
            return;
        }
        ctx.drawString(this.font, "Editing a choice on '" + selectedId + "'", px + ED_X, py + 30, NotchTheme.TEXT_DARK, false);
        NotchWidgets.primaryButton(ctx, this.font, px + ED_X + 192, py + 26, 64, 13, "< Back",
                over(mx, my, px + ED_X + 192, py + 26, 64, 13));

        ctx.drawString(this.font, "Label:", px + ED_X, py + 58, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + ED_X + 36, py + 54, ED_W - 36, 14, NotchTheme.DEEP);

        String target = c.next().isEmpty() ? "(end conversation)" : c.next();
        ctx.drawString(this.font, "Leads to:", px + ED_X, py + 76, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 58, py + 72, 160, 14, target,
                over(mx, my, px + ED_X + 58, py + 72, 160, 14));

        DialogueAction a = action(actionIdx, false);
        DialogueAction.Type at = a == null ? DialogueAction.Type.NONE : a.type();
        ctx.drawString(this.font, "Action:", px + ED_X, py + 94, NotchTheme.TEXT_DARK, false);
        drawSlotTabs(ctx, px + ED_X + 40, py + 90, actionIdx, actionSlotUsed(0), actionSlotUsed(1), mx, my);
        NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 80, py + 90, 138, 14, actionName(at),
                over(mx, my, px + ED_X + 80, py + 90, 138, 14));
        if (at == DialogueAction.Type.OPEN_SCREEN && a != null) {
            ctx.drawString(this.font, "Screen:", px + ED_X, py + 112, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 58, py + 108, 160, 14,
                    screenDisplay(a.value()), over(mx, my, px + ED_X + 58, py + 108, 160, 14));
        }
        if (actionValueField.isVisible()) {
            String hint = switch (at) {
                case GIVE_ITEM -> "Item id:";
                case SAY_LINE -> "Says:";
                default -> "Command:";
            };
            ctx.drawString(this.font, hint, px + ED_X, py + 112, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + ED_X + 58, py + 106, ED_W - 60, 14, NotchTheme.DEEP);
        }
        if (actionAmountField.isVisible()) {
            ctx.drawString(this.font, "Amount:", px + ED_X, py + 130, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + ED_X + 58, py + 124, 100, 14, NotchTheme.DEEP);
        }

        DialogueCondition cd = condition(condIdx, false);
        String condName = (cd == null || cd.type() == DialogueCondition.Type.NONE) ? "None" : conditionName(cd.type());
        ctx.drawString(this.font, "Requires:", px + ED_X, py + 148, NotchTheme.TEXT_DARK, false);
        drawSlotTabs(ctx, px + ED_X + 52, py + 144, condIdx, condSlotUsed(0), condSlotUsed(1), mx, my);
        NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 92, py + 144, 126, 14, condName,
                over(mx, my, px + ED_X + 92, py + 144, 126, 14));
        if (condValueField.isVisible()) {
            ctx.drawString(this.font, "Item id:", px + ED_X, py + 166, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + ED_X + 58, py + 160, ED_W - 60, 14, NotchTheme.DEEP);
        }
        if (condAmountField.isVisible()) {
            ctx.drawString(this.font, "At least:", px + ED_X, py + 184, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + ED_X + 58, py + 178, 100, 14, NotchTheme.DEEP);
        }
        if (condSlotUsed(0) || condSlotUsed(1)) {
            String lockLabel = c.hideWhenLocked() ? "Locked: hidden" : "Locked: greyed";
            NotchWidgets.neutralButton(ctx, this.font, px + ED_X + 166, py + 178, 90, 14, lockLabel,
                    over(mx, my, px + ED_X + 166, py + 178, 90, 14));
        }
    }

    private static String actionName(DialogueAction.Type t) {
        return switch (t) {
            case NONE -> "None";
            case SAY_LINE -> "Say a line";
            case OPEN_ROLE -> "Open role screen";
            case OPEN_SCREEN -> "Open screen...";
            case PAY_COINS -> "Pay player coins";
            case CHARGE_COINS -> "Charge coins";
            case GIVE_ITEM -> "Give item";
            case RUN_COMMAND -> "Server command";
            case RUN_COMMAND_AS_PLAYER -> "Player command";
        };
    }

    private static final String[] SCREEN_IDS = {"BANKER", "AUCTIONEER", "MAILBOX", "RAFFLE", "BOUNTY", "DEALER", "ENCHANTER", "COSMETICS"};
    private static final String[] SCREEN_NAMES = {"Bank (ATM)", "Auction House", "Mailbox", "Raffle", "Bounty Board", "Slot Machine", "Enchanter", "Cosmetics"};

    private static String screenDisplay(String value) {
        for (int i = 0; i < SCREEN_IDS.length; i++) {
            if (SCREEN_IDS[i].equals(value)) return SCREEN_NAMES[i];
        }
        return SCREEN_NAMES[0];
    }

    private static String nextScreen(String value) {
        for (int i = 0; i < SCREEN_IDS.length; i++) {
            if (SCREEN_IDS[i].equals(value)) return SCREEN_IDS[(i + 1) % SCREEN_IDS.length];
        }
        return SCREEN_IDS[0];
    }

    private static String conditionName(DialogueCondition.Type t) {
        return switch (t) {
            case NONE -> "None";
            case HAS_COINS -> "Has coins";
            case HAS_ITEM -> "Has item";
            case IS_OWNER -> "Is owner";
            case IS_OP -> "Is op";
            case IS_FACTION -> "In faction";
        };
    }

    private void drawSlotTabs(GuiGraphics ctx, int x, int y, int selected, boolean used0, boolean used1,
                              int mx, int my) {
        for (int i = 0; i < 2; i++) {
            boolean used = i == 0 ? used0 : used1;
            String label = (i + 1) + (used ? "•" : "");
            boolean hover = over(mx, my, x + i * 19, y, 17, 14);
            if (i == selected) {
                NotchWidgets.primaryButton(ctx, this.font, x + i * 19, y, 17, 14, label, hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, x + i * 19, y, 17, 14, label, hover);
            }
        }
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;

            // Bottom bar.
            if (over(mx, my, px + ED_X, py + H - 24, 104, 16)) {
                NotchWidgets.click();
                normalize();
                NotchPacketsClient.sendNpcStudioSave(npcId, tree.toNbt());
                NotchPacketsClient.sendNpcEditorReopen(npcId, 3); // return to the NPC editor
                return true;
            }
            if (!tree.isEmpty() && over(mx, my, px + ED_X + 108, py + H - 24, 78, 16)) {
                NotchWidgets.click();
                // Play the local (possibly unsaved) tree from its start page; ESC returns here.
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new PreviewDialogueScreen(this, npcId, npcDisplayName(), tree, tree.startId()));
                return true;
            }
            if (over(mx, my, px + ED_X + 190, py + H - 24, 70, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 3); // discard + return to the NPC editor
                return true;
            }

            // Page list.
            List<String> ids = new ArrayList<>(tree.nodes().keySet());
            for (int i = 0; i < LIST_ROWS; i++) {
                int idx = listScroll + i;
                if (idx >= ids.size()) break;
                if (over(mx, my, px + LIST_X, py + LIST_Y + i * ROW_H, LIST_W, ROW_H - 1)) {
                    NotchWidgets.tick();
                    selectedId = ids.get(idx);
                    choiceIdx = -1;
                    refreshFields();
                    return true;
                }
            }
            if (tree.size() < MAX_NODES && over(mx, my, px + LIST_X, py + H - 42, LIST_W, 14)) {
                NotchWidgets.tick();
                addPage();
                return true;
            }

            if (choiceIdx < 0 ? clickNodeEditor(mx, my) : clickChoiceEditor(mx, my)) {
                NotchWidgets.tick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickNodeEditor(int mx, int my) {
        DialogueNode n = node();
        if (n == null) return false;
        boolean isStart = n.id().equals(tree.startId());
        if (over(mx, my, px + ED_X + 124, py + 26, 50, 13)) { // Rename
            renamePage(n.id(), renameField.getValue().trim());
            return true;
        }
        if (!isStart && over(mx, my, px + ED_X + 178, py + 26, 52, 13)) { // Start
            tree.setStartId(n.id());
            return true;
        }
        if (over(mx, my, px + ED_X + 234, py + 26, 40, 13)) { // Del
            deletePage(n.id());
            return true;
        }
        List<DialogueChoice> choices = n.choices();
        for (int i = 0; i < choices.size(); i++) {
            int ry = py + 129 + i * 17;
            if (over(mx, my, px + ED_X + 246, ry, 16, 15)) {
                choices.remove(i);
                refreshFields();
                return true;
            }
            if (over(mx, my, px + ED_X, ry, 240, 15)) {
                choiceIdx = i;
                actionIdx = 0;
                condIdx = 0;
                refreshFields();
                // Hand focus straight to the label so it's immediately editable.
                this.setFocused(choiceLabelField);
                //? if >=1.21 {
            /*choiceLabelField.moveCursorToEnd(false);
            *///?} else {
            choiceLabelField.moveCursorToEnd();
            //?}
                return true;
            }
        }
        if (choices.size() < MAX_CHOICES) {
            int ry = py + 129 + choices.size() * 17;
            if (over(mx, my, px + ED_X, ry, 262, 15)) {
                choices.add(new DialogueChoice("New choice", ""));
                return true;
            }
        }
        return false;
    }

    private void renamePage(String oldId, String newId) {
        if (newId.equals(oldId)) return;
        if (tree.renameNode(oldId, newId)) {
            selectedId = newId;
            statusMsg = "";
            refreshFields();
        } else {
            statusMsg = newId.isEmpty() ? "Id can't be empty." : "That id is taken.";
            statusUntil = System.currentTimeMillis() + 2500;
        }
    }

    private boolean clickChoiceEditor(int mx, int my) {
        DialogueChoice c = choice();
        if (c == null) return false;
        if (over(mx, my, px + ED_X + 192, py + 26, 64, 13)) { // Done
            choiceIdx = -1;
            refreshFields();
            return true;
        }
        // Clicking anywhere in the label box focuses the field (its own strip is thinner than
        // the drawn inset, which made label editing feel broken).
        if (over(mx, my, px + ED_X + 36, py + 54, ED_W - 36, 14)) {
            this.setFocused(choiceLabelField);
            //? if >=1.21 {
            /*choiceLabelField.moveCursorToEnd(false);
            *///?} else {
            choiceLabelField.moveCursorToEnd();
            //?}
            return true;
        }
        if (over(mx, my, px + ED_X + 58, py + 72, 160, 14)) { // Leads to (cycle)
            cycleNext(c);
            return true;
        }
        // Action slot tabs [1][2].
        for (int i = 0; i < 2; i++) {
            if (over(mx, my, px + ED_X + 40 + i * 19, py + 90, 17, 14)) {
                actionIdx = i;
                refreshFields();
                return true;
            }
        }
        if (over(mx, my, px + ED_X + 80, py + 90, 138, 14)) { // Action (cycle)
            cycleAction();
            refreshFields();
            return true;
        }
        DialogueAction a = action(actionIdx, false);
        if (a != null && a.type() == DialogueAction.Type.OPEN_SCREEN
                && over(mx, my, px + ED_X + 58, py + 108, 160, 14)) { // Screen (cycle)
            a.setValue(nextScreen(a.value()));
            return true;
        }
        // Condition slot tabs [1][2].
        for (int i = 0; i < 2; i++) {
            if (over(mx, my, px + ED_X + 52 + i * 19, py + 144, 17, 14)) {
                condIdx = i;
                refreshFields();
                return true;
            }
        }
        if (over(mx, my, px + ED_X + 92, py + 144, 126, 14)) { // Requires (cycle)
            cycleCondition();
            refreshFields();
            return true;
        }
        if (condSlotUsed(0) || condSlotUsed(1)) {
            if (over(mx, my, px + ED_X + 166, py + 178, 90, 14)) {
                c.setHideWhenLocked(!c.hideWhenLocked());
                return true;
            }
        }
        return false;
    }

    // ---- edit operations ----

    private String npcDisplayName() {
        var c = net.minecraft.client.Minecraft.getInstance();
        if (c.level != null) {
            for (var e : c.level.entitiesForRendering()) {
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity n
                        && n.getUUID().equals(npcId)) {
                    return n.hasCustomName() && n.getCustomName() != null
                            ? n.getCustomName().getString() : "NPC";
                }
            }
        }
        return "NPC";
    }

    private void normalize() {
        for (DialogueNode n : tree.nodes().values()) {
            for (DialogueChoice c : n.choices()) {
                c.actions().removeIf(a -> a.type() == DialogueAction.Type.NONE);
                c.conditions().removeIf(cd -> cd.type() == DialogueCondition.Type.NONE);
            }
        }
    }

    private void addPage() {
        int n = 1;
        while (tree.get("page_" + n) != null) n++;
        DialogueNode node = new DialogueNode("page_" + n);
        node.setText("Hello, %player%!");
        tree.put(node);
        selectedId = node.id();
        choiceIdx = -1;
        refreshFields();
    }

    private void deletePage(String id) {
        tree.remove(id);
        // De-link any choices that pointed at the deleted page.
        for (DialogueNode n : tree.nodes().values()) {
            for (DialogueChoice c : n.choices()) {
                if (c.next().equals(id)) c.setNext("");
            }
        }
        selectedId = tree.startId().isEmpty()
                ? (tree.nodes().isEmpty() ? "" : tree.nodes().keySet().iterator().next())
                : tree.startId();
        choiceIdx = -1;
        refreshFields();
    }

    private void cycleNext(DialogueChoice c) {
        List<String> options = new ArrayList<>();
        options.add(""); // end conversation
        options.addAll(tree.nodes().keySet());
        int idx = options.indexOf(c.next());
        c.setNext(options.get((idx + 1) % options.size()));
    }

    private static boolean adminActionsAllowed() {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && p.hasPermissions(2);
    }

    private void cycleAction() {
        DialogueAction.Type[] types = DialogueAction.Type.values();
        DialogueAction a = action(actionIdx, false);
        DialogueAction.Type current = a == null ? DialogueAction.Type.NONE : a.type();
        DialogueAction.Type next = types[(current.ordinal() + 1) % types.length];
        while (!adminActionsAllowed() && DialogueAction.isAdminOnly(next)) {
            next = types[(next.ordinal() + 1) % types.length];
        }
        DialogueAction updated = action(actionIdx, true);
        updated.setType(next); // NONE = an empty slot; stripped when saving
        // Entering OPEN_SCREEN: seed a valid screen id so the cycle starts somewhere real.
        if (next == DialogueAction.Type.OPEN_SCREEN && !isKnownScreen(updated.value())) {
            updated.setValue(SCREEN_IDS[0]);
        }
    }

    private static boolean isKnownScreen(String value) {
        for (String id : SCREEN_IDS) {
            if (id.equals(value)) return true;
        }
        return false;
    }

    private void cycleCondition() {
        DialogueCondition cd = condition(condIdx, true);
        DialogueCondition.Type[] types = DialogueCondition.Type.values();
        cd.setType(types[(cd.type().ordinal() + 1) % types.length]); // NONE = empty slot, stripped on save
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        if (mouseX >= px + LIST_X && mouseX < px + LIST_X + LIST_W) {
            int maxScroll = Math.max(0, tree.size() - LIST_ROWS);
            listScroll = Math.max(0, Math.min(maxScroll, listScroll - (int) Math.signum(amount)));
            return true;
        }
        //? if >=1.21 {
        /*return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        *///?} else {
        return super.mouseScrolled(mouseX, mouseY, amount);
        //?}
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Plain characters insert via charTyped only: forwarding them trips select-all (the
        // "typing 'a' wipes the line" bug). Edit/nav keys are forwarded by the guards.
        if (NotchWidgets.typingInEditBox(keyCode, scanCode, modifiers, nodeTextBox)) return true;
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, renameField, choiceLabelField,
                actionValueField, actionAmountField, condValueField, condAmountField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
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
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render(). This screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
