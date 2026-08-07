package net.fugginbeenus.notchcurrency.economy;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ReceiptsScreenHandler extends AbstractContainerMenu {

    public final List<ReceiptState.Receipt> rows = new ArrayList<>();

    public ReceiptsScreenHandler(int containerId, Inventory inv, FriendlyByteBuf buf) {
        super(ModScreenHandlers.RECEIPTS, containerId);
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            long time = buf.readLong();
            long delta = buf.readLong();
            long balance = buf.readLong();
            String reason = buf.readUtf(32);
            String detail = buf.readUtf(96);
            rows.add(new ReceiptState.Receipt(time, delta, balance, reason, detail));
        }
    }

    public ReceiptsScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.RECEIPTS, containerId);
    }

    public static void open(ServerPlayer sp) {
        List<ReceiptState.Receipt> recent = ReceiptState.get(sp.getServer()).recent(sp.getUUID());
        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(sp, Component.literal("Receipts"),
                (containerId, inv, p) -> new ReceiptsScreenHandler(containerId, inv),
                buf -> {
                    buf.writeVarInt(recent.size());
                    for (ReceiptState.Receipt r : recent) {
                        buf.writeLong(r.time());
                        buf.writeLong(r.delta());
                        buf.writeLong(r.balanceAfter());
                        buf.writeUtf(r.reason());
                        buf.writeUtf(r.detail().length() > 96 ? r.detail().substring(0, 96) : r.detail());
                    }
                });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
