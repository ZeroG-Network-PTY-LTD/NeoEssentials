package com.zerog.neoessentials.features;

import com.zerog.neoessentials.integration.FTBIntegrationHelper;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central manager for player name, prefix, and suffix formatting.
 * Integrates with FTB Teams, FTB Ranks, and internal permission systems.
 * Use this for tablist, chat, scoreboard, bossbar, etc.
 */
public class NameFormatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NameFormatManager.class);
    private static NameFormatManager instance;
    
    // Configuration options
    private boolean preferFTBRankPrefix = true;
    private boolean preferFTBTeamSuffix = true;
    private boolean combinePrefixes = true;
    private boolean combineSuffixes = true;
    private String prefixSeparator = " ";
    private String suffixSeparator = " ";
    
    public static NameFormatManager getInstance() {
        if (instance == null) instance = new NameFormatManager();
        return instance;
    }

    /**
     * Get the effective prefix for a player, combining FTB and internal systems
     */
    public String getPrefix(ServerPlayer player) {
        StringBuilder prefix = new StringBuilder();
        
        try {
            // Get FTB rank prefix if available and preferred
            String ftbRankPrefix = "";
            if (FTBIntegrationHelper.isFTBRanksLoaded() && preferFTBRankPrefix) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(player);
                ftbRankPrefix = rankInfo.rankPrefix;
            }
            
            // Get internal prefix
            String internalPrefix = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance()
                    .getPlayerPrefix(player.getUUID());
            if (internalPrefix == null) internalPrefix = "";
            
            // Get FTB team prefix if available
            String ftbTeamPrefix = "";
            if (FTBIntegrationHelper.isFTBTeamsLoaded()) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(player);
                ftbTeamPrefix = teamInfo.teamPrefix;
            }
            
            // Combine prefixes based on configuration
            if (preferFTBRankPrefix && !ftbRankPrefix.isEmpty()) {
                prefix.append(ftbRankPrefix);
                if (combinePrefixes && !internalPrefix.isEmpty()) {
                    prefix.append(prefixSeparator).append(internalPrefix);
                }
            } else if (!internalPrefix.isEmpty()) {
                prefix.append(internalPrefix);
                if (combinePrefixes && !ftbRankPrefix.isEmpty()) {
                    prefix.append(prefixSeparator).append(ftbRankPrefix);
                }
            }
            
            // Add team prefix if available and combining is enabled
            if (combinePrefixes && !ftbTeamPrefix.isEmpty()) {
                if (prefix.length() > 0) {
                    prefix.append(prefixSeparator);
                }
                prefix.append(ftbTeamPrefix);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Error getting prefix for player {}: {}", player.getName().getString(), e.getMessage());
            // Fallback to internal prefix only
            String fallback = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance()
                    .getPlayerPrefix(player.getUUID());
            return fallback != null ? fallback : "";
        }
        
        return prefix.toString();
    }
    
    /**
     * Get the effective suffix for a player, combining FTB and internal systems
     */
    public String getSuffix(ServerPlayer player) {
        StringBuilder suffix = new StringBuilder();
        
        try {
            // Get internal suffix
            String internalSuffix = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance()
                    .getPlayerSuffix(player.getUUID());
            if (internalSuffix == null) internalSuffix = "";
            
            // Get FTB rank suffix if available
            String ftbRankSuffix = "";
            if (FTBIntegrationHelper.isFTBRanksLoaded()) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(player);
                ftbRankSuffix = rankInfo.rankSuffix;
            }
            
            // Get FTB team suffix if available and preferred
            String ftbTeamSuffix = "";
            if (FTBIntegrationHelper.isFTBTeamsLoaded() && preferFTBTeamSuffix) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(player);
                ftbTeamSuffix = teamInfo.teamSuffix;
            }
            
            // Combine suffixes based on configuration
            if (!internalSuffix.isEmpty()) {
                suffix.append(internalSuffix);
            }
            
            if (combineSuffixes && !ftbRankSuffix.isEmpty()) {
                if (suffix.length() > 0) {
                    suffix.append(suffixSeparator);
                }
                suffix.append(ftbRankSuffix);
            }
            
            if (preferFTBTeamSuffix && !ftbTeamSuffix.isEmpty()) {
                if (suffix.length() > 0) {
                    suffix.append(suffixSeparator);
                }
                suffix.append(ftbTeamSuffix);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Error getting suffix for player {}: {}", player.getName().getString(), e.getMessage());
            // Fallback to internal suffix only
            String fallback = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance()
                    .getPlayerSuffix(player.getUUID());
            return fallback != null ? fallback : "";
        }
        
        return suffix.toString();
    }
    
    /**
     * Get the effective group for a player, preferring FTB rank over internal group
     */
    public String getGroup(ServerPlayer player) {
        try {
            // Try FTB rank first if available
            if (FTBIntegrationHelper.isFTBRanksLoaded()) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(player);
                if (!rankInfo.rankName.isEmpty()) {
                    return rankInfo.rankName;
                }
            }
            
            // Fall back to internal group
            String internalGroup = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance()
                    .getPlayerGroup(player.getUUID());
            if (internalGroup != null && !internalGroup.isEmpty()) {
                return internalGroup;
            }
            
            // Try team name as group if available
            if (FTBIntegrationHelper.isFTBTeamsLoaded()) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(player);
                if (!teamInfo.teamName.isEmpty()) {
                    return "team_" + teamInfo.teamName;
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Error getting group for player {}: {}", player.getName().getString(), e.getMessage());
        }
        
        return "default";
    }
    
    /**
     * Get the player's team name (FTB Teams integration)
     */
    public String getTeamName(ServerPlayer player) {
        try {
            if (FTBIntegrationHelper.isFTBTeamsLoaded()) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(player);
                return teamInfo.teamDisplayName.isEmpty() ? teamInfo.teamName : teamInfo.teamDisplayName;
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting team name for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return "";
    }
    
    /**
     * Get the player's rank name (FTB Ranks integration)
     */
    public String getRankName(ServerPlayer player) {
        try {
            if (FTBIntegrationHelper.isFTBRanksLoaded()) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(player);
                return rankInfo.rankDisplayName.isEmpty() ? rankInfo.rankName : rankInfo.rankDisplayName;
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting rank name for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return "";
    }
    
    /**
     * Get the player's team role (owner, moderator, member)
     */
    public String getTeamRole(ServerPlayer player) {
        try {
            if (FTBIntegrationHelper.isFTBTeamsLoaded()) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(player);
                if (!teamInfo.teamName.isEmpty()) {
                    if (teamInfo.isTeamOwner) return "owner";
                    if (teamInfo.isTeamModerator) return "moderator";
                    return "member";
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting team role for player {}: {}", player.getName().getString(), e.getMessage());
        }
        return "";
    }
    
    /**
     * Get the player's nickname (from internal nickname system)
     */
    public String getNickname(ServerPlayer player) {
        String nick = com.zerog.neoessentials.commands.essentials.NickCommand.getNickname(player);
        return (nick != null && !nick.isEmpty()) ? nick : player.getGameProfile().getName();
    }
    
    /**
     * Get the player's display name with placeholder processing
     */
    public String getDisplayName(ServerPlayer player) {
        String nickname = getNickname(player);
        // Process placeholders in nickname (including animated)
        String processed = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance()
                .processPlaceholders(nickname, new com.zerog.neoessentials.placeholders.PlaceholderManager.PlaceholderContext(player));
        return processed.replace('&', '\u00A7');
    }
    
    /**
     * Get formatted name for tablist, chat, etc. using a format string.
     * Supported placeholders: 
     * - {PREFIX}, {DISPLAYNAME}, {SUFFIX}, {GROUP}
     * - {TEAM_NAME}, {TEAM_ROLE}, {RANK_NAME}
     * - {FTB_RANK_PREFIX}, {FTB_RANK_SUFFIX}
     * - {FTB_TEAM_PREFIX}, {FTB_TEAM_SUFFIX}
     */
    public String format(ServerPlayer player, String format) {
        try {
            String prefix = getPrefix(player);
            String suffix = getSuffix(player);
            String group = getGroup(player);
            String nickname = getNickname(player);
            String teamName = getTeamName(player);
            String teamRole = getTeamRole(player);
            String rankName = getRankName(player);
            
            // Get individual FTB components
            String ftbRankPrefix = "";
            String ftbRankSuffix = "";
            String ftbTeamPrefix = "";
            String ftbTeamSuffix = "";
            
            if (FTBIntegrationHelper.isFTBRanksLoaded()) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(player);
                ftbRankPrefix = rankInfo.rankPrefix;
                ftbRankSuffix = rankInfo.rankSuffix;
            }
            
            if (FTBIntegrationHelper.isFTBTeamsLoaded()) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(player);
                ftbTeamPrefix = teamInfo.teamPrefix;
                ftbTeamSuffix = teamInfo.teamSuffix;
            }
            
            String result = format
                .replace("{PREFIX}", prefix != null ? prefix : "")
                .replace("{DISPLAYNAME}", nickname)
                .replace("{SUFFIX}", suffix != null ? suffix : "")
                .replace("{GROUP}", group != null ? group : "")
                .replace("{TEAM_NAME}", teamName != null ? teamName : "")
                .replace("{TEAM_ROLE}", teamRole != null ? teamRole : "")
                .replace("{RANK_NAME}", rankName != null ? rankName : "")
                .replace("{FTB_RANK_PREFIX}", ftbRankPrefix != null ? ftbRankPrefix : "")
                .replace("{FTB_RANK_SUFFIX}", ftbRankSuffix != null ? ftbRankSuffix : "")
                .replace("{FTB_TEAM_PREFIX}", ftbTeamPrefix != null ? ftbTeamPrefix : "")
                .replace("{FTB_TEAM_SUFFIX}", ftbTeamSuffix != null ? ftbTeamSuffix : "");
            
            return result.replace('&', '\u00A7');
            
        } catch (Exception e) {
            LOGGER.error("Error formatting name for player {}: {}", player.getName().getString(), e.getMessage());
            return player.getName().getString();
        }
    }
    
    /**
     * Configuration methods
     */
    public void setPreferFTBRankPrefix(boolean prefer) {
        this.preferFTBRankPrefix = prefer;
    }
    
    public void setPreferFTBTeamSuffix(boolean prefer) {
        this.preferFTBTeamSuffix = prefer;
    }
    
    public void setCombinePrefixes(boolean combine) {
        this.combinePrefixes = combine;
    }
    
    public void setCombineSuffixes(boolean combine) {
        this.combineSuffixes = combine;
    }
    
    public void setPrefixSeparator(String separator) {
        this.prefixSeparator = separator != null ? separator : " ";
    }
    
    public void setSuffixSeparator(String separator) {
        this.suffixSeparator = separator != null ? separator : " ";
    }
    
    /**
     * Clear cached data for a player when they join/leave teams or ranks change
     */
    public void clearPlayerCache(ServerPlayer player) {
        FTBIntegrationHelper.clearPlayerCache(player.getUUID());
    }
}
