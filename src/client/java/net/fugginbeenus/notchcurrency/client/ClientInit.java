package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.client.AuctionTooltips;
import net.fugginbeenus.notchcurrency.client.entity.ShopkeeperRenderer;
import net.fugginbeenus.notchcurrency.auction.UserListingsScreenHandler;
import net.fugginbeenus.notchcurrency.client.UserListingsScreen;
import net.fugginbeenus.notchcurrency.crate.BarrelCleanupManager;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.trade.TradeScreen;
import net.fugginbeenus.notchcurrency.ui.ATMScreen;

import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Entity renderers
        EntityRendererRegistry.register(ModEntities.BALLOON, BalloonRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHOPKEEPER, ShopkeeperRenderer::new);

        BarrelCleanupManager.init();

        AuctionTooltips.init();

        // Screens
        HandledScreens.register(ModScreenHandlers.ATM, ATMScreen::new);
        HandledScreens.register(ModScreenHandlers.TRADE, TradeScreen::new);
        HandledScreens.register(ModScreenHandlers.PLAYER_SHOP, PlayerShopScreen::new);

        HudRenderCallback.EVENT.register(new NotchHud());

        // Balance sync → HUD
        NotchPacketsClient.registerBalanceReceiver(NotchHud::setBalance);

        // Shopkeeper settings screen receiver
        NotchPacketsClient.registerShopkeeperSettingsReceiver();

        // Trade cancel / complete messages
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.TRADE_CANCEL, (client, h, buf, response) -> {
            String reason = buf.readString(64);
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("Trade cancelled: " + reason).formatted(Formatting.RED), false);
                }
                if (client.currentScreen instanceof TradeScreen) client.setScreen(null);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.TRADE_COMPLETE, (client, h, buf, response) -> {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("Trade complete!").formatted(Formatting.GREEN), false);
                }
                if (client.currentScreen instanceof TradeScreen) client.setScreen(null);
            });
        });

        // On world join (SP/MP), request our balance
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            NotchPacketsClient.requestBalance();
        });

        // Screen handlers
        ScreenRegistry.register(
                ModScreenHandlers.AUCTION_HOUSE,
                AuctionHouseScreen::new
        );
        ScreenRegistry.register(
                ModScreenHandlers.USER_AUCTIONS,
                UserListingsScreen::new
        );

        // Auction item tooltips
        AuctionTooltips.init();
    }
}