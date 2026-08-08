package net.fugginbeenus.notchcurrency.client;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fugginbeenus.notchcurrency.compat.NetClient;
//? if <26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?}
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.client.AuctionTooltips;
import net.fugginbeenus.notchcurrency.auction.UserListingsScreenHandler;
import net.fugginbeenus.notchcurrency.client.UserListingsScreen;
import net.fugginbeenus.notchcurrency.crate.BarrelCleanupManager;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.trade.TradeScreen;
import net.fugginbeenus.notchcurrency.ui.ATMScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Packet channels must be declared before any receiver is registered or anything is sent.
        net.fugginbeenus.notchcurrency.compat.Net.declareChannels();

        // Keeps track of which screen is open. A no-op before 26.1, where Minecraft still tells us.
        net.fugginbeenus.notchcurrency.compat.Render.trackScreens();

        // Registry lookups from the render thread must use the client's registries (see RegistryAccess).
        net.fugginbeenus.notchcurrency.compat.RegistryAccess.setClientThreadCheck(
                () -> net.minecraft.client.Minecraft.getInstance().isSameThread());

        // The balance HUD ducks out of the way of wide chat lines (only matters on 1.21, where
        // HUD callbacks draw over chat).
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register(
                (message, overlay) -> { if (!overlay) NotchHud.noteChatMessage(message); });

        // Fee tags on the waystone selection menu: only when Waystones is actually present
        // (the overlay class references its GUI classes, so it must not load otherwise).
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("waystones")) {
            WaystoneFeeOverlay.register();
        }

        // Rebuild the custom-currency resource pack from the admin's art + config, before resources load.
        CurrencyPackGenerator.generate();

        // Our own copy of the player model layer, so animation packs can't hijack the NPC's poses.
        net.fugginbeenus.notchcurrency.client.npc.NpcModelLayers.register();

        // Entity renderers
        EntityRendererRegistry.register(ModEntities.BALLOON, BalloonRenderer::new);
        EntityRendererRegistry.register(ModEntities.NOTCH_NPC,
                net.fugginbeenus.notchcurrency.client.npc.NotchNpcRenderer::new);

        // Ledger Board draws the live leaderboard onto its face.
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                net.fugginbeenus.notchcurrency.registry.ModBlockEntities.LEDGER_BOARD,
                net.fugginbeenus.notchcurrency.client.render.LedgerBoardBlockEntityRenderer::new);
        // Coin Flip table renders an animated notch coin (arc + spin on flip).
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                net.fugginbeenus.notchcurrency.registry.ModBlockEntities.COIN_FLIP,
                net.fugginbeenus.notchcurrency.client.render.CoinFlipBlockEntityRenderer::new);

        // Cutout layer so the coin crest / standing coin's transparent corners aren't black.
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
                net.minecraft.client.renderer.RenderType.cutout(),
                net.fugginbeenus.notchcurrency.registry.ModBlocks.LEDGER_BOARD,
                net.fugginbeenus.notchcurrency.registry.ModBlocks.COIN_FLIP);

        BarrelCleanupManager.init();

        AuctionTooltips.init();

        // Screens
        MenuScreens.register(ModScreenHandlers.ATM, ATMScreen::new);
        MenuScreens.register(ModScreenHandlers.TRADE, TradeScreen::new);
        MenuScreens.register(ModScreenHandlers.RAFFLE, RaffleScreen::new);
        MenuScreens.register(ModScreenHandlers.RAFFLE_ADMIN, RaffleAdminScreen::new);
        MenuScreens.register(ModScreenHandlers.AUCTION_LISTING, AuctionListingScreen::new);
        MenuScreens.register(ModScreenHandlers.BOUNTY_BOARD, BountyBoardScreen::new);
        MenuScreens.register(ModScreenHandlers.BOUNTY_ADMIN, BountyAdminScreen::new);
        MenuScreens.register(ModScreenHandlers.LOAN, LoanScreen::new);
        MenuScreens.register(ModScreenHandlers.SLOT_MACHINE, SlotMachineScreen::new);
        MenuScreens.register(ModScreenHandlers.ENCHANTER, EnchanterScreen::new);
        MenuScreens.register(ModScreenHandlers.COSMETIC_SHOP, CosmeticShopScreen::new);
        MenuScreens.register(ModScreenHandlers.TRADE_OFFER_CREATE, TradeOfferCreateScreen::new);
        MenuScreens.register(ModScreenHandlers.TRADE_OFFERS, TradeOffersScreen::new);
        MenuScreens.register(ModScreenHandlers.RECEIPTS, ReceiptsScreen::new);
        MenuScreens.register(ModScreenHandlers.SHOP_BROWSE, ShopBrowseScreen::new);
        MenuScreens.register(ModScreenHandlers.SHOP_MANAGE, ShopManageScreen::new);
        MenuScreens.register(ModScreenHandlers.SHOP_LISTING_EDIT, ShopListingEditScreen::new);
        MenuScreens.register(ModScreenHandlers.COIN_FLIP, CoinFlipScreen::new);
        MenuScreens.register(ModScreenHandlers.NPC_EQUIP, NpcEquipScreen::new);

        //? if >=26.1 {
        /*net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                NotchCurrency.id("balance"), new NotchHud());
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                NotchCurrency.id("route"), new RouteHud());
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                NotchCurrency.id("bounty_tracker"), new BountyTrackerHud());
        *///?} else {
        HudRenderCallback.EVENT.register(new NotchHud());
        HudRenderCallback.EVENT.register(new RouteHud());
        HudRenderCallback.EVENT.register(new BountyTrackerHud());
        //?}
        NotchPacketsClient.registerBountyTrackerReceiver();
        NotchPacketsClient.registerCurrencySyncReceiver();

        // Toggle the bounty tracker HUD (default B).
        var trackerKey = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                new net.minecraft.client.KeyMapping("key.notchcurrency.bounty_tracker",
                        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_B,
                        "key.categories.notchcurrency"));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (trackerKey.consumeClick()) {
                BountyTrackerHud.toggle();
            }
        });

        // Balance sync → HUD
        NotchPacketsClient.registerBalanceReceiver(NotchHud::setBalance);

        NotchPacketsClient.registerNpcEditorReceiver();
        NotchPacketsClient.registerNpcDialogueReceiver();
        NotchPacketsClient.registerNpcStudioReceiver();
        NotchPacketsClient.registerNpcActionsReceiver();
        NotchPacketsClient.registerRecruiterReceiver();
        NotchPacketsClient.registerFactionListReceiver();
        NotchPacketsClient.registerNpcPresetReceiver();
        NotchPacketsClient.registerNpcScheduleReceiver();

        // Trade cancel / complete messages
        NetClient.registerClientReceiver(NotchPackets.TRADE_CANCEL, (client, buf) -> {
            String reason = buf.readUtf(64);
            client.execute(() -> {
                if (client.player != null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(client.player, Component.literal("Trade cancelled: " + reason).withStyle(ChatFormatting.RED));
                }
                if (client.screen instanceof TradeScreen) client.setScreen(null);
            });
        });
        NetClient.registerClientReceiver(NotchPackets.TRADE_COMPLETE, (client, buf) -> {
            client.execute(() -> {
                if (client.player != null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(client.player, Component.literal("Trade complete!").withStyle(ChatFormatting.GREEN));
                }
                if (client.screen instanceof TradeScreen) client.setScreen(null);
            });
        });

        // On world join (SP/MP), request our balance
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // StackData needs a registry lookup to decode carried stacks on 1.21+.
            if (client.level != null) {
                net.fugginbeenus.notchcurrency.compat.RegistryAccess.setClient(client.level.registryAccess());
            }
            NotchPacketsClient.requestBalance();
            CurrencyPackGenerator.remindIfDisabled(client);
        });

        // Screen handlers
        MenuScreens.register(
                ModScreenHandlers.AUCTION_HOUSE,
                AuctionHouseScreen::new
        );
        MenuScreens.register(
                ModScreenHandlers.USER_AUCTIONS,
                UserListingsScreen::new
        );

        // Auction item tooltips
        AuctionTooltips.init();
    }
}