package com.zerog.neoessentials.api;

import com.zerog.neoessentials.chat.ChatManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central API for chat-related features (mute, ignore, socialspy, ChatManager access).
 */
public class ChatAPI {
    private static ChatManager chatManager;

    /**
     * Set the ChatManager instance (should be called by the mod on startup).
     */
    public static void setChatManager(ChatManager manager) {
        chatManager = manager;
    }

    /**
     * Get the ChatManager instance.
     */
    public static ChatManager getChatManager() {
        return chatManager;
    }


    /**
     * Check if sender is muted or ignored by target, or if target has messages toggled off.
     * Includes op exemption for msgtoggle.
     */
    public static boolean isMutedOrIgnored(ServerPlayer sender, ServerPlayer target) {
        // Check if sender is muted
        if (com.zerog.neoessentials.chat.MuteManager.isMuted(sender)) {
            return true;
        }
        
        // Check if target is ignoring sender
        if (com.zerog.neoessentials.chat.IgnoreManager.isIgnoring(target, sender)) {
            return true;
        }
        
        // Check if target has messages toggled off (with op exemption for sender)
        if (com.zerog.neoessentials.chat.MsgToggleManager.isMsgToggled(target)) {
            // Op players bypass msgtoggle
            if (sender.hasPermissions(4) || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.msgtoggle.bypass")) {
                return false; // Op/bypass permission = can still message
            }
            return true; // Normal player blocked by msgtoggle
        }
        
        return false;
    }

    /**
     * Broadcast a message to all players with SocialSpy enabled.
     * Integrates with SocialSpyManager (stub).
     */
    public static void broadcastSocialSpy(ServerPlayer sender, ServerPlayer target, String message) {
        com.zerog.neoessentials.chat.SocialSpyManager.broadcast(sender, target, message);
    }
}
