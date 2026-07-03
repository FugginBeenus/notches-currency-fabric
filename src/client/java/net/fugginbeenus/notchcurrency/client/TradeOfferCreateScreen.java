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
 * Create an offline trade offer: drop what you're giving in the top slot, put a sample of the item
 * you want in return in the lower slot (its count = how many), set a coin price, and optionally a
 * target player's name (blank = anyone can accept). Both slots' items come back if you close without
 * creating.
 */
public class TradeOfferCreateScreen extends HandledScreen<TradeOfferCreateScreenHandler> {

    private static final int W = 176, H = 224;

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
        String oldPrice = priceField == null ? "" : priceField.getText();
        priceField = new TextFieldWidget(this.textRenderer, this.x + 46, this.y + 49, 76, 10, Text.literal("Price"));
        priceField.setMaxLength(9);
        priceField.setDrawsBackground(false);
        priceField.setPlaceholder(Text.literal("0").formatted(Formatting.DARK_GRAY));
        priceField.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        priceField.setText(oldPrice);
        addDrawableChild(priceField);

        String oldTarget = targetField == null ? "" : targetField.getText();
        targetField = new TextFieldWidget(this.textRenderer, this.x + 46, this.y + 97, 116, 10, Text.literal("Target"));
        targetField.setMaxLength(16);
        targetField.setDrawsBackground(false);
        targetField.setPlaceholder(Text.literal("anyone").formatted(Formatting.DARK_GRAY));
        targetField.setText(oldTarget);
        addDrawableChild(targetField);
    }

    private int price() {
        try {
            return priceField.getText().isEmpty() ? 0 : Integer.parseInt(priceField.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Create Trade Offer", x + W / 2, y + 8);

        // Offered slot.
        NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.OFFERED_X - 1, y + TradeOfferCreateScreenHandler.OFFERED_Y - 1);
        ItemStack offered = handler.offeredStack();
        ctx.drawText(this.textRenderer, offered.isEmpty() ? "You give (drop item)" : "You give " + offered.getCount(),
                x + 34, y + 27, offered.isEmpty() ? NotchTheme.TEXT_MUTED : NotchTheme.TEXT_DARK, false);

        // Price.
        ctx.drawText(this.textRenderer, "Price", x + 12, y + 49, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 42, y + 46, 84, 14, NotchTheme.DEEP);
        ctx.drawText(this.textRenderer, "coins", x + 132, y + 49, NotchTheme.TEXT_MUTED, false);

        // Requested item sample slot.
        NotchWidgets.slot(ctx, x + TradeOfferCreateScreenHandler.REQUESTED_X - 1, y + TradeOfferCreateScreenHandler.REQUESTED_Y - 1);
        ItemStack requested = handler.requestedStack();
        ctx.drawText(this.textRenderer, requested.isEmpty() ? "and/or want (sample)" : "and " + requested.getCount() + "x this",
                x + 34, y + 75, requested.isEmpty() ? NotchTheme.TEXT_MUTED : NotchTheme.TEXT_DARK, false);

        // Target.
        ctx.drawText(this.textRenderer, "To", x + 12, y + 97, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 42, y + 94, 122, 14, NotchTheme.DEEP);

        NotchWidgets.centerText(ctx, this.textRenderer, "Blank target = anyone can accept.",
                x + W / 2, y + 112, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + 8, y + 120, W - 16, 15, "Create Offer",
                over(mouseX, mouseY, x + 8, y + 120, W - 16, 15));

        NotchWidgets.divider(ctx, x + 8, y + 136, W - 16);
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
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && over((int) mouseX, (int) mouseY, x + 8, y + 120, W - 16, 15)) {
            NotchPacketsClient.sendTradeOfferCreate(price(), targetField.getText().trim());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }
}
