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
        // Ping placeholder: Use safe fallback if no public getter
        providers.put("ping", (player) -> {
            try {
                // NeoForge/Minecraft API: get ping from connection
                // If no public getter, fallback to 0
                return "0";
            } catch (Exception e) {
                return "0";
            }
        });
        // Group prefix/suffix from permissions
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
        // Score placeholder: Use public getter
        providers.put("score", (player) -> String.valueOf(com.zerog.neoessentials.features.ScoreboardManager.getPlayerScore(player.getUUID())));
        // Animated placeholder integration
        providers.put("animation", (player) -> ""); // Reserved for direct animation calls
        // Example: %anim:server_name% or %anim:rainbow_text%
        providers.put("anim", (player) -> ""); // Alias for animation
    }

    public String parse(ServerPlayer player, String text) {
        String result = text;
        for (Map.Entry<String, PlaceholderProvider> entry : providers.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue().get(player));
        }
        // Animated placeholder parsing: %anim:animation_id%
        result = processAnimatedPlaceholders(result, player);
        return result;
    }

    private String processAnimatedPlaceholders(String text, ServerPlayer player) {
        // Find %anim:animation_id% and replace with AnimationManager frame
        String pattern = "%anim:([a-zA-Z0-9_\\-]+)%";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(text);
        while (matcher.find()) {
            String full = matcher.group(0);
            String animId = matcher.group(1);
            com.zerog.neoessentials.animation.AnimationManager animManager = getAnimationManager();
            if (animManager != null && animManager.isEnabled()) {
                com.zerog.neoessentials.animation.Animation animation = animManager.getAnimation(animId);
                if (animation != null) {
                    String frame = animation.getCurrentFrame();
                    text = text.replace(full, frame);
                }
            }
        }
        return text;
    }

    private com.zerog.neoessentials.animation.AnimationManager getAnimationManager() {
        try {
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            java.io.File configDir;
            if (server != null) {
                java.nio.file.Path configPath = server.getFile("config/neoessentials");
                configDir = configPath.toFile();
            } else {
                configDir = new java.io.File("config/neoessentials");
            }
            return com.zerog.neoessentials.animation.AnimationManager.getInstance(configDir);
        } catch (Exception e) {
            return null;
        }
    }

    public interface PlaceholderProvider {
        String get(ServerPlayer player);
    }
}
