package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.shop.PlayerShopScreenHandler;
import net.fugginbeenus.notchcurrency.trade.TradeScreenHandler;
import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.auction.UserListingsScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {

    public static ScreenHandlerType<ATMTestScreenHandler> ATM;
    public static ScreenHandlerType<TradeScreenHandler> TRADE;

    // main auction browser (the big "MY LISTINGS" screen)
    public static ScreenHandlerType<AuctionHouseScreenHandler> AUCTION_HOUSE;

    // popup "user auctions / my listings" window
    public static ScreenHandlerType<UserListingsScreenHandler> USER_AUCTIONS;

    // Player shop (browse/manage)
    public static ScreenHandlerType<PlayerShopScreenHandler> PLAYER_SHOP;

    private ModScreenHandlers() {}

    /** Call once from NotchCurrency.onInitialize() */
    public static void register() {
        // ATM
        ATM = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("atm"),
                ATMTestScreenHandler::new
        );

        // Player-to-player trade
        TRADE = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("trade"),
                TradeScreenHandler::new
        );

        // Main auction house browser
        AUCTION_HOUSE = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("auction_house"),
                AuctionHouseScreenHandler::new
        );

        // Popup "My Listings" / user auctions window
        USER_AUCTIONS = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("user_auctions"),
                UserListingsScreenHandler::new
        );

        // Player shop (extended handler with extra data)
        PLAYER_SHOP = Registry.register(
                Registries.SCREEN_HANDLER,
                NotchCurrency.id("player_shop"),
                new ExtendedScreenHandlerType<>(PlayerShopScreenHandler::new)
        );
    }
}