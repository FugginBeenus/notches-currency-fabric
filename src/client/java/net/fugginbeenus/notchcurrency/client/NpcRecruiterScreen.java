package net.fugginbeenus.notchcurrency.client;

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

    private enum Mode { VIEW, FOUND, SETTINGS }

    private final UUID npcId;
    private final String factionId;
    private final String factionName;
    private final int memberCount;
    private final boolean alreadyIn;
    private final boolean canFound;
    private final boolean canManage;

    private Mode mode;
    private int colorIdx;
    private int fee;
    private boolean openToJoin;
    private final String motto;

    private int px, py;
    private EditBox textField; // faction name when founding, motto when editing

    public NpcRecruiterScreen(UUID npcId, String factionId, String factionName, ChatFormatting factionColor,
                              int memberCount, boolean alreadyIn, boolean canFound,
                              String motto, int fee, boolean openToJoin, boolean canManage) {
        super(Component.literal("Recruiter"));
        this.npcId = npcId;
        this.factionId = factionId;
        this.factionName = factionName;
        this.memberCount = memberCount;
        this.alreadyIn = alreadyIn;
        this.canFound = canFound;
        this.canManage = canManage;
        this.motto = motto == null ? "" : motto;
        this.fee = fee;
        this.openToJoin = openToJoin;
        this.mode = factionId.isEmpty() && canFound ? Mode.FOUND : Mode.VIEW;
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
    }

    // Rows used by the Found and Settings panes. The label sits clear above its field rather than
    // on the same line, which buried it under the inset.
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
                mode == Mode.FOUND ? "New Faction" : mode == Mode.SETTINGS ? "Faction Settings" : "Recruiter",
                px + W / 2, py + 8);

        switch (mode) {
            case VIEW -> renderView(ctx, mouseX, mouseY);
            case FOUND, SETTINGS -> renderEditor(ctx, mouseX, mouseY);
        }

        NotchWidgets.neutralButton(ctx, this.font, px + PAD, py + H - 26, BTN_W, ROW_H,
                mode == Mode.SETTINGS ? "Back" : "Close",
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
            NotchWidgets.dangerButton(ctx, this.font, px + PAD, y + 16, BTN_W, ROW_H, "Leave",
                    over(mx, my, px + PAD, y + 16, BTN_W, ROW_H));
        } else if (!openToJoin) {
            NotchWidgets.centerText(ctx, this.font, "Not taking new members.",
                    px + W / 2, y + 4, NotchTheme.TEXT_MUTED, false);
        } else {
            String label = fee > 0 ? "Join - " + fee + " " + NotchWidgets.coinName() : "Join";
            NotchWidgets.primaryButton(ctx, this.font, px + PAD, y, BTN_W, 18, label,
                    over(mx, my, px + PAD, y, BTN_W, 18));
        }

        if (canManage) {
            NotchWidgets.neutralButton(ctx, this.font, px + PAD, py + H - 48, BTN_W, ROW_H, "Settings",
                    over(mx, my, px + PAD, py + H - 48, BTN_W, ROW_H));
        }
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
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;

            if (mode == Mode.VIEW) {
                if (hasFaction() && alreadyIn && over(mx, my, px + PAD, py + 100, BTN_W, ROW_H)) {
                    NotchWidgets.click();
                    send(RecruiterManager.ACTION_LEAVE, "");
                    return true;
                }
                if (hasFaction() && !alreadyIn && openToJoin && over(mx, my, px + PAD, py + 84, BTN_W, 18)) {
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
                if (mode == Mode.SETTINGS) {
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
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, textField)) return true;
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

    // The blur hook is handed the graphics now instead of the partial tick.
    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render(). This screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
