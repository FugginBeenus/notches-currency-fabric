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

/**
 * Version-compat facade for opening a screen that carries data to the client (the shop, cosmetics
 * and receipts GUIs stuff a small buffer into the open packet).
 *
 * <p>1.21 made Fabric's {@code ExtendedScreenHandlerFactory} generic and flipped the write hook from
 * "fill this buffer" to "return the data object". Call sites hand this facade the title, the menu
 * constructor, and a buffer-writer; the per-version factory shape lives here once instead of at
 * every opening site. The data type stays {@code PacketByteBuf} on both versions, so the screen
 * handlers' buffer-reading constructors are untouched.
 */
public final class Screens {

    private Screens() {}

    @FunctionalInterface
    public interface MenuFactory {
        ScreenHandler create(int syncId, PlayerInventory inv, PlayerEntity player);
    }

    /** Open a data-carrying screen for the player; {@code data} fills the buffer the client reads. */
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
