
// DEPRECATED: Use com.zerog.neoessentials.placeholders.PlaceholderManager singleton for all placeholder logic.
// This class is retained only for legacy compatibility and will be removed in future versions.
package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/**
 * Handles placeholder replacements for tablist, scoreboard, bossbar, etc.
 */
public class PlaceholderManager {
    private final Map<String, PlaceholderProvider> providers = new HashMap<>();

    public PlaceholderManager() {
        // Register default placeholders
        providers.put("player", (player) -> player.getName().getString());
        providers.put("ping", (player) -> {
            try {
                java.lang.reflect.Field latencyField = player.connection.getClass().getDeclaredField("latency");
                latencyField.setAccessible(true);
                int ping = latencyField.getInt(player.connection);
                return String.valueOf(ping);
            } catch (Exception e) {
                return "0";
            }
        });
        providers.put("group", (player) -> {
            com.zerog.neoessentials.permissions.CustomPermissionsManager permManager = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
            return permManager.getPlayerGroup(player.getUUID());
        });
        providers.put("prefix", (player) -> {
            com.zerog.neoessentials.permissions.CustomPermissionsManager permManager = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
            return permManager.getPlayerPrefix(player.getUUID());
        });
        providers.put("suffix", (player) -> {
            com.zerog.neoessentials.permissions.CustomPermissionsManager permManager = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
            return permManager.getPlayerSuffix(player.getUUID());
        });
        providers.put("score", (player) -> String.valueOf(com.zerog.neoessentials.features.ScoreboardManager.getPlayerScore(player.getUUID())));
        providers.put("playtime", (player) -> {
            // NeoForge 1.21.1: get playtime in ticks using Stats.CUSTOM and ResourceLocation
            long ticks = player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM, net.minecraft.stats.Stats.PLAY_TIME);
            long seconds = ticks / 20;
            long hours = seconds / 3600;
            return String.valueOf(hours);
        });
        providers.put("world", (player) -> player.level().dimension().location().toString());
        providers.put("afk", (player) -> {
                // Basic AFK detection: always returns "false" unless you implement activity tracking
                // Replace with real logic if you track player activity elsewhere
                return "false";
        });
    // Animation placeholders removed
            // Conditional placeholders are handled in processConditionalPlaceholders()
            providers.put("if", (player) -> ""); // Implement conditional logic in parse()
    }

    public String parse(ServerPlayer player, String text) {
        String result = text;
        for (Map.Entry<String, PlaceholderProvider> entry : providers.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue().get(player));
        }
        // Conditional placeholder parsing: %if:group=admin:Admin:Player%
        result = processConditionalPlaceholders(result, player);
    // Animation system removed
        return result;
    }

    private String processConditionalPlaceholders(String text, ServerPlayer player) {
        // Example: %if:group=admin:Admin:Player%
        String pattern = "%if:([a-zA-Z0-9_]+)=([a-zA-Z0-9_]+):([^%]+):([^%]+)%";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(text);
        while (matcher.find()) {
            String full = matcher.group(0);
            String key = matcher.group(1);
            String value = matcher.group(2);
            String ifTrue = matcher.group(3);
            String ifFalse = matcher.group(4);
            String actual = "";
            if (key.equals("group")) {
                actual = providers.get("group").get(player);
            }
            String replacement = actual.equalsIgnoreCase(value) ? ifTrue : ifFalse;
            text = text.replace(full, replacement);
        }
        return text;
    }

    // Animation system removed

    // Animation system removed

    public interface PlaceholderProvider {
        String get(ServerPlayer player);
    }
}
