package com.zerog.neoessentials.permissions;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates optional runtime conditions attached to permission nodes.
 *
 * <p>A condition is a boolean expression stored per permission node on a
 * {@link PermissionUser} or {@link PermissionGroup}.  When a permission would
 * otherwise be <em>granted</em>, the condition is evaluated against the current
 * {@link PermissionContext}; if the condition fails the grant is withheld.
 *
 * <h3>Condition syntax</h3>
 * <pre>
 * atom     ::= "time:day" | "time:night"
 *            | "world:<name>"           e.g. world:overworld
 *            | "gamemode:survival" | "gamemode:creative" | "gamemode:spectator" | "gamemode:adventure"
 *            | "health:above:<0-20>"    e.g. health:above:10
 *            | "health:below:<0-20>"    e.g. health:below:5
 *            | "op:true" | "op:false"
 * compound ::= atom ("AND" atom)*       all atoms must match
 *            | atom ("OR"  atom)*       any atom must match
 * </pre>
 *
 * <p>Examples:
 * <pre>
 * time:day
 * gamemode:survival AND time:day
 * world:overworld OR world:the_nether
 * health:above:10
 * </pre>
 *
 * <p>Conditions are stored per-node in user/group data and evaluated by
 * {@link #evaluate(String, PermissionContext, ServerPlayer)}.
 */
public class PermissionConditionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionConditionManager.class);

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile PermissionConditionManager INSTANCE;

    private PermissionConditionManager() {}

    public static PermissionConditionManager getInstance() {
        if (INSTANCE == null) {
            synchronized (PermissionConditionManager.class) {
                if (INSTANCE == null) INSTANCE = new PermissionConditionManager();
            }
        }
        return INSTANCE;
    }

    // ── Evaluate ──────────────────────────────────────────────────────────────

    /**
     * Evaluate a condition expression.
     *
     * @param condition the raw condition string (e.g. {@code "gamemode:survival AND time:day"})
     * @param ctx       the player's current context (may be {@link PermissionContext#EMPTY})
     * @param player    the live player (used for health/OP checks), or {@code null}
     * @return {@code true} if the condition passes (or expression is blank/null)
     */
    public boolean evaluate(String condition, PermissionContext ctx, ServerPlayer player) {
        if (condition == null || condition.isBlank()) return true;
        String expr = condition.trim();

        // AND takes precedence over OR when both appear — simplistic left-to-right for now
        if (expr.contains(" AND ")) {
            for (String part : expr.split(" AND ")) {
                if (!evaluateAtom(part.trim(), ctx, player)) return false;
            }
            return true;
        }
        if (expr.contains(" OR ")) {
            for (String part : expr.split(" OR ")) {
                if (evaluateAtom(part.trim(), ctx, player)) return true;
            }
            return false;
        }
        return evaluateAtom(expr, ctx, player);
    }

    /**
     * Validate a condition expression string.
     *
     * @param condition the expression to validate
     * @return {@code null} if valid, otherwise a human-readable error message
     */
    public String validate(String condition) {
        if (condition == null || condition.isBlank()) return "Condition cannot be empty.";
        String expr = condition.trim();
        String[] parts;
        if (expr.contains(" AND ")) {
            parts = expr.split(" AND ");
        } else if (expr.contains(" OR ")) {
            parts = expr.split(" OR ");
        } else {
            parts = new String[]{expr};
        }
        for (String part : parts) {
            String err = validateAtom(part.trim());
            if (err != null) return err;
        }
        return null; // valid
    }

    // ── Atom helpers ──────────────────────────────────────────────────────────

    private boolean evaluateAtom(String atom, PermissionContext ctx, ServerPlayer player) {
        if (atom == null || atom.isBlank()) return true;
        atom = atom.toLowerCase().trim();

        // time:day / time:night
        if (atom.equals("time:day"))   return ctx != null && ctx.isDay();
        if (atom.equals("time:night")) return ctx != null && ctx.isNight();

        // world:<name>
        if (atom.startsWith("world:")) {
            String w = atom.substring(6);
            return ctx != null && w.equals(ctx.worldId);
        }

        // gamemode:<mode>
        if (atom.startsWith("gamemode:")) {
            String gm = atom.substring(9);
            return ctx != null && gm.equals(ctx.gamemode);
        }

        // health:above:<value>
        if (atom.startsWith("health:above:")) {
            if (player == null) return true; // can't evaluate, pass through
            try {
                float threshold = Float.parseFloat(atom.substring(13));
                return player.getHealth() > threshold;
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid health condition threshold in '{}'", atom);
                return true;
            }
        }

        // health:below:<value>
        if (atom.startsWith("health:below:")) {
            if (player == null) return true;
            try {
                float threshold = Float.parseFloat(atom.substring(13));
                return player.getHealth() < threshold;
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid health condition threshold in '{}'", atom);
                return true;
            }
        }

        // op:true / op:false
        if (atom.equals("op:true"))  return player != null && player.hasPermissions(2);
        if (atom.equals("op:false")) return player == null || !player.hasPermissions(2);

        LOGGER.warn("Unknown condition atom '{}' — treating as true", atom);
        return true;
    }

    private String validateAtom(String atom) {
        if (atom == null || atom.isBlank()) return "Empty condition atom.";
        String a = atom.toLowerCase().trim();
        if (a.equals("time:day") || a.equals("time:night")) return null;
        if (a.startsWith("world:") && a.length() > 6) return null;
        if (a.startsWith("gamemode:")) {
            String gm = a.substring(9);
            if (gm.equals("survival") || gm.equals("creative")
                    || gm.equals("spectator") || gm.equals("adventure")) return null;
            return "Unknown gamemode '" + gm + "'. Use: survival, creative, spectator, adventure.";
        }
        if (a.startsWith("health:above:") || a.startsWith("health:below:")) {
            String num = a.substring(a.lastIndexOf(':') + 1);
            try { Float.parseFloat(num); return null; }
            catch (NumberFormatException e) { return "Invalid health threshold '" + num + "'."; }
        }
        if (a.equals("op:true") || a.equals("op:false")) return null;
        return "Unknown condition atom '" + atom + "'. See /permissions condition syntax.";
    }

    /** Sorted list of common condition suggestions for tab-completion. */
    public static final java.util.List<String> SUGGESTIONS = java.util.List.of(
        "time:day", "time:night",
        "world:overworld", "world:the_nether", "world:the_end",
        "gamemode:survival", "gamemode:creative", "gamemode:spectator", "gamemode:adventure",
        "health:above:10", "health:below:5",
        "op:true", "op:false"
    );
}

