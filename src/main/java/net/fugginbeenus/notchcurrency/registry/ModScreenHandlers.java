package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.trade.TradeScreenHandler;
import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {

    public static ScreenHandlerType<ATMTestScreenHandler> ATM;
    public static ScreenHandlerType<TradeScreenHandler> TRADE;

    private ModScreenHandlers() {}

    /** Call once from NotchCurrency.onInitialize() */
    public static void register() {
        // ATM
        ATM = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("atm"),
                ATMTestScreenHandler::new // (syncId, PlayerInventory)
        );

        // Player-to-player trade
        TRADE = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("trade"),
                TradeScreenHandler::new // client-side ctor (syncId, PlayerInventory)
        );
    }
}
