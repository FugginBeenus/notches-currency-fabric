package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * The recruiter: where a player joins or leaves a faction, and where its owner founds one.
 *
 * <p>Deliberately the only thing most players ever need to touch — no commands, no admin. What's on
 * offer depends on who's looking: a visitor sees Join or Leave, and an owner standing at a recruiter
 * with no faction yet is offered the chance to start one.
 */
public class NpcRecruiterScreen extends Screen {

    private static final int W = 280, H = 180;

    /** The colours a faction can pick, in the order they cycle. */
    private static final Formatting[] COLORS = {
            Formatting.WHITE, Formatting.RED, Formatting.GOLD, Formatting.YELLOW, Formatting.GREEN,
            Formatting.AQUA, Formatting.BLUE, Formatting.LIGHT_PURPLE, Formatting.DARK_PURPLE, Formatting.GRAY,
    };

    private final UUID npcId;
    private final String factionId;
    private final String factionName;
    private final Formatting factionColor;
    private final int memberCount;
    private final boolean alreadyIn;
    private final boolean canFound;

    private int px, py;
    private int colorIdx;
    private TextFieldWidget nameField;

    public NpcRecruiterScreen(UUID npcId, String factionId, String factionName, Formatting factionColor,
                              int memberCount, boolean alreadyIn, boolean canFound) {
        super(Text.literal("Recruiter"));
        this.npcId = npcId;
        this.factionId = factionId;
        this.factionName = factionName;
        this.factionColor = factionColor;
        this.memberCount = memberCount;
        this.alreadyIn = alreadyIn;
        this.canFound = canFound;
    }

    private boolean hasFaction() { return !factionId.isEmpty(); }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        if (canFound) {
            nameField = new TextFieldWidget(this.textRenderer, px + 20, py + 80, W - 40, 12,
                    Text.literal("Faction name"));
            nameField.setMaxLength(32);
            nameField.setPlaceholder(Text.literal("name your faction").formatted(Formatting.DARK_GRAY));
            addDrawableChild(nameField);
            setInitialFocus(nameField);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderInGameBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Recruiter", px + W / 2, py + 8);

        if (hasFaction()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(factionName).formatted(factionColor), px + W / 2, py + 32, 0xFFFFFF);
            NotchWidgets.centerText(ctx, this.textRenderer,
                    memberCount == 1 ? "1 member" : memberCount + " members",
                    px + W / 2, py + 48, NotchTheme.TEXT_MUTED, false);

            if (alreadyIn) {
                NotchWidgets.centerText(ctx, this.textRenderer, "You're one of them.",
                        px + W / 2, py + 76, NotchTheme.TEXT_DARK, false);
                NotchWidgets.dangerButton(ctx, this.textRenderer, px + 40, py + 100, W - 80, 18, "Leave",
                        over(mouseX, mouseY, px + 40, py + 100, W - 80, 18));
            } else {
                NotchWidgets.primaryButton(ctx, this.textRenderer, px + 40, py + 96, W - 80, 20, "Join",
                        over(mouseX, mouseY, px + 40, py + 96, W - 80, 20));
            }
        } else if (canFound) {
            NotchWidgets.centerText(ctx, this.textRenderer, "This recruiter has no faction yet.",
                    px + W / 2, py + 34, NotchTheme.TEXT_DARK, false);
            NotchWidgets.centerText(ctx, this.textRenderer, "Start one and it'll recruit for you.",
                    px + W / 2, py + 46, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.inset(ctx, px + 18, py + 76, W - 36, 15, NotchTheme.DEEP);
            Formatting picked = COLORS[colorIdx];
            ctx.drawText(this.textRenderer, "Colour:", px + 20, py + 102, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, px + 70, py + 98, 90, 15,
                    titleCase(picked.getName()), over(mouseX, mouseY, px + 70, py + 98, 90, 15));
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Aa").formatted(picked), px + 180, py + 102, 0xFFFFFF);
            NotchWidgets.primaryButton(ctx, this.textRenderer, px + 40, py + 124, W - 80, 18, "Found faction",
                    over(mouseX, mouseY, px + 40, py + 124, W - 80, 18));
        } else {
            NotchWidgets.centerText(ctx, this.textRenderer, "This recruiter has no faction yet.",
                    px + W / 2, py + 60, NotchTheme.TEXT_DARK, false);
            NotchWidgets.centerText(ctx, this.textRenderer, "Come back once it's signed up.",
                    px + W / 2, py + 74, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.neutralButton(ctx, this.textRenderer, px + 40, py + H - 28, W - 80, 18, "Close",
                over(mouseX, mouseY, px + 40, py + H - 28, W - 80, 18));
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (hasFaction() && alreadyIn && over(mx, my, px + 40, py + 100, W - 80, 18)) {
                NotchWidgets.click();
                NotchPacketsClient.sendRecruiterAction(npcId, RecruiterManager.ACTION_LEAVE, "", "");
                return true;
            }
            if (hasFaction() && !alreadyIn && over(mx, my, px + 40, py + 96, W - 80, 20)) {
                NotchWidgets.click();
                NotchPacketsClient.sendRecruiterAction(npcId, RecruiterManager.ACTION_JOIN, "", "");
                return true;
            }
            if (!hasFaction() && canFound) {
                if (over(mx, my, px + 70, py + 98, 90, 15)) {
                    NotchWidgets.tick();
                    colorIdx = (colorIdx + 1) % COLORS.length;
                    return true;
                }
                if (over(mx, my, px + 40, py + 124, W - 80, 18)) {
                    String name = nameField == null ? "" : nameField.getText().trim();
                    if (name.isBlank()) return true; // the server would reject it anyway
                    NotchWidgets.click();
                    NotchPacketsClient.sendRecruiterAction(npcId, RecruiterManager.ACTION_FOUND,
                            name, COLORS[colorIdx].getName());
                    return true;
                }
            }
            if (over(mx, my, px + 40, py + H - 28, W - 80, 18)) {
                NotchWidgets.click();
                this.close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, nameField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
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
    public boolean shouldPause() { return false; }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens — they draw crisp panels over the world.
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render() — this screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
