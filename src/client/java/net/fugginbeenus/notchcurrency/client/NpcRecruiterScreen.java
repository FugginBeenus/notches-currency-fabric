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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NpcRecruiterScreen extends Screen {

    private static final int W = 280, H = 224;
    private static final int PAD = 16;
    private static final int BTN_W = W - PAD * 2;
    private static final int ROW_H = 16;
    private static final int LIST_Y = 44, LIST_ROW = 20, VISIBLE = 5;
    private static final int ACT_W = 56, EDIT_W = 34;
    private static final long CONFIRM_MS = 3000L;

    private static final ChatFormatting[] COLORS = {
            ChatFormatting.WHITE, ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
            ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE, ChatFormatting.GRAY,
    };

    public record House(String id, String name, int members, int fee, boolean open, long cost, int status,
                        boolean canEdit, String leader, List<String> memberNames) {}

    private enum Mode { VIEW, FOUND, SETTINGS, BANK, HOUSES, HOUSE_NEW, HOUSE_EDIT }

    private final UUID npcId;
    private final String factionId;
    private final String factionName;
    private final ChatFormatting factionColor;
    private final int memberCount;
    private final boolean alreadyIn;
    private final boolean canFound;
    private final boolean canManage;
    private final boolean locked;
    private final boolean bank;
    private final long balance;
    private final boolean canDeposit;
    private final boolean canWithdraw;
    private final boolean isHouse;
    private final boolean canMakeHouses;
    private final boolean runsHouses;
    private final List<House> houses;

    private Mode mode;
    private int colorIdx;
    private int fee;
    private boolean openToJoin;
    private final String motto;
    private House editing;
    private List<String> leaderChoices = List.of("");
    private int leaderIdx;
    private int scroll;
    private long endArmedAt;

    private int px, py;
    private EditBox textField;
    private EditBox amountField;

    public NpcRecruiterScreen(UUID npcId, String factionId, String factionName, ChatFormatting factionColor,
                              int memberCount, boolean alreadyIn, boolean canFound,
                              String motto, int fee, boolean openToJoin, boolean canManage, boolean locked,
                              boolean bank, long balance, boolean canDeposit, boolean canWithdraw, int page,
                              boolean isHouse, boolean canMakeHouses, boolean runsHouses, List<House> houses) {
        super(Component.literal("Recruiter"));
        this.npcId = npcId;
        this.factionId = factionId;
        this.factionName = factionName;
        this.factionColor = factionColor;
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
        this.isHouse = isHouse;
        this.canMakeHouses = canMakeHouses;
        this.runsHouses = runsHouses;
        this.houses = houses;
        this.mode = bank && page == RecruiterManager.PAGE_BANK ? Mode.BANK
                : page == RecruiterManager.PAGE_HOUSES && !isHouse && !factionId.isEmpty() ? Mode.HOUSES
                : factionId.isEmpty() && canFound ? Mode.FOUND : Mode.VIEW;
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == factionColor) { colorIdx = i; break; }
        }
    }

    private boolean hasFaction() { return !factionId.isEmpty(); }
    private ChatFormatting color() { return COLORS[colorIdx]; }

    private boolean showsHousesButton() {
        return hasFaction() && !isHouse && (!houses.isEmpty() || canMakeHouses);
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        if (mode == Mode.FOUND || mode == Mode.SETTINGS || mode == Mode.HOUSE_NEW) {
            boolean naming = mode != Mode.SETTINGS;
            textField = new EditBox(this.font, px + PAD + 4, py + 55, BTN_W - 8, 10,
                    Component.literal(naming ? "Name" : "Motto"));
            textField.setMaxLength(naming ? 32 : 64);
            textField.setBordered(false);
            textField.setHint(Component.literal(mode == Mode.HOUSE_NEW ? "name your House"
                    : naming ? "name your faction" : "a line about it").withStyle(ChatFormatting.DARK_GRAY));
            if (!naming) textField.setValue(motto);
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
    private int housesButtonY() { return py + H - 92; }
    private int bankActionY() { return py + 124; }

    private int rowLabel() { return py + 38; }
    private int rowColor() { return py + 76; }
    private int rowFee() { return py + 98; }
    private int rowOpen() { return py + 120; }
    private int rowConfirm() { return py + 148; }

    private int listRowY(int i) { return py + LIST_Y + i * LIST_ROW; }
    private int actX() { return px + PAD + BTN_W - ACT_W - 1; }
    private int editX() { return actX() - EDIT_W - 2; }
    private int pagerY() { return py + LIST_Y + VISIBLE * LIST_ROW + 2; }

    private String title() {
        return switch (mode) {
            case FOUND -> "New Faction";
            case SETTINGS -> "Faction Settings";
            case BANK -> isHouse ? "House Bank" : "Faction Bank";
            case HOUSES -> "Houses";
            case HOUSE_NEW -> "New House";
            case HOUSE_EDIT -> "House Settings";
            default -> "Recruiter";
        };
    }

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
        NotchWidgets.title(ctx, this.font, title(), px + W / 2, py + 8);

        switch (mode) {
            case VIEW -> renderView(ctx, mouseX, mouseY);
            case FOUND, SETTINGS, HOUSE_NEW -> renderEditor(ctx, mouseX, mouseY);
            case BANK -> renderBank(ctx, mouseX, mouseY);
            case HOUSES -> renderHouses(ctx, mouseX, mouseY);
            case HOUSE_EDIT -> renderHouseEdit(ctx, mouseX, mouseY);
        }

        NotchWidgets.neutralButton(ctx, this.font, px + PAD, py + H - 26, BTN_W, ROW_H,
                mode == Mode.VIEW || mode == Mode.FOUND ? "Close" : "Back",
                over(mouseX, mouseY, px + PAD, py + H - 26, BTN_W, ROW_H));
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        if (mode == Mode.HOUSES) houseTooltip(ctx, mouseX, mouseY);
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
                Component.literal(factionName).withStyle(factionColor), px + W / 2, py + 30, 0xFFFFFFFF);
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
                NotchWidgets.dangerButton(ctx, this.font, px + PAD, y + 16, BTN_W, ROW_H,
                        isHouse ? "Leave House" : "Leave", over(mx, my, px + PAD, y + 16, BTN_W, ROW_H));
            }
        } else if (locked) {
            NotchWidgets.centerText(ctx, this.font, "You're with another faction.",
                    px + W / 2, y + 4, NotchTheme.TEXT_MUTED, false);
        } else if (!openToJoin) {
            NotchWidgets.centerText(ctx, this.font, "Not taking new members.",
                    px + W / 2, y + 4, NotchTheme.TEXT_MUTED, false);
        } else {
            long cost = isHouse && !houses.isEmpty() ? houses.get(0).cost() : fee;
            String label = cost > 0 ? "Join - " + cost + " " + NotchWidgets.coinName() : "Join";
            NotchWidgets.primaryButton(ctx, this.font, px + PAD, y, BTN_W, 18, label,
                    over(mx, my, px + PAD, y, BTN_W, 18));
        }

        if (showsHousesButton()) {
            NotchWidgets.neutralButton(ctx, this.font, px + PAD, housesButtonY(), BTN_W, ROW_H, "Houses",
                    over(mx, my, px + PAD, housesButtonY(), BTN_W, ROW_H));
            String mine = myHouseName();
            if (!mine.isEmpty()) {
                String shown = this.font.plainSubstrByWidth(mine, 80);
                ctx.drawString(this.font, Component.literal(shown).withStyle(factionColor),
                        px + PAD + BTN_W - 6 - this.font.width(shown), housesButtonY() + 4, 0xFFFFFFFF, true);
            }
        }
        if (bank) {
            NotchWidgets.neutralButton(ctx, this.font, px + PAD, bankButtonY(), BTN_W, ROW_H, "Bank",
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

    private String myHouseName() {
        for (House h : houses) {
            if (h.status() == RecruiterManager.HOUSE_MINE) return h.name();
        }
        return "";
    }

    private void renderBank(GuiGraphics ctx, int mx, int my) {
        ctx.drawCenteredString(this.font,
                Component.literal(factionName).withStyle(factionColor), px + W / 2, py + 30, 0xFFFFFFFF);
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
        boolean naming = mode != Mode.SETTINGS;
        ctx.drawString(this.font, naming ? "Name:" : "Motto:", px + PAD, rowLabel(),
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + PAD, py + 52, BTN_W, 15, NotchTheme.DEEP);

        if (mode == Mode.HOUSE_NEW) {
            ctx.drawString(this.font, "Colour:", px + PAD, rowColor() + 4, NotchTheme.TEXT_DARK, false);
            ctx.drawString(this.font, Component.literal("Same as " + factionName).withStyle(factionColor),
                    px + 76, rowColor() + 4, 0xFFFFFFFF, true);
        } else {
            ctx.drawString(this.font, "Colour:", px + PAD, rowColor() + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, px + 76, rowColor(), 100, 15,
                    titleCase(net.fugginbeenus.notchcurrency.compat.Colors.name(color())), over(mx, my, px + 76, rowColor(), 100, 15));
            ctx.drawCenteredString(this.font, Component.literal("Aa").withStyle(color()),
                    px + 200, rowColor() + 4, 0xFFFFFFFF);
        }

        renderFeeAndOpen(ctx, mx, my);

        String confirm = mode == Mode.FOUND ? "Found faction" : mode == Mode.HOUSE_NEW ? "Make House" : "Save";
        NotchWidgets.primaryButton(ctx, this.font, px + PAD, rowConfirm(), BTN_W, 18, confirm,
                over(mx, my, px + PAD, rowConfirm(), BTN_W, 18));
        String hint = mode == Mode.FOUND ? "You'll lead it, and this NPC will recruit for it."
                : mode == Mode.HOUSE_NEW ? "Members join it from the Houses page here."
                : "Members keep their place; only the sign changes.";
        NotchWidgets.centerText(ctx, this.font, hint, px + W / 2, rowConfirm() + 22, NotchTheme.TEXT_MUTED, false);
    }

    private void renderFeeAndOpen(GuiGraphics ctx, int mx, int my) {
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
    }

    private void renderHouses(GuiGraphics ctx, int mx, int my) {
        ctx.drawCenteredString(this.font,
                Component.literal(factionName).withStyle(factionColor), px + W / 2, py + 28, 0xFFFFFFFF);
        if (houses.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No Houses yet.", px + W / 2, py + 70, NotchTheme.TEXT_DARK, false);
            if (canMakeHouses) {
                NotchWidgets.centerText(ctx, this.font, "Make the first one below.",
                        px + W / 2, py + 84, NotchTheme.TEXT_MUTED, false);
            }
        }
        for (int i = 0; i < VISIBLE && i + scroll < houses.size(); i++) {
            House h = houses.get(i + scroll);
            int ry = listRowY(i);
            NotchWidgets.inset(ctx, px + PAD, ry, BTN_W, LIST_ROW - 2, NotchTheme.PANEL_MID);
            int textEnd = h.canEdit() ? editX() - 6 : actX() - 6;
            String members = h.members() == 1 ? "1 member" : h.members() + " members";
            int membersX = textEnd - this.font.width(members);
            ctx.drawString(this.font, members, membersX, ry + 5, NotchTheme.TEXT_DARK, false);
            String name = this.font.plainSubstrByWidth(h.name(), Math.max(20, membersX - (px + PAD + 6) - 6));
            ctx.drawString(this.font, Component.literal(name).withStyle(factionColor), px + PAD + 6, ry + 5,
                    0xFFFFFFFF, true);
            if (h.canEdit()) {
                NotchWidgets.neutralButton(ctx, this.font, editX(), ry + 1, EDIT_W, LIST_ROW - 4, "Edit",
                        over(mx, my, editX(), ry + 1, EDIT_W, LIST_ROW - 4));
            }
            boolean hover = over(mx, my, actX(), ry + 1, ACT_W, LIST_ROW - 4);
            switch (h.status()) {
                case RecruiterManager.HOUSE_MINE -> NotchWidgets.dangerButton(ctx, this.font, actX(), ry + 1,
                        ACT_W, LIST_ROW - 4, "Leave", hover);
                case RecruiterManager.HOUSE_CAN_JOIN -> NotchWidgets.primaryButton(ctx, this.font, actX(), ry + 1,
                        ACT_W, LIST_ROW - 4, h.cost() > 0 ? "Join " + NotchWidgets.compactCount(h.cost()) : "Join", hover);
                default -> NotchWidgets.centerText(ctx, this.font,
                        h.status() == RecruiterManager.HOUSE_LOCKED ? "Locked" : "Closed",
                        actX() + ACT_W / 2, ry + 5, NotchTheme.TEXT_MUTED, false);
            }
        }
        if (houses.size() > VISIBLE) {
            int by = pagerY();
            NotchWidgets.pager(ctx, this.font, px + W / 2 - 38, px + W / 2 + 18, by, 20, 13,
                    scroll / VISIBLE + 1, NotchWidgets.pages(houses.size(), VISIBLE),
                    over(mx, my, px + W / 2 - 38, by, 20, 13), over(mx, my, px + W / 2 + 18, by, 20, 13));
        }
        if (canMakeHouses) {
            NotchWidgets.primaryButton(ctx, this.font, px + PAD, py + H - 48, BTN_W, ROW_H, "New House",
                    over(mx, my, px + PAD, py + H - 48, BTN_W, ROW_H));
        }
    }

    private void houseTooltip(GuiGraphics ctx, int mx, int my) {
        for (int i = 0; i < VISIBLE && i + scroll < houses.size(); i++) {
            int ry = listRowY(i);
            int textEnd = houses.get(i + scroll).canEdit() ? editX() - 2 : actX() - 2;
            if (!over(mx, my, px + PAD, ry, textEnd - (px + PAD), LIST_ROW - 2)) continue;
            House h = houses.get(i + scroll);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(h.name()).withStyle(factionColor));
            lines.add(Component.literal(h.leader().isEmpty() ? "No leader" : "Led by " + h.leader())
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal(h.fee() > 0 ? "House fee: " + h.fee() + " " + NotchWidgets.coinName()
                    : "No House fee").withStyle(ChatFormatting.GRAY));
            if (h.status() == RecruiterManager.HOUSE_CAN_JOIN && h.cost() != h.fee()) {
                lines.add(Component.literal("Joining costs you " + h.cost() + " " + NotchWidgets.coinName() + ".")
                        .withStyle(ChatFormatting.GRAY));
            }
            if (!h.open()) lines.add(Component.literal("Not taking new members.").withStyle(ChatFormatting.DARK_GRAY));
            if (h.status() == RecruiterManager.HOUSE_LOCKED) {
                lines.add(Component.literal("You're with another faction.").withStyle(ChatFormatting.DARK_GRAY));
            }
            ctx.renderComponentTooltip(this.font, lines, mx, my);
            return;
        }
    }

    private void renderHouseEdit(GuiGraphics ctx, int mx, int my) {
        if (editing == null) return;
        ctx.drawCenteredString(this.font,
                Component.literal(editing.name()).withStyle(factionColor), px + W / 2, py + 30, 0xFFFFFFFF);
        NotchWidgets.centerText(ctx, this.font, editing.members() == 1 ? "1 member" : editing.members() + " members",
                px + W / 2, py + 46, NotchTheme.TEXT_MUTED, false);

        ctx.drawString(this.font, "Leader:", px + PAD, rowColor() + 4, NotchTheme.TEXT_DARK, false);
        String leader = leaderChoices.get(leaderIdx);
        String leaderLabel = leader.isEmpty() ? "None" : leader;
        if (runsHouses) {
            NotchWidgets.neutralButton(ctx, this.font, px + 76, rowColor(), 128, 15, leaderLabel,
                    over(mx, my, px + 76, rowColor(), 128, 15));
        } else {
            ctx.drawString(this.font, leaderLabel, px + 80, rowColor() + 4, NotchTheme.TEXT_DARK, false);
        }

        renderFeeAndOpen(ctx, mx, my);

        NotchWidgets.primaryButton(ctx, this.font, px + PAD, rowConfirm(), BTN_W, 18, "Save",
                over(mx, my, px + PAD, rowConfirm(), BTN_W, 18));
        if (runsHouses) {
            boolean armed = System.currentTimeMillis() - endArmedAt < CONFIRM_MS;
            NotchWidgets.dangerButton(ctx, this.font, px + PAD, py + H - 48, BTN_W, ROW_H,
                    armed ? "Sure? Click again to end it" : "End House",
                    over(mx, my, px + PAD, py + H - 48, BTN_W, ROW_H));
        } else {
            NotchWidgets.centerText(ctx, this.font, "Only the founder can pick a leader.",
                    px + W / 2, rowConfirm() + 22, NotchTheme.TEXT_MUTED, false);
        }
    }

    private void startEdit(House h) {
        editing = h;
        fee = h.fee();
        openToJoin = h.open();
        List<String> choices = new ArrayList<>();
        choices.add("");
        choices.addAll(h.memberNames());
        if (!h.leader().isEmpty() && !choices.contains(h.leader())) choices.add(1, h.leader());
        leaderChoices = choices;
        leaderIdx = Math.max(0, choices.indexOf(h.leader()));
        endArmedAt = 0L;
        mode = Mode.HOUSE_EDIT;
        rebuildPane();
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
            if (clickMode(mx, my)) return true;

            if (over(mx, my, px + PAD, py + H - 26, BTN_W, ROW_H)) {
                NotchWidgets.click();
                switch (mode) {
                    case SETTINGS, BANK, HOUSES -> mode = Mode.VIEW;
                    case HOUSE_NEW -> mode = Mode.HOUSES;
                    case HOUSE_EDIT -> mode = isHouse ? Mode.VIEW : Mode.HOUSES;
                    default -> {
                        this.onClose();
                        return true;
                    }
                }
                rebuildPane();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private boolean clickMode(int mx, int my) {
        switch (mode) {
            case VIEW -> {
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
                    if (isHouse && !houses.isEmpty()) {
                        startEdit(houses.get(0));
                    } else {
                        mode = Mode.SETTINGS;
                        rebuildPane();
                    }
                    return true;
                }
                if (showsHousesButton() && over(mx, my, px + PAD, housesButtonY(), BTN_W, ROW_H)) {
                    NotchWidgets.click();
                    mode = Mode.HOUSES;
                    rebuildPane();
                    return true;
                }
                if (bank && over(mx, my, px + PAD, bankButtonY(), BTN_W, ROW_H)) {
                    NotchWidgets.click();
                    mode = Mode.BANK;
                    rebuildPane();
                    return true;
                }
            }
            case BANK -> {
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
            }
            case HOUSES -> {
                return clickHouses(mx, my);
            }
            case HOUSE_EDIT -> {
                return clickHouseEdit(mx, my);
            }
            default -> {
                return clickEditor(mx, my);
            }
        }
        return false;
    }

    private boolean clickHouses(int mx, int my) {
        if (houses.size() > VISIBLE) {
            int by = pagerY();
            if (over(mx, my, px + W / 2 - 38, by, 20, 13)) {
                if (scroll > 0) {
                    NotchWidgets.click();
                    scroll = Math.max(0, scroll - VISIBLE);
                }
                return true;
            }
            if (over(mx, my, px + W / 2 + 18, by, 20, 13)) {
                if (scroll + VISIBLE < houses.size()) {
                    NotchWidgets.click();
                    scroll += VISIBLE;
                }
                return true;
            }
        }
        for (int i = 0; i < VISIBLE && i + scroll < houses.size(); i++) {
            House h = houses.get(i + scroll);
            int ry = listRowY(i);
            if (h.canEdit() && over(mx, my, editX(), ry + 1, EDIT_W, LIST_ROW - 4)) {
                NotchWidgets.click();
                startEdit(h);
                return true;
            }
            if (over(mx, my, actX(), ry + 1, ACT_W, LIST_ROW - 4)) {
                if (h.status() == RecruiterManager.HOUSE_MINE) {
                    NotchWidgets.click();
                    send(RecruiterManager.ACTION_HOUSE_LEAVE, h.id());
                } else if (h.status() == RecruiterManager.HOUSE_CAN_JOIN) {
                    NotchWidgets.click();
                    send(RecruiterManager.ACTION_HOUSE_JOIN, h.id());
                }
                return true;
            }
        }
        if (canMakeHouses && over(mx, my, px + PAD, py + H - 48, BTN_W, ROW_H)) {
            NotchWidgets.click();
            fee = 0;
            openToJoin = true;
            mode = Mode.HOUSE_NEW;
            rebuildPane();
            return true;
        }
        return false;
    }

    private boolean clickHouseEdit(int mx, int my) {
        if (editing == null) return false;
        if (runsHouses && over(mx, my, px + 76, rowColor(), 128, 15)) {
            NotchWidgets.tick();
            leaderIdx = (leaderIdx + 1) % leaderChoices.size();
            return true;
        }
        if (clickFeeAndOpen(mx, my)) return true;
        if (over(mx, my, px + PAD, rowConfirm(), BTN_W, 18)) {
            NotchWidgets.click();
            NotchPacketsClient.sendRecruiterAction(npcId, RecruiterManager.ACTION_HOUSE_SAVE, editing.id(),
                    leaderChoices.get(leaderIdx), fee, openToJoin);
            return true;
        }
        if (runsHouses && over(mx, my, px + PAD, py + H - 48, BTN_W, ROW_H)) {
            NotchWidgets.click();
            long now = System.currentTimeMillis();
            if (now - endArmedAt < CONFIRM_MS) {
                endArmedAt = 0L;
                send(RecruiterManager.ACTION_HOUSE_END, editing.id());
            } else {
                endArmedAt = now;
            }
            return true;
        }
        return false;
    }

    private boolean clickFeeAndOpen(int mx, int my) {
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
        return false;
    }

    private boolean clickEditor(int mx, int my) {
        if (mode != Mode.HOUSE_NEW && over(mx, my, px + 76, rowColor(), 100, 15)) {
            NotchWidgets.tick();
            colorIdx = (colorIdx + 1) % COLORS.length;
            return true;
        }
        if (clickFeeAndOpen(mx, my)) return true;
        if (over(mx, my, px + PAD, rowConfirm(), BTN_W, 18)) {
            String text = textField == null ? "" : textField.getValue().trim();
            if (mode != Mode.SETTINGS && text.isBlank()) return true;
            NotchWidgets.click();
            int action = mode == Mode.FOUND ? RecruiterManager.ACTION_FOUND
                    : mode == Mode.HOUSE_NEW ? RecruiterManager.ACTION_HOUSE_NEW : RecruiterManager.ACTION_SETTINGS;
            send(action, text);
            return true;
        }
        return false;
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        if (mode == Mode.HOUSES) {
            scroll = NotchWidgets.pageStart(scroll - (int) Math.signum(amount) * VISIBLE, houses.size(), VISIBLE);
            return true;
        }
        return false;
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
