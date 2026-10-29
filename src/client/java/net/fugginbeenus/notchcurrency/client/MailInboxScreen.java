package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.mail.MailInboxMenu;
import net.fugginbeenus.notchcurrency.mail.MailLayout;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MailInboxScreen extends AbstractContainerScreen<MailInboxMenu> {

    private static final int TAKE_W = 56, TAKE_H = 14;
    private String wood = "oak";
    private int woodColor = 0xA2834F;

    public MailInboxScreen(MailInboxMenu handler, Inventory inv, Component title) {
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

    private int takeX() { return this.leftPos + MailLayout.W - 8 - TAKE_W; }
    private int takeY() { return this.topPos + 126; }

    @Override
    protected void init() {
        super.init();
        wood = "oak";
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && mc.level != null) {
            var block = mc.level.getBlockState(hit.getBlockPos()).getBlock();
            if (block instanceof net.fugginbeenus.notchcurrency.block.MailboxBlock) {
                String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                if (path.endsWith("_mailbox")) wood = path.substring(0, path.length() - "_mailbox".length());
            }
        }
        woodColor = switch (wood) {
            case "spruce" -> 0x735531;
            case "birch" -> 0xC0AF79;
            case "jungle" -> 0xA07351;
            case "acacia" -> 0xA85A32;
            case "dark_oak" -> 0x432B14;
            case "mangrove" -> 0x763631;
            case "cherry" -> 0xE3B3AD;
            case "bamboo" -> 0xC1AD50;
            case "crimson" -> 0x653147;
            case "warped" -> 0x2B6963;
            default -> 0xA2834F;
        };
    }

    private int shade(float f) {
        int r = Math.round(((woodColor >> 16) & 0xFF) * f), g = Math.round(((woodColor >> 8) & 0xFF) * f),
                b = Math.round((woodColor & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void drawMailbox(GuiGraphics ctx, int x, int y) {
        int fx = x + 4, fy = y + 36, fw = 168, fh = 82;
        NotchWidgets.roundBox(ctx, fx, fy, fw, fh, NotchTheme.OUTLINE);
        NotchWidgets.tile(ctx, net.fugginbeenus.notchcurrency.compat.Reg.id("minecraft",
                "textures/block/" + wood + "_planks.png"), fx + 1, fy + 1, fw - 2, fh - 2);
        ctx.fill(fx + 1, fy + 1, fx + fw - 1, fy + 2, 0x50FFFFFF);
        ctx.fill(fx + 1, fy + 2, fx + 2, fy + fh - 1, 0x50FFFFFF);
        ctx.fill(fx + 1, fy + fh - 2, fx + fw - 1, fy + fh - 1, 0x60000000);
        ctx.fill(fx + fw - 2, fy + 2, fx + fw - 1, fy + fh - 2, 0x60000000);
        int sx = fx + fw / 2 - 24;
        ctx.fill(sx - 1, fy + 1, sx + 49, fy + 4, 0xC0000000);
        ctx.fill(sx, fy + 2, sx + 48, fy + 3, 0xFF16181C);
        ctx.fill(sx, fy + 3, sx + 48, fy + 4, 0xFFC8C8C8);
        int grid = shade(0.62f), cell = shade(0.3f), cellDk = shade(0.22f), edge = shade(0.42f);
        for (int i = 0; i < MailInboxMenu.INBOX_SLOTS; i++) {
            int cx = x + MailLayout.SLOTS_X + (i % MailInboxMenu.COLS) * 18 - 1;
            int cy = y + MailLayout.SLOTS_Y + (i / MailInboxMenu.COLS) * 18 - 1;
            ctx.fill(cx, cy, cx + 18, cy + 18, grid);
            ctx.fill(cx + 1, cy + 1, cx + 18, cy + 18, cell);
            ctx.fill(cx + 1, cy + 1, cx + 18, cy + 2, cellDk);
            ctx.fill(cx + 1, cy + 1, cx + 2, cy + 18, cellDk);
        }
        int left = x + MailLayout.SLOTS_X - 2, right = x + MailLayout.SLOTS_X + MailInboxMenu.COLS * 18 - 1;
        int bottom = y + MailLayout.SLOTS_Y + MailInboxMenu.ROWS * 18 - 1;
        ctx.fill(left, fy + 4, left + 1, bottom + 1, edge);
        ctx.fill(right, fy + 4, right + 1, bottom + 1, edge);
        ctx.fill(left, bottom, right + 1, bottom + 1, edge);
        hinge(ctx, x + 1, y + 50);
        hinge(ctx, x + 1, y + 96);
        latch(ctx, x + 170, y + 72);
        var player = net.minecraft.client.Minecraft.getInstance().player;
        String name = player == null ? "" : player.getName().getString();
        if (!name.isEmpty()) {
            int pw = this.font.width(name) + 8, px = x + MailLayout.W / 2 - pw / 2, py = y + 113;
            NotchWidgets.roundBox(ctx, px, py, pw, 11, 0xFF5C4A32);
            ctx.fill(px + 1, py + 1, px + pw - 1, py + 10, NotchTheme.PAPER);
            ctx.fill(px + 1, py + 1, px + pw - 1, py + 2, 0xFFFBF5E6);
            ctx.fill(px + 1, py + 9, px + pw - 1, py + 10, NotchTheme.PAPER_EDGE);
            ctx.drawString(this.font, name, px + 4, py + 2, NotchTheme.INK, false);
        }
    }

    private static void hinge(GuiGraphics ctx, int x, int y) {
        NotchWidgets.roundBox(ctx, x, y, 5, 7, NotchTheme.OUTLINE);
        ctx.fill(x + 1, y + 1, x + 4, y + 6, 0xFFC8C8C8);
        ctx.fill(x + 1, y + 1, x + 4, y + 2, 0xFFEDEDED);
        ctx.fill(x + 3, y + 2, x + 4, y + 6, 0xFF8E8E8E);
        ctx.fill(x + 2, y + 3, x + 3, y + 4, 0xFF5C5C5C);
    }

    private static void latch(GuiGraphics ctx, int x, int y) {
        NotchWidgets.roundBox(ctx, x, y, 5, 8, NotchTheme.OUTLINE);
        ctx.fill(x + 1, y + 1, x + 4, y + 7, 0xFFC8C8C8);
        ctx.fill(x + 1, y + 1, x + 4, y + 2, 0xFFEDEDED);
        ctx.fill(x + 1, y + 3, x + 3, y + 5, 0xFF5C5C5C);
    }

    private static void flag(GuiGraphics ctx, int x, int y) {
        ctx.fill(x + 2, y, x + 3, y + 12, 0xFF5A5A5A);
        ctx.fill(x + 3, y, x + 4, y + 12, 0xFF8A8A8A);
        ctx.fill(x + 4, y, x + 11, y + 5, 0xFFE03333);
        ctx.fill(x + 4, y, x + 11, y + 1, 0xFFFF6A6A);
        ctx.fill(x + 4, y + 4, x + 11, y + 5, 0xFFA32424);
        ctx.fill(x + 10, y + 1, x + 11, y + 5, 0xFFBA2929);
        ctx.fill(x + 1, y + 12, x + 5, y + 13, 0xFF5A5A5A);
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, MailLayout.W, MailLayout.H);
        drawMailbox(ctx, x, y);
        MailTabs.draw(ctx, this.font, x, y, MailTabs.INBOX, this.menu.waiting(), mouseX, mouseY);

        int waiting = this.menu.waiting();
        if (waiting > 0) flag(ctx, x + 8, y + 127);
        ctx.drawString(this.font, summary(waiting), x + (waiting > 0 ? 24 : 8), y + 130,
                waiting == 0 ? NotchTheme.TEXT_MUTED : NotchTheme.TEXT_DARK, false);
        if (waiting > 0) {
            NotchWidgets.primaryButton(ctx, this.font, takeX(), takeY(), TAKE_W, TAKE_H, "Take all",
                    over(mouseX, mouseY, takeX(), takeY(), TAKE_W, TAKE_H));
        }

        MailPostScreen.drawInventory(ctx, x, y, this.font);
        NotchWidgets.title(ctx, this.font, this.title.getString(), x + MailLayout.W / 2, y + 6, NotchWidgets.Plate.STAMP);
        //? if >=26.1 {
        /*super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private String summary(int waiting) {
        if (waiting <= 0) return "Nothing waiting.";
        if (waiting > MailInboxMenu.INBOX_SLOTS) {
            return MailInboxMenu.INBOX_SLOTS + " of " + waiting + " parcels";
        }
        return waiting + (waiting == 1 ? " parcel waiting" : " parcels waiting");
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
            if (MailTabs.click(this.leftPos, this.topPos, MailTabs.INBOX, mx, my)) return true;
            if (this.menu.waiting() > 0 && over(mx, my, takeX(), takeY(), TAKE_W, TAKE_H)) {
                NotchWidgets.click();
                NotchPacketsClient.sendMailTakeAll();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
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

    @Override
    public void removed() {
        super.removed();
        MailTabs.screenClosed();
    }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}
}
