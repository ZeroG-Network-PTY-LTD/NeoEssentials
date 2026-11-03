package com.zerog.neoessentials.webdashboard.security;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canonical tab-id registry for the dashboard's custom (per-group) permission system — each
 * entry pairs a short id (what an admin writes in {@code webDashboard.groupPermissions[].tabs[].tab})
 * with the REST API path prefix it actually gates, mirroring the {@code createContext("/api/...")}
 * calls in {@link com.zerog.neoessentials.webdashboard.DashboardAPI}. Single source of truth for
 * both directions of the lookup: {@link AuthenticationFilter} resolves an incoming request path to
 * a tab id to check against a CUSTOM-role user's granted permissions, and the
 * {@code /api/auth/my-permissions}-style responses resolve the other way (tab id to "can I see
 * this nav item") for both frontends.
 *
 * <p>Adding a new {@code /api/<feature>} endpoint to {@code DashboardAPI} should also add its
 * tab id here — otherwise a CUSTOM-role user's access to it can never be granted (not a security
 * hole: an unregistered prefix just never matches any tab, so it falls through to denied, same as
 * an unknown path does today).
 */
public final class DashboardTab {
    private DashboardTab() {}

    /** tabId -> API path prefix (no trailing slash). Insertion order doesn't matter for lookups,
     *  but kept alphabetical-ish by feature area for readability when read as source. */
    private static final Map<String, String> TAB_TO_PREFIX = new LinkedHashMap<>();
    static {
        TAB_TO_PREFIX.put("admin", "/api/admin");
        TAB_TO_PREFIX.put("apikeys", "/api/apikeys");
        TAB_TO_PREFIX.put("backups", "/api/backup");
        TAB_TO_PREFIX.put("cloud", "/api/cloud");
        TAB_TO_PREFIX.put("commands", "/api/commands");
        TAB_TO_PREFIX.put("discord", "/api/discord");
        TAB_TO_PREFIX.put("economy", "/api/economy");
        TAB_TO_PREFIX.put("files", "/api/files");
        TAB_TO_PREFIX.put("game", "/api/game");
        TAB_TO_PREFIX.put("holograms", "/api/holograms");
        TAB_TO_PREFIX.put("kits", "/api/kits");
        TAB_TO_PREFIX.put("leaderboard", "/api/leaderboard");
        TAB_TO_PREFIX.put("logging", "/api/logging");
        TAB_TO_PREFIX.put("moderation", "/api/moderation");
        TAB_TO_PREFIX.put("motd", "/api/motd");
        TAB_TO_PREFIX.put("permissions", "/api/permissions");
        TAB_TO_PREFIX.put("placeholders", "/api/placeholders");
        TAB_TO_PREFIX.put("players", "/api/player");
        TAB_TO_PREFIX.put("rules", "/api/rules");
        TAB_TO_PREFIX.put("scoreboard", "/api/scoreboard");
        TAB_TO_PREFIX.put("server", "/api/server");
        TAB_TO_PREFIX.put("shops", "/api/shops");
        TAB_TO_PREFIX.put("stats", "/api/stats");
        TAB_TO_PREFIX.put("teleport", "/api/teleport");
        TAB_TO_PREFIX.put("users", "/api/users");
        TAB_TO_PREFIX.put("warps", "/api/warps");
    }

    /** @return the tab id whose prefix the given request path falls under, or null if none
     *          (e.g. {@code /api/auth/*}, which every role can always reach regardless of tabs). */
    public static String tabForPath(String path) {
        for (Map.Entry<String, String> entry : TAB_TO_PREFIX.entrySet()) {
            String prefix = entry.getValue();
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** @return every known tab id, for building a "what could I possibly be granted" list. */
    public static Iterable<String> allTabIds() {
        return TAB_TO_PREFIX.keySet();
    }
}
