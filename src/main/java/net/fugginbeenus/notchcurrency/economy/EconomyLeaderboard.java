package net.fugginbeenus.notchcurrency.economy;

import net.fugginbeenus.notchcurrency.core.BalanceState;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EconomyLeaderboard {

    private EconomyLeaderboard() {}

    public record Entry(String name, long balance) {}

    public static List<Entry> topEntries(MinecraftServer server, int limit) {
        List<Entry> out = new ArrayList<>();
        BalanceState.get(server).snapshot().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .forEach(e -> out.add(new Entry(nameOf(server, e.getKey()), e.getValue())));
        return out;
    }

    public static List<Component> topLines(MinecraftServer server, int limit) {
        List<Map.Entry<UUID, Long>> top = BalanceState.get(server).snapshot()
                .entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .toList();

        List<Component> lines = new ArrayList<>();
        if (top.isEmpty()) {
            lines.add(Component.literal("No balances yet.").withStyle(ChatFormatting.GRAY));
            return lines;
        }

        lines.add(Component.literal("─── Top Balances ───").withStyle(ChatFormatting.GOLD));
        int rank = 1;
        for (Map.Entry<UUID, Long> e : top) {
            lines.add(Component.literal(" " + (rank++) + ". ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(nameOf(server, e.getKey())).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(e.getValue() + " ").withStyle(ChatFormatting.GOLD))
                    .append(NotchCurrency.coinIcon()));
        }
        return lines;
    }

    public static String nameOf(MinecraftServer server, UUID id) {
        try {
            var cache = server.getProfileCache();
            if (cache != null) {
                var profile = cache.get(id);
                if (profile.isPresent() && profile.get().getName() != null) {
                    return profile.get().getName();
                }
            }
        } catch (Exception ignored) {}
        return id.toString().substring(0, 8);
    }
}
