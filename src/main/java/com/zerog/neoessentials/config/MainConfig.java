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
    
    // Module enabling/disabling
    public ModulesConfig modules = new ModulesConfig();
    
    // Chat configuration
    public ChatConfig chat = new ChatConfig();
    
    // Color permissions
    public ColorPermissionsConfig colorPermissions = new ColorPermissionsConfig();
    
    // Command cooldowns
    public Map<String, Integer> commandCooldowns = new HashMap<>();
    
    // Protection settings
    public ProtectConfig protect = new ProtectConfig();
    
    // Anti-build settings  
    public AntiBuildConfig antiBuild = new AntiBuildConfig();
    
    // Newbie settings
    public NewbieConfig newbies = new NewbieConfig();
    
    // Additional EssentialsX-compatible configuration modules
    public EconomyConfig economy = new EconomyConfig();
    public HomeConfig homes = new HomeConfig();
    public KitConfig kits = new KitConfig();
    public WarpConfig warps = new WarpConfig();
    public ModerationConfig moderation = new ModerationConfig();
    public MessagingConfig messaging = new MessagingConfig();
    // Discord integration removed
    public TablistConfig tablist = new TablistConfig();
    public SpawnConfig spawn = new SpawnConfig();
    
    public MainConfig() {
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Default command cooldowns
        commandCooldowns.put("home", 60);
        commandCooldowns.put("feed", 30);
        commandCooldowns.put("*warp*", 120);
        commandCooldowns.put("heal", 45);
        commandCooldowns.put("fly", 10);
    }
    
    public static class ModulesConfig {
        // ==============================
        // CORE MODULES
        // ==============================
        public boolean antiBuild = true;
        public boolean chat = true;
        public boolean protect = true;
        public boolean spawn = true;
        public boolean economy = true;
        public boolean discord = false;
        public boolean tablist = true;
        public boolean kits = true;
        public boolean warps = true;
        public boolean homes = true;
        public boolean messaging = true;
        public boolean moderation = true;
        
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
            // Basic teleportation
            public boolean tp = true;
            public boolean tphere = true;
            public boolean tpall = true;
            public boolean tpa = true;
            public boolean tpaccept = true;
            public boolean tpdeny = true;
            public boolean tptoggle = true;
            
            // Home system
            public boolean home = true;
            public boolean sethome = true;
            public boolean delhome = true;
            public boolean homes = true;
            
            // Warp system
            public boolean warp = true;
            public boolean setwarp = true;
            public boolean delwarp = true;
            public boolean warps = true;
            
            // Spawn system
            public boolean spawn = true;
            public boolean setspawn = true;
            
            // Back system
            public boolean back = true;
            
            // Random teleport
            public boolean rtp = true;
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
