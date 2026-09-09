package net.fugginbeenus.notchcurrency.client.npcsound;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NpcSoundManageScreen extends Screen {

    private static final int W = 340, H = 236;
    private static final int ROW_H = 18, ROWS = 6;

    private final java.util.function.Consumer<String> onPick;
    private final Runnable onBack;

    private int px, py, scroll;
    private EditBox nameField;
    private String status = "";
    private boolean statusBad;

    public NpcSoundManageScreen(java.util.function.Consumer<String> onPick, Runnable onBack) {
        super(Component.literal("Custom sounds"));
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        NpcSoundLoader.scan();
        String old = nameField == null ? "" : nameField.getValue();
        nameField = new EditBox(this.font, px + 14, py + H - 45, 120, 10, Component.empty());
        nameField.setMaxLength(48);
        nameField.setBordered(false);
        nameField.setHint(Component.literal("name it").withStyle(ChatFormatting.DARK_GRAY));
        nameField.setValue(old);
        addRenderableWidget(nameField);
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int rowY(int i) { return py + 44 + i * ROW_H; }

    private void setStatus(String line, boolean bad) {
        status = line;
        statusBad = bad;
    }

    private void openFolder() {
        try {
            Files.createDirectories(NpcSoundLoader.importDir());
            //? if >=1.21.11 {
            /*net.minecraft.util.Util.getPlatform().openPath(NpcSoundLoader.importDir());
            *///?} else {
            net.minecraft.Util.getPlatform().openFile(NpcSoundLoader.importDir().toFile());
            //?}
        } catch (Exception e) {
            setStatus("Could not open the folder: " + e.getMessage(), true);
        }
    }

    private void importNext() {
        List<Path> waiting = NpcSoundLoader.imported();
        if (waiting.isEmpty()) {
            setStatus("No .ogg files in the import folder.", true);
            return;
        }
        String id = nameField.getValue().trim().toLowerCase().replace(' ', '_');
        if (id.isEmpty()) {
            setStatus("Give it a short name first.", true);
            return;
        }
        String problem = NpcSoundLoader.adopt(waiting.get(0), id);
        if (problem != null) {
            setStatus(problem, true);
            return;
        }
        nameField.setValue("");
        net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelPacks.reload(
                net.minecraft.client.Minecraft.getInstance(), false);
        if (NpcSoundDownloads.mayShare()) {
            NpcSoundDownloads.upload(id);
            setStatus("Added notchcurrency:" + id + " and sent it to the server.", false);
        } else {
            setStatus("Added notchcurrency:" + id, false);
        }
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Custom sounds", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font,
                "Drop .ogg files in the import folder, then name them here.",
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.divider(ctx, px + 8, py + 34, W - 16);

        List<String> all = NpcSoundLoader.names();
        boolean ops = NpcSoundDownloads.mayShare();
        if (all.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No custom sounds yet.",
                    px + W / 2, py + 76, NotchTheme.TEXT_MUTED, false);
        }
        int shown = Math.min(ROWS, Math.max(0, all.size() - scroll));
        for (int i = 0; i < shown; i++) {
            String id = all.get(i + scroll);
            int ry = rowY(i);
            boolean hover = over(mouseX, mouseY, px + 12, ry, 168, ROW_H - 2);
            NotchWidgets.neutralButton(ctx, this.font, px + 12, ry, 168, ROW_H - 2, "", hover);
            ctx.drawString(this.font, "notchcurrency:" + id, px + 17, ry + 4,
                    NotchTheme.TEXT_DARK, false);
            if (NpcSoundDownloads.onServer(id)) {
                ctx.drawString(this.font, "on server", px + 200, ry + 4, NotchTheme.TEXT_MUTED, false);
            } else if (ops) {
                NotchWidgets.primaryButton(ctx, this.font, px + 186, ry, 74, ROW_H - 2, "Share",
                        over(mouseX, mouseY, px + 186, ry, 74, ROW_H - 2));
            }
            NotchWidgets.dangerButton(ctx, this.font, px + 300, ry, 24, ROW_H - 2, "x",
                    over(mouseX, mouseY, px + 300, ry, 24, ROW_H - 2));
        }

        if (all.size() > ROWS) {
            int sy = py + 44 + ROWS * ROW_H;
            NotchWidgets.neutralButton(ctx, this.font, px + 12, sy, 24, 12, "v",
                    over(mouseX, mouseY, px + 12, sy, 24, 12));
            NotchWidgets.neutralButton(ctx, this.font, px + 40, sy, 24, 12, "^",
                    over(mouseX, mouseY, px + 40, sy, 24, 12));
        }

        int waiting = NpcSoundLoader.imported().size();
        NotchWidgets.divider(ctx, px + 8, py + H - 54, W - 16);
        NotchWidgets.inset(ctx, px + 12, py + H - 49, 124, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.font, px + 142, py + H - 49, 96, 14,
                "Import (" + waiting + ")",
                over(mouseX, mouseY, px + 142, py + H - 49, 96, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + 244, py + H - 49, 84, 14, "Open folder",
                over(mouseX, mouseY, px + 244, py + H - 49, 84, 14));

        if (!status.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, status, px + W / 2, py + H - 30,
                    statusBad ? NotchTheme.TEXT_RED : NotchTheme.ACCENT_GREEN, false);
        }

        NotchWidgets.neutralButton(ctx, this.font, px + W / 2 - 50, py + H - 20, 100, 16, "Back",
                over(mouseX, mouseY, px + W / 2 - 50, py + H - 20, 100, 16));

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
            List<String> all = NpcSoundLoader.names();
            boolean ops = NpcSoundDownloads.mayShare();
            int shown = Math.min(ROWS, Math.max(0, all.size() - scroll));
            for (int i = 0; i < shown; i++) {
                String id = all.get(i + scroll);
                int ry = rowY(i);
                if (over(mx, my, px + 300, ry, 24, ROW_H - 2)) {
                    NotchWidgets.click();
                    String problem = NpcSoundLoader.delete(id);
                    if (ops) NotchPacketsClient.sendNpcSoundDrop(id);
                    net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelPacks.reload(
                            net.minecraft.client.Minecraft.getInstance(), false);
                    setStatus(problem == null ? "Removed " + id : problem, problem != null);
                    return true;
                }
                if (ops && !NpcSoundDownloads.onServer(id)
                        && over(mx, my, px + 186, ry, 74, ROW_H - 2)) {
                    NotchWidgets.click();
                    NpcSoundDownloads.upload(id);
                    setStatus("Sharing " + id + " with the server...", false);
                    return true;
                }
                if (over(mx, my, px + 12, ry, 168, ROW_H - 2)) {
                    NotchWidgets.click();
                    if (onPick != null) onPick.accept("notchcurrency:" + id);
                    return true;
                }
            }
            if (all.size() > ROWS) {
                int sy = py + 44 + ROWS * ROW_H;
                if (over(mx, my, px + 12, sy, 24, 12)) {
                    NotchWidgets.click();
                    scroll = Math.min(Math.max(0, all.size() - ROWS), scroll + 1);
                    return true;
                }
                if (over(mx, my, px + 40, sy, 24, 12)) {
                    NotchWidgets.click();
                    scroll = Math.max(0, scroll - 1);
                    return true;
                }
            }
            if (over(mx, my, px + 142, py + H - 49, 96, 14)) {
                NotchWidgets.click();
                importNext();
                return true;
            }
            if (over(mx, my, px + 244, py + H - 49, 84, 14)) {
                NotchWidgets.click();
                openFolder();
                return true;
            }
            if (over(mx, my, px + W / 2 - 50, py + H - 20, 100, 16)) {
                NotchWidgets.click();
                if (onBack != null) onBack.run(); else this.onClose();
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
