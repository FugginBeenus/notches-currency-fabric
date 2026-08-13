package net.fugginbeenus.notchcurrency.crate;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.minecraft.network.FriendlyByteBuf;

/**
 * The balloon settings, on the wire.
 *
 * <p>Written once and read once, in the same order, so the two ends cannot drift the way two
 * hand-written copies of thirteen fields would.
 */
public final class BalloonConfigWire {

    private BalloonConfigWire() {}

    public static void write(FriendlyByteBuf buf, NotchConfig cfg) {
        var b = cfg.balloon;
        buf.writeBoolean(b.enabled);
        buf.writeVarInt(b.centerX);
        buf.writeVarInt(b.centerY);
        buf.writeVarInt(b.centerZ);
        buf.writeVarInt(b.radius);
        buf.writeVarInt(b.minY);
        buf.writeVarInt(b.maxY);
        buf.writeVarInt(b.perDay);
        buf.writeBoolean(b.announce);
        buf.writeBoolean(b.perPlayer);
        buf.writeBoolean(b.playerInAreaOnly);
        buf.writeVarInt(b.playerHeight);
        buf.writeVarInt(b.playerSpread);
    }

    public static void read(FriendlyByteBuf buf, NotchConfig cfg) {
        var b = cfg.balloon;
        b.enabled = buf.readBoolean();
        b.centerX = buf.readVarInt();
        b.centerY = buf.readVarInt();
        b.centerZ = buf.readVarInt();
        b.radius = buf.readVarInt();
        b.minY = buf.readVarInt();
        b.maxY = buf.readVarInt();
        b.perDay = buf.readVarInt();
        b.announce = buf.readBoolean();
        b.perPlayer = buf.readBoolean();
        b.playerInAreaOnly = buf.readBoolean();
        b.playerHeight = buf.readVarInt();
        b.playerSpread = buf.readVarInt();
    }
}
