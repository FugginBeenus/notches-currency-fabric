package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.mail.MailLayout;
import net.fugginbeenus.notchcurrency.mail.MailPostScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MailPostScreen extends AbstractContainerScreen<MailPostScreenHandler> {

    public record Recipient(UUID id, String name, boolean online) {}
    private static final int EDGE = 8, INNER_W = MailLayout.W - EDGE * 2;
    private static final int TO_Y = 42, TO_H = 16;
    private static final int COINS_Y = 84, FIELD_H = 14;
    private static final int NOTE_Y = 102;
    private static final int SEND_Y = 120, SEND_H = 16;
    private static final int TRADE_W = 48, SPLIT_GAP = 4;
    private static final int DROP_Y = 36, DROP_H = MailLayout.CONTENT_BOTTOM - DROP_Y;
    private static final int SEARCH_Y = 40, SEARCH_H = 12;
    private static final int ROW_Y = 56, ROW_H = 15, VISIBLE = 4;
    private static List<Recipient> knownRecipients = List.of();
    private static UUID preselected;
    private List<Recipient> shown = new ArrayList<>();
    private UUID chosen;
    private int scroll;
    private boolean picking;
    private EditBox search;
    private EditBox note;
    private EditBox coins;
    private long hintedBalance = -1;

    public MailPostScreen(MailPostScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, MailLayout.W, MailLayout.H);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = MailLayout.W;
        this.imageHeight = MailLayout.H;
        //?}
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    public static void setRecipients(List<Recipient> recipients) {
        knownRecipients = recipients;
    }
    public static void preselect(UUID recipient) {
        preselected = recipient;
    }

    @Override
    protected void init() {
        super.init();
        if (chosen == null && preselected != null) {
            chosen = preselected;
            preselected = null;
        }

        String oldSearch = search == null ? "" : search.getValue();
        search = new EditBox(this.font, this.leftPos + EDGE + 5, this.topPos + SEARCH_Y + 2,
                INNER_W - 10, 10, Component.literal("Search"));
        search.setMaxLength(16);
        search.setBordered(false);
        search.setHint(Component.literal("search...").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(oldSearch);
        search.setResponder(value -> refilter());
        addRenderableWidget(search);

        String oldCoins = coins == null ? "" : coins.getValue();
        coins = new EditBox(this.font, this.leftPos + EDGE + 4, this.topPos + COINS_Y + 3,
                INNER_W - 8, 10, Component.literal("Coins"));
        coins.setMaxLength(12);
        coins.setBordered(false);
        coins.setResponder(value -> {
            StringBuilder digits = new StringBuilder();
            for (char c : value.toCharArray()) {
                if (Character.isDigit(c)) digits.append(c);
            }
            if (digits.length() != value.length()) coins.setValue(digits.toString());
        });
        coins.setValue(oldCoins);
        addRenderableWidget(coins);

        String oldNote = note == null ? "" : note.getValue();
        note = new EditBox(this.font, this.leftPos + EDGE + 4, this.topPos + NOTE_Y + 3,
                INNER_W - 8, 10, Component.literal("Note"));
        note.setMaxLength(80);
        note.setBordered(false);
        note.setHint(Component.literal("add a note (optional)").withStyle(ChatFormatting.DARK_GRAY));
        note.setValue(oldNote);
        addRenderableWidget(note);

        this.menu.parcelSlotsHidden = picking;
        showRightFields();
        refilter();
    }

    private void showRightFields() {
        if (search != null) search.visible = picking;
        if (note != null) note.visible = !picking;
        if (coins != null) coins.visible = !picking;
    }

    private void refilter() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).strip();
        shown = new ArrayList<>();
        for (Recipient r : knownRecipients) {
            if (query.isEmpty() || r.name().toLowerCase(Locale.ROOT).contains(query)) shown.add(r);
        }
        shown.sort((a, b) -> {
            if (a.online() != b.online()) return a.online() ? -1 : 1;
            return a.name().compareToIgnoreCase(b.name());
        });
        scroll = 0;
    }

    private int rowY(int i) { return this.topPos + ROW_Y + i * ROW_H; }
    private int toY() { return this.topPos + TO_Y; }
    private int sendY() { return this.topPos + SEND_Y; }
    private boolean canTrade() {
        if (chosen == null) return false;
        for (Recipient r : knownRecipients) {
            if (r.id().equals(chosen)) return r.online();
        }
        return false;
    }

    private int sendW() { return canTrade() ? INNER_W - TRADE_W - SPLIT_GAP : INNER_W; }
    private int tradeX() { return this.leftPos + MailLayout.W - EDGE - TRADE_W; }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, MailLayout.W, MailLayout.H);
        NotchWidgets.title(ctx, this.font, this.title.getString(), x + MailLayout.W / 2, y + 6);
        MailTabs.draw(ctx, this.font, x, y, MailTabs.OUTBOX, mouseX, mouseY);

        refreshCoinsHint();
        drawParcelSide(ctx, x, y, mouseX, mouseY);
        MailPostScreen.drawInventory(ctx, x, y, this.font);
        if (picking) drawPicker(ctx, x, y, mouseX, mouseY);
        //? if >=26.1 {
        /*super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private void refreshCoinsHint() {
        long balance = NotchHud.getBalance();
        if (coins == null || balance == hintedBalance) return;
        hintedBalance = balance;
        coins.setHint(Component.literal("coins to send (you have "
                + NotchWidgets.compactCount(balance) + ")").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void drawParcelSide(GuiGraphics ctx, int x, int y, int mouseX, int mouseY) {
        boolean toHover = !picking && over(mouseX, mouseY, x + EDGE, toY(), INNER_W, TO_H);
        if (chosen == null) {
            NotchWidgets.neutralButton(ctx, this.font, x + EDGE, toY(), INNER_W, TO_H,
                    "Choose a mailbox", toHover);
        } else {
            NotchWidgets.button(ctx, x + EDGE, toY(), INNER_W, TO_H, toHover, false);
            ctx.drawString(this.font, "To  " + nameOf(chosen), x + EDGE + 6, toY() + 4,
                    NotchTheme.TEXT_DARK, false);
        }
        NotchWidgets.triangle(ctx, x + MailLayout.W - EDGE - 8, toY() + TO_H / 2, false,
                NotchTheme.TEXT_DARK);

        for (int i = 0; i < MailPostScreenHandler.PARCEL_SLOTS; i++) {
            NotchWidgets.slot(ctx, x + MailPostScreenHandler.PARCEL_X + i * 18 - 1,
                    y + MailPostScreenHandler.PARCEL_Y - 1);
        }

        NotchWidgets.inset(ctx, x + EDGE, y + COINS_Y, INNER_W, FIELD_H, NotchTheme.DEEP);
        NotchWidgets.inset(ctx, x + EDGE, y + NOTE_Y, INNER_W, FIELD_H, NotchTheme.DEEP);

        int sendW = sendW();
        boolean sendHover = !picking && over(mouseX, mouseY, x + EDGE, sendY(), sendW, SEND_H);
        if (chosen == null) {
            NotchWidgets.neutralButton(ctx, this.font, x + EDGE, sendY(), INNER_W, SEND_H,
                    "Pick a mailbox first", false);
        } else {
            NotchWidgets.primaryButton(ctx, this.font, x + EDGE, sendY(), sendW, SEND_H,
                    fit("Send to " + nameOf(chosen), sendW - 8), sendHover);
        }
        if (canTrade()) {
            NotchWidgets.goldButton(ctx, this.font, tradeX(), sendY(), TRADE_W, SEND_H, "Trade",
                    !picking && over(mouseX, mouseY, tradeX(), sendY(), TRADE_W, SEND_H));
        }
    }

    private void drawPicker(GuiGraphics ctx, int x, int y, int mouseX, int mouseY) {
        NotchWidgets.panel(ctx, x + EDGE - 2, y + DROP_Y, INNER_W + 4, DROP_H);
        NotchWidgets.inset(ctx, x + EDGE, y + SEARCH_Y, INNER_W, SEARCH_H, NotchTheme.DEEP);

        if (shown.isEmpty()) {
            String line = knownRecipients.isEmpty() ? "Nobody has a mailbox yet." : "No match.";
            NotchWidgets.centerText(ctx, this.font, line, x + MailLayout.W / 2, y + ROW_Y + 12,
                    NotchTheme.TEXT_MUTED, false);
            return;
        }

        for (int i = 0; i < VISIBLE && i + scroll < shown.size(); i++) {
            Recipient r = shown.get(i + scroll);
            boolean selected = r.id().equals(chosen);
            boolean hover = over(mouseX, mouseY, x + EDGE, rowY(i), INNER_W, ROW_H - 1);
            if (selected) {
                NotchWidgets.primaryButton(ctx, this.font, x + EDGE, rowY(i), INNER_W, ROW_H - 1, "", hover);
            } else {
                NotchWidgets.button(ctx, x + EDGE, rowY(i), INNER_W, ROW_H - 1, hover, false);
            }
            ctx.drawString(this.font, fit(r.name(), INNER_W - 24), x + EDGE + 5, rowY(i) + 3,
                    selected ? NotchTheme.TEXT_LIGHT : NotchTheme.TEXT_DARK, false);
            if (r.online()) {
                ctx.drawString(this.font, "online", x + MailLayout.W - EDGE - 30, rowY(i) + 3,
                        0xFF6AC46A, false);
            }
        }
        if (shown.size() > VISIBLE) {
            NotchWidgets.centerText(ctx, this.font,
                    (scroll + 1) + "-" + Math.min(shown.size(), scroll + VISIBLE) + " of " + shown.size(),
                    x + MailLayout.W / 2, y + ROW_Y + VISIBLE * ROW_H + 3, NotchTheme.TEXT_MUTED, false);
        }
    }

    static void drawInventory(GuiGraphics ctx, int x, int y, Font font) {
        ctx.drawString(font, "Inventory", x + MailLayout.INV_X, y + MailLayout.INV_LABEL_Y,
                NotchTheme.TEXT_DARK, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + MailLayout.INV_X + col * 18 - 1,
                        y + MailLayout.INV_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + MailLayout.INV_X + col * 18 - 1, y + MailLayout.HOTBAR_Y - 1);
        }
    }

    private long coinsTyped() {
        String typed = coins == null ? "" : coins.getValue().strip();
        if (typed.isEmpty()) return 0L;
        try {
            return Long.parseLong(typed);
        } catch (NumberFormatException tooBig) {
            return 0L;
        }
    }

    private String nameOf(UUID id) {
        if (id == null) return "";
        for (Recipient r : knownRecipients) {
            if (r.id().equals(id)) return r.name();
        }
        return "them";
    }

    private String fit(String text, int room) {
        return this.font.width(text) <= room ? text : this.font.plainSubstrByWidth(text, room - 6) + "..";
    }

    private void setPicking(boolean open) {
        picking = open;
        this.menu.parcelSlotsHidden = open;
        showRightFields();
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
            if (picking) {
                for (int i = 0; i < VISIBLE && i + scroll < shown.size(); i++) {
                    if (over(mx, my, this.leftPos + EDGE, rowY(i), INNER_W, ROW_H - 1)) {
                        NotchWidgets.click();
                        chosen = shown.get(i + scroll).id();
                        setPicking(false);
                        return true;
                    }
                }

                if (!over(mx, my, this.leftPos + EDGE - 2, this.topPos + DROP_Y, INNER_W + 4, DROP_H)) {
                    setPicking(false);
                    return true;
                }
            } else {
                if (MailTabs.click(this.leftPos, this.topPos, MailTabs.OUTBOX, mx, my)) return true;
                if (over(mx, my, this.leftPos + EDGE, toY(), INNER_W, TO_H)) {
                    NotchWidgets.click();
                    setPicking(true);
                    this.setFocused(search);
                    search.setFocused(true);
                    return true;
                }
                if (canTrade() && over(mx, my, tradeX(), sendY(), TRADE_W, SEND_H)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendMailTrade(chosen);
                    return true;
                }
                if (chosen != null && over(mx, my, this.leftPos + EDGE, sendY(), sendW(), SEND_H)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendMailPost(chosen, note == null ? "" : note.getValue(),
                            coinsTyped());
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

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        if (!picking) return false;
        int maxScroll = Math.max(0, shown.size() - VISIBLE);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount)));
        return true;
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
        //? if >=26.1 {
        /*extractTooltip(ctx, mouseX, mouseY);
        *///?} else {
        renderTooltip(ctx, mouseX, mouseY);
        //?}
    }


    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, note, coins, search)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    @Override
    public void removed() {
        super.removed();
        MailTabs.screenClosed();
    }
}
