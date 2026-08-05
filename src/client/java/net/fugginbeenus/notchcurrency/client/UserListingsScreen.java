package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.auction.UserListingsScreenHandler;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class UserListingsScreen extends HandledScreen<UserListingsScreenHandler> {

    private static final Identifier TEX =
            NotchCurrency.id("textures/gui/auction/userauctions.png");

    // --- popup window size within the texture ---
    // userauctions.png has the small window in the top-left,
    // so we only draw that subsection.
    private static final int POPUP_W = 176;
    private static final int POPUP_H = 96;

    // --- GUI-local layout (0,0 = top-left of popup) ---

    // Player name label
    private static final int NAME_X = 12;   // where the text “FugginBeenus” should start
    private static final int NAME_Y = 14;   // vertical baseline of the name

    // Close (X) button
    private static final int CLOSE_X = 156; // top-right red X hitbox
    private static final int CLOSE_Y = 8;
    private static final int CLOSE_W = 14;
    private static final int CLOSE_H = 14;

    // (Optional) minus / collapse button, if you want to use it later
    private static final int MINUS_X = 140;
    private static final int MINUS_Y = 8;
    private static final int MINUS_W = 12;
    private static final int MINUS_H = 14;

    // Listing slots grid (you can tweak to match the plus/lock squares)
    private static final int SLOT_START_X = 20;
    private static final int SLOT_START_Y = 31;
    private static final int SLOT_SIZE    = 18;

    private static final boolean DEBUG_OUTLINES = false; // set true while lining up hitboxes

    private ButtonWidget closeButton;
    private ButtonWidget minusButton; // not wired yet, but reserved

    public UserListingsScreen(UserListingsScreenHandler handler,
                              PlayerInventory playerInventory,
                              Text title) {
        super(handler, playerInventory, title);

        this.backgroundWidth  = POPUP_W;
        this.backgroundHeight = POPUP_H;

        // Hide default titles; we draw our own player name
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();

        // Center popup in the middle of the screen
        this.x = (this.width  - this.backgroundWidth)  / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        // If you want a slight nudge relative to the AH screen, adjust here:
        int xNudge = 0;  // e.g. +10 to move right a bit
        int yNudge = -10; // move slightly up if you like
        this.x += xNudge;
        this.y += yNudge;

        this.clearChildren();

        // Close (X) button
        closeButton = ButtonWidget.builder(Text.empty(), b -> {
                    // simply close the popup
                    if (this.client != null) {
                        this.client.player.closeHandledScreen();
                    }
                })
                .dimensions(this.x + CLOSE_X, this.y + CLOSE_Y, CLOSE_W, CLOSE_H)
                .build();
        closeButton.setAlpha(0.0f); // invisible; texture provides the visuals
        addDrawableChild(closeButton);

        // Optional minus button (currently does nothing but you can hook it later)
        minusButton = ButtonWidget.builder(Text.empty(), b -> {
                    // TODO: collapse / minimize if you want that behavior
                })
                .dimensions(this.x + MINUS_X, this.y + MINUS_Y, MINUS_W, MINUS_H)
                .build();
        minusButton.setAlpha(0.0f);
        addDrawableChild(minusButton);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        // Draw just the popup window from the texture (top-left 176×96)
        ctx.drawTexture(TEX,
                this.x, this.y,       // screen position
                0, 0,                 // U,V in the texture
                this.backgroundWidth, // width to draw
                this.backgroundHeight // height to draw
        );
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // Draw player name in the header area
        String name = this.client != null && this.client.player != null
                ? this.client.player.getName().getString()
                : "";

        ctx.drawText(
                this.textRenderer,
                name,
                NAME_X,   // GUI-local coords (relative to popup)
                NAME_Y,
                0x404040, // dark gray like vanilla
                false
        );

        // (No inventory titles; all visual labels are baked into the texture)
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Don’t dim the whole screen; we want to see the world / AH behind
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);

        if (DEBUG_OUTLINES) {
            outline(ctx, closeButton, 0xFFFF0000); // red
            outline(ctx, minusButton, 0xFFFFFF00); // yellow
        }
    }

    private void outline(DrawContext ctx, ButtonWidget btn, int color) {
        int x1 = btn.getX();
        int y1 = btn.getY();
        int x2 = x1 + btn.getWidth();
        int y2 = y1 + btn.getHeight();
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
