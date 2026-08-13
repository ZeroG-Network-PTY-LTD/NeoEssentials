package com.zerog.neoessentials.chat;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChatHandler manages server chat events and applies formatting.
 * <p>
 * This handler intercepts chat messages and applies the configured
 * chat format template before broadcasting to other players.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ChatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHandler.class);
    
    /**
     * Handles server chat events and applies custom formatting.
     * Only applies custom formatting when chat-format is configured,
     * otherwise preserves vanilla playername: message format.
     */
    // Per-player channel state
    private static final java.util.Map<java.util.UUID, String> playerChannelMap = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Set the channel for a specific player
     */
    public static void setPlayerChannel(java.util.UUID playerUUID, String channel) {
        if (channel == null || channel.isEmpty()) {
            playerChannelMap.remove(playerUUID);
        } else {
            playerChannelMap.put(playerUUID, channel);
        }
    }

    /**
     * Get the current channel for a player
     */
    @SuppressWarnings("unused") // May be used by external systems
    public static String getPlayerChannel(java.util.UUID playerUUID) {
        return playerChannelMap.get(playerUUID);
    }

    /**
     * Clear channel for a player (revert to default)
     */
    @SuppressWarnings("unused") // May be used by external systems
    public static void clearPlayerChannel(java.util.UUID playerUUID) {
        playerChannelMap.remove(playerUUID);
    }

    /**
     * Resolves a player's current PERSISTENT channel — their explicitly-set channel state if
     * any, else the configured default channel, else {@code "global"}. Deliberately does NOT
     * consider a per-message prefix override (e.g. typing {@code !hello} to send one message to
     * global without switching state) — that only applies to the message it was typed on, not
     * to "what channel is this player in" as a general, ongoing question. Used by the
     * {@code {neoessentials_channel}}/{@code {channel}} placeholders and anywhere else that
     * needs a player's channel outside the context of an actual chat message.
     */
    public static String getEffectiveChannel(java.util.UUID playerUUID) {
        String channel = playerChannelMap.get(playerUUID);
        if (channel != null) return channel;

        try {
            com.google.gson.JsonObject mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance()
                .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            com.google.gson.JsonObject chatConfig = mainConfig.has("chat") ? mainConfig.getAsJsonObject("chat") : null;
            com.google.gson.JsonObject channelsConfig = (chatConfig != null && chatConfig.has("channels"))
                ? chatConfig.getAsJsonObject("channels") : null;
            boolean channelsEnabled = channelsConfig == null
                || !channelsConfig.has("enabled") || channelsConfig.get("enabled").getAsBoolean();

            if (channelsConfig != null && channelsEnabled) {
                for (String ch : channelsConfig.keySet()) {
                    if (ch.equals("enabled") || ch.endsWith("-description")) continue;
                    com.google.gson.JsonObject chObj = channelsConfig.getAsJsonObject(ch);
                    if (chObj.has("enabled") && chObj.get("enabled").getAsBoolean()
                        && chObj.has("default") && chObj.get("default").getAsBoolean()) {
                        return ch;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "global";
    }

    /**
     * Resolves the styled text to show for a channel via {@code {channel}}/
     * {@code {neoessentials_channel}} — {@code chat.channels.<channelKey>.displayName} if set,
     * else the raw channel key itself (the previous, only behavior).
     *
     * <p>The channel key itself (e.g. {@code "local"}) MUST stay a plain, simple string — it
     * doubles as the internal identifier used for prefix routing, the {@code chat.channels.*.discord}
     * lookup, and — when no {@code command} field is set — the actual registered slash-command
     * literal (see {@code ChannelCommands#register}, which registers every channel's command in
     * one shared try/catch; an invalid literal there throws and silently aborts registration for
     * every channel processed afterward, not just the broken one). {@code displayName} exists so
     * a channel can still show up as something like {@code "&d⚙ Custom"} in chat without putting
     * that text in the key.
     */
    public static String getChannelDisplayName(String channelKey) {
        if (channelKey == null) return "";
        try {
            com.google.gson.JsonObject mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance()
                .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            com.google.gson.JsonObject chatConfig = mainConfig.has("chat") ? mainConfig.getAsJsonObject("chat") : null;
            com.google.gson.JsonObject channelsConfig = (chatConfig != null && chatConfig.has("channels"))
                ? chatConfig.getAsJsonObject("channels") : null;
            if (channelsConfig != null && channelsConfig.has(channelKey)) {
                com.google.gson.JsonObject chObj = channelsConfig.getAsJsonObject(channelKey);
                if (chObj.has("displayName")) {
                    String displayName = chObj.get("displayName").getAsString();
                    if (!displayName.isEmpty()) return displayName;
                }
            }
        } catch (Exception ignored) {}
        return channelKey;
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            ServerPlayer player = event.getPlayer();
            String rawMessage = event.getRawText();
            String playerName = player.getName().getString();

            // Check if player is muted
            boolean isMuted = MuteManager.isMuted(player);
            NeoLog.debug(LOGGER, LogCategory.CHAT, "ChatHandler - Checking mute for {}, result: {}", playerName, isMuted);
            if (isMuted) {
                event.setCanceled(true);
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.chat.muted"));
                return;
            }

            // FreezeManager blocks attack/interact/block-break/place, but chat was never
            // actually checked anywhere — a frozen player could chat freely.
            if (com.zerog.neoessentials.moderation.FreezeManager.getInstance().isPlayerFrozen(player.getUUID())) {
                event.setCanceled(true);
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.freeze.cannot_chat"));
                return;
            }

            // Phase 3: Apply anti-spam filters
            AntiSpamManager.FilterResult filterResult = AntiSpamManager.getInstance().filterMessage(player, rawMessage);
            if (!filterResult.allowed) {
                event.setCanceled(true);
                if (filterResult.denyReason != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(filterResult.denyReason));
                }
                return;
            }

            // Use filtered message (may be modified by caps filter)
            String processedMessage = filterResult.filteredMessage != null ? filterResult.filteredMessage : rawMessage;

            // A genuine, unblocked chat message counts as activity for the AFK system
            AfkManager afkManager = AfkManager.getInstance();
            if (afkManager.isEnableActivityTracking() && afkManager.isTrackChat()) {
                afkManager.updateActivity(player.getUUID());
            }

            // Enforce playerChatPermissions: block chat if player lacks any required permission
            ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
            if (chatManager != null) {
                java.util.Set<String> requiredPerms = chatManager.getPlayerChatPermissions();
                if (requiredPerms != null && !requiredPerms.isEmpty()) {
                    boolean hasAny = false;
                    for (String perm : requiredPerms) {
                        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), perm)) {
                            hasAny = true;
                            break;
                        }
                    }
                    if (!hasAny) {
                        event.setCanceled(true);
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.chat.no_permission"));
                        return;
                    }
                }
            }

            // Enforce muteCommands: block chat messages that start with a muted command
            if (chatManager != null) {
                String trimmed = processedMessage.trim();
                if (trimmed.startsWith("/")) {
                    String[] split = trimmed.substring(1).split(" ", 2);
                    String command = split[0].toLowerCase();
                    if (chatManager.isCommandMuted(command)) {
                        event.setCanceled(true);
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.chat.command_muted", command));
                        return;
                    }
                }
            }

            // Get the ChatManager instance (already retrieved above)
            if (chatManager == null) {
                LOGGER.warn("ChatManager not available, using default chat formatting");
                return; // Let vanilla handle the chat
            }

            // Load channel config from chat config (assume loaded as JsonObject)
            com.google.gson.JsonObject mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            com.google.gson.JsonObject chatConfig = mainConfig.has("chat") ? mainConfig.getAsJsonObject("chat") : new com.google.gson.JsonObject();
            com.google.gson.JsonObject channelsConfig = chatConfig.has("channels") ? chatConfig.getAsJsonObject("channels") : null;

            // Check master switch for channels system
            boolean channelsEnabled = true; // Default to true for backwards compatibility
            if (channelsConfig != null && channelsConfig.has("enabled")) {
                channelsEnabled = channelsConfig.get("enabled").getAsBoolean();
            }

            if (channelsConfig == null || !channelsEnabled) {
                LOGGER.debug("Chat channels system disabled, using global chat");
                channelsConfig = null; // Treat as if no channels configured
            }

            // Detect channel by prefix or player state
            String message = processedMessage;
            String channel = null;
            // Check for explicit channel prefix (e.g. ! for global, @ for staff)
            if (channelsConfig != null) {
                for (String ch : channelsConfig.keySet()) {
                    // Skip metadata fields
                    if (ch.equals("enabled") || ch.endsWith("-description")) continue;

                    com.google.gson.JsonObject chObj = channelsConfig.getAsJsonObject(ch);
                    if (chObj.has("enabled") && !chObj.get("enabled").getAsBoolean()) continue;
                    String prefix = chObj.has("prefix") ? chObj.get("prefix").getAsString() : "";
                    if (!prefix.isEmpty() && message.startsWith(prefix)) {
                        channel = ch;
                        message = message.substring(prefix.length()).stripLeading();
                        break;
                    }
                }
            }
            // If not by prefix, fall back to the player's persistent channel state, then the
            // configured default channel, then "global" — see getEffectiveChannel() for the
            // shared logic (also used by the {neoessentials_channel} placeholder).
            if (channel == null) {
                channel = getEffectiveChannel(player.getUUID());
            }

            // Get group and world for per-group/world chat format
            String group = null;
            try {
                // Goes through PermissionAPI (checks the external adapter, e.g. LuckPerms, first) —
                // this used to go straight to the internal PermissionManager, which never knew about
                // LuckPerms group assignments and silently fell back to the default group/format.
                group = com.zerog.neoessentials.api.permissions.PermissionAPI.getPrimaryGroup(player.getUUID());
            } catch (Exception e) {
                LOGGER.debug("Could not get group for player {}: {}", playerName, e.getMessage());
            }
            String world = null;
            try {
                @SuppressWarnings("resource") // Level is not closeable, warning is false positive
                var level = player.level();
                world = level.dimension().identifier().getPath();
            } catch (Exception e) {
                LOGGER.debug("Could not get world for player {}: {}", playerName, e.getMessage());
            }




            // Only apply custom chat formatting if enabled in config
            if (com.zerog.neoessentials.config.ConfigManager.isChatFormattingEnabled()) {
                // Format priority: per-player override > group+world > group > world > default
                String perPlayerFormat = PlayerChatFormatManager.getInstance().getFormat(player.getUUID());
                String chatFormat = (perPlayerFormat != null) ? perPlayerFormat : chatManager.getChatFormat(group, world);
                // Cancel the original event to apply custom formatting
                event.setCanceled(true);
                // Format the message using our custom formatter
                Component formattedMessage = ChatFormatter.formatMessage(chatFormat, player, message, channel);
                // Route message based on channel
                // Get channel config for dynamic routing
                JsonObject channelObj = null;
                if (channelsConfig != null && channelsConfig.has(channel)) {
                    channelObj = channelsConfig.getAsJsonObject(channel);
                }
                
                // Check if channel has radius (proximity-based)
                boolean hasRadius = channelObj != null && channelObj.has("radius");
                int radius = hasRadius ? channelObj.get("radius").getAsInt() : 0;
                
                // Check if channel has permission requirement
                String requiredPermission = null;
                if (channelObj != null && channelObj.has("permission")) {
                    requiredPermission = channelObj.get("permission").getAsString();
                }

                // Check if channel is team-based (FTB Teams or similar — see TeamManager)
                boolean isTeamBased = channelObj != null && channelObj.has("teamBased")
                        && channelObj.get("teamBased").getAsBoolean();

                var server = player.level().getServer();
                @SuppressWarnings("ConstantConditions") // Defensive null check
                var playerList = server != null ? server.getPlayerList() : null;

                if (playerList != null) {
                    if (isTeamBased) {
                        // Team channel — only reaches players on the sender's team (FTB Teams
                        // or another registered TeamProviderAdapter). Not combined with radius;
                        // "permission" (if set) additionally gates who may receive it.
                        com.zerog.neoessentials.teams.TeamManager teamManager =
                            com.zerog.neoessentials.teams.TeamManager.getInstance();
                        String senderTeamId = teamManager.getTeamId(player.getUUID());

                        if (senderTeamId == null) {
                            String errorKey = teamManager.isAnyProviderAvailable()
                                ? "commands.neoessentials.channel.no_team"
                                : "commands.neoessentials.channel.no_team_provider";
                            player.sendSystemMessage(MessageUtil.error(errorKey));
                        } else {
                            for (ServerPlayer target : playerList.getPlayers()) {
                                if (requiredPermission != null && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(target.getUUID(), requiredPermission)) {
                                    continue;
                                }
                                if (senderTeamId.equals(teamManager.getTeamId(target.getUUID()))) {
                                    target.sendSystemMessage(formattedMessage);
                                }
                            }
                            if (isConsoleLoggingEnabled()) {
                                LOGGER.info("[{}] (team) <{}> {}", channel, playerName, message);
                            }
                        }
                    } else if (hasRadius) {
                        // Proximity-based channel (local, whisper, shout, etc.)
                        var playerPos = player.position();
                        @SuppressWarnings("resource") // Level is not closeable, warning is false positive
                        var playerLevel = player.level();
                        
                        for (ServerPlayer target : playerList.getPlayers()) {
                            // Check permission first if required
                            if (requiredPermission != null && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(target.getUUID(), requiredPermission)) {
                                continue;
                            }
                            
                            // Check proximity
                            @SuppressWarnings("resource") // Level is not closeable, warning is false positive
                            var targetLevel = target.level();
                            if (targetLevel.dimension().equals(playerLevel.dimension()) && target.position().distanceTo(playerPos) <= radius) {
                                target.sendSystemMessage(formattedMessage);
                            }
                        }
                        // Always log to server console so chat appears in logs
                        if (isConsoleLoggingEnabled()) {
                            LOGGER.info("[{}] (radius:{}) <{}> {}", channel, radius, playerName, message);
                        }
                    } else if (requiredPermission != null) {
                        // Permission-based channel (staff, admin, donor, etc.)
                        for (ServerPlayer target : playerList.getPlayers()) {
                            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(target.getUUID(), requiredPermission)) {
                                target.sendSystemMessage(formattedMessage);
                            }
                        }
                        // Always log to server console
                        if (isConsoleLoggingEnabled()) {
                            LOGGER.info("[{}] <{}> {}", channel, playerName, message);
                        }
                    } else {
                        // Global channel (no radius, no permission)
                        for (ServerPlayer target : playerList.getPlayers()) {
                            target.sendSystemMessage(formattedMessage);
                        }
                        // Always log to server console
                        if (isConsoleLoggingEnabled()) {
                            LOGGER.info("[{}] <{}> {}", channel, playerName, message);
                        }
                    }

                    // Note: chat is already logged above via LOGGER.info().
                    // Do NOT call server.sendSystemMessage(formattedMessage) here — it would
                    // route the formatted Component through vanilla's MinecraftServer logger
                    // producing a duplicate (and potentially unresolved-placeholder) log line.
                }

                // Send message to Discord integration (if available and enabled for this channel)
                try {
                    // Check if Discord relay is enabled for this channel
                    boolean discordEnabled = false;
                    String discordChannelId = null;
                    boolean permissionPassed = true;

                    if (channelObj != null && channelObj.has("discord")) {
                        com.google.gson.JsonObject discordConfig = channelObj.getAsJsonObject("discord");
                        if (discordConfig.has("enabled")) {
                            discordEnabled = discordConfig.get("enabled").getAsBoolean();
                        }
                        if (discordEnabled && discordConfig.has("channelId")) {
                            discordChannelId = discordConfig.get("channelId").getAsString();
                            if (discordChannelId != null && discordChannelId.trim().isEmpty()) {
                                discordChannelId = null; // Treat empty string as null
                            }
                        }
                        // Debug: Log Discord relay config for this channel
                        LOGGER.debug("Channel '{}' Discord relay config: enabled={}, channelId={}", channel, discordEnabled, discordChannelId);
                    } else {
                        LOGGER.debug("Channel '{}' has no Discord relay config.", channel);
                    }

                    // Permission check for Discord relay
                    if (requiredPermission != null && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), requiredPermission)) {
                        permissionPassed = false;
                        LOGGER.debug("Player '{}' does not have required permission '{}' for channel '{}'. Discord relay skipped.", playerName, requiredPermission, channel);
                    }

                    // Only send to Discord if enabled for this channel and permission passed
                    if (discordEnabled && permissionPassed) {
                        if (discordChannelId == null) {
                            LOGGER.debug("Discord relay enabled for channel '{}' but no channelId set. Using fallback logic.", channel);
                        }
                        String formattedMessageText = formattedMessage.getString();
                        LOGGER.debug("Relaying message to Discord: channel='{}', discordChannelId='{}', message='{}'", channel, discordChannelId, formattedMessage.getString());
                        com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastPlayerChat(
                            player, channel, message, formattedMessageText, discordChannelId);
                    } else {
                        if (!discordEnabled) {
                            LOGGER.debug("Discord relay is disabled for channel '{}'. Message will NOT be sent to Discord.", channel);
                        } else if (!permissionPassed) {
                            LOGGER.debug("Discord relay not sent: player '{}' lacks permission '{}' for channel '{}'", playerName, requiredPermission, channel);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to send chat to Discord integration: {}", e.getMessage());
                    LOGGER.debug("Discord integration error detail:", e);
                }
            } // else: do not cancel event, let vanilla formatting happen

        } catch (Exception e) {
            LOGGER.error("Error handling chat event for player {}: {}", 
                event.getPlayer().getName().getString(), e.getMessage(), e);
            // Don't cancel the event on error - let vanilla handle it
        }
    }

    /**
     * Returns whether chat messages should be logged to the server console.
     * Reads chat.logChatToConsole from config, defaults to true.
     */
    private static boolean isConsoleLoggingEnabled() {
        try {
            com.google.gson.JsonObject config = com.zerog.neoessentials.config.ConfigManager.getInstance()
                    .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (config.has("chat")) {
                com.google.gson.JsonObject chat = config.getAsJsonObject("chat");
                if (chat.has("logChatToConsole")) {
                    return chat.get("logChatToConsole").getAsBoolean();
                }
            }
        } catch (Exception ignored) {}
        return true; // Default: always log to console
    }
}

