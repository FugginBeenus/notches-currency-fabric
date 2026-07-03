package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition;
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

/**
 * The visual dialogue studio: edit an NPC's whole branching conversation. Left pane lists the pages
 * (nodes); right pane edits the selected page's text and choices, or — one level deeper — a single
 * choice (label, where it leads, its action, its requirement, and lock behavior). Everything edits a
 * local copy of the tree; Save ships it back whole and the server re-validates ownership.
 */
public class DialogueStudioScreen extends Screen {

    private static final int W = 380, H = 226;
    // Left pane: page list.
    private static final int LIST_X = 8, LIST_Y = 40, LIST_W = 100, ROW_H = 14, LIST_ROWS = 9;
    // Right pane.
    private static final int ED_X = 116, ED_W = 256;
    private static final int MAX_NODES = 24, MAX_CHOICES = 5;

    private final UUID npcId;
    private final DialogueTree tree;

    private int px, py;
    private String selectedId = "";
    private int choiceIdx = -1; // -1 = editing the page; >=0 = editing that choice
    private int listScroll = 0;

    private TextFieldWidget nodeTextField;
    private TextFieldWidget choiceLabelField;
    private TextFieldWidget actionValueField, actionAmountField;
    private TextFieldWidget condValueField, condAmountField;

    public DialogueStudioScreen(UUID npcId, DialogueTree tree) {
        super(Text.literal("Dialogue Studio"));
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

        nodeTextField = field(px + ED_X + 38, py + 56, ED_W - 40, 300);
        nodeTextField.setChangedListener(s -> {
            DialogueNode n = node();
            if (n != null && choiceIdx < 0) n.setText(s);
        });

        choiceLabelField = field(px + ED_X + 38, py + 56, ED_W - 40, 48);
        choiceLabelField.setChangedListener(s -> {
            DialogueChoice c = choice();
            if (c != null) c.setLabel(s);
        });

        actionValueField = field(px + ED_X + 60, py + 108, ED_W - 62, 200);
        actionValueField.setChangedListener(s -> {
            DialogueAction a = action(false);
            if (a != null) a.setValue(s);
        });

        actionAmountField = field(px + ED_X + 60, py + 126, 96, 9);
        actionAmountField.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        actionAmountField.setChangedListener(s -> {
            DialogueAction a = action(false);
            if (a != null) a.setAmount(parse(s));
        });

        condValueField = field(px + ED_X + 60, py + 162, ED_W - 62, 200);
        condValueField.setChangedListener(s -> {
            DialogueCondition c = condition(false);
            if (c != null) c.setValue(s);
        });

        condAmountField = field(px + ED_X + 60, py + 180, 96, 9);
        condAmountField.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        condAmountField.setChangedListener(s -> {
            DialogueCondition c = condition(false);
            if (c != null) c.setAmount(parse(s));
        });

        refreshFields();
    }

    private TextFieldWidget field(int x, int y, int w, int maxLen) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x + 2, y + 3, w - 4, 9, Text.empty());
        f.setMaxLength(maxLen);
        f.setDrawsBackground(false);
        addDrawableChild(f);
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

    /** The choice's first action ({@code create} = add one if missing). */
    private DialogueAction action(boolean create) {
        DialogueChoice c = choice();
        if (c == null) return null;
        if (c.actions().isEmpty()) {
            if (!create) return null;
            c.actions().add(new DialogueAction());
        }
        return c.actions().get(0);
    }

    /** The choice's first condition ({@code create} = add one if missing). */
    private DialogueCondition condition(boolean create) {
        DialogueChoice c = choice();
        if (c == null) return null;
        if (c.conditions().isEmpty()) {
            if (!create) return null;
            c.conditions().add(new DialogueCondition());
        }
        return c.conditions().get(0);
    }

    /** Re-fill widgets from the model (call after changing selection/mode). Order matters: set the
     *  selection FIRST so setText's changed-listener writes back the same value harmlessly. */
    private void refreshFields() {
        DialogueNode n = node();
        DialogueChoice c = choice();
        boolean nodeMode = (choiceIdx < 0);

        nodeTextField.setVisible(nodeMode && n != null);
        if (nodeMode && n != null) nodeTextField.setText(n.text());

        choiceLabelField.setVisible(!nodeMode && c != null);
        if (!nodeMode && c != null) choiceLabelField.setText(c.label());

        DialogueAction a = action(false);
        DialogueAction.Type at = a == null ? DialogueAction.Type.NONE : a.type();
        boolean valVisible = !nodeMode && c != null
                && (at == DialogueAction.Type.GIVE_ITEM || at == DialogueAction.Type.RUN_COMMAND
                || at == DialogueAction.Type.RUN_COMMAND_AS_PLAYER);
        boolean amtVisible = !nodeMode && c != null
                && (at == DialogueAction.Type.PAY_COINS || at == DialogueAction.Type.CHARGE_COINS
                || at == DialogueAction.Type.GIVE_ITEM);
        actionValueField.setVisible(valVisible);
        if (valVisible) actionValueField.setText(a.value());
        actionAmountField.setVisible(amtVisible);
        if (amtVisible) actionAmountField.setText(a.amount() > 0 ? Long.toString(a.amount()) : "");

        DialogueCondition cd = condition(false);
        boolean cvVisible = !nodeMode && c != null && cd != null && cd.type() == DialogueCondition.Type.HAS_ITEM;
        boolean caVisible = !nodeMode && c != null && cd != null
                && (cd.type() == DialogueCondition.Type.HAS_COINS || cd.type() == DialogueCondition.Type.HAS_ITEM);
        condValueField.setVisible(cvVisible);
        if (cvVisible) condValueField.setText(cd.value());
        condAmountField.setVisible(caVisible);
        if (caVisible) condAmountField.setText(cd.amount() > 0 ? Long.toString(cd.amount()) : "");
    }

    // ---- rendering ----

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Dialogue Studio", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.textRenderer, "Pages are what the NPC says. Choices are the buttons players click.",
                px + W / 2, py + 18, NotchTheme.TEXT_MUTED, false);

        drawPageList(ctx, mouseX, mouseY);
        if (choiceIdx < 0) {
            drawNodeEditor(ctx, mouseX, mouseY);
        } else {
            drawChoiceEditor(ctx, mouseX, mouseY);
        }

        // Bottom bar.
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + ED_X, py + H - 24, 130, 16, "Save & Close",
                over(mouseX, mouseY, px + ED_X, py + H - 24, 130, 16));
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X + 138, py + H - 24, 90, 16, "Discard",
                over(mouseX, mouseY, px + ED_X + 138, py + H - 24, 90, 16));

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawPageList(DrawContext ctx, int mx, int my) {
        ctx.drawText(this.textRenderer, "Pages:", px + LIST_X, py + 28, NotchTheme.TEXT_DARK, false);
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
                NotchWidgets.primaryButton(ctx, this.textRenderer, px + LIST_X, ry, LIST_W, ROW_H - 1, pageLabel(id), hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.textRenderer, px + LIST_X, ry, LIST_W, ROW_H - 1, pageLabel(id), hover);
            }
        }
        if (tree.size() < MAX_NODES) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + LIST_X, py + H - 42, LIST_W, 14, "+ Add Page",
                    over(mx, my, px + LIST_X, py + H - 42, LIST_W, 14));
        }
        NotchWidgets.centerText(ctx, this.textRenderer, tree.size() + "/" + MAX_NODES,
                px + LIST_X + LIST_W / 2, py + H - 24, NotchTheme.TEXT_MUTED, false);
    }

    private String pageLabel(String id) {
        String label = id.equals(tree.startId()) ? "[S] " + id : id;
        return this.textRenderer.getWidth(label) <= LIST_W - 8 ? label
                : this.textRenderer.trimToWidth(label, LIST_W - 14) + "..";
    }

    private void drawNodeEditor(DrawContext ctx, int mx, int my) {
        DialogueNode n = node();
        if (n == null) {
            NotchWidgets.centerText(ctx, this.textRenderer, "Add a page to get started.",
                    px + ED_X + ED_W / 2, py + 90, NotchTheme.TEXT_MUTED, false);
            return;
        }
        boolean isStart = n.id().equals(tree.startId());
        ctx.drawText(this.textRenderer, "Page: " + n.id(), px + ED_X, py + 30,
                isStart ? NotchTheme.TEXT_GOLD : NotchTheme.TEXT_DARK, isStart);
        if (!isStart) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X + 126, py + 26, 64, 13, "Set Start",
                    over(mx, my, px + ED_X + 126, py + 26, 64, 13));
        }
        NotchWidgets.dangerButton(ctx, this.textRenderer, px + ED_X + 196, py + 26, 60, 13, "Delete",
                over(mx, my, px + ED_X + 196, py + 26, 60, 13));

        ctx.drawText(this.textRenderer, "Says:", px + ED_X, py + 58, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + ED_X + 36, py + 54, ED_W - 36, 14, NotchTheme.DEEP);
        ctx.drawText(this.textRenderer, "%player% / %npc% / %balance% fill in automatically",
                px + ED_X + 36, py + 70, NotchTheme.TEXT_MUTED, false);

        ctx.drawText(this.textRenderer, "Choices:", px + ED_X, py + 80, NotchTheme.TEXT_DARK, false);
        ctx.drawText(this.textRenderer, "(click one to edit it)", px + ED_X + 50, py + 80, NotchTheme.TEXT_MUTED, false);
        List<DialogueChoice> choices = n.choices();
        for (int i = 0; i < choices.size(); i++) {
            int ry = py + 90 + i * 17;
            DialogueChoice c = choices.get(i);
            String label = c.label().isEmpty() ? "(unnamed)" : c.label();
            String target = c.next().isEmpty() ? "end" : c.next();
            String row = label + "  ->  " + target;
            if (this.textRenderer.getWidth(row) > 200) {
                row = this.textRenderer.trimToWidth(row, 194) + "..";
            }
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X, ry, 220, 15, row,
                    over(mx, my, px + ED_X, ry, 220, 15));
            NotchWidgets.dangerButton(ctx, this.textRenderer, px + ED_X + 226, ry, 16, 15, "x",
                    over(mx, my, px + ED_X + 226, ry, 16, 15));
        }
        if (choices.size() < MAX_CHOICES) {
            int ry = py + 90 + choices.size() * 17;
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X, ry, 242, 15, "+ Add Choice",
                    over(mx, my, px + ED_X, ry, 242, 15));
        }
    }

    private void drawChoiceEditor(DrawContext ctx, int mx, int my) {
        DialogueChoice c = choice();
        if (c == null) {
            choiceIdx = -1;
            return;
        }
        ctx.drawText(this.textRenderer, "Editing a choice on '" + selectedId + "'", px + ED_X, py + 30, NotchTheme.TEXT_DARK, false);
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + ED_X + 192, py + 26, 64, 13, "< Back",
                over(mx, my, px + ED_X + 192, py + 26, 64, 13));

        ctx.drawText(this.textRenderer, "Label:", px + ED_X, py + 58, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + ED_X + 36, py + 54, ED_W - 36, 14, NotchTheme.DEEP);

        String target = c.next().isEmpty() ? "(end conversation)" : c.next();
        ctx.drawText(this.textRenderer, "Leads to:", px + ED_X, py + 76, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X + 58, py + 72, 160, 14, target,
                over(mx, my, px + ED_X + 58, py + 72, 160, 14));

        DialogueAction a = action(false);
        DialogueAction.Type at = a == null ? DialogueAction.Type.NONE : a.type();
        ctx.drawText(this.textRenderer, "Action:", px + ED_X, py + 94, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X + 58, py + 90, 160, 14, actionName(at),
                over(mx, my, px + ED_X + 58, py + 90, 160, 14));
        if (at == DialogueAction.Type.OPEN_SCREEN && a != null) {
            ctx.drawText(this.textRenderer, "Screen:", px + ED_X, py + 112, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X + 58, py + 108, 160, 14,
                    screenDisplay(a.value()), over(mx, my, px + ED_X + 58, py + 108, 160, 14));
        }
        if (actionValueField.isVisible()) {
            String hint = (at == DialogueAction.Type.GIVE_ITEM) ? "Item id:" : "Command:";
            ctx.drawText(this.textRenderer, hint, px + ED_X, py + 112, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + ED_X + 58, py + 106, ED_W - 60, 14, NotchTheme.DEEP);
        }
        if (actionAmountField.isVisible()) {
            ctx.drawText(this.textRenderer, "Amount:", px + ED_X, py + 130, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + ED_X + 58, py + 124, 100, 14, NotchTheme.DEEP);
        }

        DialogueCondition cd = condition(false);
        String condName = cd == null ? "None" : conditionName(cd.type());
        ctx.drawText(this.textRenderer, "Requires:", px + ED_X, py + 148, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X + 58, py + 144, 160, 14, condName,
                over(mx, my, px + ED_X + 58, py + 144, 160, 14));
        if (condValueField.isVisible()) {
            ctx.drawText(this.textRenderer, "Item id:", px + ED_X, py + 166, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + ED_X + 58, py + 160, ED_W - 60, 14, NotchTheme.DEEP);
        }
        if (condAmountField.isVisible()) {
            ctx.drawText(this.textRenderer, "At least:", px + ED_X, py + 184, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + ED_X + 58, py + 178, 100, 14, NotchTheme.DEEP);
        }
        if (cd != null) {
            String lockLabel = c.hideWhenLocked() ? "Locked: hidden" : "Locked: greyed";
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + ED_X + 166, py + 178, 90, 14, lockLabel,
                    over(mx, my, px + ED_X + 166, py + 178, 90, 14));
        }
    }

    private static String actionName(DialogueAction.Type t) {
        return switch (t) {
            case NONE -> "None";
            case OPEN_ROLE -> "Open role screen";
            case OPEN_SCREEN -> "Open screen...";
            case PAY_COINS -> "Pay player coins";
            case CHARGE_COINS -> "Charge coins";
            case GIVE_ITEM -> "Give item";
            case RUN_COMMAND -> "Server command";
            case RUN_COMMAND_AS_PLAYER -> "Player command";
        };
    }

    /** Screens an OPEN_SCREEN action can target (value = NpcRole name). */
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
            case HAS_COINS -> "Has coins";
            case HAS_ITEM -> "Has item";
            case IS_OWNER -> "Is owner";
            case IS_OP -> "Is op";
        };
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;

            // Bottom bar.
            if (over(mx, my, px + ED_X, py + H - 24, 130, 16)) {
                NotchPacketsClient.sendNpcStudioSave(npcId, tree.toNbt());
                this.close();
                return true;
            }
            if (over(mx, my, px + ED_X + 138, py + H - 24, 90, 16)) {
                this.close();
                return true;
            }

            // Page list.
            List<String> ids = new ArrayList<>(tree.nodes().keySet());
            for (int i = 0; i < LIST_ROWS; i++) {
                int idx = listScroll + i;
                if (idx >= ids.size()) break;
                if (over(mx, my, px + LIST_X, py + LIST_Y + i * ROW_H, LIST_W, ROW_H - 1)) {
                    selectedId = ids.get(idx);
                    choiceIdx = -1;
                    refreshFields();
                    return true;
                }
            }
            if (tree.size() < MAX_NODES && over(mx, my, px + LIST_X, py + H - 42, LIST_W, 14)) {
                addPage();
                return true;
            }

            if (choiceIdx < 0 ? clickNodeEditor(mx, my) : clickChoiceEditor(mx, my)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickNodeEditor(int mx, int my) {
        DialogueNode n = node();
        if (n == null) return false;
        boolean isStart = n.id().equals(tree.startId());
        if (!isStart && over(mx, my, px + ED_X + 126, py + 26, 64, 13)) {
            tree.setStartId(n.id());
            return true;
        }
        if (over(mx, my, px + ED_X + 196, py + 26, 60, 13)) {
            deletePage(n.id());
            return true;
        }
        List<DialogueChoice> choices = n.choices();
        for (int i = 0; i < choices.size(); i++) {
            int ry = py + 90 + i * 17;
            if (over(mx, my, px + ED_X + 226, ry, 16, 15)) {
                choices.remove(i);
                refreshFields();
                return true;
            }
            if (over(mx, my, px + ED_X, ry, 220, 15)) {
                choiceIdx = i;
                refreshFields();
                return true;
            }
        }
        if (choices.size() < MAX_CHOICES) {
            int ry = py + 90 + choices.size() * 17;
            if (over(mx, my, px + ED_X, ry, 242, 15)) {
                choices.add(new DialogueChoice("New choice", ""));
                return true;
            }
        }
        return false;
    }

    private boolean clickChoiceEditor(int mx, int my) {
        DialogueChoice c = choice();
        if (c == null) return false;
        if (over(mx, my, px + ED_X + 192, py + 26, 64, 13)) { // Done
            choiceIdx = -1;
            refreshFields();
            return true;
        }
        if (over(mx, my, px + ED_X + 58, py + 72, 160, 14)) { // Leads to (cycle)
            cycleNext(c);
            return true;
        }
        if (over(mx, my, px + ED_X + 58, py + 90, 160, 14)) { // Action (cycle)
            cycleAction(c);
            refreshFields();
            return true;
        }
        DialogueAction a = action(false);
        if (a != null && a.type() == DialogueAction.Type.OPEN_SCREEN
                && over(mx, my, px + ED_X + 58, py + 108, 160, 14)) { // Screen (cycle)
            a.setValue(nextScreen(a.value()));
            return true;
        }
        if (over(mx, my, px + ED_X + 58, py + 144, 160, 14)) { // Requires (cycle)
            cycleCondition(c);
            refreshFields();
            return true;
        }
        if (condition(false) != null && over(mx, my, px + ED_X + 166, py + 178, 90, 14)) {
            c.setHideWhenLocked(!c.hideWhenLocked());
            return true;
        }
        return false;
    }

    // ---- edit operations ----

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

    private void cycleAction(DialogueChoice c) {
        DialogueAction.Type[] types = DialogueAction.Type.values();
        DialogueAction a = action(false);
        DialogueAction.Type current = a == null ? DialogueAction.Type.NONE : a.type();
        DialogueAction.Type next = types[(current.ordinal() + 1) % types.length];
        if (next == DialogueAction.Type.NONE) {
            c.actions().clear();
        } else {
            DialogueAction updated = action(true);
            updated.setType(next);
            // Entering OPEN_SCREEN: seed a valid screen id so the cycle starts somewhere real.
            if (next == DialogueAction.Type.OPEN_SCREEN && !isKnownScreen(updated.value())) {
                updated.setValue(SCREEN_IDS[0]);
            }
        }
    }

    private static boolean isKnownScreen(String value) {
        for (String id : SCREEN_IDS) {
            if (id.equals(value)) return true;
        }
        return false;
    }

    private void cycleCondition(DialogueChoice c) {
        DialogueCondition cd = condition(false);
        if (cd == null) {
            condition(true).setType(DialogueCondition.Type.HAS_COINS);
            return;
        }
        DialogueCondition.Type[] types = DialogueCondition.Type.values();
        int next = cd.type().ordinal() + 1;
        if (next >= types.length) {
            c.conditions().clear(); // wraps back to "None"
        } else {
            cd.setType(types[next]);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= px + LIST_X && mouseX < px + LIST_X + LIST_W) {
            int maxScroll = Math.max(0, tree.size() - LIST_ROWS);
            listScroll = Math.max(0, Math.min(maxScroll, listScroll - (int) Math.signum(amount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
