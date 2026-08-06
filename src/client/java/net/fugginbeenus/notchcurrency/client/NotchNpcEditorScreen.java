package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.client.npc.NpcAppearances;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Owner-only Notch NPC editor. The Appearance tab shows a live preview of the actual NPC (so edits are
 * reflected instantly) with a model selector (Humanoid default; APP.ly if installed), skin controls
 * per model (humanoid preset cycling + slim; APP.ly themed variants), and a size control. Code-drawn
 * with {@link NotchWidgets}; edits go to the server, which re-validates ownership.
 */
public class NotchNpcEditorScreen extends Screen {

    // Taller than it needs to be for most tabs: Moves carries three rows of toggles now, and every
    // tab draws from the top down, so the extra height is just breathing room everywhere else.
    private static final int W = 300, H = 282;
    // GREETER retired: NONE + dialogue does the same job now (any NPC can talk).
    private static final NpcRole[] SELECTABLE = {
            NpcRole.NONE, NpcRole.SHOP, NpcRole.BANKER, NpcRole.AUCTIONEER,
            NpcRole.MAILBOX, NpcRole.RAFFLE, NpcRole.BOUNTY, NpcRole.DEALER, NpcRole.ENCHANTER,
            NpcRole.COSMETICS, NpcRole.RECRUITER
    };

    private final UUID npcId;
    /** Set during a draw when the cursor is over something worth explaining, and painted last so
     *  it lands above the panel. Hints that would be clipped to "..." live here instead. */
    @org.jetbrains.annotations.Nullable private java.util.List<Text> tooltip = null;
    private final String ownerName;
    private final boolean canEdit;
    private final boolean applyInstalled = FabricLoader.getInstance().isModLoaded("apply");

    private NpcRole currentRole;
    private NpcRole pendingRole = null; // set while confirming a switch away from the SHOP role
    private String currentName;
    private String currentModel, currentSkinType, currentSkinValue;
    private boolean currentSlim;
    private float currentScale, currentScaleY, currentScaleZ, currentNameOffset;
    private NotchNpcEntity.Behavior currentBehavior;
    private int currentRadius;
    private int dialogueNodes;
    private boolean dialogueFlat; // flat = Quick Lines shape; branching = Studio territory
    private int statsBits; // toggle bitmask, see NotchPackets.NPC_SET_STATS
    private int maxHealth, speedPct, regen; // slider attributes for the stats screen
    private String followName; // blank = follow the owner
    private int movesBits; // 1=avoid monsters 2=watch players
    private int dialogueMode; // 0=window 1=chat
    private int waypointCount;
    private int patrolSpeedIdx; // 0 stroll / 1 walk / 2 jog
    private int patrolWaitIdx;  // dwell at each waypoint: 0 none / 1 2s / 2 5s / 3 10s / 4 20s
    private int poseId; // 0 stand / 1 sit / 2 sneak / 3 sleep
    private int poseAnim; // idle animation: 0 statue / 1 breathe (vanilla default) / 2 lively
    private int tab = 0; // 0 Look, 1 Behavior, 2 Role, 3 Talk, 4 Manage

    private int px, py;
    private TextFieldWidget nameField;
    private TextFieldWidget subtitleField;
    /** Personality, held here between the packet arriving and the widgets being built. */
    private String currentSubtitle = "";
    private String currentVoice = "";
    private int currentVoicePitch = 100;
    private TextFieldWidget playerField;
    private TextFieldWidget urlField;
    private TextFieldWidget followField;
    private TextFieldWidget farewellField;
    private String currentFarewell;
    private String currentBillboard;
    private NotchNpcEntity preview;

    /** Which tab the NEXT editor open should land on. Sub-screens (pose editor, studio, stats…) set
     *  this to their home tab right before sending the reopen packet, so Back returns you to where
     *  you came from instead of the first tab. Consumed (reset to 0) on construction. */
    public static int reopenAtTab = 0;

    public NotchNpcEditorScreen(net.fugginbeenus.notchcurrency.client.npc.NpcEditorState state) {
        super(Text.literal("NPC Editor"));
        this.tab = Math.max(0, Math.min(5, reopenAtTab));
        reopenAtTab = 0;
        this.npcId = state.npcId();
        this.currentRole = roleFromOrdinal(state.roleOrdinal());
        this.currentName = state.name() == null ? "" : state.name();
        this.currentFarewell = state.farewell() == null ? "" : state.farewell();
        this.currentBillboard = state.billboard() == null ? "" : state.billboard();
        this.currentSubtitle = state.subtitle() == null ? "" : state.subtitle();
        this.currentVoice = state.voice() == null ? "" : state.voice();
        this.currentVoicePitch = state.voicePitch();
        this.ownerName = state.ownerName() == null ? "" : state.ownerName();
        this.canEdit = state.canEdit();
        this.currentModel = (state.model() == null || state.model().isEmpty())
                ? NotchNpcEntity.MODEL_HUMANOID : state.model();
        this.currentSkinType = (state.skinType() == null || state.skinType().isEmpty())
                ? NotchNpcEntity.SKIN_PRESET : state.skinType();
        this.currentSkinValue = state.skinValue() == null ? "" : state.skinValue();
        this.currentSlim = state.slim();
        this.currentScale = state.scale() <= 0 ? 1.0f : state.scale();
        this.currentScaleY = state.scaleY() <= 0 ? 1.0f : state.scaleY();
        this.currentScaleZ = state.scaleZ() <= 0 ? 1.0f : state.scaleZ();
        this.currentNameOffset = state.nameOffset();
        NotchNpcEntity.Behavior[] modes = NotchNpcEntity.Behavior.values();
        this.currentBehavior = (state.behaviorOrdinal() >= 0 && state.behaviorOrdinal() < modes.length)
                ? modes[state.behaviorOrdinal()] : NotchNpcEntity.Behavior.STATIONARY;
        this.currentRadius = state.wanderRadius() <= 0 ? 8 : state.wanderRadius();
        this.dialogueNodes = state.dialogueNodes();
        this.dialogueFlat = state.dialogueFlat();
        this.statsBits = state.statsBits();
        this.maxHealth = state.maxHealth() <= 0 ? 20 : state.maxHealth();
        this.speedPct = state.speedPct() <= 0 ? 30 : state.speedPct();
        this.regen = state.regen();
        this.followName = state.followName() == null ? "" : state.followName();
        this.movesBits = state.movesBits();
        this.dialogueMode = state.dialogueMode();
        this.waypointCount = state.waypointCount();
        this.patrolSpeedIdx = state.patrolSpeedIdx();
        this.patrolWaitIdx = state.patrolWaitIdx();
        this.poseId = state.poseId();
        this.poseAnim = state.poseAnim();
    }

    private static NpcRole roleFromOrdinal(int ord) {
        NpcRole[] all = NpcRole.values();
        return (ord >= 0 && ord < all.length) ? all[ord] : NpcRole.NONE;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        nameField = new TextFieldWidget(this.textRenderer, px + 152, py + 49, 136, 9, Text.literal("Name"));
        nameField.setMaxLength(48);
        nameField.setDrawsBackground(false);
        nameField.setText(currentName);
        addDrawableChild(nameField);

        subtitleField = new TextFieldWidget(this.textRenderer, px + 154, py + 219, 98, 9, Text.literal("Subtitle"));
        subtitleField.setMaxLength(NotchNpcEntity.MAX_SUBTITLE_LENGTH);
        subtitleField.setDrawsBackground(false);
        subtitleField.setText(currentSubtitle);
        addDrawableChild(subtitleField);

        farewellField = new TextFieldWidget(this.textRenderer, px + 78, py + 175, 158, 9, Text.literal("Goodbye"));
        farewellField.setMaxLength(150);
        farewellField.setDrawsBackground(false);
        farewellField.setPlaceholder(Text.literal("(optional)").formatted(Formatting.DARK_GRAY));
        farewellField.setText(currentFarewell);
        addDrawableChild(farewellField);

        playerField = new TextFieldWidget(this.textRenderer, px + 152, py + 159, 134, 9, Text.literal("Player"));
        playerField.setMaxLength(16);
        playerField.setDrawsBackground(false);
        playerField.setPlaceholder(Text.literal("player name").formatted(Formatting.DARK_GRAY));
        if (NotchNpcEntity.SKIN_PLAYER.equals(currentSkinType)) playerField.setText(currentSkinValue);
        addDrawableChild(playerField);

        urlField = new TextFieldWidget(this.textRenderer, px + 152, py + 177, 134, 9, Text.literal("URL"));
        urlField.setMaxLength(256);
        urlField.setDrawsBackground(false);
        urlField.setPlaceholder(Text.literal("skin URL").formatted(Formatting.DARK_GRAY));
        if (NotchNpcEntity.SKIN_URL.equals(currentSkinType)) urlField.setText(currentSkinValue);
        addDrawableChild(urlField);

        followField = new TextFieldWidget(this.textRenderer, px + BEH_X + 52, py + 143, 104, 9, Text.literal("Follow"));
        followField.setMaxLength(16);
        followField.setDrawsBackground(false);
        followField.setPlaceholder(Text.literal("owner").formatted(Formatting.DARK_GRAY));
        followField.setText(followName);
        addDrawableChild(followField);

        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        nameField.visible = (tab == 0);
        subtitleField.visible = (tab == 0);
        farewellField.visible = (tab == 3);
        nameField.setFocusUnlocked(tab == 0);
        subtitleField.setFocusUnlocked(tab == 0);
        boolean hum = (tab == 0 && isHumanoid());
        playerField.visible = hum;
        urlField.visible = hum;
        followField.visible = (tab == 1 && currentBehavior == NotchNpcEntity.Behavior.FOLLOW_OWNER);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderInGameBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        tooltip = null;
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "NPC Editor", px + W / 2, py + 8);
        drawTabs(ctx, mouseX, mouseY);
        switch (tab) {
            case 0 -> drawAppearance(ctx, mouseX, mouseY);
            case 1 -> drawBehavior(ctx, mouseX, mouseY);
            case 2 -> drawRole(ctx, mouseX, mouseY);
            case 3 -> drawTalk(ctx, mouseX, mouseY);
            case 4 -> drawPose(ctx, mouseX, mouseY);
            case 5 -> drawManage(ctx, mouseX, mouseY);
        }
        super.render(ctx, mouseX, mouseY, delta);
        if (tooltip != null) {
            ctx.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    // ---- tabs ----

    private static final int TAB_Y = 22, TAB_H = 16, TAB_W = 44;
    private int tabX(int i) { return px + 9 + i * 47; }

    private void drawTabs(DrawContext ctx, int mx, int my) {
        String[] names = {"Look", "Moves", "Role", "Talk", "Pose", "Manage"};
        for (int i = 0; i < 6; i++) {
            int tx = tabX(i);
            boolean hover = over(mx, my, tx, py + TAB_Y, TAB_W, TAB_H);
            if (i == tab) NotchWidgets.primaryButton(ctx, this.textRenderer, tx, py + TAB_Y, TAB_W, TAB_H, names[i], hover);
            else NotchWidgets.neutralButton(ctx, this.textRenderer, tx, py + TAB_Y, TAB_W, TAB_H, names[i], hover);
        }
        NotchWidgets.divider(ctx, px + 8, py + 42, W - 16);
    }

    // ---- appearance tab ----

    private static final int PREV_X = 8, PREV_Y = 46, PREV_W = 100, PREV_H = 176;
    private static final int RX = 116;
    private boolean isApplyModel() { return NotchNpcEntity.MODEL_APPLY.equals(currentModel); }

    private void drawAppearance(DrawContext ctx, int mx, int my) {
        // Live preview of the actual NPC.
        NotchWidgets.inset(ctx, px + PREV_X, py + PREV_Y, PREV_W, PREV_H, NotchTheme.DEEP);
        NotchNpcEntity npc = findPreview();
        if (npc != null) {
            float oldYaw = npc.getYaw(), oldBody = npc.bodyYaw;
            boolean wasInvisible = npc.isInvisible();
            npc.setYaw(180);
            npc.bodyYaw = 180;
            npc.setInvisible(false); // always show the NPC in its own editor preview
            float lookX = (px + PREV_X + PREV_W / 2f) - mx;
            float lookY = (py + PREV_Y + 40f) - my;
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx, px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H - 18, previewSize(),
                    lookX, lookY, npc);
            npc.setYaw(oldYaw);
            npc.bodyYaw = oldBody;
            npc.setInvisible(wasInvisible);
        } else {
            NotchWidgets.centerText(ctx, this.textRenderer, "Preview", px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H / 2, NotchTheme.TEXT_MUTED, false);
        }

        // Name
        ctx.drawText(this.textRenderer, "Name:", px + RX, py + 50, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + 150, py + 46, 140, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 150, py + 64, 140, 14, "Save Name",
                over(mx, my, px + 150, py + 64, 140, 14));
        if (over(mx, my, px + 150, py + 46, 140, 14)) {
            tooltip = java.util.List.of(
                    Text.literal("Name").formatted(Formatting.WHITE),
                    Text.literal("Colour it with & codes: &6Carol").formatted(Formatting.GRAY),
                    Text.literal("Same codes as dialogue and signs.").formatted(Formatting.DARK_GRAY));
        }



        // Where the floating name sits: models vary enough that one height never fits them all.
        drawNameOffsetRow(ctx, mx, my);
        drawTitleRow(ctx, mx, my);
        drawSignButton(ctx, mx, my);

        // Model: current name + Change button (opens the vanilla/modded model picker)
        ctx.drawText(this.textRenderer, "Model:", px + RX, py + 104, NotchTheme.TEXT_DARK, false);
        ctx.drawText(this.textRenderer, trim(modelDisplayName(currentModel), 15), px + 150, py + 104, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 224, py + 100, 66, 14, "Change...", over(mx, my, px + 224, py + 100, 66, 14));

        // Skin controls per model
        if (isApplyModel()) {
            var variants = NpcAppearances.all();
            for (int i = 0; i < variants.size(); i++) {
                var v = variants.get(i);
                int vx = varX(i), vy = varY(i);
                boolean hover = over(mx, my, vx, vy, VAR_W, VAR_H);
                if (v.id().equals(currentSkinValue)) NotchWidgets.primaryButton(ctx, this.textRenderer, vx, vy, VAR_W, VAR_H, v.displayName(), hover);
                else NotchWidgets.neutralButton(ctx, this.textRenderer, vx, vy, VAR_W, VAR_H, v.displayName(), hover);
            }
        } else if (isHumanoid()) {
            boolean preset = NotchNpcEntity.SKIN_PRESET.equals(currentSkinType);
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + 118, py + 122, 20, 16, "<", over(mx, my, px + 118, py + 122, 20, 16));
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + 268, py + 122, 20, 16, ">", over(mx, my, px + 268, py + 122, 20, 16));
            NotchWidgets.centerText(ctx, this.textRenderer, "Preset " + presetIndex(), px + 203, py + 126,
                    preset ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + 118, py + 140, 174, 14,
                    currentSlim ? "Arms: Slim" : "Arms: Wide", over(mx, my, px + 118, py + 140, 174, 14));
            // Player-name skin (checkbox enables)
            drawCheck(ctx, px + 118, py + 157, NotchNpcEntity.SKIN_PLAYER.equals(currentSkinType));
            NotchWidgets.inset(ctx, px + 150, py + 156, 138, 13, NotchTheme.DEEP);
            // Custom URL skin (checkbox enables)
            drawCheck(ctx, px + 118, py + 175, NotchNpcEntity.SKIN_URL.equals(currentSkinType));
            NotchWidgets.inset(ctx, px + 150, py + 174, 138, 13, NotchTheme.DEEP);
        } else {
            NotchWidgets.centerText(ctx, this.textRenderer, "This mob uses its own look.", px + 203, py + 134, NotchTheme.TEXT_MUTED, false);
        }

    }

    // The nameplate nudge sits right under the name it moves. Size isn't here. It needs to be watched
    // in the world, so it lives on the Move, Rotate & Size panel.
    private static final int NAME_Y_ROW = 82;
    private static final int STEP_W = 18, STEP_H = 14;
    private static final int NAME_Y_MINUS_X = 196, NAME_Y_PLUS_X = 272;

    // The floating sign sits under the skin controls, in the right-hand column: the left side of
    // the tab is the preview panel, and a full-width button would cut across it.
    private int signRow() { return py + 196; }
    private int signWidth() { return W - RX - 10; }

    /** Row constants for the title, which sits with the floating sign: both are text on the NPC. */
    private int titleRow() { return py + 216; }

    private void drawTitleRow(DrawContext ctx, int mx, int my) {
        int y = titleRow();
        ctx.drawText(this.textRenderer, "Title:", px + RX, y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + 150, y, 106, 13, NotchTheme.DEEP);
        if (subtitleField.getText().isEmpty()) {
            ctx.drawText(this.textRenderer, "Blacksmith", px + 154, y + 3, 0xFF555555, false);
        }
        boolean setHover = over(mx, my, px + 260, y, 30, 13);
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 260, y, 30, 13, "Set", setHover);
        if (setHover || over(mx, my, px + 150, y, 106, 13)) {
            tooltip = java.util.List.of(
                    Text.literal("Title").formatted(Formatting.WHITE),
                    Text.literal("A small line under the name.").formatted(Formatting.GRAY),
                    Text.literal("Takes & colour codes. Shows only when").formatted(Formatting.GRAY),
                    Text.literal("the nameplate is on.").formatted(Formatting.DARK_GRAY));
        }
    }

    private void drawSignButton(DrawContext ctx, int mx, int my) {
        int y = signRow();
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + RX, y, signWidth(), 15,
                currentBillboard.isBlank() ? "Add a floating sign..." : "Edit floating sign...",
                over(mx, my, px + RX, y, signWidth(), 15));
    }

    private void drawNameOffsetRow(DrawContext ctx, int mx, int my) {
        int y = py + NAME_Y_ROW;
        ctx.drawText(this.textRenderer, "Name Y:", px + RX, y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + NAME_Y_MINUS_X, y, STEP_W, STEP_H, "-",
                over(mx, my, px + NAME_Y_MINUS_X, y, STEP_W, STEP_H));
        NotchWidgets.centerText(ctx, this.textRenderer, String.format("%+.1f", currentNameOffset),
                px + 245, y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + NAME_Y_PLUS_X, y, STEP_W, STEP_H, "+",
                over(mx, my, px + NAME_Y_PLUS_X, y, STEP_W, STEP_H));
    }

    private boolean isHumanoid() { return NotchNpcEntity.MODEL_HUMANOID.equals(currentModel); }

    /** Fit the preview to the current model's size so big disguises don't overflow the panel. */
    private int previewSize() {
        // Fit the model to the preview box using both dimensions. In drawEntity, rendered pixels ≈
        // size × blocks, so size = boxPixels / blocks; take the tighter of width/height so nothing
        // clips, and leave a margin (esp. at the top for the nameplate).
        float h = 1.9f, w = 0.6f; // humanoid default
        if (currentModel != null && currentModel.startsWith("entity:")) {
            EntityType<?> t = Registries.ENTITY_TYPE.get(Reg.parse(currentModel.substring("entity:".length())));
            if (t != null) {
                //? if >=1.21 {
                /*h = Math.max(0.5f, t.getDimensions().height());
                w = Math.max(0.5f, t.getDimensions().width());
                *///?} else {
                h = Math.max(0.5f, t.getDimensions().height);
                w = Math.max(0.5f, t.getDimensions().width);
                //?}
            }
        }
        float fitH = (PREV_H - 34) / h;
        float fitW = (PREV_W - 20) / w;
        // 0.75 = a touch smaller than a full fit, so the model sits comfortably inside the box.
        return (int) Math.max(8, Math.min(68, Math.min(fitH, fitW) * 0.75f));
    }

    private static String modelDisplayName(String model) {
        if (NotchNpcEntity.MODEL_HUMANOID.equals(model)) return "Humanoid";
        if (NotchNpcEntity.MODEL_APPLY.equals(model)) return "APP.ly";
        if (model != null && model.startsWith("entity:")) {
            EntityType<?> t = Registries.ENTITY_TYPE.get(Reg.parse(model.substring("entity:".length())));
            return t != null ? t.getName().getString() : model.substring("entity:".length());
        }
        return model == null ? "Humanoid" : model;
    }

    private String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "..";
    }

    private static final int VAR_W = 84, VAR_H = 16;
    private int varX(int i) { return (i % 2 == 0) ? px + 118 : px + 206; }
    private int varY(int i) { return py + 106 + (i / 2) * 18; }

    private int presetIndex() {
        try { return Math.max(1, Math.min(NpcSkinsPresetCount(), Integer.parseInt(currentSkinValue))); }
        catch (NumberFormatException e) { return 1; }
    }
    private int NpcSkinsPresetCount() { return 12; }

    // ---- behavior tab ----

    private static final int BEH_X = 50, BEH_W = 200, BEH_H = 15;
    // The two always-on toggle rows, clear of the patrol row that ends just above them.
    private static final int TOGGLE_W = 98, TOGGLE_H = 15;
    private int movesRow1() { return py + 190; }
    private int movesRow2() { return py + 207; }
    private int movesRow3() { return py + 224; }
    private int movesRow4() { return py + 241; }

    /** One always-on toggle: green when on, plain when off. */
    private void drawToggle(DrawContext ctx, int mx, int my, int x, int y, String label, boolean on) {
        boolean hover = over(mx, my, x, y, TOGGLE_W, TOGGLE_H);
        if (on) NotchWidgets.primaryButton(ctx, this.textRenderer, x, y, TOGGLE_W, TOGGLE_H, label, hover);
        else NotchWidgets.neutralButton(ctx, this.textRenderer, x, y, TOGGLE_W, TOGGLE_H, label, hover);
    }
    private int behY(int i) { return py + 48 + i * 17; }

    private boolean usesRadius() {
        return currentBehavior == NotchNpcEntity.Behavior.WANDER || currentBehavior == NotchNpcEntity.Behavior.GUARD;
    }

    private void drawBehavior(DrawContext ctx, int mx, int my) {
        String[] labels = {"Stationary", "Wander around home", "Follow owner", "Patrol waypoints", "Guard this area"};
        NotchNpcEntity.Behavior[] modes = NotchNpcEntity.Behavior.values();
        for (int i = 0; i < modes.length; i++) {
            boolean hover = over(mx, my, px + BEH_X, behY(i), BEH_W, BEH_H);
            if (modes[i] == currentBehavior) {
                NotchWidgets.primaryButton(ctx, this.textRenderer, px + BEH_X, behY(i), BEH_W, BEH_H, labels[i], hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.textRenderer, px + BEH_X, behY(i), BEH_W, BEH_H, labels[i], hover);
            }
        }

        // Sixth in the list rather than a chip among the toggles: choosing a schedule is choosing
        // how the NPC moves, which is what every other button in this column is for.
        boolean schedHover = over(mx, my, px + BEH_X, behY(5), BEH_W, BEH_H);
        NotchWidgets.goldButton(ctx, this.textRenderer, px + BEH_X, behY(5), BEH_W, BEH_H,
                "Daily Schedule", schedHover);
        if (schedHover) {
            tooltip = java.util.List.of(
                    Text.literal("Daily Schedule").formatted(Formatting.GOLD),
                    Text.literal("Sleep, open the shop, wander, walk a round,").formatted(Formatting.GRAY),
                    Text.literal("all on a clock. Overrides the choice above").formatted(Formatting.GRAY),
                    Text.literal("while it is running.").formatted(Formatting.GRAY));
        }

        int cy = py + 152;
        if (usesRadius()) {
            ctx.drawText(this.textRenderer, "Radius:", px + BEH_X, cy + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + BEH_X + 50, cy, 20, 16, "-",
                    over(mx, my, px + BEH_X + 50, cy, 20, 16));
            NotchWidgets.centerText(ctx, this.textRenderer, currentRadius + " blocks", px + BEH_X + 105, cy + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + BEH_X + 140, cy, 20, 16, "+",
                    over(mx, my, px + BEH_X + 140, cy, 20, 16));
        } else if (currentBehavior == NotchNpcEntity.Behavior.FOLLOW_OWNER) {
            ctx.drawText(this.textRenderer, "Follow:", px + BEH_X, cy + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + BEH_X + 48, cy, 112, 14, NotchTheme.DEEP);
            NotchWidgets.primaryButton(ctx, this.textRenderer, px + BEH_X + 166, cy, 34, 14, "Set",
                    over(mx, my, px + BEH_X + 166, cy, 34, 14));
        } else if (currentBehavior == NotchNpcEntity.Behavior.PATROL) {
            ctx.drawText(this.textRenderer, "Points: " + waypointCount, px + BEH_X, cy + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.primaryButton(ctx, this.textRenderer, px + BEH_X + 56, cy, 84, 16, "Route tool",
                    over(mx, my, px + BEH_X + 56, cy, 84, 16));
            NotchWidgets.dangerButton(ctx, this.textRenderer, px + BEH_X + 146, cy, 54, 16, "Clear",
                    over(mx, my, px + BEH_X + 146, cy, 54, 16));

            // Speed + waypoint dwell time cycle buttons, then Done.
            int cy2 = cy + 18;
            String[] speeds = {"Stroll", "Walk", "Jog"};
            String[] waits = {"Wait: none", "Wait 2s", "Wait 5s", "Wait 10s", "Wait 20s"};
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + BEH_X, cy2, 62, 16,
                    speeds[Math.max(0, Math.min(speeds.length - 1, patrolSpeedIdx))],
                    over(mx, my, px + BEH_X, cy2, 62, 16));
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + BEH_X + 66, cy2, 76, 16,
                    waits[Math.max(0, Math.min(waits.length - 1, patrolWaitIdx))],
                    over(mx, my, px + BEH_X + 66, cy2, 76, 16));
            NotchWidgets.primaryButton(ctx, this.textRenderer, px + BEH_X + 146, cy2, 54, 16, "Done",
                    over(mx, my, px + BEH_X + 146, cy2, 54, 16));
        }

        // Extras that ride along with any behavior. Everything about who it fights is here together,
        // rather than half of it hiding on the stats screen.
        drawToggle(ctx, mx, my, px + BEH_X, movesRow1(), "Avoid monsters", (movesBits & 1) != 0);
        drawToggle(ctx, mx, my, px + BEH_X + 102, movesRow1(), "Watch players", (movesBits & 2) != 0);
        drawToggle(ctx, mx, my, px + BEH_X, movesRow2(), "Protect owner", (movesBits & 4) != 0);
        drawToggle(ctx, mx, my, px + BEH_X + 102, movesRow2(), "Fight monsters", (movesBits & 8) != 0);
        drawToggle(ctx, mx, my, px + BEH_X, movesRow3(), "Fight players", (movesBits & 16) != 0);
        drawToggle(ctx, mx, my, px + BEH_X + 102, movesRow3(), "Fight back", (movesBits & 32) != 0);
        drawToggle(ctx, mx, my, px + BEH_X, movesRow4(), "Fight rivals", (movesBits & 64) != 0);

        String hint = switch (currentBehavior) {
            case STATIONARY -> "Stays exactly where you placed it.";
            case WANDER -> "Roams near its home (set where placed).";
            case FOLLOW_OWNER -> "Follows its owner, or the player named above.";
            case PATROL -> "Grab the route tool, right-click the ground; Done takes it back.";
            case GUARD -> "Holds a post and fights what comes near (ignores creepers).";
        };
        NotchWidgets.centerText(ctx, this.textRenderer, hint, px + W / 2, py + 261, NotchTheme.TEXT_MUTED, false);
    }

    /**
     * The voices worth offering, as sound ids paired with a plain name.
     *
     * <p>Deliberately a short list of vanilla voices rather than every sound in the game. Pitch is
     * what actually makes a cast: the same villager grunt at 70% and 130% reads as two people, so a
     * handful of voices times a pitch slider covers far more ground than a long menu would.
     */
    private static final String[][] VOICES = {
            {"", "Silent"},
            {"entity.villager.ambient", "Villager"},
            {"entity.villager.trade", "Trader"},
            {"entity.wandering_trader.ambient", "Wanderer"},
            {"entity.pillager.ambient", "Gruff"},
            {"entity.piglin.ambient", "Snorty"},
            {"entity.witch.ambient", "Cackle"},
            {"entity.evoker.ambient", "Mystic"},
            {"entity.allay.ambient_without_item", "Chime"},
            {"entity.cat.ambient", "Cat"},
            {"entity.wolf.ambient", "Dog"},
            {"entity.parrot.ambient", "Bird"},
    };

    private int voiceIndex() {
        String id = currentVoice.startsWith("minecraft:") ? currentVoice.substring(10) : currentVoice;
        for (int i = 0; i < VOICES.length; i++) {
            if (VOICES[i][0].equals(id)) return i;
        }
        return 0;
    }

    private void sendFlavor() {
        NotchPacketsClient.sendNpcFlavor(npcId, subtitleField.getText(), currentVoice, currentVoicePitch);
    }

    /** The voice row on the Talk tab: which sound, how high, and hear it. */
    private void drawVoiceRow(DrawContext ctx, int mx, int my) {
        NotchWidgets.divider(ctx, px + 8, py + 214, W - 16);
        ctx.drawText(this.textRenderer, "Voice:", px + 22, py + 226, NotchTheme.TEXT_DARK, false);
        boolean voiceHover = over(mx, my, px + 75, py + 222, 90, 14);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 75, py + 222, 90, 14,
                VOICES[voiceIndex()][1], voiceHover);
        if (voiceHover) {
            tooltip = java.util.List.of(
                    Text.literal("Voice").formatted(Formatting.WHITE),
                    Text.literal("A short sound when it is spoken to,").formatted(Formatting.GRAY),
                    Text.literal("and on every line it says.").formatted(Formatting.GRAY),
                    Text.literal("Click to cycle. Silent NPCs stay silent.").formatted(Formatting.DARK_GRAY));
        }

        ctx.drawText(this.textRenderer, "Pitch:", px + 172, py + 226, NotchTheme.TEXT_DARK, false);
        boolean downHover = over(mx, my, px + 208, py + 222, 16, 14);
        boolean upHover = over(mx, my, px + 254, py + 222, 16, 14);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 208, py + 222, 16, 14, "-", downHover);
        NotchWidgets.centerText(ctx, this.textRenderer, currentVoicePitch + "%", px + 239, py + 226,
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 254, py + 222, 16, 14, "+", upHover);
        if (downHover || upHover) {
            tooltip = java.util.List.of(
                    Text.literal("Pitch").formatted(Formatting.WHITE),
                    Text.literal("Low for big and slow, high for small").formatted(Formatting.GRAY),
                    Text.literal("and quick. This is what makes a cast").formatted(Formatting.GRAY),
                    Text.literal("out of one sound.").formatted(Formatting.DARK_GRAY));
        }
    }

    private boolean clickVoiceRow(int mx, int my) {
        if (over(mx, my, px + 75, py + 222, 90, 14)) {
            currentVoice = VOICES[(voiceIndex() + 1) % VOICES.length][0];
            sendFlavor(); // the server plays it back, so cycling auditions the voices
            return true;
        }
        if (over(mx, my, px + 208, py + 222, 16, 14)) {
            currentVoicePitch = Math.max(50, currentVoicePitch - (hasShiftDown() ? 5 : 10));
            sendFlavor();
            return true;
        }
        if (over(mx, my, px + 254, py + 222, 16, 14)) {
            currentVoicePitch = Math.min(200, currentVoicePitch + (hasShiftDown() ? 5 : 10));
            sendFlavor();
            return true;
        }
        return false;
    }

    private boolean clickBehavior(int mx, int my) {
        NotchNpcEntity.Behavior[] modes = NotchNpcEntity.Behavior.values();
        for (int i = 0; i < modes.length; i++) {
            if (over(mx, my, px + BEH_X, behY(i), BEH_W, BEH_H)) {
                currentBehavior = modes[i];
                sendBehavior();
                updateWidgetVisibility(); // the follow field only shows in FOLLOW mode
                return true;
            }
        }
        if (over(mx, my, px + BEH_X, behY(5), BEH_W, BEH_H)) {
            net.fugginbeenus.notchcurrency.net.NotchPacketsClient.sendNpcScheduleOpen(npcId);
            return true;
        }
        int[][] toggles = {
                {px + BEH_X, movesRow1(), 1},
                {px + BEH_X + 102, movesRow1(), 2},
                {px + BEH_X, movesRow2(), 4},
                {px + BEH_X + 102, movesRow2(), 8},
                {px + BEH_X, movesRow3(), 16},
                {px + BEH_X + 102, movesRow3(), 32},
                {px + BEH_X, movesRow4(), 64},
        };
        for (int[] t : toggles) {
            if (over(mx, my, t[0], t[1], TOGGLE_W, TOGGLE_H)) {
                movesBits ^= t[2];
                sendBehavior();
                return true;
            }
        }
        int cy = py + 152;
        if (currentBehavior == NotchNpcEntity.Behavior.FOLLOW_OWNER
                && over(mx, my, px + BEH_X + 166, cy, 34, 14)) {
            sendBehavior(); // picks up the field text
            return true;
        }
        if (usesRadius()) {
            if (over(mx, my, px + BEH_X + 50, cy, 20, 16)) {
                currentRadius = Math.max(4, currentRadius - 4);
                sendBehavior();
                return true;
            }
            if (over(mx, my, px + BEH_X + 140, cy, 20, 16)) {
                currentRadius = Math.min(64, currentRadius + 4);
                sendBehavior();
                return true;
            }
        } else if (currentBehavior == NotchNpcEntity.Behavior.PATROL) {
            if (over(mx, my, px + BEH_X + 56, cy, 84, 16)) {
                NotchPacketsClient.sendNpcPatrol(npcId, 0, 0); // hands over the bound route tool
                this.close(); // straight to walking the route
                return true;
            }
            if (over(mx, my, px + BEH_X + 146, cy, 54, 16)) {
                waypointCount = 0;
                NotchPacketsClient.sendNpcPatrol(npcId, 1, 0);
                return true;
            }
            int cy2 = cy + 18;
            if (over(mx, my, px + BEH_X, cy2, 62, 16)) {
                patrolSpeedIdx = (patrolSpeedIdx + 1) % 3;
                NotchPacketsClient.sendNpcPatrol(npcId, 3, patrolSpeedIdx);
                return true;
            }
            if (over(mx, my, px + BEH_X + 66, cy2, 76, 16)) {
                patrolWaitIdx = (patrolWaitIdx + 1) % 5;
                NotchPacketsClient.sendNpcPatrol(npcId, 4, patrolWaitIdx);
                return true;
            }
            if (over(mx, my, px + BEH_X + 146, cy2, 54, 16)) {
                NotchPacketsClient.sendNpcPatrol(npcId, 2, 0); // finalize: take the route tool back
                return true;
            }
        }
        return false;
    }

    private void sendBehavior() {
        if (followField != null) followName = followField.getText().trim();
        NotchPacketsClient.sendNpcSetBehavior(npcId, currentBehavior.ordinal(), currentRadius, followName, movesBits);
    }

    // ---- role tab ----

    private static final int ROLE_W = 98, ROLE_H = 16;
    private int roleX(int i) { return (i % 2 == 0) ? px + 40 : px + 164; }
    private int roleY(int i) { return py + 52 + (i / 2) * 18; }

    private void drawRole(DrawContext ctx, int mx, int my) {
        for (int i = 0; i < SELECTABLE.length; i++) {
            int rx = roleX(i), ry = roleY(i);
            boolean hover = over(mx, my, rx, ry, ROLE_W, ROLE_H);
            String label = roleLabel(SELECTABLE[i]);
            if (SELECTABLE[i] == currentRole) NotchWidgets.primaryButton(ctx, this.textRenderer, rx, ry, ROLE_W, ROLE_H, label, hover);
            else NotchWidgets.neutralButton(ctx, this.textRenderer, rx, ry, ROLE_W, ROLE_H, label, hover);
        }
        // Allegiance sits with the role: it's who the NPC answers to, not how it looks or moves.
        NotchWidgets.divider(ctx, px + 8, py + 158, W - 16);
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 40, py + 164, 220, 16, "Faction...",
                over(mx, my, px + 40, py + 164, 220, 16));
        NotchWidgets.centerText(ctx, this.textRenderer, "Recruiters sign players up.",
                px + W / 2, py + 184, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.centerText(ctx, this.textRenderer, "Guards use it to tell friend from foe.",
                px + W / 2, py + 194, NotchTheme.TEXT_MUTED, false);

        // Confirmation before leaving the SHOP role (it closes the shop + returns its stock).
        if (pendingRole != null) {
            int by = py + H - 54;
            NotchWidgets.centerText(ctx, this.textRenderer, "Switch to " + roleLabel(pendingRole) + "?",
                    px + W / 2, by, NotchTheme.TEXT_RED, false);
            NotchWidgets.centerText(ctx, this.textRenderer, "This closes the shop & returns its stock.",
                    px + W / 2, by + 10, NotchTheme.TEXT_RED, false);
            NotchWidgets.dangerButton(ctx, this.textRenderer, px + 40, by + 24, 100, 16, "Confirm",
                    over(mx, my, px + 40, by + 24, 100, 16));
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + 160, by + 24, 100, 16, "Cancel",
                    over(mx, my, px + 160, by + 24, 100, 16));
        }
    }

    // ---- talk (dialogue) tab ----

    private void drawTalk(DrawContext ctx, int mx, int my) {
        // Header: what this NPC currently says.
        String header = dialogueNodes <= 0 ? "No dialogue yet."
                : dialogueFlat ? "Quick lines: " + dialogueNodes
                : "Dialogue: " + dialogueNodes + " page" + (dialogueNodes == 1 ? "" : "s") + " (branching)";
        NotchWidgets.centerText(ctx, this.textRenderer, header, px + W / 2, py + 48, NotchTheme.TEXT_DARK, false);
        if (dialogueNodes <= 0) {
            NotchWidgets.centerText(ctx, this.textRenderer, "Talking to it goes straight to its job.",
                    px + W / 2, py + 58, NotchTheme.TEXT_MUTED, false);
        }

        // Two ways to author: quick random lines, or the full branching studio.
        boolean branching = dialogueNodes > 0 && !dialogueFlat;
        if (branching) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + 50, py + 70, 200, 18, "Quick Lines", false);
        } else {
            NotchWidgets.primaryButton(ctx, this.textRenderer, px + 50, py + 70, 200, 18,
                    "Quick Lines (random chat)", over(mx, my, px + 50, py + 70, 200, 18));
        }
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 50, py + 92, 200, 18,
                "Dialogue Studio (branching)", over(mx, my, px + 50, py + 92, 200, 18));
        if (branching) {
            NotchWidgets.centerText(ctx, this.textRenderer, "Quick Lines is off while this NPC",
                    px + W / 2, py + 113, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.textRenderer, "has branching pages.",
                    px + W / 2, py + 123, NotchTheme.TEXT_MUTED, false);
        } else {
            NotchWidgets.centerText(ctx, this.textRenderer, "Lines: one random chat line per talk.",
                    px + W / 2, py + 113, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.textRenderer, "Studio: full conversations.",
                    px + W / 2, py + 123, NotchTheme.TEXT_MUTED, false);
        }

        // Style: full conversation window, or a quick chat line + straight to the role.
        NotchWidgets.divider(ctx, px + 8, py + 134, W - 16);
        ctx.drawText(this.textRenderer, "Style:", px + 50, py + 146, NotchTheme.TEXT_DARK, false);
        boolean winHover = over(mx, my, px + 90, py + 142, 76, 14);
        boolean chatHover = over(mx, my, px + 170, py + 142, 76, 14);
        if (dialogueMode == 0) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, px + 90, py + 142, 76, 14, "Window", winHover);
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + 170, py + 142, 76, 14, "Chat", chatHover);
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + 90, py + 142, 76, 14, "Window", winHover);
            NotchWidgets.primaryButton(ctx, this.textRenderer, px + 170, py + 142, 76, 14, "Chat", chatHover);
        }
        String styleHint = dialogueMode == 0
                ? "Window: opens the conversation window."
                : "Chat: one random line, then opens its job.";
        NotchWidgets.centerText(ctx, this.textRenderer, styleHint, px + W / 2, py + 160, NotchTheme.TEXT_MUTED, false);

        // Optional goodbye line, said in chat when a screen this NPC opened closes.
        drawVoiceRow(ctx, mx, my);
        ctx.drawText(this.textRenderer, "Goodbye:", px + 22, py + 175, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + 75, py + 171, 164, 13, NotchTheme.PANEL_MID);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 244, py + 171, 34, 13, "Set",
                over(mx, my, px + 244, py + 171, 34, 13));

        if (dialogueNodes > 0) {
            NotchWidgets.divider(ctx, px + 8, py + 190, W - 16);
            NotchWidgets.dangerButton(ctx, this.textRenderer, px + 80, py + 196, 140, 14, "Remove Dialogue",
                    over(mx, my, px + 80, py + 196, 140, 14));
        }
    }

    private boolean clickTalk(int mx, int my) {
        boolean branching = dialogueNodes > 0 && !dialogueFlat;
        if (!branching && over(mx, my, px + 50, py + 70, 200, 18)) {
            NotchPacketsClient.sendNpcStudioOpen(npcId, true); // routes the reply into Quick Lines
            return true;
        }
        if (over(mx, my, px + 50, py + 92, 200, 18)) {
            NotchPacketsClient.sendNpcStudioOpen(npcId);
            return true;
        }
        if (over(mx, my, px + 90, py + 142, 76, 14)) {
            dialogueMode = 0;
            NotchPacketsClient.sendNpcDialogueMode(npcId, 0);
            return true;
        }
        if (over(mx, my, px + 170, py + 142, 76, 14)) {
            dialogueMode = 1;
            NotchPacketsClient.sendNpcDialogueMode(npcId, 1);
            return true;
        }
        if (over(mx, my, px + 244, py + 171, 34, 13)) {
            currentFarewell = farewellField.getText().trim();
            NotchPacketsClient.sendNpcSetFarewell(npcId, currentFarewell);
            return true;
        }
        if (dialogueNodes > 0 && over(mx, my, px + 80, py + 196, 140, 14)) {
            NotchPacketsClient.sendNpcDialogueClear(npcId);
            dialogueNodes = 0;
            dialogueFlat = true;
            return true;
        }
        return false;
    }

    // ---- pose tab ----

    private static final String[] POSE_NAMES = {"Standing", "Sitting", "Sneaking", "Sleeping", "Chilling", "Prone", "Waving", "Custom"};
    private static final String[] ANIM_NAMES = {"Statue (frozen)", "Breathe (default)", "Lively"};

    private int poseX(int i) { return (i % 2 == 0) ? px + 30 : px + 155; }
    private int poseY(int i) { return py + 50 + (i / 2) * 19; }

    private void drawPose(DrawContext ctx, int mx, int my) {
        for (int i = 0; i < POSE_NAMES.length; i++) {
            boolean hover = over(mx, my, poseX(i), poseY(i), 115, 16);
            if (i == poseId) {
                NotchWidgets.primaryButton(ctx, this.textRenderer, poseX(i), poseY(i), 115, 16, POSE_NAMES[i], hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.textRenderer, poseX(i), poseY(i), 115, 16, POSE_NAMES[i], hover);
            }
        }
        // Idle animation layered on the pose (statue = frozen, the old behavior).
        ctx.drawText(this.textRenderer, "Idle:", px + 50, py + 132, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 80, py + 128, 170, 16,
                ANIM_NAMES[Math.max(0, Math.min(ANIM_NAMES.length - 1, poseAnim))],
                over(mx, my, px + 80, py + 128, 170, 16));
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 50, py + 148, 200, 18, "Open Pose Editor",
                over(mx, my, px + 50, py + 148, 200, 18));
        // Position, rotation and size all live behind this. It's a floating panel, so you watch the
        // NPC change out in the world instead of guessing from a tab with no preview.
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 50, py + 170, 200, 18, "Move, Rotate & Size",
                over(mx, my, px + 50, py + 170, 200, 18));
        NotchWidgets.centerText(ctx, this.textRenderer, "Opens a movable panel; the world stays visible.",
                px + W / 2, py + 194, NotchTheme.TEXT_MUTED, false);
    }

    private boolean clickPose(int mx, int my) {
        for (int i = 0; i < POSE_NAMES.length; i++) {
            if (over(mx, my, poseX(i), poseY(i), 115, 16)) {
                poseId = i;
                NotchPacketsClient.sendNpcSetPose(npcId, poseId);
                return true;
            }
        }
        if (over(mx, my, px + 80, py + 128, 170, 16)) {
            poseAnim = (poseAnim + 1) % ANIM_NAMES.length;
            NotchPacketsClient.sendNpcSetAnim(npcId, poseAnim);
            return true;
        }
        if (over(mx, my, px + 50, py + 148, 200, 18)) {
            poseId = 7; // custom: the editor's edits show immediately
            NotchPacketsClient.sendNpcSetPose(npcId, poseId);
            MinecraftClient.getInstance().setScreen(new PoseEditorScreen(npcId));
            return true;
        }
        if (over(mx, my, px + 50, py + 170, 200, 18)) {
            MinecraftClient.getInstance().setScreen(new NpcMoveScreen(npcId));
            return true;
        }
        return false;
    }

    // ---- manage tab ----

    private void drawManage(DrawContext ctx, int mx, int my) {
        NotchWidgets.centerText(ctx, this.textRenderer, "Owner: " + (ownerName.isEmpty() ? "server" : ownerName),
                px + W / 2, py + 50, NotchTheme.TEXT_DARK, false);
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 70, py + 64, 160, 16, "Pick Up", over(mx, my, px + 70, py + 64, 160, 16));
        NotchWidgets.centerText(ctx, this.textRenderer, "Returns the NPC as an item to place elsewhere.",
                px + W / 2, py + 84, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.dangerButton(ctx, this.textRenderer, px + 70, py + 96, 160, 16, "Delete NPC", over(mx, my, px + 70, py + 96, 160, 16));

        NotchWidgets.divider(ctx, px + 8, py + 120, W - 16);
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 70, py + 126, 160, 16, "Edit Stats & Abilities",
                over(mx, my, px + 70, py + 126, 160, 16));
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 70, py + 146, 160, 16, "Open Equipment",
                over(mx, my, px + 70, py + 146, 160, 16));
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 70, py + 166, 160, 16, "Reactions",
                over(mx, my, px + 70, py + 166, 160, 16));
        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 70, py + 186, 160, 16, "Presets",
                over(mx, my, px + 70, py + 186, 160, 16));
        NotchWidgets.centerText(ctx, this.textRenderer, "Reactions: what it does when things happen to it.",
                px + W / 2, py + 206, NotchTheme.TEXT_MUTED, false);
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            for (int i = 0; i < 6; i++) {
                if (over(mx, my, tabX(i), py + TAB_Y, TAB_W, TAB_H)) {
                    if (tab != i) NotchWidgets.tick();
                    tab = i;
                    updateWidgetVisibility();
                    return true;
                }
            }
            if (tab == 0 && clickAppearance(mx, my)) { NotchWidgets.tick(); return true; }
            if (tab == 1 && clickBehavior(mx, my)) { NotchWidgets.tick(); return true; }
            if (tab == 3 && clickVoiceRow(mx, my)) { NotchWidgets.tick(); return true; }
            if (tab == 2) {
                if (pendingRole == null && over(mx, my, px + 40, py + 164, 220, 16)) {
                    NotchWidgets.click();
                    // The server replies with the factions this player may use, which opens the picker.
                    NotchPacketsClient.sendFactionPick(npcId,
                            net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager.PICK_LIST, "");
                    return true;
                }
                // Handle the pending SHOP-change confirmation first.
                if (pendingRole != null) {
                    int by = py + H - 54;
                    if (over(mx, my, px + 40, by + 24, 100, 16)) {
                        NotchWidgets.click();
                        currentRole = pendingRole;
                        pendingRole = null;
                        NotchPacketsClient.sendNpcSetRole(npcId, currentRole.ordinal());
                        return true;
                    }
                    if (over(mx, my, px + 160, by + 24, 100, 16)) {
                        NotchWidgets.click();
                        pendingRole = null;
                        return true;
                    }
                }
                for (int i = 0; i < SELECTABLE.length; i++) {
                    if (over(mx, my, roleX(i), roleY(i), ROLE_W, ROLE_H)) {
                        NpcRole picked = SELECTABLE[i];
                        if (picked == currentRole) return true;
                        NotchWidgets.tick();
                        // Leaving SHOP wipes the shop: require confirmation.
                        if (currentRole == NpcRole.SHOP) {
                            pendingRole = picked;
                        } else {
                            currentRole = picked;
                            NotchPacketsClient.sendNpcSetRole(npcId, currentRole.ordinal());
                        }
                        return true;
                    }
                }
            }
            if (tab == 3 && clickTalk(mx, my)) { NotchWidgets.tick(); return true; }
            if (tab == 4 && clickPose(mx, my)) { NotchWidgets.tick(); return true; }
            if (tab == 5) {
                if (over(mx, my, px + 70, py + 64, 160, 16)) { NotchWidgets.click(); NotchPacketsClient.sendNpcPickup(npcId); this.close(); return true; }
                if (over(mx, my, px + 70, py + 96, 160, 16)) { NotchWidgets.click(); NotchPacketsClient.sendNpcDelete(npcId); this.close(); return true; }
                if (over(mx, my, px + 70, py + 126, 160, 16)) {
                    NotchWidgets.click();
                    MinecraftClient.getInstance().setScreen(
                            new NpcStatsScreen(npcId, statsBits, maxHealth, speedPct, regen));
                    return true;
                }
                if (over(mx, my, px + 70, py + 146, 160, 16)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendNpcOpenEquip(npcId);
                    return true;
                }
                if (over(mx, my, px + 70, py + 166, 160, 16)) {
                    NotchWidgets.click();
                    // Server replies with this NPC's reactions, which opens the editor.
                    NotchPacketsClient.sendNpcActionsOpen(npcId);
                    return true;
                }
                if (over(mx, my, px + 70, py + 186, 160, 16)) {
                    NotchWidgets.click();
                    // Server replies with the preset list, which opens the preset screen.
                    NotchPacketsClient.sendNpcPreset(npcId,
                            net.fugginbeenus.notchcurrency.npc.NpcPresetManager.ACTION_OPEN, "");
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickAppearance(int mx, int my) {
        if (over(mx, my, px + 150, py + 64, 140, 14)) {
            currentName = nameField.getText();
            NotchPacketsClient.sendNpcSetName(npcId, currentName);
            return true;
        }
        if (over(mx, my, px + 260, titleRow(), 30, 13)) {
            sendFlavor();
            return true;
        }
        // Model: open the picker
        if (over(mx, my, px + 224, py + 100, 66, 14)) {
            MinecraftClient.getInstance().setScreen(new NotchNpcModelPickerScreen(this));
            return true;
        }
        // Skins per model
        if (isApplyModel()) {
            var variants = NpcAppearances.all();
            for (int i = 0; i < variants.size(); i++) {
                if (over(mx, my, varX(i), varY(i), VAR_W, VAR_H)) {
                    currentSkinValue = variants.get(i).id();
                    sendAppearance();
                    return true;
                }
            }
        } else if (isHumanoid()) {
            if (over(mx, my, px + 118, py + 122, 20, 16)) { cyclePreset(-1); return true; }
            if (over(mx, my, px + 268, py + 122, 20, 16)) { cyclePreset(1); return true; }
            if (over(mx, my, px + 118, py + 140, 174, 14)) { currentSlim = !currentSlim; sendAppearance(); return true; }
            if (over(mx, my, px + 118, py + 157, 12, 12)) { togglePlayerSkin(); return true; }
            if (over(mx, my, px + 118, py + 175, 12, 12)) { toggleUrlSkin(); return true; }
        }
        if (over(mx, my, px + RX, signRow(), signWidth(), 15)) {
            NotchWidgets.click();
            MinecraftClient.getInstance().setScreen(new NpcBillboardScreen(npcId, currentBillboard));
            return true;
        }
        // Nameplate nudge (size axes live on the Pose tab).
        if (over(mx, my, px + NAME_Y_MINUS_X, py + NAME_Y_ROW, STEP_W, STEP_H)) {
            currentNameOffset = Math.max(-2.0f, round1(currentNameOffset - 0.1f)); sendAppearance(); return true;
        }
        if (over(mx, my, px + NAME_Y_PLUS_X, py + NAME_Y_ROW, STEP_W, STEP_H)) {
            currentNameOffset = Math.min(3.0f, round1(currentNameOffset + 0.1f)); sendAppearance(); return true;
        }
        return false;
    }

    /** Called by the model picker when a model is chosen. */
    public void applyModel(String model) {
        currentModel = model;
        if (NotchNpcEntity.MODEL_APPLY.equals(model)) {
            currentSkinType = NotchNpcEntity.SKIN_VARIANT;
            if (currentSkinValue == null || currentSkinValue.isEmpty() || currentSkinValue.matches("\\d+")) currentSkinValue = "default";
        } else if (NotchNpcEntity.MODEL_HUMANOID.equals(model)) {
            currentSkinType = NotchNpcEntity.SKIN_PRESET;
            if (currentSkinValue == null || !currentSkinValue.matches("\\d+")) currentSkinValue = "1";
        } else {
            // Entity disguise: the mob provides its own appearance.
            currentSkinType = "entity";
            currentSkinValue = "";
        }
        sendAppearance();
    }

    private void cyclePreset(int dir) {
        currentSkinType = NotchNpcEntity.SKIN_PRESET;
        int n = presetIndex() + dir;
        if (n < 1) n = 12;
        if (n > 12) n = 1;
        currentSkinValue = Integer.toString(n);
        sendAppearance();
    }

    private void togglePlayerSkin() {
        if (NotchNpcEntity.SKIN_PLAYER.equals(currentSkinType)) {
            currentSkinType = NotchNpcEntity.SKIN_PRESET;
            currentSkinValue = "1";
        } else {
            currentSkinType = NotchNpcEntity.SKIN_PLAYER;
            currentSkinValue = playerField.getText().trim();
        }
        sendAppearance();
    }

    private void toggleUrlSkin() {
        if (NotchNpcEntity.SKIN_URL.equals(currentSkinType)) {
            currentSkinType = NotchNpcEntity.SKIN_PRESET;
            currentSkinValue = "1";
        } else {
            currentSkinType = NotchNpcEntity.SKIN_URL;
            currentSkinValue = urlField.getText().trim();
        }
        sendAppearance();
    }

    private void drawCheck(DrawContext ctx, int x, int y, boolean checked) {
        NotchWidgets.inset(ctx, x, y, 12, 12, checked ? NotchTheme.ACCENT_GREEN : NotchTheme.DEEP);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter re-applies the focused skin field (when its checkbox is on).
        if (keyCode == 257 || keyCode == 335) {
            if (playerField != null && playerField.isFocused() && NotchNpcEntity.SKIN_PLAYER.equals(currentSkinType)) {
                currentSkinValue = playerField.getText().trim();
                sendAppearance();
                return true;
            }
            if (urlField != null && urlField.isFocused() && NotchNpcEntity.SKIN_URL.equals(currentSkinType)) {
                currentSkinValue = urlField.getText().trim();
                sendAppearance();
                return true;
            }
        }
        // Plain characters insert via charTyped only (guards against the select-all wipe).
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, nameField, subtitleField, playerField, urlField, followField, farewellField)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static float round1(float v) { return Math.round(v * 10f) / 10f; }

    private void sendAppearance() {
        NotchPacketsClient.sendNpcSetAppearance(npcId, currentModel, currentSkinType, currentSkinValue,
                currentSlim, currentScale, currentScaleY, currentScaleZ, currentNameOffset);
    }

    private NotchNpcEntity findPreview() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null) return null;
        if (preview != null && !preview.isRemoved()) return preview;
        for (Entity e : c.world.getEntities()) {
            if (e instanceof NotchNpcEntity n && n.getUuid().equals(npcId)) { preview = n; return n; }
        }
        return null;
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private static String roleLabel(NpcRole role) {
        return switch (role) {
            case NONE -> "Basic"; // not "no role". It's the plain NPC, and a perfectly good one
            case GREETER -> "Greeter";
            case SHOP -> "Shop";
            case ENCHANTER -> "Enchanter";
            case ADMIN_SHOP -> "Admin Shop";
            case BANKER -> "Banker";
            case AUCTIONEER -> "Auctioneer";
            case MAILBOX -> "Mailbox";
            case RAFFLE -> "Raffle";
            case BOUNTY -> "Bounty";
            case DEALER -> "Dealer";
            case COSMETICS -> "Cosmetics";
            case RECRUITER -> "Recruiter";
            case CUSTOM -> "Custom (API)";
        };
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
