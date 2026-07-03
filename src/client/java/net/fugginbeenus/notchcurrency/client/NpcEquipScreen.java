package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.npc.NpcEquipScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * The NPC equipment screen: drop armor into the typed slots (helmet/chest/legs/boots), anything into
 * the hands, straight from your inventory — chest-style. Changes land on the NPC instantly.
 */
public class NpcEquipScreen extends HandledScreen<NpcEquipScreenHandler> {

    private static final int W = 176, H = 166;

    public NpcEquipScreen(NpcEquipScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Equipment", x + W / 2, y + 4);

        // Armor column + labels.
        String[] labels = {"Helmet", "Chest", "Legs", "Boots"};
        for (int i = 0; i < 4; i++) {
            NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.ARMOR_X - 1, y + NpcEquipScreenHandler.ARMOR_Y - 1 + i * 18);
            ctx.drawText(this.textRenderer, labels[i], x + NpcEquipScreenHandler.ARMOR_X + 20,
                    y + NpcEquipScreenHandler.ARMOR_Y + 4 + i * 18, NotchTheme.TEXT_MUTED, false);
        }

        // Hands.
        NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.HAND_X - 1, y + NpcEquipScreenHandler.MAIN_Y - 1);
        ctx.drawText(this.textRenderer, "Hand", x + NpcEquipScreenHandler.HAND_X + 20,
                y + NpcEquipScreenHandler.MAIN_Y + 4, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.HAND_X - 1, y + NpcEquipScreenHandler.OFF_Y - 1);
        ctx.drawText(this.textRenderer, "Offhand", x + NpcEquipScreenHandler.HAND_X + 20,
                y + NpcEquipScreenHandler.OFF_Y + 4, NotchTheme.TEXT_MUTED, false);

        // Player inventory + hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.INV_X - 1 + col * 18,
                        y + NpcEquipScreenHandler.INV_Y - 1 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.INV_X - 1 + col * 18,
                    y + NpcEquipScreenHandler.HOTBAR_Y - 1);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
