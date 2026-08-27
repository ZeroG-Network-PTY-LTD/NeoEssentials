package com.zerog.neoessentials.sidebar;

import com.zerog.neoessentials.api.PlaceholderAPI;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.LevelCompat;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Tiny comparison grammar for scoreboard board/line conditions — deliberately not a
 * scripting engine (see the scoreboard feature plan's explicit scope cut). Supported forms,
 * matched in order:
 * <ul>
 *   <li>{@code perm:some.node} — true if the player has that permission</li>
 *   <li>{@code world:name} — true if the player's current dimension path equals {@code name}</li>
 *   <li>{@code {placeholder} == value} / {@code != value} — placeholder resolved via
 *       {@link PlaceholderAPI}, compared case-insensitively</li>
 *   <li>anything else — placeholder-resolved and treated as a boolean ("true"/non-empty = true)</li>
 * </ul>
 * A board/line's condition LIST is implicit AND — every entry must pass.
 */
public final class ConditionEvaluator {
    private ConditionEvaluator() {}

    public static boolean evaluateAll(List<String> conditions, ServerPlayer player) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (String condition : conditions) {
            if (!evaluate(condition, player)) return false;
        }
        return true;
    }

    public static boolean evaluate(String condition, ServerPlayer player) {
        if (condition == null || condition.isBlank()) return true;
        String c = condition.trim();

        if (c.startsWith("perm:")) {
            return PermissionAPI.hasPermission(player.getUUID(), c.substring("perm:".length()).trim());
        }
        if (c.startsWith("world:")) {
            String world = LevelCompat.of(player).dimension().identifier().getPath();
            return world.equalsIgnoreCase(c.substring("world:".length()).trim());
        }

        int neIdx = c.indexOf("!=");
        int eqIdx = c.indexOf("==");
        if (neIdx >= 0 && (eqIdx < 0 || neIdx < eqIdx)) {
            String left = resolve(c.substring(0, neIdx).trim(), player);
            String right = resolve(c.substring(neIdx + 2).trim(), player);
            return !left.equalsIgnoreCase(right);
        }
        if (eqIdx >= 0) {
            String left = resolve(c.substring(0, eqIdx).trim(), player);
            String right = resolve(c.substring(eqIdx + 2).trim(), player);
            return left.equalsIgnoreCase(right);
        }

        String resolved = resolve(c, player);
        return !resolved.isEmpty() && !resolved.equalsIgnoreCase("false");
    }

    private static String resolve(String text, ServerPlayer player) {
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            return text;
        }
    }
}
