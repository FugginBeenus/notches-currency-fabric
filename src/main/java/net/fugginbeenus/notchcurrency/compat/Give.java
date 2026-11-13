package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Give {

    private Give() {}

    public static void drop(Player player, ItemStack stack) {
        //? if >=26.3 {
        /*player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        *///?} else {
        player.drop(stack, false);
        //?}
    }

    public static void placeBack(Player player, ItemStack stack) {
        //? if >=26.3 {
        /*player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
        *///?} else {
        player.getInventory().placeItemBackInInventory(stack);
        //?}
    }
}
