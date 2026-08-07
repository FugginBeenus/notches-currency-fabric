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
import net.minecraft.world.item.ItemStack;

public class TradeOfferCreateScreen extends AbstractContainerScreen<TradeOfferCreateScreenHandler> {

    private static final int W = 226, H = 256;

    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

    private EditBox giveCoinsField;
    private EditBox priceField;
    private EditBox targetField;

    public TradeOfferCreateScreen(TradeOfferCreateScreenHandler handler, Inventory inv, Component title) {
        super(handler, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        giveCoinsField = numberField(giveCoinsField, this.leftPos + 20, this.topPos + 96, 54);
        priceField = numberField(priceField, this.leftPos + 136, this.topPos + 96, 54);

        String oldTarget = targetField == null ? "" : targetField.getValue();
        targetField = new EditBox(this.font, this.leftPos + 30, this.topPos + 135, 102, 10, Component.literal("Target"));
        targetField.setMaxLength(16);
        targetField.setBordered(false);
        targetField.setHint(Component.literal("anyone").withStyle(ChatFormatting.DARK_GRAY));
        targetField.setValue(oldTarget);
        addRenderableWidget(targetField);
    }

    private EditBox numberField(EditBox old, int fx, int fy, int fw) {
        String kept = old == null ? "" : old.getValue();
        EditBox field = new EditBox(this.font, fx, fy, fw, 10, Component.literal(NotchWidgets.coinName()));
        field.setMaxLength(9);
        field.setBordered(false);
        field.setHint(Component.literal("0").withStyle(ChatFormatting.DARK_GRAY));
        field.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
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

    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Create Trade Offer", x + W / 2, y + 8);

        // YOU GIVE: the 3×3 grid + attached coins, like your side of a live trade.
        NotchWidgets.inset(ctx, x + 8, y + 22, 94, 104, NotchTheme.PANEL_MID);
        NotchWidgets.centerText(ctx, this.font, "YOU GIVE", x + 55, y + 24, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < TradeOfferCreateScreenHandler.GIVE_COUNT; i++) {
            NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.GIVE_X + (i % 3) * 18 - 1,
                    y + TradeOfferCreateScreenHandler.GIVE_Y + (i / 3) * 18 - 1);
        }
        NotchWidgets.pill(ctx, x + 14, y + 92, 82, 15);
        ctx.renderItem(COIN, x + 78, y + 91);
        NotchWidgets.centerText(ctx, this.font, NotchWidgets.coinName() + " attached", x + 55, y + 111, NotchTheme.TEXT_MUTED, false);

        // The exchange arrow.
        NotchWidgets.arrowRight(ctx, x + 105, y + 68, NotchTheme.TEXT_MUTED);

        // YOU GET: the requested 3×3 grid (samples, returned on close) + coin price.
        NotchWidgets.inset(ctx, x + 124, y + 22, 94, 104, NotchTheme.PANEL_MID);
        NotchWidgets.centerText(ctx, this.font, "YOU GET", x + 171, y + 24, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < TradeOfferCreateScreenHandler.WANT_COUNT; i++) {
            NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.WANT_X + (i % 3) * 18 - 1,
                    y + TradeOfferCreateScreenHandler.WANT_Y + (i / 3) * 18 - 1);
        }
        NotchWidgets.pill(ctx, x + 130, y + 92, 82, 15);
        ctx.renderItem(COIN, x + 194, y + 91);
        NotchWidgets.centerText(ctx, this.font, NotchWidgets.coinName() + " wanted", x + 171, y + 111, NotchTheme.TEXT_MUTED, false);

        // Who can accept + create.
        ctx.drawString(this.font, "To", x + 10, y + 136, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 26, y + 132, 110, 14, NotchTheme.DEEP);
        ctx.drawString(this.font, "(blank = anyone)", x + 141, y + 136, NotchTheme.TEXT_MUTED, false);
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
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        super.render(ctx, mouseX, mouseY, delta);
        this.renderTooltip(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && over((int) mouseX, (int) mouseY, leftPos + 8, topPos + 150, W - 16, 16)) {
            NotchWidgets.click();
            NotchPacketsClient.sendTradeOfferCreate(parse(priceField), parse(giveCoinsField),
                    targetField.getValue().trim());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keep the screen from closing / hotbar-swapping while typing in a focused field.
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, giveCoinsField, priceField, targetField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
