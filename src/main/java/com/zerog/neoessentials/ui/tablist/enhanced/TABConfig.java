package com.zerog.neoessentials.ui.tablist.enhanced;

import java.util.*;

/**
 * Configuration manager for TAB-like tablist system
 * Handles loading and managing all tablist configuration options
 */
public class TABConfig {
    
    // Header & Footer configuration
    private boolean headerFooterEnabled = true;
    private List<String> defaultHeaders = new ArrayList<>();
    private List<String> defaultFooters = new ArrayList<>();
    private String headerFooterDisableCondition = "";
    private Map<String, List<String>> perWorldHeaders = new HashMap<>();
    private Map<String, List<String>> perWorldFooters = new HashMap<>();
    private Map<String, List<String>> perServerHeaders = new HashMap<>();
    private Map<String, List<String>> perServerFooters = new HashMap<>();
    private Map<String, List<String>> groupHeaders = new HashMap<>();
    private Map<String, List<String>> groupFooters = new HashMap<>();
    
    // Tablist name formatting
    private boolean tablistNameFormattingEnabled = true;
    private String tablistNameDisableCondition = "";
    
    // Team settings (for sorting and collision)
    private boolean enableCollision = true;
    private boolean invisibleNametags = false;
    private List<String> sortingTypes = new ArrayList<>();
    private boolean caseSensitiveSorting = true;
    private boolean canSeeFriendlyInvisibles = false;
    private Map<String, Integer> groupPriorities = new HashMap<>();
    
    // Playerlist objective
    private boolean playerlistObjectiveEnabled = true;
    private String playerlistObjectiveValue = "%ping%";
    private String playerlistObjectiveFancyValue = "&7Ping: %ping%";
    private String playerlistObjectiveTitle = "TAB";
    private String playerlistObjectiveRenderType = "INTEGER";
    private String playerlistObjectiveDisableCondition = "";
    
    // Belowname objective
    private boolean belownameObjectiveEnabled = false;
    private String belownameObjectiveValue = "%health%";
    private String belownameObjectiveTitle = "&cHealth";
    private String belownameObjectiveFancyValue = "&c%health%";
    private String belownameObjectiveFancyValueDefault = "NPC";
    private String belownameObjectiveDisableCondition = "";
    
    // Boss bar configuration
    private boolean bossBarEnabled = false;
    private String bossBarToggleCommand = "/bossbar";
    private boolean bossBarRememberToggle = false;
    private boolean bossBarHiddenByDefault = false;
    private Map<String, BossBarConfig> bossBars = new HashMap<>();
    
    // Layout configuration
    private boolean layoutEnabled = false;
    private String layoutDirection = "COLUMNS";
    private String layoutDefaultSkin = "mineskin:383747683";
    private boolean layoutEnableRemainingPlayersText = true;
    private String layoutRemainingPlayersText = "... and %s more";
    private int layoutEmptySlotPingValue = 1000;
    private Map<String, LayoutConfig> layouts = new HashMap<>();
    
    // Ping spoof
    private boolean pingSpoofEnabled = false;
    private int pingSpoofValue = 0;
    
    // Placeholder settings
    private String dateFormat = "dd.MM.yyyy";
    private String timeFormat = "[HH:mm:ss / h:mm a]";
    private int timeOffset = 0;
    private boolean registerTabExpansion = false;
    private Map<String, Long> placeholderRefreshIntervals = new HashMap<>();
    private long defaultRefreshInterval = 500;
    
    // Placeholder output replacements
    private Map<String, Map<String, String>> placeholderOutputReplacements = new HashMap<>();
    
    // Conditional placeholders
    private Map<String, ConditionalPlaceholder> conditions = new HashMap<>();
    
    // Update interval
    private long updateInterval = 1000; // 1 second default
    
    // Spectator effect prevention
    private boolean preventSpectatorEffect = false;
    
    public TABConfig() {
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Default headers
        defaultHeaders.addAll(Arrays.asList(
            "&6&l✦ &b&lNeoEssentials Server &6&l✦",
            "&eWelcome, &a%player%&e!",
            "&eOnline players: &a%online%/%max%",
            "&eServer time: &a%time%"
        ));
        
        // Default footers
        defaultFooters.addAll(Arrays.asList(
            "&eBalance: &a%balance% coins",
            "&eWebsite: &awww.example.com",
            "&eThanks for playing!",
            "&eRunning &aNeoForge Server"
        ));
        
        // Default sorting
        sortingTypes.addAll(Arrays.asList(
            "GROUPS:owner,admin,mod,helper,builder,vip,default",
            "PLACEHOLDER_A_TO_Z:%player%"
        ));
        
        // Group priorities (lower number = higher priority)
        groupPriorities.put("owner", 1);
        groupPriorities.put("admin", 2);
        groupPriorities.put("mod", 3);
        groupPriorities.put("helper", 4);
        groupPriorities.put("builder", 5);
        groupPriorities.put("vip", 6);
        groupPriorities.put("default", 7);
        
        // Default placeholder refresh intervals
        placeholderRefreshIntervals.put("%server_uptime%", 1000L);
        placeholderRefreshIntervals.put("%server_tps_1_colored%", 1000L);
        placeholderRefreshIntervals.put("%server_unique_joins%", 5000L);
        placeholderRefreshIntervals.put("%player_health%", 200L);
        placeholderRefreshIntervals.put("%player_ping%", 1000L);
        placeholderRefreshIntervals.put("%vault_prefix%", 1000L);
        
        // Example boss bar
        BossBarConfig serverInfo = new BossBarConfig();
        serverInfo.setStyle("PROGRESS");
        serverInfo.setColor("%animation:barcolors%");
        serverInfo.setProgress("100");
        serverInfo.setText("&fWebsite: &bwww.domain.com");
        bossBars.put("ServerInfo", serverInfo);
        
        // Example placeholder output replacements
        Map<String, String> vanishedReplacements = new HashMap<>();
        vanishedReplacements.put("yes", "&7| Vanished");
        vanishedReplacements.put("no", "");
        placeholderOutputReplacements.put("%essentials_vanished%", vanishedReplacements);
    }
    
    // Getters and setters for all configuration options
    
    public boolean isHeaderFooterEnabled() { return headerFooterEnabled; }
    public void setHeaderFooterEnabled(boolean headerFooterEnabled) { this.headerFooterEnabled = headerFooterEnabled; }
    
    public List<String> getDefaultHeaders() { return new ArrayList<>(defaultHeaders); }
    public void setDefaultHeaders(List<String> defaultHeaders) { this.defaultHeaders = new ArrayList<>(defaultHeaders); }
    
    public List<String> getDefaultFooters() { return new ArrayList<>(defaultFooters); }
    public void setDefaultFooters(List<String> defaultFooters) { this.defaultFooters = new ArrayList<>(defaultFooters); }
    
    public String getHeaderFooterDisableCondition() { return headerFooterDisableCondition; }
    public void setHeaderFooterDisableCondition(String condition) { this.headerFooterDisableCondition = condition; }
    
    public boolean hasPerWorldHeaders(String world) { return perWorldHeaders.containsKey(world); }
    public List<String> getPerWorldHeaders(String world) { return perWorldHeaders.getOrDefault(world, defaultHeaders); }
    public void setPerWorldHeaders(String world, List<String> headers) { perWorldHeaders.put(world, headers); }
    
    public boolean hasPerWorldFooters(String world) { return perWorldFooters.containsKey(world); }
    public List<String> getPerWorldFooters(String world) { return perWorldFooters.getOrDefault(world, defaultFooters); }
    public void setPerWorldFooters(String world, List<String> footers) { perWorldFooters.put(world, footers); }
    
    public boolean hasPerServerHeaders(String server) { return perServerHeaders.containsKey(server); }
    public List<String> getPerServerHeaders(String server) { return perServerHeaders.getOrDefault(server, defaultHeaders); }
    public void setPerServerHeaders(String server, List<String> headers) { perServerHeaders.put(server, headers); }
    
    public boolean hasPerServerFooters(String server) { return perServerFooters.containsKey(server); }
    public List<String> getPerServerFooters(String server) { return perServerFooters.getOrDefault(server, defaultFooters); }
    public void setPerServerFooters(String server, List<String> footers) { perServerFooters.put(server, footers); }
    
    public boolean hasGroupHeaders(String group) { return groupHeaders.containsKey(group); }
    public List<String> getGroupHeaders(String group) { return groupHeaders.getOrDefault(group, defaultHeaders); }
    public void setGroupHeaders(String group, List<String> headers) { groupHeaders.put(group, headers); }
    
    public boolean hasGroupFooters(String group) { return groupFooters.containsKey(group); }
    public List<String> getGroupFooters(String group) { return groupFooters.getOrDefault(group, defaultFooters); }
    public void setGroupFooters(String group, List<String> footers) { groupFooters.put(group, footers); }
    
    public boolean isTablistNameFormattingEnabled() { return tablistNameFormattingEnabled; }
    public void setTablistNameFormattingEnabled(boolean enabled) { this.tablistNameFormattingEnabled = enabled; }
    
    public String getTablistNameDisableCondition() { return tablistNameDisableCondition; }
    public void setTablistNameDisableCondition(String condition) { this.tablistNameDisableCondition = condition; }
    
    public boolean isEnableCollision() { return enableCollision; }
    public void setEnableCollision(boolean enableCollision) { this.enableCollision = enableCollision; }
    
    public boolean isInvisibleNametags() { return invisibleNametags; }
    public void setInvisibleNametags(boolean invisibleNametags) { this.invisibleNametags = invisibleNametags; }
    
    public List<String> getSortingTypes() { return new ArrayList<>(sortingTypes); }
    public void setSortingTypes(List<String> sortingTypes) { this.sortingTypes = new ArrayList<>(sortingTypes); }
    
    public boolean isCaseSensitiveSorting() { return caseSensitiveSorting; }
    public void setCaseSensitiveSorting(boolean caseSensitiveSorting) { this.caseSensitiveSorting = caseSensitiveSorting; }
    
    public boolean isCanSeeFriendlyInvisibles() { return canSeeFriendlyInvisibles; }
    public void setCanSeeFriendlyInvisibles(boolean canSeeFriendlyInvisibles) { this.canSeeFriendlyInvisibles = canSeeFriendlyInvisibles; }
    
    public int getGroupPriority(String group) { return groupPriorities.getOrDefault(group, 999); }
    public void setGroupPriority(String group, int priority) { groupPriorities.put(group, priority); }
    
    public Map<String, Integer> getGroupPriorities() { return new HashMap<>(groupPriorities); }
    public void setGroupPriorities(Map<String, Integer> priorities) { this.groupPriorities = new HashMap<>(priorities); }
    
    public boolean isPlayerlistObjectiveEnabled() { return playerlistObjectiveEnabled; }
    public void setPlayerlistObjectiveEnabled(boolean enabled) { this.playerlistObjectiveEnabled = enabled; }
    
    public String getPlayerlistObjectiveValue() { return playerlistObjectiveValue; }
    public void setPlayerlistObjectiveValue(String value) { this.playerlistObjectiveValue = value; }
    
    public String getPlayerlistObjectiveFancyValue() { return playerlistObjectiveFancyValue; }
    public void setPlayerlistObjectiveFancyValue(String value) { this.playerlistObjectiveFancyValue = value; }
    
    public String getPlayerlistObjectiveTitle() { return playerlistObjectiveTitle; }
    public void setPlayerlistObjectiveTitle(String title) { this.playerlistObjectiveTitle = title; }
    
    public String getPlayerlistObjectiveRenderType() { return playerlistObjectiveRenderType; }
    public void setPlayerlistObjectiveRenderType(String type) { this.playerlistObjectiveRenderType = type; }
    
    public String getPlayerlistObjectiveDisableCondition() { return playerlistObjectiveDisableCondition; }
    public void setPlayerlistObjectiveDisableCondition(String condition) { this.playerlistObjectiveDisableCondition = condition; }
    
    public boolean isBelownameObjectiveEnabled() { return belownameObjectiveEnabled; }
    public void setBelownameObjectiveEnabled(boolean enabled) { this.belownameObjectiveEnabled = enabled; }
    
    public String getBelownameObjectiveValue() { return belownameObjectiveValue; }
    public void setBelownameObjectiveValue(String value) { this.belownameObjectiveValue = value; }
    
    public String getBelownameObjectiveTitle() { return belownameObjectiveTitle; }
    public void setBelownameObjectiveTitle(String title) { this.belownameObjectiveTitle = title; }
    
    public String getBelownameObjectiveFancyValue() { return belownameObjectiveFancyValue; }
    public void setBelownameObjectiveFancyValue(String value) { this.belownameObjectiveFancyValue = value; }
    
    public String getBelownameObjectiveFancyValueDefault() { return belownameObjectiveFancyValueDefault; }
    public void setBelownameObjectiveFancyValueDefault(String value) { this.belownameObjectiveFancyValueDefault = value; }
    
    public String getBelownameObjectiveDisableCondition() { return belownameObjectiveDisableCondition; }
    public void setBelownameObjectiveDisableCondition(String condition) { this.belownameObjectiveDisableCondition = condition; }
    
    public boolean isBossBarEnabled() { return bossBarEnabled; }
    public void setBossBarEnabled(boolean enabled) { this.bossBarEnabled = enabled; }
    
    public String getBossBarToggleCommand() { return bossBarToggleCommand; }
    public void setBossBarToggleCommand(String command) { this.bossBarToggleCommand = command; }
    
    public boolean isBossBarRememberToggle() { return bossBarRememberToggle; }
    public void setBossBarRememberToggle(boolean remember) { this.bossBarRememberToggle = remember; }
    
    public boolean isBossBarHiddenByDefault() { return bossBarHiddenByDefault; }
    public void setBossBarHiddenByDefault(boolean hidden) { this.bossBarHiddenByDefault = hidden; }
    
    public Map<String, BossBarConfig> getBossBars() { return new HashMap<>(bossBars); }
    public void setBossBars(Map<String, BossBarConfig> bossBars) { this.bossBars = new HashMap<>(bossBars); }
    
    public BossBarConfig getBossBar(String name) { return bossBars.get(name); }
    public void setBossBar(String name, BossBarConfig config) { bossBars.put(name, config); }
    
    public boolean isLayoutEnabled() { return layoutEnabled; }
    public void setLayoutEnabled(boolean enabled) { this.layoutEnabled = enabled; }
    
    public String getLayoutDirection() { return layoutDirection; }
    public void setLayoutDirection(String direction) { this.layoutDirection = direction; }
    
    public String getLayoutDefaultSkin() { return layoutDefaultSkin; }
    public void setLayoutDefaultSkin(String skin) { this.layoutDefaultSkin = skin; }
    
    public boolean isLayoutEnableRemainingPlayersText() { return layoutEnableRemainingPlayersText; }
    public void setLayoutEnableRemainingPlayersText(boolean enabled) { this.layoutEnableRemainingPlayersText = enabled; }
    
    public String getLayoutRemainingPlayersText() { return layoutRemainingPlayersText; }
    public void setLayoutRemainingPlayersText(String text) { this.layoutRemainingPlayersText = text; }
    
    public int getLayoutEmptySlotPingValue() { return layoutEmptySlotPingValue; }
    public void setLayoutEmptySlotPingValue(int value) { this.layoutEmptySlotPingValue = value; }
    
    public Map<String, LayoutConfig> getLayouts() { return new HashMap<>(layouts); }
    public void setLayouts(Map<String, LayoutConfig> layouts) { this.layouts = new HashMap<>(layouts); }
    
    public boolean isPingSpoofEnabled() { return pingSpoofEnabled; }
    public void setPingSpoofEnabled(boolean enabled) { this.pingSpoofEnabled = enabled; }
    
    public int getPingSpoofValue() { return pingSpoofValue; }
    public void setPingSpoofValue(int value) { this.pingSpoofValue = value; }
    
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String format) { this.dateFormat = format; }
    
    public String getTimeFormat() { return timeFormat; }
    public void setTimeFormat(String format) { this.timeFormat = format; }
    
    public int getTimeOffset() { return timeOffset; }
    public void setTimeOffset(int offset) { this.timeOffset = offset; }
    
    public boolean isRegisterTabExpansion() { return registerTabExpansion; }
    public void setRegisterTabExpansion(boolean register) { this.registerTabExpansion = register; }
    
    public long getPlaceholderRefreshInterval(String placeholder) {
        return placeholderRefreshIntervals.getOrDefault(placeholder, defaultRefreshInterval);
    }
    
    public void setPlaceholderRefreshInterval(String placeholder, long interval) {
        placeholderRefreshIntervals.put(placeholder, interval);
    }
    
    public long getDefaultRefreshInterval() { return defaultRefreshInterval; }
    public void setDefaultRefreshInterval(long interval) { this.defaultRefreshInterval = interval; }
    
    public Map<String, Map<String, String>> getPlaceholderOutputReplacements() {
        return new HashMap<>(placeholderOutputReplacements);
    }
    
    public void setPlaceholderOutputReplacements(Map<String, Map<String, String>> replacements) {
        this.placeholderOutputReplacements = new HashMap<>(replacements);
    }
    
    public Map<String, ConditionalPlaceholder> getConditions() { return new HashMap<>(conditions); }
    public void setConditions(Map<String, ConditionalPlaceholder> conditions) { this.conditions = new HashMap<>(conditions); }
    
    public long getUpdateInterval() { return updateInterval; }
    public void setUpdateInterval(long interval) { this.updateInterval = interval; }
    
    public boolean isPreventSpectatorEffect() { return preventSpectatorEffect; }
    public void setPreventSpectatorEffect(boolean prevent) { this.preventSpectatorEffect = prevent; }
    
    // Configuration classes for complex structures
    
    public static class BossBarConfig {
        private String style = "PROGRESS";
        private String color = "BLUE";
        private String progress = "100";
        private String text = "";
        private String displayCondition = "";
        
        // Getters and setters
        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getProgress() { return progress; }
        public void setProgress(String progress) { this.progress = progress; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getDisplayCondition() { return displayCondition; }
        public void setDisplayCondition(String displayCondition) { this.displayCondition = displayCondition; }
    }
    
    public static class LayoutConfig {
        private List<String> fixedSlots = new ArrayList<>();
        private Map<String, GroupConfig> groups = new HashMap<>();
        
        // Getters and setters
        public List<String> getFixedSlots() { return new ArrayList<>(fixedSlots); }
        public void setFixedSlots(List<String> slots) { this.fixedSlots = new ArrayList<>(slots); }
        
        public Map<String, GroupConfig> getGroups() { return new HashMap<>(groups); }
        public void setGroups(Map<String, GroupConfig> groups) { this.groups = new HashMap<>(groups); }
        
        public static class GroupConfig {
            private String condition = "";
            private List<String> slots = new ArrayList<>();
            
            public String getCondition() { return condition; }
            public void setCondition(String condition) { this.condition = condition; }
            
            public List<String> getSlots() { return new ArrayList<>(slots); }
            public void setSlots(List<String> slots) { this.slots = new ArrayList<>(slots); }
        }
    }
    
    public static class ConditionalPlaceholder {
        private List<String> conditions = new ArrayList<>();
        private String yes = "";
        private String no = "";
        
        public List<String> getConditions() { return new ArrayList<>(conditions); }
        public void setConditions(List<String> conditions) { this.conditions = new ArrayList<>(conditions); }
        
        public String getYes() { return yes; }
        public void setYes(String yes) { this.yes = yes; }
        
        public String getNo() { return no; }
        public void setNo(String no) { this.no = no; }
    }
}
