package net.fugginbeenus.notchcurrency.economy;

import com.google.gson.JsonObject;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class EconomyLedger {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-Ledger");

    private EconomyLedger() {}
    private static final AtomicLong sessionCreated = new AtomicLong();
    private static final AtomicLong sessionDestroyed = new AtomicLong();
    private static final Object FILE_LOCK = new Object();
    private static String currentDate;
    private static BufferedWriter writer;
    private static volatile HttpClient httpClient;

    public static void record(MinecraftServer server, UUID playerId, long delta,
                              long newBalance, TransactionReason reason, @Nullable String detail) {
        if (server == null || playerId == null || delta == 0) return;

        switch (reason) {
            case FAUCET -> { if (delta > 0) sessionCreated.addAndGet(delta); }
            case SINK -> { if (delta < 0) sessionDestroyed.addAndGet(-delta); }
            case ADMIN -> {
                if (delta > 0) sessionCreated.addAndGet(delta);
                else sessionDestroyed.addAndGet(-delta);
            }
            default -> { /* circulation: no effect on total supply */ }
        }

        String name = resolveName(server, playerId);
        NotchConfig.Ledger cfg = NotchConfigIO.get().ledger;

        if (cfg.fileLogEnabled) {
            appendToFile(server, playerId, name, delta, newBalance, reason, detail);
        }

        if (cfg.webhookEnabled && !cfg.webhookUrl.isBlank()) {
            boolean bySize = cfg.webhookLargeTxnThreshold > 0
                    && Math.abs(delta) >= cfg.webhookLargeTxnThreshold;
            if (reason.isAdminRelevant() || bySize) {
                postWebhook(cfg.webhookUrl, formatWebhook(name, delta, newBalance, reason, detail));
            }
        }
    }

    public static long getSessionCreated() { return sessionCreated.get(); }
    public static long getSessionDestroyed() { return sessionDestroyed.get(); }
    private static String resolveName(MinecraftServer server, UUID id) {
        try {
            var name = net.fugginbeenus.notchcurrency.compat.Profiles.nameOf(server, id);
            if (name.isPresent()) return name.get();
        } catch (Exception ignored) {
        }
        return id.toString();
    }

    private static void appendToFile(MinecraftServer server, UUID id, String name, long delta,
                                     long newBalance, TransactionReason reason, @Nullable String detail) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("ts", Instant.now().toString());
            o.addProperty("uuid", id.toString());
            o.addProperty("name", name);
            o.addProperty("delta", delta);
            o.addProperty("balance", newBalance);
            o.addProperty("reason", reason.name());
            if (detail != null && !detail.isBlank()) o.addProperty("detail", detail);

            synchronized (FILE_LOCK) {
                BufferedWriter w = writerFor(server);
                if (w != null) {
                    w.write(o.toString());
                    w.newLine();
                    w.flush();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to write economy ledger entry", e);
        }
    }

    private static BufferedWriter writerFor(MinecraftServer server) throws IOException {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        if (writer != null && today.equals(currentDate)) {
            return writer;
        }
        if (writer != null) {
            try { writer.close(); } catch (IOException ignored) {}
            writer = null;
        }
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("notchcurrency").resolve("ledger");
        Files.createDirectories(dir);
        Path file = dir.resolve("economy-" + today + ".jsonl");
        writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        currentDate = today;
        return writer;
    }

    public static void close() {
        synchronized (FILE_LOCK) {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
                writer = null;
                currentDate = null;
            }
        }
    }

    private static String formatWebhook(String name, long delta, long newBalance,
                                        TransactionReason reason, @Nullable String detail) {
        String sign = delta >= 0 ? "+" : "";
        StringBuilder sb = new StringBuilder();
        sb.append(reason.isAdminRelevant() ? "🛠️ **[ADMIN]** " : "💰 ");
        sb.append("`").append(name).append("` ")
                .append(sign).append(delta)
                .append(" → ").append(newBalance)
                .append("  (").append(reason.name());
        if (detail != null && !detail.isBlank()) sb.append(": ").append(detail);
        sb.append(")");
        return sb.toString();
    }

    private static void postWebhook(String url, String content) {
        try {
            if (httpClient == null) {
                synchronized (EconomyLedger.class) {
                    if (httpClient == null) httpClient = HttpClient.newHttpClient();
                }
            }
            JsonObject body = new JsonObject();
            body.addProperty("content", content);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        LOGGER.warn("Economy webhook post failed: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            LOGGER.warn("Economy webhook error", e);
        }
    }
}
