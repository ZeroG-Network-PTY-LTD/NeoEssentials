package com.zerog.neoessentials.config;

import java.util.Arrays;
import java.util.List;

/**
 * Tablist (player list) configuration for NeoEssentials
 * Compatible with EssentialsX tablist features
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TablistConfig {
    

    // Basic tablist settings
    public boolean enableCustomHeader = true;
    public boolean enableCustomFooter = true;
    public boolean enablePlayerPrefixes = true;
    public boolean enablePlayerSuffixes = true;
    public boolean enableGroupSorting = true;
    
    // Header and footer settings
    public HeaderFooterConfig headerFooter = new HeaderFooterConfig();
    
    // Player name formatting
    public PlayerFormatConfig playerFormat = new PlayerFormatConfig();
    
    // Group settings
    public GroupConfig groups = new GroupConfig();
    
    // Update settings
    public UpdateConfig updates = new UpdateConfig();
    
    // Ping settings
    public PingConfig ping = new PingConfig();
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public static class HeaderFooterConfig {
        public boolean enabled = true;
        public List<String> headers = Arrays.asList(
            "&6&l✦ &e&lWELCOME TO NEOESSENTIALS &6&l✦",
            "&6&l⚡ &e&lWELCOME TO NEOESSENTIALS &6&l⚡",
            "&6&l★ &e&lWELCOME TO NEOESSENTIALS &6&l★"
        );
        public List<String> footers = Arrays.asList(
            "&7Players: &a{ONLINE}&7/&a{MAX} &8| &7TPS: &a{TPS} &8| &7Ping: &a{PING}ms",
            "&7Online: &a{ONLINE}&7/&a{MAX} &8| &7World: &a{WORLD} &8| &7Time: &a{TIME}",
            "&7Players: &a{ONLINE}&7/&a{MAX} &8| &7Balance: &a${BALANCE} &8| &7Rank: &a{GROUP}"
        );
        public int animationSpeed = 20; // Ticks (1 second)
        public boolean animateHeaders = true;
        public boolean animateFooters = true;
        public boolean showPlayerSpecificInfo = true;
    }
    
    public static class PlayerFormatConfig {
        public boolean enabled = true;
        public String defaultFormat = "{PREFIX}{PLAYER}{SUFFIX}";
        public String afkFormat = "&7[AFK] {PREFIX}{PLAYER}{SUFFIX}";
        public String vanishedFormat = "&8[V] {PREFIX}{PLAYER}{SUFFIX}";
        public String godmodeFormat = "&e[GOD] {PREFIX}{PLAYER}{SUFFIX}";
        public String muteFormat = "&c[MUTED] {PREFIX}{PLAYER}{SUFFIX}";
        public String jailedFormat = "&6[JAIL] {PREFIX}{PLAYER}{SUFFIX}";
        public boolean showAfkStatus = true;
        public boolean showVanishStatus = true;
        public boolean showGodmodeStatus = true;
        public boolean showMuteStatus = true;
        public boolean showJailStatus = true;
        public boolean useNicknames = true;
        public int maxNameLength = 16;
        public boolean shortenLongNames = true;
    }
    
    public static class GroupConfig {
        public boolean enabled = true;
        public GroupSorting sorting = new GroupSorting();
        public List<GroupDefinition> groups = Arrays.asList(
            new GroupDefinition("owner", "&4&l[OWNER] ", " &4⚡", 1),
            new GroupDefinition("admin", "&c&l[ADMIN] ", " &c⭐", 2),
            new GroupDefinition("moderator", "&6&l[MOD] ", " &6★", 3),
            new GroupDefinition("helper", "&e&l[HELPER] ", " &e◆", 4),
            new GroupDefinition("vip", "&b&l[VIP] ", " &b♦", 5),
            new GroupDefinition("premium", "&a&l[PREMIUM] ", " &a♠", 6),
            new GroupDefinition("player", "&7", "", 7),
            new GroupDefinition("default", "&7", "", 8)
        );
        
        public static class GroupSorting {
            public boolean enabled = true;
            public String sortBy = "PRIORITY"; // PRIORITY, ALPHABETICAL, PERMISSION
            public boolean sortAscending = true;
            public boolean groupStaff = true;
            public boolean groupDonators = true;
            public boolean separateGroups = false; // Add spacing between groups
        }
        
        public static class GroupDefinition {
            public String name;
            public String prefix;
            public String suffix;
            public int priority;
            
            public GroupDefinition() {}
            
            public GroupDefinition(String name, String prefix, String suffix, int priority) {
                this.name = name;
                this.prefix = prefix;
                this.suffix = suffix;
                this.priority = priority;
            }
        }
    }
    
    public static class UpdateConfig {
        public boolean enabled = true;
        public int updateIntervalTicks = 20; // 1 second
        public int headerFooterUpdateTicks = 20; // 1 second for animations
        public boolean updateOnPlayerJoin = true;
        public boolean updateOnPlayerLeave = true;
        public boolean updateOnPlayerChat = false;
        public boolean updateOnBalanceChange = true;
        public boolean updateOnRankChange = true;
        public boolean asyncUpdates = true;
    }
    
    public static class PingConfig {
        public boolean enabled = true;
        public boolean showInTablist = true;
        public boolean colorCodePing = true;
        public int goodPingThreshold = 50;
        public int okayPingThreshold = 100;
        public int badPingThreshold = 200;
        public String goodPingColor = "&a";
        public String okayPingColor = "&e";
        public String badPingColor = "&c";
        public String terriblePingColor = "&4";
        public String pingFormat = "{COLOR}{PING}ms";
    }
    
    public static class MessagesConfig {
        // Placeholder values for messages that might be used
        public String loadingTablist = "&7Loading tablist...";
        public String tablistDisabled = "&cTablist is disabled!";
        public String invalidFormat = "&cInvalid tablist format!";
        public String noPermission = "&cYou don't have permission to modify the tablist!";
        public String formatUpdated = "&aTablist format updated!";
        public String headerUpdated = "&aTablist header updated!";
        public String footerUpdated = "&aTablist footer updated!";
        
        // Group messages
        public String groupNotFound = "&cGroup '{GROUP}' not found!";
        public String groupFormatUpdated = "&aGroup '{GROUP}' format updated!";
        public String playerGroupChanged = "&aPlayer {PLAYER} moved to group {GROUP}!";
        
        // Status indicators
        public String afkIndicator = "&7[AFK]";
        public String vanishIndicator = "&8[VANISH]";
        public String godmodeIndicator = "&e[GOD]";
        public String muteIndicator = "&c[MUTED]";
        public String jailIndicator = "&6[JAIL]";
        
        // Time formats
        public String timeFormat12Hour = "h:mm a";
        public String timeFormat24Hour = "HH:mm";
        public boolean use24HourFormat = false;
        
        // Number formats
        public String balanceFormat = "${BALANCE}";
        public String largeNumberFormat = "{NUMBER}k"; // For numbers > 1000
        public boolean shortenLargeNumbers = true;
    }
    
    /**
     * Check if tablist is enabled
     */
    public boolean isEnabled() {
    // Centralized: check main config for tablist enable/disable
    return com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().modules.tablist;
    }
    
    /**
     * Check if custom header/footer is enabled
     */
    public boolean isHeaderFooterEnabled() {
    return com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().modules.tablist
        && enableCustomHeader && enableCustomFooter && headerFooter.enabled;
    }
    
    /**
     * Check if player formatting is enabled
     */
    public boolean isPlayerFormatEnabled() {
    return com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().modules.tablist
        && enablePlayerPrefixes && enablePlayerSuffixes && playerFormat.enabled;
    }
    
    /**
     * Check if group sorting is enabled
     */
    public boolean isGroupSortingEnabled() {
    return com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().modules.tablist
        && enableGroupSorting && groups.enabled && groups.sorting.enabled;
    }
    
    /**
     * Get group definition by name
     */
    public GroupConfig.GroupDefinition getGroupDefinition(String groupName) {
        return groups.groups.stream()
            .filter(group -> group.name.equalsIgnoreCase(groupName))
            .findFirst()
            .orElse(groups.groups.stream()
                .filter(group -> group.name.equalsIgnoreCase("default"))
                .findFirst()
                .orElse(new GroupConfig.GroupDefinition("default", "&7", "", 999)));
    }
    
    /**
     * Get ping color based on ping value
     */
    public String getPingColor(int pingMs) {
        if (!ping.colorCodePing) return "";
        
        if (pingMs <= ping.goodPingThreshold) {
            return ping.goodPingColor;
        } else if (pingMs <= ping.okayPingThreshold) {
            return ping.okayPingColor;
        } else if (pingMs <= ping.badPingThreshold) {
            return ping.badPingColor;
        } else {
            return ping.terriblePingColor;
        }
    }
    
    /**
     * Get current header (cycling through headers if animation enabled)
     */
    public String getCurrentHeader(long currentTick) {
        if (!headerFooter.animateHeaders || headerFooter.headers.isEmpty()) {
            return headerFooter.headers.isEmpty() ? "" : headerFooter.headers.get(0);
        }
        
        int index = (int) ((currentTick / headerFooter.animationSpeed) % headerFooter.headers.size());
        return headerFooter.headers.get(index);
    }
    
    /**
     * Get current footer (cycling through footers if animation enabled)
     */
    public String getCurrentFooter(long currentTick) {
        if (!headerFooter.animateFooters || headerFooter.footers.isEmpty()) {
            return headerFooter.footers.isEmpty() ? "" : headerFooter.footers.get(0);
        }
        
        int index = (int) ((currentTick / headerFooter.animationSpeed) % headerFooter.footers.size());
        return headerFooter.footers.get(index);
    }
}
