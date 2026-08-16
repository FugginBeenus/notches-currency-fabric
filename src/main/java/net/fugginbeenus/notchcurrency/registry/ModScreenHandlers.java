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
    public static MenuType<AuctionHouseScreenHandler> AUCTION_HOUSE;
    public static MenuType<UserListingsScreenHandler> USER_AUCTIONS;
    public static MenuType<RaffleScreenHandler> RAFFLE;
    public static MenuType<RaffleAdminScreenHandler> RAFFLE_ADMIN;
    public static MenuType<AuctionListingScreenHandler> AUCTION_LISTING;
    public static MenuType<BountyBoardScreenHandler> BOUNTY_BOARD;
    public static MenuType<BountyAdminScreenHandler> BOUNTY_ADMIN;
    public static MenuType<net.fugginbeenus.notchcurrency.economy.loan.LoanScreenHandler> LOAN;
    public static MenuType<net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler> SLOT_MACHINE;
    public static MenuType<net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipScreenHandler> COIN_FLIP;
    public static MenuType<net.fugginbeenus.notchcurrency.npc.NpcEquipScreenHandler> NPC_EQUIP;
    public static MenuType<net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler> ENCHANTER;
    public static MenuType<net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler> COSMETIC_SHOP;
    public static MenuType<net.fugginbeenus.notchcurrency.mail.MailInboxMenu> MAIL_INBOX;
    public static MenuType<net.fugginbeenus.notchcurrency.mail.MailPostScreenHandler> MAIL_POST;
    public static MenuType<net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler> TRADE_OFFER_CREATE;
    public static MenuType<net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler> TRADE_OFFERS;
    public static MenuType<net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler> RECEIPTS;
    public static MenuType<net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler> SHOP_BROWSE;
    public static MenuType<net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler> SHOP_MANAGE;
    public static MenuType<net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler> SHOP_LISTING_EDIT;

    private ModScreenHandlers() {}

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> simple(
            net.minecraft.resources.ResourceLocation id, MenuType.MenuSupplier<T> factory) {
        return Registry.register(BuiltInRegistries.MENU, id,
                new MenuType<>(factory,
                        net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    }

    public static void register() {
        ATM = simple(
                NotchCurrency.id("atm"),
                ATMTestScreenHandler::new
        );

        TRADE = simple(
                NotchCurrency.id("trade"),
                TradeScreenHandler::new
        );

        AUCTION_HOUSE = simple(
                NotchCurrency.id("auction_house"),
                AuctionHouseScreenHandler::new
        );

        USER_AUCTIONS = simple(
                NotchCurrency.id("user_auctions"),
                UserListingsScreenHandler::new
        );

        RAFFLE = simple(
                NotchCurrency.id("raffle"),
                RaffleScreenHandler::new
        );

        RAFFLE_ADMIN = simple(
                NotchCurrency.id("raffle_admin"),
                RaffleAdminScreenHandler::new
        );

        AUCTION_LISTING = simple(
                NotchCurrency.id("auction_listing"),
                AuctionListingScreenHandler::new
        );

        BOUNTY_BOARD = simple(
                NotchCurrency.id("bounty_board"),
                BountyBoardScreenHandler::new
        );

        BOUNTY_ADMIN = simple(
                NotchCurrency.id("bounty_admin"),
                BountyAdminScreenHandler::new
        );

        LOAN = simple(
                NotchCurrency.id("loan"),
                net.fugginbeenus.notchcurrency.economy.loan.LoanScreenHandler::new
        );

        SLOT_MACHINE = simple(
                NotchCurrency.id("slot_machine"),
                net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler::new
        );

        COIN_FLIP = simple(
                NotchCurrency.id("coin_flip"),
                net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipScreenHandler::new
        );

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

        ENCHANTER = simple(
                NotchCurrency.id("enchanter"),
                net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler::new
        );

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

        MAIL_INBOX = simple(
                NotchCurrency.id("mail_inbox"),
                net.fugginbeenus.notchcurrency.mail.MailInboxMenu::new
        );
        MAIL_POST = simple(
                NotchCurrency.id("mail_post"),
                net.fugginbeenus.notchcurrency.mail.MailPostScreenHandler::new
        );

        TRADE_OFFER_CREATE = simple(
                NotchCurrency.id("trade_offer_create"),
                net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler::new
        );
        TRADE_OFFERS = simple(
                NotchCurrency.id("trade_offers"),
                net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler::new
        );

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