package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler;
import net.fugginbeenus.notchcurrency.economy.gambling.SlotSymbol;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SlotMachineScreen extends AbstractContainerScreen<SlotMachineScreenHandler> {

    private static final int W = 200, H = 196;
    private static final int REEL_Y = 28, REEL_H = 40, REEL_W = 34;
    private static final int[] REEL_X = {43, 83, 123};
    private static final int FRAME_X1 = 37, FRAME_Y1 = 22, FRAME_X2 = 163, FRAME_Y2 = 72;
    private static final int MARQUEE_Y = 16, PAYLINE_CY = REEL_Y + REEL_H / 2;
    private static final int STATUS_Y = 78, BAL_Y = 90;
    private static final int FIELD_X = 46, FIELD_Y = 108, FIELD_W = 140, FIELD_H = 14;
    private static final int SPIN_X = 14, SPIN_Y = 126, SPIN_W = 172, SPIN_H = 18;
    private static final long MIN_SPIN_MS = 900L;
    private static final long REEL_STAGGER_MS = 240L;
    private static final long MAX_WAIT_MS = 5000L;
    private EditBox betField;
    private boolean spinning;
    private long spinStartMs;
    private int lastSpinIdSeen;
    private int announcedStops;
    private boolean resultPlayed;
    private boolean hasShown;
    private long shownWin;
    private long frozenBalance;
    private String errorMsg;
    private long errorUntilMs;

    public SlotMachineScreen(SlotMachineScreenHandler handler, Inventory inv, Component title) {
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
        betField = new EditBox(this.font, this.leftPos + FIELD_X + 2, this.topPos + FIELD_Y + 3,
                FIELD_W - 4, FIELD_H - 5, Component.literal("Bet"));
        betField.setMaxLength(12);
        betField.setBordered(false);
        net.fugginbeenus.notchcurrency.compat.Render.setFilter(betField, s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        int min = menu.prop(SlotMachineScreenHandler.P_MIN);
        betField.setValue(Integer.toString(Math.max(1, min)));
        addRenderableWidget(betField);
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        final long now = System.currentTimeMillis();
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Slot Machine", x + W / 2, y + 6);

        int[] display = computeReels();
        boolean jackpotShown = !spinning && hasShown && shownWin > 0
                && display[0] == display[1] && display[1] == display[2]
                && display[0] == SlotSymbol.STAR.ordinal();

        drawMarquee(ctx, x, y, now);
        drawFrame(ctx, x, y);
        drawPaylineMarkers(ctx, x, y, now);
        for (int i = 0; i < 3; i++) {
            int rx = x + REEL_X[i];
            NotchWidgets.slot(ctx, rx, y + REEL_Y, REEL_W, REEL_H);
            drawSymbol(ctx, display[i], rx + REEL_W / 2, y + PAYLINE_CY);
        }
        if (!spinning && hasShown && shownWin > 0) drawWinGlow(ctx, x, y, now, jackpotShown);

        if (errorMsg != null && now < errorUntilMs) {
            NotchWidgets.centerText(ctx, this.font, errorMsg, x + W / 2, y + STATUS_Y, NotchTheme.TEXT_RED, false);
        } else if (!spinning && hasShown) {
            if (shownWin > 0) {
                String msg = jackpotShown ? "JACKPOT!  +" + shownWin + " " + NotchWidgets.coinName() : "You won " + shownWin + " " + NotchWidgets.coinName() + "!";
                NotchWidgets.centerText(ctx, this.font, msg, x + W / 2, y + STATUS_Y,
                        jackpotShown ? NotchTheme.TEXT_GOLD : NotchTheme.TEXT_GREEN, true);
            } else {
                NotchWidgets.centerText(ctx, this.font, "No win - spin again!",
                        x + W / 2, y + STATUS_Y, NotchTheme.TEXT_MUTED, false);
            }
        }

        long bal = spinning ? frozenBalance : (menu.prop(SlotMachineScreenHandler.P_BAL) & 0xFFFFFFFFL);
        NotchWidgets.centerText(ctx, this.font, bal + " " + NotchWidgets.coinName(), x + W / 2, y + BAL_Y, NotchTheme.TEXT_GOLD, true);

        NotchWidgets.divider(ctx, x + 8, y + 102, W - 16);

        ctx.drawString(this.font, "Bet:", x + 16, y + FIELD_Y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + FIELD_X, y + FIELD_Y, FIELD_W, FIELD_H, NotchTheme.DEEP);

        boolean spinHover = !spinning && over(mouseX, mouseY, x + SPIN_X, y + SPIN_Y, SPIN_W, SPIN_H);
        if (spinning) {
            NotchWidgets.neutralButton(ctx, this.font, x + SPIN_X, y + SPIN_Y, SPIN_W, SPIN_H, "Spinning...", false);
        } else {
            NotchWidgets.primaryButton(ctx, this.font, x + SPIN_X, y + SPIN_Y, SPIN_W, SPIN_H, "SPIN", spinHover);
        }
        NotchWidgets.divider(ctx, x + 8, y + 148, W - 16);
        NotchWidgets.centerText(ctx, this.font, "3-in-a-row pays:", x + W / 2, y + 152, NotchTheme.TEXT_MUTED, false);
        SlotSymbol[] syms = SlotSymbol.values();
        for (int i = 0; i < syms.length; i++) {
            int cx = x + 24 + i * 38;
            ctx.renderItem(new ItemStack(syms[i].displayItem()), cx - 8, y + 162);
            int m10 = menu.prop(SlotMachineScreenHandler.P_MULT_BASE + i);
            String mult = "x" + (m10 / 10) + "." + (m10 % 10);
            NotchWidgets.centerText(ctx, this.font, mult, cx, y + 180, NotchTheme.TEXT_DARK, false);
        }
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private int[] computeReels() {
        int count = SlotSymbol.values().length;
        int curSpinId = menu.prop(SlotMachineScreenHandler.P_SPINID);
        int[] finalReels = {
                menu.prop(SlotMachineScreenHandler.P_REEL0),
                menu.prop(SlotMachineScreenHandler.P_REEL1),
                menu.prop(SlotMachineScreenHandler.P_REEL2)
        };
        if (!spinning) return finalReels;

        long now = System.currentTimeMillis();
        boolean fresh = curSpinId != lastSpinIdSeen;
        if (!fresh && now - spinStartMs > MAX_WAIT_MS) {
            spinning = false;
            setError("Spin failed - try again.");
            return finalReels;
        }

        int[] out = new int[3];
        int stopped = 0;
        for (int i = 0; i < 3; i++) {
            long stopAt = spinStartMs + MIN_SPIN_MS + (long) i * REEL_STAGGER_MS;
            if (fresh && now >= stopAt) {
                out[i] = finalReels[i];
                stopped++;
            } else {
                out[i] = (int) ((now / 60 + (long) i * 3) % count);
            }
        }
        if (stopped > announcedStops) {
            for (int i = announcedStops; i < stopped; i++) {
                playSnd(SoundEvents.NOTE_BLOCK_HAT.value(), 0.9f + i * 0.25f);
            }
            announcedStops = stopped;
        }
        if (stopped == 3 && !resultPlayed) {
            resultPlayed = true;
            spinning = false;
            hasShown = true;
            shownWin = menu.prop(SlotMachineScreenHandler.P_LASTWIN) & 0xFFFFFFFFL;
            boolean jackpot = finalReels[0] == finalReels[1] && finalReels[1] == finalReels[2]
                    && finalReels[0] == SlotSymbol.STAR.ordinal();
            if (jackpot) {
                playSnd(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
            } else if (shownWin > 0) {
                playSnd(SoundEvents.PLAYER_LEVELUP, 1.2f);
            } else {
                playSnd(SoundEvents.VILLAGER_NO, 0.9f);
            }
        }
        return out;
    }

    private void drawSymbol(GuiGraphics ctx, int symbolIndex, int cx, int cy) {
        SlotSymbol[] syms = SlotSymbol.values();
        if (symbolIndex < 0 || symbolIndex >= syms.length) symbolIndex = 0;
        ItemStack stack = new ItemStack(syms[symbolIndex].displayItem());
        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, cx - 16, cy - 16);
        net.fugginbeenus.notchcurrency.compat.Render.scaleGui(ctx, 2f, 2f);
        ctx.renderItem(stack, 0, 0);
        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
    }

    private void drawFrame(GuiGraphics ctx, int x, int y) {
        int x1 = x + FRAME_X1, y1 = y + FRAME_Y1, x2 = x + FRAME_X2, y2 = y + FRAME_Y2;
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, NotchTheme.OUTLINE);
        ctx.fill(x1, y1, x2, y2, NotchTheme.ACCENT_GOLD);
        ctx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, NotchTheme.GOLD_LO);
        ctx.fill(x1 + 3, y1 + 3, x2 - 3, y2 - 3, NotchTheme.DEEP);
    }

    private void drawMarquee(GuiGraphics ctx, int x, int y, long now) {
        int bulbs = 11;
        int runner = (int) ((now / 110) % bulbs);
        for (int i = 0; i < bulbs; i++) {
            int bx = x + 41 + i * 12;
            int by = y + MARQUEE_Y;
            int col = ((i + (int) (now / 150)) % 2 == 0) ? NotchTheme.ACCENT_GOLD : NotchTheme.GOLD_LO;
            if (i == runner) col = 0xFFFFF3B0;
            ctx.fill(bx, by, bx + 3, by + 3, col);
        }
    }

    private void drawPaylineMarkers(GuiGraphics ctx, int x, int y, long now) {
        boolean pulse = (now / 400) % 2 == 0;
        int col = pulse ? NotchTheme.ACCENT_GOLD : NotchTheme.GOLD_HI;
        triRight(ctx, x + FRAME_X1 - 8, y + PAYLINE_CY, 5, col);
        triLeft(ctx, x + FRAME_X2 + 4, y + PAYLINE_CY, 5, col);
    }

    private void drawWinGlow(GuiGraphics ctx, int x, int y, long now, boolean jackpot) {
        double s = 0.5 + 0.5 * Math.sin(now / 150.0);
        int a = 0x40 + (int) (0x80 * s);
        int base = jackpot ? 0xFFF3B0 : 0xE0A526;
        int col = (a << 24) | base;
        int x1 = x + FRAME_X1 - 2, y1 = y + FRAME_Y1 - 2, x2 = x + FRAME_X2 + 2, y2 = y + FRAME_Y2 + 2;
        ctx.fill(x1, y1, x2, y1 + 2, col);
        ctx.fill(x1, y2 - 2, x2, y2, col);
        ctx.fill(x1, y1, x1 + 2, y2, col);
        ctx.fill(x2 - 2, y1, x2, y2, col);
    }

    private void triRight(GuiGraphics ctx, int x, int cy, int size, int color) {
        for (int i = 0; i < size; i++) {
            int h = size - 1 - i;
            ctx.fill(x + i, cy - h, x + i + 1, cy + h + 1, color);
        }
    }

    private void triLeft(GuiGraphics ctx, int x, int cy, int size, int color) {
        for (int i = 0; i < size; i++) {
            int h = i;
            ctx.fill(x + i, cy - h, x + i + 1, cy + h + 1, color);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private void setError(String msg) {
        errorMsg = msg;
        errorUntilMs = System.currentTimeMillis() + 2500L;
    }

    private void playSnd(SoundEvent e, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch));
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
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
        if (button == 0 && !spinning
                && over((int) mouseX, (int) mouseY, this.leftPos + SPIN_X, this.topPos + SPIN_Y, SPIN_W, SPIN_H)) {
            NotchWidgets.click();
            if (menu.prop(SlotMachineScreenHandler.P_ENABLED) == 0) {
                setError("Gambling is disabled.");
                playSnd(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f);
                return true;
            }
            long bet = betValue();
            long bal = menu.prop(SlotMachineScreenHandler.P_BAL) & 0xFFFFFFFFL;
            int min = menu.prop(SlotMachineScreenHandler.P_MIN);
            int max = menu.prop(SlotMachineScreenHandler.P_MAX);
            if (bet < min || bet > max) {
                setError("Bet must be " + min + "-" + max + " " + NotchWidgets.coinName() + ".");
                playSnd(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f);
                return true;
            }
            if (bet > bal) {
                setError("Not enough " + NotchWidgets.coinName() + " for that bet.");
                playSnd(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f);
                return true;
            }
            spinning = true;
            spinStartMs = System.currentTimeMillis();
            lastSpinIdSeen = menu.prop(SlotMachineScreenHandler.P_SPINID);
            announcedStops = 0;
            resultPlayed = false;
            hasShown = false;
            frozenBalance = bal;
            errorMsg = null;
            playSnd(SoundEvents.LEVER_CLICK, 0.8f);
            FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
            buf.writeVarLong(bet);
            NetClient.sendToServer(NotchPackets.SLOTS_SPIN, buf);
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private long betValue() {
        try {
            return Long.parseLong(betField.getValue().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, betField)) return true;
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
