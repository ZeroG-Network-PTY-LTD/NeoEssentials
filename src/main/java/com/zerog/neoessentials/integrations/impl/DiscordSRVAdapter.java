package com.zerog.neoessentials.integrations.impl;

import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * DiscordSRV integration adapter for NeoEssentials.
 * Sends NeoEssentials events to Discord via DiscordSRV.
 * Uses reflection to avoid a hard compile-time dependency on DiscordSRV.
 */
public class DiscordSRVAdapter implements ChatIntegrationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordSRVAdapter.class);

    private boolean discordSRVLoaded = false;

    // Reflected DiscordSRV messaging API
    private Method sendMessageMethod = null;
    private Object messagingInstance = null; // null = static call

    @Override
    public String getName() {
        return "DiscordSRV";
    }

    @Override
    public boolean initialize() {
        discordSRVLoaded = ModList.get().isLoaded("discordsrv");

        if (!discordSRVLoaded) {
            LOGGER.debug("DiscordSRV mod not found, integration disabled");
            return false;
        }

        try {
            LOGGER.info("DiscordSRV mod detected, initializing messaging integration...");

            // DiscordSRV (Adventure / v2) exposes DiscordSRV.get().sendMessage(String channelId, String msg)
            if (tryInitDiscordSRVV2()) {
                LOGGER.info("DiscordSRV v2 messaging API initialised");
                return true;
            }

            // DiscordSRV classic (v1) exposes static DiscordUtil.sendMessage(TextChannel, String)
            // or DiscordSRV.getPlugin().getMainTextChannel().sendMessage(String)
            if (tryInitDiscordSRVClassic()) {
                LOGGER.info("DiscordSRV classic messaging API initialised");
                return true;
            }

            LOGGER.warn("DiscordSRV detected but no supported messaging API found. " +
                        "Events will be logged only. Ensure DiscordSRV is up to date.");
            discordSRVLoaded = true;
            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to initialize DiscordSRV integration: {}", e.getMessage(), e);
            return false;
        }
    }

    /** DiscordSRV v2 (Adventure API): DiscordSRV.get().sendMessage(String, String) */
    private boolean tryInitDiscordSRVV2() {
        try {
            Class<?> dsrvClass = Class.forName("com.discordsrv.api.DiscordSRVApi");
            // DiscordSRV.get() returns the singleton
            Method getMethod = Class.forName("com.discordsrv.api.DiscordSRV").getMethod("get");
            Object instance = getMethod.invoke(null);
            Method method = dsrvClass.getMethod("sendMessage", String.class, String.class);
            messagingInstance = instance;
            sendMessageMethod = method;
            return true;
        } catch (Exception e) {
            LOGGER.debug("DiscordSRV v2 API not found: {}", e.getMessage());
            return false;
        }
    }

    /** DiscordSRV classic (v1): DiscordUtil.sendMessage(TextChannel, String) via getMainTextChannel */
    private boolean tryInitDiscordSRVClassic() {
        try {
            // github.scarsz.discordsrv.DiscordSRV.getPlugin().getMainTextChannel()
            Class<?> pluginClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Method getPlugin = pluginClass.getMethod("getPlugin");
            Object plugin = getPlugin.invoke(null);
            // Use DiscordUtil.sendMessage(channel, message)
            Class<?> utilClass = Class.forName("github.scarsz.discordsrv.util.DiscordUtil");
            // sendMessage(TextChannel channel, String message) — we'll store the util method
            // and pass the main channel on each call; store both plugin + method
            Method method = utilClass.getMethod("sendMessage",
                Class.forName("net.dv8tion.jda.api.entities.TextChannel"), String.class);
            messagingInstance = plugin; // store plugin so we can get main channel at call time
            sendMessageMethod = method;
            return true;
        } catch (Exception e) {
            LOGGER.debug("DiscordSRV classic API not found: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return discordSRVLoaded;
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
     * Send a message to Discord via DiscordSRV using the reflected API.
     * Falls back to info logging if the API is not available.
     */
    private void sendToDiscord(String channel, String message) {
        if (sendMessageMethod != null) {
            try {
                // For classic DiscordSRV the first arg is a TextChannel, not a String.
                // In that case call DiscordUtil.sendMessage(plugin.getMainTextChannel(), message)
                if (messagingInstance != null &&
                    messagingInstance.getClass().getName().contains("DiscordSRV") &&
                    sendMessageMethod.getParameterCount() == 2 &&
                    !sendMessageMethod.getParameterTypes()[0].equals(String.class)) {
                    Method getChannel = messagingInstance.getClass().getMethod("getMainTextChannel");
                    Object textChannel = getChannel.invoke(messagingInstance);
                    if (textChannel != null) {
                        sendMessageMethod.invoke(null, textChannel, message);
                    }
                } else {
                    sendMessageMethod.invoke(messagingInstance, channel, message);
                }
                LOGGER.debug("Sent to Discord channel '{}' via DiscordSRV: {}", channel, message);
            } catch (Exception e) {
                LOGGER.warn("DiscordSRV sendMessage failed for channel '{}': {}", channel, e.getMessage());
            }
        } else {
            LOGGER.info("[Discord->{}] {}", channel, message);
        }
    }
    
    @Override
    public void shutdown() {
        sendMessageMethod = null;
        messagingInstance = null;
        LOGGER.info("DiscordSRV integration shut down");
    }
}