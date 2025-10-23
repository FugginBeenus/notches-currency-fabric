package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    public static ScreenHandlerType<ATMTestScreenHandler> ATM;

    public static void register() {
        ATM = ScreenHandlerRegistry.registerSimple(
                new Identifier(NotchCurrency.MOD_ID, "atm"),
                ATMTestScreenHandler::new
        );
    }
}
