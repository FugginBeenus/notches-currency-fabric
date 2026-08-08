package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;

/**
 * Building the click and hover events that go on chat messages.
 *
 * <p>Both were plain two-argument constructors taking an action and a payload until 1.21.11, which
 * turned them into sealed interfaces with a record per action. The payload types moved with them: a
 * link now wants a real {@link java.net.URI} rather than a string, and an item hover takes the stack
 * directly instead of wrapping it. {@code Style.withClickEvent} still accepts the same interface
 * either way, so naming the action once here is enough to keep the call sites identical.
 */
public final class Chat {

    private Chat() {}

    public static ClickEvent runCommand(String command) {
        //? if >=1.21.11 {
        /*return new ClickEvent.RunCommand(command);
        *///?} else {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        //?}
    }

    public static ClickEvent copyToClipboard(String text) {
        //? if >=1.21.11 {
        /*return new ClickEvent.CopyToClipboard(text);
        *///?} else {
        return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
        //?}
    }

    public static ClickEvent openUrl(String url) {
        //? if >=1.21.11 {
        /*return new ClickEvent.OpenUrl(java.net.URI.create(url));
        *///?} else {
        return new ClickEvent(ClickEvent.Action.OPEN_URL, url);
        //?}
    }

    public static HoverEvent showText(Component text) {
        //? if >=1.21.11 {
        /*return new HoverEvent.ShowText(text);
        *///?} else {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
        //?}
    }

    public static HoverEvent showItem(ItemStack stack) {
        //? if >=26.1 {
        /*return new HoverEvent.ShowItem(new net.minecraft.world.item.ItemStackTemplate(
                stack.getItem(), stack.getCount()));
        *///?} elif >=1.21.11 {
        /*return new HoverEvent.ShowItem(stack);
        *///?} else {
        return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack));
        //?}
    }
}
