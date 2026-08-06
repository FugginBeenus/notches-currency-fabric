package net.fugginbeenus.notchcurrency.client.ui;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;

import java.util.UUID;

public final class NpcPreviewWidget {

    private NotchNpcEntity cached;

    public void draw(DrawContext ctx, int x, int y, int w, int h, UUID npcId) {
        draw(ctx, x, y, w, h, npcId, false);
    }

    public void drawBust(DrawContext ctx, int x, int y, int w, int h, UUID npcId) {
        draw(ctx, x, y, w, h, npcId, true);
    }

    private void draw(DrawContext ctx, int x, int y, int w, int h, UUID npcId, boolean bust) {
        NotchWidgets.inset(ctx, x, y, w, h, NotchTheme.DEEP);
        if (npcId == null) return;
        NotchNpcEntity npc = find(npcId);
        if (npc == null) {
            NotchWidgets.centerText(ctx, MinecraftClient.getInstance().textRenderer, "…",
                    x + w / 2, y + h / 2 - 4, NotchTheme.TEXT_MUTED, false);
            return;
        }

        float mh = Math.max(0.6f, npc.getHeight());
        float mw = Math.max(0.6f, npc.getWidth());
        int cx = x + w / 2;
        int size;
        int feetY;
        if (bust && NotchNpcEntity.MODEL_HUMANOID.equals(npc.getModelId())) {
            // Waist-up: scale so head-top→waist (~60% of the height) fills the box, then drop the
            // figure ~a quarter of the box for headroom; the legs clip below via the scissor.
            size = (int) Math.max(6, (h - 10) / (0.61f * mh));
            feetY = y + 5 + h / 4 + (int) (mh * size);
        } else {
            // Fit the whole figure to the box, feet near the bottom, head with a small top margin.
            size = (int) Math.max(6, Math.min((h - 14) / mh, (w - 12) / mw));
            feetY = y + h - 8;
        }

        // Face the viewer; unhide for the preview; restore afterwards.
        float oldYaw = npc.getYaw(), oldBody = npc.bodyYaw, oldHead = npc.headYaw;
        boolean wasInvisible = npc.isInvisible();
        npc.setYaw(180f);
        npc.bodyYaw = 180f;
        npc.headYaw = 180f;
        npc.setInvisible(false);
        // Hide the nametag while rendering the portrait.
        net.minecraft.text.Text oldName = npc.getCustomName();
        boolean oldNameVisible = npc.isCustomNameVisible();
        npc.setCustomName(null);
        npc.setCustomNameVisible(false);

        ctx.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx, cx, feetY, size, 0f, 0f, npc);
        ctx.disableScissor();

        npc.setYaw(oldYaw);
        npc.bodyYaw = oldBody;
        npc.headYaw = oldHead;
        npc.setInvisible(wasInvisible);
        npc.setCustomName(oldName);
        npc.setCustomNameVisible(oldNameVisible);
    }

    private NotchNpcEntity find(UUID npcId) {
        if (cached != null && !cached.isRemoved() && cached.getUuid().equals(npcId)) return cached;
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null) return null;
        for (Entity e : c.world.getEntities()) {
            if (e instanceof NotchNpcEntity n && n.getUuid().equals(npcId)) {
                cached = n;
                return n;
            }
        }
        return null;
    }
}
