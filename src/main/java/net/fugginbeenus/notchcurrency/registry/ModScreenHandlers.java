package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {
    public static ScreenHandlerType<ATMTestScreenHandler> ATM;

    public static void register() {
        // Simple 2-arg factory (syncId, PlayerInventory)
        ATM = ScreenHandlerRegistry.registerSimple(
                NotchCurrency.id("atm"),
                ATMTestScreenHandler::new
        );
    }

    private ModScreenHandlers() {}
}
