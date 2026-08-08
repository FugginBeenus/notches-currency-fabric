package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.world.entity.player.Player;

/**
 * One answer to "is this player an operator", on every version.
 *
 * <p>1.21.11 retired numeric permission levels in favour of named permissions, so the old
 * {@code hasPermissions(2)} has no direct equivalent. Level 2 is the gamemaster tier, which is what
 * {@code Permissions.COMMANDS_GAMEMASTER} means, and that is the mapping used below.
 *
 * <p>This lives in one place on purpose. The same check gates every action that can mint coins or run
 * a command, so it is a security boundary rather than a convenience. Spread across a dozen call sites
 * it would eventually be written two different ways on two different versions, and the version that
 * got it wrong would be the one nobody was testing.
 */
public final class Perms {

    private Perms() {}

    /** Operator, in the sense the rest of this mod means it: allowed to run commands and mint value. */
    public static boolean isOperator(Player player) {
        //? if >=1.21.11 {
        /*return player instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return player.hasPermissions(2);
        //?}
    }

    /** The same tier, asked of a command source. This is what gates every admin subcommand. */
    public static boolean isOperator(net.minecraft.commands.CommandSourceStack source) {
        //? if >=1.21.11 {
        /*return source.permissions()
                .hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return source.hasPermission(2);
        //?}
    }
}
