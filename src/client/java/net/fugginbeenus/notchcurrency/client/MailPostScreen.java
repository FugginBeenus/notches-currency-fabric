package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.mail.MailPostScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Making up a parcel: who it goes to on the left, what is in it on the right.
 *
 * <p>Picking from a list rather than typing a name, because a mistyped name is a parcel that goes
 * nowhere and the sender has no way to tell. Anyone who has claimed a mailbox is on the list.
 */
public class MailPostScreen extends AbstractContainerScreen<MailPostScreenHandler> {

    /** A player who has a mailbox, as the server described them. */
    public record Recipient(UUID id, String name, boolean online) {}

    private static final int W = 296, H = 232;
    private static final int LIST_X = 8, LIST_Y = 40, LIST_W = 100, ROW_H = 14, VISIBLE = 6;

    // Filled by the recipients packet, which arrives just after the screen opens.
    private static List<Recipient> knownRecipients = List.of();
    private static UUID preselected;

    private List<Recipient> shown = new ArrayList<>();
    private UUID chosen;
    private int scroll;

    private EditBox search;
    private EditBox note;

    public MailPostScreen(MailPostScreenHandler handler, Inventory inv, Component title) {
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

    /** Handed over by the packet, since a menu screen is built by Minecraft rather than by us. */
    public static void setRecipients(List<Recipient> recipients) {
        knownRecipients = recipients;
    }

    /** Opening someone else's mailbox aims the parcel at them without having to hunt the list. */
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
        search = new EditBox(this.font, this.leftPos + LIST_X + 4, this.topPos + 26,
                LIST_W - 8, 10, Component.literal("Search"));
        search.setMaxLength(16);
        search.setBordered(false);
        search.setHint(Component.literal("search...").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(oldSearch);
        search.setResponder(value -> refilter());
        addRenderableWidget(search);

        String oldNote = note == null ? "" : note.getValue();
        note = new EditBox(this.font, this.leftPos + 120, this.topPos + 116, 160, 10,
                Component.literal("Note"));
        note.setMaxLength(80);
        note.setBordered(false);
        note.setHint(Component.literal("say something (optional)").withStyle(ChatFormatting.DARK_GRAY));
        note.setValue(oldNote);
        addRenderableWidget(note);

        refilter();
    }

    private void refilter() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).strip();
        shown = new ArrayList<>();
        for (Recipient r : knownRecipients) {
            if (query.isEmpty() || r.name().toLowerCase(Locale.ROOT).contains(query)) shown.add(r);
        }
        shown.sort((a, b) -> {
            if (a.online() != b.online()) return a.online() ? -1 : 1; // people who are here, first
            return a.name().compareToIgnoreCase(b.name());
        });
        scroll = 0;
    }

    private int rowY(int i) {
        return this.topPos + LIST_Y + i * ROW_H;
    }

    private int sendX() { return this.leftPos + 120; }
    private int sendY() { return this.topPos + 130; }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Post a Parcel", x + W / 2, y + 8);

        // Left: who it is going to.
        ctx.drawString(this.font, "Mailboxes", x + LIST_X + 2, y + 16, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + LIST_X, y + 23, LIST_W, 14, NotchTheme.DEEP);
        NotchWidgets.inset(ctx, x + LIST_X, y + LIST_Y - 2, LIST_W, VISIBLE * ROW_H + 4, NotchTheme.PANEL_MID);

        if (knownRecipients.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "Nobody has a", x + LIST_X + LIST_W / 2,
                    y + LIST_Y + 16, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "mailbox yet.", x + LIST_X + LIST_W / 2,
                    y + LIST_Y + 26, NotchTheme.TEXT_MUTED, false);
        }
        for (int i = 0; i < VISIBLE && i + scroll < shown.size(); i++) {
            Recipient r = shown.get(i + scroll);
            boolean selected = r.id().equals(chosen);
            boolean hover = over(mouseX, mouseY, x + LIST_X + 2, rowY(i), LIST_W - 4, ROW_H - 2);
            if (selected) {
                NotchWidgets.primaryButton(ctx, this.font, x + LIST_X + 2, rowY(i), LIST_W - 4, ROW_H - 2, "", hover);
            } else {
                NotchWidgets.button(ctx, x + LIST_X + 2, rowY(i), LIST_W - 4, ROW_H - 2, hover, false);
            }
            ctx.drawString(this.font, fit(r.name(), LIST_W - 16), x + LIST_X + 6, rowY(i) + 3,
                    selected ? NotchTheme.TEXT_LIGHT : NotchTheme.TEXT_DARK, false);
            if (r.online()) {
                ctx.drawString(this.font, "*", x + LIST_X + LIST_W - 10, rowY(i) + 3, 0xFF6AC46A, false);
            }
        }
        if (shown.size() > VISIBLE) {
            NotchWidgets.centerText(ctx, this.font,
                    (scroll + 1) + "-" + Math.min(shown.size(), scroll + VISIBLE) + " of " + shown.size(),
                    x + LIST_X + LIST_W / 2, y + LIST_Y + VISIBLE * ROW_H + 6, NotchTheme.TEXT_MUTED, false);
        }

        // Right: the parcel itself.
        ctx.drawString(this.font, "Parcel", x + 120, y + 30, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < MailPostScreenHandler.PARCEL_SLOTS; i++) {
            NotchWidgets.slot(ctx, x + 200 + (i % 2) * 18 - 1, y + 40 + (i / 2) * 18 - 1);
        }
        NotchWidgets.centerText(ctx, this.font, "Drop items in, then send.",
                x + 160, y + 46, NotchTheme.TEXT_MUTED, false);

        ctx.drawString(this.font, "Note:", x + 120, y + 105, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 118, y + 113, 164, 14, NotchTheme.DEEP);

        String to = nameOf(chosen);
        boolean ready = chosen != null;
        if (ready) {
            NotchWidgets.primaryButton(ctx, this.font, sendX(), sendY(), 164, 16, "Send to " + to,
                    over(mouseX, mouseY, sendX(), sendY(), 164, 16));
        } else {
            NotchWidgets.neutralButton(ctx, this.font, sendX(), sendY(), 164, 16, "Pick a mailbox", false);
        }

        ctx.drawString(this.font, "Inventory", x + 118, y + 140, NotchTheme.TEXT_DARK, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + 118 + col * 18 - 1, y + 150 + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + 118 + col * 18 - 1, y + 208 - 1);
        }
        //? if >=26.1 {
        /*super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
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
            for (int i = 0; i < VISIBLE && i + scroll < shown.size(); i++) {
                if (over(mx, my, this.leftPos + LIST_X + 2, rowY(i), LIST_W - 4, ROW_H - 2)) {
                    NotchWidgets.click();
                    chosen = shown.get(i + scroll).id();
                    return true;
                }
            }
            if (chosen != null && over(mx, my, sendX(), sendY(), 164, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendMailPost(chosen, note == null ? "" : note.getValue());
                return true;
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
}
