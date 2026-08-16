package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.client.ui.NpcPreviewWidget;
import net.fugginbeenus.notchcurrency.npc.NpcEquipScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public class NpcEquipScreen extends AbstractContainerScreen<NpcEquipScreenHandler> {

    private static final int W = 248, H = 240;
    private static final int PV_X = 174, PV_Y = 22, PV_W = 68, PV_H = 126;

    private static final String[] SLOT_LABELS = {"Helmet", "Chest", "Legs", "Boots", "Hand", "Offhand"};
    private static final String[] SLOT_HINTS = {
            "Any head item - armor, skulls, carved pumpkins.",
            "Any chestplate (or elytra).",
            "Any leggings.",
            "Any boots.",
            "Anything - tools, weapons, or just a prop.",
            "Anything - shields live here."
    };

    private final NpcPreviewWidget preview = new NpcPreviewWidget();

    public NpcEquipScreen(NpcEquipScreenHandler handler, Inventory inv, Component title) {
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

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Equipment", x + W / 2, y + 7);
        NotchWidgets.inset(ctx, x + 6, y + 20, 152, 130, NotchTheme.PANEL_MID);
        NotchWidgets.inset(ctx, x + 172, y + 20, 72, 130, NotchTheme.PANEL_MID);

        for (int i = 0; i < 4; i++) {
            NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.ARMOR_X - 1, y + NpcEquipScreenHandler.ARMOR_Y - 1 + i * 18);
            ctx.drawString(this.font, SLOT_LABELS[i], x + NpcEquipScreenHandler.ARMOR_X + 22,
                    y + NpcEquipScreenHandler.ARMOR_Y + 4 + i * 18, NotchTheme.TEXT_DARK, false);
        }

        for (int i = 0; i < menu.trinketCount(); i++) {
            NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.TRINKET_X - 1 + (i % 2) * 18,
                    y + NpcEquipScreenHandler.TRINKET_Y - 1 + (i / 2) * 18);
        }

        NotchWidgets.divider(ctx, x + 12, y + NpcEquipScreenHandler.MAIN_Y - 8, 140);
        NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.HAND_X - 1, y + NpcEquipScreenHandler.MAIN_Y - 1);
        ctx.drawString(this.font, SLOT_LABELS[4], x + NpcEquipScreenHandler.HAND_X + 22,
                y + NpcEquipScreenHandler.MAIN_Y + 4, NotchTheme.TEXT_DARK, false);
        NotchWidgets.slot(ctx, x + NpcEquipScreenHandler.OFF_X - 1, y + NpcEquipScreenHandler.OFF_Y - 1);
        ctx.drawString(this.font, SLOT_LABELS[5], x + NpcEquipScreenHandler.OFF_X + 22,
                y + NpcEquipScreenHandler.OFF_Y + 4, NotchTheme.TEXT_DARK, false);

        preview.draw(ctx, x + PV_X, y + PV_Y, PV_W, PV_H, menu.npcId());

        NotchWidgets.divider(ctx, x + 8, y + 153, W - 16);
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
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
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
        this.renderTooltip(ctx, mouseX, mouseY);

        // Empty gear and trinket slots explain what they take.
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            int id = this.menu.slots.indexOf(this.hoveredSlot);
            List<Component> lines = null;
            if (id >= 0 && id < 6) {
                lines = List.of(
                        Component.literal(SLOT_LABELS[id]).withStyle(ChatFormatting.WHITE),
                        Component.literal(SLOT_HINTS[id]).withStyle(ChatFormatting.GRAY));
            } else if (id >= 42) {
                lines = List.of(
                        Component.literal("Trinket: " + menu.trinketLabel(id - 42)).withStyle(ChatFormatting.WHITE),
                        Component.literal("Accessory slot from the Trinkets mod.").withStyle(ChatFormatting.GRAY));
            }
            if (lines != null) ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
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
