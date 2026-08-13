package net.fugginbeenus.notchcurrency.client;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fugginbeenus.notchcurrency.compat.NetClient;
//? if <1.21.11 {
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

        // Our panels are laid out at a fixed size, so on a small display at a high GUI scale they run
        // off the edges. One rule for all of them, rather than thirty-seven layouts to rework.
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (net.fugginbeenus.notchcurrency.compat.GuiScale.isOurs(screen)) {
                net.fugginbeenus.notchcurrency.compat.GuiScale.fit(client, screen);
                // Closing to the world fires no init anywhere, so the scale goes back from here. No
                // relayout: the screen is on its way out, and anything opening next lays itself out.
                net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.remove(screen).register(
                        closed -> net.fugginbeenus.notchcurrency.compat.GuiScale.release(client, null));
            } else {
                net.fugginbeenus.notchcurrency.compat.GuiScale.release(client, screen);
            }
        });

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
        //? if >=26.1 {
        /*// 26.x reads the layer off the block model instead; see render_type in the model json.
        *///?} elif >=1.21.11 {
        /*net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT,
                net.fugginbeenus.notchcurrency.registry.ModBlocks.LEDGER_BOARD,
                net.fugginbeenus.notchcurrency.registry.ModBlocks.COIN_FLIP);
        *///?} else {
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
                net.minecraft.client.renderer.RenderType.cutout(),
                net.fugginbeenus.notchcurrency.registry.ModBlocks.LEDGER_BOARD,
                net.fugginbeenus.notchcurrency.registry.ModBlocks.COIN_FLIP);
        //?}

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
        MenuScreens.register(ModScreenHandlers.MAIL_POST, MailPostScreen::new);
        MenuScreens.register(ModScreenHandlers.MAIL_INBOX, MailInboxScreen::new);

        //? if >=1.21.11 {
        /*net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                net.fugginbeenus.notchcurrency.core.NotchCurrency.id("balance"), new NotchHud());
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                net.fugginbeenus.notchcurrency.core.NotchCurrency.id("route"), new RouteHud());
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                net.fugginbeenus.notchcurrency.core.NotchCurrency.id("bounty_tracker"), new BountyTrackerHud());
        *///?} else {
        HudRenderCallback.EVENT.register(new NotchHud());
        HudRenderCallback.EVENT.register(new RouteHud());
        HudRenderCallback.EVENT.register(new BountyTrackerHud());
        //?}
        NotchPacketsClient.registerBountyTrackerReceiver();
        NotchPacketsClient.registerCurrencySyncReceiver();

        // Toggle the bounty tracker HUD (default B).
        var trackerKey = //? if >=26.1 {
                /*net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                *///?} else {
                net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                //?}
                //? if >=1.21.11 {
                /*new net.minecraft.client.KeyMapping("key.notchcurrency.bounty_tracker",
                        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_B,
                        net.minecraft.client.KeyMapping.Category.MISC));
                *///?} else {
                new net.minecraft.client.KeyMapping("key.notchcurrency.bounty_tracker",
                        com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_B,
                        "key.categories.notchcurrency"));
                //?}
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
        NotchPacketsClient.registerMailRecipientsReceiver();
        NotchPacketsClient.registerModelReloadReceiver();
        NotchPacketsClient.registerNpcModelReceivers();
        NotchPacketsClient.registerBalloonConfigReceiver();
        net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelHint.register();
        NotchPacketsClient.registerMailAimReceiver();
        NotchPacketsClient.registerNpcPresetReceiver();
        NotchPacketsClient.registerNpcScheduleReceiver();

        // Trade cancel / complete messages
        NetClient.registerClientReceiver(NotchPackets.TRADE_CANCEL, (client, buf) -> {
            String reason = buf.readUtf(64);
            client.execute(() -> {
                if (client.player != null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(client.player, Component.literal("Trade cancelled: " + reason).withStyle(ChatFormatting.RED));
                }
                if (net.fugginbeenus.notchcurrency.compat.Render.currentScreen() instanceof TradeScreen) client.setScreen(null);
            });
        });
        NetClient.registerClientReceiver(NotchPackets.TRADE_COMPLETE, (client, buf) -> {
            client.execute(() -> {
                if (client.player != null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(client.player, Component.literal("Trade complete!").withStyle(ChatFormatting.GREEN));
                }
                if (net.fugginbeenus.notchcurrency.compat.Render.currentScreen() instanceof TradeScreen) client.setScreen(null);
            });
        });

        // Custom NPC models are read once, quietly, when a world opens. Doing it here rather than
        // at client start means the reload lands while a screen is already up.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> client.execute(() ->
                        net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelPacks.reload(client, false)));

        // Leaving a server abandons anything half downloaded rather than carrying it to the next.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelDownloads.reset();
                    net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelHint.reset();
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