package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
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

import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Packet channels must be declared before any receiver is registered or anything is sent.
        net.fugginbeenus.notchcurrency.compat.Net.declareChannels();

        // Registry lookups from the render thread must use the client's registries (see RegistryAccess).
        net.fugginbeenus.notchcurrency.compat.RegistryAccess.setClientThreadCheck(
                () -> net.minecraft.client.MinecraftClient.getInstance().isOnThread());

        // Rebuild the custom-currency resource pack from the admin's art + config, before resources load.
        CurrencyPackGenerator.generate();

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
                net.minecraft.client.render.RenderLayer.getCutout(),
                net.fugginbeenus.notchcurrency.registry.ModBlocks.LEDGER_BOARD,
                net.fugginbeenus.notchcurrency.registry.ModBlocks.COIN_FLIP);

        BarrelCleanupManager.init();

        AuctionTooltips.init();

        // Screens
        HandledScreens.register(ModScreenHandlers.ATM, ATMScreen::new);
        HandledScreens.register(ModScreenHandlers.TRADE, TradeScreen::new);
        HandledScreens.register(ModScreenHandlers.RAFFLE, RaffleScreen::new);
        HandledScreens.register(ModScreenHandlers.RAFFLE_ADMIN, RaffleAdminScreen::new);
        HandledScreens.register(ModScreenHandlers.AUCTION_LISTING, AuctionListingScreen::new);
        HandledScreens.register(ModScreenHandlers.BOUNTY_BOARD, BountyBoardScreen::new);
        HandledScreens.register(ModScreenHandlers.BOUNTY_ADMIN, BountyAdminScreen::new);
        HandledScreens.register(ModScreenHandlers.LOAN, LoanScreen::new);
        HandledScreens.register(ModScreenHandlers.SLOT_MACHINE, SlotMachineScreen::new);
        HandledScreens.register(ModScreenHandlers.ENCHANTER, EnchanterScreen::new);
        HandledScreens.register(ModScreenHandlers.COSMETIC_SHOP, CosmeticShopScreen::new);
        HandledScreens.register(ModScreenHandlers.TRADE_OFFER_CREATE, TradeOfferCreateScreen::new);
        HandledScreens.register(ModScreenHandlers.TRADE_OFFERS, TradeOffersScreen::new);
        HandledScreens.register(ModScreenHandlers.RECEIPTS, ReceiptsScreen::new);
        HandledScreens.register(ModScreenHandlers.SHOP_BROWSE, ShopBrowseScreen::new);
        HandledScreens.register(ModScreenHandlers.SHOP_MANAGE, ShopManageScreen::new);
        HandledScreens.register(ModScreenHandlers.SHOP_LISTING_EDIT, ShopListingEditScreen::new);
        HandledScreens.register(ModScreenHandlers.COIN_FLIP, CoinFlipScreen::new);
        HandledScreens.register(ModScreenHandlers.NPC_EQUIP, NpcEquipScreen::new);

        HudRenderCallback.EVENT.register(new NotchHud());
        HudRenderCallback.EVENT.register(new RouteHud());
        HudRenderCallback.EVENT.register(new BountyTrackerHud());
        NotchPacketsClient.registerBountyTrackerReceiver();
        NotchPacketsClient.registerCurrencySyncReceiver();

        // Toggle the bounty tracker HUD (default B).
        var trackerKey = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
                new net.minecraft.client.option.KeyBinding("key.notchcurrency.bounty_tracker",
                        net.minecraft.client.util.InputUtil.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_B,
                        "key.categories.notchcurrency"));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (trackerKey.wasPressed()) {
                BountyTrackerHud.toggle();
            }
        });

        // Balance sync → HUD
        NotchPacketsClient.registerBalanceReceiver(NotchHud::setBalance);

        NotchPacketsClient.registerNpcEditorReceiver();
        NotchPacketsClient.registerNpcDialogueReceiver();
        NotchPacketsClient.registerNpcStudioReceiver();
        NotchPacketsClient.registerNpcPresetReceiver();

        // Trade cancel / complete messages
        NetClient.registerClientReceiver(NotchPackets.TRADE_CANCEL, (client, buf) -> {
            String reason = buf.readString(64);
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("Trade cancelled: " + reason).formatted(Formatting.RED), false);
                }
                if (client.currentScreen instanceof TradeScreen) client.setScreen(null);
            });
        });
        NetClient.registerClientReceiver(NotchPackets.TRADE_COMPLETE, (client, buf) -> {
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
            // StackData needs a registry lookup to decode carried stacks on 1.21+.
            if (client.world != null) {
                net.fugginbeenus.notchcurrency.compat.RegistryAccess.setClient(client.world.getRegistryManager());
            }
            NotchPacketsClient.requestBalance();
            CurrencyPackGenerator.remindIfDisabled(client);
        });

        // Screen handlers
        HandledScreens.register(
                ModScreenHandlers.AUCTION_HOUSE,
                AuctionHouseScreen::new
        );
        HandledScreens.register(
                ModScreenHandlers.USER_AUCTIONS,
                UserListingsScreen::new
        );

        // Auction item tooltips
        AuctionTooltips.init();
    }
}