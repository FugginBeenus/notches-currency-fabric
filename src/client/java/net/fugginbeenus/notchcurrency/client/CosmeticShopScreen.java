package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.client.ui.NpcPreviewWidget;
import net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class CosmeticShopScreen extends AbstractContainerScreen<CosmeticShopScreenHandler> {

    private static final int W = 248, H = 240;
    private static final int LIST_X = 8, LIST_Y = 22, ROW_W = 148, ROW_H = 20, ROW_STEP = 21;
    private static final int SB_X = 160, SB_Y = 22, SB_W = 8, SB_H = 126;
    private static final int PV_X = 174, PV_Y = 22, PV_W = 68, PV_H = 126;

    private static ItemStack coin;

    private static ItemStack coin() {
        if (coin == null) coin = new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);
        return coin;
    }

    private final NpcPreviewWidget preview = new NpcPreviewWidget();
    private boolean draggingScroll;

    public CosmeticShopScreen(CosmeticShopScreenHandler handler, Inventory inv, Component title) {
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

    private record Cell(ItemStack icon, String id, String name, long price, boolean owned) {}

    private Cell cell(int i) {
        ItemStack s = menu.rowStack(i);
        if (s.isEmpty()) return null;
        CompoundTag t = StackData.getData(s);
        if (!t.contains("nc_cid")) return null;
        return new Cell(s, t.getString("nc_cid"), t.getString("nc_name"),
                t.getLong("nc_price"), t.getBoolean("nc_owned"));
    }

    private int rowY(int i) { return this.topPos + LIST_Y + i * ROW_STEP; }
    private int pages() { return Math.max(1, menu.prop(CosmeticShopScreenHandler.P_TOTAL_PAGES)); }
    private int page() { return menu.prop(CosmeticShopScreenHandler.P_PAGE); }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Cosmetics", x + W / 2, y + 7);
        NotchWidgets.inset(ctx, x + 6, y + 20, 152, 130, NotchTheme.PANEL_MID);
        NotchWidgets.inset(ctx, x + 172, y + 20, 72, 130, NotchTheme.PANEL_MID);
        for (int i = 0; i < CosmeticShopScreenHandler.VIS_ROWS; i++) {
            Cell c = cell(i);
            if (c == null) continue;
            int ry = rowY(i);
            boolean hover = !c.owned() && over(mouseX, mouseY, x + LIST_X, ry, ROW_W, ROW_H);
            NotchWidgets.button(ctx, x + LIST_X, ry, ROW_W, ROW_H, hover, false);

            if (c.owned()) {
                ctx.drawString(this.font, Component.literal("Owned").withStyle(ChatFormatting.DARK_GREEN),
                        x + LIST_X + 6, ry + 6, NotchTheme.TEXT_DARK, false);
            } else {
                ctx.renderItem(coin(), x + LIST_X + 4, ry + 2);
                ctx.renderItemDecorations(this.font, coin(), x + LIST_X + 4, ry + 2,
                        NotchWidgets.compactCount(c.price()));
            }
            arrow(ctx, x + LIST_X + ROW_W - 40, ry + 6, NotchTheme.TEXT_MUTED);
            ctx.renderItem(c.icon(), x + LIST_X + ROW_W - 20, ry + 2);
        }

        NotchWidgets.slot(ctx, x + SB_X, y + SB_Y, SB_W, SB_H);
        if (pages() > 1) {
            NotchWidgets.button(ctx, x + SB_X + 1, thumbY(), SB_W - 2, thumbH(), false, false);
        }
        preview.draw(ctx, x + PV_X, y + PV_Y, PV_W, PV_H, menu.npcId());

        NotchWidgets.divider(ctx, x + 8, y + 153, W - 16);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + 43 + col * 18 - 1, y + 158 + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + 43 + col * 18 - 1, y + 158 + 58 - 1);
        }
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private static void arrow(GuiGraphics ctx, int x, int y, int color) {
        ctx.fill(x, y + 3, x + 10, y + 5, color);
        for (int i = 0; i < 4; i++) {
            ctx.fill(x + 10 + i, y + i, x + 11 + i, y + 8 - i, color);
        }
    }

    private int thumbH() { return Math.max(18, SB_H / pages()); }
    private int thumbY() {
        int tp = pages();
        if (tp <= 1) return this.topPos + SB_Y;
        return this.topPos + SB_Y + (int) ((page() / (double) (tp - 1)) * (SB_H - thumbH()));
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
        for (int i = 0; i < CosmeticShopScreenHandler.VIS_ROWS; i++) {
            Cell c = cell(i);
            if (c != null && over(mouseX, mouseY, leftPos + LIST_X, rowY(i), ROW_W, ROW_H)) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(c.name()));
                lines.add(NotchWidgets.priceText(c.price(), "", 0));
                lines.add(c.owned() ? Component.literal("Owned").withStyle(ChatFormatting.GREEN)
                        : Component.literal("Click to buy").withStyle(ChatFormatting.DARK_GRAY));
                ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
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
            for (int i = 0; i < CosmeticShopScreenHandler.VIS_ROWS; i++) {
                Cell c = cell(i);
                if (c != null && !c.owned() && over(mx, my, leftPos + LIST_X, rowY(i), ROW_W, ROW_H)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendCosmeticBuy(c.id());
                    return true;
                }
            }
            if (pages() > 1 && over(mx, my, leftPos + SB_X, topPos + SB_Y, SB_W, SB_H)) {
                if (my < thumbY()) { NotchWidgets.tick(); clickButton(0); return true; }
                if (my >= thumbY() + thumbH()) { NotchWidgets.tick(); clickButton(1); return true; }
                draggingScroll = true;
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
    //?}
        if (draggingScroll && pages() > 1) {
            int rel = (int) mouseY - (this.topPos + SB_Y) - thumbH() / 2;
            int track = SB_H - thumbH();
            int target = track <= 0 ? 0 : Math.round(rel / (float) track * (pages() - 1));
            target = Math.max(0, Math.min(pages() - 1, target));
            if (target != page()) clickButton(target > page() ? 1 : 0);
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseDragged(event, dx, dy);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //?}
        draggingScroll = false;
        //? if >=1.21.11 {
        /*return super.mouseReleased(event);
        *///?} else {
        return super.mouseReleased(mouseX, mouseY, button);
        //?}
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        if (pages() > 1) {
            if (amount < 0) clickButton(1);
            else if (amount > 0) clickButton(0);
            return true;
        }
        //? if >=1.21 {
        /*return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        *///?} else {
        return super.mouseScrolled(mouseX, mouseY, amount);
        //?}
    }

    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
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
