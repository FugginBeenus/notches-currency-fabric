package net.fugginbeenus.notchcurrency.compat;

import java.util.Locale;
import net.minecraft.ChatFormatting;

/**
 * Naming a chat colour, and telling a colour apart from a style.
 *
 * <p>26.2 stripped ChatFormatting back to a bare enum: no {@code getName}, no {@code getByName}, no
 * {@code isColor}. Factions store their colour by name in NBT and in share codes, so those strings
 * have to keep reading the same on every version, not merely compile.
 *
 * <p>The names below are what vanilla always produced, since its own were the lowercased enum
 * constants. Colour is decided from the formatting code rather than the enum order, because the codes
 * are part of the chat protocol and will not quietly renumber.
 */
public final class Colors {

    private Colors() {}

    private static final String COLOR_CODES = "0123456789abcdef";

    /** The stored name of a formatting, for example {@code dark_red}. */
    public static String name(ChatFormatting formatting) {
        //? if >=26.2 {
        /*return formatting.name().toLowerCase(Locale.ROOT);
        *///?} else {
        return formatting.getName();
        //?}
    }

    /** The formatting with this stored name, or null when nothing matches. */
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

    /** The RGB behind a colour formatting, or null for a style that has none. */
    public static Integer rgb(ChatFormatting formatting) {
        //? if >=26.2 {
        /*net.minecraft.network.chat.TextColor color =
                net.minecraft.network.chat.TextColor.fromLegacyFormat(formatting);
        return color == null ? null : color.getValue();
        *///?} else {
        return formatting.getColor();
        //?}
    }

    /** True for the sixteen colours, false for bold, italic and the rest. */
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
