package com.zerog.neoessentials.config.dedicated;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for Discord Integration customization
 * Provides comprehensive settings for Discord webhook integration
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Main Integration Settings
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<String> WEBHOOK_URL;
    public static final ModConfigSpec.ConfigValue<String> BOT_NAME;
    public static final ModConfigSpec.ConfigValue<String> BOT_AVATAR_URL;
    
    // Message Templates
    public static final ModConfigSpec.ConfigValue<String> SERVER_START_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> SERVER_STOP_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> PLAYER_JOIN_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> PLAYER_LEAVE_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> PLAYER_DEATH_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> ADVANCEMENT_MESSAGE;
    
    // Moderation Messages
    public static final ModConfigSpec.ConfigValue<String> BAN_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> UNBAN_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> KICK_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> MUTE_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> UNMUTE_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> TEMPBAN_MESSAGE;
    
    // Event Notification Settings
    public static final ModConfigSpec.BooleanValue NOTIFY_SERVER_START;
    public static final ModConfigSpec.BooleanValue NOTIFY_SERVER_STOP;
    public static final ModConfigSpec.BooleanValue NOTIFY_PLAYER_JOIN;
    public static final ModConfigSpec.BooleanValue NOTIFY_PLAYER_LEAVE;
    public static final ModConfigSpec.BooleanValue NOTIFY_PLAYER_DEATH;
    public static final ModConfigSpec.BooleanValue NOTIFY_ADVANCEMENTS;
    public static final ModConfigSpec.BooleanValue NOTIFY_MODERATION_ACTIONS;
    public static final ModConfigSpec.BooleanValue NOTIFY_ADMIN_COMMANDS;
    
    // Rich Embed Customization
    public static final ModConfigSpec.BooleanValue USE_RICH_EMBEDS;
    public static final ModConfigSpec.ConfigValue<String> EMBED_COLOR;
    public static final ModConfigSpec.ConfigValue<String> EMBED_FOOTER_TEXT;
    public static final ModConfigSpec.ConfigValue<String> EMBED_FOOTER_ICON;
    public static final ModConfigSpec.BooleanValue EMBED_SHOW_TIMESTAMP;
    public static final ModConfigSpec.BooleanValue EMBED_SHOW_THUMBNAIL;
    
    // Server Statistics
    public static final ModConfigSpec.BooleanValue SEND_PLAYER_STATS;
    public static final ModConfigSpec.IntValue STATS_UPDATE_INTERVAL;
    public static final ModConfigSpec.BooleanValue SEND_ECONOMY_REPORTS;
    public static final ModConfigSpec.IntValue ECONOMY_REPORT_INTERVAL;
    public static final ModConfigSpec.BooleanValue SEND_PERFORMANCE_STATS;
    
    // Formatting Settings
    public static final ModConfigSpec.ConfigValue<String> TIMESTAMP_FORMAT;
    public static final ModConfigSpec.BooleanValue MENTION_ROLES_ON_EVENTS;
    public static final ModConfigSpec.ConfigValue<String> ADMIN_ROLE_MENTION;
    public static final ModConfigSpec.ConfigValue<String> MODERATOR_ROLE_MENTION;
    
    // Security Settings
    public static final ModConfigSpec.BooleanValue ENABLE_COMMAND_EXECUTION;
    public static final ModConfigSpec.ConfigValue<String> COMMAND_PREFIX;
    public static final ModConfigSpec.BooleanValue REQUIRE_ROLE_FOR_COMMANDS;
    public static final ModConfigSpec.ConfigValue<String> AUTHORIZED_ROLE_ID;
    
    static {
        BUILDER.comment("Discord Integration Configuration")
               .comment("Customize Discord webhook integration and notifications");
        
        BUILDER.push("main");
        ENABLED = BUILDER
            .comment("Enable Discord integration")
            .define("enabled", false);
        WEBHOOK_URL = BUILDER
            .comment("Discord webhook URL for sending messages")
            .define("webhook_url", "");
        BOT_NAME = BUILDER
            .comment("Name displayed for the bot in Discord")
            .define("bot_name", "NeoEssentials");
        BOT_AVATAR_URL = BUILDER
            .comment("Avatar URL for the bot (leave empty for default)")
            .define("bot_avatar_url", "");
        BUILDER.pop();
        
        BUILDER.push("templates");
        SERVER_START_MESSAGE = BUILDER
            .comment("Message template for server start notifications")
            .comment("Placeholders: {server_name}, {version}, {timestamp}")
            .define("server_start_message", "🟢 **Server Started!** `{server_name}` is now online!");
        SERVER_STOP_MESSAGE = BUILDER
            .comment("Message template for server stop notifications")
            .define("server_stop_message", "🔴 **Server Stopped!** `{server_name}` is now offline.");
        PLAYER_JOIN_MESSAGE = BUILDER
            .comment("Message template for player join notifications")
            .comment("Placeholders: {player}, {online_count}, {max_players}")
            .define("player_join_message", "📥 **{player}** joined the server! ({online_count}/{max_players})");
        PLAYER_LEAVE_MESSAGE = BUILDER
            .comment("Message template for player leave notifications")
            .define("player_leave_message", "📤 **{player}** left the server! ({online_count}/{max_players})");
        PLAYER_DEATH_MESSAGE = BUILDER
            .comment("Message template for player death notifications")
            .comment("Placeholders: {player}, {death_message}")
            .define("player_death_message", "💀 {death_message}");
        ADVANCEMENT_MESSAGE = BUILDER
            .comment("Message template for advancement notifications")
            .comment("Placeholders: {player}, {advancement}")
            .define("advancement_message", "🏆 **{player}** achieved `{advancement}`!");
        BUILDER.pop();
        
        BUILDER.push("moderation_templates");
        BAN_MESSAGE = BUILDER
            .comment("Message template for ban notifications")
            .comment("Placeholders: {player}, {admin}, {reason}")
            .define("ban_message", "🔨 **{player}** was banned by **{admin}** | Reason: `{reason}`");
        UNBAN_MESSAGE = BUILDER
            .comment("Message template for unban notifications")
            .define("unban_message", "✅ **{player}** was unbanned by **{admin}**");
        KICK_MESSAGE = BUILDER
            .comment("Message template for kick notifications")
            .define("kick_message", "👢 **{player}** was kicked by **{admin}** | Reason: `{reason}`");
        MUTE_MESSAGE = BUILDER
            .comment("Message template for mute notifications")
            .comment("Placeholders: {player}, {admin}, {duration}, {reason}")
            .define("mute_message", "🔇 **{player}** was muted by **{admin}** for `{duration}` | Reason: `{reason}`");
        UNMUTE_MESSAGE = BUILDER
            .comment("Message template for unmute notifications")
            .define("unmute_message", "🔊 **{player}** was unmuted by **{admin}**");
        TEMPBAN_MESSAGE = BUILDER
            .comment("Message template for temporary ban notifications")
            .define("tempban_message", "⏱️ **{player}** was temporarily banned by **{admin}** for `{duration}` | Reason: `{reason}`");
        BUILDER.pop();
        
        BUILDER.push("notifications");
        NOTIFY_SERVER_START = BUILDER
            .comment("Send notifications when server starts")
            .define("notify_server_start", true);
        NOTIFY_SERVER_STOP = BUILDER
            .comment("Send notifications when server stops")
            .define("notify_server_stop", true);
        NOTIFY_PLAYER_JOIN = BUILDER
            .comment("Send notifications when players join")
            .define("notify_player_join", true);
        NOTIFY_PLAYER_LEAVE = BUILDER
            .comment("Send notifications when players leave")
            .define("notify_player_leave", true);
        NOTIFY_PLAYER_DEATH = BUILDER
            .comment("Send notifications when players die")
            .define("notify_player_death", false);
        NOTIFY_ADVANCEMENTS = BUILDER
            .comment("Send notifications for player advancements")
            .define("notify_advancements", true);
        NOTIFY_MODERATION_ACTIONS = BUILDER
            .comment("Send notifications for moderation actions")
            .define("notify_moderation_actions", true);
        NOTIFY_ADMIN_COMMANDS = BUILDER
            .comment("Send notifications for admin command usage")
            .define("notify_admin_commands", false);
        BUILDER.pop();
        
        BUILDER.push("embeds");
        USE_RICH_EMBEDS = BUILDER
            .comment("Use rich embeds instead of plain text messages")
            .define("use_rich_embeds", true);
        EMBED_COLOR = BUILDER
            .comment("Color for Discord embeds (hex format: #FF0000)")
            .define("embed_color", "#00FF00");
        EMBED_FOOTER_TEXT = BUILDER
            .comment("Footer text for embeds")
            .define("embed_footer_text", "NeoEssentials");
        EMBED_FOOTER_ICON = BUILDER
            .comment("Footer icon URL for embeds")
            .define("embed_footer_icon", "");
        EMBED_SHOW_TIMESTAMP = BUILDER
            .comment("Show timestamp in embeds")
            .define("embed_show_timestamp", true);
        EMBED_SHOW_THUMBNAIL = BUILDER
            .comment("Show player head thumbnails in embeds")
            .define("embed_show_thumbnail", true);
        BUILDER.pop();
        
        BUILDER.push("statistics");
        SEND_PLAYER_STATS = BUILDER
            .comment("Send periodic player statistics to Discord")
            .define("send_player_stats", false);
        STATS_UPDATE_INTERVAL = BUILDER
            .comment("How often to send player stats (in minutes)")
            .defineInRange("stats_update_interval", 60, 5, 1440);
        SEND_ECONOMY_REPORTS = BUILDER
            .comment("Send periodic economy reports to Discord")
            .define("send_economy_reports", false);
        ECONOMY_REPORT_INTERVAL = BUILDER
            .comment("How often to send economy reports (in minutes)")
            .defineInRange("economy_report_interval", 180, 30, 1440);
        SEND_PERFORMANCE_STATS = BUILDER
            .comment("Send server performance statistics")
            .define("send_performance_stats", false);
        BUILDER.pop();
        
        BUILDER.push("formatting");
        TIMESTAMP_FORMAT = BUILDER
            .comment("Format for timestamps (Java DateTimeFormatter format)")
            .define("timestamp_format", "yyyy-MM-dd HH:mm:ss");
        MENTION_ROLES_ON_EVENTS = BUILDER
            .comment("Mention roles for important events")
            .define("mention_roles_on_events", false);
        ADMIN_ROLE_MENTION = BUILDER
            .comment("Role ID to mention for admin notifications")
            .define("admin_role_mention", "");
        MODERATOR_ROLE_MENTION = BUILDER
            .comment("Role ID to mention for moderation events")
            .define("moderator_role_mention", "");
        BUILDER.pop();
        
        BUILDER.push("security");
        ENABLE_COMMAND_EXECUTION = BUILDER
            .comment("Allow executing server commands from Discord (SECURITY RISK!)")
            .define("enable_command_execution", false);
        COMMAND_PREFIX = BUILDER
            .comment("Prefix for Discord commands")
            .define("command_prefix", "!");
        REQUIRE_ROLE_FOR_COMMANDS = BUILDER
            .comment("Require specific role for command execution")
            .define("require_role_for_commands", true);
        AUTHORIZED_ROLE_ID = BUILDER
            .comment("Role ID authorized to execute commands")
            .define("authorized_role_id", "");
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
