package com.zerog.neoessentials.permissions;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Startup utility that reads every known external-permission-mod version from the
 * NeoForge {@link ModList} and emits a formatted compatibility report.
 *
 * <p>If a detected version is <em>newer</em> than the last version we tested against,
 * a {@code WARN} line is emitted so server admins know to watch for permission issues.</p>
 *
 * <p>This class is purely informational — it never blocks startup or changes any state.</p>
 */
public final class AdapterCompatibilityChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdapterCompatibilityChecker.class);

    /**
     * Mapping of NeoForge mod-ID → last-tested version.
     * The version string uses the first two numeric segments for comparison so that
     * minor patch bumps within the same minor line do not trigger a warning.
     */
    private static final Map<String, String> LAST_TESTED = new LinkedHashMap<>();

    static {
        LAST_TESTED.put("ftbranks",  "2101.1.3");
        LAST_TESTED.put("luckperms", "5.4");       // LuckPerms uses MAJOR.MINOR.PATCH
        LAST_TESTED.put("ftb-library",  "2101.1.21"); // optional indirect dep
    }

    private AdapterCompatibilityChecker() { /* utility class */ }

    /**
     * Scans the mod list, builds a compatibility table, and logs it at INFO / WARN level.
     *
     * @param activeAdapter the adapter that was selected during initialisation, or {@code null}
     *                      if none was found.
     */
    public static void generateReport(ExternalPermissionAdapter activeAdapter) {
        LOGGER.info("╔══════════════════════════════════════════════════════════════════════╗");
        LOGGER.info("║        NeoEssentials — External Permissions Compatibility Report      ║");
        LOGGER.info("╠══════════════════════════════════════════════════════════════════════╣");

        if (activeAdapter == null) {
            LOGGER.info("║  Active adapter  : NONE (using internal NeoEssentials permissions)    ║");
        } else {
            String adapterLine = String.format("%-32s v%-18s",
                    activeAdapter.getName(), activeAdapter.getVersion());
            LOGGER.info("║  Active adapter  : {}  ║", padRight(adapterLine, 54));
            LOGGER.info("║  Health status   : {}  ║",
                    padRight(activeAdapter.isHealthy() ? "✓ HEALTHY" : "✗ UNHEALTHY — fallback active", 54));
        }

        LOGGER.info("╠══════════════════════════════════════════════════════════════════════╣");
        LOGGER.info("║  Installed permission mods:                                          ║");

        boolean anyWarn = false;
        for (Map.Entry<String, String> entry : LAST_TESTED.entrySet()) {
            String modId      = entry.getKey();
            String lastTested = entry.getValue();

            String detected = ModList.get().getModContainerById(modId)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse(null);

            if (detected == null) continue; // not installed — skip

            boolean compatible = isCompatible(detected, lastTested);
            String status = compatible ? "✓ compatible" : "⚠ NEWER THAN TESTED";
            String row = String.format("  %-14s  detected: %-14s  last tested: %-10s  %s",
                    modId, detected, lastTested, status);
            if (compatible) {
                LOGGER.info("║  {}  ║", padRight(row, 68));
            } else {
                LOGGER.warn("║  {}  ║", padRight(row, 68));
                anyWarn = true;
            }
        }

        if (anyWarn) {
            LOGGER.warn("╠══════════════════════════════════════════════════════════════════════╣");
            LOGGER.warn("║  ⚠  One or more permission mods are newer than last-tested versions.  ║");
            LOGGER.warn("║     Permissions should still work — NeoEssentials probes APIs via     ║");
            LOGGER.warn("║     reflection and will fall back to the internal system if needed.    ║");
            LOGGER.warn("║     If you see issues, please open a GitHub issue with your mod list.  ║");
        }

        LOGGER.info("╚══════════════════════════════════════════════════════════════════════╝");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the detected version is the same major.minor line as
     * (or older than) the last-tested version.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code 2101.1.3} detected vs {@code 2101.1.3} tested → {@code true}</li>
     *   <li>{@code 2101.1.4} detected vs {@code 2101.1.3} tested → {@code true} (same minor)</li>
     *   <li>{@code 2101.2.0} detected vs {@code 2101.1.3} tested → {@code false} (minor bump)</li>
     * </ul>
     */
    static boolean isCompatible(String detected, String lastTested) {
        try {
            String[] d = detected.split("[.\\-]");
            String[] t = lastTested.split("[.\\-]");
            // Compare up to the number of segments in lastTested
            int segments = Math.min(t.length, Math.min(d.length, 2));
            for (int i = 0; i < segments; i++) {
                int di = parseSegment(d[i]);
                int ti = parseSegment(t[i]);
                if (di < ti) return true;   // older than tested → fine
                if (di > ti) return false;  // newer on this segment → warn
            }
            return true; // identical up to compared segments
        } catch (Exception e) {
            return true; // can't parse — assume compatible to avoid noise
        }
    }

    private static int parseSegment(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Right-pad a string to exactly {@code width} chars. */
    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}

