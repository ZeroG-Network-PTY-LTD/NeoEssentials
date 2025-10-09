package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.util.MessageUtil;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SocialSpy feature for moderators/admins.
 * Thread-safe implementation with permission checking.
 */
public class SocialSpyManager {
    // Thread-safe Set for concurrent access
    private static final Set<String> socialSpyPlayers = ConcurrentHashMap.newKeySet();

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
        // Check if sender is exempt from socialspy monitoring
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.socialspy.exempt")) {
            return; // Don't broadcast messages from exempt players
        }
        
        // Broadcasts the private message to all players with SocialSpy enabled
        for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
            if (hasSocialSpy(player) && !player.equals(sender) && !player.equals(target)) {
                // Verify the player still has socialspy permission
                if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.chat.socialspy")) {
                    player.sendSystemMessage(MessageUtil.component(
                        "neoessentials.socialspy.format", sender.getName().getString(), target.getName().getString(), message));
                } else {
                    // Remove socialspy if they no longer have permission
                    socialSpyPlayers.remove(player.getName().getString().toLowerCase());
                }
            }
        }
    }
}
