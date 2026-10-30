package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TradeOfferCreateScreen extends AbstractContainerScreen<TradeOfferCreateScreenHandler> {

    private static final int W = 226, H = 256;
    private static final int GIVE_MONEY_X = 14, WANT_MONEY_X = 130, MONEY_Y = 92, MONEY_W = 82;

    private EditBox giveCoinsField;
    private EditBox priceField;
    private EditBox targetField;

    public TradeOfferCreateScreen(TradeOfferCreateScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, W, H);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        //?}
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        giveCoinsField = numberField(giveCoinsField, this.leftPos + GIVE_MONEY_X + 18, this.topPos + MONEY_Y + 5);
        priceField = numberField(priceField, this.leftPos + WANT_MONEY_X + 18, this.topPos + MONEY_Y + 5);

        String oldTarget = targetField == null ? "" : targetField.getValue();
        targetField = new EditBox(this.font, this.leftPos + 30, this.topPos + 135, W - 44, 10, Component.literal("Target"));
        targetField.setMaxLength(16);
        targetField.setBordered(false);
        targetField.setHint(Component.literal("anyone").withStyle(ChatFormatting.DARK_GRAY));
        targetField.setValue(oldTarget);
        addRenderableWidget(targetField);
    }

    private EditBox numberField(EditBox old, int fx, int fy) {
        String kept = old == null ? "" : old.getValue();
        EditBox field = new EditBox(this.font, fx, fy, MONEY_W - 21, 10, Component.literal(NotchWidgets.coinName()));
        field.setMaxLength(9);
        field.setBordered(false);
        field.setTextColor(NotchWidgets.COIN_GOLD);
        field.setHint(Component.literal("0").withStyle(ChatFormatting.DARK_GRAY));
        net.fugginbeenus.notchcurrency.compat.Render.setFilter(field, s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        field.setValue(kept);
        addRenderableWidget(field);
        return field;
    }

    private long parse(EditBox field) {
        try {
            return field.getValue().isEmpty() ? 0 : Long.parseLong(field.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.inset(ctx, x + 8, y + 22, 94, 104, NotchTheme.PANEL_MID);
        header(ctx, "You give", x + 55, y + 26);
        for (int i = 0; i < TradeOfferCreateScreenHandler.GIVE_COUNT; i++) {
            NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.GIVE_X + (i % 3) * 18 - 1,
                    y + TradeOfferCreateScreenHandler.GIVE_Y + (i / 3) * 18 - 1);
        }
        NotchWidgets.coinInput(ctx, x + GIVE_MONEY_X, y + MONEY_Y, MONEY_W);
        NotchWidgets.centerText(ctx, this.font, NotchWidgets.coinName() + " attached", x + 55, y + 112, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.arrowRight(ctx, x + 106, y + 56, NotchTheme.TEXT_MUTED);
        NotchWidgets.inset(ctx, x + 124, y + 22, 94, 104, NotchTheme.PANEL_MID);
        header(ctx, "You get", x + 171, y + 26);
        for (int i = 0; i < TradeOfferCreateScreenHandler.WANT_COUNT; i++) {
            NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.WANT_X + (i % 3) * 18 - 1,
                    y + TradeOfferCreateScreenHandler.WANT_Y + (i / 3) * 18 - 1);
        }
        NotchWidgets.coinInput(ctx, x + WANT_MONEY_X, y + MONEY_Y, MONEY_W);
        NotchWidgets.centerText(ctx, this.font, NotchWidgets.coinName() + " wanted", x + 171, y + 112, NotchTheme.TEXT_MUTED, false);
        ctx.drawString(this.font, "To", x + 10, y + 136, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 26, y + 132, W - 34, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.font, x + 8, y + 150, W - 16, 16, "Create Offer",
                over(mouseX, mouseY, x + 8, y + 150, W - 16, 16));

        NotchWidgets.divider(ctx, x + 8, y + 168, W - 16);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.INV_X + col * 18 - 1,
                        y + TradeOfferCreateScreenHandler.INV_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.INV_X + col * 18 - 1,
                    y + TradeOfferCreateScreenHandler.HOTBAR_Y - 1);
        }
        NotchWidgets.title(ctx, this.font, "Create Trade Offer", x + W / 2, y + 8, NotchWidgets.Plate.BRASS);
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private static void header(GuiGraphics ctx, String label, int cx, int y) {
        NotchWidgets.smallCaps(ctx, label, cx - NotchWidgets.smallCapsWidth(label) / 2, y, NotchTheme.TEXT_DARK);
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
        this.renderTooltip(ctx, mouseX, mouseY);
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
        if (button == 0 && over((int) mouseX, (int) mouseY, leftPos + 8, topPos + 150, W - 16, 16)) {
            NotchWidgets.click();
            NotchPacketsClient.sendTradeOfferCreate(parse(priceField), parse(giveCoinsField),
                    targetField.getValue().trim());
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
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
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, giveCoinsField, priceField, targetField)) return true;
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
