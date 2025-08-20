package com.zerog.neoessentials.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Main configuration for NeoEssentials mod
 * Based on EssentialsX configuration structure
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MainConfig {
    public KitSettings kitSettings = new KitSettings();
    public static class KitSettings {
        public boolean giveKitOnFirstJoin = true;
        public String firstJoinKit = "starter";
        public boolean enableCooldowns = true;
        public boolean persistCooldowns = true;
        public boolean enableKitCosts = true;
        public boolean enabled = true;
        public java.util.Map<String, KitDefinition> kits = new java.util.HashMap<>();
        public MessagesConfig messages = new MessagesConfig();

        public static class KitDefinition {
            public String displayName = "";
            public java.util.List<String> items = java.util.Arrays.asList();
            public int cooldown = 0;
            public java.math.BigDecimal cost = java.math.BigDecimal.ZERO;
            public boolean clearInventory = false;
            public String permission = "";
            public int delay = 0;
            public boolean autoEquip = false;
            public java.util.List<String> commands = java.util.Arrays.asList();

            public boolean hasCost() { return cost != null && cost.compareTo(java.math.BigDecimal.ZERO) > 0; }
            public boolean hasDelay() { return delay > 0; }
        }

        public static class MessagesConfig {
            public String kitNoPermission = "&cYou don't have permission for this kit!";
            public String kitCooldown = "&cYou must wait {TIME} before using this kit again.";
            public String kitCost = "&cKit '{KIT}' costs {AMOUNT}. You need {REQUIRED} more.";
            public String inventoryFull = "&cYour inventory is full!";
            public String kitGiven = "&aKit '{KIT}' given!";
            public String kitNotFound = "&cKit '{KIT}' not found!";
            public String cooldownActive = "&cYou must wait {TIME} before using this kit again.";
            public String insufficientFunds = "&cYou do not have enough funds: {AMOUNT}";
            public String kitListHeader = "&aAvailable Kits:";
            public String kitListEntry = "&e- {KIT}";
            public String kitListEmpty = "&cNo kits available!";
            public String firstJoinKit = "&aYou have received your first join kit!";
        }
    }
    public WarpSettings warpSettings = new WarpSettings();
    public static class WarpSettings {
    /**
     * Check for void below player when teleporting to warp
     */
    public boolean checkForVoid = true;
    /**
     * Check for suffocation blocks at warp location
     */
    public boolean checkForSuffocation = true;
    /**
     * Radius to check for safe location around warp
     */
    public int safeLocationRadius = 3;
    /**
     * Enable public warps (accessible by all players)
     */
    public boolean enablePublicWarps = true;
    /**
     * Enable private warps (owner only)
     */
    public boolean enablePrivateWarps = true;
    /**
     * Allow teleporting between dimensions
     */
    public boolean allowCrossDimensionTeleport = true;
    /**
     * Enable warp categories (group warps by type)
     */
    public boolean enableWarpCategories = true;
    /**
     * Default categories for warps
     */
    public java.util.List<String> defaultCategories = java.util.Arrays.asList("spawn", "shops", "arenas", "farms", "builds");
    /**
     * Worlds where teleporting to warps is not allowed
     */
    public java.util.List<String> noTeleportWorlds = java.util.Arrays.asList();
    /**
     * Maximum length for warp names
     */
    public int maxWarpNameLength = 20;
    /**
     * Allow spaces in warp names
     */
    public boolean allowSpacesInNames = false;
    /**
     * Allow special characters in warp names
     */
    public boolean allowSpecialCharacters = false;
    /**
     * List of banned warp names
     */
    public java.util.List<String> bannedWarpNames = java.util.Arrays.asList("spawn", "home", "admin", "server", "console");
    /**
     * Enable permission-based warps
     */
    public boolean enablePermissionWarps = true;
    /**
     * Permission prefix for warps
     */
    public String permissionPrefix = "neoessentials.warp.";
    /**
     * Enable warp signs (physical sign teleport)
     */
    public boolean enableWarpSigns = true;
    /**
     * Format for warp signs
     */
    public String warpSignFormat = "[Warp]";
    /**
     * Cost to use a warp sign
     */
    public java.math.BigDecimal warpSignCost = new java.math.BigDecimal("100.00");
        public MessagesConfig messages = new MessagesConfig();

        public static class MessagesConfig {
            public String warpCreated = "&aWarp '{0}' created successfully!";
            public String warpDeleted = "&cWarp '{0}' deleted!";
            public String warpNotFound = "&cWarp '{0}' not found!";
            public String warpTeleporting = "&aTeleporting to warp '{0}'...";
            public String warpTeleportCancelled = "&cTeleport cancelled due to movement!";
            public String maxWarpsReached = "&cMaximum number of warps reached ({0})!";
            public String warpAlreadyExists = "&cWarp '{0}' already exists!";
            public String invalidWarpName = "&cInvalid warp name! Use only letters and numbers.";
            public String warpListHeader = "&6Available warps:";
            public String warpListEntry = "&7- &a{0} &7({1}) [{2}] &8({3} {4}, {5}, {6})";
            public String warpListEmpty = "&cNo warps available!";
            public String unsafeLocation = "&cUnsafe location! Teleport cancelled.";
            public String restrictedWorld = "&cYou cannot create warps in this world!";
            public String noTeleportWorld = "&cYou cannot teleport to this world!";
            public String insufficientFunds = "&cYou need {0} to create/use this warp!";
            public String cooldownActive = "&cYou must wait {0} before using this command again!";
            public String warpNoPermission = "&cYou don't have permission to access this warp!";
            public String warpPrivate = "&cThis is a private warp!";
            public String warpSignCreated = "&aWarp sign created for '{0}'!";
            public String warpSignUsed = "&aTeleporting via warp sign...";
            public String categoryNotFound = "&cCategory '{0}' not found!";
            public String categoryListHeader = "&6Warp categories:";
            public String categoryListEntry = "&7- &a{0} &7({1} warps)";
            public String categoryWarpsHeader = "&6Warps in category '{0}':";
        }
        // Utility methods from WarpConfig can be migrated here as needed
    /**
     * Enable warp system
     */
    public boolean enabled = true;
    /**
     * Maximum number of warps allowed on the server
     */
    public int maxWarps = 50;
    /**
     * Maximum number of warps per player
     */
    public int maxWarpsPerPlayer = 5;
    /**
     * Cost to create a warp
     */
    public java.math.BigDecimal createWarpCost = new java.math.BigDecimal("500.00");
    /**
     * Cost to teleport to a warp
     */
    public java.math.BigDecimal teleportWarpCost = new java.math.BigDecimal("25.00");
    /**
     * Cost to delete a warp
     */
    public java.math.BigDecimal deleteWarpCost = java.math.BigDecimal.ZERO;
    /**
     * Cooldown (seconds) after creating a warp
     */
    public int createWarpCooldown = 600;
    /**
     * Cooldown (seconds) after teleporting to a warp
     */
    public int teleportWarpCooldown = 120;
    /**
     * Cooldown (seconds) after deleting a warp
     */
    public int deleteWarpCooldown = 60;
    /**
     * Warmup (seconds) before teleporting to a warp
     */
    public int teleportWarmup = 5;
    /**
     * Cancel teleport if player moves
     */
    public boolean cancelOnMove = true;
    /**
     * Cancel teleport if player takes damage
     */
    public boolean cancelOnDamage = true;
    /**
     * Maximum allowed movement distance before canceling teleport
     */
    public double maxMoveDistance = 0.5;
    /**
     * Require safe location for teleport
     */
    public boolean requireSafeLocation = true;
    /**
     * Check for lava at warp location
     */
    public boolean checkForLava = true;
    /**
     * Worlds where warps are restricted
     */
    public java.util.List<String> restrictedWorlds = java.util.Arrays.asList();
    }
    
    public String serverName = "NeoServer";
    public String defaultLanguage = "en";
    public boolean debugMode = false;
    public Modules modules = new Modules();
    public HomeSettings homeSettings = new HomeSettings();
    public static class HomeSettings {
    /**
     * Maximum number of homes a regular player can set
     */
    public int maxHomes = 3;
    /**
     * Maximum number of homes a VIP player can set
     */
    public int maxHomesVip = 5;
    /**
     * Maximum number of homes an admin can set
     */
    public int maxHomesAdmin = 10;
    /**
     * Default maximum homes for new players
     */
    public int defaultMaxHomes = 3;
    /**
     * Cooldown (seconds) between home-related actions
     */
    public int cooldown = 60; // seconds
    /**
     * Cost to set a home
     */
    public java.math.BigDecimal setHomeCost = new java.math.BigDecimal("50.00");
    /**
     * Cost to teleport to a home
     */
    public java.math.BigDecimal teleportHomeCost = new java.math.BigDecimal("10.00");
    /**
     * Cost to delete a home
     */
    public java.math.BigDecimal deleteHomeCost = java.math.BigDecimal.ZERO;
    /**
     * Cooldown (seconds) after setting a home
     */
    public int setHomeCooldown = 300; // 5 minutes
    /**
     * Cooldown (seconds) after teleporting to a home
     */
    public int teleportHomeCooldown = 60; // 1 minute
    /**
     * Cooldown (seconds) after deleting a home
     */
    public int deleteHomeCooldown = 30; // 30 seconds
    /**
     * Warmup (seconds) before teleporting to a home
     */
    public int teleportWarmup = 3; // 3 seconds warmup
    /**
     * Cancel teleport if player moves
     */
    public boolean cancelOnMove = true;
    /**
     * Cancel teleport if player takes damage
     */
    public boolean cancelOnDamage = true;
    /**
     * Maximum allowed movement distance before canceling teleport
     */
    public double maxMoveDistance = 0.5; // blocks
    /**
     * Require safe location for teleport
     */
    public boolean requireSafeLocation = true;
    /**
     * Worlds where homes are restricted
     */
    public java.util.List<String> restrictedWorlds = java.util.Arrays.asList();
        public Messages messages = new Messages();

        public static class Messages {
            public String invalidHomeName = "&cInvalid home name!";
            public String maxHomesReached = "&cYou have reached the maximum number of homes: {MAX}";
            public String restrictedWorld = "&cYou cannot set a home in this world.";
            public String insufficientFunds = "&cYou do not have enough funds: {AMOUNT}";
            public String homeSet = "&aHome '{HOME}' set!";
            public String homeNotFound = "&cHome '{HOME}' not found!";
            public String homeDeleted = "&aHome '{HOME}' deleted!";
            public String homeListEmpty = "&cYou have no homes set.";
            public String homeListHeader = "&aYour Homes ({COUNT}/{MAX}):";
            public String homeListEntry = "&e- {HOME}";
            public String cooldownActive = "&cYou must wait {TIME} before teleporting again.";
            public String unsafeLocation = "&cHome location is not safe!";
            public String homeTeleporting = "&aTeleporting to home '{HOME}'...";
        }
    }
    public Database database = new Database();

    public static class Modules {
    public boolean economy = true;
    public boolean homes = true;
    public boolean kits = true;
    public boolean warps = true;
    public boolean moderation = true;
    public ChatModules chat = new ChatModules();
    public boolean tablist = true;
    public boolean bossbar = true;
    public boolean spawn = true;

        public static class ChatModules {
            public boolean enabled = true;
            public boolean messaging = true;
        }
    }

    public static class Database {
        public String type = "mysql"; // Database type SQLite, Mysql, Flatfile
        public String host = "localhost";
        public int port = 3306;
        public String username = "root";
        public String password = "password";
        public String database = "neoessentials";
    }
        
        // ==============================
        // ESSENTIAL COMMANDS
        // ==============================
        public EssentialCommandsConfig essentialCommands = new EssentialCommandsConfig();
        
        // ==============================
        // TELEPORTATION SYSTEM
        // ==============================
        public TeleportationConfig teleportation = new TeleportationConfig();
        
        // ==============================
        // PLAYER MANAGEMENT
        // ==============================
        public PlayerManagementConfig playerManagement = new PlayerManagementConfig();
        
        // ==============================
        // ITEM MANAGEMENT
        // ==============================
        public ItemManagementConfig itemManagement = new ItemManagementConfig();
        
        // ==============================
        // UTILITY FEATURES
        // ==============================
        public UtilityConfig utilities = new UtilityConfig();
        
        // ==============================
        // INTEGRATION FEATURES
        // ==============================
        public IntegrationConfig integrations = new IntegrationConfig();
        
        public static class EssentialCommandsConfig {
            // Basic player commands
            public boolean heal = true;
            public boolean feed = true;
            public boolean fly = true;
            public boolean god = true;
            public boolean vanish = true;
            public boolean speed = true;
            public boolean gamemode = true;
            
            // Admin commands
            public boolean time = true;
            public boolean weather = true;
            public boolean give = true;
            public boolean repair = true;
            public boolean clear = true;
            public boolean invsee = true;
            public boolean enderchest = true;
            
            // Utility commands
            public boolean workbench = true;
            public boolean anvil = true;
            public boolean enchantingtable = true;
            public boolean smithingtable = true;
            public boolean grindstone = true;
            public boolean loom = true;
            public boolean cartographytable = true;
            public boolean stonecutter = true;
        }
        
        public static class TeleportationConfig {
            /**
             * Enable /tp command (teleport to another player)
             */
            public boolean tp = true;
            /**
             * Enable /tphere command (teleport another player to you)
             */
            public boolean tphere = true;
            /**
             * Enable /tpall command (teleport all players to you)
             */
            public boolean tpall = true;
            /**
             * Enable /tpa command (request teleport to another player)
             */
            public boolean tpa = true;
            /**
             * Enable /tpaccept command (accept teleport request)
             */
            public boolean tpaccept = true;
            /**
             * Enable /tpdeny command (deny teleport request)
             */
            public boolean tpdeny = true;
            /**
             * Enable /tptoggle command (toggle receiving teleport requests)
             */
            public boolean tptoggle = true;

            /**
             * Enable /home command (teleport to a saved home)
             */
            public boolean home = true;
            /**
             * Enable /sethome command (set a new home location)
             */
            public boolean sethome = true;
            /**
             * Enable /delhome command (delete a saved home)
             */
            public boolean delhome = true;
            /**
             * Enable /homes command (list all saved homes)
             */
            public boolean homes = true;

            /**
             * Enable /warp command (teleport to a warp point)
             */
            public boolean warp = true;
            /**
             * Enable /setwarp command (set a new warp point)
             */
            public boolean setwarp = true;
            /**
             * Enable /delwarp command (delete a warp point)
             */
            public boolean delwarp = true;
            /**
             * Enable /warps command (list all warp points)
             */
            public boolean warps = true;

            /**
             * Enable /spawn command (teleport to server spawn)
             */
            public boolean spawn = true;
            /**
             * Enable /setspawn command (set server spawn location)
             */
            public boolean setspawn = true;

            /**
             * Enable /back command (teleport to previous location)
             */
            public boolean back = true;

            /**
             * Enable /rtp command (random teleport)
             */
            public boolean rtp = true;
            /**
             * Enable /randomtp command (alias for random teleport)
             */
            public boolean randomtp = true;
        }
        
        public static class PlayerManagementConfig {
            // Player information
            public boolean list = true;
            public boolean whois = true;
            public boolean seen = true;
            public boolean realname = true;
            
            // Nickname system
            public boolean nick = true;
            public boolean delnick = true;
            
            // Player states
            public boolean afk = true;
            public boolean afkcheck = true;
            
            // Player data
            public boolean playerdata = true;
            public boolean exp = true;
            public boolean skull = true;
        }
        
        public static class ItemManagementConfig {
            // Item commands
            public boolean give = true;
            public boolean item = true;
            public boolean more = true;
            public boolean repair = true;
            public boolean enchant = true;
            
            // Inventory management
            public boolean clear = true;
            public boolean invsee = true;
            public boolean enderchest = true;
            
            // Item utilities
            public boolean hat = true;
            public boolean disposal = true;
            public boolean condense = true;
        }
        
        public static class UtilityConfig {
            // World editing
            public boolean fill = true;
            public boolean clone = true;
            public boolean setblock = true;
            
            // Information commands
            public boolean coords = true;
            public boolean depth = true;
            public boolean getpos = true;
            public boolean biome = true;
            
            // Utility features
            public boolean jump = true;
            public boolean top = true;
            public boolean up = true;
            public boolean thru = true;
            
            // Signs and books
            public boolean editsign = true;
            public boolean book = true;
            
            // Chat utilities
            public boolean me = true;
            public boolean say = true;
            public boolean broadcast = true;
        }
        
        public static class IntegrationConfig {
            // External mod compatibility
            public boolean disableIfEssentialsXFound = true;
            public boolean disableIfEssentialsFound = true;
            public boolean disableIfCMIFound = true;
            public boolean disableIfLuckPermsFound = false;
            
            // Feature conflicts
            public boolean disableEconomyIfVaultFound = false;
            public boolean disablePermissionsIfLPFound = false;
            public boolean disableChatIfChatModFound = false;
            public boolean disableTablistIfTabModFound = false;
            
            // Specific mod integrations
            public boolean enableWorldEditIntegration = true;
            public boolean enableJEIIntegration = true;
            public boolean enableJourneyMapIntegration = true;
            public boolean enableWaystoneIntegration = true;
        }

    public static class ChatConfig {
        public int radius = 0; // 0 = global chat
        public String format = "<{PREFIX}{DISPLAYNAME}{SUFFIX}> {MESSAGE}";
        public boolean localChatOnly = false;
        public boolean enableNearbyChat = false;
        public int nearbyRadius = 100;
    }
    
    public static class ColorPermissionsConfig {
        public boolean nick = true;
        public boolean chat = true;
        public boolean rgb = true;
        public boolean signs = true;
        public boolean books = true;
    }
    
    public static class ProtectConfig {
        public boolean fireSpread = false;
        public boolean creeperExplosion = false;
        public boolean tntExplosion = false;
        public boolean endermenPickup = false;
        public boolean mobGriefing = false;
        public boolean preventLavaFlow = false;
        public boolean preventWaterFlow = false;
    }
    
    public static class AntiBuildConfig {
        public boolean build = true;
        public boolean use = true;
        public boolean warnOnBuildDisallow = true;
        public boolean alertOnViolation = true;
        public String alertMessage = "&cPlayer {PLAYER} tried to build without permission!";
    }
    
    public static class NewbieConfig {
        public String announceFormat = "&dWelcome {DISPLAYNAME}&d to the server!";
        public String kit = "tools";
        public String respawnListenerPriority = "high";
        public boolean giveKitOnJoin = true;
        public boolean announceOnJoin = true;
        public int protectionTime = 300; // 5 minutes of protection
    }
}
