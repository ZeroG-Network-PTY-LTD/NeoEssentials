package com.zerog.neoessentials.integrations.impl;

import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Simple Discord Link (SDLink) integration adapter for NeoEssentials.
 * Sends NeoEssentials events to Discord via Simple Discord Link mod by hypherionsa.
 * Uses reflection to avoid a hard compile-time dependency on SDLink.
 */
public class SDLinkAdapter implements ChatIntegrationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SDLinkAdapter.class);

    private boolean sdLinkLoaded = false;

    // Reflected SDLink messaging API
    private Method sendMessageMethod = null;   // SDLinkMessagingService or BotController.sendMessage
    private Object messagingInstance = null;   // instance to call the method on (null = static)

    @Override
    public String getName() {
        return "Simple Discord Link";
    }

    @Override
    public boolean initialize() {
        sdLinkLoaded = ModList.get().isLoaded("sdlink");

        if (!sdLinkLoaded) {
            LOGGER.debug("Simple Discord Link mod not found, integration disabled");
            return false;
        }

        try {
            LOGGER.info("Simple Discord Link mod detected, initializing messaging integration...");

            // SDLink v3+ exposes a static sendMessage on BotController:
            //   BotController.INSTANCE.sendMessage(String channelNameOrId, String message)
            // Try that first, then fall back to older API shapes.
            if (tryInitBotController()) {
                LOGGER.info("SDLink BotController messaging API initialised successfully");
                return true;
            }

            // Fallback: SDLinkAPI static helper (older versions)
            if (tryInitStaticApi()) {
                LOGGER.info("SDLink static messaging API initialised successfully");
                return true;
            }

            LOGGER.warn("SDLink detected but no supported messaging API found. " +
                        "Messages will be logged only. Ensure SDLink v3.2+ is installed.");
            // Return true — the mod IS loaded; we log events but can't forward to Discord
            sdLinkLoaded = true;
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to initialize Simple Discord Link integration: {}", e.getMessage(), e);
            return false;
        }
    }

    /** Try to get BotController.INSTANCE.sendMessage(String, String) */
    private boolean tryInitBotController() {
        try {
            Class<?> botControllerClass =
                Class.forName("com.hypherionmc.sdlink.core.discord.BotController");
            Object instance = botControllerClass.getField("INSTANCE").get(null);
            Method method = botControllerClass.getMethod("sendMessage", String.class, String.class);
            messagingInstance = instance;
            sendMessageMethod = method;
            return true;
        } catch (Exception e) {
            LOGGER.debug("BotController.sendMessage not found: {}", e.getMessage());
            return false;
        }
    }

    /** Try static SDLinkAPI.sendMessage(String, String) (older/alternative API) */
    private boolean tryInitStaticApi() {
        try {
            Class<?> apiClass = Class.forName("com.hypherionmc.sdlink.api.SDLinkAPI");
            Method method = apiClass.getMethod("sendMessage", String.class, String.class);
            messagingInstance = null; // static
            sendMessageMethod = method;
            return true;
        } catch (Exception e) {
            LOGGER.debug("SDLinkAPI.sendMessage not found: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return sdLinkLoaded; // loaded = we can at least log; messaging uses sendMessageMethod
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
     * Send a message to Discord via Simple Discord Link using the reflected API.
     * Falls back to debug logging if the API is not available.
     */
    private void sendToDiscord(String channel, String message) {
        if (sendMessageMethod != null) {
            try {
                sendMessageMethod.invoke(messagingInstance, channel, message);
                LOGGER.debug("Sent to Discord channel '{}' via SDLink: {}", channel, message);
            } catch (Exception e) {
                LOGGER.warn("SDLink sendMessage failed for channel '{}': {}", channel, e.getMessage());
            }
        } else {
            // API unavailable — log so server admins can see the event
            LOGGER.info("[Discord->{}] {}", channel, message);
        }
    }
    
    @Override
    public void shutdown() {
        sendMessageMethod = null;
        messagingInstance = null;
        LOGGER.info("Simple Discord Link integration shut down");
    }
}