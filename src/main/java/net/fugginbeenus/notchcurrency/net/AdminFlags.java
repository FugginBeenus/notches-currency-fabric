package net.fugginbeenus.notchcurrency.net;

import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.compat.Perms;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class AdminFlags {

    public static final int NPC = 1, NPC_COMMANDS = 2, QUEST = 4;

    private AdminFlags() {}

    public static void send(ServerPlayer sp) {
        int bits = (Perms.isAdmin(sp, Perms.NPC) ? NPC : 0)
                | (Perms.isAdmin(sp, Perms.NPC_COMMANDS) ? NPC_COMMANDS : 0)
                | (Perms.isAdmin(sp, Perms.QUEST) ? QUEST : 0);
        FriendlyByteBuf buf = Net.buf();
        buf.writeVarInt(bits);
        Net.sendToClient(sp, NotchPackets.ADMIN_FLAGS, buf);
    }
}
