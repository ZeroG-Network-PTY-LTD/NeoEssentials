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
    public static final String HEAL_SELF = "neoessentials.heal";
    public static final String HEAL_OTHERS = "neoessentials.heal.others";
    public static final String HEAL_ALL = "neoessentials.heal.*";
    
    // CORE PERMISSION LEVELS
    public static final String PLAYER_DEFAULT = "neoessentials.player.default";
    public static final String ESSENTIALS_USE = "neoessentials.use";
    public static final String MODERATION_BASIC = "neoessentials.moderation.basic";
    public static final String ADMIN_BASIC = "neoessentials.admin.basic";
    public static final String ADMIN_FULL = "neoessentials.admin.full";
    
    // Feed Command
    public static final String FEED_SELF = "neoessentials.feed";
    public static final String FEED_OTHERS = "neoessentials.feed.others";
    public static final String FEED_ALL = "neoessentials.feed.*";
    
    // God Mode Command
    public static final String GOD_SELF = "neoessentials.god";
    public static final String GOD_OTHERS = "neoessentials.god.others";
    public static final String GOD_ALL = "neoessentials.god.*";
    
    // Vanish Command
    public static final String VANISH_SELF = "neoessentials.vanish";
    public static final String VANISH_OTHERS = "neoessentials.vanish.others";
    public static final String VANISH_SEE = "neoessentials.vanish.see";
    public static final String VANISH_ALL = "neoessentials.vanish.*";
    
    // Fly Command
    public static final String FLY_SELF = "neoessentials.fly";
    public static final String FLY_OTHERS = "neoessentials.fly.others";
    public static final String FLY_ALL = "neoessentials.fly.*";
    
    // Speed Command
    public static final String SPEED_WALK = "neoessentials.speed.walk";
    public static final String SPEED_FLY = "neoessentials.speed.fly";
    public static final String SPEED_OTHERS = "neoessentials.speed.others";
    public static final String SPEED_ALL = "neoessentials.speed.*";
    
    // Repair Command
    public static final String REPAIR_HAND = "neoessentials.repair";
    public static final String REPAIR_ALL = "neoessentials.repair.all";
    public static final String REPAIR_OTHERS = "neoessentials.repair.others";
    
    // Time Command
    public static final String TIME_SET = "neoessentials.time.set";
    public static final String TIME_ADD = "neoessentials.time.add";
    public static final String TIME_QUERY = "neoessentials.time.query";
    public static final String TIME_ALL = "neoessentials.time.*";
    
    // Weather Command
    public static final String WEATHER_SET = "neoessentials.weather.set";
    public static final String WEATHER_CLEAR = "neoessentials.weather.clear";
    public static final String WEATHER_RAIN = "neoessentials.weather.rain";
    public static final String WEATHER_THUNDER = "neoessentials.weather.thunder";
    public static final String WEATHER_ALL = "neoessentials.weather.*";
    
    // Give Command
    public static final String GIVE_ITEM = "neoessentials.give";
    public static final String GIVE_UNLIMITED = "neoessentials.give.unlimited";
    public static final String GIVE_ALL = "neoessentials.give.*";
    
    // GameMode Command
    public static final String GAMEMODE = "neoessentials.gamemode";
    public static final String GAMEMODE_OTHERS = "neoessentials.gamemode.others";
    public static final String GAMEMODE_ALL = "neoessentials.gamemode.*";
    
    // EnderChest Command
    public static final String ENDERCHEST = "neoessentials.enderchest";
    public static final String ENDERCHEST_OTHERS = "neoessentials.enderchest.others";
    
    // Workbench & Anvil
    public static final String WORKBENCH = "neoessentials.workbench";
    public static final String ANVIL = "neoessentials.anvil";
    
    // ==============================
    // TELEPORTATION COMMANDS
    // ==============================
    
    // Basic Teleport
    public static final String TP_SELF = "neoessentials.tp";
    public static final String TP_OTHERS = "neoessentials.tp.others";
    public static final String TP_COORDS = "neoessentials.tp.coords";
    public static final String TP_HERE = "neoessentials.tphere";
    public static final String TP_ALL = "neoessentials.tp.*";
    
    // Home System
    public static final String HOME = "neoessentials.home";
    public static final String HOME_SET = "neoessentials.sethome";
    public static final String HOME_DELETE = "neoessentials.delhome";
    public static final String HOME_LIST = "neoessentials.homes";
    public static final String HOME_OTHERS = "neoessentials.home.others";
    public static final String HOME_MULTIPLE = "neoessentials.home.multiple";
    public static final String HOME_ALL = "neoessentials.home.*";
    
    // Warp System
    public static final String WARP = "neoessentials.warp";
    public static final String WARP_SET = "neoessentials.setwarp";
    public static final String WARP_DELETE = "neoessentials.delwarp";
    public static final String WARP_LIST = "neoessentials.warps";
    public static final String WARP_ALL = "neoessentials.warp.*";
    
    // TPA System
    public static final String TPA_REQUEST = "neoessentials.tpa";
    public static final String TPA_HERE = "neoessentials.tpahere";
    public static final String TPA_ACCEPT = "neoessentials.tpaccept";
    public static final String TPA_DENY = "neoessentials.tpdeny";
    public static final String TPA_CANCEL = "neoessentials.tpcancel";
    public static final String TPA_ALL = "neoessentials.tpa.*";
    
    // Spawn System
    public static final String SPAWN = "neoessentials.spawn";
    public static final String SPAWN_SET = "neoessentials.setspawn";
    public static final String SPAWN_OTHERS = "neoessentials.spawn.others";
    public static final String SPAWN_ALL = "neoessentials.spawn.*";
    
    // Back Command
    public static final String BACK = "neoessentials.back";
    public static final String BACK_ONDEATH = "neoessentials.back.ondeath";
    public static final String BACK_ONTELEPORT = "neoessentials.back.onteleport";
    
    // ==============================
    // MODERATION COMMANDS
    // ==============================
    
    // Ban System
    public static final String BAN = "neoessentials.ban";
    public static final String BAN_TEMP = "neoessentials.tempban";
    public static final String BAN_IP = "neoessentials.banip";
    public static final String UNBAN = "neoessentials.unban";
    public static final String BAN_EXEMPT = "neoessentials.ban.exempt";
    public static final String BAN_ALL = "neoessentials.ban.*";
    
    // Kick Command
    public static final String KICK = "neoessentials.kick";
    public static final String KICK_EXEMPT = "neoessentials.kick.exempt";
    
    // Mute System
    public static final String MUTE = "neoessentials.mute";
    public static final String UNMUTE = "neoessentials.unmute";
    public static final String MUTE_EXEMPT = "neoessentials.mute.exempt";
    public static final String MUTE_ALL = "neoessentials.mute.*";
    
    // Jail System
    public static final String JAIL = "neoessentials.jail";
    public static final String UNJAIL = "neoessentials.unjail";
    public static final String JAIL_SET = "neoessentials.setjail";
    public static final String JAIL_DELETE = "neoessentials.deljail";
    public static final String JAIL_EXEMPT = "neoessentials.jail.exempt";
    public static final String JAIL_ALL = "neoessentials.jail.*";
    
    // ==============================
    // ECONOMY SYSTEM
    // ==============================
    
    // Basic Economy
    public static final String ECO_BALANCE = "neoessentials.balance";
    public static final String ECO_BALANCE_OTHERS = "neoessentials.balance.others";
    public static final String ECO_PAY = "neoessentials.pay";
    public static final String ECO_TOP = "neoessentials.balancetop";
    
    // Economy Administration
    public static final String ECO_GIVE = "neoessentials.eco.give";
    public static final String ECO_TAKE = "neoessentials.eco.take";
    public static final String ECO_SET = "neoessentials.eco.set";
    public static final String ECO_RESET = "neoessentials.eco.reset";
    public static final String ECO_ALL = "neoessentials.eco.*";
    
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
    public static final String SHOP_BYPASS_PROTECTION = "neoessentials.shop.bypass.protection";
    public static final String SHOP_ALL = "neoessentials.shop.*";
    
    // ==============================
    // MESSAGING SYSTEM
    // ==============================
    
    // Private Messages
    public static final String MSG = "neoessentials.msg";
    public static final String REPLY = "neoessentials.reply";
    public static final String MSGTOGGLE = "neoessentials.msgtoggle";
    public static final String SOCIALSPY = "neoessentials.socialspy";
    
    // Mail System
    public static final String MAIL_SEND = "neoessentials.mail.send";
    public static final String MAIL_READ = "neoessentials.mail.read";
    public static final String MAIL_CLEAR = "neoessentials.mail.clear";
    public static final String MAIL_ALL = "neoessentials.mail.*";
    
    // Broadcast
    public static final String BROADCAST = "neoessentials.broadcast";
    public static final String BROADCAST_WORLD = "neoessentials.broadcast.world";
    
    // ==============================
    // PLAYER INFORMATION
    // ==============================
    
    // Player Lists
    public static final String LIST = "neoessentials.list";
    public static final String LIST_HIDDEN = "neoessentials.list.hidden";
    
    // Player Information
    public static final String WHOIS = "neoessentials.whois";
    public static final String SEEN = "neoessentials.seen";
    public static final String REALNAME = "neoessentials.realname";
    
    // Nickname System
    public static final String NICK = "neoessentials.nick";
    public static final String NICK_OTHERS = "neoessentials.nick.others";
    public static final String NICK_COLOR = "neoessentials.nick.color";
    public static final String NICK_MAGIC = "neoessentials.nick.magic";

    // Inventory See
    public static final String INVSEE = "neoessentials.invsee";
    public static final String INVSEE_MODIFY = "neoessentials.invsee.modify";

    // Inventory-opening commands
    public static final String SMITHING = "neoessentials.smithing";
    public static final String STONECUTTER = "neoessentials.stonecutter";
    
    // AFK System
    public static final String AFK = "neoessentials.afk";
    public static final String AFK_OTHERS = "neoessentials.afk.others";
    public static final String AFK_EXEMPT = "neoessentials.afk.exempt";
    public static final String AFK_ALL = "neoessentials.afk.*";
    
    // ==============================
    // KIT SYSTEM
    // ==============================
    
    // Kit Usage
    public static final String KIT = "neoessentials.kit";
    public static final String KIT_LIST = "neoessentials.kit.list";
    public static final String KIT_PREVIEW = "neoessentials.kit.preview";
    
    // Kit Administration
    public static final String KIT_CREATE = "neoessentials.kit.create";
    public static final String KIT_DELETE = "neoessentials.kit.delete";
    public static final String KIT_EDIT = "neoessentials.kit.edit";
    public static final String KIT_GIVE = "neoessentials.kit.give";
    public static final String KIT_ALL = "neoessentials.kit.*";
    
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
    public static final String GUI_ALL = "neoessentials.gui.*";
    
    // Security System
    public static final String SECURITY_VIEW = "neoessentials.security.view";
    public static final String SECURITY_ADMIN = "neoessentials.security.admin";
    public static final String SECURITY_ALERTS = "neoessentials.security.alerts";
    public static final String SECURITY_ALL = "neoessentials.security.*";
    
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
    
    // Cleanup and Maintenance System
    public static final String ADMIN_CLEANUP = "neoessentials.admin.cleanup";
    public static final String ADMIN_CLEANUP_ALL = "neoessentials.admin.cleanup.all";
    public static final String ADMIN_CLEANUP_MEMORY = "neoessentials.admin.cleanup.memory";
    public static final String ADMIN_CLEANUP_CACHE = "neoessentials.admin.cleanup.cache";
    public static final String ADMIN_CLEANUP_FILES = "neoessentials.admin.cleanup.files";
    public static final String ADMIN_CLEANUP_DATA = "neoessentials.admin.cleanup.data";
    public static final String ADMIN_CLEANUP_SCOREBOARD = "neoessentials.admin.cleanup.scoreboard";
    public static final String ADMIN_CLEANUP_AUTO = "neoessentials.admin.cleanup.auto";
    public static final String ADMIN_CLEANUP_INFO = "neoessentials.admin.cleanup.info";
    public static final String ADMIN_CLEANUP_FORCE = "neoessentials.admin.cleanup.force";
    
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
    public static final String BYPASS_COOLDOWN = "neoessentials.bypass.cooldown";
    public static final String BYPASS_COOLDOWN_TELEPORT = "neoessentials.bypass.cooldown.teleport";
    public static final String BYPASS_COOLDOWN_COMMAND = "neoessentials.bypass.cooldown.command";
    
    // Limit Bypasses
    public static final String BYPASS_LIMIT_HOME = "neoessentials.bypass.limit.home";
    public static final String BYPASS_LIMIT_WARP = "neoessentials.bypass.limit.warp";
    
    // Cost Bypasses
    public static final String BYPASS_COST = "neoessentials.bypass.cost";
    public static final String BYPASS_COST_TELEPORT = "neoessentials.bypass.cost.teleport";
    public static final String BYPASS_COST_COMMAND = "neoessentials.bypass.cost.command";
    
    // ==============================
    // DISCORD INTEGRATION (DEPRECATED/DISABLED)
    // ==============================
    
    // Discord Commands - kept for config compatibility
    public static final String DISCORD_LINK = "neoessentials.discord.link";
    public static final String DISCORD_ITEM = "neoessentials.discord.item";
    public static final String DISCORD_INVENTORY = "neoessentials.discord.inventory";
    public static final String DISCORD_ENDERCHEST = "neoessentials.discord.enderchest";
    
    // ==============================
    // ADMINISTRATIVE WILDCARD PERMISSIONS
    // ==============================
    
    // Category Wildcards
    public static final String ALL_ESSENTIALS = "neoessentials.*";
    public static final String ALL_NEOESSENTIALS = "neoessentials.*";
    public static final String ALL_TELEPORT = "neoessentials.teleport.*";
    public static final String ALL_MODERATION = "neoessentials.moderation.*";
    public static final String ALL_ECONOMY = "neoessentials.economy.*";
    public static final String ALL_MESSAGING = "neoessentials.messaging.*";
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
            // Core Permission Levels
            PLAYER_DEFAULT, ESSENTIALS_USE, MODERATION_BASIC, ADMIN_BASIC, ADMIN_FULL,
            
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
            GAMEMODE, GAMEMODE_OTHERS, GAMEMODE_ALL,
            
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
            SHOP_ADMIN, SHOP_MANAGE_OTHERS, SHOP_BYPASS_LIMITS, SHOP_BYPASS_PROTECTION, SHOP_ALL,
            
            // Messaging
            MSG, REPLY, MSGTOGGLE, SOCIALSPY,
            MAIL_SEND, MAIL_READ, MAIL_CLEAR, MAIL_ALL,
            BROADCAST, BROADCAST_WORLD,
            
            // Player Information
            LIST, LIST_HIDDEN,
            WHOIS, SEEN, REALNAME,
            NICK, NICK_OTHERS, NICK_COLOR, NICK_MAGIC,
            
            // AFK System
            AFK, AFK_OTHERS, AFK_EXEMPT, AFK_ALL,
            
            // Kit System
            KIT, KIT_LIST, KIT_PREVIEW,
            KIT_CREATE, KIT_DELETE, KIT_EDIT, KIT_GIVE, KIT_ALL,
            
            // NeoEssentials Features
            BOSSBAR_SHOW, BOSSBAR_SHOW_OTHERS, BOSSBAR_HIDE, BOSSBAR_BROADCAST,
            BOSSBAR_CREATE, BOSSBAR_UPDATE, BOSSBAR_DELETE, BOSSBAR_TEMPLATES, BOSSBAR_ALL,
            PLACEHOLDER_TEST, PLACEHOLDER_LIST, PLACEHOLDER_INFO, PLACEHOLDER_RELOAD, PLACEHOLDER_ALL,
            GUI_OPEN, GUI_ADMIN, GUI_ALL,
            SECURITY_VIEW, SECURITY_ADMIN, SECURITY_ALERTS, SECURITY_ALL,
            
            // Permission Management
            PERMISSIONS_INFO, PERMISSIONS_CHECK, PERMISSIONS_USER, PERMISSIONS_GROUP,
            PERMISSIONS_RELOAD, PERMISSIONS_STATS, PERMISSIONS_ALL,
            
            // Administration
            CONFIG_RELOAD, CONFIG_SAVE, CONFIG_RESET, CONFIG_ALL,
            LANGUAGE_SET, LANGUAGE_LIST, LANGUAGE_RELOAD, LANGUAGE_ALL,
            PERFORMANCE_VIEW, PERFORMANCE_ADMIN, PERFORMANCE_ALL,
            STATUS_VIEW, STATUS_ADMIN, STATUS_ALL,
            
            // Cleanup and Maintenance
            ADMIN_CLEANUP, ADMIN_CLEANUP_ALL, ADMIN_CLEANUP_MEMORY, ADMIN_CLEANUP_CACHE,
            ADMIN_CLEANUP_FILES, ADMIN_CLEANUP_DATA, ADMIN_CLEANUP_SCOREBOARD,
            ADMIN_CLEANUP_AUTO, ADMIN_CLEANUP_INFO, ADMIN_CLEANUP_FORCE,
            
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
