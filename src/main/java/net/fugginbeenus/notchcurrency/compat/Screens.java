package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public final class Screens {

    private Screens() {}

    @FunctionalInterface
    public interface MenuFactory {
        ScreenHandler create(int syncId, PlayerInventory inv, PlayerEntity player);
    }

    public static void openExtended(ServerPlayerEntity sp, Text title, MenuFactory menu, Consumer<PacketByteBuf> data) {
        //? if >=1.21 {
        /*sp.openHandledScreen(new ExtendedScreenHandlerFactory<PacketByteBuf>() {
            @Override
            public Text getDisplayName() {
                return title;
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                return menu.create(syncId, inv, player);
            }

            @Override
            public PacketByteBuf getScreenOpeningData(ServerPlayerEntity player) {
                PacketByteBuf buf = PacketByteBufs.create();
                data.accept(buf);
                return buf;
            }
        });
        *///?} else {
        sp.openHandledScreen(new ExtendedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return title;
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                return menu.create(syncId, inv, player);
            }

            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                data.accept(buf);
            }
        });
        //?}
    }
}
