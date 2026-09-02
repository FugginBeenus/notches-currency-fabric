package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.client.Minecraft;

public final class PermsClient {

    private PermsClient() {}

    public static boolean isOperator() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        net.minecraft.client.multiplayer.ClientPacketListener conn = mc.getConnection();
        if (conn != null) {
            //? if >=1.21.11 {
            /*if (conn.getSuggestionsProvider().permissions().hasPermission(
                    net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
                return true;
            }
            *///?} else {
            if (conn.getSuggestionsProvider().hasPermission(2)) return true;
            //?}
        }
        return Perms.isOperator(mc.player);
    }
}
