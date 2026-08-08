package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.UUID;

public class NpcFactionPickerScreen extends Screen {

    public record Entry(String id, String name, ChatFormatting color, int members) {}

    private static final int W = 280, H = 210;
    private static final int LIST_X = 12, LIST_Y = 44, ROW_H = 18, VISIBLE = 7;

    private final UUID npcId;
    private final String currentId;
    private final List<Entry> factions;

    private int px, py;
    private int scroll;

    public NpcFactionPickerScreen(UUID npcId, String currentId, List<Entry> factions) {
        super(Component.literal("Faction"));
        this.npcId = npcId;
        this.currentId = currentId == null ? "" : currentId;
        this.factions = factions;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
    }

    private int rowY(int i) { return py + LIST_Y + i * ROW_H; }

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
        NotchWidgets.title(ctx, this.font, "Faction", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "Who this NPC stands with.",
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);

        NotchWidgets.inset(ctx, px + LIST_X - 2, py + LIST_Y - 4, W - 20, VISIBLE * ROW_H + 6, NotchTheme.PANEL_MID);
        if (factions.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No factions you can use yet.",
                    px + W / 2, py + LIST_Y + 34, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "Found one at a Recruiter NPC.",
                    px + W / 2, py + LIST_Y + 46, NotchTheme.TEXT_MUTED, false);
        }
        for (int i = 0; i < VISIBLE && i + scroll < factions.size(); i++) {
            Entry f = factions.get(i + scroll);
            int ry = rowY(i);
            boolean selected = f.id().equals(currentId);
            boolean hover = over(mouseX, mouseY, px + LIST_X, ry, W - 24, ROW_H - 2);
            if (selected) {
                NotchWidgets.primaryButton(ctx, this.font, px + LIST_X, ry, W - 24, ROW_H - 2, "", hover);
            } else {
                NotchWidgets.button(ctx, px + LIST_X, ry, W - 24, ROW_H - 2, hover, false);
            }
            ctx.drawString(this.font, Component.literal(f.name()).withStyle(f.color()),
                    px + LIST_X + 6, ry + 4, 0xFFFFFF, selected);
            String members = f.members() == 1 ? "1 member" : f.members() + " members";
            ctx.drawString(this.font, members, px + W - 92, ry + 4,
                    selected ? NotchTheme.TEXT_LIGHT : NotchTheme.TEXT_MUTED, false);
        }
        if (factions.size() > VISIBLE) {
            NotchWidgets.centerText(ctx, this.font,
                    (scroll + 1) + "-" + Math.min(factions.size(), scroll + VISIBLE) + " of " + factions.size(),
                    px + W / 2, py + LIST_Y + VISIBLE * ROW_H + 6, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.dangerButton(ctx, this.font, px + 12, py + H - 26, 120, 16, "No faction",
                over(mouseX, mouseY, px + 12, py + H - 26, 120, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + W - 132, py + H - 26, 120, 16, "Back",
                over(mouseX, mouseY, px + W - 132, py + H - 26, 120, 16));
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
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
            for (int i = 0; i < VISIBLE && i + scroll < factions.size(); i++) {
                if (over(mx, my, px + LIST_X, rowY(i), W - 24, ROW_H - 2)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendFactionPick(npcId, RecruiterManager.PICK_SET,
                            factions.get(i + scroll).id());
                    return true;
                }
            }
            if (over(mx, my, px + 12, py + H - 26, 120, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendFactionPick(npcId, RecruiterManager.PICK_CLEAR, "");
                return true;
            }
            if (over(mx, my, px + W - 132, py + H - 26, 120, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 2); // back to the Role tab we came from
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
        int maxScroll = Math.max(0, factions.size() - VISIBLE);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount)));
        return true;
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    //? if >=1.21 {
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
