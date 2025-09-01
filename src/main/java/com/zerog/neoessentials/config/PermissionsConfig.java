package com.zerog.neoessentials.config;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Permissions Configuration for NeoEssentials
 * Represents the permissions.json file structure
 */
public class PermissionsConfig {
    
    public Settings settings = new Settings();
    public Map<String, Group> groups = new HashMap<>();
    
    public PermissionsConfig() {
        // Initialize with default groups
        groups.put("owner", new Group(1000, List.of("*"), "Owner", "&4[OWNER]&r", "", new ArrayList<>()));
        groups.put("admin", new Group(800, List.of("neoessentials.*"), "Admin", "&c[ADMIN]&r", "", List.of("moderator")));
        groups.put("default", new Group(0, List.of("neoessentials.basic"), null, "&7[PLAYER]&r", "", new ArrayList<>()));
    }
    
    public static class Settings {
        public boolean enabled = true;
        public boolean useDiscordRoles = true;
        public boolean enableInheritance = true;
        public boolean enableFTBIntegration = true;
        public String defaultGroup = "default";
    }
    
    public static class Group {
        public int priority;
        public List<String> permissions;
        public String discordRole;
        public String prefix;
        public String suffix;
        public List<String> inheritance;
        
        public Group() {}
        
        public Group(int priority, List<String> permissions, String discordRole, String prefix, String suffix, List<String> inheritance) {
            this.priority = priority;
            this.permissions = permissions;
            this.discordRole = discordRole;
            this.prefix = prefix;
            this.suffix = suffix;
            this.inheritance = inheritance;
        }
    }
}
