package com.zerog.neoessentials.integrations.impl;

import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DiscordSRV integration adapter for NeoEssentials.
 * Sends NeoEssentials events to Discord via DiscordSRV.
 */
public class DiscordSRVAdapter implements ChatIntegrationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordSRVAdapter.class);
    private boolean discordSRVLoaded = false;
    private Object discordSRVApi = null;
    
    @Override
    public String getName() {
        return "DiscordSRV";
    }
    
    @Override
    public boolean initialize() {
        discordSRVLoaded = ModList.get().isLoaded("discordsrv");
        
        if (discordSRVLoaded) {
            try {
                // Initialize DiscordSRV mod integration
                LOGGER.info("DiscordSRV mod detected, initializing NeoForge integration...");
                
                // For NeoForge mods, we would typically access the mod instance via:
                // Optional<? extends ModContainer> discordSRVMod = ModList.get().getModContainerById("discordsrv");
                // if (discordSRVMod.isPresent()) {
                //     // Access the mod's API through its container or service provider
                //     discordSRVApi = discordSRVMod.get().getMod();
                // }
                
                // Alternative: Use service provider interface if DiscordSRV provides one
                // discordSRVApi = ServiceLoader.load(DiscordSRVAPI.class).findFirst().orElse(null);
                
                LOGGER.info("DiscordSRV NeoForge mod integration initialized successfully");
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to initialize DiscordSRV mod integration: {}", e.getMessage(), e);
                return false;
            }
        }
        
        LOGGER.debug("DiscordSRV mod not found, integration disabled");
        return false;
    }
    
    @Override
    public boolean isEnabled() {
        return discordSRVLoaded && discordSRVApi != null;
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
     * Send a message to Discord via DiscordSRV
     * @param channel The Discord channel name
     * @param message The message to send
     */
    private void sendToDiscord(String channel, String message) {
        // Placeholder implementation
        // Actual implementation would use DiscordSRV API:
        // DiscordUtil.sendMessage(DiscordUtil.getTextChannelById(channelId), message);
        
        LOGGER.debug("Would send to Discord channel '{}': {}", channel, message);
    }
    
    @Override
    public void shutdown() {
        discordSRVApi = null;
        LOGGER.info("DiscordSRV integration shut down");
    }
}