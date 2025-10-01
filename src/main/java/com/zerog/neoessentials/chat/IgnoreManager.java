package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages ignore lists for players.
 */
public class IgnoreManager {
    private static final Map<String, Set<String>> ignoreMap = new HashMap<>();

    public static void ignore(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        ignoreMap.computeIfAbsent(playerName, k -> new HashSet<>()).add(targetName.toLowerCase());
    }

    public static void unignore(ServerPlayer player, String targetName) {
        String playerName = player.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        if (ignored != null) {
            ignored.remove(targetName.toLowerCase());
            if (ignored.isEmpty()) {
                ignoreMap.remove(playerName);
            }
        }
    }

    public static boolean isIgnoring(ServerPlayer player, ServerPlayer target) {
        String playerName = player.getName().getString().toLowerCase();
        String targetName = target.getName().getString().toLowerCase();
        Set<String> ignored = ignoreMap.get(playerName);
        return ignored != null && ignored.contains(targetName);
    }
}
