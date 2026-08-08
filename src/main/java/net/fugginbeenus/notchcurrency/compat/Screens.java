package net.fugginbeenus.notchcurrency.compat;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import java.util.function.Consumer;

public final class Screens {

    private Screens() {}

    @FunctionalInterface
    public interface MenuFactory {
        AbstractContainerMenu create(int containerId, Inventory inv, Player player);
    }

    public static void openExtended(ServerPlayer sp, Component title, MenuFactory menu, Consumer<FriendlyByteBuf> data) {
        //? if >=1.21 {
        /*sp.openMenu(new ExtendedScreenHandlerFactory<FriendlyByteBuf>() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
                return menu.create(containerId, inv, player);
            }

            @Override
            public FriendlyByteBuf getScreenOpeningData(ServerPlayer player) {
                FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
                data.accept(buf);
                return buf;
            }
        });
        *///?} else {
        sp.openMenu(new ExtendedScreenHandlerFactory() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
                return menu.create(containerId, inv, player);
            }

            @Override
            public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                data.accept(buf);
            }
        });
        //?}
    }
}
