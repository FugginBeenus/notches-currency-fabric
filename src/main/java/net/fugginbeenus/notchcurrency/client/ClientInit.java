package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fugginbeenus.notchcurrency.client.hud.NotchHud;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.trade.TradeScreen;
import net.fugginbeenus.notchcurrency.ui.ATMScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // ATM screen
        HandledScreens.register(ModScreenHandlers.ATM, ATMScreen::new);

        // Trade screen
        HandledScreens.register(ModScreenHandlers.TRADE, TradeScreen::new);

        // HUD overlay
        HudRenderCallback.EVENT.register(new NotchHud());

        // Balance sync → HUD
        NotchPackets.registerClientReceiver(NotchHud::setBalance);

        // Trade cancel / complete messages
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.TRADE_CANCEL, (client, h, buf, response) -> {
            String reason = buf.readString(64);
            client.execute(() -> {
                if (client.player != null) client.player.sendMessage(Text.literal("Trade cancelled: " + reason).formatted(Formatting.RED), false);
                if (client.currentScreen instanceof TradeScreen) client.setScreen(null);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.TRADE_COMPLETE, (client, h, buf, response) -> {
            client.execute(() -> {
                if (client.player != null) client.player.sendMessage(Text.literal("Trade complete!").formatted(Formatting.GREEN), false);
                if (client.currentScreen instanceof TradeScreen) client.setScreen(null);
            });
        });

        // On world join (SP/MP), request our balance
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            NotchPackets.requestBalance();
        });
    }
}
