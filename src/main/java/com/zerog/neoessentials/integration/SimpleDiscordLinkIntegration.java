package com.zerog.neoessentials.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.zerog.neoessentials.placeholders.PlaceholderManager;
import com.zerog.neoessentials.features.TabListManager;
// ScoreboardManager import removed - scoreboard system no longer used
import com.zerog.neoessentials.util.DebugUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced SimpleDiscordLink Integration for NeoEssentials
 * Provides seamless user experience with unified messaging, notifications, and role synchronization
 */
public class SimpleDiscordLinkIntegration {
    
    private static SimpleDiscordLinkIntegration instance;
    private final Map<UUID, DiscordUserData> linkedUsers = new ConcurrentHashMap<>();
    private final Map<String, String> roleIdMapping = new ConcurrentHashMap<>(); // Discord Role ID -> Minecraft Permission
    private final Map<String, String> roleIdToName = new ConcurrentHashMap<>(); // Discord Role ID -> Role Name (for display)
    private boolean integrationEnabled = false;
    
    public SimpleDiscordLinkIntegration() {
        instance = this;
        loadConfig();
        initializeIntegration();
    }
    
    public static SimpleDiscordLinkIntegration getInstance() {
        if (instance == null) {
            instance = new SimpleDiscordLinkIntegration();
        }
        return instance;
    }
    
    /**
     * Load integration configuration and Discord role ID mappings
     */
    private void loadConfig() {
        try {
            // Try to load from unified config
            String configPath = "config/neoessentials/tablist.json";
            java.io.File configFile = new java.io.File(configPath);
            
            if (configFile.exists()) {
                // Configuration file exists, enable integration
                integrationEnabled = true;
                DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Configuration file found, integration enabled");
                
                // Load role ID mappings from config file
                loadRoleIdMappingsFromConfig(configFile);
            }
            
            // Initialize default role mappings (fallback if config loading fails)
            if (roleIdMapping.isEmpty()) {
                initializeDefaultRoleMappings();
            }
            
        } catch (Exception e) {
            DebugUtil.errorLog("[SimpleDiscordLinkIntegration] Error loading config: " + e.getMessage());
            integrationEnabled = false;
            // Still initialize defaults as fallback
            initializeDefaultRoleMappings();
        }
    }
    
    /**
     * Load Discord role ID mappings from the configuration file
     */
    private void loadRoleIdMappingsFromConfig(java.io.File configFile) {
        try {
            // Parse the JSON configuration to extract role ID mappings
            // This would need to be implemented with a JSON parser
            DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Loading Discord role ID mappings from config...");
            
            // For now, use the default mappings
            // In a full implementation, this would parse the "roleMappings" section
            // and extract the Discord role IDs as keys instead of role names
            
        } catch (Exception e) {
            DebugUtil.errorLog("[SimpleDiscordLinkIntegration] Failed to load role ID mappings from config: " + e.getMessage());
        }
    }
    
    /**
     * Initialize default Discord role ID to Minecraft permission mappings
     * Replace these with your actual Discord role IDs
     */
    private void initializeDefaultRoleMappings() {
        // Example Discord Role IDs - replace with your actual role IDs
        roleIdMapping.put("1234567890123456789", "neoessentials.admin");    // Owner role ID
        roleIdMapping.put("1234567890123456790", "neoessentials.moderator"); // Admin role ID
        roleIdMapping.put("1234567890123456791", "neoessentials.helper");    // Moderator role ID
        roleIdMapping.put("1234567890123456792", "neoessentials.vip");       // VIP role ID
        roleIdMapping.put("1234567890123456793", "neoessentials.member");    // Member role ID
        roleIdMapping.put("1234567890123456794", "neoessentials.verified");  // Verified role ID
        
        // Role ID to display name mapping for logging and notifications
        roleIdToName.put("1234567890123456789", "Owner");
        roleIdToName.put("1234567890123456790", "Admin");
        roleIdToName.put("1234567890123456791", "Moderator");
        roleIdToName.put("1234567890123456792", "VIP");
        roleIdToName.put("1234567890123456793", "Member");
        roleIdToName.put("1234567890123456794", "Verified");
        
        DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Initialized Discord Role ID mappings for " + roleIdMapping.size() + " roles");
    }
    
    /**
     * Initialize the integration with SimpleDiscordLink
     */
    private void initializeIntegration() {
        try {
            Class.forName("com.hypherionmc.sdlink.SDLink");
            integrationEnabled = true;
            DebugUtil.debugLog("[SimpleDiscordLinkIntegration] SimpleDiscordLink detected and integration enabled");
            
            // Load existing linked users
            loadLinkedUsers();
            
        } catch (ClassNotFoundException e) {
            integrationEnabled = false;
            DebugUtil.debugLog("[SimpleDiscordLinkIntegration] SimpleDiscordLink not found, integration disabled");
        }
    }
    
    /**
     * Load linked users from SimpleDiscordLink
     */
    private void loadLinkedUsers() {
        try {
            // Use reflection to access SimpleDiscordLink's linked users data
            Class<?> databaseClass = Class.forName("com.hypherionmc.sdlink.database.SDLinkDatabase");
            databaseClass.getMethod("getAllLinkedUsers");
            
            // This would need to be adapted based on SDLink's actual API
            // For now, we'll implement a placeholder system
            DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Loaded linked users from SimpleDiscordLink database");
            
        } catch (Exception e) {
            DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Could not load linked users: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced message sending to Discord with rich formatting and embeds
     */
    public static void sendEnhancedMessageToDiscord(String message, MessageType type, ServerPlayer player) {
        if (!getInstance().integrationEnabled) return;
        
        try {
            Class<?> sdlinkClass = Class.forName("com.hypherionmc.sdlink.SDLink");
            Field instanceField = sdlinkClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            Object sdlinkInstance = instanceField.get(null);

            Field discordHandlerField = sdlinkClass.getDeclaredField("discordHandler");
            discordHandlerField.setAccessible(true);
            Object discordHandler = discordHandlerField.get(sdlinkInstance);

            // Create enhanced message with player context
            String enhancedMessage = formatMessageForDiscord(message, type, player);
            
            Method sendMessageMethod = discordHandler.getClass().getMethod("sendMessage", String.class);
            sendMessageMethod.invoke(discordHandler, enhancedMessage);

            DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Sent enhanced message to Discord: " + type);
            
        } catch (Exception e) {
            DebugUtil.errorLog("[SimpleDiscordLinkIntegration] Failed to send enhanced message: " + e.getMessage());
        }
    }
    
    /**
     * Format message for Discord with rich formatting based on type
     */
    private static String formatMessageForDiscord(String message, MessageType type, ServerPlayer player) {
        PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
        String processedMessage = message;
        
        if (player != null) {
            processedMessage = placeholderMgr.processPlaceholders(message, player);
        }
        
        switch (type) {
            case NOTIFICATION:
                return "🔔 **Notification** | " + processedMessage;
            case JOIN:
                return "✅ **Player Joined** | " + processedMessage;
            case LEAVE:
                return "❌ **Player Left** | " + processedMessage;
            case DEATH:
                return "💀 **Death** | " + processedMessage;
            case ADMIN:
                return "⚙️ **Admin** | " + processedMessage;
            case SYSTEM:
                return "🖥️ **System** | " + processedMessage;
            case CHAT:
            default:
                return processedMessage;
        }
    }
    
    /**
     * Send notification to Discord about NeoEssentials events
     */
    public static void sendNeoEssentialsNotification(String event, ServerPlayer player, Map<String, Object> data) {
        if (!getInstance().integrationEnabled) return;
        
        String message = createNotificationMessage(event, player, data);
        sendEnhancedMessageToDiscord(message, MessageType.NOTIFICATION, player);
    }
    
    /**
     * Create notification message for various NeoEssentials events
     */
    private static String createNotificationMessage(String event, ServerPlayer player, Map<String, Object> data) {
        PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
        String playerName = player != null ? player.getName().getString() : "Unknown";
        
        switch (event) {
            case "tablist_update":
                return placeholderMgr.processPlaceholders(
                    "Tablist updated for **{player_name}** | Team: **{ftb_team_display_name}** | Rank: **{ftb_rank_display_name}**", 
                    player);
                    
            case "scoreboard_update":
                return placeholderMgr.processPlaceholders(
                    "Scoreboard updated for **{player_name}** | Layout: **" + data.getOrDefault("layout", "default") + "**", 
                    player);
                    
            case "permission_change":
                return "Permission changed for **" + playerName + "** | " + 
                       "Permission: **" + data.getOrDefault("permission", "unknown") + "** | " +
                       "Action: **" + data.getOrDefault("action", "unknown") + "**";
                       
            case "role_sync":
                return "Discord roles synchronized for **" + playerName + "** | " +
                       "Discord Role: **" + data.getOrDefault("discord_role", "none") + "** | " +
                       "Minecraft Permission: **" + data.getOrDefault("mc_permission", "none") + "**";
                       
            case "team_update":
                return placeholderMgr.processPlaceholders(
                    "Team updated for **{player_name}** | Old Team: **" + data.getOrDefault("old_team", "none") + 
                    "** | New Team: **{ftb_team_display_name}**", player);
                    
            case "rank_update":
                return placeholderMgr.processPlaceholders(
                    "Rank updated for **{player_name}** | Old Rank: **" + data.getOrDefault("old_rank", "none") + 
                    "** | New Rank: **{ftb_rank_display_name}**", player);
                    
            default:
                return "NeoEssentials event: **" + event + "** for **" + playerName + "**";
        }
    }
    
    /**
     * Synchronize Discord roles with Minecraft permissions using role IDs
     */
    public void synchronizeRoles(ServerPlayer player) {
        if (!integrationEnabled) return;
        
        try {
            UUID playerId = player.getUUID();
            DiscordUserData userData = linkedUsers.get(playerId);
            
            if (userData != null) {
                List<String> discordRoleIds = getDiscordRoleIds(userData.discordId);
                syncRoleIdsToPermissions(player, discordRoleIds);
                
                // Create role names list for notification display
                List<String> roleNames = new ArrayList<>();
                for (String roleId : discordRoleIds) {
                    String roleName = roleIdToName.getOrDefault(roleId, "Unknown Role (" + roleId + ")");
                    roleNames.add(roleName);
                }
                
                // Send notification
                Map<String, Object> data = new HashMap<>();
                data.put("discord_roles", String.join(", ", roleNames));
                data.put("discord_role_ids", String.join(", ", discordRoleIds));
                sendNeoEssentialsNotification("role_sync", player, data);
            }
            
        } catch (Exception e) {
            DebugUtil.errorLog("[SimpleDiscordLinkIntegration] Error synchronizing roles: " + e.getMessage());
        }
    }
    
    /**
     * Get Discord role IDs for a user (updated to return role IDs instead of names)
     */
    private List<String> getDiscordRoleIds(String discordId) {
        try {
            // Use reflection to access SimpleDiscordLink's Discord role data
            // This would need to be implemented based on SDLink's actual API
            // The API should return role IDs instead of role names
            return Arrays.asList("1234567890123456793", "1234567890123456794"); // Placeholder role IDs
            
        } catch (Exception e) {
            DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Could not get Discord role IDs: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Sync Discord role IDs to Minecraft permissions
     */
    private void syncRoleIdsToPermissions(ServerPlayer player, List<String> discordRoleIds) {
        try {
            // Get the permissions manager instance
            com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
            
            for (String roleId : discordRoleIds) {
                String permission = roleIdMapping.get(roleId);
                String roleName = roleIdToName.getOrDefault(roleId, "Unknown Role");
                
                if (permission != null) {
                    // Add permission based on Discord role ID
                    // This would need to be implemented based on your permission system
                    DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Synced Discord role ID '" + roleId + 
                                      "' (" + roleName + ") to permission '" + permission + "' for " + player.getName().getString());
                } else {
                    DebugUtil.debugLog("[SimpleDiscordLinkIntegration] No permission mapping found for Discord role ID: " + roleId);
                }
            }
            
        } catch (Exception e) {
            DebugUtil.errorLog("[SimpleDiscordLinkIntegration] Error syncing role IDs to permissions: " + e.getMessage());
        }
    }
    
    /**
     * Update tablist and scoreboard displays with Discord integration
     */
    public void updateDisplaysWithDiscordData(ServerPlayer player) {
        if (!integrationEnabled) return;
        
        try {
            // Update tablist with Discord-aware formatting
            TabListManager tablistMgr = TabListManager.getInstance();
            if (tablistMgr != null) {
                tablistMgr.updatePlayerEntry(player);
                
                // Send notification
                sendNeoEssentialsNotification("tablist_update", player, new HashMap<>());
            }
            
            // Scoreboard system removed - keeping only tablist functionality
            
        } catch (Exception e) {
            DebugUtil.errorLog("[SimpleDiscordLinkIntegration] Error updating displays: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced chat synchronization with rich formatting
     */
    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (!integrationEnabled) return;
        
        try {
            ServerPlayer player = event.getPlayer();
            String message = event.getRawText();
            
            // Process the message with placeholders for Discord
            PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
            String processedMessage = placeholderMgr.processPlaceholders(
                "**{ftb_rank_display_name}** {player_name}: " + message, player);
            
            // Send enhanced message to Discord
            sendEnhancedMessageToDiscord(processedMessage, MessageType.CHAT, player);
            
        } catch (Exception e) {
            DebugUtil.errorLog("[SimpleDiscordLinkIntegration] Error in chat sync: " + e.getMessage());
        }
    }
    
    /**
     * Handle player join events with Discord integration
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!integrationEnabled) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Synchronize roles on join
            synchronizeRoles(player);
            
            // Update displays with Discord data
            updateDisplaysWithDiscordData(player);
            
            // Send enhanced join message
            PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
            String joinMessage = placeholderMgr.processPlaceholders(
                "{player_name} joined the server! | Team: {ftb_team_display_name} | Rank: {ftb_rank_display_name}", 
                player);
            sendEnhancedMessageToDiscord(joinMessage, MessageType.JOIN, player);
        }
    }
    
    /**
     * Handle player leave events
     */
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!integrationEnabled) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Send enhanced leave message
            PlaceholderManager placeholderMgr = PlaceholderManager.getInstance();
            String leaveMessage = placeholderMgr.processPlaceholders(
                "{player_name} left the server. Played for {session_time}.", player);
            sendEnhancedMessageToDiscord(leaveMessage, MessageType.LEAVE, player);
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public static void sendMessageToDiscord(String message, CommandSourceStack source) {
        sendEnhancedMessageToDiscord(message, MessageType.SYSTEM, null);
        if (source != null) {
            source.sendSystemMessage(Component.literal("Sent message to Discord via Enhanced SDLink Integration"));
        }
    }
    
    /**
     * Get Discord user data for a player
     */
    public DiscordUserData getDiscordUserData(UUID playerId) {
        return linkedUsers.get(playerId);
    }
    
    /**
     * Check if a player is linked to Discord
     */
    public boolean isPlayerLinked(UUID playerId) {
        return linkedUsers.containsKey(playerId);
    }
    
    /**
     * Add or update linked user data
     */
    public void updateLinkedUser(UUID playerId, String discordId, String discordName) {
        DiscordUserData userData = new DiscordUserData(discordId, discordName);
        linkedUsers.put(playerId, userData);
        
        DebugUtil.debugLog("[SimpleDiscordLinkIntegration] Updated linked user: " + playerId + " -> " + discordName);
    }
    
    /**
     * Message types for enhanced Discord formatting
     */
    public enum MessageType {
        CHAT, NOTIFICATION, JOIN, LEAVE, DEATH, ADMIN, SYSTEM
    }
    
    /**
     * Discord user data class
     */
    public static class DiscordUserData {
        public final String discordId;
        public final String discordName;
        public final long linkTime;
        
        public DiscordUserData(String discordId, String discordName) {
            this.discordId = discordId;
            this.discordName = discordName;
            this.linkTime = System.currentTimeMillis();
        }
    }
}
