package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Sending a player a line of text.
 *
 * <p>Vanilla had one method with a boolean deciding chat or action bar, and 26.1 split it into two.
 * Naming the destination rather than passing a flag is clearer at the call site anyway, so the mod
 * says which one it means and this picks the method that exists.
 */
public final class Msg {

    private Msg() {}

    /** A line in the chat log. */
    public static void chat(Player player, Component text) {
        //? if >=26.1 {
        /*player.sendSystemMessage(text);
        *///?} else {
        player.displayClientMessage(text, false);
        //?}
    }

    /** A line above the hotbar, which replaces itself and fades. */
    public static void actionBar(Player player, Component text) {
        //? if >=26.1 {
        /*player.sendOverlayMessage(text);
        *///?} else {
        player.displayClientMessage(text, true);
        //?}
    }
}
