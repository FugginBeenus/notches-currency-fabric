package net.fugginbeenus.notchcurrency.text;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fugginbeenus.notchcurrency.compat.Chat;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.compat.Perms;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.KeybindComponent;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.TextComponent;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.TranslatableComponent;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.TranslationArgument;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.event.ClickEvent;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.event.HoverEvent;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.format.TextDecoration;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.minimessage.MiniMessage;
import net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TextTags {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final MiniMessage CLICKS = MiniMessage.builder().tags(StandardTags.clickEvent()).build();
    private static final String[] COLORS = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
            "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"};
    private static final Map<String, Component> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
                    return size() > 1024;
                }
            });

    private static volatile Boolean synced;
    private static volatile int generation;
    private static volatile MinecraftServer server;

    private TextTags() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
    }

    public static boolean enabled() {
        Boolean s = synced;
        return s != null ? s : NotchConfigIO.get().text.tags;
    }

    public static int generation() {
        return generation;
    }

    public static void setSynced(Boolean value) {
        synced = value;
        changed();
    }

    private static void changed() {
        CACHE.clear();
        generation++;
    }

    public static void applyConfig(NotchConfig cfg) {
        changed();
        MinecraftServer s = server;
        if (s != null) s.execute(() -> {
            for (ServerPlayer sp : s.getPlayerList().getPlayers()) sendTo(sp);
        });
    }

    public static void sendTo(ServerPlayer sp) {
        MinecraftServer s = sp.level().getServer();
        if (s != null && net.fugginbeenus.notchcurrency.compat.Profiles.isSingleplayerOwner(s, sp)) return;
        FriendlyByteBuf buf = Net.buf();
        buf.writeBoolean(NotchConfigIO.get().text.tags);
        Net.sendToClient(sp, NotchPackets.TEXT_TAGS, buf);
    }

    public static MutableComponent parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        Component hit = CACHE.get(raw);
        if (hit == null) {
            hit = build(raw);
            CACHE.put(raw, hit);
        }
        return hit.copy();
    }

    public static String plain(String raw) {
        return parse(raw).getString();
    }

    public static String legacy(String raw) {
        return raw == null ? "" : raw.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    public static String forSave(ServerPlayer sp, String area, String raw) {
        if (raw == null || raw.isEmpty() || sp == null || Perms.isAdmin(sp, area)) return raw;
        return escapeClicks(raw);
    }

    public static String escapeClicks(String raw) {
        if (raw == null || raw.indexOf('<') < 0) return raw;
        try {
            return CLICKS.escapeTags(raw);
        } catch (RuntimeException e) {
            return raw.replace("<", "\\<");
        }
    }

    private static Component build(String raw) {
        if (!enabled()) return Component.literal(legacy(raw));
        try {
            return convert(MINI.deserialize(tagged(raw)));
        } catch (RuntimeException e) {
            return Component.literal(legacy(raw));
        }
    }

    private static String tagged(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < raw.length()) {
                char k = Character.toLowerCase(raw.charAt(i + 1));
                String tag = null;
                if ((k >= '0' && k <= '9') || (k >= 'a' && k <= 'f')) {
                    tag = "<reset><" + COLORS[Character.digit(k, 16)] + ">";
                } else if (k == 'k') {
                    tag = "<obfuscated>";
                } else if (k == 'l') {
                    tag = "<bold>";
                } else if (k == 'm') {
                    tag = "<strikethrough>";
                } else if (k == 'n') {
                    tag = "<underlined>";
                } else if (k == 'o') {
                    tag = "<italic>";
                } else if (k == 'r') {
                    tag = "<reset>";
                }
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static MutableComponent convert(net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.Component c) {
        MutableComponent out;
        if (c instanceof TextComponent t) {
            out = Component.literal(t.content());
        } else if (c instanceof TranslatableComponent t) {
            List<TranslationArgument> args = t.arguments();
            Object[] mc = new Object[args.size()];
            for (int i = 0; i < mc.length; i++) mc[i] = convert(args.get(i).asComponent());
            out = t.fallback() != null ? Component.translatableWithFallback(t.key(), t.fallback(), mc)
                    : Component.translatable(t.key(), mc);
        } else if (c instanceof KeybindComponent k) {
            out = Component.keybind(k.keybind());
        } else {
            out = Component.empty();
        }
        out.setStyle(style(c.style()));
        for (net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.Component child : c.children()) {
            out.append(convert(child));
        }
        return out;
    }

    private static Style style(net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.format.Style s) {
        Style out = Style.EMPTY;
        if (s.color() != null) out = out.withColor(net.minecraft.network.chat.TextColor.fromRgb(s.color().value()));
        out = out.withBold(flag(s.decoration(TextDecoration.BOLD)))
                .withItalic(flag(s.decoration(TextDecoration.ITALIC)))
                .withUnderlined(flag(s.decoration(TextDecoration.UNDERLINED)))
                .withStrikethrough(flag(s.decoration(TextDecoration.STRIKETHROUGH)))
                .withObfuscated(flag(s.decoration(TextDecoration.OBFUSCATED)));
        if (s.insertion() != null) out = out.withInsertion(s.insertion());
        if (s.font() != null) {
            ResourceLocation id = ResourceLocation.tryParse(s.font().asString());
            if (id != null) out = font(out, id);
        }
        //? if >=1.21.11 {
        /*if (s.shadowColor() != null) out = out.withShadowColor(s.shadowColor().value());
        *///?}
        if (s.clickEvent() != null) {
            net.minecraft.network.chat.ClickEvent click = click(s.clickEvent());
            if (click != null) out = out.withClickEvent(click);
        }
        if (s.hoverEvent() != null) {
            net.minecraft.network.chat.HoverEvent hover = hover(s.hoverEvent());
            if (hover != null) out = out.withHoverEvent(hover);
        }
        return out;
    }

    private static Boolean flag(TextDecoration.State state) {
        return state == TextDecoration.State.TRUE ? Boolean.TRUE
                : state == TextDecoration.State.FALSE ? Boolean.FALSE : null;
    }

    private static Style font(Style style, ResourceLocation id) {
        //? if >=1.21.11 {
        /*return style.withFont(new net.minecraft.network.chat.FontDescription.Resource(id));
        *///?} else {
        return style.withFont(id);
        //?}
    }

    private static net.minecraft.network.chat.ClickEvent click(ClickEvent e) {
        try {
            String value = e.value();
            ClickEvent.Action action = e.action();
            if (action == ClickEvent.Action.RUN_COMMAND) return Chat.runCommand(value);
            if (action == ClickEvent.Action.SUGGEST_COMMAND) return Chat.suggestCommand(value);
            if (action == ClickEvent.Action.OPEN_URL) return Chat.openUrl(value);
            if (action == ClickEvent.Action.COPY_TO_CLIPBOARD) return Chat.copyToClipboard(value);
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static net.minecraft.network.chat.HoverEvent hover(HoverEvent<?> e) {
        try {
            Object value = e.value();
            if (value instanceof net.fugginbeenus.notchcurrency.lib.kyori.adventure.text.Component text) {
                return Chat.showText(convert(text));
            }
            if (value instanceof HoverEvent.ShowItem item) {
                ResourceLocation id = ResourceLocation.tryParse(item.item().asString());
                if (id == null) return null;
                return Chat.showItem(new ItemStack(BuiltInRegistries.ITEM.get(id), Math.max(1, item.count())));
            }
            if (value instanceof HoverEvent.ShowEntity entity) {
                return Chat.showText(entity.name() != null ? convert(entity.name())
                        : Component.literal(entity.type().asString()));
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }
}
