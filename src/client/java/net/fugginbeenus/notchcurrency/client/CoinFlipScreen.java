package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * The coin-flip betting screen. A scaled-up Notch Coin with a soft gold glow and twinkling sparkles
 * gives it some casino flair. Pick Heads or Tails, set a wager, and hit FLIP — the server closes this
 * screen so the coin-flip block does the spin-and-reveal. Bets are validated client-side before sending.
 */
public class CoinFlipScreen extends HandledScreen<CoinFlipScreenHandler> {

    private static final int W = 200, H = 196;
    private static final int COIN_CY = 58, COIN_HALF = 24; // 16px coin drawn at 3x = 48px
    private static final int BAL_Y = 20;
    private static final int HEADS_X = 20, TAILS_X = 104, SIDE_Y = 90, SIDE_W = 76, SIDE_H = 18;
    private static final int FIELD_X = 46, FIELD_Y = 142, FIELD_W = 140, FIELD_H = 14;
    private static final int FLIP_X = 14, FLIP_Y = 160, FLIP_W = 172, FLIP_H = 18;

    private TextFieldWidget betField;
    private boolean selectedHeads = true;

    private String errorMsg;
    private long errorUntilMs;

    public CoinFlipScreen(CoinFlipScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        betField = new TextFieldWidget(this.textRenderer, this.x + FIELD_X + 2, this.y + FIELD_Y + 3,
                FIELD_W - 4, FIELD_H - 5, Text.literal("Bet"));
        betField.setMaxLength(12);
        betField.setDrawsBackground(false);
        betField.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        int min = handler.prop(CoinFlipScreenHandler.P_MIN);
        betField.setText(Integer.toString(Math.max(1, min)));
        addDrawableChild(betField);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        final long now = System.currentTimeMillis();
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Coin Flip", x + W / 2, y + 8);

        long bal = handler.prop(CoinFlipScreenHandler.P_BAL) & 0xFFFFFFFFL;
        NotchWidgets.centerText(ctx, this.textRenderer, bal + " " + NotchWidgets.coinName(), x + W / 2, y + BAL_Y, NotchTheme.TEXT_GOLD, true);

        // The coin: soft glow + sparkles behind the scaled Notch Coin.
        int ccx = x + W / 2, ccy = y + COIN_CY;
        drawGlow(ctx, ccx, ccy, now);
        drawSparkles(ctx, ccx, ccy, now);
        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(ccx - COIN_HALF, ccy - COIN_HALF, 0);
        m.scale(3f, 3f, 1f);
        ctx.drawItem(new ItemStack(selectedHeads ? ModItems.NOTCH_COIN : ModItems.COIN_TAILS), 0, 0);
        m.pop();

        // Side pickers: selected = green, other = neutral grey.
        boolean hHover = over(mouseX, mouseY, x + HEADS_X, y + SIDE_Y, SIDE_W, SIDE_H);
        boolean tHover = over(mouseX, mouseY, x + TAILS_X, y + SIDE_Y, SIDE_W, SIDE_H);
        if (selectedHeads) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + HEADS_X, y + SIDE_Y, SIDE_W, SIDE_H, "Heads", hHover);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + TAILS_X, y + SIDE_Y, SIDE_W, SIDE_H, "Tails", tHover);
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + HEADS_X, y + SIDE_Y, SIDE_W, SIDE_H, "Heads", hHover);
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + TAILS_X, y + SIDE_Y, SIDE_W, SIDE_H, "Tails", tHover);
        }

        // Error > projection.
        int payout = handler.prop(CoinFlipScreenHandler.P_PAYOUT);
        long bet = betValue();
        if (errorMsg != null && now < errorUntilMs) {
            NotchWidgets.centerText(ctx, this.textRenderer, errorMsg, x + W / 2, y + 116, NotchTheme.TEXT_RED, false);
        } else if (bet > 0) {
            long net = Math.round(bet * (payout / 100.0)) - bet;
            NotchWidgets.centerText(ctx, this.textRenderer, "Call it right: +" + net + " " + NotchWidgets.coinName(),
                    x + W / 2, y + 114, NotchTheme.TEXT_GREEN, false);
            NotchWidgets.centerText(ctx, this.textRenderer, "Call it wrong: -" + bet + " " + NotchWidgets.coinName(),
                    x + W / 2, y + 124, NotchTheme.TEXT_RED, false);
        } else {
            NotchWidgets.centerText(ctx, this.textRenderer, "Enter a bet below.",
                    x + W / 2, y + 118, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, x + 8, y + 136, W - 16);

        ctx.drawText(this.textRenderer, "Bet:", x + 16, y + FIELD_Y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + FIELD_X, y + FIELD_Y, FIELD_W, FIELD_H, NotchTheme.DEEP);

        boolean flipHover = over(mouseX, mouseY, x + FLIP_X, y + FLIP_Y, FLIP_W, FLIP_H);
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + FLIP_X, y + FLIP_Y, FLIP_W, FLIP_H, "FLIP", flipHover);

        NotchWidgets.centerText(ctx, this.textRenderer, "A true 50/50. Win pays " + payout + "%.",
                x + W / 2, y + 182, NotchTheme.TEXT_MUTED, false);
    }

    // ---- coin flair ----

    private void drawGlow(DrawContext ctx, int cx, int cy, long now) {
        double s = 0.5 + 0.5 * Math.sin(now / 300.0);
        int gr = COIN_HALF + 3 + (int) (4 * s);
        int a = 0x22 + (int) (0x30 * s);
        fillCircle(ctx, cx, cy, gr, (a << 24) | 0xE0A526);
    }

    private void fillCircle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            ctx.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }

    private void drawSparkles(DrawContext ctx, int cx, int cy, long now) {
        int d = COIN_HALF + 6;
        int[][] pts = {{-d, -d + 3}, {d, -d + 6}, {-d + 4, d - 3}, {d - 3, d - 1}};
        for (int i = 0; i < pts.length; i++) {
            boolean on = ((now / 260) + i) % 3 == 0;
            int px = cx + pts[i][0], py = cy + pts[i][1];
            int c = on ? 0xFFFFF3B0 : 0x55FFF3B0;
            ctx.fill(px - 1, py, px + 2, py + 1, c);
            ctx.fill(px, py - 1, px + 1, py + 2, c);
        }
    }

    // ---- input ----

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private void setError(String msg) {
        errorMsg = msg;
        errorUntilMs = System.currentTimeMillis() + 2500L;
    }

    private void playErr() {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.6f));
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // no default labels
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, this.x + HEADS_X, this.y + SIDE_Y, SIDE_W, SIDE_H)) { NotchWidgets.tick(); selectedHeads = true; return true; }
            if (over(mx, my, this.x + TAILS_X, this.y + SIDE_Y, SIDE_W, SIDE_H)) { NotchWidgets.tick(); selectedHeads = false; return true; }
            if (over(mx, my, this.x + FLIP_X, this.y + FLIP_Y, FLIP_W, FLIP_H)) {
                NotchWidgets.click();
                if (handler.prop(CoinFlipScreenHandler.P_ENABLED) == 0) { setError("Gambling is disabled."); playErr(); return true; }
                long bet = betValue();
                long bal = handler.prop(CoinFlipScreenHandler.P_BAL) & 0xFFFFFFFFL;
                int min = handler.prop(CoinFlipScreenHandler.P_MIN);
                int max = handler.prop(CoinFlipScreenHandler.P_MAX);
                if (bet < min || bet > max) { setError("Bet must be " + min + "-" + max + " " + NotchWidgets.coinName() + "."); playErr(); return true; }
                if (bet > bal) { setError("Not enough " + NotchWidgets.coinName() + " for that bet."); playErr(); return true; }
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(selectedHeads);
                buf.writeVarLong(bet);
                NetClient.sendToServer(NotchPackets.COINFLIP_FLIP, buf);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private long betValue() {
        try {
            return Long.parseLong(betField.getText().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keep the screen from closing / hotbar-swapping while typing in a focused field.
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, betField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens — they draw crisp panels over the world.
    }
    *///?}
}
