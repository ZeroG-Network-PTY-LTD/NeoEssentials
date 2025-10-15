package com.zerog.neoessentials.webdashboard.security;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provider for Discord authentication via Simple Discord Link integration
 * Uses SDLink's MinecraftAccount API and JDA Member API to retrieve Discord linkage and roles
 */
public class DiscordAuthProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordAuthProvider.class);
    private static DiscordAuthProvider INSTANCE;
    
    private boolean sdLinkAvailable;
    private Class<?> minecraftAccountClass;
    private Class<?> cacheManagerClass;
    private Method fromDiscordIdMethod;
    private Method getDiscordMembersMethod;
    
    private DiscordAuthProvider() {
        initialize();
    }
    
    public static DiscordAuthProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DiscordAuthProvider();
        }
        return INSTANCE;
    }
    
    /**
     * Initialize Simple Discord Link integration via reflection
     * Loads SDLink API classes: MinecraftAccount and CacheManager
     */
    private void initialize() {
        sdLinkAvailable = ModList.get().isLoaded("sdlink");
        
        if (sdLinkAvailable) {
            try {
                LOGGER.info("Simple Discord Link mod detected, initializing integration...");
                
                // Load SDLink API classes
                minecraftAccountClass = Class.forName("com.hypherionmc.sdlink.api.accounts.MinecraftAccount");
                cacheManagerClass = Class.forName("com.hypherionmc.sdlink.core.managers.CacheManager");
                
                // Get methods
                fromDiscordIdMethod = minecraftAccountClass.getMethod("fromDiscordId", String.class);
                getDiscordMembersMethod = cacheManagerClass.getMethod("getDiscordMembers");
                
                LOGGER.info("Discord authentication provider initialized successfully");
                LOGGER.info("SDLink API available: MinecraftAccount, CacheManager");
                
            } catch (ClassNotFoundException e) {
                LOGGER.error("SDLink API classes not found. This version of SDLink may not have the required API: {}", e.getMessage());
                LOGGER.error("Please ensure you're using Simple Discord Link v3.2.1 or newer (with developer API support)");
                sdLinkAvailable = false;
            } catch (NoSuchMethodException e) {
                LOGGER.error("SDLink API methods not found: {}", e.getMessage());
                sdLinkAvailable = false;
            } catch (Exception e) {
                LOGGER.error("Failed to initialize Discord authentication provider: {}", e.getMessage(), e);
                sdLinkAvailable = false;
            }
        } else {
            LOGGER.warn("Simple Discord Link mod not found. Discord authentication will not be available.");
            LOGGER.warn("Install Simple Discord Link (sdlink) mod to enable Discord authentication.");
        }
    }
    
    /**
     * Check if Discord authentication is available
     */
    public boolean isAvailable() {
        return sdLinkAvailable;
    }
    
    /**
     * Get linked Discord account for a Minecraft username
     */
    public DiscordUser getLinkedAccount(String minecraftUsername) {
        if (!sdLinkAvailable) {
            LOGGER.debug("SDLink not available, cannot get linked account for: {}", minecraftUsername);
            return null;
        }
        
        try {
            // Get Minecraft server
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.warn("Server not available, cannot retrieve linked account");
                return null;
            }
            
            // Find player by username
            ServerPlayer player = server.getPlayerList().getPlayerByName(minecraftUsername);
            if (player == null) {
                LOGGER.debug("Player not online: {}", minecraftUsername);
                // Try to get UUID from offline player data
                UUID playerUuid = server.getProfileCache().get(minecraftUsername)
                    .map(profile -> profile.getId())
                    .orElse(null);
                
                if (playerUuid == null) {
                    LOGGER.warn("Cannot find UUID for player: {}", minecraftUsername);
                    return null;
                }
                
                return getLinkedAccountByUuid(playerUuid);
            }
            
            return getLinkedAccountByUuid(player.getUUID());
            
        } catch (Exception e) {
            LOGGER.error("Error getting linked account for {}: {}", minecraftUsername, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get linked Discord account by Minecraft UUID
     * Uses SDLink's MinecraftAccount API to get linked Discord ID,
     * then fetches Discord roles from JDA Member cache
     */
    public DiscordUser getLinkedAccountByUuid(UUID minecraftUuid) {
        if (!sdLinkAvailable) {
            return null;
        }
        
        // Check if Discord bot is ready before making API calls
        if (!isBotReady()) {
            LOGGER.warn("Discord bot is not ready yet, cannot retrieve linked account for UUID: {}", minecraftUuid);
            return null;
        }
        
        try {
            // Get MinecraftAccount from SDLink
            Object minecraftAccount = getMinecraftAccountByUuid(minecraftUuid);
            if (minecraftAccount == null) {
                LOGGER.debug("No linked account found for UUID: {}", minecraftUuid);
                return null;
            }
            
            // Get Discord ID from MinecraftAccount
            Method getStoredAccountMethod = minecraftAccountClass.getMethod("getStoredAccount");
            Object sdLinkAccount = getStoredAccountMethod.invoke(minecraftAccount);
            
            if (sdLinkAccount == null) {
                return null;
            }
            
            Class<?> sdLinkAccountClass = sdLinkAccount.getClass();
            Method getDiscordIDMethod = sdLinkAccountClass.getMethod("getDiscordID");
            Method getInGameNameMethod = sdLinkAccountClass.getMethod("getInGameName");
            Method getUuidMethod = sdLinkAccountClass.getMethod("getUuid");
            
            String discordId = (String) getDiscordIDMethod.invoke(sdLinkAccount);
            String mcUsername = (String) getInGameNameMethod.invoke(sdLinkAccount);
            String uuidStr = (String) getUuidMethod.invoke(sdLinkAccount);
            
            if (discordId == null || discordId.isEmpty()) {
                LOGGER.debug("No Discord ID linked for Minecraft account: {}", mcUsername);
                return null;
            }
            
            // Get Discord roles from JDA Member
            List<String> discordRoles = getDiscordRoles(discordId);
            
            // Get Discord username from cached members
            String discordUsername = getDiscordUsername(discordId);
            
            return new DiscordUser(
                discordId,
                discordUsername != null ? discordUsername : discordId,
                mcUsername,
                uuidStr,
                discordRoles
            );
            
        } catch (Exception e) {
            LOGGER.error("Error getting linked account by UUID {}: {}", minecraftUuid, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get linked Discord account by Discord ID
     * Uses SDLink's MinecraftAccount.fromDiscordId() API
     */
    public DiscordUser getLinkedAccountByDiscordId(String discordId) {
        if (!sdLinkAvailable || discordId == null) {
            return null;
        }
        
        // Check if Discord bot is ready before making API calls
        if (!isBotReady()) {
            LOGGER.warn("Discord bot is not ready yet, cannot retrieve linked account for Discord ID: {}", discordId);
            return null;
        }
        
        try {
            // Use SDLink API: MinecraftAccount.fromDiscordId(discordId)
            Object minecraftAccount = fromDiscordIdMethod.invoke(null, discordId);
            
            if (minecraftAccount == null) {
                LOGGER.debug("No linked Minecraft account found for Discord ID: {}", discordId);
                return null;
            }
            
            // Get account details
            Method getStoredAccountMethod = minecraftAccountClass.getMethod("getStoredAccount");
            Object sdLinkAccount = getStoredAccountMethod.invoke(minecraftAccount);
            
            if (sdLinkAccount == null) {
                return null;
            }
            
            Class<?> sdLinkAccountClass = sdLinkAccount.getClass();
            Method getInGameNameMethod = sdLinkAccountClass.getMethod("getInGameName");
            Method getUuidMethod = sdLinkAccountClass.getMethod("getUuid");
            
            String mcUsername = (String) getInGameNameMethod.invoke(sdLinkAccount);
            String uuidStr = (String) getUuidMethod.invoke(sdLinkAccount);
            
            // Get Discord roles and username
            List<String> discordRoles = getDiscordRoles(discordId);
            String discordUsername = getDiscordUsername(discordId);
            
            return new DiscordUser(
                discordId,
                discordUsername != null ? discordUsername : discordId,
                mcUsername,
                uuidStr,
                discordRoles
            );
            
        } catch (Exception e) {
            LOGGER.error("Error getting linked account by Discord ID {}: {}", discordId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get Discord roles for a user (as role IDs, not names)
     * Fetches from SDLink's cached JDA Member list
     */
    public List<String> getDiscordRoles(String discordId) {
        if (!sdLinkAvailable || discordId == null) {
            return new ArrayList<>();
        }
        
        // Check if Discord bot is ready
        if (!isBotReady()) {
            LOGGER.debug("Discord bot is not ready yet, cannot retrieve roles");
            return new ArrayList<>();
        }
        
        try {
            // Get cached Discord members from SDLink
            Object membersObj = getDiscordMembersMethod.invoke(null);
            
            if (membersObj == null) {
                LOGGER.debug("No cached Discord members available");
                return new ArrayList<>();
            }
            
            // Handle both List and Set return types from SDLink
            java.util.Collection<?> members;
            if (membersObj instanceof java.util.Collection) {
                members = (java.util.Collection<?>) membersObj;
            } else {
                LOGGER.warn("Unexpected return type from getDiscordMembers: {}", membersObj.getClass());
                return new ArrayList<>();
            }
            
            if (members.isEmpty()) {
                LOGGER.debug("No cached Discord members available");
                return new ArrayList<>();
            }
            
            // Find member by Discord ID
            for (Object memberObj : members) {
                Method getIdMethod = memberObj.getClass().getMethod("getId");
                String memberId = (String) getIdMethod.invoke(memberObj);
                
                if (memberId.equals(discordId)) {
                    // Found the member, get their roles
                    Method getRolesMethod = memberObj.getClass().getMethod("getRoles");
                    Object rolesObj = getRolesMethod.invoke(memberObj);
                    
                    // JDA Member.getRoles() returns a List, but it might be wrapped in a collection
                    // Handle both List and Set cases
                    List<String> roleIds = new ArrayList<>();
                    
                    if (rolesObj instanceof java.util.Collection) {
                        java.util.Collection<?> roles = (java.util.Collection<?>) rolesObj;
                        for (Object roleObj : roles) {
                            Method getRoleIdMethod = roleObj.getClass().getMethod("getId");
                            String roleId = (String) getRoleIdMethod.invoke(roleObj);
                            roleIds.add(roleId);
                        }
                    }
                    
                    LOGGER.debug("Found {} roles for Discord ID {}: {}", roleIds.size(), discordId, roleIds);
                    return roleIds;
                }
            }
            
            LOGGER.debug("Discord member {} not found in cache", discordId);
            return new ArrayList<>();
            
        } catch (Exception e) {
            LOGGER.error("Error getting Discord roles for {}: {}", discordId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get Discord username from cached members
     */
    private String getDiscordUsername(String discordId) {
        try {
            List<?> members = (List<?>) getDiscordMembersMethod.invoke(null);
            
            if (members == null || members.isEmpty()) {
                return null;
            }
            
            for (Object memberObj : members) {
                Method getIdMethod = memberObj.getClass().getMethod("getId");
                String memberId = (String) getIdMethod.invoke(memberObj);
                
                if (memberId.equals(discordId)) {
                    Method getEffectiveNameMethod = memberObj.getClass().getMethod("getEffectiveName");
                    return (String) getEffectiveNameMethod.invoke(memberObj);
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Could not get Discord username for {}: {}", discordId, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get MinecraftAccount from SDLink by UUID
     */
    private Object getMinecraftAccountByUuid(UUID minecraftUuid) {
        try {
            // First, try to get the Discord ID from verifiedaccounts.json
            SDLinkDataReader dataReader = new SDLinkDataReader(
                ServerLifecycleHooks.getCurrentServer().getServerDirectory()
            );
            
            String discordId = dataReader.getDiscordId(minecraftUuid);
            if (discordId == null) {
                return null;
            }
            
            // Then use SDLink API to get the full MinecraftAccount
            return fromDiscordIdMethod.invoke(null, discordId);
            
        } catch (Exception e) {
            LOGGER.debug("Could not get MinecraftAccount for UUID {}: {}", minecraftUuid, e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a Minecraft account is linked to Discord
     */
    public boolean isAccountLinked(String minecraftUsername) {
        DiscordUser user = getLinkedAccount(minecraftUsername);
        return user != null && user.isLinked();
    }
    
    /**
     * Check if a Minecraft UUID is linked to Discord
     */
    public boolean isAccountLinkedByUuid(UUID minecraftUuid) {
        DiscordUser user = getLinkedAccountByUuid(minecraftUuid);
        return user != null && user.isLinked();
    }
    
    /**
     * Refresh cached Discord data (if applicable)
     */
    public void refreshCache() {
        if (!sdLinkAvailable) {
            return;
        }
        
        try {
            // Attempt to refresh CacheManager if available
            if (cacheManagerClass != null) {
                try {
                    // Try to call getInstance() method on CacheManager
                    java.lang.reflect.Method getInstanceMethod = cacheManagerClass.getMethod("getInstance");
                    Object cacheManagerInstance = getInstanceMethod.invoke(null);
                    
                    if (cacheManagerInstance != null) {
                        // Try to find and call refresh methods
                        try {
                            java.lang.reflect.Method refreshMethod = cacheManagerClass.getMethod("refreshCache");
                            refreshMethod.invoke(cacheManagerInstance);
                            LOGGER.info("Discord cache refreshed successfully");
                        } catch (NoSuchMethodException e) {
                            // Try alternative method names
                            try {
                                java.lang.reflect.Method reloadMethod = cacheManagerClass.getMethod("reload");
                                reloadMethod.invoke(cacheManagerInstance);
                                LOGGER.info("Discord cache reloaded successfully");
                            } catch (NoSuchMethodException e2) {
                                LOGGER.debug("SDLink CacheManager does not provide refresh/reload methods");
                            }
                        }
                    }
                } catch (NoSuchMethodException e) {
                    LOGGER.debug("SDLink CacheManager does not provide getInstance method");
                }
            }
            
            LOGGER.debug("Discord cache refresh requested");
            
        } catch (Exception e) {
            LOGGER.error("Error refreshing Discord cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check if the Discord bot is ready to accept API calls.
     * Uses both BotController (if available) and our event listener state.
     * 
     * @return true if bot is connected and ready, false otherwise
     */
    private boolean isBotReady() {
        // Check our event listener first (most reliable)
        if (!SDLinkEventListener.isBotReady()) {
            return false;
        }
        
        // Also check BotController if available as a double-check
        try {
            Class<?> botControllerClass = Class.forName("com.hypherionmc.sdlink.core.discord.BotController");
            Object botController = botControllerClass.getField("INSTANCE").get(null);
            Method isBotReadyMethod = botControllerClass.getMethod("isBotReady");
            Boolean ready = (Boolean) isBotReadyMethod.invoke(botController);
            return ready != null && ready;
        } catch (Exception e) {
            // If BotController check fails, fallback to event listener state
            LOGGER.debug("Could not check BotController.isBotReady(), using event listener state: {}", e.getMessage());
            return SDLinkEventListener.isBotReady();
        }
    }
}
