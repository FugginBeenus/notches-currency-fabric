package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fugginbeenus.notchcurrency.client.hud.NotchHud;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.ui.ATMScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // ATM screen
        HandledScreens.register(ModScreenHandlers.ATM, ATMScreen::new);

        // HUD overlay
        HudRenderCallback.EVENT.register(new NotchHud());

        // Balance sync → HUD
        NotchPackets.registerClientReceiver(NotchHud::setBalance);

        // After the client joins any world (integrated or dedicated), ask the server for our balance.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            NotchPackets.requestBalance();
        });
    }
}
