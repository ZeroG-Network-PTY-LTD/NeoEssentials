package com.zerog.neoessentials.permissions;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for FTB Ranks integration using reflection to avoid a hard compile-time dependency.
 *
 * <p>Known API strategies probed in order:
 * <ol>
 *   <li>{@code FTBRanksAPI.getPermission(ServerPlayer, String, boolean)} — 2101.1.x (NeoForge)</li>
 *   <li>{@code instance.hasPermission(UUID, String)} — older builds via INSTANCE/getInstance()</li>
 *   <li>{@code FTBRanksAPI.hasPermission(ServerPlayer, String)} — possible future static variant</li>
 *   <li>{@code FTBRanksAPI.checkPermission(ServerPlayer, String)} — possible future naming change</li>
 * </ol>
 */
public class FtbRanksAdapter implements ExternalPermissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtbRanksAdapter.class);

    /** Number of consecutive failures before we declare the adapter unhealthy. */
    private static final int MAX_FAILURES = 5;

    /** Last-tested FTB Ranks version.  Warn if detected version differs. */
    private static final String LAST_TESTED_VERSION = "2101.1.3";

    private final boolean ftbRanksLoaded;
    private final String  detectedVersion;

    // Resolved API method + the object to call it on (null = static)
    private Method resolvedMethod   = null;
    private Object resolvedInstance = null;
    private int    resolvedStrategy = 0;
    // 1 = getPermission(ServerPlayer,String,boolean)
    // 2 = hasPermission(UUID,String)       [instance]
    // 3 = hasPermission(ServerPlayer,String) [static]
    // 4 = checkPermission(ServerPlayer,String) [static]

    /** Consecutive failure counter — reset on every successful check. */
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    public FtbRanksAdapter() {
        this.ftbRanksLoaded = ModList.get().isLoaded("ftbranks");
        this.detectedVersion = ModList.get().getModContainerById("ftbranks")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        if (ftbRanksLoaded) {
            LOGGER.info("FTB Ranks detected — version: {}", detectedVersion);
            if (!detectedVersion.equals("unknown")
                    && !detectedVersion.startsWith(LAST_TESTED_VERSION.substring(0, LAST_TESTED_VERSION.lastIndexOf('.')))) {
                LOGGER.warn("╔══════════════════════════════════════════════════════════════╗");
                LOGGER.warn("║  FTB RANKS COMPATIBILITY WARNING                              ║");
                LOGGER.warn("║  Detected version : {}                              ║", padRight(detectedVersion, 30));
                LOGGER.warn("║  Last tested with : {}                              ║", padRight(LAST_TESTED_VERSION, 30));
                LOGGER.warn("║  If permissions stop working, please report this version     ║");
                LOGGER.warn("║  mismatch at github.com/your-repo/neoessentials/issues       ║");
                LOGGER.warn("╚══════════════════════════════════════════════════════════════╝");
            }
            probeApi();
        }
    }

    // ── API probe ────────────────────────────────────────────────────────────────

    private void probeApi() {
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");

            // ── Strategy 1: static getPermission(ServerPlayer, String, boolean) ─────
            // Standard API in FTB Ranks 2101.1.x
            try {
                Method m = apiClass.getMethod("getPermission",
                        net.minecraft.server.level.ServerPlayer.class, String.class, boolean.class);
                resolvedMethod   = m;
                resolvedInstance = null;
                resolvedStrategy = 1;
                LOGGER.info("FTB Ranks adapter: strategy 1 — getPermission(ServerPlayer, String, boolean)");
                return;
            } catch (NoSuchMethodException ignored) {}

            // ── Strategy 2: instance hasPermission(UUID, String) ─────────────────────
            // Older builds via INSTANCE / getInstance()
            Object instance = null;
            try {
                instance = apiClass.getField("INSTANCE").get(null);
            } catch (NoSuchFieldException e) {
                try {
                    instance = apiClass.getMethod("getInstance").invoke(null);
                } catch (Exception ignored2) {}
            }
            if (instance != null) {
                try {
                    Method m = instance.getClass().getMethod("hasPermission", UUID.class, String.class);
                    resolvedMethod   = m;
                    resolvedInstance = instance;
                    resolvedStrategy = 2;
                    LOGGER.info("FTB Ranks adapter: strategy 2 — instance.hasPermission(UUID, String)");
                    return;
                } catch (NoSuchMethodException ignored) {}
            }

            // ── Strategy 3: static hasPermission(ServerPlayer, String) ───────────────
            // Possible future static variant without the boolean default parameter
            try {
                Method m = apiClass.getMethod("hasPermission",
                        net.minecraft.server.level.ServerPlayer.class, String.class);
                resolvedMethod   = m;
                resolvedInstance = null;
                resolvedStrategy = 3;
                LOGGER.info("FTB Ranks adapter: strategy 3 — hasPermission(ServerPlayer, String)");
                return;
            } catch (NoSuchMethodException ignored) {}

            // ── Strategy 4: static checkPermission(ServerPlayer, String) ─────────────
            // Alternative naming used by some FTB Ranks forks / future versions
            try {
                Method m = apiClass.getMethod("checkPermission",
                        net.minecraft.server.level.ServerPlayer.class, String.class);
                resolvedMethod   = m;
                resolvedInstance = null;
                resolvedStrategy = 4;
                LOGGER.info("FTB Ranks adapter: strategy 4 — checkPermission(ServerPlayer, String)");
                return;
            } catch (NoSuchMethodException ignored) {}

            LOGGER.warn("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.warn("║  FTB RANKS API NOT RESOLVED                                  ║");
            LOGGER.warn("║  Version {} did not match any known API signature.  ║", padRight(detectedVersion, 24));
            LOGGER.warn("║  Permission checks will fall back to OP / internal system.   ║");
            LOGGER.warn("║  Please report this at the NeoEssentials issue tracker.      ║");
            LOGGER.warn("╚══════════════════════════════════════════════════════════════╝");

        } catch (ClassNotFoundException e) {
            LOGGER.debug("FTB Ranks API class not found — mod may not be installed");
        } catch (Exception e) {
            LOGGER.warn("FTB Ranks adapter init failed: {}", e.getMessage());
        }
    }

    // ── Permission check ─────────────────────────────────────────────────────────

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        if (!ftbRanksLoaded || resolvedMethod == null) return false;
        try {
            boolean result = invokeResolvedMethod(uuid, permission);
            consecutiveFailures.set(0); // reset on success
            return result;
        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();
            emitHealthWarnIfNeeded(failures, permission, e);
        }
        return false;
    }

    private boolean invokeResolvedMethod(UUID uuid, String permission) throws Exception {
        var server = ServerLifecycleHooks.getCurrentServer();

        if (resolvedStrategy == 1) {
            // getPermission(ServerPlayer, String, boolean)
            if (server == null) return false;
            net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) return false;
            return extractBoolean(resolvedMethod.invoke(null, player, permission, false));

        } else if (resolvedStrategy == 3 || resolvedStrategy == 4) {
            // hasPermission(ServerPlayer, String) or checkPermission(ServerPlayer, String)
            if (server == null) return false;
            net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) return false;
            return extractBoolean(resolvedMethod.invoke(null, player, permission));

        } else if (resolvedStrategy == 2) {
            // instance.hasPermission(UUID, String)
            Object result = resolvedMethod.invoke(resolvedInstance, uuid, permission);
            return result instanceof Boolean b && b;
        }
        return false;
    }

    /**
     * Coerce the raw return value from the FTB Ranks API into a boolean.
     * Handles Boolean, Optional&lt;Boolean&gt;, and TriState/enum return types.
     */
    private boolean extractBoolean(Object result) {
        if (result == null) return false;
        if (result instanceof Boolean b) return b;
        if (result instanceof java.util.Optional<?> opt) {
            Object inner = opt.orElse(null);
            if (inner instanceof Boolean b) return b;
        }
        // TriState / enum — try a get() method first, then toString comparison
        try {
            return (boolean) result.getClass().getMethod("get").invoke(result);
        } catch (Exception ignored) {}
        String s = result.toString().toUpperCase();
        return !s.equals("FALSE") && !s.equals("UNDEFINED") && !s.equals("DENY");
    }

    private void emitHealthWarnIfNeeded(int failures, String permission, Exception cause) {
        if (failures == 1) {
            LOGGER.error("FTB Ranks permission check failed for '{}': {}", permission,
                    cause.getMessage());
        } else if (failures == MAX_FAILURES) {
            LOGGER.warn("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.warn("║  FTB RANKS ADAPTER UNHEALTHY — {} consecutive failures    ║", MAX_FAILURES);
            LOGGER.warn("║  Version     : {}                                   ║", padRight(detectedVersion, 21));
            LOGGER.warn("║  Last error  : {}  ║", padRight(cause.getMessage() != null
                    ? cause.getMessage().substring(0, Math.min(cause.getMessage().length(), 42)) : "n/a", 42));
            LOGGER.warn("║  NeoEssentials will fall back to internal permissions.       ║");
            LOGGER.warn("║  Resolve the FTB Ranks API issue and run /neoe reload.       ║");
            LOGGER.warn("╚══════════════════════════════════════════════════════════════╝");
        }
    }

    // ── ExternalPermissionAdapter extras ─────────────────────────────────────────

    @Override
    public String getPrefix(UUID uuid) { return null; }

    @Override
    public String getSuffix(UUID uuid) { return null; }

    @Override
    public void reload() { /* FTB Ranks handles its own reload */ }

    @Override
    public String getName() { return "FTB Ranks"; }

    @Override
    public boolean isAvailable() {
        return ftbRanksLoaded && resolvedMethod != null;
    }

    @Override
    public String getVersion() { return detectedVersion; }

    @Override
    public boolean isHealthy() { return consecutiveFailures.get() < MAX_FAILURES; }

    @Override
    public int getConsecutiveFailures() { return consecutiveFailures.get(); }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Right-pad a string to exactly {@code width} chars (for log alignment). */
    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}
