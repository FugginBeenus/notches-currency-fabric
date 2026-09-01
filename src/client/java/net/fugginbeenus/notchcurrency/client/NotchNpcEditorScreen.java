package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.client.npc.NpcAppearances;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import java.util.UUID;

public class NotchNpcEditorScreen extends Screen {

    private static final int W = 300, H = 282;
    private static final NpcRole[] SELECTABLE = {
            NpcRole.NONE, NpcRole.SHOP, NpcRole.BANKER, NpcRole.AUCTIONEER,
            NpcRole.MAILBOX, NpcRole.RAFFLE, NpcRole.BOUNTY, NpcRole.DEALER, NpcRole.ENCHANTER,
            NpcRole.COSMETICS, NpcRole.RECRUITER
    };

    private final UUID npcId;
    @org.jetbrains.annotations.Nullable private java.util.List<Component> tooltip = null;
    private final String ownerName;
    private final boolean canEdit;
    private final boolean applyInstalled = FabricLoader.getInstance().isModLoaded("apply");
    private NpcRole currentRole;
    private NpcRole pendingRole = null;
    private String currentName;
    private String currentModel, currentSkinType, currentSkinValue;
    private boolean currentSlim;
    private float currentScale, currentScaleY, currentScaleZ, currentNameOffset;
    private NotchNpcEntity.Behavior currentBehavior;
    private int currentRadius;
    private int dialogueNodes;
    private boolean dialogueFlat;
    private int statsBits;
    private int maxHealth, speedPct, regen;
    private String followName;
    private int movesBits;
    private int dialogueMode;
    private int waypointCount;
    private int patrolSpeedIdx;
    private int patrolWaitIdx;
    private int poseId;
    private int poseAnim;
    private int tab = 0;
    private int px, py;
    private EditBox nameField;
    private String currentSubtitle = "";
    private String currentVoice = "";
    private int currentVoicePitch = 100;
    private EditBox playerField;
    private EditBox urlField;
    private EditBox followField;
    private EditBox farewellField;
    private String currentFarewell;
    private String currentBillboard;
    private NotchNpcEntity preview;

    public static int reopenAtTab = 0;

    public NotchNpcEditorScreen(net.fugginbeenus.notchcurrency.client.npc.NpcEditorState state) {
        super(Component.literal("NPC Editor"));
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
        nameField = new EditBox(this.font, px + 152, py + 49, 136, 9, Component.literal("Name"));
        nameField.setMaxLength(48);
        nameField.setBordered(false);
        nameField.setValue(currentName);
        addRenderableWidget(nameField);


        farewellField = new EditBox(this.font, px + 78, py + 175, 158, 9, Component.literal("Goodbye"));
        farewellField.setMaxLength(150);
        farewellField.setBordered(false);
        farewellField.setHint(Component.literal("(optional)").withStyle(ChatFormatting.DARK_GRAY));
        farewellField.setValue(currentFarewell);
        addRenderableWidget(farewellField);

        playerField = new EditBox(this.font, px + 152, py + 159, 134, 9, Component.literal("Player"));
        playerField.setMaxLength(16);
        playerField.setBordered(false);
        playerField.setHint(Component.literal("player name").withStyle(ChatFormatting.DARK_GRAY));
        if (NotchNpcEntity.SKIN_PLAYER.equals(currentSkinType)) playerField.setValue(currentSkinValue);
        addRenderableWidget(playerField);

        urlField = new EditBox(this.font, px + 152, py + 177, 134, 9, Component.literal("URL"));
        urlField.setMaxLength(256);
        urlField.setBordered(false);
        urlField.setHint(Component.literal("skin URL").withStyle(ChatFormatting.DARK_GRAY));
        if (NotchNpcEntity.SKIN_URL.equals(currentSkinType)) urlField.setValue(currentSkinValue);
        addRenderableWidget(urlField);

        followField = new EditBox(this.font, px + BEH_X + 52, py + 143, 104, 9, Component.literal("Follow"));
        followField.setMaxLength(16);
        followField.setBordered(false);
        followField.setHint(Component.literal("owner").withStyle(ChatFormatting.DARK_GRAY));
        followField.setValue(followName);
        addRenderableWidget(followField);

        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        nameField.visible = (tab == 0);
        farewellField.visible = (tab == 3);
        nameField.setCanLoseFocus(tab == 0);
        boolean hum = (tab == 0 && isHumanoid());
        playerField.visible = hum;
        urlField.visible = hum;
        followField.visible = (tab == 1 && currentBehavior == NotchNpcEntity.Behavior.FOLLOW_OWNER);
    }

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
        tooltip = null;
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "NPC Editor", px + W / 2, py + 8);
        drawTabs(ctx, mouseX, mouseY);
        switch (tab) {
            case 0 -> drawAppearance(ctx, mouseX, mouseY);
            case 1 -> drawBehavior(ctx, mouseX, mouseY);
            case 2 -> drawRole(ctx, mouseX, mouseY);
            case 3 -> drawTalk(ctx, mouseX, mouseY);
            case 4 -> drawPose(ctx, mouseX, mouseY);
            case 5 -> drawManage(ctx, mouseX, mouseY);
        }
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        if (tooltip != null) {
            ctx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private static final int TAB_Y = 22, TAB_H = 16, TAB_W = 44;
    private int tabX(int i) { return px + 9 + i * 47; }

    private void drawTabs(GuiGraphics ctx, int mx, int my) {
        String[] names = {"Look", "Moves", "Role", "Talk", "Pose", "Manage"};
        for (int i = 0; i < 6; i++) {
            int tx = tabX(i);
            boolean hover = over(mx, my, tx, py + TAB_Y, TAB_W, TAB_H);
            if (i == tab) NotchWidgets.primaryButton(ctx, this.font, tx, py + TAB_Y, TAB_W, TAB_H, names[i], hover);
            else NotchWidgets.neutralButton(ctx, this.font, tx, py + TAB_Y, TAB_W, TAB_H, names[i], hover);
        }
        NotchWidgets.divider(ctx, px + 8, py + 42, W - 16);
    }

    private static final int PREV_X = 8, PREV_Y = 46, PREV_W = 100, PREV_H = 176;
    private static final int RX = 116;
    private boolean isApplyModel() { return NotchNpcEntity.MODEL_APPLY.equals(currentModel); }

    private void drawAppearance(GuiGraphics ctx, int mx, int my) {
        NotchWidgets.inset(ctx, px + PREV_X, py + PREV_Y, PREV_W, PREV_H, NotchTheme.DEEP);
        NotchNpcEntity npc = findPreview();
        if (npc != null) {
            float oldYaw = npc.getYRot(), oldBody = npc.yBodyRot;
            boolean wasInvisible = npc.isInvisible();
            npc.setYRot(180);
            npc.yBodyRot = 180;
            npc.setInvisible(false); // always show the NPC in its own editor preview
            float lookX = (px + PREV_X + PREV_W / 2f) - mx;
            float lookY = (py + PREV_Y + 40f) - my;
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx, px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H - 18, previewSize(),
                    lookX, lookY, npc);
            npc.setYRot(oldYaw);
            npc.yBodyRot = oldBody;
            npc.setInvisible(wasInvisible);
        } else {
            NotchWidgets.centerText(ctx, this.font, "Preview", px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H / 2, NotchTheme.TEXT_MUTED, false);
        }

        ctx.drawString(this.font, "Name:", px + RX, py + 50, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + 150, py + 46, 140, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.font, px + 150, py + 64, 140, 14, "Save Name",
                over(mx, my, px + 150, py + 64, 140, 14));
        if (over(mx, my, px + 150, py + 46, 140, 14)) {
            tooltip = java.util.List.of(
                    Component.literal("Name").withStyle(ChatFormatting.WHITE),
                    Component.literal("Colour it with & codes: &6Carol").withStyle(ChatFormatting.GRAY),
                    Component.literal("Same codes as dialogue and signs.").withStyle(ChatFormatting.DARK_GRAY));
        }
        drawSignButton(ctx, mx, my);
        ctx.drawString(this.font, "Model:", px + RX, py + 104, NotchTheme.TEXT_DARK, false);
        ctx.drawString(this.font, trim(modelDisplayName(currentModel), 15), px + 150, py + 104, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + 224, py + 100, 66, 14, "Change...", over(mx, my, px + 224, py + 100, 66, 14));
        if (isApplyModel()) {
            NotchWidgets.neutralButton(ctx, this.font, px + 118, py + 122, 20, 16, "<", over(mx, my, px + 118, py + 122, 20, 16));
            NotchWidgets.neutralButton(ctx, this.font, px + 268, py + 122, 20, 16, ">", over(mx, my, px + 268, py + 122, 20, 16));
            NotchWidgets.centerText(ctx, this.font, variantDisplayName(), px + 203, py + 126, NotchTheme.TEXT_DARK, false);
        } else if (isHumanoid()) {
            boolean preset = NotchNpcEntity.SKIN_PRESET.equals(currentSkinType);
            NotchWidgets.neutralButton(ctx, this.font, px + 118, py + 122, 20, 16, "<", over(mx, my, px + 118, py + 122, 20, 16));
            NotchWidgets.neutralButton(ctx, this.font, px + 268, py + 122, 20, 16, ">", over(mx, my, px + 268, py + 122, 20, 16));
            NotchWidgets.centerText(ctx, this.font, "Preset " + presetIndex(), px + 203, py + 126,
                    preset ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);
            NotchWidgets.neutralButton(ctx, this.font, px + 118, py + 140, 174, 14,
                    currentSlim ? "Arms: Slim" : "Arms: Wide", over(mx, my, px + 118, py + 140, 174, 14));
            drawCheck(ctx, px + 118, py + 157, NotchNpcEntity.SKIN_PLAYER.equals(currentSkinType));
            NotchWidgets.inset(ctx, px + 150, py + 156, 138, 13, NotchTheme.DEEP);
            drawCheck(ctx, px + 118, py + 175, NotchNpcEntity.SKIN_URL.equals(currentSkinType));
            NotchWidgets.inset(ctx, px + 150, py + 174, 138, 13, NotchTheme.DEEP);
        } else {
            NotchWidgets.centerText(ctx, this.font, "This mob uses its own look.", px + 203, py + 134, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + 118, py + 230, 174, 18, "Appearance",
                over(mx, my, px + 118, py + 230, 174, 18));
    }


    private static final int NAME_Y_ROW = 82;
    private static final int STEP_W = 18, STEP_H = 14;
    private static final int NAME_Y_MINUS_X = 196, NAME_Y_PLUS_X = 272;
    private int signRow() { return py + 196; }
    private int signWidth() { return W - RX - 10; }
    private void drawSignButton(GuiGraphics ctx, int mx, int my) {
        int y = signRow();
        NotchWidgets.neutralButton(ctx, this.font, px + RX, y, signWidth(), 15,
                currentBillboard.isBlank() && currentSubtitle.isBlank()
                        ? "Add floating text..." : "Edit floating text...",
                over(mx, my, px + RX, y, signWidth(), 15));
    }

    private void drawNameOffsetRow(GuiGraphics ctx, int mx, int my) {
        int y = py + NAME_Y_ROW;
        ctx.drawString(this.font, "Name Y:", px + RX, y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + NAME_Y_MINUS_X, y, STEP_W, STEP_H, "-",
                over(mx, my, px + NAME_Y_MINUS_X, y, STEP_W, STEP_H));
        NotchWidgets.centerText(ctx, this.font, String.format("%+.1f", currentNameOffset),
                px + 245, y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + NAME_Y_PLUS_X, y, STEP_W, STEP_H, "+",
                over(mx, my, px + NAME_Y_PLUS_X, y, STEP_W, STEP_H));
    }

    private boolean isHumanoid() { return NotchNpcEntity.MODEL_HUMANOID.equals(currentModel); }

    private int previewSize() {
        float h = 1.9f, w = 0.6f; // humanoid default
        if (currentModel != null && currentModel.startsWith("entity:")) {
            EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.get(Reg.parse(currentModel.substring("entity:".length())));
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
        return (int) Math.max(8, Math.min(68, Math.min(fitH, fitW) * 0.75f));
    }

    private static String modelDisplayName(String model) {
        if (NotchNpcEntity.MODEL_HUMANOID.equals(model)) return "Humanoid";
        if (NotchNpcEntity.MODEL_APPLY.equals(model)) return "APP.ly";
        if (model != null && model.startsWith("entity:")) {
            EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.get(Reg.parse(model.substring("entity:".length())));
            return t != null ? t.getDescription().getString() : model.substring("entity:".length());
        }
        return model == null ? "Humanoid" : model;
    }

    private String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "..";
    }

    private int variantIndex() {
        var variants = NpcAppearances.all();
        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).id().equals(currentSkinValue)) return i;
        }
        return 0;
    }

    private String variantDisplayName() {
        return NpcAppearances.all().get(variantIndex()).displayName();
    }

    private void cycleVariant(int delta) {
        var variants = NpcAppearances.all();
        int next = Math.floorMod(variantIndex() + delta, variants.size());
        currentSkinValue = variants.get(next).id();
        NotchWidgets.click();
        sendAppearance();
    }

    private int presetIndex() {
        try { return Math.max(1, Math.min(NpcSkinsPresetCount(), Integer.parseInt(currentSkinValue))); }
        catch (NumberFormatException e) { return 1; }
    }
    private int NpcSkinsPresetCount() { return 12; }

    private static final int BEH_X = 50, BEH_W = 200, BEH_H = 15;
    private static final int TOGGLE_W = 98, TOGGLE_H = 15;
    private int movesRow1() { return py + 190; }
    private int movesRow2() { return py + 207; }
    private int movesRow3() { return py + 224; }
    private int movesRow4() { return py + 241; }

    private void drawToggle(GuiGraphics ctx, int mx, int my, int x, int y, String label, boolean on) {
        boolean hover = over(mx, my, x, y, TOGGLE_W, TOGGLE_H);
        if (on) NotchWidgets.primaryButton(ctx, this.font, x, y, TOGGLE_W, TOGGLE_H, label, hover);
        else NotchWidgets.neutralButton(ctx, this.font, x, y, TOGGLE_W, TOGGLE_H, label, hover);
    }
    private int behY(int i) { return py + 48 + i * 17; }

    private boolean usesRadius() {
        return currentBehavior == NotchNpcEntity.Behavior.WANDER || currentBehavior == NotchNpcEntity.Behavior.GUARD;
    }

    private void drawBehavior(GuiGraphics ctx, int mx, int my) {
        String[] labels = {"Stationary", "Wander around home", "Follow owner", "Patrol waypoints", "Guard this area"};
        NotchNpcEntity.Behavior[] modes = NotchNpcEntity.Behavior.values();
        for (int i = 0; i < modes.length; i++) {
            boolean hover = over(mx, my, px + BEH_X, behY(i), BEH_W, BEH_H);
            if (modes[i] == currentBehavior) {
                NotchWidgets.primaryButton(ctx, this.font, px + BEH_X, behY(i), BEH_W, BEH_H, labels[i], hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + BEH_X, behY(i), BEH_W, BEH_H, labels[i], hover);
            }
        }

        boolean schedHover = over(mx, my, px + BEH_X, behY(5), BEH_W, BEH_H);
        NotchWidgets.goldButton(ctx, this.font, px + BEH_X, behY(5), BEH_W, BEH_H,
                "Daily Schedule", schedHover);
        if (schedHover) {
            tooltip = java.util.List.of(
                    Component.literal("Daily Schedule").withStyle(ChatFormatting.GOLD),
                    Component.literal("Sleep, open the shop, wander, walk a round,").withStyle(ChatFormatting.GRAY),
                    Component.literal("all on a clock. Overrides the choice above").withStyle(ChatFormatting.GRAY),
                    Component.literal("while it is running.").withStyle(ChatFormatting.GRAY));
        }

        int cy = py + 152;
        if (usesRadius()) {
            ctx.drawString(this.font, "Radius:", px + BEH_X, cy + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, px + BEH_X + 50, cy, 20, 16, "-",
                    over(mx, my, px + BEH_X + 50, cy, 20, 16));
            NotchWidgets.centerText(ctx, this.font, currentRadius + " blocks", px + BEH_X + 105, cy + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, px + BEH_X + 140, cy, 20, 16, "+",
                    over(mx, my, px + BEH_X + 140, cy, 20, 16));
        } else if (currentBehavior == NotchNpcEntity.Behavior.FOLLOW_OWNER) {
            ctx.drawString(this.font, "Follow:", px + BEH_X, cy + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + BEH_X + 48, cy, 112, 14, NotchTheme.DEEP);
            NotchWidgets.primaryButton(ctx, this.font, px + BEH_X + 166, cy, 34, 14, "Set",
                    over(mx, my, px + BEH_X + 166, cy, 34, 14));
        } else if (currentBehavior == NotchNpcEntity.Behavior.PATROL) {
            ctx.drawString(this.font, "Points: " + waypointCount, px + BEH_X, cy + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.primaryButton(ctx, this.font, px + BEH_X + 56, cy, 84, 16, "Route tool",
                    over(mx, my, px + BEH_X + 56, cy, 84, 16));
            NotchWidgets.dangerButton(ctx, this.font, px + BEH_X + 146, cy, 54, 16, "Clear",
                    over(mx, my, px + BEH_X + 146, cy, 54, 16));

            int cy2 = cy + 18;
            String[] speeds = {"Stroll", "Walk", "Jog"};
            String[] waits = {"Wait: none", "Wait 2s", "Wait 5s", "Wait 10s", "Wait 20s"};
            NotchWidgets.neutralButton(ctx, this.font, px + BEH_X, cy2, 62, 16,
                    speeds[Math.max(0, Math.min(speeds.length - 1, patrolSpeedIdx))],
                    over(mx, my, px + BEH_X, cy2, 62, 16));
            NotchWidgets.neutralButton(ctx, this.font, px + BEH_X + 66, cy2, 76, 16,
                    waits[Math.max(0, Math.min(waits.length - 1, patrolWaitIdx))],
                    over(mx, my, px + BEH_X + 66, cy2, 76, 16));
            NotchWidgets.primaryButton(ctx, this.font, px + BEH_X + 146, cy2, 54, 16, "Done",
                    over(mx, my, px + BEH_X + 146, cy2, 54, 16));
        }

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
        NotchWidgets.centerText(ctx, this.font, hint, px + W / 2, py + 261, NotchTheme.TEXT_MUTED, false);
    }

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
        NotchPacketsClient.sendNpcFlavor(npcId, currentSubtitle, currentVoice, currentVoicePitch);
    }

    private void drawVoiceRow(GuiGraphics ctx, int mx, int my) {
        NotchWidgets.divider(ctx, px + 8, py + 214, W - 16);
        ctx.drawString(this.font, "Voice:", px + 22, py + 226, NotchTheme.TEXT_DARK, false);
        boolean voiceHover = over(mx, my, px + 75, py + 222, 90, 14);
        NotchWidgets.neutralButton(ctx, this.font, px + 75, py + 222, 90, 14,
                VOICES[voiceIndex()][1], voiceHover);
        if (voiceHover) {
            tooltip = java.util.List.of(
                    Component.literal("Voice").withStyle(ChatFormatting.WHITE),
                    Component.literal("A short sound when it is spoken to,").withStyle(ChatFormatting.GRAY),
                    Component.literal("and on every line it says.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Click to cycle. Silent NPCs stay silent.").withStyle(ChatFormatting.DARK_GRAY));
        }

        ctx.drawString(this.font, "Pitch:", px + 172, py + 226, NotchTheme.TEXT_DARK, false);
        boolean downHover = over(mx, my, px + 208, py + 222, 16, 14);
        boolean upHover = over(mx, my, px + 254, py + 222, 16, 14);
        NotchWidgets.neutralButton(ctx, this.font, px + 208, py + 222, 16, 14, "-", downHover);
        NotchWidgets.centerText(ctx, this.font, currentVoicePitch + "%", px + 239, py + 226,
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + 254, py + 222, 16, 14, "+", upHover);
        if (downHover || upHover) {
            tooltip = java.util.List.of(
                    Component.literal("Pitch").withStyle(ChatFormatting.WHITE),
                    Component.literal("Low for big and slow, high for small").withStyle(ChatFormatting.GRAY),
                    Component.literal("and quick. This is what makes a cast").withStyle(ChatFormatting.GRAY),
                    Component.literal("out of one sound.").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private boolean clickVoiceRow(int mx, int my) {
        if (over(mx, my, px + 75, py + 222, 90, 14)) {
            currentVoice = VOICES[(voiceIndex() + 1) % VOICES.length][0];
            sendFlavor();
            return true;
        }
        if (over(mx, my, px + 208, py + 222, 16, 14)) {
            currentVoicePitch = Math.max(50, currentVoicePitch - (net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 5 : 10));
            sendFlavor();
            return true;
        }
        if (over(mx, my, px + 254, py + 222, 16, 14)) {
            currentVoicePitch = Math.min(200, currentVoicePitch + (net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 5 : 10));
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
                updateWidgetVisibility();
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
            sendBehavior();
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
                NotchPacketsClient.sendNpcPatrol(npcId, 0, 0);
                this.onClose();
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
                NotchPacketsClient.sendNpcPatrol(npcId, 2, 0);
                return true;
            }
        }
        return false;
    }

    private void sendBehavior() {
        if (followField != null) followName = followField.getValue().trim();
        NotchPacketsClient.sendNpcSetBehavior(npcId, currentBehavior.ordinal(), currentRadius, followName, movesBits);
    }

    private static final int ROLE_W = 98, ROLE_H = 16;
    private int roleX(int i) { return (i % 2 == 0) ? px + 40 : px + 164; }
    private int roleY(int i) { return py + 52 + (i / 2) * 18; }

    private void drawRole(GuiGraphics ctx, int mx, int my) {
        for (int i = 0; i < SELECTABLE.length; i++) {
            int rx = roleX(i), ry = roleY(i);
            boolean hover = over(mx, my, rx, ry, ROLE_W, ROLE_H);
            String label = roleLabel(SELECTABLE[i]);
            if (SELECTABLE[i] == currentRole) NotchWidgets.primaryButton(ctx, this.font, rx, ry, ROLE_W, ROLE_H, label, hover);
            else NotchWidgets.neutralButton(ctx, this.font, rx, ry, ROLE_W, ROLE_H, label, hover);
        }
        NotchWidgets.divider(ctx, px + 8, py + 158, W - 16);
        NotchWidgets.primaryButton(ctx, this.font, px + 40, py + 164, 220, 16, "Faction...",
                over(mx, my, px + 40, py + 164, 220, 16));
        NotchWidgets.centerText(ctx, this.font, "Recruiters sign players up.",
                px + W / 2, py + 184, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.centerText(ctx, this.font, "Guards use it to tell friend from foe.",
                px + W / 2, py + 194, NotchTheme.TEXT_MUTED, false);

        if (pendingRole != null) {
            int by = py + H - 54;
            NotchWidgets.centerText(ctx, this.font, "Switch to " + roleLabel(pendingRole) + "?",
                    px + W / 2, by, NotchTheme.TEXT_RED, false);
            NotchWidgets.centerText(ctx, this.font, "This closes the shop & returns its stock.",
                    px + W / 2, by + 10, NotchTheme.TEXT_RED, false);
            NotchWidgets.dangerButton(ctx, this.font, px + 40, by + 24, 100, 16, "Confirm",
                    over(mx, my, px + 40, by + 24, 100, 16));
            NotchWidgets.neutralButton(ctx, this.font, px + 160, by + 24, 100, 16, "Cancel",
                    over(mx, my, px + 160, by + 24, 100, 16));
        }
    }

    private void drawTalk(GuiGraphics ctx, int mx, int my) {
        String header = dialogueNodes <= 0 ? "No dialogue yet."
                : dialogueFlat ? "Quick lines: " + dialogueNodes
                : "Dialogue: " + dialogueNodes + " page" + (dialogueNodes == 1 ? "" : "s") + " (branching)";
        NotchWidgets.centerText(ctx, this.font, header, px + W / 2, py + 48, NotchTheme.TEXT_DARK, false);
        if (dialogueNodes <= 0) {
            NotchWidgets.centerText(ctx, this.font, "Talking to it goes straight to its job.",
                    px + W / 2, py + 58, NotchTheme.TEXT_MUTED, false);
        }

        boolean branching = dialogueNodes > 0 && !dialogueFlat;
        if (branching) {
            NotchWidgets.neutralButton(ctx, this.font, px + 50, py + 70, 200, 18, "Quick Lines", false);
        } else {
            NotchWidgets.primaryButton(ctx, this.font, px + 50, py + 70, 200, 18,
                    "Quick Lines (random chat)", over(mx, my, px + 50, py + 70, 200, 18));
        }
        NotchWidgets.primaryButton(ctx, this.font, px + 50, py + 92, 200, 18,
                "Dialogue Studio (branching)", over(mx, my, px + 50, py + 92, 200, 18));
        if (branching) {
            NotchWidgets.centerText(ctx, this.font, "Quick Lines is off while this NPC",
                    px + W / 2, py + 113, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "has branching pages.",
                    px + W / 2, py + 123, NotchTheme.TEXT_MUTED, false);
        } else {
            NotchWidgets.centerText(ctx, this.font, "Lines: one random chat line per talk.",
                    px + W / 2, py + 113, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "Studio: full conversations.",
                    px + W / 2, py + 123, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, px + 8, py + 134, W - 16);
        ctx.drawString(this.font, "Style:", px + 50, py + 146, NotchTheme.TEXT_DARK, false);
        boolean winHover = over(mx, my, px + 90, py + 142, 76, 14);
        boolean chatHover = over(mx, my, px + 170, py + 142, 76, 14);
        if (dialogueMode == 0) {
            NotchWidgets.primaryButton(ctx, this.font, px + 90, py + 142, 76, 14, "Window", winHover);
            NotchWidgets.neutralButton(ctx, this.font, px + 170, py + 142, 76, 14, "Chat", chatHover);
        } else {
            NotchWidgets.neutralButton(ctx, this.font, px + 90, py + 142, 76, 14, "Window", winHover);
            NotchWidgets.primaryButton(ctx, this.font, px + 170, py + 142, 76, 14, "Chat", chatHover);
        }
        String styleHint = dialogueMode == 0
                ? "Window: opens the conversation window."
                : "Chat: one random line, then opens its job.";
        NotchWidgets.centerText(ctx, this.font, styleHint, px + W / 2, py + 160, NotchTheme.TEXT_MUTED, false);

        drawVoiceRow(ctx, mx, my);
        ctx.drawString(this.font, "Goodbye:", px + 22, py + 175, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + 75, py + 171, 164, 13, NotchTheme.PANEL_MID);
        NotchWidgets.neutralButton(ctx, this.font, px + 244, py + 171, 34, 13, "Set",
                over(mx, my, px + 244, py + 171, 34, 13));

        if (dialogueNodes > 0) {
            NotchWidgets.divider(ctx, px + 8, py + 190, W - 16);
            NotchWidgets.dangerButton(ctx, this.font, px + 80, py + 196, 140, 14, "Remove Dialogue",
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
            currentFarewell = farewellField.getValue().trim();
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

    private static final String[] POSE_NAMES = {"Standing", "Sitting", "Sneaking", "Sleeping", "Chilling", "Prone", "Waving", "Custom"};
    private static final String[] ANIM_NAMES = {"Statue (frozen)", "Breathe (default)", "Lively"};
    private String customClip = null; // null until read off the NPC standing in the world
    private static final int POSE_PREV_X = 22, POSE_PREV_Y = 74, POSE_PREV_W = 96, POSE_PREV_H = 124;
    private static final int POSE_CTL_X = 130, POSE_CTL_W = 148;

    private void drawPose(GuiGraphics ctx, int mx, int my) {
        NotchWidgets.neutralButton(ctx, this.font, px + 30, py + 50, 20, 16, "<", over(mx, my, px + 30, py + 50, 20, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + 250, py + 50, 20, 16, ">", over(mx, my, px + 250, py + 50, 20, 16));
        NotchWidgets.primaryButton(ctx, this.font, px + 54, py + 50, 192, 16, POSE_NAMES[poseIndex()], false);

        NotchWidgets.inset(ctx, px + POSE_PREV_X, py + POSE_PREV_Y, POSE_PREV_W, POSE_PREV_H, NotchTheme.DEEP);
        NotchNpcEntity npc = findPreview();
        if (npc != null) {
            float oldYaw = npc.getYRot(), oldBody = npc.yBodyRot;
            boolean wasInvisible = npc.isInvisible();
            npc.setYRot(180);
            npc.yBodyRot = 180;
            npc.setInvisible(false);
            float lookX = (px + POSE_PREV_X + POSE_PREV_W / 2f) - mx;
            float lookY = (py + POSE_PREV_Y + 30f) - my;
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx,
                    px + POSE_PREV_X + POSE_PREV_W / 2, py + POSE_PREV_Y + POSE_PREV_H - 14,
                    Math.max(8, (int) (previewSize() * 0.66f)), lookX, lookY, npc);
            npc.setYRot(oldYaw);
            npc.yBodyRot = oldBody;
            npc.setInvisible(wasInvisible);
        } else {
            NotchWidgets.centerText(ctx, this.font, "Preview", px + POSE_PREV_X + POSE_PREV_W / 2,
                    py + POSE_PREV_Y + POSE_PREV_H / 2, NotchTheme.TEXT_MUTED, false);
        }

        ctx.drawString(this.font, "Idle:", px + POSE_CTL_X, py + 78, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + POSE_CTL_X, py + 88, POSE_CTL_W, 16,
                ANIM_NAMES[Math.max(0, Math.min(ANIM_NAMES.length - 1, poseAnim))],
                over(mx, my, px + POSE_CTL_X, py + 88, POSE_CTL_W, 16));
        ctx.drawString(this.font, "Clip:", px + POSE_CTL_X, py + 110, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + POSE_CTL_X, py + 120, POSE_CTL_W, 16, clipLabel(),
                over(mx, my, px + POSE_CTL_X, py + 120, POSE_CTL_W, 16));

        NotchWidgets.primaryButton(ctx, this.font, px + POSE_CTL_X, py + 148, POSE_CTL_W, 18, "Open Pose Editor",
                over(mx, my, px + POSE_CTL_X, py + 148, POSE_CTL_W, 18));
        NotchWidgets.primaryButton(ctx, this.font, px + POSE_CTL_X, py + 170, POSE_CTL_W, 18, "Move & Rotate",
                over(mx, my, px + POSE_CTL_X, py + 170, POSE_CTL_W, 18));
        NotchWidgets.centerText(ctx, this.font, "Opens a movable panel; the world stays visible.",
                px + W / 2, py + 214, NotchTheme.TEXT_MUTED, false);
    }

    private int poseIndex() { return Math.max(0, Math.min(POSE_NAMES.length - 1, poseId)); }

    private void cyclePose(int delta) {
        poseId = Math.floorMod(poseIndex() + delta, POSE_NAMES.length);
        NotchWidgets.click();
        NotchPacketsClient.sendNpcSetPose(npcId, poseId);
    }

    private boolean clickPose(int mx, int my) {
        if (over(mx, my, px + 30, py + 50, 20, 16)) { cyclePose(-1); return true; }
        if (over(mx, my, px + 250, py + 50, 20, 16)) { cyclePose(1); return true; }
        if (over(mx, my, px + POSE_CTL_X, py + 88, POSE_CTL_W, 16)) {
            poseAnim = (poseAnim + 1) % ANIM_NAMES.length;
            NotchPacketsClient.sendNpcSetAnim(npcId, poseAnim);
            return true;
        }
        if (over(mx, my, px + POSE_CTL_X, py + 120, POSE_CTL_W, 16)) {
            cycleClip();
            return true;
        }
        if (over(mx, my, px + POSE_CTL_X, py + 148, POSE_CTL_W, 18)) {
            poseId = 7;
            NotchPacketsClient.sendNpcSetPose(npcId, poseId);
            Minecraft.getInstance().setScreen(new PoseEditorScreen(npcId));
            return true;
        }
        if (over(mx, my, px + POSE_CTL_X, py + 170, POSE_CTL_W, 18)) {
            Minecraft.getInstance().setScreen(new NpcMoveScreen(npcId));
            return true;
        }
        return false;
    }

    private java.util.List<String> clipChoices() {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add("");
        out.addAll(net.fugginbeenus.notchcurrency.compat.Geo.clipNames());
        return out;
    }

    private String currentClip() {
        if (customClip == null) {
            net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc = findNpc();
            customClip = npc == null ? "" : npc.getCustomClip();
        }
        return customClip;
    }

    private String clipLabel() {
        String clip = currentClip();
        if (clip.isEmpty()) return "Automatic";
        int dot = clip.lastIndexOf('.');
        String shown = dot >= 0 && dot < clip.length() - 1 ? clip.substring(dot + 1) : clip;
        return net.fugginbeenus.notchcurrency.compat.Geo.hasClip(clip) ? shown : shown + " (missing)";
    }

    private void cycleClip() {
        java.util.List<String> choices = clipChoices();
        int at = choices.indexOf(currentClip());
        customClip = choices.get((at + 1) % choices.size());
        NotchPacketsClient.sendNpcSetClip(npcId, customClip);
    }

    private net.fugginbeenus.notchcurrency.entity.NotchNpcEntity findNpc() {
        Minecraft c = Minecraft.getInstance();
        if (c.level == null) return null;
        for (net.minecraft.world.entity.Entity e : c.level.entitiesForRendering()) {
            if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity n
                    && n.getUUID().equals(npcId)) {
                return n;
            }
        }
        return null;
    }

    private void drawManage(GuiGraphics ctx, int mx, int my) {
        NotchWidgets.centerText(ctx, this.font, "Owner: " + (ownerName.isEmpty() ? "server" : ownerName),
                px + W / 2, py + 50, NotchTheme.TEXT_DARK, false);
        NotchWidgets.primaryButton(ctx, this.font, px + 70, py + 64, 160, 16, "Pick Up", over(mx, my, px + 70, py + 64, 160, 16));
        NotchWidgets.centerText(ctx, this.font, "Returns the NPC as an item to place elsewhere.",
                px + W / 2, py + 84, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.dangerButton(ctx, this.font, px + 70, py + 96, 160, 16, "Delete NPC", over(mx, my, px + 70, py + 96, 160, 16));

        NotchWidgets.divider(ctx, px + 8, py + 120, W - 16);
        NotchWidgets.primaryButton(ctx, this.font, px + 70, py + 126, 160, 16, "Edit Stats & Abilities",
                over(mx, my, px + 70, py + 126, 160, 16));
        NotchWidgets.primaryButton(ctx, this.font, px + 70, py + 146, 160, 16, "Open Equipment",
                over(mx, my, px + 70, py + 146, 160, 16));
        NotchWidgets.primaryButton(ctx, this.font, px + 70, py + 166, 160, 16, "Reactions",
                over(mx, my, px + 70, py + 166, 160, 16));
        NotchWidgets.primaryButton(ctx, this.font, px + 70, py + 186, 160, 16, "Presets",
                over(mx, my, px + 70, py + 186, 160, 16));
        NotchWidgets.centerText(ctx, this.font, "Reactions: what it does when things happen to it.",
                px + W / 2, py + 206, NotchTheme.TEXT_MUTED, false);
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
            if (tab == 0 && over(mx, my, px + 118, py + 230, 174, 18)) {
                NotchWidgets.click();
                net.minecraft.client.Minecraft.getInstance().setScreen(new NpcLooksScreen(npcId, statsBits));
                return true;
            }
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
                    NotchPacketsClient.sendFactionPick(npcId,
                            net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager.PICK_LIST, "");
                    return true;
                }
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
                if (over(mx, my, px + 70, py + 64, 160, 16)) { NotchWidgets.click(); NotchPacketsClient.sendNpcPickup(npcId); this.onClose(); return true; }
                if (over(mx, my, px + 70, py + 96, 160, 16)) { NotchWidgets.click(); NotchPacketsClient.sendNpcDelete(npcId); this.onClose(); return true; }
                if (over(mx, my, px + 70, py + 126, 160, 16)) {
                    NotchWidgets.click();
                    Minecraft.getInstance().setScreen(
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
                    NotchPacketsClient.sendNpcActionsOpen(npcId);
                    return true;
                }
                if (over(mx, my, px + 70, py + 186, 160, 16)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendNpcPreset(npcId,
                            net.fugginbeenus.notchcurrency.npc.NpcPresetManager.ACTION_OPEN, "");
                    return true;
                }
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private boolean clickAppearance(int mx, int my) {
        if (over(mx, my, px + 150, py + 64, 140, 14)) {
            currentName = nameField.getValue();
            NotchPacketsClient.sendNpcSetName(npcId, currentName);
            return true;
        }

        if (over(mx, my, px + 224, py + 100, 66, 14)) {
            Minecraft.getInstance().setScreen(new NotchNpcModelPickerScreen(this));
            return true;
        }

        if (isApplyModel()) {
            if (over(mx, my, px + 118, py + 122, 20, 16)) { cycleVariant(-1); return true; }
            if (over(mx, my, px + 268, py + 122, 20, 16)) { cycleVariant(1); return true; }
        } else if (isHumanoid()) {
            if (over(mx, my, px + 118, py + 122, 20, 16)) { cyclePreset(-1); return true; }
            if (over(mx, my, px + 268, py + 122, 20, 16)) { cyclePreset(1); return true; }
            if (over(mx, my, px + 118, py + 140, 174, 14)) { currentSlim = !currentSlim; sendAppearance(); return true; }
            if (over(mx, my, px + 118, py + 157, 12, 12)) { togglePlayerSkin(); return true; }
            if (over(mx, my, px + 118, py + 175, 12, 12)) { toggleUrlSkin(); return true; }
        }
        if (over(mx, my, px + RX, signRow(), signWidth(), 15)) {
            NotchWidgets.click();
            Minecraft.getInstance().setScreen(
                    new NpcBillboardScreen(npcId, currentBillboard, currentSubtitle, currentVoice, currentVoicePitch));
            return true;
        }

        return false;
    }

    public void applyModel(String model) {
        currentModel = model;
        if (NotchNpcEntity.MODEL_APPLY.equals(model)) {
            currentSkinType = NotchNpcEntity.SKIN_VARIANT;
            if (currentSkinValue == null || currentSkinValue.isEmpty() || currentSkinValue.matches("\\d+")) currentSkinValue = "default";
        } else if (NotchNpcEntity.MODEL_HUMANOID.equals(model)) {
            currentSkinType = NotchNpcEntity.SKIN_PRESET;
            if (currentSkinValue == null || !currentSkinValue.matches("\\d+")) currentSkinValue = "1";
        } else {
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
            currentSkinValue = playerField.getValue().trim();
        }
        sendAppearance();
    }

    private void toggleUrlSkin() {
        if (NotchNpcEntity.SKIN_URL.equals(currentSkinType)) {
            currentSkinType = NotchNpcEntity.SKIN_PRESET;
            currentSkinValue = "1";
        } else {
            currentSkinType = NotchNpcEntity.SKIN_URL;
            currentSkinValue = urlField.getValue().trim();
        }
        sendAppearance();
    }

    private void drawCheck(GuiGraphics ctx, int x, int y, boolean checked) {
        NotchWidgets.inset(ctx, x, y, 12, 12, checked ? NotchTheme.ACCENT_GREEN : NotchTheme.DEEP);
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (keyCode == 257 || keyCode == 335) {
            if (playerField != null && playerField.isFocused() && NotchNpcEntity.SKIN_PLAYER.equals(currentSkinType)) {
                currentSkinValue = playerField.getValue().trim();
                sendAppearance();
                return true;
            }
            if (urlField != null && urlField.isFocused() && NotchNpcEntity.SKIN_URL.equals(currentSkinType)) {
                currentSkinValue = urlField.getValue().trim();
                sendAppearance();
                return true;
            }
        }
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, nameField, playerField, urlField, followField, farewellField)) {
            return true;
        }
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    private static float round1(float v) { return Math.round(v * 10f) / 10f; }

    private void sendAppearance() {
        NotchPacketsClient.sendNpcSetAppearance(npcId, currentModel, currentSkinType, currentSkinValue,
                currentSlim, currentScale, currentScaleY, currentScaleZ, currentNameOffset,
                findPreview() == null ? 0f : findPreview().getBodyOffset());
    }

    private NotchNpcEntity findPreview() {
        Minecraft c = Minecraft.getInstance();
        if (c.level == null) return null;
        if (preview != null && !preview.isRemoved()) return preview;
        for (Entity e : c.level.entitiesForRendering()) {
            if (e instanceof NotchNpcEntity n && n.getUUID().equals(npcId)) { preview = n; return n; }
        }
        return null;
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private static String roleLabel(NpcRole role) {
        return switch (role) {
            case NONE -> "Basic";
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
    public boolean isPauseScreen() {
        return false;
    }

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
