package com.zerog.neoessentials.config;

// ...existing code...


/**
 * Main configuration for NeoEssentials mod
 * Based on EssentialsX configuration structure
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MainConfig {
    // Tablist, scoreboard, and bossbar config are now controlled via TablistConfig and tablist.json
    // ...existing code...
    /** Example: Color permissions configuration */
    public ColorPermissionsConfig colorPermissionsConfig = new ColorPermissionsConfig();

    /** Example: Configuration for color and formatting permissions */
    public static class ColorPermissionsConfig {
        /** Allow chat color codes */
        public boolean chat = true;
        /** Allow RGB color codes */
        public boolean rgb = true;
    }
    /** Example: Warp settings configuration */
    public WarpConfig warpConfig = new WarpConfig();

    /** Example: Configuration for warp features */
    public static class WarpConfig {
        /** Maximum length for warp names */
        public int maxWarpNameLength = 16;
        /** Allow spaces in warp names */
        public boolean allowSpacesInNames = false;
        /** List of banned warp names */
        public java.util.List<String> bannedWarpNames = new java.util.ArrayList<>();
        /** Cost to create a warp */
        public Double createWarpCost = 0.0;
        /** Cost to teleport to a warp */
        public Double teleportWarpCost = 0.0;
        /** List of worlds where teleport is not allowed */
        public java.util.List<String> noTeleportWorlds = new java.util.ArrayList<>();
        /** Teleport warmup in seconds */
        public int teleportWarmup = 0;
        /** Teleport cooldown in seconds */
        public int teleportWarpCooldown = 0;
        /** Check for void when teleporting */
        public boolean checkForVoid = true;
        /** Messages for warp system (deprecated, use lang keys) */
        public Messages messages = new Messages();
        public static class Messages {
            public String warpCreated = "warp.created";
            public String warpListEmpty = "warp.list.empty";
            public String warpListHeader = "warp.list.header";
            public String warpListEntry = "warp.list.entry";
        }
        /** Enable/disable warps */
        public boolean enabled = true;
        /** Maximum warps per player */
        public int maxWarpsPerPlayer = 10;
        /** Allow cross-world warps */
        public boolean allowCrossWorld = true;
        /** Cost to set a warp */
        public double setWarpCost = 0.0;
        /** Cost to teleport to a warp */
        public double teleportCost = 0.0;
        /** Cooldown in seconds between warp teleports */
        public int warpCooldown = 10;
        /** Require safe location for teleport */
        public boolean requireSafeLocation = true;
        /** List of restricted worlds for warps */
        public java.util.List<String> restrictedWorlds = new java.util.ArrayList<>();
        /** Allow public warps */
        public boolean allowPublicWarps = true;
        /** Allow private warps */
        public boolean allowPrivateWarps = true;
        /** Allow admin warps */
        public boolean allowAdminWarps = true;
    }
    /** Example: Spawn settings configuration */
    public SpawnConfig spawnConfig = new SpawnConfig();

    /** Example: Configuration for spawn features */
    public static class SpawnConfig {
        /** Enable/disable spawn system */
        public boolean enabled = true;
        /** Set spawn on first join */
        public boolean setSpawnOnFirstJoin = true;
        /** Set spawn on respawn */
        public boolean setSpawnOnRespawn = true;
        /** Set spawn on death */
        public boolean setSpawnOnDeath = false;
        /** Main spawn location */
        public SpawnLocation mainSpawn = new SpawnLocation();
        /** New player settings */
        public NewPlayer newPlayer = new NewPlayer();
        /** Safety settings */
        public Safety safety = new Safety();
        /** Respawn settings */
        public Respawn respawn = new Respawn();

        public static class SpawnLocation {
            public String world = "minecraft:overworld";
            public double x = 0;
            public double y = 64;
            public double z = 0;
            public float yaw = 0;
            public float pitch = 0;
        }

        public static class NewPlayer {
            public boolean giveWelcomeMessage = true;
        }

        public static class Safety {
            public boolean enabled = true;
            public double safetySearchRadius = 16.0;
        }

        public static class Respawn {
            public boolean respectBedSpawns = true;
        }
    }
    /** Example: Home settings configuration */
    public HomeSettings homeSettings = new HomeSettings();

    /** Example: Configuration for home features */
    public static class HomeSettings {
        /** Enable/disable homes */
        public boolean enabled = true;
        /** Maximum homes per player */
        public int maxHomes = 3;
        /** Cooldown in seconds between home teleports */
        public int homeCooldown = 60;
        /** Cost to set a home */
        public double setHomeCost = 50.0;
        /** Cost to teleport to a home */
        public double teleportCost = 10.0;
        /** Allow cross-world homes */
        public boolean allowCrossWorld = false;
    /** List of restricted worlds for homes */
    public java.util.List<String> restrictedWorlds = new java.util.ArrayList<>();
    /** Use cost to set home */
    public boolean useSetHomeCost = true;
    /** Cost to teleport to a home */
    public double teleportHomeCost = 10.0;
    /** Require safe location for teleport */
    public boolean requireSafeLocation = true;
    /** Warmup time in seconds before teleport */
    public int teleportWarmup = 5;
    /** Maximum homes for admin */
    public int maxHomesAdmin = 10;
    /** Maximum homes for VIP */
    public int maxHomesVip = 5;
    /** Cooldown in seconds for home teleport */
    public int teleportHomeCooldown = 30;
    }
    /** Example: Economy settings configuration */
    public EconomySettings economySettings = new EconomySettings();

    /** Example: Configuration for economy features */
    public static class EconomySettings {
        /** Enable/disable economy */
        public boolean enabled = true;
        /** Currency symbol */
        public String currencySymbol = "$";
        /** Starting balance for new players */
        public double startingBalance = 100.0;
        /** Transaction fee percentage */
        public double transactionFeePercent = 1.0;
        /** Maximum transfer amount */
        public double maxTransferAmount = 10000.0;
        /** Enable/disable banking system */
        public boolean bankingEnabled = true;
        /** Enable/disable auction house */
        public boolean auctionEnabled = true;
        /** Enable/disable player shops */
        public boolean shopsEnabled = true;
        /** Enable/disable cross-server sync */
        public boolean crossServerSync = false;
        /** Tax rate percentage */
        public double taxRatePercent = 5.0;
        /** Economy analytics enabled */
        public boolean analyticsEnabled = true;
        /** Maximum balance allowed per player */
        public double maxBalance = 100000.0;
        /** Transfer fee percent (for money transfers) */
        public double transferFeePercent = 1.0;
        /** Currency format string for display */
        public String currencyFormat = "#,##0.00";
        /** Command costs (map of command name to cost) */
        public java.util.Map<String, java.math.BigDecimal> commandCosts = new java.util.HashMap<>();
        /** Bank settings */
        public BankSettings bank = new BankSettings();
        public static class BankSettings {
            public boolean enabled = true;
            public double interestRate = 2.5;
            public double minimumBalance = 100.0;
            public double maxInterestPayout = 500.0;
        }
        /** Cleanup inactive accounts */
        public boolean cleanupInactiveAccounts = true;
    }
    /** Example: Chat settings configuration */
    public ChatSettings chatSettings = new ChatSettings();

    /** Example: Configuration for chat and private messages */
    public static class ChatSettings {
    /** Group-based chat formats */
    public java.util.Map<String, String> groupFormats = new java.util.HashMap<>();
        /** Format for sender in private messages */
        public String pmFormatSender = "[PM to {RECEIVER}] {MESSAGE}";
        /** Format for receiver in private messages */
        public String pmFormatReceiver = "[PM from {SENDER}] {MESSAGE}";
        /** Enable/disable private messaging */
        public boolean enablePrivateMessages = true;
        /** Enable/disable chat formatting */
        public boolean enableChatFormatting = true;
    /** Chat format string (only formats the message) */
    public String chatFormat = "{MESSAGE}";
    /** Chat name format string (formats the name brackets) */
    public String chatname = "<{PREFIX}{DISPLAYNAME}{SUFFIX}>";
    /** Enable/disable chat formatting */
    public boolean isEnabled = true;
    /** Enable/disable chat colors */
    public boolean enableColors = true;

        /** Anti-spam settings */
        public AntiSpam antiSpam = new AntiSpam();
        public static class AntiSpam {
            public boolean enabled = true;
            public int maxMessagesPerSecond = 2;
            public int maxDuplicateMessages = 3;
        }

        /** Chat filter settings */
        public Filter filter = new Filter();
        public static class Filter {
            public boolean enabled = true;
            public boolean caseSensitive = false;
            public boolean censorMode = true;
            public String censorReplacement = "*";
            public java.util.List<String> blockedWords = new java.util.ArrayList<>();
        }

        /** Nickname settings */
        public Nicknames nicknames = new Nicknames();
        public static class Nicknames {
            public boolean enabled = true;
            public boolean showInChat = true;
            public boolean allowColors = true;
        }

        /** Prefix/suffix settings */
        public PrefixSuffix prefixSuffix = new PrefixSuffix();
        public static class PrefixSuffix {
            public boolean enabled = true;
            public boolean colorEnabled = true;
            public String defaultPrefix = "";
            public String defaultSuffix = "";
            public boolean permissionSystemEnabled = true;
            public boolean groupSystemEnabled = false;
            public boolean isPermissionSystemEnabled() { return permissionSystemEnabled; }
            public boolean isGroupSystemEnabled() { return groupSystemEnabled; }
            public boolean isColorEnabled() { return colorEnabled; }
        }
    }
    /** Example: Kit settings configuration */
    public KitSettings kitSettings = new KitSettings();

    /** Example: Configuration for kits */
    public static class KitSettings {
    /** Enable cooldowns for kits */
    public boolean enableCooldowns = true;
    /** Automatically equip kit items */
    public boolean autoEquip = true;
    /** Give kit on first join */
    public boolean giveKitOnFirstJoin = true;
    /** Name of kit to give on first join */
    public String firstJoinKit = "starter";
    /** Commands to run when kit is given */
    public java.util.List<String> commands = new java.util.ArrayList<>();

        /** Enable/disable kits */
        public boolean enabled = true;
        /** Default kit given on first join */
        /** Cooldown in seconds between kit uses */
        public int kitCooldown = 3600;
        /** Maximum kits per player */
        public int maxKitsPerPlayer = 5;
        /** Allow kit preview */
        public boolean allowPreview = true;
        /** Allow kit permissions */
        public boolean usePermissions = true;
    }
    /** Example: Item management configuration */
    public ItemManagementConfig itemManagement = new ItemManagementConfig();

    /** Example: Configuration for item management features */
    public static class ItemManagementConfig {
        /** Enable/disable item commands */
        public boolean enabled = true;
        /** Maximum stack size allowed for item commands */
        public int maxStackSize = 64;
        /** Allow giving items with custom NBT */
        public boolean allowCustomNbt = true;
        /** Allow giving items with enchantments */
        public boolean allowEnchantments = true;
        /** Cooldown in seconds between item commands */
        public int commandCooldown = 10;
    }
    /** Example: InvSee command configuration */
    public InvSeeConfig invseeConfig = new InvSeeConfig();

    /** Example: Configuration for /invsee command */
    public static class InvSeeConfig {
        /** Enable/disable /invsee command */
        public boolean enabled = true;
        /** Maximum number of inventories a player can view per session */
        public int maxViewsPerSession = 5;
        /** Allow viewing ender chest */
        public boolean allowEnderChest = true;
        /** Allow editing target inventory */
        public boolean allowEdit = false;
        /** Cooldown in seconds between uses */
        public int cooldownSeconds = 30;
    }
    /** Example: Maximum amount allowed for /give command */
    public int maxGiveAmount = 64;
    /** Example: Allow giving items with enchantments using /give */
    public boolean allowGiveEnchantments = true;
    // Example: Only show fields that are actually used and important for users
    /** Example: Server name shown in tablist, bossbar, etc. */
    public String serverName = "NeoServer";
    /** Example: Default language for messages */
    public String defaultLanguage = "en";
    /** Enable debug logging */
    public boolean debugMode = false;
    /** Enable/disable modules */
    public Modules modules = new Modules();

    /** Example: Home settings */
    public int maxHomes = 3; // Maximum homes per player
    public int homeCooldown = 60; // Cooldown in seconds
    public double setHomeCost = 50.0; // Cost to set a home

    /** Example: Economy settings */
    public boolean economyEnabled = true;
    public String currencySymbol = "$";
    public double startingBalance = 100.0;

    /** Example: Kit settings */
    public boolean kitsEnabled = true;
    public String firstJoinKit = "starter";

    /** Example: Warp settings */
    public boolean warpsEnabled = true;
    public int maxWarps = 10;

    /** Example: Chat format */
    public String chatFormat = "<{PREFIX}{DISPLAYNAME}{SUFFIX}> {MESSAGE}";

    // Tablist, scoreboard, and bossbar config are now controlled via TabListConfig and tablist.json
    // Tablist

    // Scoreboard

    // Bossbar

    // Bossbar welcome message

    /** Example: Permissions system */
    public boolean permissionsEnabled = true;

    /** Example: Animated placeholders */
    public boolean enableAnimations = true;

    /** Example: Custom placeholders */
    public java.util.Map<String, String> customPlaceholders = new java.util.HashMap<>();

    /** Example: Modules used in NeoEssentials */
    public static class Modules {
    /** Enable/disable chat module */
    public boolean chat = true;
        public boolean economy = true;
        public boolean homes = true;
        public boolean kits = true;
        public boolean warps = true;
        public boolean moderation = true;
        public boolean tablist = true;
        public boolean bossbar = true;
        public boolean spawn = true;
    }

}
