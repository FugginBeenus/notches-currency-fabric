package net.fugginbeenus.notchcurrency.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fugginbeenus.notchcurrency.client.bounty.BoardArt;
import net.fugginbeenus.notchcurrency.client.bounty.BookPaper;
import net.fugginbeenus.notchcurrency.client.bounty.PaperNote;
import net.fugginbeenus.notchcurrency.client.bounty.PickList;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyPostScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BountyPostScreen extends AbstractContainerScreen<BountyPostScreenHandler> {

    private static final int W = 200, H = 222;
    private static final int NOTE_X = 10, NOTE_Y = 18, NOTE_W = 180, NOTE_H = 106, COL = 62;
    private static final int TAB_Y = 27, ROW_Y = 43, REWARD_Y = 66, BTN_Y = 104;
    private static final String[] TABS = {"Wanted", "Help wanted", "Player"};
    private static final int[] TAB_X = {20, 58, 115};
    private static final long NOTE_SEED = 0x6E07E5EEDL;
    private static final List<EntityType<?>> MOBS = new ArrayList<>();
    private static final List<String> MOB_NAMES = new ArrayList<>();

    private int mode;
    private EditBox amountField;
    private EditBox coinsField;
    private EditBox targetField;
    private PickList pick;
    private PaperNote note;
    private int noteVersion = -1;

    public BountyPostScreen(BountyPostScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, W, H);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        //?}
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
        if (MOBS.isEmpty()) {
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                if (type.getCategory() == MobCategory.MONSTER) MOBS.add(type);
            }
            MOBS.sort(Comparator.comparing(t -> t.getDescription().getString()));
            for (EntityType<?> type : MOBS) MOB_NAMES.add(type.getDescription().getString());
        }
    }

    @Override
    protected void init() {
        super.init();
        amountField = numberField(amountField, this.leftPos + COL + 4, this.topPos + ROW_Y + 3, 24, 3, "10");
        coinsField = numberField(coinsField, this.leftPos + COL + 18, this.topPos + REWARD_Y + 5, 55, 9, "");
        coinsField.setTextColor(NotchWidgets.COIN_GOLD);
        String kept = targetField == null ? "Zombie" : targetField.getValue();
        targetField = new EditBox(this.font, 0, this.topPos + ROW_Y + 3, 10, 10, Component.literal("Target"));
        targetField.setMaxLength(32);
        targetField.setBordered(false);
        targetField.setValue(kept);
        addRenderableWidget(targetField);
        pick = new PickList(this.font, targetField);
        setMode(mode, false);
    }

    private EditBox numberField(EditBox old, int fx, int fy, int w, int max, String start) {
        String kept = old == null ? start : old.getValue();
        EditBox field = new EditBox(this.font, fx, fy, w, 10, Component.literal("Amount"));
        field.setMaxLength(max);
        field.setBordered(false);
        field.setHint(Component.literal("0").withStyle(ChatFormatting.DARK_GRAY));
        net.fugginbeenus.notchcurrency.compat.Render.setFilter(field, s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        field.setValue(kept);
        addRenderableWidget(field);
        return field;
    }

    private void setMode(int next, boolean sound) {
        if (sound) NotchWidgets.tick();
        if (next != mode && (next == 2 || mode == 2)) targetField.setValue("");
        mode = next;
        menu.setFetchMode(mode == 1);
        amountField.visible = mode != 2;
        targetField.visible = mode != 1;
        targetField.setX(this.leftPos + (mode == 2 ? COL + 3 : 101));
        targetField.setWidth(mode == 2 ? 114 : 78);
        targetField.setHint(Component.literal(mode == 2 ? "player name" : "monster").withStyle(ChatFormatting.DARK_GRAY));
        pick.setOptions(mode == 2 ? players() : MOB_NAMES);
    }

    private List<String> players() {
        List<String> out = new ArrayList<>();
        if (this.minecraft == null || this.minecraft.getConnection() == null || this.minecraft.player == null) return out;
        for (var info : this.minecraft.getConnection().getOnlinePlayers()) {
            //? if >=1.21.11 {
            /*String name = info.getProfile().name();
            java.util.UUID id = info.getProfile().id();
            *///?} else {
            String name = info.getProfile().getName();
            java.util.UUID id = info.getProfile().getId();
            //?}
            if (!id.equals(this.minecraft.player.getUUID())) out.add(name);
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static long parse(EditBox field) {
        try {
            return field.getValue().isEmpty() ? 0 : Long.parseLong(field.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private PaperNote paper() {
        BookPaper.Sheet sheet = BookPaper.sheet();
        if (note == null || sheet.version != noteVersion) {
            if (note != null) note.release();
            note = PaperNote.bake(NOTE_SEED, NOTE_W, NOTE_H, sheet);
            noteVersion = sheet.version;
        }
        return note;
    }

    @Override
    public void removed() {
        if (note != null) note.release();
        note = null;
        super.removed();
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        BookPaper.Sheet sheet = BookPaper.sheet();
        BoardArt.frame(ctx, x, y, W, H);
        BoardArt.cork(ctx, x + 5, y + 5, W - 10, H - 10);
        paper().draw(ctx, x + NOTE_X, y + NOTE_Y);
        BoardArt.pin(ctx, x + NOTE_X + NOTE_W / 2, y + NOTE_Y + 2);

        for (int i = 0; i < TABS.length; i++) {
            if (i == 2 && !menu.pvpAllowed()) continue;
            int color = mode != i ? sheet.fringe : i == 1 ? sheet.ink : BoardArt.WANTED;
            NotchWidgets.smallCaps(ctx, TABS[i], x + TAB_X[i], y + TAB_Y, color);
            if (mode == i) ctx.fill(x + TAB_X[i], y + TAB_Y + 7, x + TAB_X[i] + NotchWidgets.smallCapsWidth(TABS[i]), y + TAB_Y + 8, color);
        }

        ctx.drawString(this.font, mode == 1 ? "Collect" : "Kill", x + 20, y + ROW_Y + 4, sheet.ink, false);
        if (mode != 2) NotchWidgets.inset(ctx, x + COL, y + ROW_Y, 30, 13, NotchTheme.DEEP);
        if (mode == 1) {
            NotchWidgets.slot(ctx, x + BountyPostScreenHandler.SAMPLE_X - 1, y + BountyPostScreenHandler.SAMPLE_Y - 1);
            ctx.drawString(this.font, "the item", x + 122, y + ROW_Y - 1, sheet.faint, false);
            ctx.drawString(this.font, "you want", x + 122, y + ROW_Y + 8, sheet.faint, false);
        } else {
            NotchWidgets.inset(ctx, targetField.getX() - 3, y + ROW_Y, targetField.getWidth() + 6, 13, NotchTheme.DEEP);
        }

        ctx.drawString(this.font, "Reward", x + 20, y + REWARD_Y + 5, sheet.ink, false);
        NotchWidgets.coinInput(ctx, x + COL, y + REWARD_Y, 76);
        ctx.drawString(this.font, "+", x + 139, y + REWARD_Y + 5, sheet.faint, false);
        NotchWidgets.slot(ctx, x + BountyPostScreenHandler.REWARD_X - 1, y + BountyPostScreenHandler.REWARD_Y - 1);
        NotchWidgets.centerText(ctx, this.font, "The reward is held until it's done.", x + W / 2, y + 89, sheet.faint, false);

        NotchWidgets.neutralButton(ctx, this.font, x + 20, y + BTN_Y, 40, 14, "Back", over(mouseX, mouseY, x + 20, y + BTN_Y, 40, 14));
        NotchWidgets.primaryButton(ctx, this.font, x + 124, y + BTN_Y, 48, 14, "Post", over(mouseX, mouseY, x + 124, y + BTN_Y, 48, 14));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + BountyPostScreenHandler.INV_X + col * 18 - 1,
                        y + BountyPostScreenHandler.INV_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + BountyPostScreenHandler.INV_X + col * 18 - 1, y + BountyPostScreenHandler.HOTBAR_Y - 1);
        }
        NotchWidgets.title(ctx, this.font, "Post a Bounty", x + W / 2, y + 8, NotchWidgets.Plate.PARCHMENT);
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        //? if >=1.21.11 {
        /*ctx.nextStratum();
        *///?} else {
        ctx.pose().pushPose();
        ctx.pose().translate(0f, 0f, 400f);
        //?}
        pick.render(ctx, mouseX, mouseY);
        //? if <1.21.11 {
        ctx.pose().popPose();
        //?}
        if (!pick.isOpen()) {
            this.renderTooltip(ctx, mouseX, mouseY);
            drawHints(ctx, mouseX, mouseY);
        }
    }

    private void drawHints(GuiGraphics ctx, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        List<Component> lines = null;
        if (mode == 1 && menu.sample().isEmpty() && over(mouseX, mouseY, x + BountyPostScreenHandler.SAMPLE_X - 1,
                y + BountyPostScreenHandler.SAMPLE_Y - 1, 18, 18)) {
            lines = List.of(Component.literal("Item to collect").withStyle(ChatFormatting.WHITE),
                    Component.literal("Put one of the item you want here.").withStyle(ChatFormatting.GRAY),
                    Component.literal("You get it back.").withStyle(ChatFormatting.GRAY));
        } else if (menu.reward().isEmpty() && over(mouseX, mouseY, x + BountyPostScreenHandler.REWARD_X - 1,
                y + BountyPostScreenHandler.REWARD_Y - 1, 18, 18)) {
            lines = List.of(Component.literal("Reward item").withStyle(ChatFormatting.WHITE),
                    Component.literal("Optional. It is held with the coins").withStyle(ChatFormatting.GRAY),
                    Component.literal("until someone finishes the job.").withStyle(ChatFormatting.GRAY));
        } else if (mode == 2 && over(mouseX, mouseY, x + TAB_X[2], y + TAB_Y - 3, 30, 12)) {
            lines = List.of(Component.literal("Bounty on a player").withStyle(ChatFormatting.WHITE),
                    Component.literal("The hunter who takes it and kills").withStyle(ChatFormatting.GRAY),
                    Component.literal("that player gets the reward.").withStyle(ChatFormatting.GRAY));
        }
        if (lines != null) ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
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
        int mx = (int) mouseX, my = (int) mouseY, x = this.leftPos, y = this.topPos;
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            if (pick.click(mouseX, mouseY)) {
                NotchWidgets.tick();
                return true;
            }
            for (int i = 0; i < TABS.length; i++) {
                if (i == 2 && !menu.pvpAllowed()) continue;
                if (mode != i && over(mx, my, x + TAB_X[i] - 2, y + TAB_Y - 3, NotchWidgets.smallCapsWidth(TABS[i]) + 4, 12)) {
                    setMode(i, true);
                    return true;
                }
            }
            if (over(mx, my, x + 20, y + BTN_Y, 40, 14) && this.minecraft != null && this.minecraft.gameMode != null) {
                NotchWidgets.click();
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
            if (over(mx, my, x + 124, y + BTN_Y, 48, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendBountyPost(mode, wanted(), (int) Math.min(999, parse(amountField)), parse(coinsField));
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private String wanted() {
        if (mode == 2) return targetField.getValue().trim();
        String name = pick.chosen();
        int i = name == null ? -1 : MOB_NAMES.indexOf(name);
        return i < 0 ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(MOBS.get(i)).toString();
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        if (pick.scroll(mouseX, mouseY, amount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (pick.scroll(mouseX, mouseY, amount)) return true;
        return super.mouseScrolled(mouseX, mouseY, amount);
    //?}
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (pick.key(keyCode)) return true;
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, amountField, coinsField, targetField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
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
}
