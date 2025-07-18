package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Tablist configuration for NeoEssentials
 */
public class TablistConfig {
    public boolean enabled = true;
    public boolean enableHeader = true;
    public boolean enableFooter = true;
    public boolean enableBelowName = true;
    public boolean enablePlayerlistObjective = true;
    public boolean enableSorting = true;
    public boolean enableAnimations = true;
    public boolean enableRGBSupport = true;
    public boolean enablePerWorldPlayerlist = false;
    public int updateInterval = 5; // seconds
    public int animationSpeed = 20; // ticks
    
    // Header/Footer settings
    public String headerText = "&6Welcome to {SERVER_NAME}";
    public String footerText = "&7Players: {ONLINE_PLAYERS}/{MAX_PLAYERS}";
    
    // Below name settings
    public String belowNameTitle = "Health";
    public String belowNameValue = "&c{HEALTH}";
    
    // Playerlist objective settings
    public String playerlistObjectiveTitle = "Ping";
    public String playerlistObjectiveValue = "&7{PING}ms";
    
    // Group settings
    public Map<String, TablistGroup> groups = new HashMap<>();
    
    public static TablistConfig createDefault() {
        TablistConfig config = new TablistConfig();
        
        // Create default groups
        TablistGroup adminGroup = new TablistGroup();
        adminGroup.priority = 1;
        adminGroup.prefix = "&4[Admin] ";
        adminGroup.suffix = "";
        adminGroup.nameColor = "&4";
        config.groups.put("admin", adminGroup);
        
        TablistGroup vipGroup = new TablistGroup();
        vipGroup.priority = 2;
        vipGroup.prefix = "&6[VIP] ";
        vipGroup.suffix = "";
        vipGroup.nameColor = "&6";
        config.groups.put("vip", vipGroup);
        
        TablistGroup defaultGroup = new TablistGroup();
        defaultGroup.priority = 3;
        defaultGroup.prefix = "&7";
        defaultGroup.suffix = "";
        defaultGroup.nameColor = "&f";
        config.groups.put("default", defaultGroup);
        
        return config;
    }
    
    public static class TablistGroup {
        public int priority = 100;
        public String prefix = "";
        public String suffix = "";
        public String nameColor = "&f";
        public String permission = "";
    }
}
