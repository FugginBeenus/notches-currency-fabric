package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.trade.TradeScreenHandler;
import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler;
import net.fugginbeenus.notchcurrency.auction.UserListingsScreenHandler;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyAdminScreenHandler;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyBoardScreenHandler;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleAdminScreenHandler;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleScreenHandler;
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

    // Raffle info/action panel
    public static ScreenHandlerType<RaffleScreenHandler> RAFFLE;

    // Raffle admin setup panel
    public static ScreenHandlerType<RaffleAdminScreenHandler> RAFFLE_ADMIN;

    // "List an item" creation screen
    public static ScreenHandlerType<AuctionListingScreenHandler> AUCTION_LISTING;

    // Bounty board panel
    public static ScreenHandlerType<BountyBoardScreenHandler> BOUNTY_BOARD;

    // Bounty admin setup panel
    public static ScreenHandlerType<BountyAdminScreenHandler> BOUNTY_ADMIN;

    // Loan application panel
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.economy.loan.LoanScreenHandler> LOAN;

    // Slot machine panel
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler> SLOT_MACHINE;

    // Coin flip betting panel
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipScreenHandler> COIN_FLIP;

    // NPC equipment panel
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.npc.NpcEquipScreenHandler> NPC_EQUIP;

    // Enchanter service panel
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler> ENCHANTER;

    // Cosmetics shop panel
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler> COSMETIC_SHOP;

    // Offline trade offers: create + board
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler> TRADE_OFFER_CREATE;
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler> TRADE_OFFERS;

    // Receipts (transaction history) panel
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler> RECEIPTS;

    // Shop browser (buyer side, extended: shop identity in the opening buf)
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler> SHOP_BROWSE;

    // Shop manage hub (owner side)
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler> SHOP_MANAGE;

    // Shop listing editor (owner side)
    public static ScreenHandlerType<net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler> SHOP_LISTING_EDIT;

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

        // Raffle panel
        RAFFLE = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("raffle"),
                RaffleScreenHandler::new
        );

        // Raffle admin setup panel
        RAFFLE_ADMIN = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("raffle_admin"),
                RaffleAdminScreenHandler::new
        );

        // List-an-item screen
        AUCTION_LISTING = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("auction_listing"),
                AuctionListingScreenHandler::new
        );

        // Bounty board
        BOUNTY_BOARD = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("bounty_board"),
                BountyBoardScreenHandler::new
        );

        // Bounty admin setup
        BOUNTY_ADMIN = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("bounty_admin"),
                BountyAdminScreenHandler::new
        );

        // Loan application
        LOAN = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("loan"),
                net.fugginbeenus.notchcurrency.economy.loan.LoanScreenHandler::new
        );

        // Slot machine
        SLOT_MACHINE = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("slot_machine"),
                net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler::new
        );

        // Coin flip
        COIN_FLIP = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("coin_flip"),
                net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipScreenHandler::new
        );

        // NPC equipment
        NPC_EQUIP = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("npc_equip"),
                net.fugginbeenus.notchcurrency.npc.NpcEquipScreenHandler::new
        );

        // Enchanter service
        ENCHANTER = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("enchanter"),
                net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler::new
        );

        // Cosmetics shop (extended: linked NPC uuid in the opening buf for the preview)
        COSMETIC_SHOP = Registry.register(
                Registries.SCREEN_HANDLER,
                NotchCurrency.id("cosmetic_shop"),
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler::new)
        );

        // Offline trade offers
        TRADE_OFFER_CREATE = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("trade_offer_create"),
                net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler::new
        );
        TRADE_OFFERS = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("trade_offers"),
                net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler::new
        );

        // Receipts (extended: history snapshot in the opening buf)
        RECEIPTS = Registry.register(
                Registries.SCREEN_HANDLER,
                NotchCurrency.id("receipts"),
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler::new)
        );

        // Shop browser (buyer side)
        SHOP_BROWSE = Registry.register(
                Registries.SCREEN_HANDLER,
                NotchCurrency.id("shop_browse"),
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler::new)
        );

        // Shop manage hub (owner side)
        SHOP_MANAGE = Registry.register(
                Registries.SCREEN_HANDLER,
                NotchCurrency.id("shop_manage"),
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler::new)
        );

        // Shop listing editor (owner side)
        SHOP_LISTING_EDIT = Registry.register(
                Registries.SCREEN_HANDLER,
                NotchCurrency.id("shop_listing_edit"),
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler::new)
        );
    }
}