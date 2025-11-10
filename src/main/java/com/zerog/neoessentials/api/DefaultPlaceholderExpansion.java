package com.zerog.neoessentials.api;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.HashSet;

/**
 * Default placeholder expansion for NeoEssentials.
 * Provides all the built-in placeholders that NeoEssentials supports.
 */
public class DefaultPlaceholderExpansion extends PlaceholderExpansion {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPlaceholderExpansion.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    
    private final Set<String> placeholders = new HashSet<>();
    
    public DefaultPlaceholderExpansion() {
        // Register all default placeholders
        initializePlaceholders();
    }
    
    private void initializePlaceholders() {
        // Player identity placeholders
        placeholders.add("displayname");
        placeholders.add("username");
        placeholders.add("name"); // alias for username
        
        // Permission system placeholders
        placeholders.add("prefix");
        placeholders.add("suffix");
        placeholders.add("group");
        
        // Location placeholders
        placeholders.add("world");
        placeholders.add("x");
        placeholders.add("y");
        placeholders.add("z");
        placeholders.add("biome");
        
        // Player status placeholders
        placeholders.add("health");
        placeholders.add("max_health");
        placeholders.add("food");
        placeholders.add("level");
        placeholders.add("exp");
        placeholders.add("gamemode");
        
        // Economy placeholders
        placeholders.add("balance");
        placeholders.add("balance_formatted");
        
        // Server placeholders
        placeholders.add("server_name");
        placeholders.add("online_players");
        placeholders.add("max_players");
        
        // Time placeholders
        placeholders.add("time");
        placeholders.add("time_24");
        placeholders.add("date");
        
        // AFK status placeholders
        placeholders.add("afk");
        placeholders.add("afk_time");
        placeholders.add("afk_reason");
        
        LOGGER.debug("Initialized {} default placeholders", placeholders.size());
    }
    
    @Override
    public String getIdentifier() {
        return "neoessentials";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getAuthor() {
        return "ZeroG Network";
    }
    
    @Override
    public Set<String> getPlaceholders() {
        return new HashSet<>(placeholders);
    }
    
    @Override
    @Nullable
    public String onPlaceholderRequest(@Nullable ServerPlayer player, String identifier, @Nullable String params) {
        if (player == null && requiresPlayer(identifier)) {
            return null;
        }
        
        try {
            return switch (identifier.toLowerCase()) {
                // Player identity
                case "displayname" -> player != null ? player.getDisplayName().getString() : null;
                case "username", "name" -> player != null ? player.getName().getString() : null;
                
                // Permission system
                case "prefix" -> getPlayerPrefix(player);
                case "suffix" -> getPlayerSuffix(player);
                case "group" -> getPlayerGroup(player);
                
                // Location
                case "world" -> player != null ? getWorldName(player) : null;
                case "x" -> player != null ? String.valueOf((int) player.getX()) : null;
                case "y" -> player != null ? String.valueOf((int) player.getY()) : null;
                case "z" -> player != null ? String.valueOf((int) player.getZ()) : null;
                case "biome" -> player != null ? getBiome(player) : null;
                
                // Player status
                case "health" -> player != null ? DECIMAL_FORMAT.format(player.getHealth()) : null;
                case "max_health" -> player != null ? DECIMAL_FORMAT.format(player.getMaxHealth()) : null;
                case "food" -> player != null ? String.valueOf(player.getFoodData().getFoodLevel()) : null;
                case "level" -> player != null ? String.valueOf(player.experienceLevel) : null;
                case "exp" -> player != null ? String.valueOf((int) (player.experienceProgress * 100)) + "%" : null;
                case "gamemode" -> player != null ? player.gameMode.getGameModeForPlayer().getName() : null;
                
                // Economy
                case "balance" -> getBalance(player);
                case "balance_formatted" -> getFormattedBalance(player);
                
                // Server
                case "server_name" -> getServerName(player);
                case "online_players" -> getOnlinePlayerCount(player);
                case "max_players" -> getMaxPlayerCount(player);
                
                // Time
                case "time" -> getCurrentTime();
                case "time_24" -> getCurrentTime24();
                case "date" -> getCurrentDate();
                
                // AFK status
                case "afk" -> getAfkStatus(player);
                case "afk_time" -> getAfkTime(player);
                case "afk_reason" -> getAfkReason(player);
                
                default -> null;
            };
        } catch (Exception e) {
            LOGGER.error("Error resolving placeholder '{}': {}", identifier, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if a placeholder requires a player context.
     */
    private boolean requiresPlayer(String identifier) {
        return switch (identifier.toLowerCase()) {
            case "server_name", "online_players", "max_players", "time", "time_24", "date" -> false;
            default -> true;
        };
    }
    
    /**
     * Get player's prefix from the permission system.
     */
    @Nullable
    private String getPlayerPrefix(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            String prefix = PermissionAPI.getPrefix(player.getUUID());
            return prefix != null ? prefix : "";
        } catch (Exception e) {
            LOGGER.debug("Error getting prefix for player {}: {}", player.getName().getString(), e.getMessage());
            return "";
        }
    }
    
    /**
     * Get player's suffix from the permission system.
     */
    @Nullable
    private String getPlayerSuffix(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            String suffix = PermissionAPI.getSuffix(player.getUUID());
            return suffix != null ? suffix : "";
        } catch (Exception e) {
            LOGGER.debug("Error getting suffix for player {}: {}", player.getName().getString(), e.getMessage());
            return "";
        }
    }
    
    /**
     * Get player's primary group from the permission system.
     */
    @Nullable
    private String getPlayerGroup(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            // Get the player's group through the PermissionManager
            var manager = PermissionAPI.getManager();
            if (manager != null) {
                var user = manager.getUser(player.getUUID());
                if (user != null && user.getGroup() != null) {
                    return user.getGroup();
                }
                return manager.getDefaultGroup();
            }
            return "default";
        } catch (Exception e) {
            LOGGER.debug("Error getting group for player {}: {}", player.getName().getString(), e.getMessage());
            return "default";
        }
    }
    
    /**
     * Get the name of the world the player is in.
     */
    @Nullable
    private String getWorldName(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            Level level = player.level();
            if (level != null) {
                return level.dimension().location().getPath();
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting world name for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return "unknown";
    }
    
    /**
     * Get the biome the player is currently in.
     */
    @Nullable
    private String getBiome(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            var biome = player.level().getBiome(player.blockPosition());
            return biome.unwrapKey().map(key -> key.location().getPath()).orElse("unknown");
        } catch (Exception e) {
            LOGGER.debug("Error getting biome for player {}: {}", player.getName().getString(), e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Get player's balance from the economy system.
     */
    @Nullable
    private String getBalance(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            if (economyManager != null) {
                var balance = economyManager.getBalance(player.getUUID());
                return balance.toString();
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting balance for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return "0.0";
    }
    
    /**
     * Get player's formatted balance from the economy system.
     */
    @Nullable
    private String getFormattedBalance(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            if (economyManager != null) {
                var balance = economyManager.getBalance(player.getUUID());
                return DECIMAL_FORMAT.format(balance.doubleValue());
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting formatted balance for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return "0.00";
    }
    
    /**
     * Get the server name (motd or configured name).
     */
    @Nullable
    private String getServerName(@Nullable ServerPlayer player) {
        try {
            if (player != null && player.getServer() != null) {
                return player.getServer().getMotd();
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting server name: {}", e.getMessage());
        }
        return "Minecraft Server";
    }
    
    /**
     * Get the current online player count.
     */
    @Nullable
    private String getOnlinePlayerCount(@Nullable ServerPlayer player) {
        try {
            if (player != null && player.getServer() != null) {
                return String.valueOf(player.getServer().getPlayerCount());
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting online player count: {}", e.getMessage());
        }
        return "0";
    }
    
    /**
     * Get the maximum player count.
     */
    @Nullable
    private String getMaxPlayerCount(@Nullable ServerPlayer player) {
        try {
            if (player != null && player.getServer() != null) {
                return String.valueOf(player.getServer().getMaxPlayers());
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting max player count: {}", e.getMessage());
        }
        return "20";
    }
    
    /**
     * Get current time in 12-hour format.
     */
    @Nullable
    private String getCurrentTime() {
        try {
            return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            LOGGER.debug("Error getting current time: {}", e.getMessage());
            return "00:00 AM";
        }
    }
    
    /**
     * Get current time in 24-hour format.
     */
    @Nullable
    private String getCurrentTime24() {
        try {
            return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            LOGGER.debug("Error getting current time (24h): {}", e.getMessage());
            return "00:00";
        }
    }
    
    /**
     * Get current date.
     */
    @Nullable
    private String getCurrentDate() {
        try {
            return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            LOGGER.debug("Error getting current date: {}", e.getMessage());
            return "1970-01-01";
        }
    }
    
    /**
     * Get player's AFK status.
     * Returns "AFK" if player is AFK, empty string otherwise.
     */
    @Nullable
    private String getAfkStatus(@Nullable ServerPlayer player) {
        if (player == null) return "";
        
        try {
            var afkManager = com.zerog.neoessentials.chat.AfkManager.getInstance();
            boolean isAfk = afkManager.isAfk(player.getUUID());
            return isAfk ? "AFK" : "";
        } catch (Exception e) {
            LOGGER.debug("Error getting AFK status for player {}: {}", player.getName().getString(), e.getMessage());
            return "";
        }
    }
    
    /**
     * Get how long player has been AFK.
     * Returns formatted time like "5m 30s" or empty if not AFK.
     */
    @Nullable
    private String getAfkTime(@Nullable ServerPlayer player) {
        if (player == null) return "";
        
        try {
            var afkManager = com.zerog.neoessentials.chat.AfkManager.getInstance();
            if (!afkManager.isAfk(player.getUUID())) {
                return "";
            }
            
            long afkMs = afkManager.getAfkDuration(player.getUUID());
            if (afkMs <= 0) return "";
            
            long seconds = afkMs / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            
            if (hours > 0) {
                return String.format("%dh %dm", hours, minutes % 60);
            } else if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds % 60);
            } else {
                return String.format("%ds", seconds);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting AFK time for player {}: {}", player.getName().getString(), e.getMessage());
            return "";
        }
    }
    
    /**
     * Get player's AFK reason.
     * Returns the reason text or empty string if no reason or not AFK.
     */
    @Nullable
    private String getAfkReason(@Nullable ServerPlayer player) {
        if (player == null) return "";
        
        try {
            var afkManager = com.zerog.neoessentials.chat.AfkManager.getInstance();
            if (!afkManager.isAfk(player.getUUID())) {
                return "";
            }
            
            String reason = afkManager.getAfkReason(player.getUUID());
            return reason != null ? reason : "";
        } catch (Exception e) {
            LOGGER.debug("Error getting AFK reason for player {}: {}", player.getName().getString(), e.getMessage());
            return "";
        }
    }
}