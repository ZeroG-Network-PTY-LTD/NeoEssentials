package com.zerog.neoessentials.teams;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapter for FTB Teams, resolved entirely via reflection (same approach as
 * {@link com.zerog.neoessentials.permissions.FtbRanksAdapter}) so this class has zero
 * compile-time or classloading dependency on FTB Teams' types — safe to construct
 * unconditionally even on packs that don't have FTB Teams installed at all.
 *
 * <p>Known API strategies probed in order (FTB Teams' public API has shifted the exact
 * accessor path across versions):
 * <ol>
 *   <li>{@code FTBTeamsAPI.api().getManager().getTeamForPlayerID(UUID)} — current NeoForge API</li>
 *   <li>{@code FTBTeamsAPI.api().getTeamForPlayerID(UUID)} — older versions without a
 *       separate manager indirection</li>
 * </ol>
 * Both return an {@code Optional<Team>} (or a bare, possibly-null {@code Team}) — handled
 * either way in {@link #extractTeamId(Object)}.
 */
public class FtbTeamsAdapter implements TeamProviderAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtbTeamsAdapter.class);
    private static final int MAX_FAILURES = 5;

    private final boolean ftbTeamsLoaded;
    private final String detectedVersion;

    private Method resolvedMethod = null;
    private Object resolvedInstance = null;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    public FtbTeamsAdapter() {
        this.ftbTeamsLoaded = ModList.get().isLoaded("ftbteams");
        this.detectedVersion = ModList.get().getModContainerById("ftbteams")
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");

        if (ftbTeamsLoaded) {
            LOGGER.info("FTB Teams detected — version: {}", detectedVersion);
            probeApi();
        }
    }

    private void probeApi() {
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object apiInstance = apiClass.getMethod("api").invoke(null);
            if (apiInstance == null) {
                LOGGER.warn("FTB Teams adapter: FTBTeamsAPI.api() returned null, cannot resolve team lookups");
                return;
            }

            // Strategy 1: api().getManager().getTeamForPlayerID(UUID)
            try {
                Object manager = apiClass.getMethod("getManager").invoke(apiInstance);
                if (manager != null) {
                    Method m = manager.getClass().getMethod("getTeamForPlayerID", UUID.class);
                    resolvedMethod = m;
                    resolvedInstance = manager;
                    LOGGER.info("FTB Teams adapter: strategy 1 — manager.getTeamForPlayerID(UUID)");
                    return;
                }
            } catch (Exception ignored) {}

            // Strategy 2: api().getTeamForPlayerID(UUID) directly on the API instance
            try {
                Method m = apiClass.getMethod("getTeamForPlayerID", UUID.class);
                resolvedMethod = m;
                resolvedInstance = apiInstance;
                LOGGER.info("FTB Teams adapter: strategy 2 — api().getTeamForPlayerID(UUID)");
                return;
            } catch (NoSuchMethodException ignored) {}

            LOGGER.warn("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.warn("║  FTB TEAMS API NOT RESOLVED                                   ║");
            LOGGER.warn("║  Version {} did not match any known API signature.   ║",
                    padRight(detectedVersion, 24));
            LOGGER.warn("║  The team chat channel will not work until this is resolved.  ║");
            LOGGER.warn("║  Please report this at the NeoEssentials issue tracker.       ║");
            LOGGER.warn("╚══════════════════════════════════════════════════════════════╝");

        } catch (ClassNotFoundException e) {
            LOGGER.debug("FTB Teams API class not found — mod may not be installed");
        } catch (Exception e) {
            LOGGER.warn("FTB Teams adapter init failed: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return ftbTeamsLoaded && resolvedMethod != null;
    }

    @Override
    public String getName() {
        return "FTB Teams";
    }

    @Override
    public String getTeamId(UUID playerUUID) {
        if (!isAvailable()) return null;
        try {
            Object result = resolvedMethod.invoke(resolvedInstance, playerUUID);
            String teamId = extractTeamId(result);
            consecutiveFailures.set(0);
            return teamId;
        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();
            if (failures == 1) {
                LOGGER.error("FTB Teams lookup failed for player {}: {}", playerUUID, e.getMessage());
            } else if (failures == MAX_FAILURES) {
                LOGGER.warn("FTB Teams adapter unhealthy — {} consecutive failures, last error: {}",
                        MAX_FAILURES, e.getMessage());
            }
            return null;
        }
    }

    /** Unwraps an {@code Optional<Team>} (or a bare, nullable {@code Team}) into a stable id string. */
    private String extractTeamId(Object result) {
        if (result == null) return null;
        Object team = result;
        if (result instanceof Optional<?> opt) {
            team = opt.orElse(null);
            if (team == null) return null;
        }
        try {
            Object id = team.getClass().getMethod("getId").invoke(team);
            if (id != null) return id.toString();
        } catch (Exception ignored) {}
        try {
            Object shortName = team.getClass().getMethod("getShortName").invoke(team);
            if (shortName != null) return shortName.toString();
        } catch (Exception ignored) {}
        return team.toString();
    }

    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}
