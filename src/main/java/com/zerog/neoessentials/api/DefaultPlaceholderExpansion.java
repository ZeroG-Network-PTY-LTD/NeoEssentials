package com.zerog.neoessentials.api;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.util.commands.NickCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
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
        // Hover/click variants — resolved to plain text here; ChatFormatter applies
        // the actual click/hover component when chat enhancements are enabled
        placeholders.add("username_hover");
        placeholders.add("displayname_hover");
        
        // Permission system placeholders
        placeholders.add("prefix");
        placeholders.add("suffix");
        placeholders.add("group");

        // Chat channel placeholder
        placeholders.add("channel");

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
        placeholders.add("ping");

        // Economy placeholders
        placeholders.add("balance");
        placeholders.add("balance_formatted");
        placeholders.add("balance_raw");       // plain number, no formatting
        placeholders.add("currency_symbol");   // configured currency symbol
        placeholders.add("baltop_rank");       // player's rank on the balance leaderboard
        placeholders.add("pay_toggle");        // "enabled" / "disabled" pay acceptance status
        
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

        // Stat placeholders
        placeholders.add("deaths");
        placeholders.add("player_kills");
        placeholders.add("mob_kills");
        placeholders.add("play_time");
        
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
                // displayname → nickname if set, otherwise the scoreboard-based display name
                case "displayname" -> player != null ? getNickOrDisplayName(player) : null;
                // username → always the real game-profile name (for admin use / realname lookup)
                case "username", "name" -> player != null ? player.getName().getString() : null;
                // Hover variants — plain text; ChatFormatter renders the Component side
                case "username_hover"   -> player != null ? player.getName().getString() : null;
                case "displayname_hover" -> player != null ? getNickOrDisplayName(player) : null;
                
                // Permission system
                case "prefix" -> getPlayerPrefix(player);
                case "suffix" -> getPlayerSuffix(player);
                case "group" -> getPlayerGroup(player);
                case "channel" -> player != null ? getPlayerChannel(player) : null;
                
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
                case "exp" -> player != null ? (int) (player.experienceProgress * 100) + "%" : null;
                case "gamemode" -> player != null ? player.gameMode.getGameModeForPlayer().getName() : null;
                case "ping" -> player != null ? String.valueOf(player.connection.latency()) : null;

                // Economy
                case "balance" -> getBalance(player);
                case "balance_formatted" -> getFormattedBalance(player);
                case "balance_raw" -> getBalanceRaw(player);
                case "currency_symbol" -> getCurrencySymbol();
                case "baltop_rank" -> getBaltopRank(player);
                case "pay_toggle" -> getPayToggle(player);
                
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

                // Stats
                case "deaths" -> player != null ? String.valueOf(player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS))) : null;
                case "player_kills" -> player != null ? String.valueOf(player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS))) : null;
                case "mob_kills" -> player != null ? String.valueOf(player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS))) : null;
                case "play_time" -> player != null ? formatPlayTime(player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME))) : null;
                
                default -> null;
            };
        } catch (Exception e) {
            LOGGER.error("Error resolving placeholder '{}': {}", identifier, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Returns the player's nickname (color-formatted) if one is set via {@code /nick},
     * otherwise falls back to the scoreboard display name (team prefix/suffix + real name).
     */
    private String getNickOrDisplayName(ServerPlayer player) {
        try {
            String nick = NickCommand.getNickname(player.getUUID());
            if (nick != null && !nick.isEmpty()) {
                return nick.replace("&", "§");
            }
        } catch (Exception e) {
            LOGGER.debug("getNickOrDisplayName: error reading nickname for {}: {}",
                player.getName().getString(), e.getMessage());
        }
        return player.getDisplayName().getString();
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
        if (player == null) {
            LOGGER.warn("getPlayerPrefix called with null player");
            return null;
        }

        boolean debugEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isDebugLoggingEnabled();
        if (debugEnabled) {
            LOGGER.info(">>> DefaultPlaceholderExpansion.getPlayerPrefix() for: {}", player.getName().getString());
            LOGGER.info(">>> Player UUID: {}", player.getUUID());
        }

        try {
            String prefix = PermissionAPI.getPrefix(player.getUUID());
            if (debugEnabled) {
                LOGGER.info(">>> PermissionAPI returned prefix: [{}]", prefix);
                LOGGER.info(">>> Returning prefix: [{}]", prefix);
            }
            return prefix;
        } catch (Exception e) {
            LOGGER.error("Error getting prefix for player {}: {}", player.getName().getString(), e.getMessage(), e);
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
            return PermissionAPI.getSuffix(player.getUUID());
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
     * Get the styled channel text to show for the player's current channel (persistent state, or
     * the configured default channel, or "global" — see
     * {@link com.zerog.neoessentials.chat.ChatHandler#getEffectiveChannel}), resolved through
     * {@link com.zerog.neoessentials.chat.ChatHandler#getChannelDisplayName} so a channel's
     * optional {@code displayName} (e.g. a colored icon) is used instead of the raw channel key.
     */
    @Nullable
    private String getPlayerChannel(@Nullable ServerPlayer player) {
        if (player == null) return null;
        try {
            String channelKey = com.zerog.neoessentials.chat.ChatHandler.getEffectiveChannel(player.getUUID());
            return com.zerog.neoessentials.chat.ChatHandler.getChannelDisplayName(channelKey);
        } catch (Exception e) {
            LOGGER.debug("Error getting channel for player {}: {}", player.getName().getString(), e.getMessage());
            return "global";
        }
    }

    /**
     * Get the name of the world the player is in.
     */
    @Nullable
    private String getWorldName(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            @SuppressWarnings("resource") // Level is managed by Minecraft
            Level level = player.level();
            return level.dimension().identifier().getPath();
        } catch (Exception e) {
            LOGGER.debug("Error getting world name for player {}: {}", player.getName().getString(), e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Get the biome the player is currently in.
     */
    @Nullable
    private String getBiome(@Nullable ServerPlayer player) {
        if (player == null) return null;
        
        try {
            @SuppressWarnings("resource") // Level is managed by Minecraft
            var biome = player.level().getBiome(player.blockPosition());
            return biome.unwrapKey().map(key -> key.identifier().getPath()).orElse("unknown");
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

    /** Raw plain balance with no trailing zeros. */
    @Nullable
    private String getBalanceRaw(@Nullable ServerPlayer player) {
        if (player == null) return null;
        try {
            return EconomyManager.getInstance().getBalance(player.getUUID()).toPlainString();
        } catch (Exception e) {
            return "0";
        }
    }

    /** Configured currency symbol (e.g. "$"). */
    private String getCurrencySymbol() {
        try {
            return EconomyManager.getInstance().getCurrencySymbol();
        } catch (Exception e) {
            return "$";
        }
    }

    /** Player's rank on the /baltop leaderboard, or "N/A" if unknown. */
    @Nullable
    private String getBaltopRank(@Nullable ServerPlayer player) {
        if (player == null) return null;
        try {
            java.util.Map<java.util.UUID, java.math.BigDecimal> all =
                EconomyManager.getInstance().getAllBalances();
            java.math.BigDecimal myBal = EconomyManager.getInstance().getBalance(player.getUUID());
            long rank = all.values().stream()
                .filter(b -> b.compareTo(myBal) > 0)
                .count() + 1;
            return String.valueOf(rank);
        } catch (Exception e) {
            return "N/A";
        }
    }

    /** Whether the player currently accepts payments ("enabled" / "disabled"). */
    @Nullable
    private String getPayToggle(@Nullable ServerPlayer player) {
        if (player == null) return null;
        try {
            boolean accepts = com.zerog.neoessentials.economy.managers.PayToggleManager
                .getInstance().getPayToggle(player.getUUID());
            return accepts ? "enabled" : "disabled";
        } catch (Exception e) {
            return "enabled";
        }
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
    private String getServerName(@Nullable ServerPlayer player) {
        try {
            if (player != null && player.level().getServer() != null) {
                return player.level().getServer().getMotd();
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting server name: {}", e.getMessage());
        }
        return "Minecraft Server";
    }
    
    /**
     * Get the current online player count.
     */
    private String getOnlinePlayerCount(@Nullable ServerPlayer player) {
        try {
            if (player != null && player.level().getServer() != null) {
                return String.valueOf(player.level().getServer().getPlayerCount());
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting online player count: {}", e.getMessage());
        }
        return "0";
    }
    
    /**
     * Get the maximum player count.
     */
    private String getMaxPlayerCount(@Nullable ServerPlayer player) {
        try {
            if (player != null && player.level().getServer() != null) {
                return String.valueOf(player.level().getServer().getMaxPlayers());
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting max player count: {}", e.getMessage());
        }
        return "20";
    }
    
    /**
     * Get current time in 12-hour format.
     */
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

    /**
     * Format play time from ticks (20 ticks = 1 second) into a human-readable string.
     * e.g. "3d 4h 12m" or "45m 30s"
     */
    private String formatPlayTime(int ticks) {
        long totalSeconds = ticks / 20;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours   = (totalSeconds / 3600) % 24;
        long days    = totalSeconds / 86400;

        if (days > 0)        return String.format("%dd %dh %dm", days, hours, minutes);
        else if (hours > 0)  return String.format("%dh %dm", hours, minutes);
        else if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        else                 return String.format("%ds", seconds);
    }
}