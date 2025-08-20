package com.zerog.neoessentials.config;


/**
 * Main configuration for NeoEssentials mod
 * Based on EssentialsX configuration structure
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MainConfig {
    public HomeSettings homeSettings = new HomeSettings();
    public static class HomeSettings {
        // ...fields from HomeConfig...
    }
    public KitSettings kitSettings = new KitSettings();
    public static class KitSettings {
        // ...fields from KitConfig...
    }
    public WarpSettings warpSettings = new WarpSettings();
    public static class WarpSettings {
        // ...fields from WarpConfig...
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
        // ...fields from ChatConfig...
    }
    public SpawnSettings spawnSettings = new SpawnSettings();
    public static class SpawnSettings {
        // ...fields from SpawnConfig...
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
            // Add bank config fields here as needed
        }
        public static class MessagesConfig {
            // Add economy messages here as needed
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
