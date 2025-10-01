package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages SocialSpy feature for moderators/admins.
 */
public class SocialSpyManager {
    private static final Set<String> socialSpyPlayers = new HashSet<>();

    public static void toggleSocialSpy(ServerPlayer player) {
        String name = player.getName().getString().toLowerCase();
        if (socialSpyPlayers.contains(name)) {
            socialSpyPlayers.remove(name);
        } else {
            socialSpyPlayers.add(name);
        }
    }

    public static boolean hasSocialSpy(ServerPlayer player) {
        return socialSpyPlayers.contains(player.getName().getString().toLowerCase());
    }

    public static void broadcast(ServerPlayer sender, ServerPlayer target, String message) {
        // Broadcasts the private message to all players with SocialSpy enabled
        for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
            if (hasSocialSpy(player) && !player.equals(sender) && !player.equals(target)) {
                player.sendSystemMessage(Component.translatable(
                    "neoessentials.socialspy.format", sender.getName(), target.getName(), message));
            }
        }
    }
}
