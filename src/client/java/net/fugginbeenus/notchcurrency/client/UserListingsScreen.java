package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.auction.UserListingsScreenHandler;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class UserListingsScreen extends AbstractContainerScreen<UserListingsScreenHandler> {

    private static final ResourceLocation TEX =
            NotchCurrency.id("textures/gui/auction/userauctions.png");

    private static final int POPUP_W = 176;
    private static final int POPUP_H = 96;
    private static final int NAME_X = 12;
    private static final int NAME_Y = 14;
    private static final int CLOSE_X = 156;
    private static final int CLOSE_Y = 8;
    private static final int CLOSE_W = 14;
    private static final int CLOSE_H = 14;
    private static final int MINUS_X = 140;
    private static final int MINUS_Y = 8;
    private static final int MINUS_W = 12;
    private static final int MINUS_H = 14;
    private static final int SLOT_START_X = 20;
    private static final int SLOT_START_Y = 31;
    private static final int SLOT_SIZE    = 18;

    private static final boolean DEBUG_OUTLINES = false;

    private Button closeButton;
    private Button minusButton;

    public UserListingsScreen(UserListingsScreenHandler handler,
                              Inventory playerInventory,
                              Component title) {
        //? if >=26.1 {
        /*super(handler, playerInventory, title, POPUP_W, POPUP_H);
        *///?} else {
        super(handler, playerInventory, title);
        this.imageWidth  = POPUP_W;
        this.imageHeight = POPUP_H;
        //?}

        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width  - this.imageWidth)  / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        int xNudge = 0;
        int yNudge = -10;
        this.leftPos += xNudge;
        this.topPos += yNudge;

        this.clearWidgets();

        closeButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.player.closeContainer();
                    }
                })
                .bounds(this.leftPos + CLOSE_X, this.topPos + CLOSE_Y, CLOSE_W, CLOSE_H)
                .build();
        closeButton.setAlpha(0.0f);
        addRenderableWidget(closeButton);

        minusButton = Button.builder(Component.empty(), b -> {
                    // TODO: collapse/minimize
                })
                .bounds(this.leftPos + MINUS_X, this.topPos + MINUS_Y, MINUS_W, MINUS_H)
                .build();
        minusButton.setAlpha(0.0f);
        addRenderableWidget(minusButton);
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        //? if >=1.21.11 {
        /*ctx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEX,
                this.leftPos, this.topPos, 0f, 0f,
                this.imageWidth, this.imageHeight, 256, 256);
        *///?} else {
        ctx.blit(TEX,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth,
                this.imageHeight
        );
        //?}
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
        String name = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getName().getString()
                : "";

        ctx.drawString(
                this.font,
                name,
                NAME_X,
                NAME_Y,
                0xFF404040,
                false
        );
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        this.renderTooltip(ctx, mouseX, mouseY);

        if (DEBUG_OUTLINES) {
            outline(ctx, closeButton, 0xFFFF0000);
            outline(ctx, minusButton, 0xFFFFFF00);
        }
    }

    private void outline(GuiGraphics ctx, Button btn, int color) {
        int x1 = btn.getX();
        int y1 = btn.getY();
        int x2 = x1 + btn.getWidth();
        int y2 = y1 + btn.getHeight();
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
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
