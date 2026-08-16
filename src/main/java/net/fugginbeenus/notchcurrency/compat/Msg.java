package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class Msg {

    private Msg() {}

    public static void chat(Player player, Component text) {
        //? if >=26.1 {
        /*player.sendSystemMessage(text);
        *///?} else {
        player.displayClientMessage(text, false);
        //?}
    }

    public static void actionBar(Player player, Component text) {
        //? if >=26.1 {
        /*player.sendOverlayMessage(text);
        *///?} else {
        player.displayClientMessage(text, true);
        //?}
    }
}
