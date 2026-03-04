package com.zerog.neoessentials.integrations.impl;

import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * DCIntegration (Discord Integration) adapter for NeoEssentials.
 * Sends NeoEssentials events to Discord via DCIntegration mod by ErdbeerbaerLP.
 * Uses reflection to avoid a hard compile-time dependency on DCIntegration.
 */
public class DCIntegrationAdapter implements ChatIntegrationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DCIntegrationAdapter.class);

    private boolean dcIntegrationLoaded = false;

    // Reflected DCIntegration messaging API
    private Method sendMessageMethod = null;
    private Object messagingInstance = null; // null = static call

    @Override
    public String getName() {
        return "DCIntegration";
    }

    @Override
    public boolean initialize() {
        dcIntegrationLoaded = ModList.get().isLoaded("dcintegration");

        if (!dcIntegrationLoaded) {
            LOGGER.debug("DCIntegration mod not found, integration disabled");
            return false;
        }

        try {
            LOGGER.info("DCIntegration mod detected, initializing messaging integration...");

            // DCIntegration exposes DiscordIntegration.instance.sendMessage(String channel, String message)
            if (tryInitDiscordIntegration()) {
                LOGGER.info("DCIntegration DiscordIntegration messaging API initialised");
                return true;
            }

            // Fallback: static helper DiscordUtil.sendMessage(TextChannel, String)
            // For channel-name-based sending try the channelHandler path
            if (tryInitChannelHandler()) {
                LOGGER.info("DCIntegration ChannelHandler messaging API initialised");
                return true;
            }

            LOGGER.warn("DCIntegration detected but no supported messaging API found. " +
                        "Events will be logged only. Ensure DCIntegration is up to date.");
            dcIntegrationLoaded = true;
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to initialize DCIntegration integration: {}", e.getMessage(), e);
            return false;
        }
    }

    /** Try DiscordIntegration.instance.sendMessage(String, String) */
    private boolean tryInitDiscordIntegration() {
        try {
            Class<?> diClass =
                Class.forName("de.erdbeerbaerlp.dcintegration.common.DiscordIntegration");
            Object instance = diClass.getField("instance").get(null);
            Method method = diClass.getMethod("sendMessage", String.class, String.class);
            messagingInstance = instance;
            sendMessageMethod = method;
            return true;
        } catch (Exception e) {
            LOGGER.debug("DiscordIntegration.sendMessage not found: {}", e.getMessage());
            return false;
        }
    }

    /** Try DiscordUtil.sendMessage(TextChannel ch, String message) with channel lookup */
    private boolean tryInitChannelHandler() {
        try {
            // DCIntegration provides a static sendToChannel(String channelId, String message)
            Class<?> utilClass =
                Class.forName("de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage");
            Method method = utilClass.getMethod("sendToChannel", String.class, String.class);
            messagingInstance = null;
            sendMessageMethod = method;
            return true;
        } catch (Exception e) {
            LOGGER.debug("DiscordMessage.sendToChannel not found: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return dcIntegrationLoaded;
    }
    
    @Override
    public void onPlayerChat(ServerPlayer player, String channel, String message, String formattedMessage, String discordChannelId) {
        if (!isEnabled()) return;

        try {
            String emoji = getChannelEmoji(channel);
            // Strip Minecraft formatting codes for Discord
            String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
            String discordMessage = String.format("%s **[%s]** %s: %s",
                emoji, channel.toUpperCase(), player.getName().getString(), cleanMessage);

            // Determine which Discord channel to use
            String targetChannel;
            if (discordChannelId != null && !discordChannelId.isEmpty()) {
                // Use configured Discord channel ID
                targetChannel = discordChannelId;
            } else {
                // Fallback to default channel names based on channel type
                targetChannel = switch (channel.toLowerCase()) {
                    case "staff" -> "staff"; // Staff channel goes to staff/moderation Discord channel
                    case "global" -> "chat"; // Global goes to main chat
                    case "local" -> "chat";  // Local (if enabled) goes to main chat
                    default -> "chat";
                };
            }

            sendToDiscord(targetChannel, discordMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to send chat message to Discord: {}", e.getMessage());
        }
    }

    /**
     * Get emoji for channel type
     */
    private String getChannelEmoji(String channel) {
        return switch (channel.toLowerCase()) {
            case "local" -> "💬";
            case "global" -> "🌍";
            case "staff" -> "🛡️";
            default -> "💭";
        };
    }

    @Override
    public void onPrivateMessage(ServerPlayer sender, ServerPlayer recipient, String message) {
        if (!isEnabled()) return;
        
        try {
            // Send private message notification to Discord
            String discordMessage = String.format("📩 **Private Message** | %s → %s: %s", 
                sender.getName().getString(), 
                recipient.getName().getString(), 
                message);
            
            sendToDiscord("private-messages", discordMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to send private message to Discord: {}", e.getMessage());
        }
    }
    
    @Override
    public void onPlayerMute(ServerPlayer player, String reason, boolean isMuted) {
        if (!isEnabled()) return;
        
        try {
            String action = isMuted ? "muted" : "unmuted";
            String emoji = isMuted ? "🔇" : "🔊";
            String discordMessage = String.format("%s **%s** has been %s%s", 
                emoji,
                player.getName().getString(), 
                action,
                reason != null && !reason.isEmpty() ? " (Reason: " + reason + ")" : "");
            
            sendToDiscord("moderation", discordMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to send mute event to Discord: {}", e.getMessage());
        }
    }
    
    @Override
    public void onAfkStatusChange(ServerPlayer player, boolean isAfk, String reason) {
        if (!isEnabled()) return;
        
        try {
            String status = isAfk ? "is now AFK" : "is no longer AFK";
            String emoji = isAfk ? "💤" : "✅";
            String discordMessage = String.format("%s **%s** %s%s", 
                emoji,
                player.getName().getString(), 
                status,
                (isAfk && reason != null && !reason.isEmpty()) ? " (" + reason + ")" : "");
            
            sendToDiscord("chat", discordMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to send AFK event to Discord: {}", e.getMessage());
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled()) return;
        
        try {
            String discordMessage = String.format("➡️ **%s** joined the server", 
                player.getName().getString());
            
            sendToDiscord("chat", discordMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to send join event to Discord: {}", e.getMessage());
        }
    }
    
    @Override
    public void onPlayerQuit(ServerPlayer player) {
        if (!isEnabled()) return;
        
        try {
            String discordMessage = String.format("⬅️ **%s** left the server", 
                player.getName().getString());
            
            sendToDiscord("chat", discordMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to send quit event to Discord: {}", e.getMessage());
        }
    }
    
    /**
     * Send a message to Discord via DCIntegration using the reflected API.
     * Falls back to info logging if the API is not available.
     */
    private void sendToDiscord(String channel, String message) {
        if (sendMessageMethod != null) {
            try {
                sendMessageMethod.invoke(messagingInstance, channel, message);
                LOGGER.debug("Sent to Discord channel '{}' via DCIntegration: {}", channel, message);
            } catch (Exception e) {
                LOGGER.warn("DCIntegration sendMessage failed for channel '{}': {}", channel, e.getMessage());
            }
        } else {
            LOGGER.info("[Discord->{}] {}", channel, message);
        }
    }
    
    @Override
    public void shutdown() {
        sendMessageMethod = null;
        messagingInstance = null;
        LOGGER.info("DCIntegration integration shut down");
    }
}