package com.zerog.neoessentials.permissions;

/**
 * Comprehensive Permission Nodes for NeoEssentials
 * 
 * This class defines all permission nodes used throughout the mod.
 * Organi    public static final String SHOP_SIGN_CREATE = "neoessentials.shop.sign.create";
    public static final String SHOP_SIGN_USE = "neoessentials.shop.sign.use";
    public static final String SHOP_SIGN_BREAK = "neoessentials.shop.sign.break";
    public static final String SHOP_SIGN_ADMIN = "neoessentials.shop.sign.admin";
    public static final String SHOP_ADMIN = "neoessentials.shop.admin";ermissions by category for easy maintenance and documentation.
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public final class PermissionNodes {
    
    // Base permission for all NeoEssentials features
    public static final String BASE = "neoessentials";
    public static final String ESSENTIALS_BASE = "essentials";
    
    // ==============================
    // ESSENTIAL UTILITY COMMANDS
    // ==============================
    
    // Heal Command
    public static final String HEAL_SELF = "essentials.heal";
    public static final String HEAL_OTHERS = "essentials.heal.others";
    public static final String HEAL_ALL = "essentials.heal.*";
    
    // Feed Command
    public static final String FEED_SELF = "essentials.feed";
    public static final String FEED_OTHERS = "essentials.feed.others";
    public static final String FEED_ALL = "essentials.feed.*";
    
    // God Mode Command
    public static final String GOD_SELF = "essentials.god";
    public static final String GOD_OTHERS = "essentials.god.others";
    public static final String GOD_ALL = "essentials.god.*";
    
    // Vanish Command
    public static final String VANISH_SELF = "essentials.vanish";
    public static final String VANISH_OTHERS = "essentials.vanish.others";
    public static final String VANISH_SEE = "essentials.vanish.see";
    public static final String VANISH_ALL = "essentials.vanish.*";
    
    // Fly Command
    public static final String FLY_SELF = "essentials.fly";
    public static final String FLY_OTHERS = "essentials.fly.others";
    public static final String FLY_ALL = "essentials.fly.*";
    
    // Speed Command
    public static final String SPEED_WALK = "essentials.speed.walk";
    public static final String SPEED_FLY = "essentials.speed.fly";
    public static final String SPEED_OTHERS = "essentials.speed.others";
    public static final String SPEED_ALL = "essentials.speed.*";
    
    // Repair Command
    public static final String REPAIR_HAND = "essentials.repair";
    public static final String REPAIR_ALL = "essentials.repair.all";
    public static final String REPAIR_OTHERS = "essentials.repair.others";
    
    // Time Command
    public static final String TIME_SET = "essentials.time.set";
    public static final String TIME_ADD = "essentials.time.add";
    public static final String TIME_QUERY = "essentials.time.query";
    public static final String TIME_ALL = "essentials.time.*";
    
    // Weather Command
    public static final String WEATHER_SET = "essentials.weather.set";
    public static final String WEATHER_CLEAR = "essentials.weather.clear";
    public static final String WEATHER_RAIN = "essentials.weather.rain";
    public static final String WEATHER_THUNDER = "essentials.weather.thunder";
    public static final String WEATHER_ALL = "essentials.weather.*";
    
    // Give Command
    public static final String GIVE_ITEM = "essentials.give";
    public static final String GIVE_UNLIMITED = "essentials.give.unlimited";
    public static final String GIVE_ALL = "essentials.give.*";
    
    // Workbench & Anvil
    public static final String WORKBENCH = "essentials.workbench";
    public static final String ANVIL = "essentials.anvil";
    public static final String ENDERCHEST = "essentials.enderchest";
    public static final String ENDERCHEST_OTHERS = "essentials.enderchest.others";
    
    // ==============================
    // TELEPORTATION COMMANDS
    // ==============================
    
    // Basic Teleport
    public static final String TP_SELF = "essentials.tp";
    public static final String TP_OTHERS = "essentials.tp.others";
    public static final String TP_COORDS = "essentials.tp.coords";
    public static final String TP_HERE = "essentials.tphere";
    public static final String TP_ALL = "essentials.tp.*";
    
    // Home System
    public static final String HOME = "essentials.home";
    public static final String HOME_SET = "essentials.sethome";
    public static final String HOME_DELETE = "essentials.delhome";
    public static final String HOME_LIST = "essentials.homes";
    public static final String HOME_OTHERS = "essentials.home.others";
    public static final String HOME_MULTIPLE = "essentials.home.multiple";
    public static final String HOME_ALL = "essentials.home.*";
    
    // Warp System
    public static final String WARP = "essentials.warp";
    public static final String WARP_SET = "essentials.setwarp";
    public static final String WARP_DELETE = "essentials.delwarp";
    public static final String WARP_LIST = "essentials.warps";
    public static final String WARP_ALL = "essentials.warp.*";
    
    // TPA System
    public static final String TPA_REQUEST = "essentials.tpa";
    public static final String TPA_HERE = "essentials.tpahere";
    public static final String TPA_ACCEPT = "essentials.tpaccept";
    public static final String TPA_DENY = "essentials.tpdeny";
    public static final String TPA_CANCEL = "essentials.tpcancel";
    public static final String TPA_ALL = "essentials.tpa.*";
    
    // Spawn System
    public static final String SPAWN = "essentials.spawn";
    public static final String SPAWN_SET = "essentials.setspawn";
    public static final String SPAWN_OTHERS = "essentials.spawn.others";
    public static final String SPAWN_ALL = "essentials.spawn.*";
    
    // Back Command
    public static final String BACK = "essentials.back";
    public static final String BACK_ONDEATH = "essentials.back.ondeath";
    public static final String BACK_ONTELEPORT = "essentials.back.onteleport";
    
    // ==============================
    // MODERATION COMMANDS
    // ==============================
    
    // Ban System
    public static final String BAN = "essentials.ban";
    public static final String BAN_TEMP = "essentials.tempban";
    public static final String BAN_IP = "essentials.banip";
    public static final String UNBAN = "essentials.unban";
    public static final String BAN_EXEMPT = "essentials.ban.exempt";
    public static final String BAN_ALL = "essentials.ban.*";
    
    // Kick Command
    public static final String KICK = "essentials.kick";
    public static final String KICK_EXEMPT = "essentials.kick.exempt";
    
    // Mute System
    public static final String MUTE = "essentials.mute";
    public static final String UNMUTE = "essentials.unmute";
    public static final String MUTE_EXEMPT = "essentials.mute.exempt";
    public static final String MUTE_ALL = "essentials.mute.*";
    
    // Jail System
    public static final String JAIL = "essentials.jail";
    public static final String UNJAIL = "essentials.unjail";
    public static final String JAIL_SET = "essentials.setjail";
    public static final String JAIL_DELETE = "essentials.deljail";
    public static final String JAIL_EXEMPT = "essentials.jail.exempt";
    public static final String JAIL_ALL = "essentials.jail.*";
    
    // ==============================
    // ECONOMY SYSTEM
    // ==============================
    
    // Basic Economy
    public static final String ECO_BALANCE = "essentials.balance";
    public static final String ECO_BALANCE_OTHERS = "essentials.balance.others";
    public static final String ECO_PAY = "essentials.pay";
    public static final String ECO_TOP = "essentials.balancetop";
    
    // Economy Administration
    public static final String ECO_GIVE = "essentials.eco.give";
    public static final String ECO_TAKE = "essentials.eco.take";
    public static final String ECO_SET = "essentials.eco.set";
    public static final String ECO_RESET = "essentials.eco.reset";
    public static final String ECO_ALL = "essentials.eco.*";
    
    // Economy Analytics
    public static final String ECO_ANALYTICS = "neoessentials.economy.analytics";
    public static final String ECO_TRANSACTIONS = "neoessentials.economy.transactions";
    public static final String ECO_HISTORY = "neoessentials.economy.history";
    
    // Shop System
    public static final String SHOP_USE = "neoessentials.shop.use";
    public static final String SHOP_BUY = "neoessentials.shop.buy";
    public static final String SHOP_SELL = "neoessentials.shop.sell";
    public static final String SHOP_CREATE = "neoessentials.shop.create";
    public static final String SHOP_DELETE = "neoessentials.shop.delete";
    public static final String SHOP_EDIT = "neoessentials.shop.edit";
    public static final String SHOP_BROWSE = "neoessentials.shop.browse";
    public static final String SHOP_SEARCH = "neoessentials.shop.search";
    public static final String SHOP_SIGN_CREATE = "neoessentials.shop.sign.create";
    public static final String SHOP_SIGN_USE = "neoessentials.shop.sign.use";
    public static final String SHOP_SIGN_BREAK = "neoessentials.shop.sign.break";
    public static final String SHOP_SIGN_ADMIN = "neoessentials.shop.sign.admin";
    public static final String SHOP_ADMIN = "neoessentials.shop.admin";
    public static final String SHOP_MANAGE_OTHERS = "neoessentials.shop.manage.others";
    public static final String SHOP_BYPASS_LIMITS = "neoessentials.shop.bypass.limits";
    public static final String SHOP_ALL = "neoessentials.shop.*";
    
    // ==============================
    // MESSAGING SYSTEM
    // ==============================
    
    // Private Messages
    public static final String MSG = "essentials.msg";
    public static final String REPLY = "essentials.reply";
    public static final String MSGTOGGLE = "essentials.msgtoggle";
    public static final String SOCIALSPY = "essentials.socialspy";
    
    // Mail System
    public static final String MAIL_SEND = "essentials.mail.send";
    public static final String MAIL_READ = "essentials.mail.read";
    public static final String MAIL_CLEAR = "essentials.mail.clear";
    public static final String MAIL_ALL = "essentials.mail.*";
    
    // Broadcast
    public static final String BROADCAST = "essentials.broadcast";
    public static final String BROADCAST_WORLD = "essentials.broadcast.world";
    
    // ==============================
    // PLAYER INFORMATION
    // ==============================
    
    // Player Lists
    public static final String LIST = "essentials.list";
    public static final String LIST_HIDDEN = "essentials.list.hidden";
    
    // Player Information
    public static final String WHOIS = "essentials.whois";
    public static final String SEEN = "essentials.seen";
    public static final String REALNAME = "essentials.realname";
    
    // Nickname System
    public static final String NICK = "essentials.nick";
    public static final String NICK_OTHERS = "essentials.nick.others";
    public static final String NICK_COLOR = "essentials.nick.color";
    public static final String NICK_MAGIC = "essentials.nick.magic";
    
    // ==============================
    // KIT SYSTEM
    // ==============================
    
    // Kit Usage
    public static final String KIT = "essentials.kit";
    public static final String KIT_LIST = "essentials.kit.list";
    public static final String KIT_PREVIEW = "essentials.kit.preview";
    
    // Kit Administration
    public static final String KIT_CREATE = "essentials.kit.create";
    public static final String KIT_DELETE = "essentials.kit.delete";
    public static final String KIT_EDIT = "essentials.kit.edit";
    public static final String KIT_GIVE = "essentials.kit.give";
    public static final String KIT_ALL = "essentials.kit.*";
    
    // ==============================
    // NEOESSENTIALS FEATURES
    // ==============================
    
    // Bossbar System
    public static final String BOSSBAR_SHOW = "neoessentials.bossbar.show";
    public static final String BOSSBAR_SHOW_OTHERS = "neoessentials.bossbar.show.others";
    public static final String BOSSBAR_HIDE = "neoessentials.bossbar.hide";
    public static final String BOSSBAR_BROADCAST = "neoessentials.bossbar.broadcast";
    public static final String BOSSBAR_CREATE = "neoessentials.bossbar.create";
    public static final String BOSSBAR_UPDATE = "neoessentials.bossbar.update";
    public static final String BOSSBAR_DELETE = "neoessentials.bossbar.delete";
    public static final String BOSSBAR_TEMPLATES = "neoessentials.bossbar.templates";
    public static final String BOSSBAR_ALL = "neoessentials.bossbar.*";
    
    // Placeholder System
    public static final String PLACEHOLDER_TEST = "neoessentials.placeholder.test";
    public static final String PLACEHOLDER_LIST = "neoessentials.placeholder.list";
    public static final String PLACEHOLDER_INFO = "neoessentials.placeholder.info";
    public static final String PLACEHOLDER_RELOAD = "neoessentials.placeholder.reload";
    public static final String PLACEHOLDER_ALL = "neoessentials.placeholder.*";
    
    // GUI System
    public static final String GUI_OPEN = "neoessentials.gui.open";
    public static final String GUI_ADMIN = "neoessentials.gui.admin";
    public static final String GUI_THEMES = "neoessentials.gui.themes";
    public static final String GUI_ALL = "neoessentials.gui.*";
    
    // Security System
    public static final String SECURITY_VIEW = "neoessentials.security.view";
    public static final String SECURITY_ADMIN = "neoessentials.security.admin";
    public static final String SECURITY_ALERTS = "neoessentials.security.alerts";
    public static final String SECURITY_ALL = "neoessentials.security.*";
    
    // ==============================
    // DISCORD INTEGRATION
    // ==============================
    
    // Discord Commands
    public static final String DISCORD_LINK = "neoessentials.discord.link";
    public static final String DISCORD_UNLINK = "neoessentials.discord.unlink";
    public static final String DISCORD_INFO = "neoessentials.discord.info";
    
    // Discord Interactive Features
    public static final String DISCORD_ITEM = "neoessentials.discord.item";
    public static final String DISCORD_INVENTORY = "neoessentials.discord.inventory";
    public static final String DISCORD_ENDERCHEST = "neoessentials.discord.enderchest";
    public static final String DISCORD_INTERACTIVE = "neoessentials.discord.interactive";
    public static final String DISCORD_ALL = "neoessentials.discord.*";
    
    // ==============================
    // PERMISSION MANAGEMENT
    // ==============================
    
    // Permission Commands
    public static final String PERMISSIONS_INFO = "neoessentials.permissions.info";
    public static final String PERMISSIONS_CHECK = "neoessentials.permissions.check";
    public static final String PERMISSIONS_USER = "neoessentials.permissions.user";
    public static final String PERMISSIONS_GROUP = "neoessentials.permissions.group";
    public static final String PERMISSIONS_RELOAD = "neoessentials.permissions.reload";
    public static final String PERMISSIONS_STATS = "neoessentials.permissions.stats";
    public static final String PERMISSIONS_ALL = "neoessentials.permissions.*";
    
    // ==============================
    // ADMINISTRATION
    // ==============================
    
    // Configuration Management
    public static final String CONFIG_RELOAD = "neoessentials.config.reload";
    public static final String CONFIG_SAVE = "neoessentials.config.save";
    public static final String CONFIG_RESET = "neoessentials.config.reset";
    public static final String CONFIG_ALL = "neoessentials.config.*";
    
    // Language System
    public static final String LANGUAGE_SET = "neoessentials.language.set";
    public static final String LANGUAGE_LIST = "neoessentials.language.list";
    public static final String LANGUAGE_RELOAD = "neoessentials.language.reload";
    public static final String LANGUAGE_ALL = "neoessentials.language.*";
    
    // Performance Monitoring
    public static final String PERFORMANCE_VIEW = "neoessentials.performance.view";
    public static final String PERFORMANCE_ADMIN = "neoessentials.performance.admin";
    public static final String PERFORMANCE_ALL = "neoessentials.performance.*";
    
    // Status Monitoring
    public static final String STATUS_VIEW = "neoessentials.status.view";
    public static final String STATUS_ADMIN = "neoessentials.status.admin";
    public static final String STATUS_ALL = "neoessentials.status.*";
    
    // Analytics
    public static final String ANALYTICS_VIEW = "neoessentials.analytics.view";
    public static final String ANALYTICS_ADMIN = "neoessentials.analytics.admin";
    public static final String ANALYTICS_ALL = "neoessentials.analytics.*";
    
    // ==============================
    // PLAYER FEATURES
    // ==============================
    
    // Playtime Tracking
    public static final String PLAYTIME_VIEW = "neoessentials.playtime.view";
    public static final String PLAYTIME_OTHERS = "neoessentials.playtime.others";
    public static final String PLAYTIME_TOP = "neoessentials.playtime.top";
    public static final String PLAYTIME_ALL = "neoessentials.playtime.*";
    
    // Achievement System
    public static final String ACHIEVEMENTS_VIEW = "neoessentials.achievements.view";
    public static final String ACHIEVEMENTS_OTHERS = "neoessentials.achievements.others";
    public static final String ACHIEVEMENTS_ADMIN = "neoessentials.achievements.admin";
    public static final String ACHIEVEMENTS_ALL = "neoessentials.achievements.*";
    
    // Player Preferences
    public static final String PREFERENCES_SET = "neoessentials.preferences.set";
    public static final String PREFERENCES_VIEW = "neoessentials.preferences.view";
    public static final String PREFERENCES_ALL = "neoessentials.preferences.*";
    
    // ==============================
    // ANIMATION SYSTEM
    // ==============================
    
    // Animation Commands
    public static final String ANIMATION_PLAY = "neoessentials.animation.play";
    public static final String ANIMATION_STOP = "neoessentials.animation.stop";
    public static final String ANIMATION_LIST = "neoessentials.animation.list";
    public static final String ANIMATION_CREATE = "neoessentials.animation.create";
    public static final String ANIMATION_DELETE = "neoessentials.animation.delete";
    public static final String ANIMATION_ALL = "neoessentials.animation.*";
    
    // ==============================
    // WEB DASHBOARD
    // ==============================
    
    // Web Dashboard Access
    public static final String WEBDASH_USE = "neoessentials.webdash.use";
    public static final String WEBDASH_ACCESS = "neoessentials.webdash.access";
    public static final String WEBDASH_MANAGE = "neoessentials.webdash.manage";
    public static final String WEBDASH_ANALYTICS = "neoessentials.webdash.analytics";
    public static final String WEBDASH_ADMIN = "neoessentials.webdash.admin";
    public static final String WEBDASH_ALL = "neoessentials.webdash.*";
    
    // ==============================
    // BYPASS PERMISSIONS
    // ==============================
    
    // Cooldown Bypasses
    public static final String BYPASS_COOLDOWN = "essentials.bypass.cooldown";
    public static final String BYPASS_COOLDOWN_TELEPORT = "essentials.bypass.cooldown.teleport";
    public static final String BYPASS_COOLDOWN_COMMAND = "essentials.bypass.cooldown.command";
    
    // Limit Bypasses
    public static final String BYPASS_LIMIT_HOME = "essentials.bypass.limit.home";
    public static final String BYPASS_LIMIT_WARP = "essentials.bypass.limit.warp";
    
    // Cost Bypasses
    public static final String BYPASS_COST = "essentials.bypass.cost";
    public static final String BYPASS_COST_TELEPORT = "essentials.bypass.cost.teleport";
    public static final String BYPASS_COST_COMMAND = "essentials.bypass.cost.command";
    
    // ==============================
    // ADMINISTRATIVE WILDCARD PERMISSIONS
    // ==============================
    
    // Category Wildcards
    public static final String ALL_ESSENTIALS = "essentials.*";
    public static final String ALL_NEOESSENTIALS = "neoessentials.*";
    public static final String ALL_TELEPORT = "essentials.teleport.*";
    public static final String ALL_MODERATION = "essentials.moderation.*";
    public static final String ALL_ECONOMY = "essentials.economy.*";
    public static final String ALL_MESSAGING = "essentials.messaging.*";
    public static final String ALL_ADMIN = "*.admin";
    
    // Ultimate permissions
    public static final String ALL_PERMISSIONS = "*";
    
    /**
     * Check if a permission node is valid format
     */
    public static boolean isValidPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return false;
        }
        
        // Basic format validation - letters, numbers, dots, underscores, hyphens, and wildcards
        return permission.matches("^[a-zA-Z0-9._*-]+$") && permission.length() <= 100;
    }
    
    /**
     * Get all permission nodes as an array for documentation
     */
    public static String[] getAllPermissions() {
        return new String[] {
            // Essential Commands
            HEAL_SELF, HEAL_OTHERS, HEAL_ALL,
            FEED_SELF, FEED_OTHERS, FEED_ALL,
            GOD_SELF, GOD_OTHERS, GOD_ALL,
            VANISH_SELF, VANISH_OTHERS, VANISH_SEE, VANISH_ALL,
            FLY_SELF, FLY_OTHERS, FLY_ALL,
            SPEED_WALK, SPEED_FLY, SPEED_OTHERS, SPEED_ALL,
            REPAIR_HAND, REPAIR_ALL, REPAIR_OTHERS,
            TIME_SET, TIME_ADD, TIME_QUERY, TIME_ALL,
            WEATHER_SET, WEATHER_CLEAR, WEATHER_RAIN, WEATHER_THUNDER, WEATHER_ALL,
            GIVE_ITEM, GIVE_UNLIMITED, GIVE_ALL,
            WORKBENCH, ANVIL, ENDERCHEST, ENDERCHEST_OTHERS,
            
            // Teleportation
            TP_SELF, TP_OTHERS, TP_COORDS, TP_HERE, TP_ALL,
            HOME, HOME_SET, HOME_DELETE, HOME_LIST, HOME_OTHERS, HOME_MULTIPLE, HOME_ALL,
            WARP, WARP_SET, WARP_DELETE, WARP_LIST, WARP_ALL,
            TPA_REQUEST, TPA_HERE, TPA_ACCEPT, TPA_DENY, TPA_CANCEL, TPA_ALL,
            SPAWN, SPAWN_SET, SPAWN_OTHERS, SPAWN_ALL,
            BACK, BACK_ONDEATH, BACK_ONTELEPORT,
            
            // Moderation
            BAN, BAN_TEMP, BAN_IP, UNBAN, BAN_EXEMPT, BAN_ALL,
            KICK, KICK_EXEMPT,
            MUTE, UNMUTE, MUTE_EXEMPT, MUTE_ALL,
            JAIL, UNJAIL, JAIL_SET, JAIL_DELETE, JAIL_EXEMPT, JAIL_ALL,
            
            // Economy
            ECO_BALANCE, ECO_BALANCE_OTHERS, ECO_PAY, ECO_TOP,
            ECO_GIVE, ECO_TAKE, ECO_SET, ECO_RESET, ECO_ALL,
            ECO_ANALYTICS, ECO_TRANSACTIONS, ECO_HISTORY,
            
            // Shop System
            SHOP_USE, SHOP_BUY, SHOP_SELL, SHOP_CREATE, SHOP_DELETE, SHOP_EDIT,
            SHOP_BROWSE, SHOP_SEARCH, SHOP_SIGN_CREATE, SHOP_SIGN_USE, SHOP_SIGN_BREAK, SHOP_SIGN_ADMIN,
            SHOP_ADMIN, SHOP_MANAGE_OTHERS, SHOP_BYPASS_LIMITS, SHOP_ALL,
            
            // Messaging
            MSG, REPLY, MSGTOGGLE, SOCIALSPY,
            MAIL_SEND, MAIL_READ, MAIL_CLEAR, MAIL_ALL,
            BROADCAST, BROADCAST_WORLD,
            
            // Player Information
            LIST, LIST_HIDDEN,
            WHOIS, SEEN, REALNAME,
            NICK, NICK_OTHERS, NICK_COLOR, NICK_MAGIC,
            
            // Kit System
            KIT, KIT_LIST, KIT_PREVIEW,
            KIT_CREATE, KIT_DELETE, KIT_EDIT, KIT_GIVE, KIT_ALL,
            
            // NeoEssentials Features
            BOSSBAR_SHOW, BOSSBAR_SHOW_OTHERS, BOSSBAR_HIDE, BOSSBAR_BROADCAST,
            BOSSBAR_CREATE, BOSSBAR_UPDATE, BOSSBAR_DELETE, BOSSBAR_TEMPLATES, BOSSBAR_ALL,
            PLACEHOLDER_TEST, PLACEHOLDER_LIST, PLACEHOLDER_INFO, PLACEHOLDER_RELOAD, PLACEHOLDER_ALL,
            GUI_OPEN, GUI_ADMIN, GUI_THEMES, GUI_ALL,
            SECURITY_VIEW, SECURITY_ADMIN, SECURITY_ALERTS, SECURITY_ALL,
            
            // Discord Integration
            DISCORD_LINK, DISCORD_UNLINK, DISCORD_INFO,
            DISCORD_ITEM, DISCORD_INVENTORY, DISCORD_ENDERCHEST, DISCORD_INTERACTIVE, DISCORD_ALL,
            
            // Permission Management
            PERMISSIONS_INFO, PERMISSIONS_CHECK, PERMISSIONS_USER, PERMISSIONS_GROUP,
            PERMISSIONS_RELOAD, PERMISSIONS_STATS, PERMISSIONS_ALL,
            
            // Administration
            CONFIG_RELOAD, CONFIG_SAVE, CONFIG_RESET, CONFIG_ALL,
            LANGUAGE_SET, LANGUAGE_LIST, LANGUAGE_RELOAD, LANGUAGE_ALL,
            PERFORMANCE_VIEW, PERFORMANCE_ADMIN, PERFORMANCE_ALL,
            STATUS_VIEW, STATUS_ADMIN, STATUS_ALL,
            ANALYTICS_VIEW, ANALYTICS_ADMIN, ANALYTICS_ALL,
            
            // Player Features
            PLAYTIME_VIEW, PLAYTIME_OTHERS, PLAYTIME_TOP, PLAYTIME_ALL,
            ACHIEVEMENTS_VIEW, ACHIEVEMENTS_OTHERS, ACHIEVEMENTS_ADMIN, ACHIEVEMENTS_ALL,
            PREFERENCES_SET, PREFERENCES_VIEW, PREFERENCES_ALL,
            
            // Animation System
            ANIMATION_PLAY, ANIMATION_STOP, ANIMATION_LIST,
            ANIMATION_CREATE, ANIMATION_DELETE, ANIMATION_ALL,
            
            // Web Dashboard
            WEBDASH_USE, WEBDASH_ACCESS, WEBDASH_MANAGE, WEBDASH_ANALYTICS, WEBDASH_ADMIN, WEBDASH_ALL,
            
            // Bypass Permissions
            BYPASS_COOLDOWN, BYPASS_COOLDOWN_TELEPORT, BYPASS_COOLDOWN_COMMAND,
            BYPASS_LIMIT_HOME, BYPASS_LIMIT_WARP,
            BYPASS_COST, BYPASS_COST_TELEPORT, BYPASS_COST_COMMAND,
            
            // Wildcard Permissions
            ALL_ESSENTIALS, ALL_NEOESSENTIALS, ALL_TELEPORT, ALL_MODERATION,
            ALL_ECONOMY, ALL_MESSAGING, ALL_ADMIN, ALL_PERMISSIONS
        };
    }
}
