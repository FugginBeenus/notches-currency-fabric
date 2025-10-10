package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.ui.ATMScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

public class NotchCurrencyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Screen wiring (unchanged)
        HandledScreens.register(ModScreenHandlers.ATM, ATMScreen::new);

        // HUD: small “Notches” readout near hunger/armor, with mount-aware Y offset
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            var mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            // —— Positioning relative to vanilla hotbar/hunger area ——
            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();

            // Vanilla hotbar is 182 wide and sits at the bottom (22px tall). The right “edge”
            // used by hunger starts at center + 91. We’ll place our readout just above it.
            int hotbarY = sh - 22;

            // If on a mount with a jump/health bar, vanilla shifts some bars upward.
            boolean mounted = mc.player.getVehicle() instanceof LivingEntity;
            int yLiftForMount = mounted ? 10 : 0;

            // Final placement: a few px above the hotbar, slightly inset from the right side of the hotbar
            int baseX = (sw / 2) + 94;       // to the right of the hotbar center
            int baseY = hotbarY - 11 - yLiftForMount;

            // Draw small scaled text
            var tr = mc.textRenderer;
            String label = "Notches:";
            String value = "0"; // TODO: replace with synced balance later

            // scale down to 85%
            ctx.getMatrices().push();
            float s = 0.85f;
            ctx.getMatrices().translate(baseX, baseY, 0);
            ctx.getMatrices().scale(s, s, 1.0f);

            // subtle shadow, vanilla-like
            ctx.drawText(tr, Text.literal(label), 0, 0, 0xFFB0B0B0, false);
            int labelW = tr.getWidth(label);
            ctx.drawText(tr, Text.literal(value), labelW + 4, 0, 0xFFFFFFFF, true);

            ctx.getMatrices().pop();
        });

        System.out.println("[NotchCurrency] onInitializeClient finished.");
    }
}
