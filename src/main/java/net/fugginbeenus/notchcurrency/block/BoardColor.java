package net.fugginbeenus.notchcurrency.block;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum BoardColor implements StringRepresentable {
    NONE(0xE2DED2), BLACK(0x24242A), DARK_BLUE(0x353CA0), DARK_GREEN(0x54701A), DARK_AQUA(0x168C91),
    DARK_RED(0x8C2020), DARK_PURPLE(0x7A2CA8), GOLD(0xE69628), GRAY(0x9D9D97), DARK_GRAY(0x4A5054),
    BLUE(0x486CDE), GREEN(0x6EB61E), AQUA(0x3AB3DA), RED(0xB02E26), LIGHT_PURPLE(0xC74EBD),
    YELLOW(0xECC434), WHITE(0xE9ECEC);

    private final int rgb;

    BoardColor(int rgb) {
        this.rgb = rgb;
    }

    public int argb() {
        return 0xFF000000 | rgb;
    }

    public static BoardColor of(@Nullable ChatFormatting color) {
        if (color == null) return NONE;
        for (BoardColor c : values()) {
            if (c != NONE && c.name().equals(color.name())) return c;
        }
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
