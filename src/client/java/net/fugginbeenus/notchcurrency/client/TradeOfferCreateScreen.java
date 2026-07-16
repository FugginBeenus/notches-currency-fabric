package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Create an offline trade offer, laid out like the live-trade screen: a YOU GIVE 3×3 grid (drop the
 * actual items) with a coin pill under it, an arrow, and a YOU GET column (requested item sample +
 * coin pill), then who it's for and a big Create button. Everything in the grid comes back if you
 * close without creating.
 */
public class TradeOfferCreateScreen extends HandledScreen<TradeOfferCreateScreenHandler> {

    private static final int W = 226, H = 256;

    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

    private TextFieldWidget giveCoinsField;
    private TextFieldWidget priceField;
    private TextFieldWidget targetField;

    public TradeOfferCreateScreen(TradeOfferCreateScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        giveCoinsField = numberField(giveCoinsField, this.x + 20, this.y + 96, 54);
        priceField = numberField(priceField, this.x + 136, this.y + 96, 54);

        String oldTarget = targetField == null ? "" : targetField.getText();
        targetField = new TextFieldWidget(this.textRenderer, this.x + 30, this.y + 135, 102, 10, Text.literal("Target"));
        targetField.setMaxLength(16);
        targetField.setDrawsBackground(false);
        targetField.setPlaceholder(Text.literal("anyone").formatted(Formatting.DARK_GRAY));
        targetField.setText(oldTarget);
        addDrawableChild(targetField);
    }

    private TextFieldWidget numberField(TextFieldWidget old, int fx, int fy, int fw) {
        String kept = old == null ? "" : old.getText();
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, fx, fy, fw, 10, Text.literal(NotchWidgets.coinName()));
        field.setMaxLength(9);
        field.setDrawsBackground(false);
        field.setPlaceholder(Text.literal("0").formatted(Formatting.DARK_GRAY));
        field.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        field.setText(kept);
        addDrawableChild(field);
        return field;
    }

    private long parse(TextFieldWidget field) {
        try {
            return field.getText().isEmpty() ? 0 : Long.parseLong(field.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Create Trade Offer", x + W / 2, y + 8);

        // YOU GIVE: the 3×3 grid + attached coins, like your side of a live trade.
        NotchWidgets.inset(ctx, x + 8, y + 22, 94, 104, NotchTheme.PANEL_MID);
        NotchWidgets.centerText(ctx, this.textRenderer, "YOU GIVE", x + 55, y + 24, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < TradeOfferCreateScreenHandler.GIVE_COUNT; i++) {
            NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.GIVE_X + (i % 3) * 18 - 1,
                    y + TradeOfferCreateScreenHandler.GIVE_Y + (i / 3) * 18 - 1);
        }
        NotchWidgets.pill(ctx, x + 14, y + 92, 82, 15);
        ctx.drawItem(COIN, x + 78, y + 91);
        NotchWidgets.centerText(ctx, this.textRenderer, NotchWidgets.coinName() + " attached", x + 55, y + 111, NotchTheme.TEXT_MUTED, false);

        // The exchange arrow.
        NotchWidgets.arrowRight(ctx, x + 105, y + 68, NotchTheme.TEXT_MUTED);

        // YOU GET: the requested 3×3 grid (samples — returned on close) + coin price.
        NotchWidgets.inset(ctx, x + 124, y + 22, 94, 104, NotchTheme.PANEL_MID);
        NotchWidgets.centerText(ctx, this.textRenderer, "YOU GET", x + 171, y + 24, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < TradeOfferCreateScreenHandler.WANT_COUNT; i++) {
            NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.WANT_X + (i % 3) * 18 - 1,
                    y + TradeOfferCreateScreenHandler.WANT_Y + (i / 3) * 18 - 1);
        }
        NotchWidgets.pill(ctx, x + 130, y + 92, 82, 15);
        ctx.drawItem(COIN, x + 194, y + 91);
        NotchWidgets.centerText(ctx, this.textRenderer, NotchWidgets.coinName() + " wanted", x + 171, y + 111, NotchTheme.TEXT_MUTED, false);

        // Who can accept + create.
        ctx.drawText(this.textRenderer, "To", x + 10, y + 136, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 26, y + 132, 110, 14, NotchTheme.DEEP);
        ctx.drawText(this.textRenderer, "(blank = anyone)", x + 141, y + 136, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + 8, y + 150, W - 16, 16, "Create Offer",
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
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*this.renderBackground(ctx, mouseX, mouseY, delta);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && over((int) mouseX, (int) mouseY, x + 8, y + 150, W - 16, 16)) {
            NotchWidgets.click();
            NotchPacketsClient.sendTradeOfferCreate(parse(priceField), parse(giveCoinsField),
                    targetField.getText().trim());
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
        // No 1.21 menu blur behind the mod's screens — they draw crisp panels over the world.
    }
    *///?}
}
