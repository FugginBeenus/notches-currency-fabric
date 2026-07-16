package net.fugginbeenus.notchcurrency.economy;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Backing handler for the receipts screen. The player's recent transaction history is a static
 * snapshot serialized into the opening buf (no live slots) — the client renders it as text rows.
 */
public class ReceiptsScreenHandler extends ScreenHandler {

    /** Client-side parsed rows (empty on the server). */
    public final List<ReceiptState.Receipt> rows = new ArrayList<>();

    /** Client constructor: read the receipt snapshot out of the opening buf. */
    public ReceiptsScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        super(ModScreenHandlers.RECEIPTS, syncId);
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            long time = buf.readLong();
            long delta = buf.readLong();
            long balance = buf.readLong();
            String reason = buf.readString(32);
            String detail = buf.readString(96);
            rows.add(new ReceiptState.Receipt(time, delta, balance, reason, detail));
        }
    }

    /** Server constructor (no rows needed server-side). */
    public ReceiptsScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.RECEIPTS, syncId);
    }

    /** Open the receipts screen for a player, sending their history in the opening buf. */
    public static void open(ServerPlayerEntity sp) {
        List<ReceiptState.Receipt> recent = ReceiptState.get(sp.getServer()).recent(sp.getUuid());
        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(sp, Text.literal("Receipts"),
                (syncId, inv, p) -> new ReceiptsScreenHandler(syncId, inv),
                buf -> {
                    buf.writeVarInt(recent.size());
                    for (ReceiptState.Receipt r : recent) {
                        buf.writeLong(r.time());
                        buf.writeLong(r.delta());
                        buf.writeLong(r.balanceAfter());
                        buf.writeString(r.reason());
                        buf.writeString(r.detail().length() > 96 ? r.detail().substring(0, 96) : r.detail());
                    }
                });
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
