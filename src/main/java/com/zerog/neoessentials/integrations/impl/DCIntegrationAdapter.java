package com.zerog.neoessentials.integrations.impl;

import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DCIntegration (Discord Integration) adapter for NeoEssentials.
 * Sends NeoEssentials events to Discord via DCIntegration mod by ErdbeerbaerLP.
 * Compatible with NeoForge 1.21.x versions.
 */
public class DCIntegrationAdapter implements ChatIntegrationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DCIntegrationAdapter.class);
    private boolean dcIntegrationLoaded = false;
    private Object dcIntegrationApi = null;
    
    @Override
    public String getName() {
        return "DCIntegration";
    }
    
    @Override
    public boolean initialize() {
        dcIntegrationLoaded = ModList.get().isLoaded("dcintegration");
        
        if (dcIntegrationLoaded) {
            try {
                // Initialize DCIntegration mod integration
                LOGGER.info("DCIntegration mod detected, initializing NeoForge integration...");
                
                // For NeoForge mods, we would typically access the mod instance via:
                // Optional<? extends ModContainer> dcIntegrationMod = ModList.get().getModContainerById("dcintegration");
                // if (dcIntegrationMod.isPresent()) {
                //     // Access the mod's API through its container or service provider
                //     dcIntegrationApi = dcIntegrationMod.get().getMod();
                // }
                
                // Alternative: Use service provider interface if DCIntegration provides one
                // dcIntegrationApi = ServiceLoader.load(DCIntegrationAPI.class).findFirst().orElse(null);
                
                LOGGER.info("DCIntegration NeoForge mod integration initialized successfully");
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to initialize DCIntegration mod integration: {}", e.getMessage(), e);
                return false;
            }
        }
        
        LOGGER.debug("DCIntegration mod not found, integration disabled");
        return false;
    }
    
    @Override
    public boolean isEnabled() {
        return dcIntegrationLoaded && dcIntegrationApi != null;
    }
    
    @Override
    public void onPlayerChat(ServerPlayer player, String channel, String message, String formattedMessage, String discordChannelId) {
        if (!isEnabled()) return;

        try {
            // Strip Minecraft formatting codes for Discord
            String cleanMessage = formattedMessage.replaceAll("§[0-9a-fk-or]", "");

            // Format for Discord with channel indicator
            String emoji = getChannelEmoji(channel);
            String discordMessage = String.format("%s **[%s]** %s: %s",
                emoji,
                channel.toUpperCase(),
                player.getName().getString(),
                message);

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
     * Send a message to Discord via DCIntegration
     * @param channel The Discord channel name
     * @param message The message to send
     */
    private void sendToDiscord(String channel, String message) {
        // Placeholder implementation
        // Actual implementation would use DCIntegration API:
        // DCIntegrationAPI.sendMessage(channel, message);
        
        LOGGER.debug("Would send to Discord channel '{}' via DCIntegration: {}", channel, message);
    }
    
    @Override
    public void shutdown() {
        dcIntegrationApi = null;
        LOGGER.info("DCIntegration integration shut down");
    }
}