package net.fugginbeenus.notchcurrency.core;

import net.fabricmc.api.ModInitializer;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.util.Identifier;

public class NotchCurrency implements ModInitializer {
    public static final String MOD_ID = "notchcurrency";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static final Identifier DEPOSIT_PACKET_ID = new Identifier(MOD_ID, "deposit");

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModScreenHandlers.register();

        // Server receiver for deposit
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                DEPOSIT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
                    server.execute(() -> {
                        if (player.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler atm) {
                            atm.depositAll(player);
                        }
                    });
                });
    }
}


