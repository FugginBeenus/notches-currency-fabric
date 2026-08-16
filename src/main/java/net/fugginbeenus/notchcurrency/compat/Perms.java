package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.world.entity.player.Player;

public final class Perms {

    private Perms() {}

    public static boolean isOperator(Player player) {
        //? if >=1.21.11 {
        /*return player instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return player.hasPermissions(2);
        //?}
    }

    public static boolean isOperator(net.minecraft.commands.CommandSourceStack source) {
        //? if >=1.21.11 {
        /*return source.permissions()
                .hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return source.hasPermission(2);
        //?}
    }
}
