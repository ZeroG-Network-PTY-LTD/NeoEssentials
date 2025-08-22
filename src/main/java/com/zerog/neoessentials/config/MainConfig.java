package com.zerog.neoessentials.config;


/**
 * Main configuration for NeoEssentials mod
 * Based on EssentialsX configuration structure
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MainConfig {
    public ColorPermissionsConfig colorPermissionsConfig = new ColorPermissionsConfig();
    public InvseeConfig invseeConfig = new InvseeConfig();
    public static class InvseeConfig {
        public boolean enable = true;
        public boolean allowModify = false;
    }
    public HomeSettings homeSettings = new HomeSettings();
    public static class HomeSettings {
        public int maxHomes = 3;
        public int maxHomesVip = 5;
        public int maxHomesAdmin = 10;
        public int defaultMaxHomes = 3;
        public int cooldown = 60;
        public java.math.BigDecimal setHomeCost = new java.math.BigDecimal("50.00");
        public java.math.BigDecimal teleportHomeCost = new java.math.BigDecimal("10.00");
        public java.math.BigDecimal deleteHomeCost = java.math.BigDecimal.ZERO;
        public int setHomeCooldown = 300;
        public int teleportHomeCooldown = 60;
        public int deleteHomeCooldown = 30;
        public int teleportWarmup = 3;
        public boolean cancelOnMove = true;
        public boolean cancelOnDamage = true;
        public double maxMoveDistance = 0.5;
        public boolean requireSafeLocation = true;
        public java.util.List<String> restrictedWorlds = java.util.Arrays.asList();
        public boolean useSetHomeCost = true;
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
            public java.util.List<String> description = java.util.Arrays.asList();
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
        public boolean checkForVoid = true;
        public boolean checkForSuffocation = true;
        public int safeLocationRadius = 3;
        public boolean enablePublicWarps = true;
        public boolean enablePrivateWarps = true;
        public boolean allowCrossDimensionTeleport = true;
        public boolean enableWarpCategories = true;
        public java.util.List<String> defaultCategories = java.util.Arrays.asList("spawn", "shops", "arenas", "farms", "builds");
        public java.util.List<String> noTeleportWorlds = java.util.Arrays.asList();
        public int maxWarpNameLength = 20;
        public boolean allowSpacesInNames = false;
        public boolean allowSpecialCharacters = false;
        public java.util.List<String> bannedWarpNames = java.util.Arrays.asList("spawn", "home", "admin", "server", "console");
        public boolean enablePermissionWarps = true;
        public String permissionPrefix = "neoessentials.warp.";
        public boolean enableWarpSigns = true;
        public String warpSignFormat = "[Warp]";
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
        public boolean enabled = true;
        public int maxWarps = 50;
        public int maxWarpsPerPlayer = 5;
        public java.math.BigDecimal createWarpCost = new java.math.BigDecimal("500.00");
        public java.math.BigDecimal teleportWarpCost = new java.math.BigDecimal("25.00");
        public java.math.BigDecimal deleteWarpCost = java.math.BigDecimal.ZERO;
        public int createWarpCooldown = 600;
        public int teleportWarpCooldown = 120;
        public int deleteWarpCooldown = 60;
        public int teleportWarmup = 5;
        public boolean cancelOnMove = true;
        public boolean cancelOnDamage = true;
        public double maxMoveDistance = 0.5;
        public boolean requireSafeLocation = true;
        public boolean checkForLava = true;
        public java.util.List<String> restrictedWorlds = java.util.Arrays.asList();
    }
    public ModerationSettings moderationSettings = new ModerationSettings();
    public static class ModerationSettings {
        // ...fields from ModerationConfig...
    }
    public MessagingSettings messagingSettings = new MessagingSettings();
    public static class MessagingSettings {
        public boolean enabled = true;
        public boolean enablePrivateMessages = true;
        public boolean enableMail = true;
        public boolean enableReply = true;
        public boolean enableSocialSpy = true;
        public PrivateMessageConfig privateMessages = new PrivateMessageConfig();
        public MailConfig mail = new MailConfig();
        public SocialSpyConfig socialSpy = new SocialSpyConfig();
        public HelpOpConfig helpop = new HelpOpConfig();
        public BroadcastConfig broadcast = new BroadcastConfig();
        public MessagesConfig messages = new MessagesConfig();

        public static class PrivateMessageConfig {
            public boolean enabled = true;
            public boolean requirePermission = false;
            public boolean allowCrossWorld = true;
            public boolean allowOfflineMessages = false;
            public boolean logMessages = true;
            public int messageHistoryLimit = 50;
            public int cooldownSeconds = 3;
            public boolean enableToggle = true;
            public java.util.List<String> blockedWords = java.util.Arrays.asList("spam", "advertisement");
            public boolean enableIgnoreList = true;
            public int maxIgnoreListSize = 20;
        }
        public static class MailConfig {
            public boolean enabled = true;
            public int maxMailsPerPlayer = 50;
            public int maxMailsPerSender = 10;
            public int mailExpiryDays = 30;
            public boolean notifyOnJoin = true;
            public boolean notifyOnSend = true;
            public boolean requirePermissionToSend = false;
            public boolean allowAttachments = false;
            public int maxMailLength = 500;
            public int cooldownSeconds = 30;
        }
        public static class SocialSpyConfig {
            public boolean enabled = true;
            public boolean defaultEnabled = false;
            public boolean logToConsole = true;
            public boolean logToFile = true;
            public java.util.List<String> exemptPermissions = java.util.Arrays.asList("neoessentials.socialspy.exempt");
            public boolean showCommands = true;
            public boolean showPrivateMessages = true;
            public boolean showMail = false;
        }
        public static class HelpOpConfig {
            public boolean enabled = true;
        }
        public static class BroadcastConfig {
            public boolean enabled = true;
        }
        public static class MessagesConfig {
            public String pmFormatSocialSpy = "&7[Spy] {SENDER} -> {RECEIVER}: {MESSAGE}";
        }
    }
    public ChatSettings chatSettings = new ChatSettings();
    public static class ChatSettings {
    public boolean isEnabled() { return enabled; }
    public boolean enableColors = true;
    public boolean enabled = true;
    public String format = "{MESSAGE}";
    public boolean localChatOnly = false;
    public boolean enableNearbyChat = false;
    public int nearbyRadius = 100;
    public AntiSpamConfig antiSpam = new AntiSpamConfig();
    public FilterConfig filter = new FilterConfig();
    public NicknamesConfig nicknames = new NicknamesConfig();
    public PrefixSuffixConfig prefixSuffix = new PrefixSuffixConfig();

    // Configurable private message formats (for /msg)
    public String pmFormatSender = "[PM to {0}] {1}";
    public String pmFormatReceiver = "[PM from {0}] {1}";

        public static class AntiSpamConfig {
            public int maxMessagesPerSecond = 5;
            public int maxDuplicateMessages = 3;
            public boolean enabled = false;
            public int spamThreshold = 5;
            public int spamCooldown = 10;
        }
        public static class FilterConfig {
            public boolean caseSensitive = false;
            public boolean censorMode = false;
            public String censorReplacement = "*";
            public int maxMessagesPerSecond = 5;
            public int maxDuplicateMessages = 3;
            public boolean enabled = false;
            public java.util.List<String> blockedWords = java.util.Arrays.asList("spam", "advertisement");
        }
        public static class NicknamesConfig {
            public boolean enabled = true;
            public boolean showInChat = true;
            public boolean allowColors = true;
        }
        public static class PrefixSuffixConfig {
            public boolean isColorEnabled() { return true; }
            public boolean enabled = true;
            public String defaultPrefix = "";
            public String defaultSuffix = "";
            public boolean permissionSystemEnabled = true;
            public boolean groupSystemEnabled = false;
            public boolean isPermissionSystemEnabled() { return permissionSystemEnabled; }
            public boolean isGroupSystemEnabled() { return groupSystemEnabled; }
        }
    }
    public SpawnSettings spawnSettings = new SpawnSettings();
    public static class SpawnSettings {
        public SafetyConfig safety = new SafetyConfig();
        public static class SafetyConfig {
            public boolean enabled = true;
            public int safetySearchRadius = 5;
        }
        public boolean setSpawnOnRespawn = false;
        public boolean setSpawnOnDeath = false;
        public RespawnConfig respawn = new RespawnConfig();
        public static class RespawnConfig {
            public boolean respectBedSpawns = true;
        }
        public boolean setSpawnOnFirstJoin = true;
        public NewPlayerConfig newPlayer = new NewPlayerConfig();
        public Messages messages = new Messages();
        public static class NewPlayerConfig {
            public boolean giveWelcomeMessage = true;
        }
        public static class Messages {
            public String welcomeMessage = "&aWelcome to the server!";
        }
    public boolean enabled = true;
        public MainSpawn mainSpawn = new MainSpawn();
        public static class MainSpawn {
            public String world = "";
            public double x = 0.0;
            public double y = 64.0;
            public double z = 0.0;
            public float yaw = 0.0f;
            public float pitch = 0.0f;
        }
    }
    public EconomySettings economySettings = new EconomySettings();
    public static class EconomySettings {
        public boolean enabled = true;
        public String currencySymbol = "$";
        public String currencyName = "dollar";
        public String currencyNamePlural = "dollars";
        public String currencyFormat = "#,##0.00";
        public double startingBalance = 100.00;
        public double minimumBalance = 0.00;
        public double maxBalance = 10000000.00;
        public double minimumPayAmount = 0.01;
        public double maximumPayAmount = 10000.00;
        public boolean logTransactions = true;
        public double transferFeePercent = 0.0;
        public java.util.Map<String, java.math.BigDecimal> commandCosts = new java.util.HashMap<>();
        public VaultConfig vault = new VaultConfig();
        public ShopConfig shop = new ShopConfig();
        public BankConfig bank = new BankConfig();
        public boolean cleanupInactiveAccounts = false;
        public MessagesConfig messages = new MessagesConfig();
        public EconomySettings() {
            initializeDefaults();
        }
        private void initializeDefaults() {
            commandCosts.put("heal", new java.math.BigDecimal("10.00"));
            commandCosts.put("feed", new java.math.BigDecimal("5.00"));
            commandCosts.put("fly", new java.math.BigDecimal("20.00"));
            commandCosts.put("god", new java.math.BigDecimal("50.00"));
            commandCosts.put("repair", new java.math.BigDecimal("15.00"));
            commandCosts.put("kit", new java.math.BigDecimal("25.00"));
        }
        public static class VaultConfig {
            public boolean enabled = true;
            public String economyName = "NeoEssentials Economy";
            public boolean requireServer = false;
            public boolean supportBanks = false;
        }
        public static class ShopConfig {
            // Add shop config fields here as needed
        }
        public static class BankConfig {
            public boolean enabled = true;
            public double interestRate = 0.0;
            public double minimumBalance = 0.0;
            public double maxInterestPayout = 1000.0;
        }
        public static class MessagesConfig {
            public String insufficientFunds = "&cYou do not have enough funds: {AMOUNT}";
            public String commandCostCharged = "&aCharged {AMOUNT} for command: {COMMAND}";
        }
    }
    // ...repeat for HomeSettings, KitSettings, WarpSettings, ModerationSettings, MessagingSettings, ChatSettings, SpawnSettings...
    
    public String serverName = "NeoServer";
    public String defaultLanguage = "en";
    public boolean debugMode = false;
    public Modules modules = new Modules();
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
        /**
         * Enable/disable cost for setting a home
         */
        public boolean useSetHomeCost = true;
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
                // EssentialsX-style config options for /give
                public int maxGiveAmount = 64;
                public boolean allowGiveEnchantments = true;
            
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
