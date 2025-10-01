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
     * Check if sender is muted or ignored by target.
     * Integrates with MuteManager (ignore logic can be added similarly).
     */
    public static boolean isMutedOrIgnored(ServerPlayer sender, ServerPlayer target) {
        return com.zerog.neoessentials.chat.MuteManager.isMuted(sender)
            || com.zerog.neoessentials.chat.IgnoreManager.isIgnoring(target, sender);
    }

    /**
     * Broadcast a message to all players with SocialSpy enabled.
     * Integrates with SocialSpyManager (stub).
     */
    public static void broadcastSocialSpy(ServerPlayer sender, ServerPlayer target, String message) {
        com.zerog.neoessentials.chat.SocialSpyManager.broadcast(sender, target, message);
    }
}
