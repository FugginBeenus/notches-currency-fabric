package net.fugginbeenus.notchcurrency.economy;

import net.fugginbeenus.notchcurrency.core.BalanceState;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared formatting for the balance leaderboard, used by both {@code /baltop} and the
 * Ledger Board block so they render identically.
 */
public final class EconomyLeaderboard {

    private EconomyLeaderboard() {}

    /** The header + ranked lines for the top {@code limit} balances. Empty list if none. */
    public static List<Text> topLines(MinecraftServer server, int limit) {
        List<Map.Entry<UUID, Long>> top = BalanceState.get(server).snapshot()
                .entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .toList();

        List<Text> lines = new ArrayList<>();
        if (top.isEmpty()) {
            lines.add(Text.literal("No balances yet.").formatted(Formatting.GRAY));
            return lines;
        }

        lines.add(Text.literal("─── Top Balances ───").formatted(Formatting.GOLD));
        int rank = 1;
        for (Map.Entry<UUID, Long> e : top) {
            lines.add(Text.literal(" " + (rank++) + ". ").formatted(Formatting.YELLOW)
                    .append(Text.literal(nameOf(server, e.getKey())).formatted(Formatting.WHITE))
                    .append(Text.literal(" — ").formatted(Formatting.GRAY))
                    .append(Text.literal(e.getValue() + " ").formatted(Formatting.GOLD))
                    .append(NotchCurrency.coinIcon()));
        }
        return lines;
    }

    /** Resolve a UUID to a username via the server user cache, falling back to a short id. */
    public static String nameOf(MinecraftServer server, UUID id) {
        try {
            var cache = server.getUserCache();
            if (cache != null) {
                var profile = cache.getByUuid(id);
                if (profile.isPresent() && profile.get().getName() != null) {
                    return profile.get().getName();
                }
            }
        } catch (Exception ignored) {}
        return id.toString().substring(0, 8);
    }
}
