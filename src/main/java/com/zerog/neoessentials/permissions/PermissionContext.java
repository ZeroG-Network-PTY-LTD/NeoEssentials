package com.zerog.neoessentials.permissions;

import net.minecraft.server.level.ServerPlayer;

/**
 * Lightweight value object that captures the relevant runtime context of a
 * permission check: which world the player is in and the current day-time.
 *
 * <p>Pass an instance to {@link PermissionManager#hasPermission(java.util.UUID, String, PermissionContext)}
 * so that contextual permission overrides (world, time) and conditions can be
 * evaluated without an additional player lookup.
 *
 * <p>Use {@link #EMPTY} when no context is available (console, non-player checks).
 */
public final class PermissionContext {

    /** Sentinel — no contextual information available. */
    public static final PermissionContext EMPTY = new PermissionContext(null, -1, null);

    /**
     * Short world dimension path, lower-cased (e.g. {@code "overworld"},
     * {@code "the_nether"}, {@code "the_end"}).
     * {@code null} when no world context is available.
     */
    public final String worldId;

    /**
     * Vanilla day-time (0–23999 ticks), or {@code -1} when unavailable.
     * <ul>
     *   <li>Day:   0 – 12 999</li>
     *   <li>Night: 13 000 – 23 999</li>
     * </ul>
     */
    public final long dayTime;

    /**
     * Gamemode as a lower-case string ({@code "survival"}, {@code "creative"},
     * {@code "spectator"}, {@code "adventure"}), or {@code null} when unavailable.
     */
    public final String gamemode;

    private PermissionContext(String worldId, long dayTime, String gamemode) {
        this.worldId  = worldId;
        this.dayTime  = dayTime;
        this.gamemode = gamemode;
    }

    /**
     * Build a context from a live {@link ServerPlayer}.
     *
     * @param player non-null online player
     * @return populated context object
     */
    public static PermissionContext forPlayer(ServerPlayer player) {
        String world    = player.level().dimension().identifier().getPath().toLowerCase();
        long   time     = com.zerog.neoessentials.util.WorldClockCompat.getTime(player.level()) % 24000L;
        String gamemodeStr = gamemodeStr(player);
        return new PermissionContext(world, time, gamemodeStr);
    }

    /** {@code true} if the day-time falls in the day phase (0 – 12 999 ticks). */
    public boolean isDay() {
        return dayTime >= 0 && dayTime < 13000;
    }

    /** {@code true} if the day-time falls in the night phase (13 000+ ticks). */
    public boolean isNight() {
        return dayTime >= 13000;
    }

    /**
     * Returns {@code true} if this context matches the given context key.
     *
     * <p>Supported keys:
     * <ul>
     *   <li>{@code world:<name>}       — e.g. {@code world:overworld}</li>
     *   <li>{@code time:day}           — day phase</li>
     *   <li>{@code time:night}         — night phase</li>
     *   <li>{@code gamemode:survival}  — player gamemode</li>
     * </ul>
     */
    public boolean matches(String contextKey) {
        if (contextKey == null) return false;
        contextKey = contextKey.toLowerCase().trim();
        if (contextKey.startsWith("world:")) {
            String w = contextKey.substring(6);
            return w.equals(worldId);
        }
        if (contextKey.equals("time:day"))   return isDay();
        if (contextKey.equals("time:night")) return isNight();
        if (contextKey.startsWith("gamemode:")) {
            String gm = contextKey.substring(9);
            return gm.equals(gamemode);
        }
        return false;
    }

    /** Canonical sorted list of valid context key prefixes for tab-completion. */
    public static final java.util.List<String> SUGGESTIONS = java.util.List.of(
        "world:overworld", "world:the_nether", "world:the_end",
        "time:day", "time:night",
        "gamemode:survival", "gamemode:creative", "gamemode:spectator", "gamemode:adventure"
    );

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String gamemodeStr(ServerPlayer player) {
        return switch (player.gameMode.getGameModeForPlayer()) {
            case SURVIVAL   -> "survival";
            case CREATIVE   -> "creative";
            case SPECTATOR  -> "spectator";
            case ADVENTURE  -> "adventure";
        };
    }
}

