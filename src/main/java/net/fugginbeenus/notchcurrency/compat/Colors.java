package net.fugginbeenus.notchcurrency.compat;

import java.util.Locale;
import net.minecraft.ChatFormatting;

public final class Colors {

    private Colors() {}

    private static final String COLOR_CODES = "0123456789abcdef";

    public static String name(ChatFormatting formatting) {
        //? if >=26.2 {
        /*return formatting.name().toLowerCase(Locale.ROOT);
        *///?} else {
        return formatting.getName();
        //?}
    }

    public static ChatFormatting byName(String name) {
        //? if >=26.2 {
        /*if (name == null) return null;
        String wanted = name.toLowerCase(Locale.ROOT);
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.name().toLowerCase(Locale.ROOT).equals(wanted)) return formatting;
        }
        return null;
        *///?} else {
        return ChatFormatting.getByName(name);
        //?}
    }

    public static Integer rgb(ChatFormatting formatting) {
        //? if >=26.2 {
        /*net.minecraft.network.chat.TextColor color =
                net.minecraft.network.chat.TextColor.fromLegacyFormat(formatting);
        return color == null ? null : color.getValue();
        *///?} else {
        return formatting.getColor();
        //?}
    }

    public static boolean isColor(ChatFormatting formatting) {
        //? if >=26.2 {
        /*// toString is the section sign followed by the formatting code.
        String code = formatting.toString();
        return code.length() == 2 && COLOR_CODES.indexOf(code.charAt(1)) >= 0;
        *///?} else {
        return formatting.isColor();
        //?}
    }
}
