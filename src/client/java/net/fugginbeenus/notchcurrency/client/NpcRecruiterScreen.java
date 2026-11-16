package net.fugginbeenus.notchcurrency.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.UUID;

public class NpcRecruiterScreen extends Screen {

    private static final int W = 280, H = 224;
    private static final int PAD = 16;
    private static final int BTN_W = W - PAD * 2;
    private static final int ROW_H = 16;

    private static final ChatFormatting[] COLORS = {
            ChatFormatting.WHITE, ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
            ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE, ChatFormatting.GRAY,
    };

    private enum Mode { VIEW, FOUND, SETTINGS, BANK }

    private final UUID npcId;
    private final String factionId;
    private final String factionName;
    private final int memberCount;
    private final boolean alreadyIn;
    private final boolean canFound;
    private final boolean canManage;
    private final boolean locked;
    private final boolean bank;
    private final long balance;
    private final boolean canDeposit;
    private final boolean canWithdraw;

    private Mode mode;
    private int colorIdx;
    private int fee;
    private boolean openToJoin;
    private final String motto;

    private int px, py;
    private EditBox textField;
    private EditBox amountField;

    public NpcRecruiterScreen(UUID npcId, String factionId, String factionName, ChatFormatting factionColor,
                              int memberCount, boolean alreadyIn, boolean canFound,
                              String motto, int fee, boolean openToJoin, boolean canManage, boolean locked,
                              boolean bank, long balance, boolean canDeposit, boolean canWithdraw, boolean bankPage) {
        super(Component.literal("Recruiter"));
        this.npcId = npcId;
        this.factionId = factionId;
        this.factionName = factionName;
        this.memberCount = memberCount;
        this.alreadyIn = alreadyIn;
        this.canFound = canFound;
        this.canManage = canManage;
        this.locked = locked;
        this.motto = motto == null ? "" : motto;
        this.fee = fee;
        this.openToJoin = openToJoin;
        this.bank = bank;
        this.balance = balance;
        this.canDeposit = canDeposit;
        this.canWithdraw = canWithdraw;
        this.mode = bank && bankPage ? Mode.BANK : factionId.isEmpty() && canFound ? Mode.FOUND : Mode.VIEW;
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == factionColor) { colorIdx = i; break; }
        }
    }

    private boolean hasFaction() { return !factionId.isEmpty(); }
    private ChatFormatting color() { return COLORS[colorIdx]; }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        if (mode == Mode.FOUND || mode == Mode.SETTINGS) {
            boolean founding = mode == Mode.FOUND;
            textField = new EditBox(this.font, px + PAD + 4, py + 55, BTN_W - 8, 10,
                    Component.literal(founding ? "Name" : "Motto"));
            textField.setMaxLength(founding ? 32 : 64);
            textField.setBordered(false);
            textField.setHint(Component.literal(founding ? "name your faction" : "a line about it")
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (!founding) textField.setValue(motto);
            addRenderableWidget(textField);
            setInitialFocus(textField);
        }
        if (mode == Mode.BANK) {
            amountField = new EditBox(this.font, px + PAD + 4, py + 101, BTN_W - 8, 10, Component.literal("Amount"));
            amountField.setMaxLength(12);
            amountField.setBordered(false);
            amountField.setHint(Component.literal("how many").withStyle(ChatFormatting.DARK_GRAY));
            addRenderableWidget(amountField);
            setInitialFocus(amountField);
        }
    }

    private int bankButtonY() { return py + H - 70; }
    private int bankActionY() { return py + 124; }

    private int rowLabel() { return py + 38; }
    private int rowColor() { return py + 76; }
    private int rowFee() { return py + 98; }
    private int rowOpen() { return py + 120; }
    private int rowConfirm() { return py + 148; }

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
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font,
                mode == Mode.FOUND ? "New Faction" : mode == Mode.SETTINGS ? "Faction Settings"
                        : mode == Mode.BANK ? "Faction Bank" : "Recruiter",
                px + W / 2, py + 8);

        switch (mode) {
            case VIEW -> renderView(ctx, mouseX, mouseY);
            case FOUND, SETTINGS -> renderEditor(ctx, mouseX, mouseY);
            case BANK -> renderBank(ctx, mouseX, mouseY);
        }

        NotchWidgets.neutralButton(ctx, this.font, px + PAD, py + H - 26, BTN_W, ROW_H,
                mode == Mode.SETTINGS || mode == Mode.BANK ? "Back" : "Close",
                over(mouseX, mouseY, px + PAD, py + H - 26, BTN_W, ROW_H));
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
    }

    private void renderView(GuiGraphics ctx, int mx, int my) {
        if (!hasFaction()) {
            NotchWidgets.centerText(ctx, this.font, "This recruiter has no faction yet.",
                    px + W / 2, py + 60, NotchTheme.TEXT_DARK, false);
            NotchWidgets.centerText(ctx, this.font, "Come back once it's signed up.",
                    px + W / 2, py + 74, NotchTheme.TEXT_MUTED, false);
            return;
        }

        ctx.drawCenteredString(this.font,
                Component.literal(factionName).withStyle(color()), px + W / 2, py + 30, 0xFFFFFF);
        if (!motto.isBlank()) {
            NotchWidgets.centerText(ctx, this.font,
                    this.font.plainSubstrByWidth(motto, W - PAD * 2),
                    px + W / 2, py + 46, NotchTheme.TEXT_MUTED, false);
        }
        NotchWidgets.centerText(ctx, this.font,
                memberCount == 1 ? "1 member" : memberCount + " members",
                px + W / 2, py + 62, NotchTheme.TEXT_DARK, false);

        int y = py + 84;
        if (alreadyIn) {
            NotchWidgets.centerText(ctx, this.font, "You're one of them.",
                    px + W / 2, y, NotchTheme.TEXT_DARK, false);
            if (locked) {
                NotchWidgets.centerText(ctx, this.font, "Membership is permanent.",
                        px + W / 2, y + 20, NotchTheme.TEXT_MUTED, false);
            } else {
                NotchWidgets.dangerButton(ctx, this.font, px + PAD, y + 16, BTN_W, ROW_H, "Leave",
                        over(mx, my, px + PAD, y + 16, BTN_W, ROW_H));
            }
        } else if (locked) {
            NotchWidgets.centerText(ctx, this.font, "You're with another faction.",
                    px + W / 2, y + 4, NotchTheme.TEXT_MUTED, false);
        } else if (!openToJoin) {
            NotchWidgets.centerText(ctx, this.font, "Not taking new members.",
                    px + W / 2, y + 4, NotchTheme.TEXT_MUTED, false);
        } else {
            String label = fee > 0 ? "Join - " + fee + " " + NotchWidgets.coinName() : "Join";
            NotchWidgets.primaryButton(ctx, this.font, px + PAD, y, BTN_W, 18, label,
                    over(mx, my, px + PAD, y, BTN_W, 18));
        }

        if (bank) {
            String label = "Bank";
            NotchWidgets.neutralButton(ctx, this.font, px + PAD, bankButtonY(), BTN_W, ROW_H, label,
                    over(mx, my, px + PAD, bankButtonY(), BTN_W, ROW_H));
            int cw = NotchWidgets.coinFieldWidth(this.font, balance);
            NotchWidgets.coinField(ctx, this.font, NotchWidgets.coinStack(), px + PAD + BTN_W - cw - 4,
                    bankButtonY() - 1, balance, NotchWidgets.COIN_GOLD);
        }
        if (canManage) {
            NotchWidgets.neutralButton(ctx, this.font, px + PAD, py + H - 48, BTN_W, ROW_H, "Settings",
                    over(mx, my, px + PAD, py + H - 48, BTN_W, ROW_H));
        }
    }

    private void renderBank(GuiGraphics ctx, int mx, int my) {
        ctx.drawCenteredString(this.font,
                Component.literal(factionName).withStyle(color()), px + W / 2, py + 30, 0xFFFFFF);
        NotchWidgets.centerText(ctx, this.font, "In the bank", px + W / 2, py + 50, NotchTheme.TEXT_MUTED, false);
        int cw = NotchWidgets.coinFieldWidth(this.font, balance);
        NotchWidgets.coinField(ctx, this.font, NotchWidgets.coinStack(), px + W / 2 - cw / 2, py + 60, balance,
                NotchWidgets.COIN_GOLD);

        ctx.drawString(this.font, "Amount:", px + PAD, py + 86, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + PAD, py + 98, BTN_W, 15, NotchTheme.DEEP);

        int y = bankActionY();
        if (canDeposit && canWithdraw) {
            int half = (BTN_W - 6) / 2;
            NotchWidgets.primaryButton(ctx, this.font, px + PAD, y, half, 18, "Deposit",
                    over(mx, my, px + PAD, y, half, 18));
            NotchWidgets.goldButton(ctx, this.font, px + PAD + half + 6, y, half, 18, "Withdraw",
                    over(mx, my, px + PAD + half + 6, y, half, 18));
        } else if (canDeposit) {
            NotchWidgets.primaryButton(ctx, this.font, px + PAD, y, BTN_W, 18, "Deposit",
                    over(mx, my, px + PAD, y, BTN_W, 18));
        } else if (canWithdraw) {
            NotchWidgets.goldButton(ctx, this.font, px + PAD, y, BTN_W, 18, "Withdraw",
                    over(mx, my, px + PAD, y, BTN_W, 18));
        }
        String hint = canDeposit && canWithdraw ? "Deposit takes from your wallet. Withdraw pays into it."
                : canDeposit ? "Deposit takes the coins from your wallet."
                : "Withdraw puts the coins in your wallet.";
        NotchWidgets.centerText(ctx, this.font, hint, px + W / 2, y + 24, NotchTheme.TEXT_MUTED, false);
    }

    private void renderEditor(GuiGraphics ctx, int mx, int my) {
        boolean founding = mode == Mode.FOUND;
        ctx.drawString(this.font, founding ? "Name:" : "Motto:", px + PAD, rowLabel(),
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + PAD, py + 52, BTN_W, 15, NotchTheme.DEEP);

        ctx.drawString(this.font, "Colour:", px + PAD, rowColor() + 4, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + 76, rowColor(), 100, 15,
                titleCase(net.fugginbeenus.notchcurrency.compat.Colors.name(color())), over(mx, my, px + 76, rowColor(), 100, 15));
        ctx.drawCenteredString(this.font, Component.literal("Aa").withStyle(color()),
                px + 200, rowColor() + 4, 0xFFFFFF);

        ctx.drawString(this.font, "Join fee:", px + PAD, rowFee() + 4, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + 76, rowFee(), 18, 15, "-",
                over(mx, my, px + 76, rowFee(), 18, 15));
        NotchWidgets.centerText(ctx, this.font,
                fee == 0 ? "free" : fee + " " + NotchWidgets.coinName(),
                px + 140, rowFee() + 4, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + 186, rowFee(), 18, 15, "+",
                over(mx, my, px + 186, rowFee(), 18, 15));

        ctx.drawString(this.font, "Joining:", px + PAD, rowOpen() + 4, NotchTheme.TEXT_DARK, false);
        String openLabel = openToJoin ? "Anyone may join" : "Closed";
        if (openToJoin) {
            NotchWidgets.primaryButton(ctx, this.font, px + 76, rowOpen(), 128, 15, openLabel,
                    over(mx, my, px + 76, rowOpen(), 128, 15));
        } else {
            NotchWidgets.neutralButton(ctx, this.font, px + 76, rowOpen(), 128, 15, openLabel,
                    over(mx, my, px + 76, rowOpen(), 128, 15));
        }

        NotchWidgets.primaryButton(ctx, this.font, px + PAD, rowConfirm(), BTN_W, 18,
                founding ? "Found faction" : "Save",
                over(mx, my, px + PAD, rowConfirm(), BTN_W, 18));
        NotchWidgets.centerText(ctx, this.font,
                founding ? "You'll lead it, and this NPC will recruit for it."
                        : "Members keep their place; only the sign changes.",
                px + W / 2, rowConfirm() + 22, NotchTheme.TEXT_MUTED, false);
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
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) mouseX, my = (int) mouseY;

            if (mode == Mode.VIEW) {
                if (hasFaction() && alreadyIn && !locked && over(mx, my, px + PAD, py + 100, BTN_W, ROW_H)) {
                    NotchWidgets.click();
                    send(RecruiterManager.ACTION_LEAVE, "");
                    return true;
                }
                if (hasFaction() && !alreadyIn && !locked && openToJoin && over(mx, my, px + PAD, py + 84, BTN_W, 18)) {
                    NotchWidgets.click();
                    send(RecruiterManager.ACTION_JOIN, "");
                    return true;
                }
                if (canManage && over(mx, my, px + PAD, py + H - 48, BTN_W, ROW_H)) {
                    NotchWidgets.click();
                    mode = Mode.SETTINGS;
                    rebuildPane();
                    return true;
                }
                if (bank && over(mx, my, px + PAD, bankButtonY(), BTN_W, ROW_H)) {
                    NotchWidgets.click();
                    mode = Mode.BANK;
                    rebuildPane();
                    return true;
                }
            } else if (mode == Mode.BANK) {
                int y = bankActionY();
                int half = (BTN_W - 6) / 2;
                boolean both = canDeposit && canWithdraw;
                boolean depositHit = canDeposit && over(mx, my, px + PAD, y, both ? half : BTN_W, 18);
                boolean withdrawHit = canWithdraw && (both
                        ? over(mx, my, px + PAD + half + 6, y, half, 18)
                        : !canDeposit && over(mx, my, px + PAD, y, BTN_W, 18));
                if (depositHit || withdrawHit) {
                    String amount = amountField == null ? "" : amountField.getValue().trim();
                    if (!amount.matches("[0-9]{1,12}")) return true;
                    NotchWidgets.click();
                    send(depositHit ? RecruiterManager.ACTION_DEPOSIT : RecruiterManager.ACTION_WITHDRAW, amount);
                    return true;
                }
            } else {
                if (over(mx, my, px + 76, rowColor(), 100, 15)) {
                    NotchWidgets.tick();
                    colorIdx = (colorIdx + 1) % COLORS.length;
                    return true;
                }
                if (over(mx, my, px + 76, rowFee(), 18, 15)) {
                    NotchWidgets.tick();
                    fee = Math.max(0, fee - feeStep());
                    return true;
                }
                if (over(mx, my, px + 186, rowFee(), 18, 15)) {
                    NotchWidgets.tick();
                    fee = Math.min(1_000_000, fee + feeStep());
                    return true;
                }
                if (over(mx, my, px + 76, rowOpen(), 128, 15)) {
                    NotchWidgets.tick();
                    openToJoin = !openToJoin;
                    return true;
                }
                if (over(mx, my, px + PAD, rowConfirm(), BTN_W, 18)) {
                    String text = textField == null ? "" : textField.getValue().trim();
                    if (mode == Mode.FOUND && text.isBlank()) return true; // the server would refuse anyway
                    NotchWidgets.click();
                    send(mode == Mode.FOUND ? RecruiterManager.ACTION_FOUND : RecruiterManager.ACTION_SETTINGS,
                            text);
                    return true;
                }
            }

            if (over(mx, my, px + PAD, py + H - 26, BTN_W, ROW_H)) {
                NotchWidgets.click();
                if (mode == Mode.SETTINGS || mode == Mode.BANK) {
                    mode = Mode.VIEW;
                    rebuildPane();
                } else {
                    this.onClose();
                }
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private int feeStep() {
        return net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 100 : 10;
    }

    private void send(int action, String text) {
        NotchPacketsClient.sendRecruiterAction(npcId, action, text, net.fugginbeenus.notchcurrency.compat.Colors.name(color()), fee, openToJoin);
    }

    private void rebuildPane() {
        this.clearWidgets();
        this.init();
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, textField, amountField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    private static String titleCase(String s) {
        String cleaned = s.replace('_', ' ');
        return cleaned.isEmpty() ? cleaned
                : Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() { return false; }

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
