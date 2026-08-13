package net.fugginbeenus.notchcurrency.client.npcmodel;

import net.fugginbeenus.notchcurrency.client.NotchNpcModelPickerScreen;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelBundle;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Everything installed, one per row, with a way to get rid of each.
 *
 * <p>A list rather than a control that cycles through them: with a handful of models installed,
 * clicking through them one at a time to find the one to remove is worse the more you have, which
 * is exactly backwards.
 */
public class NpcModelManageScreen extends Screen {

    private static final int W = 300, H = 186;
    private static final int LIST_X = 12, LIST_Y = 34, ROW_H = 18, VISIBLE = 6;
    private static final int REMOVE_W = 60, SHARE_W = 52;

    private final Screen parent;

    private int px, py;
    private int scroll;
    /** Which row has been asked about, since there is no undo and one click is too few. */
    private String confirming = "";
    private String status = "";
    private boolean statusIsError;

    public NpcModelManageScreen(Screen parent) {
        super(Component.literal("NPC Models"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
    }

    private List<NpcModelBundle> installed() {
        return NpcModelRegistry.all();
    }

    private int rowY(int i) {
        return py + LIST_Y + i * ROW_H;
    }

    private int removeX() {
        return px + W - LIST_X - REMOVE_W;
    }

    private int shareX() {
        return removeX() - SHARE_W - 4;
    }

    /**
     * Whether to offer Share at all: on a server, and only to somebody who can run commands.
     *
     * <p>Only decides the button. The server checks again on every packet, since a button that is
     * not drawn is not a permission.
     */
    private boolean canShare() {
        return NpcModelDownloads.mayShare();
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
        NotchWidgets.title(ctx, this.font, "NPC Models", px + W / 2, py + 8);

        List<NpcModelBundle> models = installed();
        if (models.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No custom models yet.",
                    px + W / 2, py + LIST_Y + 20, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "New model turns a Blockbench export into one.",
                    px + W / 2, py + LIST_Y + 34, NotchTheme.TEXT_MUTED, false);
        } else {
            NotchWidgets.inset(ctx, px + LIST_X - 2, py + LIST_Y - 3, W - LIST_X * 2 + 4,
                    VISIBLE * ROW_H + 4, NotchTheme.PANEL_MID);
        }

        for (int i = 0; i < VISIBLE && i + scroll < models.size(); i++) {
            NpcModelBundle bundle = models.get(i + scroll);
            int y = rowY(i);
            boolean asking = bundle.id().equals(confirming);

            int room = W - LIST_X * 2 - REMOVE_W - (canShare() ? SHARE_W + 4 : 0) - 12;
            ctx.drawString(this.font, fit(bundle.displayName(), room),
                    px + LIST_X + 4, y + 1, NotchTheme.TEXT_DARK, false);
            ctx.drawString(this.font, detail(bundle), px + LIST_X + 4, y + 10,
                    NotchTheme.TEXT_MUTED, false);

            if (canShare()) {
                NotchWidgets.neutralButton(ctx, this.font, shareX(), y, SHARE_W, 14, "Share",
                        over(mouseX, mouseY, shareX(), y, SHARE_W, 14));
            }
            NotchWidgets.dangerButton(ctx, this.font, removeX(), y, REMOVE_W, 14,
                    asking ? "Sure?" : "Remove",
                    over(mouseX, mouseY, removeX(), y, REMOVE_W, 14));
        }

        if (models.size() > VISIBLE) {
            NotchWidgets.centerText(ctx, this.font,
                    (scroll + 1) + "-" + Math.min(models.size(), scroll + VISIBLE)
                            + " of " + models.size(),
                    px + W / 2, py + LIST_Y + VISIBLE * ROW_H + 6, NotchTheme.TEXT_MUTED, false);
        } else if (!status.isEmpty()) {
            ctx.drawString(this.font, fit(status, W - LIST_X * 2), px + LIST_X,
                    py + LIST_Y + VISIBLE * ROW_H + 6,
                    statusIsError ? 0xFFD05A5A : 0xFF6AC46A, false);
        }

        int by = py + H - 26;
        NotchWidgets.primaryButton(ctx, this.font, px + LIST_X, by, 80, 16, "New model",
                over(mouseX, mouseY, px + LIST_X, by, 80, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + LIST_X + 84, by, 90, 16, "Import folder",
                over(mouseX, mouseY, px + LIST_X + 84, by, 90, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + W - LIST_X - 60, by, 60, 16, "Back",
                over(mouseX, mouseY, px + W - LIST_X - 60, by, 60, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
    }

    /** The line under the name: who made it, and whether it moves. */
    private String detail(NpcModelBundle bundle) {
        StringBuilder line = new StringBuilder();
        if (!bundle.author().isBlank()) line.append("by ").append(bundle.author()).append("  ");
        if (bundle.idle().isEmpty()) {
            line.append("no animations");
        } else {
            int clips = 1 + (bundle.walk().isEmpty() ? 0 : 1) + bundle.special().size();
            line.append(clips).append(clips == 1 ? " clip" : " clips");
        }
        return line.toString();
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
            List<NpcModelBundle> models = installed();

            for (int i = 0; i < VISIBLE && i + scroll < models.size(); i++) {
                if (over(mx, my, removeX(), rowY(i), REMOVE_W, 14)) {
                    remove(models.get(i + scroll));
                    NotchWidgets.click();
                    return true;
                }
                if (canShare() && over(mx, my, shareX(), rowY(i), SHARE_W, 14)) {
                    NpcModelBundle bundle = models.get(i + scroll);
                    String problem = NpcModelPacks.share(bundle.id());
                    setStatus(problem == null
                            ? "Sending " + bundle.displayName() + " to the server..."
                            : "Could not share it: " + problem, problem != null);
                    NotchWidgets.click();
                    return true;
                }
            }

            int by = py + H - 26;
            if (over(mx, my, px + LIST_X, by, 80, 16)) {
                NotchWidgets.click();
                Minecraft.getInstance().setScreen(new NpcModelCreateScreen(this));
                return true;
            }
            if (over(mx, my, px + LIST_X + 84, by, 90, 16)) {
                NotchWidgets.click();
                openImportFolder();
                return true;
            }
            if (over(mx, my, px + W - LIST_X - 60, by, 60, 16)) {
                NotchWidgets.click();
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    /** Asks first, then does it. The folder goes and there is no getting it back. */
    private void remove(NpcModelBundle bundle) {
        if (!bundle.id().equals(confirming)) {
            confirming = bundle.id();
            setStatus("Click Remove again to delete " + bundle.displayName() + ".", false);
            return;
        }
        confirming = "";

        String problem = NpcModelLoader.delete(bundle.id());
        if (problem != null) {
            setStatus("Could not remove it: " + problem, true);
            return;
        }
        NpcModelPacks.reload(Minecraft.getInstance(), false);
        NotchNpcModelPickerScreen.markStale();
        scroll = 0;
        setStatus("Removed " + bundle.displayName() + ". Any NPC wearing it falls back.", false);
    }

    private void openImportFolder() {
        try {
            java.nio.file.Files.createDirectories(NpcModelLoader.importDir());
            // Util moved package at 1.21.11, not at 26 like most of the rest.
            //? if >=1.21.11 {
            /*net.minecraft.util.Util.getPlatform().openPath(NpcModelLoader.importDir());
            *///?} else {
            net.minecraft.Util.getPlatform().openFile(NpcModelLoader.importDir().toFile());
            //?}
        } catch (Exception e) {
            setStatus("Could not open the folder: " + e.getMessage(), true);
        }
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        int max = Math.max(0, installed().size() - VISIBLE);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(amount)));
        return true;
    }

    private void setStatus(String line, boolean error) {
        status = line;
        statusIsError = error;
    }

    private String fit(String text, int room) {
        return this.font.width(text) <= room ? text : this.font.plainSubstrByWidth(text, room - 6) + "..";
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(GuiGraphics ctx) {
        // No menu blur behind the mod's screens.
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}
}
