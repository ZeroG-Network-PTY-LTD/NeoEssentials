package com.zerog.neoessentials.config;

/**
 * Unified Display Configuration for Tablist, Scoreboard, and Bossbar
 * Supports multiline layouts with FTB integration
 */
public class TablistConfig {

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
        public int updateInterval = 20;
        public String format = "{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix}";
        public java.util.List<Layout> layouts = new java.util.ArrayList<>();
        public java.util.List<PlayerOrder> playerOrder = new java.util.ArrayList<>();
    }

    /**
     * Scoreboard configuration section
     */
    public static class ScoreboardSection {
        public boolean enabled = true;
        public int updateInterval = 20;
        public int maxLines = 15;
        public String title = "&6&lNeoEssentials";
        public java.util.List<Layout> layouts = new java.util.ArrayList<>();
        public TitleAnimations titleAnimations = new TitleAnimations();
        public AnimationConfig animations = new AnimationConfig();
        public java.util.Map<String, String> conditional_logic = new java.util.HashMap<>();
    }

    /**
     * Bossbar configuration section
     */
    public static class BossbarSection {
        public boolean enabled = true;
        public int updateInterval = 20;
        public java.util.List<BossbarLayout> layouts = new java.util.ArrayList<>();
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
        public java.util.List<BossbarInfo> bars = new java.util.ArrayList<>();
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
        public MessageFormatting messageFormatting = new MessageFormatting();
        public Notifications notifications = new Notifications();
        public RoleSync roleSync = new RoleSync();
        public ChatSync chatSync = new ChatSync();
        public StatusUpdates statusUpdates = new StatusUpdates();
        public Webhooks webhooks = new Webhooks();
        public ErrorHandling errorHandling = new ErrorHandling();
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
            "🔄 **{player_name}** | Tablist updated | Team: **{ftb_team_display_name}** | Rank: **{ftb_rank_display_name}**");
        public NotificationConfig scoreboardUpdates = new NotificationConfig(true, "general",
            "📊 **{player_name}** | Scoreboard updated | Layout: **{layout_name}**");
        public NotificationConfig playerJoin = new NotificationConfig(true, "general",
            "✅ **{player_name}** joined! | Team: **{ftb_team_display_name}** | Rank: **{ftb_rank_display_name}** | Health: **{player_health}/{player_max_health}** ❤️");
        public NotificationConfig playerLeave = new NotificationConfig(true, "general",
            "❌ **{player_name}** left | Session: **{session_time}** | Team: **{ftb_team_display_name}**");
        public NotificationConfig teamUpdates = new NotificationConfig(true, "general",
            "👥 **{player_name}** | Team updated | Old: **{old_team}** → New: **{ftb_team_display_name}**");
        public NotificationConfig rankUpdates = new NotificationConfig(true, "general",
            "🎖️ **{player_name}** | Rank updated | Old: **{old_rank}** → New: **{ftb_rank_display_name}**");
        public NotificationConfig permissionChanges = new NotificationConfig(true, "admin",
            "🔐 **{player_name}** | Permission **{permission}** | Action: **{action}** | By: **{admin}**");
        public NotificationConfig achievements = new NotificationConfig(true, "general",
            "🏆 **{player_name}** earned **{achievement}**! | Team: **{ftb_team_display_name}**");
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
        public java.util.Map<String, RoleMapping> roleMappings = new java.util.HashMap<>();
        public RoleMapping fallbackRole = new RoleMapping("neoessentials.default", 0, "&8[GUEST]&r", "&8&lGUEST");

        public RoleSync() {
            // Initialize default role mappings
            roleMappings.put("Owner", new RoleMapping("neoessentials.admin", 1000, "&4[OWNER]&r", "&4&lOWNER"));
            roleMappings.put("Admin", new RoleMapping("neoessentials.moderator", 800, "&c[ADMIN]&r", "&c&lADMIN"));
            roleMappings.put("Moderator", new RoleMapping("neoessentials.helper", 600, "&6[MOD]&r", "&6&lMODERATOR"));
            roleMappings.put("VIP", new RoleMapping("neoessentials.vip", 400, "&d[VIP]&r", "&d&lVIP"));
            roleMappings.put("Member", new RoleMapping("neoessentials.member", 200, "&a[MEMBER]&r", "&a&lMEMBER"));
            roleMappings.put("Verified", new RoleMapping("neoessentials.verified", 100, "&7[VERIFIED]&r", "&7&lVERIFIED"));
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
