package net.fugginbeenus.notchcurrency.client.npctexture;

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

public class NpcTextureManageScreen extends Screen {

    private static final int W = 340, H = 262;
    private static final int ROW_H = 19, LIST_Y = 40;

    private final Runnable onBack;
    private int px, py, pickedSlot = -1;
    private EditBox nameField;
    private String status = "";
    private boolean statusBad;

    public NpcTextureManageScreen(Runnable onBack) {
        super(Component.literal("Particle textures"));
        this.onBack = onBack;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        NpcTextureLoader.scan();
        String old = nameField == null ? "" : nameField.getValue();
        nameField = new EditBox(this.font, px + 14, py + H - 45, 110, 10, Component.empty());
        nameField.setMaxLength(32);
        nameField.setBordered(false);
        nameField.setHint(Component.literal("name it").withStyle(ChatFormatting.DARK_GRAY));
        nameField.setValue(old);
        addRenderableWidget(nameField);
        if (pickedSlot < 0) pickedSlot = Math.max(1, NpcTextureLoader.freeSlot());
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int rowY(int slot) { return py + LIST_Y + (slot - 1) * ROW_H; }

    private void setStatus(String line, boolean bad) {
        status = line;
        statusBad = bad;
    }

    private void openFolder() {
        try {
            Files.createDirectories(NpcTextureLoader.importDir());
            //? if >=1.21.11 {
            /*net.minecraft.util.Util.getPlatform().openPath(NpcTextureLoader.importDir());
            *///?} else {
            net.minecraft.Util.getPlatform().openFile(NpcTextureLoader.importDir().toFile());
            //?}
        } catch (Exception e) {
            setStatus("Could not open the folder: " + e.getMessage(), true);
        }
    }

    private void reload() {
        net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelPacks.reload(
                net.minecraft.client.Minecraft.getInstance(), false);
    }

    private void importNext() {
        List<Path> waiting = NpcTextureLoader.imported();
        if (waiting.isEmpty()) {
            setStatus("No PNG files in the import folder.", true);
            return;
        }
        String name = nameField.getValue().trim().toLowerCase().replace(' ', '_');
        if (name.isEmpty()) {
            setStatus("Give it a short name first.", true);
            return;
        }
        if (pickedSlot < 1) {
            setStatus("All 8 slots are full. Clear one first.", true);
            return;
        }
        String problem = NpcTextureLoader.adopt(waiting.get(0), pickedSlot, name);
        if (problem != null) {
            setStatus(problem, true);
            return;
        }
        nameField.setValue("");
        reload();
        if (NpcTextureDownloads.mayShare()) {
            NpcTextureDownloads.upload(pickedSlot);
            setStatus("Slot " + pickedSlot + " is now " + name + ", and it went to the server.", false);
        } else {
            setStatus("Slot " + pickedSlot + " is now " + name + ".", false);
        }
        pickedSlot = Math.max(1, NpcTextureLoader.freeSlot());
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Particle textures", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "Drop PNGs in the import folder, then name them into a slot.",
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.divider(ctx, px + 8, py + 34, W - 16);

        boolean ops = NpcTextureDownloads.mayShare();
        for (int slot = 1; slot <= NpcTextureLoader.SLOTS; slot++) {
            int ry = rowY(slot);
            NpcTextureLoader.Local have = NpcTextureLoader.slot(slot);
            boolean picked = slot == pickedSlot;
            boolean hover = over(mouseX, mouseY, px + 12, ry, 200, ROW_H - 3);
            if (picked) NotchWidgets.primaryButton(ctx, this.font, px + 12, ry, 200, ROW_H - 3, "", hover);
            else NotchWidgets.neutralButton(ctx, this.font, px + 12, ry, 200, ROW_H - 3, "", hover);
            ctx.drawString(this.font, slot + ".", px + 18, ry + 4, NotchTheme.TEXT_MUTED, false);
            ctx.drawString(this.font, have == null ? "empty" : have.name(), px + 34, ry + 4,
                    have == null ? NotchTheme.TEXT_MUTED : NotchTheme.TEXT_DARK, false);
            if (have != null) {
                net.fugginbeenus.notchcurrency.client.particle.ParticleThumbs.draw(ctx,
                        "notchcurrency:custom_" + slot, px + 192, ry + 1, 14);
                if (NpcTextureDownloads.onServer(slot)) {
                    ctx.drawString(this.font, "on server", px + 222, ry + 4, NotchTheme.TEXT_MUTED, false);
                } else if (ops) {
                    NotchWidgets.primaryButton(ctx, this.font, px + 220, ry, 60, ROW_H - 3, "Share",
                            over(mouseX, mouseY, px + 220, ry, 60, ROW_H - 3));
                }
                NotchWidgets.dangerButton(ctx, this.font, px + 300, ry, 24, ROW_H - 3, "x",
                        over(mouseX, mouseY, px + 300, ry, 24, ROW_H - 3));
            }
        }

        int waiting = NpcTextureLoader.imported().size();
        NotchWidgets.divider(ctx, px + 8, py + H - 54, W - 16);
        NotchWidgets.inset(ctx, px + 12, py + H - 49, 114, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.font, px + 132, py + H - 49, 110, 14,
                "Import into " + (pickedSlot < 1 ? "?" : String.valueOf(pickedSlot)) + " (" + waiting + ")",
                over(mouseX, mouseY, px + 132, py + H - 49, 110, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + 248, py + H - 49, 80, 14, "Open folder",
                over(mouseX, mouseY, px + 248, py + H - 49, 80, 14));

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
            boolean ops = NpcTextureDownloads.mayShare();
            for (int slot = 1; slot <= NpcTextureLoader.SLOTS; slot++) {
                int ry = rowY(slot);
                NpcTextureLoader.Local have = NpcTextureLoader.slot(slot);
                if (have != null && over(mx, my, px + 300, ry, 24, ROW_H - 3)) {
                    NotchWidgets.click();
                    String problem = NpcTextureLoader.delete(slot);
                    if (ops) NotchPacketsClient.sendNpcTextureDrop(slot);
                    reload();
                    setStatus(problem == null ? "Cleared slot " + slot : problem, problem != null);
                    if (pickedSlot < 1) pickedSlot = slot;
                    return true;
                }
                if (have != null && ops && !NpcTextureDownloads.onServer(slot)
                        && over(mx, my, px + 220, ry, 60, ROW_H - 3)) {
                    NotchWidgets.click();
                    NpcTextureDownloads.upload(slot);
                    setStatus("Sharing slot " + slot + " with the server...", false);
                    return true;
                }
                if (over(mx, my, px + 12, ry, 200, ROW_H - 3)) {
                    NotchWidgets.click();
                    pickedSlot = slot;
                    return true;
                }
            }
            if (over(mx, my, px + 132, py + H - 49, 110, 14)) {
                NotchWidgets.click();
                importNext();
                return true;
            }
            if (over(mx, my, px + 248, py + H - 49, 80, 14)) {
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
