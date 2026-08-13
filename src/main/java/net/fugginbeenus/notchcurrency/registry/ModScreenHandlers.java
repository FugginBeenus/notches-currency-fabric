package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.trade.TradeScreenHandler;
import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler;
import net.fugginbeenus.notchcurrency.auction.UserListingsScreenHandler;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyAdminScreenHandler;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyBoardScreenHandler;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleAdminScreenHandler;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleScreenHandler;

public final class ModScreenHandlers {

    public static MenuType<ATMTestScreenHandler> ATM;
    public static MenuType<TradeScreenHandler> TRADE;

    // main auction browser (the big "MY LISTINGS" screen)
    public static MenuType<AuctionHouseScreenHandler> AUCTION_HOUSE;

    // popup "user auctions / my listings" window
    public static MenuType<UserListingsScreenHandler> USER_AUCTIONS;

    // Raffle info/action panel
    public static MenuType<RaffleScreenHandler> RAFFLE;

    // Raffle admin setup panel
    public static MenuType<RaffleAdminScreenHandler> RAFFLE_ADMIN;

    // "List an item" creation screen
    public static MenuType<AuctionListingScreenHandler> AUCTION_LISTING;

    // Bounty board panel
    public static MenuType<BountyBoardScreenHandler> BOUNTY_BOARD;

    // Bounty admin setup panel
    public static MenuType<BountyAdminScreenHandler> BOUNTY_ADMIN;

    // Loan application panel
    public static MenuType<net.fugginbeenus.notchcurrency.economy.loan.LoanScreenHandler> LOAN;

    // Slot machine panel
    public static MenuType<net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler> SLOT_MACHINE;

    // Coin flip betting panel
    public static MenuType<net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipScreenHandler> COIN_FLIP;

    // NPC equipment panel
    public static MenuType<net.fugginbeenus.notchcurrency.npc.NpcEquipScreenHandler> NPC_EQUIP;

    // Enchanter service panel
    public static MenuType<net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler> ENCHANTER;

    // Cosmetics shop panel
    public static MenuType<net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler> COSMETIC_SHOP;

    // Offline trade offers: create + board
    public static MenuType<net.fugginbeenus.notchcurrency.mail.MailPostScreenHandler> MAIL_POST;
    public static MenuType<net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler> TRADE_OFFER_CREATE;
    public static MenuType<net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler> TRADE_OFFERS;

    // Receipts (transaction history) panel
    public static MenuType<net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler> RECEIPTS;

    // Shop browser (buyer side, extended: shop identity in the opening buf)
    public static MenuType<net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler> SHOP_BROWSE;

    // Shop manage hub (owner side)
    public static MenuType<net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler> SHOP_MANAGE;

    // Shop listing editor (owner side)
    public static MenuType<net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler> SHOP_LISTING_EDIT;

    private ModScreenHandlers() {}

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> simple(
            net.minecraft.resources.ResourceLocation id, MenuType.MenuSupplier<T> factory) {
        return Registry.register(BuiltInRegistries.MENU, id,
                new MenuType<>(factory,
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    }

    public static void register() {
        // ATM
        ATM = simple(
                NotchCurrency.id("atm"),
                ATMTestScreenHandler::new
        );

        // Player-to-player trade
        TRADE = simple(
                NotchCurrency.id("trade"),
                TradeScreenHandler::new
        );

        // Main auction house browser
        AUCTION_HOUSE = simple(
                NotchCurrency.id("auction_house"),
                AuctionHouseScreenHandler::new
        );

        // Popup "My Listings" / user auctions window
        USER_AUCTIONS = simple(
                NotchCurrency.id("user_auctions"),
                UserListingsScreenHandler::new
        );

        // Raffle panel
        RAFFLE = simple(
                NotchCurrency.id("raffle"),
                RaffleScreenHandler::new
        );

        // Raffle admin setup panel
        RAFFLE_ADMIN = simple(
                NotchCurrency.id("raffle_admin"),
                RaffleAdminScreenHandler::new
        );

        // List-an-item screen
        AUCTION_LISTING = simple(
                NotchCurrency.id("auction_listing"),
                AuctionListingScreenHandler::new
        );

        // Bounty board
        BOUNTY_BOARD = simple(
                NotchCurrency.id("bounty_board"),
                BountyBoardScreenHandler::new
        );

        // Bounty admin setup
        BOUNTY_ADMIN = simple(
                NotchCurrency.id("bounty_admin"),
                BountyAdminScreenHandler::new
        );

        // Loan application
        LOAN = simple(
                NotchCurrency.id("loan"),
                net.fugginbeenus.notchcurrency.economy.loan.LoanScreenHandler::new
        );

        // Slot machine
        SLOT_MACHINE = simple(
                NotchCurrency.id("slot_machine"),
                net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler::new
        );

        // Coin flip
        COIN_FLIP = simple(
                NotchCurrency.id("coin_flip"),
                net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipScreenHandler::new
        );

        // NPC equipment
        // NPC equipment (extended: the NPC uuid rides the opening buf for the live preview)
        NPC_EQUIP = Registry.register(
                BuiltInRegistries.MENU,
                NotchCurrency.id("npc_equip"),
                //? if >=1.21 {
                /*new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.npc.NpcEquipScreenHandler::new,
                        net.fugginbeenus.notchcurrency.compat.Net.RAW_BUF_CODEC)
                *///?} else {
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.npc.NpcEquipScreenHandler::new)
                //?}
        );

        // Enchanter service
        ENCHANTER = simple(
                NotchCurrency.id("enchanter"),
                net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler::new
        );

        // Cosmetics shop (extended: linked NPC uuid in the opening buf for the preview)
        COSMETIC_SHOP = Registry.register(
                BuiltInRegistries.MENU,
                NotchCurrency.id("cosmetic_shop"),
                //? if >=1.21 {
                /*new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler::new,
                        net.fugginbeenus.notchcurrency.compat.Net.RAW_BUF_CODEC)
                *///?} else {
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler::new)
                //?}
        );

        MAIL_POST = simple(
                NotchCurrency.id("mail_post"),
                net.fugginbeenus.notchcurrency.mail.MailPostScreenHandler::new
        );

        // Offline trade offers
        TRADE_OFFER_CREATE = simple(
                NotchCurrency.id("trade_offer_create"),
                net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler::new
        );
        TRADE_OFFERS = simple(
                NotchCurrency.id("trade_offers"),
                net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler::new
        );

        // Receipts (extended: history snapshot in the opening buf)
        RECEIPTS = Registry.register(
                BuiltInRegistries.MENU,
                NotchCurrency.id("receipts"),
                //? if >=1.21 {
                /*new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler::new,
                        net.fugginbeenus.notchcurrency.compat.Net.RAW_BUF_CODEC)
                *///?} else {
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler::new)
                //?}
        );

        // Shop browser (buyer side)
        SHOP_BROWSE = Registry.register(
                BuiltInRegistries.MENU,
                NotchCurrency.id("shop_browse"),
                //? if >=1.21 {
                /*new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler::new,
                        net.fugginbeenus.notchcurrency.compat.Net.RAW_BUF_CODEC)
                *///?} else {
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler::new)
                //?}
        );

        // Shop manage hub (owner side)
        SHOP_MANAGE = Registry.register(
                BuiltInRegistries.MENU,
                NotchCurrency.id("shop_manage"),
                //? if >=1.21 {
                /*new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler::new,
                        net.fugginbeenus.notchcurrency.compat.Net.RAW_BUF_CODEC)
                *///?} else {
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler::new)
                //?}
        );

        // Shop listing editor (owner side)
        SHOP_LISTING_EDIT = Registry.register(
                BuiltInRegistries.MENU,
                NotchCurrency.id("shop_listing_edit"),
                //? if >=1.21 {
                /*new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler::new,
                        net.fugginbeenus.notchcurrency.compat.Net.RAW_BUF_CODEC)
                *///?} else {
                new ExtendedScreenHandlerType<>(net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler::new)
                //?}
        );
    }
}