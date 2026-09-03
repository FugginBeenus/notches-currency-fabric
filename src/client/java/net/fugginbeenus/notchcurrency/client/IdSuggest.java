package net.fugginbeenus.notchcurrency.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class IdSuggest {

    private IdSuggest() {}

    public static final int KIND_ITEM = 0;
    public static final int KIND_MOB = 1;

    public static String best(String typed, int kind) {
        String want = typed == null ? "" : typed.trim().toLowerCase();
        if (want.isEmpty()) return "";
        String bare = want.startsWith("minecraft:") ? want.substring(10) : want;
        String hit = "";
        for (ResourceLocation id : (kind == KIND_MOB
                ? BuiltInRegistries.ENTITY_TYPE.keySet()
                : BuiltInRegistries.ITEM.keySet())) {
            String full = id.toString();
            boolean match = full.startsWith(want)
                    || ("minecraft".equals(id.getNamespace()) && id.getPath().startsWith(bare));
            if (!match) continue;
            if (hit.isEmpty() || full.length() < hit.length()) hit = full;
        }
        return hit;
    }

    public static boolean known(String typed, int kind) {
        ResourceLocation id = ResourceLocation.tryParse(fill(typed));
        if (id == null) return false;
        return kind == KIND_MOB
                ? BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                : BuiltInRegistries.ITEM.containsKey(id);
    }

    private static String fill(String typed) {
        String t = typed == null ? "" : typed.trim();
        return t.contains(":") ? t : "minecraft:" + t;
    }

    public static String friendly(String typed, int kind) {
        ResourceLocation id = ResourceLocation.tryParse(fill(typed));
        if (id == null) return "";
        if (kind == KIND_MOB) {
            var t = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            return t.isEmpty() ? "" : t.get().getDescription().getString();
        }
        var it = BuiltInRegistries.ITEM.getOptional(id);
        return it.isEmpty() ? "" : new net.minecraft.world.item.ItemStack(it.get())
                .getHoverName().getString();
    }

    public static void tip(GuiGraphics ctx, Font font, EditBox box, int kind, int mx, int my) {
        if (box == null || !box.isVisible()) return;
        if (mx < box.getX() - 2 || mx > box.getX() + box.getWidth() + 2) return;
        if (my < box.getY() - 4 || my > box.getY() + 14) return;
        String typed = box.getValue();
        if (typed.isBlank()) return;
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        if (known(typed, kind)) {
            lines.add(net.minecraft.network.chat.Component.literal(friendly(typed, kind))
                    .withStyle(net.minecraft.ChatFormatting.WHITE));
            lines.add(net.minecraft.network.chat.Component.literal(fill(typed))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            String hit = best(typed, kind);
            if (hit.isEmpty()) {
                lines.add(net.minecraft.network.chat.Component.literal("No match")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            } else {
                lines.add(net.minecraft.network.chat.Component.literal(friendly(hit, kind))
                        .withStyle(net.minecraft.ChatFormatting.WHITE));
                lines.add(net.minecraft.network.chat.Component.literal(hit)
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                lines.add(net.minecraft.network.chat.Component.literal("Tab or right arrow to fill")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        ctx.renderComponentTooltip(font, lines, mx, my);
    }

    public static void draw(GuiGraphics ctx, Font font, EditBox box, int kind) {
        if (box == null || !box.isVisible()) return;
        String typed = box.getValue();
        if (typed.isBlank()) return;
        String hit = best(typed, kind);
        int colour = known(typed, kind) ? 0xFF6FC274 : 0xFFD06B5A;
        if (!hit.isEmpty() && hit.length() > typed.trim().length()) {
            String tail = hit.substring(typed.trim().length());
            int at = box.getX() + font.width(typed);
            int room = box.getX() + box.getWidth() - at;
            if (room > 4) {
                if (font.width(tail) > room) tail = font.plainSubstrByWidth(tail, room);
                ctx.drawString(font, tail, at, box.getY(), 0xFF6B7062, false);
            }
        }
        ctx.fill(box.getX(), box.getY() + 10, box.getX() + box.getWidth(), box.getY() + 11, colour);
    }

    public static boolean accept(EditBox box, int kind) {
        if (box == null || !box.isVisible() || box.getValue().isBlank()) return false;
        if (box.getCursorPosition() < box.getValue().length()) return false;
        String hit = best(box.getValue(), kind);
        if (hit.isEmpty() || hit.equals(box.getValue().trim())) return false;
        box.setValue(hit);
        return true;
    }
}
