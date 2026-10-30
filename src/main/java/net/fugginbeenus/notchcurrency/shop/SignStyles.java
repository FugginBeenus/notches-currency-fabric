package net.fugginbeenus.notchcurrency.shop;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.IntUnaryOperator;

public final class SignStyles {

    public static final String SHOP_DEFAULT = "wood", NPC_DEFAULT = "plain";
    public static final int SLOTS = 8;
    private static final int MAX_LENGTH = SLOTS * 3, BASE = 38;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789_";

    private SignStyles() {}

    public static boolean valid(String id) {
        if (id == null || id.isEmpty() || id.length() > MAX_LENGTH) return false;
        for (int i = 0; i < id.length(); i++) {
            if (ALPHABET.indexOf(id.charAt(i)) < 0) return false;
        }
        return true;
    }

    public static String clean(String id, String fallback) {
        return valid(id) ? id : fallback;
    }

    public static void write(ContainerData data, int start, String id) {
        for (int s = 0; s < SLOTS; s++) {
            int v = 0;
            for (int k = 0; k < 3; k++) {
                int i = s * 3 + k;
                v = v * BASE + (i < id.length() ? ALPHABET.indexOf(id.charAt(i)) + 1 : 0);
            }
            data.set(start + s, v);
        }
    }

    public static String read(IntUnaryOperator prop, int start, String fallback) {
        StringBuilder out = new StringBuilder();
        for (int s = 0; s < SLOTS; s++) {
            int v = prop.applyAsInt(start + s) & 0xFFFF;
            int[] chars = {v / (BASE * BASE), v / BASE % BASE, v % BASE};
            for (int c : chars) {
                if (c == 0 || c > ALPHABET.length()) return clean(out.toString(), fallback);
                out.append(ALPHABET.charAt(c - 1));
            }
        }
        return clean(out.toString(), fallback);
    }
}
