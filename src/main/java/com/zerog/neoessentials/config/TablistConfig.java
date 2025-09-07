package com.zerog.neoessentials.config;

import com.zerog.neoessentials.util.DebugUtil;
import java.util.*;

/**
 * Unified Display Configuration for Tablist, Scoreboard, and Bossbar
 * Supports multiline layouts with FTB integration
 */
public class TablistConfig {
    
    /**
     * Ensures all required defaults are initialized
     * Call this after loading config from JSON to fix missing defaults
     */
    public void ensureDefaults() {
        if (tablist != null) {
            tablist.ensureDefaultLayouts();
        }
    }

    // Legacy fields for backward compatibility
    @Deprecated
    public String tablistFormat = "[{group}] {player_name} | Ping: {ping}";
    @Deprecated
    public String scoreboardFormat = "Score: {score} | Player: {player_name}";
    @Deprecated
    public String bossbarFormat = "Boss: {bossbar} | {message} [{progress}%]";
    @Deprecated
    public boolean enableScoreboard = true;
    @Deprecated
    public boolean enableBossbar = true;
    @Deprecated
    public boolean enableTablist = true;
    
    // New unified configuration structure
    public TablistSection tablist = new TablistSection();
    public ScoreboardSection scoreboard = new ScoreboardSection();
    public BossbarSection bossbar = new BossbarSection();
    public AnimationSection animations = new AnimationSection();
    public DiscordIntegration discordIntegration = new DiscordIntegration();
    public java.util.Map<String, String> conditional_placeholders = new java.util.HashMap<>();

    // Legacy compatibility fields
    public String showTo = "all";
    public int priority = 1;
    public boolean showHeaderFooter = true;
    public double footerInterval = 5.0;
    public String layout = "DYNAMIC_SIZE";
    public boolean enableNametag = true;
    public int size = 60;
    public java.util.Map<String, PermSet> PermSets = new java.util.HashMap<>();
    public java.util.Map<String, FilterSet> filter = new java.util.HashMap<>();
    public String teamFiltersOrder = null;
    
    // Legacy tablist layouts for backward compatibility
    @Deprecated
    public java.util.List<TablistLayout> tablistLayouts = new java.util.ArrayList<>();

    /**
     * Tablist configuration section
     */
    public static class TablistSection {
        public boolean enabled = true;
        public int updateInterval = 60; // Reduced frequency - only for animations now
        public String format = "{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix}";
        
        // New permission-based system
        public java.util.Map<String, PermissionSet> permissionSets = new java.util.HashMap<>();
        public java.util.Map<String, Layout> layouts = new java.util.HashMap<>();
        public FTBIntegration ftbIntegration = new FTBIntegration();
        public PermissionSetIntegration permissionSetIntegration = new PermissionSetIntegration();
        
        public java.util.List<PlayerOrder> playerOrder = new java.util.ArrayList<>();
        
        public TablistSection() {
            DebugUtil.debugLog("[TablistConfig] TablistSection constructor called!");
            // Initialize default permission sets
            initializeDefaultPermissionSets();
            initializeDefaultLayouts();
            DebugUtil.debugLog("[TablistConfig] TablistSection constructor completed - layouts count: " + 
                (layouts != null ? layouts.size() : "null"));
        }
        
        /**
         * Ensures required layouts exist after deserialization
         * Called to fix configs loaded from JSON that bypass constructor
         */
        public void ensureDefaultLayouts() {
            DebugUtil.debugLog("[TablistConfig] ensureDefaultLayouts() called");
            if (layouts == null) {
                layouts = new LinkedHashMap<>();
                DebugUtil.debugLog("[TablistConfig] layouts was null, created new LinkedHashMap");
            }
            
            if (permissionSets == null) {
                permissionSets = new LinkedHashMap<>();
                initializeDefaultPermissionSets();
                DebugUtil.debugLog("[TablistConfig] permissionSets was null, initialized defaults");
            }
            
            // Check if we have the required layouts, if not, create them
            String[] requiredLayouts = {"default_layout", "vip_layout", "owner_layout", "admin_layout", 
                                      "moderator_layout", "helper_layout", "member_layout", "verified_layout"};
            
            int createdCount = 0;
            for (String layoutId : requiredLayouts) {
                if (!layouts.containsKey(layoutId)) {
                    switch (layoutId) {
                        case "default_layout": layouts.put(layoutId, createDefaultLayout()); break;
                        case "vip_layout": layouts.put(layoutId, createVipLayout()); break;
                        case "owner_layout": layouts.put(layoutId, createOwnerLayout()); break;
                        case "admin_layout": layouts.put(layoutId, createAdminLayout()); break;
                        case "moderator_layout": layouts.put(layoutId, createModeratorLayout()); break;
                        case "helper_layout": layouts.put(layoutId, createHelperLayout()); break;
                        case "member_layout": layouts.put(layoutId, createMemberLayout()); break;
                        case "verified_layout": layouts.put(layoutId, createVerifiedLayout()); break;
                    }
                    createdCount++;
                }
            }
            
            DebugUtil.debugLog("[TablistConfig] ensureDefaultLayouts() completed - created " + createdCount + 
                               " missing layouts, total layouts: " + layouts.size());
        }
        
        private void initializeDefaultPermissionSets() {
            permissionSets.put("owner", new PermissionSet(1000, "neoessentials.tablist.owner", "permission", "owner_layout"));
            permissionSets.put("admin", new PermissionSet(900, "neoessentials.tablist.admin", "permission", "admin_layout"));
            permissionSets.put("moderator", new PermissionSet(800, "neoessentials.tablist.moderator", "permission", "moderator_layout"));
            permissionSets.put("helper", new PermissionSet(700, "neoessentials.tablist.helper", "permission", "helper_layout"));
            permissionSets.put("vip", new PermissionSet(600, "neoessentials.tablist.vip", "permission", "vip_layout"));
            permissionSets.put("member", new PermissionSet(500, "neoessentials.tablist.member", "permission", "member_layout"));
            permissionSets.put("verified", new PermissionSet(400, "neoessentials.tablist.verified", "permission", "verified_layout"));
            permissionSets.put("default", new PermissionSet(0, "", "default", "default_layout"));
        }
        
        private void initializeDefaultLayouts() {
            DebugUtil.debugLog("[TablistConfig] initializeDefaultLayouts() called");
            layouts.put("default_layout", createDefaultLayout());
            layouts.put("vip_layout", createVipLayout());
            layouts.put("owner_layout", createOwnerLayout());
            layouts.put("admin_layout", createAdminLayout());
            layouts.put("moderator_layout", createModeratorLayout());
            layouts.put("helper_layout", createHelperLayout());
            layouts.put("member_layout", createMemberLayout());
            layouts.put("verified_layout", createVerifiedLayout());
            DebugUtil.debugLog("[TablistConfig] initializeDefaultLayouts() completed - created " + layouts.size() + " layouts");
        }
        
        private Layout createDefaultLayout() {
            Layout layout = new Layout();
            layout.priority = 0;
            layout.conditionType = "default";
            layout.condition = "";
            layout.header = java.util.Arrays.asList(
                "&6&l╔═══════════════════════════════════╗",
                "&6&l║         &f&lNeoEssentials         &6&l║",
                "&6&l║ &7Welcome &e{player_name}           &6&l║",
                "&6&l║              &e{player_health_bar}           &6&l║",
                "&6&l╚═══════════════════════════════════╝"
            );
            layout.footer = java.util.Arrays.asList(
                "&6&l╔═══════════════════════════════════╗",
                "&6&l║ &7Online: &e{server_players}&7/&e{server_max_players}              &6&l║",
                "&6&l║ &7Time: &f{time}                   &6&l║",
                "&6&l╚═══════════════════════════════════╝"
            );
            return layout;
        }
        
        private Layout createVipLayout() {
            Layout layout = new Layout();
            layout.priority = 600;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.vip";
            layout.header = java.util.Arrays.asList(
                "&d&l════════════════════════════════════════════════",
                "&d&l║                &f&lVIP PANEL                  &d&l║",
                "&d&l║            &e&lNEOESSENTIALS SERVER            &d&l║",
                "&d&l════════════════════════════════════════════════",
                "",
                "&f💎 &7VIP: &d&l{player_name} &5[VIP]",
                "&f💎 &7Rank: &e{ftb_rank_display_name} &7| &bTeam: &3{ftb_team_display_name}",
                "&f❤️ &7Health: &c{player_health}&7/&c{player_max_health} &7| &f🍖 Food: &6{player_food}",
                "&f📍 &7Location: &a{player_x}&7, &a{player_y}&7, &a{player_z} &7in &e{player_world}",
                "&f⚡ &7Ping: &{ping_colored}{player_ping}ms &7| &fLevel: &a{player_level}",
                "",
                "&f🌟 &7VIP Perks Active:",
                "&f├─ &7Players Online: &a{server_players}&7/&a{server_max_players}",
                "&f└─ &7Server TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}"
            );
            layout.footer = java.util.Arrays.asList(
                "&d&l════════════════════════════════════════════════",
                "&f🔗 &d&lVIP FEATURES &7&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&f├─ &7Kit Access: &d/kit vip",
                "&f├─ &7Homes: &d{player_homes}/10",
                "&f└─ &7VIP Chat: &d/vipc",
                "",
                "&f⏰ &7Current Time: &f{datetime}",
                "&d&l════════════════════════════════════════════════"
            );
            return layout;
        }
        
        private Layout createOwnerLayout() {
            Layout layout = new Layout();
            layout.priority = 1000;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.owner";
            layout.header = java.util.Arrays.asList(
                "&4&l════════════════════════════════════════",
                "&4&l║              &f&lOWNER PANEL              &4&l║",
                "&4&l║            &e&lNEOESSENTIALS SERVER            &4&l║",
                "&4&l════════════════════════════════════════",
                "",
                "&f👑 &7Owner: &4&l{player_name} &c[OWNER]",
                "&f👑 &7Rank: &e{ftb_rank_display_name} &7| &bTeam: &3{ftb_team_display_name}",
                "&f❤️ &7Health: &c{player_health}&7/&c{player_max_health} &7| &f🍖 Food: &6{player_food}",
                "&f📍 &7Location: &a{player_x}&7, &a{player_y}&7, &a{player_z} &7in &e{player_world}",
                "&f⚡ &7Ping: &{ping_colored}{player_ping}ms &7| &fLevel: &a{player_level}",
                "",
                "&f🔧 &7Server Management:",
                "&f├─ &7Players Online: &a{server_players}&7/&a{server_max_players}",
                "&f├─ &7Server TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}",
                "&f└─ &7Memory Usage: &b{server_memory_percent}%"
            );
            layout.footer = java.util.Arrays.asList(
                "&4&l════════════════════════════════════════",
                "&f🎖️ &4&lOWNER PRIVILEGES &7&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&f├─ &7Full Server Access",
                "&f├─ &7All Commands Available",
                "&f└─ &7Server Management Tools",
                "",
                "&f⏰ &7Current Time: &f{datetime}",
                "&4&l════════════════════════════════════════"
            );
            return layout;
        }
        
        private Layout createAdminLayout() {
            Layout layout = new Layout();
            layout.priority = 900;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.admin";
            layout.header = java.util.Arrays.asList(
                "&c&l════════════════════════════════════════",
                "&c&l║              &f&lADMIN PANEL              &c&l║",
                "&c&l║            &e&lNEOESSENTIALS SERVER            &c&l║",
                "&c&l════════════════════════════════════════",
                "",
                "&f⚔️ &7Admin: &c&l{player_name} &c[ADMIN]",
                "&f⚔️ &7Rank: &e{ftb_rank_display_name} &7| &bTeam: &3{ftb_team_display_name}",
                "&f❤️ &7Health: &c{player_health}&7/&c{player_max_health} &7| &f🍖 Food: &6{player_food}",
                "&f📍 &7Location: &a{player_x}&7, &a{player_y}&7, &a{player_z} &7in &e{player_world}",
                "&f⚡ &7Ping: &{ping_colored}{player_ping}ms &7| &fLevel: &a{player_level}",
                "",
                "&f🛡️ &7Admin Tools:",
                "&f├─ &7Players Online: &a{server_players}&7/&a{server_max_players}",
                "&f├─ &7Server TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}",
                "&f└─ &7Uptime: &a{server_uptime}"
            );
            layout.footer = java.util.Arrays.asList(
                "&c&l════════════════════════════════════════",
                "&f🔧 &c&lADMIN TOOLS &7&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&f├─ &7Admin Commands: &c/admin",
                "&f├─ &7Player Management: &c/kick /ban",
                "&f└─ &7Server Control: &c/stop /restart",
                "",
                "&f⏰ &7Current Time: &f{datetime}",
                "&c&l════════════════════════════════════════"
            );
            return layout;
        }
        
        private Layout createModeratorLayout() {
            Layout layout = new Layout();
            layout.priority = 800;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.moderator";
            layout.header = java.util.Arrays.asList(
                "&6&l════════════════════════════════════════",
                "&6&l║            &f&lMODERATOR PANEL            &6&l║",
                "&6&l║            &e&lNEOESSENTIALS SERVER            &6&l║",
                "&6&l════════════════════════════════════════",
                "",
                "&f🛡️ &7Moderator: &6&l{player_name} &6[MOD]",
                "&f🛡️ &7Rank: &e{ftb_rank_display_name} &7| &bTeam: &3{ftb_team_display_name}",
                "&f❤️ &7Health: &c{player_health}&7/&c{player_max_health} &7| &f🍖 Food: &6{player_food}",
                "&f📍 &7Location: &a{player_x}&7, &a{player_y}&7, &a{player_z} &7in &e{player_world}",
                "&f⚡ &7Ping: &{ping_colored}{player_ping}ms &7| &fLevel: &a{player_level}",
                "",
                "&f👮 &7Mod Powers:",
                "&f├─ &7Players Online: &a{server_players}&7/&a{server_max_players}",
                "&f└─ &7Server TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}"
            );
            layout.footer = java.util.Arrays.asList(
                "&6&l════════════════════════════════════════",
                "&f⚡ &6&lMODERATOR TOOLS &7&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&f├─ &7Player Moderation: &6/warn /mute",
                "&f├─ &7Chat Management: &6/clearchat",
                "&f└─ &7Player Help: &6/tp /heal",
                "",
                "&f⏰ &7Current Time: &f{datetime}",
                "&6&l════════════════════════════════════════"
            );
            return layout;
        }
        
        private Layout createHelperLayout() {
            Layout layout = new Layout();
            layout.priority = 700;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.helper";
            layout.header = java.util.Arrays.asList(
                "&b&l════════════════════════════════════════",
                "&b&l║             &f&lHELPER PANEL             &b&l║",
                "&b&l║            &e&lNEOESSENTIALS SERVER            &b&l║",
                "&b&l════════════════════════════════════════",
                "",
                "&f🤝 &7Helper: &b&l{player_name} &b[HELPER]",
                "&f🤝 &7Rank: &e{ftb_rank_display_name} &7| &bTeam: &3{ftb_team_display_name}",
                "&f❤️ &7Health: &c{player_health}&7/&c{player_max_health} &7| &f🍖 Food: &6{player_food}",
                "&f📍 &7Location: &a{player_x}&7, &a{player_y}&7, &a{player_z} &7in &e{player_world}",
                "&f⚡ &7Ping: &{ping_colored}{player_ping}ms &7| &fLevel: &a{player_level}",
                "",
                "&f💡 &7Helper Status:",
                "&f├─ &7Players Online: &a{server_players}&7/&a{server_max_players}",
                "&f└─ &7Server TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}"
            );
            layout.footer = java.util.Arrays.asList(
                "&b&l════════════════════════════════════════",
                "&f🆘 &b&lHELPER TOOLS &7&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&f├─ &7Player Assistance: &b/help /guide",
                "&f├─ &7Basic Commands: &b/tp /spawn",
                "&f└─ &7Support Tools: &b/ticket",
                "",
                "&f⏰ &7Current Time: &f{datetime}",
                "&b&l════════════════════════════════════════"
            );
            return layout;
        }
        
        private Layout createMemberLayout() {
            Layout layout = new Layout();
            layout.priority = 500;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.member";
            layout.header = java.util.Arrays.asList(
                "&a&l════════════════════════════════════════",
                "&a&l║             &f&lMEMBER PANEL             &a&l║",
                "&a&l║            &e&lNEOESSENTIALS SERVER            &a&l║",
                "&a&l════════════════════════════════════════",
                "",
                "&f🎖️ &7Member: &a&l{player_name} &a[MEMBER]",
                "&f🎖️ &7Rank: &e{ftb_rank_display_name} &7| &bTeam: &3{ftb_team_display_name}",
                "&f❤️ &7Health: &c{player_health}&7/&c{player_max_health} &7| &f🍖 Food: &6{player_food}",
                "&f📍 &7Location: &a{player_x}&7, &a{player_y}&7, &a{player_z} &7in &e{player_world}",
                "&f⚡ &7Ping: &{ping_colored}{player_ping}ms &7| &fLevel: &a{player_level}",
                "",
                "&f🌟 &7Member Benefits:",
                "&f├─ &7Players Online: &a{server_players}&7/&a{server_max_players}",
                "&f└─ &7Server TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}"
            );
            layout.footer = java.util.Arrays.asList(
                "&a&l════════════════════════════════════════",
                "&f🏡 &a&lMEMBER PERKS &7&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&f├─ &7Home Commands: &a/home /sethome",
                "&f├─ &7Member Kit: &a/kit member",
                "&f└─ &7Member Chat: &a/mc",
                "",
                "&f⏰ &7Current Time: &f{datetime}",
                "&a&l════════════════════════════════════════"
            );
            return layout;
        }
        
        private Layout createVerifiedLayout() {
            Layout layout = new Layout();
            layout.priority = 400;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.verified";
            layout.header = java.util.Arrays.asList(
                "&7&l════════════════════════════════════════",
                "&7&l║            &f&lVERIFIED PANEL            &7&l║",
                "&7&l║            &e&lNEOESSENTIALS SERVER            &7&l║",
                "&7&l════════════════════════════════════════",
                "",
                "&f✅ &7Verified: &7&l{player_name} &7[VERIFIED]",
                "&f✅ &7Rank: &e{ftb_rank_display_name} &7| &bTeam: &3{ftb_team_display_name}",
                "&f❤️ &7Health: &c{player_health}&7/&c{player_max_health} &7| &f🍖 Food: &6{player_food}",
                "&f📍 &7Location: &a{player_x}&7, &a{player_y}&7, &a{player_z} &7in &e{player_world}",
                "&f⚡ &7Ping: &{ping_colored}{player_ping}ms &7| &fLevel: &a{player_level}",
                "",
                "&f🔐 &7Verified Status:",
                "&f├─ &7Players Online: &a{server_players}&7/&a{server_max_players}",
                "&f└─ &7Server TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}"
            );
            layout.footer = java.util.Arrays.asList(
                "&7&l════════════════════════════════════════",
                "&f🔒 &7&lVERIFIED ACCESS &7&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "&f├─ &7Basic Commands: &7/spawn /warp",
                "&f├─ &7Chat Access: &7Global Chat",
                "&f└─ &7Build Permission: &7Protected Areas",
                "",
                "&f⏰ &7Current Time: &f{datetime}",
                "&7&l════════════════════════════════════════"
            );
            return layout;
        }
    }

    /**
     * Permission Set Configuration for new system
     */
    public static class PermissionSet {
        public int priority;
        public String permission;
        public String conditionType;
        public String layoutId;
        
        public PermissionSet() {}
        
        public PermissionSet(int priority, String permission, String conditionType, String layoutId) {
            this.priority = priority;
            this.permission = permission;
            this.conditionType = conditionType;
            this.layoutId = layoutId;
        }
    }
    
    /**
     * FTB Integration Configuration
     */
    public static class FTBIntegration {
        public boolean enabled = true;
        public int updateInterval = 30;
        public boolean syncWithPermissionSets = true;
        public boolean teamColorForPrefix = true;
        public String rankPrefixFormat = "[{rank_display_name}]";
        public String teamPrefixFormat = "{team_color}[{team_display_name}]";
        public String suffixFormat = "";
        public String combinedFormat = "{ftb_rank_prefix} {ftb_team_prefix}";
        public java.util.Map<String, PermissionMapping> permissionMappings = new java.util.HashMap<>();
        
        public FTBIntegration() {
            // Initialize default FTB permission mappings
            permissionMappings.put("ftb_admin", new PermissionMapping("admin", 900));
            permissionMappings.put("ftb_moderator", new PermissionMapping("moderator", 800));
            permissionMappings.put("ftb_member", new PermissionMapping("member", 500));
        }
    }
    
    /**
     * Permission mapping for FTB integration
     */
    public static class PermissionMapping {
        public String targetPermissionSet;
        public int priority;
        
        public PermissionMapping() {}
        
        public PermissionMapping(String targetPermissionSet, int priority) {
            this.targetPermissionSet = targetPermissionSet;
            this.priority = priority;
        }
    }
    
    /**
     * Permission Set Integration Configuration
     */
    public static class PermissionSetIntegration {
        public boolean enabled = true;
        public int updateInterval = 300; // Much longer - only for cleanup/validation
        public boolean syncWithDiscord = true;
        public boolean syncWithFTB = true;
        public boolean priorityBasedSelection = true;
        public boolean fallbackToDefault = true;
        public boolean debugMode = false;
        public boolean eventDrivenUpdates = true; // New: prefer event-based updates
        public boolean animationUpdatesOnly = true; // New: only animate specific placeholders
    }

    /**
     * Scoreboard configuration section
     */
    public static class ScoreboardSection {
        public boolean enabled = true;
        public int updateInterval = 20;
        public int maxLines = 15;
        public String title = "&6&lNeoEssentials";
        public java.util.Map<String, Layout> layouts = new java.util.HashMap<>();
        public TitleAnimations titleAnimations = new TitleAnimations();
        public AnimationConfig animations = new AnimationConfig();
        public java.util.Map<String, String> conditional_logic = new java.util.HashMap<>();
        
        public ScoreboardSection() {
            // Initialize default scoreboard layouts
            initializeDefaultScoreboardLayouts();
        }
        
        private void initializeDefaultScoreboardLayouts() {
            layouts.put("default_scoreboard", createDefaultScoreboardLayout());
            layouts.put("vip_scoreboard", createVipScoreboardLayout());
            layouts.put("admin_scoreboard", createAdminScoreboardLayout());
        }
        
        private Layout createDefaultScoreboardLayout() {
            Layout layout = new Layout();
            layout.priority = 0;
            layout.conditionType = "default";
            layout.condition = "";
            layout.title = "&e&lNeoEssentials";
            layout.lines = java.util.Arrays.asList(
                "",
                "&7Player: &f{player_name}",
                "&7Health: &c{player_health}&7/&c{player_max_health}",
                "&7Level: &a{player_level}",
                "&7Ping: &{ping_colored}{player_ping}ms",
                "",
                "&7Players: &a{server_players}&7/&a{server_max_players}",
                "&7TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}",
                "",
                "&7Time: &f{time}",
                ""
            );
            return layout;
        }
        
        private Layout createVipScoreboardLayout() {
            Layout layout = new Layout();
            layout.priority = 600;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.vip";
            layout.title = "&d&lVIP &5&lEssentials";
            layout.lines = java.util.Arrays.asList(
                "",
                "&7VIP: &d&l{player_name}",
                "&7Rank: &e{ftb_rank_display_name}",
                "&7Team: &3{ftb_team_display_name}",
                "&7Health: &c{player_health}&7/&c{player_max_health}",
                "&7Level: &a{player_level}",
                "&7Ping: &{ping_colored}{player_ping}ms",
                "",
                "&7Players: &a{server_players}&7/&a{server_max_players}",
                "&7TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}",
                "&7Memory: &b{server_memory_percent}%",
                "",
                "&7Time: &f{time}",
                "&7Balance: &a${player_balance}",
                ""
            );
            return layout;
        }
        
        private Layout createAdminScoreboardLayout() {
            Layout layout = new Layout();
            layout.priority = 900;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.admin";
            layout.title = "&c&lAdmin &f&lPanel";
            layout.lines = java.util.Arrays.asList(
                "",
                "&7Admin: &c&l{player_name}",
                "&7Rank: &e{ftb_rank_display_name}",
                "&7Team: &3{ftb_team_display_name}",
                "",
                "&7Server Management:",
                "&7├ Players: &a{server_players}&7/&a{server_max_players}",
                "&7├ TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}",
                "&7├ Memory: &b{server_memory_percent}%",
                "&7└ Uptime: &a{server_uptime}",
                "",
                "&7Time: &f{time}",
                ""
            );
            return layout;
        }
    }

    /**
     * Bossbar configuration section
     */
    public static class BossbarSection {
        public boolean enabled = true;
        public int updateInterval = 20;
        public java.util.Map<String, BossbarLayout> layouts = new java.util.HashMap<>();
        
        public BossbarSection() {
            // Initialize default bossbar layouts
            initializeDefaultBossbarLayouts();
        }
        
        private void initializeDefaultBossbarLayouts() {
            layouts.put("default_bossbar", createDefaultBossbarLayout());
            layouts.put("vip_bossbar", createVipBossbarLayout());
            layouts.put("staff_bossbar", createStaffBossbarLayout());
        }
        
        private BossbarLayout createDefaultBossbarLayout() {
            BossbarLayout layout = new BossbarLayout();
            layout.priority = 0;
            layout.conditionType = "default";
            layout.condition = "";
            layout.message = "&eWelcome to NeoEssentials! &7| &fOnline: &a{server_players}&7/&a{server_max_players}";
            layout.color = "YELLOW";
            layout.style = "SOLID";
            layout.progress = 1.0;
            return layout;
        }
        
        private BossbarLayout createVipBossbarLayout() {
            BossbarLayout layout = new BossbarLayout();
            layout.priority = 600;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.vip";
            layout.message = "&d&lVIP Status Active! &7| &fServer TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}";
            layout.color = "PURPLE";
            layout.style = "SEGMENTED_6";
            layout.progress = 1.0;
            return layout;
        }
        
        private BossbarLayout createStaffBossbarLayout() {
            BossbarLayout layout = new BossbarLayout();
            layout.priority = 800;
            layout.conditionType = "permission";
            layout.condition = "neoessentials.tablist.moderator";
            layout.message = "&6&lStaff Panel Active &7| &fMemory: &b{server_memory_percent}% &7| &fTPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}";
            layout.color = "BLUE";
            layout.style = "SEGMENTED_10";
            layout.progress = 1.0;
            return layout;
        }
    }

    /**
     * Animation configuration section
     */
    public static class AnimationSection {
        public boolean enabled = true;
        public int updateInterval = 5;
        public java.util.List<AnimationSequence> sequences = new java.util.ArrayList<>();
    }

    /**
     * Generic layout for tablist and scoreboard
     */
    public static class Layout {
        public int priority = 1;
        public String conditionType = "default";
        public String condition = "";
        public java.util.List<String> header = new java.util.ArrayList<>();
        public java.util.List<String> footer = new java.util.ArrayList<>();
        public java.util.List<String> lines = new java.util.ArrayList<>(); // For scoreboard
        public String title = ""; // For scoreboard
        public String format = ""; // For player formatting
    }

    /**
     * Bossbar-specific layout
     */
    public static class BossbarLayout {
        public int priority = 1;
        public String conditionType = "default";
        public String condition = "";
        public String message = "";
        public String color = "WHITE";
        public String style = "PROGRESS";
        public double progress = 1.0;
        public java.util.List<BossbarInfo> bars = new java.util.ArrayList<>(); // For backward compatibility
    }

    /**
     * Individual bossbar configuration
     */
    public static class BossbarInfo {
        public String id = "default";
        public String text = "";
        public String color = "WHITE";
        public String style = "PROGRESS";
        public double progress = 1.0;
    }

    /**
     * Player ordering configuration
     */
    public static class PlayerOrder {
        public String placeholder = "";
        public String direction = "asc"; // asc or desc
        public boolean asNumber = false;
    }

    /**
     * Animation sequence configuration
     */
    public static class AnimationSequence {
        public String id = "";
        public java.util.List<String> frames = new java.util.ArrayList<>();
        public double duration = 1.0;
    }

    /**
     * Title animations configuration for scoreboard
     */
    public static class TitleAnimations {
        public boolean enabled = true;
        public java.util.List<String> frames = new java.util.ArrayList<>();
        public double duration = 1.0;
    }

    /**
     * Animation configuration for scoreboard
     */
    public static class AnimationConfig {
        public boolean enabled = true;
        public int updateInterval = 10;
        public java.util.List<AnimationSequence> sequences = new java.util.ArrayList<>();
    }

    // Legacy classes for backward compatibility
    @Deprecated
    public static class TablistLayout {
        public int priority = 1;
        public String conditionType = "default";
        public String condition = "";
        public java.util.List<String> header = new java.util.ArrayList<>();
        public java.util.List<String> footer = new java.util.ArrayList<>();
        public String format = "%player%";
        public String color = "";
        public String prefix = "";
        public String suffix = "";
    }

    @Deprecated
    public static class PermSet {
        public Condition condition = new Condition();
        public String permission;
        public LegacyTablistSection tablist = new LegacyTablistSection();
    }

    @Deprecated
    public static class Condition {
        public String type = "all";
        public String value = "";
    }

    @Deprecated
    public static class FilterSet {
        public String filter;
    }

    @Deprecated
    public static class LegacyTablistSection {
        public java.util.List<String> header = new java.util.ArrayList<>();
        public java.util.List<String> footer = new java.util.ArrayList<>();
    }

    @Deprecated
    public static class Placeholder {
        public String type = "conditional";
        public String condition = "";
        public String trueValue = "";
        public String falseValue = "";

        public Placeholder() {}
        public Placeholder(String type, String condition, String trueValue, String falseValue) {
            this.type = type;
            this.condition = condition;
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }
    }

    @Deprecated
    public static class PlayerSet {
        public String filter = "";
        public java.util.List<String> header = null;
        public java.util.List<String> footer = null;

        public PlayerSet() {}
        public PlayerSet(String filter) {
            this.filter = filter;
        }
        public PlayerSet(String filter, java.util.List<String> header, java.util.List<String> footer) {
            this.filter = filter;
            this.header = header;
            this.footer = footer;
        }
    }

    @Deprecated
    public static class Component {
        public String text = "";
        public String icon = "";
        public int ping = 0;
        public boolean animated = false;
        public double interval = 0.0;
        public java.util.List<String> frames = new java.util.ArrayList<>();

        public Component() {}
        public Component(String text, String icon, int ping, boolean animated, double interval, java.util.List<String> frames) {
            this.text = text;
            this.icon = icon;
            this.ping = ping;
            this.animated = animated;
            this.interval = interval;
            this.frames = frames;
        }
    }

    /**
     * Discord Integration Configuration
     */
    public static class DiscordIntegration {
        public boolean enabled = true;
        public boolean syncWithPermissionSets = true;
        public int updateInterval = 60;
        public java.util.Map<String, DiscordRoleMapping> roleMappings = new java.util.HashMap<>();
        public String fallbackPermissionSet = "default";
        public MessageFormatting messageFormatting = new MessageFormatting();
        public Notifications notifications = new Notifications();
        public RoleSync roleSync = new RoleSync();
        public ChatSync chatSync = new ChatSync();
        public StatusUpdates statusUpdates = new StatusUpdates();
        public Webhooks webhooks = new Webhooks();
        public ErrorHandling errorHandling = new ErrorHandling();
        
        public DiscordIntegration() {
            // Initialize default Discord role ID mappings to permission sets
            // Replace these example role IDs with your actual Discord role IDs
            roleMappings.put("1234567890123456789", new DiscordRoleMapping("owner", 1000, "neoessentials.discord.owner"));      // Owner Role ID
            roleMappings.put("1234567890123456790", new DiscordRoleMapping("admin", 900, "neoessentials.discord.admin"));       // Admin Role ID
            roleMappings.put("1234567890123456791", new DiscordRoleMapping("moderator", 800, "neoessentials.discord.moderator")); // Moderator Role ID
            roleMappings.put("1234567890123456792", new DiscordRoleMapping("helper", 700, "neoessentials.discord.helper"));     // Helper Role ID
            roleMappings.put("1234567890123456793", new DiscordRoleMapping("vip", 600, "neoessentials.discord.vip"));           // VIP Role ID
            roleMappings.put("1234567890123456794", new DiscordRoleMapping("member", 500, "neoessentials.discord.member"));     // Member Role ID
            roleMappings.put("1234567890123456795", new DiscordRoleMapping("verified", 400, "neoessentials.discord.verified")); // Verified Role ID
        }
    }
    
    /**
     * Discord Role Mapping to Permission Sets
     */
    public static class DiscordRoleMapping {
        public String targetPermissionSet;
        public int priority;
        public String requiresMinecraftPermission;
        
        public DiscordRoleMapping() {}
        
        public DiscordRoleMapping(String targetPermissionSet, int priority, String requiresMinecraftPermission) {
            this.targetPermissionSet = targetPermissionSet;
            this.priority = priority;
            this.requiresMinecraftPermission = requiresMinecraftPermission;
        }
    }

    public static class MessageFormatting {
        public boolean useEmbeds = true;
        public String timestampFormat = "yyyy-MM-dd HH:mm:ss";
        public boolean includePlayerStats = true;
        public boolean includeTeamInfo = true;
        public boolean includeRankInfo = true;
    }

    public static class Notifications {
        public NotificationConfig tablistUpdates = new NotificationConfig(true, "general", 
            "🔄 **{player_name}** | Tablist updated | Layout: **{layout_name}** | Permission Set: **{permission_set}**");
        public NotificationConfig permissionSetChanges = new NotificationConfig(true, "admin",
            "� **{player_name}** | Permission Set: **{old_set}** → **{new_set}** | Priority: **{priority}**");
        public NotificationConfig playerJoin = new NotificationConfig(true, "general",
            "✅ **{player_name}** joined! | Permission Set: **{permission_set}** | Layout: **{layout_name}**");
        public NotificationConfig scoreboardUpdates = new NotificationConfig(true, "general",
            "📊 **{player_name}** | Scoreboard updated | Layout: **{layout_name}**");
        public NotificationConfig playerLeave = new NotificationConfig(true, "general",
            "❌ **{player_name}** left | Session: **{session_time}** | Permission Set: **{permission_set}**");
        public NotificationConfig teamUpdates = new NotificationConfig(true, "general",
            "👥 **{player_name}** | Team updated | Old: **{old_team}** → New: **{ftb_team_display_name}**");
        public NotificationConfig rankUpdates = new NotificationConfig(true, "general",
            "🎖️ **{player_name}** | Rank updated | Old: **{old_rank}** → New: **{ftb_rank_display_name}**");
        public NotificationConfig permissionChanges = new NotificationConfig(true, "admin",
            "🔐 **{player_name}** | Permission **{permission}** | Action: **{action}** | By: **{admin}**");
        public NotificationConfig achievements = new NotificationConfig(true, "general",
            "🏆 **{player_name}** earned **{achievement}**! | Permission Set: **{permission_set}**");
    }

    public static class NotificationConfig {
        public boolean enabled;
        public String channel;
        public String format;

        public NotificationConfig() {}
        public NotificationConfig(boolean enabled, String channel, String format) {
            this.enabled = enabled;
            this.channel = channel;
            this.format = format;
        }
    }

    public static class RoleSync {
        public boolean enabled = true;
        public boolean syncOnJoin = true;
        public int syncInterval = 300;
        public boolean bidirectional = true;
        public boolean usePermissionSets = true;
        public java.util.Map<String, RoleMapping> roleMappings = new java.util.HashMap<>();
        public RoleMapping fallbackRole = new RoleMapping("neoessentials.default", 0, "&8[GUEST]&r", "&8&lGUEST");

        public RoleSync() {
            // Initialize default role ID mappings (Discord Role ID -> Minecraft permissions)
            // Replace these example role IDs with your actual Discord role IDs
            roleMappings.put("1234567890123456789", new RoleMapping("neoessentials.admin", 1000, "&4[OWNER]&r", "&4&lOWNER"));      // Owner Role ID
            roleMappings.put("1234567890123456790", new RoleMapping("neoessentials.moderator", 800, "&c[ADMIN]&r", "&c&lADMIN"));  // Admin Role ID
            roleMappings.put("1234567890123456791", new RoleMapping("neoessentials.helper", 600, "&6[MOD]&r", "&6&lMODERATOR"));   // Moderator Role ID
            roleMappings.put("1234567890123456792", new RoleMapping("neoessentials.vip", 400, "&d[VIP]&r", "&d&lVIP"));            // VIP Role ID
            roleMappings.put("1234567890123456793", new RoleMapping("neoessentials.member", 200, "&a[MEMBER]&r", "&a&lMEMBER"));   // Member Role ID
            roleMappings.put("1234567890123456794", new RoleMapping("neoessentials.verified", 100, "&7[VERIFIED]&r", "&7&lVERIFIED")); // Verified Role ID
        }
    }

    public static class RoleMapping {
        public String minecraftPermission;
        public int priority;
        public String tablistPrefix;
        public String scoreboardTitle;

        public RoleMapping() {}
        public RoleMapping(String minecraftPermission, int priority, String tablistPrefix, String scoreboardTitle) {
            this.minecraftPermission = minecraftPermission;
            this.priority = priority;
            this.tablistPrefix = tablistPrefix;
            this.scoreboardTitle = scoreboardTitle;
        }
    }

    public static class ChatSync {
        public boolean enabled = true;
        public boolean includeTeamInfo = true;
        public boolean includeRankInfo = true;
        public String format = "**[{ftb_rank_display_name}]** {player_name}: {message}";
        public boolean filterProfanity = true;
        public int maxMessageLength = 2000;
    }

    public static class StatusUpdates {
        public boolean enabled = true;
        public int updateInterval = 60;
        public String channel = "status";
        public EmbedStyle embedStyle = new EmbedStyle();
    }

    public static class EmbedStyle {
        public String color = "#00ff00";
        public String title = "🎮 NeoEssentials Server Status";
        public java.util.List<EmbedField> fields = java.util.Arrays.asList(
            new EmbedField("👥 Players Online", "{players_online}/{max_players}", true),
            new EmbedField("🏆 Top Team", "{top_team_name} ({top_team_members} members)", true),
            new EmbedField("⚡ Server Health", "TPS: {server_tps} | RAM: {used_memory}/{max_memory}MB", false),
            new EmbedField("📊 Active Layouts", "Tablist: {active_tablist_layouts} | Scoreboard: {active_scoreboard_layouts}", false)
        );
        public String footer = "Last updated: {timestamp}";
        public String thumbnail = "https://i.imgur.com/server-icon.png";
    }

    public static class EmbedField {
        public String name;
        public String value;
        public boolean inline;

        public EmbedField() {}
        public EmbedField(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }

    public static class Webhooks {
        public boolean enabled = true;
        public boolean useWebhookForChat = true;
        public boolean useWebhookForNotifications = true;
        public String avatarPlaceholder = "https://minotar.net/helm/{player_name}/64.png";
        public String defaultAvatar = "https://i.imgur.com/default-avatar.png";
    }

    public static class ErrorHandling {
        public int retryAttempts = 3;
        public int retryDelay = 5000;
        public boolean logErrors = true;
        public boolean fallbackToBasicMessage = true;
    }
}
