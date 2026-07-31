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

            // Strategy 1/2 (fast path): the two exact signatures known from past FTB Teams
            // versions. Kept first since they're cheap and avoid the broader scan below.
            try {
                Object manager = apiClass.getMethod("getManager").invoke(apiInstance);
                if (manager != null) {
                    Method m = manager.getClass().getMethod("getTeamForPlayerID", UUID.class);
                    m.setAccessible(true);
                    resolvedMethod = m;
                    resolvedInstance = manager;
                    LOGGER.info("FTB Teams adapter: strategy 1 — manager.getTeamForPlayerID(UUID)");
                    return;
                }
            } catch (Exception ignored) {}

            try {
                Method m = apiClass.getMethod("getTeamForPlayerID", UUID.class);
                m.setAccessible(true);
                resolvedMethod = m;
                resolvedInstance = apiInstance;
                LOGGER.info("FTB Teams adapter: strategy 2 — api().getTeamForPlayerID(UUID)");
                return;
            } catch (NoSuchMethodException ignored) {}

            // Strategy 3 (auto-discovery): neither exact signature matched — FTB Teams' public
            // API has shifted method/accessor names across versions before (e.g. getManager() vs
            // manager()). Instead of hardcoding yet another guess, scan every object reachable
            // from the API instance for a single-UUID-arg method whose name reads like a
            // player-to-team lookup. setAccessible(true) also sidesteps a real reflection trap:
            // getManager() commonly returns an instance of a package-private impl class, and a
            // Method resolved from that concrete class (rather than its public interface) throws
            // IllegalAccessException on invoke() even though the method itself is public.
            java.util.List<Object> targets = new java.util.ArrayList<>();
            targets.add(apiInstance);
            for (Method m : apiClass.getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().toLowerCase().contains("manager")
                        || m.getReturnType().getSimpleName().toLowerCase().contains("manager"))) {
                    try {
                        Object val = m.invoke(apiInstance);
                        if (val != null) targets.add(val);
                    } catch (Exception ignored) {}
                }
            }

            for (Object target : targets) {
                Method found = findTeamLookupMethod(target.getClass());
                if (found != null) {
                    found.setAccessible(true);
                    resolvedMethod = found;
                    resolvedInstance = target;
                    LOGGER.info("FTB Teams adapter: strategy 3 (auto-discovered) — {}.{}(UUID)",
                            target.getClass().getSimpleName(), found.getName());
                    return;
                }
            }

            LOGGER.warn("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.warn("║  FTB TEAMS API NOT RESOLVED                                   ║");
            LOGGER.warn("║  Version {} did not match any known API signature.   ║",
                    padRight(detectedVersion, 24));
            LOGGER.warn("║  The team chat channel will not work until this is resolved.  ║");
            LOGGER.warn("║  Please report this at the NeoEssentials issue tracker, along ║");
            LOGGER.warn("║  with the method list logged just below.                      ║");
            LOGGER.warn("╚══════════════════════════════════════════════════════════════╝");

            // Deep dump: every public method on the API instance itself, PLUS one level deep
            // into whatever every zero-arg accessor on it returns — the earlier "manager"-name
            // guess found nothing, so this dumps everything rather than guessing again what the
            // real accessor/registry chain is called on this version.
            dumpAllMethods("api instance (" + apiInstance.getClass().getName() + ")", apiInstance);
            for (Method m : apiClass.getMethods()) {
                if (m.getParameterCount() != 0 || m.getReturnType() == void.class || m.getReturnType().isPrimitive()) continue;
                try {
                    Object val = m.invoke(apiInstance);
                    if (val != null && val != apiInstance) {
                        dumpAllMethods("api." + m.getName() + "() -> " + val.getClass().getName(), val);
                    }
                } catch (Exception ignored) {}
            }

        } catch (ClassNotFoundException e) {
            LOGGER.debug("FTB Teams API class not found — mod may not be installed");
        } catch (Exception e) {
            LOGGER.warn("FTB Teams adapter init failed: {}", e.getMessage());
        }
    }

    /** Finds a public, single-{@link UUID}-arg method whose name reads like a player→team lookup. */
    private Method findTeamLookupMethod(Class<?> clazz) {
        Method best = null;
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() != 1 || m.getParameterTypes()[0] != UUID.class) continue;
            String name = m.getName().toLowerCase();
            if (name.contains("team") && (name.contains("player") || name.contains("id"))) {
                if (best == null || name.contains("player")) best = m;
            }
        }
        return best;
    }

    /** Logs every public, non-Object method declared on a candidate target's class, for diagnosing an unresolved API. */
    private void dumpAllMethods(String label, Object target) {
        StringBuilder sb = new StringBuilder("FTB Teams adapter diagnostic — ").append(label).append(": ");
        boolean any = false;
        for (Method m : target.getClass().getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue;
            sb.append(m.getName()).append("(");
            Class<?>[] params = m.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(params[i].getSimpleName());
            }
            sb.append(")->").append(m.getReturnType().getSimpleName()).append("  ");
            any = true;
        }
        LOGGER.warn(any ? sb.toString() : sb.append("(no non-Object public methods)").toString());
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
