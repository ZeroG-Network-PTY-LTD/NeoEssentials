package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.webdashboard.analytics.PlayerSessionTracker;
import com.zerog.neoessentials.webdashboard.analytics.WebSocketEventBroadcaster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aggregated statistics endpoint for the dashboard Statistics page.
 *
 * <pre>
 *   GET /api/stats/overview     — economy + activity + performance combined
 *   GET /api/stats/economy      — economy metrics (top balances, total wealth, accounts)
 *   GET /api/stats/activity     — player session metrics (active, peak, unique today)
 *   GET /api/stats/performance  — performance metrics (TPS, memory, uptime, history)
 * </pre>
 *
 * <p>TPS history is sampled once per minute into a 60-point ring buffer (last 60 minutes).
 * Memory history is sampled alongside TPS.
 */
public class StatsEndpoint implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatsEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    // ── Performance history ring buffer ───────────────────────────────────────

    /** 60-minute rolling TPS/memory history (one sample per minute). */
    private static final int HISTORY_POINTS = 60;
    private final double[] tpsHistory   = new double[HISTORY_POINTS];
    private final long[]   memHistory   = new long[HISTORY_POINTS];    // used MB
    private final String[] timeLabels   = new String[HISTORY_POINTS];
    private int historyHead = 0;
    private int historySamples = 0;

    private final ScheduledExecutorService sampler;
    private final MinecraftServer server;

    /** Peak online count tracked since last server start. */
    private final AtomicInteger peakOnline = new AtomicInteger(0);

    /** Unique player UUIDs seen online since last server start. */
    private final Set<java.util.UUID> uniqueToday = Collections.synchronizedSet(new LinkedHashSet<>());

    public StatsEndpoint(MinecraftServer server) {
        this.server = server;
        // Start sampling TPS + memory every 60 seconds
        sampler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NeoEssentials-StatsSampler");
            t.setDaemon(true);
            return t;
        });
        sampler.scheduleAtFixedRate(this::sample, 5, 60, TimeUnit.SECONDS);
    }

    /** Called by the lifecycle manager when the server starts. */
    public void shutdown() {
        sampler.shutdownNow();
    }

    // ── Sampling ─────────────────────────────────────────────────────────────

    private void sample() {
        try {
            // TPS
            double avgMs = server.getAverageTickTimeNanos() / 1_000_000.0;
            double tps = Math.min(20.0, 1000.0 / Math.max(50.0, avgMs));
            // Memory
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            // Player peak
            int online = server.getPlayerCount();
            if (online > peakOnline.get()) peakOnline.set(online);
            // Track UUIDs seen today
            server.getPlayerList().getPlayers().forEach(p -> uniqueToday.add(p.getUUID()));

            // Store
            tpsHistory[historyHead]  = Math.round(tps * 10.0) / 10.0;
            memHistory[historyHead]  = usedMb;
            timeLabels[historyHead]  = ISO.format(Instant.now());
            historyHead = (historyHead + 1) % HISTORY_POINTS;
            if (historySamples < HISTORY_POINTS) historySamples++;

            // Push live stats pulse to WebSocket clients
            WebSocketEventBroadcaster.broadcastStatsPulse(server);
        } catch (Exception e) {
            LOGGER.debug("[Stats] Sample error: {}", e.getMessage());
        }
    }

    // ── HTTP Handler ─────────────────────────────────────────────────────────

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        if (!"GET".equals(method)) {
            respond(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "StatsEndpoint request: GET {}", path);
        try {
            if      (path.endsWith("/overview"))    respond(exchange, 200, GSON.toJson(buildOverview()));
            else if (path.endsWith("/economy"))     respond(exchange, 200, GSON.toJson(buildEconomy()));
            else if (path.endsWith("/activity"))    respond(exchange, 200, GSON.toJson(buildActivity()));
            else if (path.endsWith("/performance")) respond(exchange, 200, GSON.toJson(buildPerformance()));
            else respond(exchange, 404, "{\"error\":\"Not found\"}");
        } catch (Exception e) {
            LOGGER.error("[StatsEndpoint] Error for {}: {}", path, e.getMessage(), e);
            respond(exchange, 500, "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    // ── Economy stats ─────────────────────────────────────────────────────────

    private JsonObject buildEconomy() {
        JsonObject eco = new JsonObject();
        try {
            EconomyManager em = EconomyManager.getInstance();
            Map<UUID, BigDecimal> all = em.getAllBalances();

            // Total wealth & account count
            BigDecimal total = all.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            eco.addProperty("totalWealth",    total.setScale(2, RoundingMode.HALF_UP).toPlainString());
            eco.addProperty("accountCount",   all.size());
            eco.addProperty("currencySymbol", ConfigManager.getCurrencySymbol());
            eco.addProperty("startingBalance", ConfigManager.getEconomyStartingBalance());

            // Average balance (excluding zero)
            long nonZero = all.values().stream().filter(b -> b.compareTo(BigDecimal.ZERO) > 0).count();
            if (nonZero > 0) {
                BigDecimal avg = total.divide(BigDecimal.valueOf(nonZero), 2, RoundingMode.HALF_UP);
                eco.addProperty("averageBalance", avg.toPlainString());
            } else {
                eco.addProperty("averageBalance", "0.00");
            }

            // Top 10 by balance
            GameProfileCache cache = server.getProfileCache();
            List<Map.Entry<UUID, BigDecimal>> top = all.entrySet().stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .toList();

            JsonArray topArr = new JsonArray();
            int rank = 1;
            for (Map.Entry<UUID, BigDecimal> e : top) {
                String name = resolveUsername(e.getKey(), cache);
                JsonObject entry = new JsonObject();
                entry.addProperty("rank",    rank++);
                entry.addProperty("uuid",    e.getKey().toString());
                entry.addProperty("name",    name);
                entry.addProperty("balance", e.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString());
                entry.addProperty("online",  server.getPlayerList().getPlayer(e.getKey()) != null);
                topArr.add(entry);
            }
            eco.add("topPlayers", topArr);

            // Balance distribution brackets
            eco.add("distribution", buildBalanceDistribution(all));
        } catch (Exception e) {
            LOGGER.warn("[Stats] Economy stats error: {}", e.getMessage());
            eco.addProperty("error", e.getMessage());
        }
        return eco;
    }

    /** Builds a simple balance-bracket histogram for the distribution chart. */
    private JsonArray buildBalanceDistribution(Map<UUID, BigDecimal> balances) {
        long[] brackets         = {0, 100, 500, 1000, 5000, 10000, 50000, Long.MAX_VALUE};
        String[] bracketLabels  = {"$0", "$1–100", "$101–500", "$501–1k", "$1k–5k", "$5k–10k", "$10k–50k", "$50k+"};
        int[] counts = new int[bracketLabels.length];

        for (BigDecimal b : balances.values()) {
            // longValueExact() throws ArithmeticException ("Rounding necessary") for any balance
            // with a fractional part (i.e. almost every real balance, since cents are normal) —
            // this bracket histogram only needs the whole-dollar bucket, so longValue() (which
            // truncates instead of throwing) is correct here.
            long v = b.longValue();
            for (int i = brackets.length - 1; i >= 0; i--) {
                if (v >= brackets[i]) { counts[i]++; break; }
            }
        }
        JsonArray arr = new JsonArray();
        for (int i = 0; i < bracketLabels.length; i++) {
            JsonObject o = new JsonObject();
            o.addProperty("label", bracketLabels[i]);
            o.addProperty("count", counts[i]);
            arr.add(o);
        }
        return arr;
    }

    // ── Activity stats ────────────────────────────────────────────────────────

    private JsonObject buildActivity() {
        JsonObject act = new JsonObject();
        try {
            PlayerSessionTracker tracker = PlayerSessionTracker.getInstance();

            // Current active sessions
            Collection<PlayerSessionTracker.PlayerSession> active = tracker.getActiveSessions();
            act.addProperty("currentOnline",   active.size());
            act.addProperty("peakOnlineToday",  Math.max(peakOnline.get(), active.size()));
            act.addProperty("uniqueToday",      uniqueToday.size() + active.size()); // approximate

            // Active sessions list
            JsonArray sessions = new JsonArray();
            active.stream()
                .sorted(Comparator.comparingLong(PlayerSessionTracker.PlayerSession::getStartTime))
                .forEach(s -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("name",       s.getPlayerName());
                    o.addProperty("sessionMs",  s.getDuration());
                    o.addProperty("sessionMin", s.getDuration() / 60_000);
                    sessions.add(o);
                });
            act.add("activeSessions", sessions);

            // Historical summary
            List<PlayerSessionTracker.PlayerSession> history = tracker.getHistoricalSessions();
            act.addProperty("completedSessionsToday", history.size());
            double avgMs = history.stream()
                .mapToLong(PlayerSessionTracker.PlayerSession::getDuration)
                .average().orElse(0);
            act.addProperty("avgSessionMinutes", Math.round(avgMs / 60_000));
            long totalPlayMs = history.stream().mapToLong(PlayerSessionTracker.PlayerSession::getDuration).sum();
            act.addProperty("totalPlayMinutesToday", totalPlayMs / 60_000);

            // Recent sessions (last 10)
            JsonArray recent = new JsonArray();
            history.stream()
                .sorted(Comparator.comparingLong(PlayerSessionTracker.PlayerSession::getEndTime).reversed())
                .limit(10)
                .forEach(s -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("name",       s.getPlayerName());
                    o.addProperty("sessionMin", s.getDuration() / 60_000);
                    o.addProperty("endedAt",    s.getEndTime());
                    recent.add(o);
                });
            act.add("recentSessions", recent);
        } catch (Exception e) {
            LOGGER.warn("[Stats] Activity stats error: {}", e.getMessage());
            act.addProperty("error", e.getMessage());
        }
        return act;
    }

    // ── Performance stats ─────────────────────────────────────────────────────

    private JsonObject buildPerformance() {
        JsonObject perf = new JsonObject();
        try {
            // Current values
            double avgMs = server.getAverageTickTimeNanos() / 1_000_000.0;
            double tps   = Math.min(20.0, 1000.0 / Math.max(50.0, avgMs));
            Runtime rt   = Runtime.getRuntime();
            long usedMb  = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long maxMb   = rt.maxMemory() / (1024 * 1024);
            long upMs    = ManagementFactory.getRuntimeMXBean().getUptime();

            perf.addProperty("tps",           Math.round(tps * 10.0) / 10.0);
            perf.addProperty("tickMs",        Math.round(avgMs * 10.0) / 10.0);
            perf.addProperty("memUsedMb",     usedMb);
            perf.addProperty("memMaxMb",      maxMb);
            perf.addProperty("memPercent",    Math.round((double) usedMb / maxMb * 100.0));
            perf.addProperty("uptimeMs",      upMs);
            perf.addProperty("uptimeHours",   upMs / 3_600_000);
            perf.addProperty("uptimeMinutes", (upMs % 3_600_000) / 60_000);
            perf.addProperty("players",       server.getPlayerCount());
            perf.addProperty("playersMax",    server.getMaxPlayers());

            // CPU
            var os = ManagementFactory.getOperatingSystemMXBean();
            perf.addProperty("cpuCores",  os.getAvailableProcessors());
            perf.addProperty("loadAvg",   Math.round(os.getSystemLoadAverage() * 100.0) / 100.0);

            // History (oldest → newest)
            int n = historySamples;
            JsonArray tpsH = new JsonArray();
            JsonArray memH = new JsonArray();
            JsonArray lblH = new JsonArray();
            int start = (historyHead - n + HISTORY_POINTS) % HISTORY_POINTS;
            for (int i = 0; i < n; i++) {
                int idx = (start + i) % HISTORY_POINTS;
                tpsH.add(tpsHistory[idx]);
                memH.add(memHistory[idx]);
                lblH.add(timeLabels[idx] != null ? timeLabels[idx] : "");
            }
            perf.add("tpsHistory",    tpsH);
            perf.add("memHistory",    memH);
            perf.add("timeLabels",    lblH);
        } catch (Exception e) {
            LOGGER.warn("[Stats] Performance stats error: {}", e.getMessage());
            perf.addProperty("error", e.getMessage());
        }
        return perf;
    }

    // ── Overview (all in one) ─────────────────────────────────────────────────

    private JsonObject buildOverview() {
        JsonObject o = new JsonObject();
        o.add("economy",     buildEconomy());
        o.add("activity",    buildActivity());
        o.add("performance", buildPerformance());
        return o;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveUsername(UUID uuid, GameProfileCache cache) {
        if (cache != null) {
            try {
                var opt = cache.get(uuid);
                if (opt.isPresent() && opt.get().getName() != null) return opt.get().getName();
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Failed to resolve username for uuid={}", uuid, e);
            }
        }
        var player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getName().getString();
        return uuid.toString().substring(0, 8) + "…";
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}

