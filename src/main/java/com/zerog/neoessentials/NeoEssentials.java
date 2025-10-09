package com.zerog.neoessentials;
import com.zerog.neoessentials.commands.ModRootCommand;
import com.zerog.neoessentials.commands.CommandRegistry;

import com.zerog.neoessentials.economy.commands.EconomyCommands;

import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.economy.managers.PayToggleManager;
import com.zerog.neoessentials.economy.managers.TransactionHistoryManager;
import com.zerog.neoessentials.items.commands.DisposeCommand;
import com.zerog.neoessentials.permissions.PermissionManager;
import com.zerog.neoessentials.permissions.PermissionStorage;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.permissions.command.PermissionsCommand;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.permissions.LuckPermsAdapter;
import com.zerog.neoessentials.permissions.FtbRanksAdapter;
import net.neoforged.fml.ModList;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.api.ChatAPI;


import java.io.File;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Mod("neoessentials")
public class NeoEssentials {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentials.class);
    
    /**
     * Manages all chat-related config and logic for the mod.
     * Initialized from config.json (chat and commands sections).
     */
    private ChatManager chatManager; // Used in future chat system integration
    /**
     * Main mod constructor. Loads configs, sets up permissions, registers event handlers, and initializes chat system.
     */
    public NeoEssentials() {
        LOGGER.info("=== NeoEssentials CONSTRUCTOR STARTING ===");
        // Initialize centralized configuration system
        com.zerog.neoessentials.config.ConfigManager.getInstance().loadAll();
        LOGGER.info("=== ABOUT TO CALL initializePlaceholderAPI ===");
        // Initialize PlaceholderAPI with default placeholders
        initializePlaceholderAPI();
        LOGGER.info("=== COMPLETED initializePlaceholderAPI CALL ===");
        
        ensureServerLangFile();
        // Initialize the core manager
        NeoEssentialsManager.getInstance();
        
        // Initialize the kit manager
        try {
            com.zerog.neoessentials.kits.KitManager.getInstance().initialize();
            LOGGER.info("Kit Manager initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Kit Manager: {}", e.getMessage(), e);
        }

    // Suppress unused field warning for chatManager (placeholder for future integration)
    assert chatManager != null || true;

        // --- Permissions module config ---
        com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
        
        // Read permissions config through ConfigManager
        boolean permissionsEnabled = true;
        String integration = "auto";
        try {
            JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            
            // Read modules.permissionsEnabled
            if (config.has("modules")) {
                JsonObject modulesObj = config.getAsJsonObject("modules");
                if (modulesObj.has("permissionsEnabled")) {
                    permissionsEnabled = modulesObj.get("permissionsEnabled").getAsBoolean();
                }
            }
            
            // Read permissions.integration
            if (config.has("permissions")) {
                JsonObject permObj = config.getAsJsonObject("permissions");
                if (permObj.has("integration")) {
                    integration = permObj.get("integration").getAsString();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read permissions config: {}", e.getMessage(), e);
        }

    if (permissionsEnabled) {
            // Ensure default permissions file exists before loading
            ensureDefaultPermissionsFile();
            
            // Adapter selection logic
            boolean luckPermsLoaded = ModList.get().isLoaded("luckperms");
            boolean ftbRanksLoaded = ModList.get().isLoaded("ftbranks");
            PermissionManager permManager = new PermissionManager();
            try {
                PermissionStorage.load(permManager);
            } catch (Exception e) {
                LOGGER.error("Failed to load permission storage", e);
            }
            PermissionAPI.setManager(permManager);
            // Integration selection
            if ("luckperms".equalsIgnoreCase(integration)) {
                if (luckPermsLoaded) {
                    PermissionAPI.setExternalAdapter(new LuckPermsAdapter());
                } else {
                    LOGGER.error("LuckPerms integration forced but LuckPerms not loaded!");
                }
            } else if ("ftbranks".equalsIgnoreCase(integration)) {
                if (ftbRanksLoaded) {
                    PermissionAPI.setExternalAdapter(new FtbRanksAdapter());
                } else {
                    LOGGER.error("FTB Ranks integration forced but FTB Ranks not loaded!");
                }
            } else if ("internal".equalsIgnoreCase(integration)) {
                PermissionAPI.setExternalAdapter(null);
            } else { // auto
                if (luckPermsLoaded) {
                    PermissionAPI.setExternalAdapter(new LuckPermsAdapter());
                } else if (ftbRanksLoaded) {
                    PermissionAPI.setExternalAdapter(new FtbRanksAdapter());
                } else {
                    PermissionAPI.setExternalAdapter(null);
                }
            }
        } else {
            LOGGER.info("Permissions module is disabled via config.");
        }

    // Register this mod class with the NeoForge event bus for non-static event handlers
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    // Register item interaction handler for powertool functionality
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.items.handlers.ItemInteractionHandler.class);
    // Removed DataComponentType registration for server-only compatibility

        // Load chat config and commands config for ChatManager using ConfigManager
        try {
            JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            JsonObject chatObj = config.has("chat") ? config.getAsJsonObject("chat") : new JsonObject();
            JsonObject commandsObj = config.has("commands") ? config.getAsJsonObject("commands") : new JsonObject();
            chatManager = new ChatManager(chatObj, commandsObj);
                
            // Set the ChatManager in ChatAPI for global access
            ChatAPI.setChatManager(chatManager);
            LOGGER.info("ChatManager initialized with chat-format: {}", chatManager.getChatFormat());
        } catch (Exception e) {
            LOGGER.error("Failed to load chat config: {}", e.getMessage(), e);
        }

        // Register chat event handler for message formatting
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.ChatHandler.class);
        
        // Register player join/quit message handler for custom messages
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.handlers.PlayerJoinQuitHandler.class);
        
        // Register AFK system event handlers for comprehensive activity tracking
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.handlers.AfkActivityHandler.class);
        // Enhanced AFK activity handler with pattern detection and anti-abuse measures
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.handlers.EnhancedAfkActivityHandler.class);
        // AfkMovementHandler disabled - no working tick events in this version
        // net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.handlers.AfkMovementHandler.class);
        // Register movement detector for position-based activity tracking
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.handlers.AfkMovementDetector.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.handlers.AfkCommandHandler.class);
        // AfkSleepHandler disabled - no working sleep events in this version
        // net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.handlers.AfkSleepHandler.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.handlers.AfkTablistHandler.class);
        
        // Initialize chat integrations for external plugins
        initializeChatIntegrations();

        // --- Chat event listeners ---
        // All chat event logic (join/quit, AFK, death, etc.) is handled via event handlers below.
        // ✅ IMPLEMENTED SUGGESTIONS:
        //   ✅ Localize all user-facing messages (see en_us.json)
        //   ✅ Add advanced formatting (hover/click events, color codes)
        //   ✅ Integrate with external chat plugins (e.g., DiscordSRV)
        //   ✅ Add runtime config reload support
        //   - Add more AFK and advanced chat event logic as needed
}

    /**
     * Initialize chat integration adapters for external NeoForge mods
     */
    private void initializeChatIntegrations() {
        try {
            int initializedAdapters = 0;
            
            // Initialize DCIntegration (Discord Integration) mod if available
            com.zerog.neoessentials.integrations.impl.DCIntegrationAdapter dcIntegrationAdapter = 
                new com.zerog.neoessentials.integrations.impl.DCIntegrationAdapter();
            
            if (dcIntegrationAdapter.initialize() && dcIntegrationAdapter.isEnabled()) {
                com.zerog.neoessentials.integrations.ChatIntegrationManager.registerAdapter(dcIntegrationAdapter);
                LOGGER.info("DCIntegration mod integration initialized successfully");
                initializedAdapters++;
            }
            
            // Initialize Simple Discord Link (SDLink) mod if available
            com.zerog.neoessentials.integrations.impl.SDLinkAdapter sdLinkAdapter = 
                new com.zerog.neoessentials.integrations.impl.SDLinkAdapter();
            
            if (sdLinkAdapter.initialize() && sdLinkAdapter.isEnabled()) {
                com.zerog.neoessentials.integrations.ChatIntegrationManager.registerAdapter(sdLinkAdapter);
                LOGGER.info("Simple Discord Link mod integration initialized successfully");
                initializedAdapters++;
            }
            
            // Add more NeoForge mod integrations here as needed
            // e.g., other chat-related mods, webhook mods, etc.
            
            LOGGER.info("Chat mod integrations initialized: {} adapters active", initializedAdapters);
                
        } catch (Exception e) {
            LOGGER.error("Failed to initialize chat mod integrations: {}", e.getMessage(), e);
        }
    }



    private void ensureServerLangFile() {
        try {
            File serverLangDir = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "lang");
            if (!serverLangDir.exists()) serverLangDir.mkdirs();
            File serverLangFile = new File(serverLangDir, "en_us.json");
            if (!serverLangFile.exists()) {
                // Try to copy from mod jar resources
                try (InputStream in = com.zerog.neoessentials.util.ResourceUtil.getJarLanguageResource("en_us")) {
                    if (in != null) {
                        Files.copy(in, serverLangFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.info("Copied default language file to server directory: {}", serverLangFile.getAbsolutePath());
                    } else {
                        LOGGER.warn("Could not find default language file in mod resources");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to ensure server language file", e);
        }
    }

    private void ensureDefaultPermissionsFile() {
        try {
            File permissionsFile = com.zerog.neoessentials.util.ResourceUtil.getConfigFile("permissions.json");
            
            // Check if file doesn't exist or is empty/has no groups
            boolean needsDefault = false;
            if (!permissionsFile.exists()) {
                needsDefault = true;
            } else {
                // Check if file is empty or has no groups
                try {
                    String content = Files.readString(permissionsFile.toPath());
                    if (content.trim().isEmpty() || content.contains("\"groups\": []") || !content.contains("\"groups\"")) {
                        needsDefault = true;
                    }
                } catch (Exception e) {
                    needsDefault = true;
                }
            }
            
            if (needsDefault) {
                // Try to copy from mod jar resources
                try (InputStream in = this.getClass().getResourceAsStream("/data/permissions.json")) {
                    if (in != null) {
                        permissionsFile.getParentFile().mkdirs();
                        Files.copy(in, permissionsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.info("Copied default permissions file to server config: {}", permissionsFile.getAbsolutePath());
                    } else {
                        LOGGER.warn("Could not find default permissions file in mod resources, creating minimal config");
                        createMinimalPermissionsFile(permissionsFile);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to copy default permissions file, creating minimal config", e);
                    createMinimalPermissionsFile(permissionsFile);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to ensure permissions file", e);
        }
    }

    private void createMinimalPermissionsFile(File permissionsFile) {
        try {
            String defaultContent = """
                {
                  "groups": [
                    {
                      "name": "default",
                      "prefix": "",
                      "suffix": "",
                      "permissions": [
                        "neoessentials.economy.balance",
                        "neoessentials.economy.pay",
                        "neoessentials.item.repair",
                        "neoessentials.chat.msg"
                      ],
                      "inherits": []
                    },
                    {
                      "name": "admin",
                      "prefix": "[Admin] ",
                      "suffix": "",
                      "permissions": [
                        "neoessentials.*"
                      ],
                      "inherits": ["default"]
                    }
                  ]
                }
                """;
            
            permissionsFile.getParentFile().mkdirs();
            Files.writeString(permissionsFile.toPath(), defaultContent);
            LOGGER.info("Created minimal permissions file: {}", permissionsFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to create minimal permissions file", e);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandRegistry registry = CommandRegistry.getInstance();
        
        LOGGER.info("Registering NeoEssentials commands...");
        
        // Clear registry for fresh registration
        registry.clear();
        
        // Economy commands
        try {
            EconomyCommands.register(dispatcher);
            // Register economy commands in the registry
            registry.registerCommand("balance", "Display your or another player's balance", "bal");
            registry.registerCommand("pay", "Send money to another player");
            registry.registerCommand("paytoggle", "Toggle accepting payments");
            registry.registerCommand("eco", "Admin economy commands (give, take, set, history)");
            registry.registerCommand("baltop", "Display top player balances");
            LOGGER.info("Economy commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register economy commands", e);
        }
        
        // Permission commands
        try {
            PermissionsCommand.register(dispatcher);
            registry.registerCommand("pex", "Permission management commands", "permissions");
            registry.registerCommand("permissions", "Permission management commands", "pex");
            LOGGER.info("Permission commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register permission commands", e);
        }
        
        // Item commands
        try {
            com.zerog.neoessentials.items.commands.RepairCommand.register(dispatcher);
            registry.registerCommand("repair", "Repair items in hand or inventory", "fix");
            
            com.zerog.neoessentials.items.commands.DisposeCommand.register(dispatcher);
            registry.registerCommand("dispose", "Safely dispose of items with confirmation", "trash");
            
            com.zerog.neoessentials.items.commands.ClearInventoryCommand.register(dispatcher);
            registry.registerCommand("clearinventory", "Clear player inventory", "ci", "clearinv");
            
            com.zerog.neoessentials.items.commands.EnchantCommand.register(dispatcher);
            registry.registerCommand("enchant", "Enchant items with specific enchantments");
            
            com.zerog.neoessentials.items.commands.PowertoolCommand.register(dispatcher);
            registry.registerCommand("powertool", "Bind commands to items", "pt");

            com.zerog.neoessentials.items.commands.PowertoolToggleCommand.register(dispatcher);
            registry.registerCommand("powertooltoggle", "Toggle powertool functionality", "pttoggle");            LOGGER.info("Item commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register item commands", e);
        }
        
        // Chat commands
        try {
            // Register custom messaging commands  
            System.out.println("DEBUG: Registering custom messaging commands");
            
            com.zerog.neoessentials.chat.command.MsgCommand.register(dispatcher);
            registry.registerCommand("msg", "Send private messages to players");
            
            com.zerog.neoessentials.chat.command.IgnoreCommand.register(dispatcher);
            registry.registerCommand("ignore", "Ignore messages from a player");
            
            com.zerog.neoessentials.chat.command.UnignoreCommand.register(dispatcher);
            registry.registerCommand("unignore", "Stop ignoring a player");
            
            com.zerog.neoessentials.chat.command.MuteCommand.register(dispatcher);
            registry.registerCommand("mute", "Mute a player from chat");
            
            com.zerog.neoessentials.chat.command.UnmuteCommand.register(dispatcher);
            registry.registerCommand("unmute", "Unmute a player");
            
            com.zerog.neoessentials.chat.command.MuteListCommand.register(dispatcher);
            registry.registerCommand("mutelist", "List all muted players");
            
            com.zerog.neoessentials.chat.command.MsgToggleCommand.register(dispatcher);
            registry.registerCommand("msgtoggle", "Toggle receiving private messages");
            
            com.zerog.neoessentials.chat.command.SocialSpyCommand.register(dispatcher);
            registry.registerCommand("socialspy", "Toggle message spying for moderators");
            
            com.zerog.neoessentials.chat.command.ReplyCommand.register(dispatcher);
            registry.registerCommand("reply", "Reply to last private message", "r");
            
            LOGGER.info("Chat commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register chat commands", e);
        }
        
        // Utility commands
        try {
            com.zerog.neoessentials.util.commands.AfkCommand.register(dispatcher);
            registry.registerCommand("afk", "Toggle AFK status");
            
            LOGGER.info("Utility commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register utility commands", e);
        }
        
        // Kit commands
        try {
            com.zerog.neoessentials.kits.command.KitCommands.register(dispatcher);
            registry.registerCommand("kit", "Use or list available kits");
            registry.registerCommand("createkit", "Create a kit from inventory");
            registry.registerCommand("delkit", "Delete a kit with confirmation");
            registry.registerCommand("listkits", "List all kits with details");
            
            LOGGER.info("Kit commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register kit commands", e);
        }
        
        // Root commands (register last so they can see all available commands)
        try {
            ModRootCommand.register(dispatcher);
            LOGGER.info("Root commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register root commands", e);
        }
        
        // Log registration statistics
        var stats = registry.getStats();
        LOGGER.info("Command registration complete: {} commands, {} aliases, {} total available", 
                   stats.get("commands"), stats.get("aliases"), stats.get("total"));
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            LOGGER.debug("Player logged in: {} ({})", player.getName().getString(), uuid);
            try {
                // Economy data is automatically managed by EconomyManager
                LOGGER.debug("Economy auto-loaded for: {}", uuid);
                
                // Load general player data (homes, warps, etc.)
                NeoEssentialsManager.getInstance().loadPlayerData(uuid);
                LOGGER.debug("Player data loaded for: {}", uuid);
            } catch (Exception e) {
                LOGGER.error("Exception loading player data for: {}: {}", uuid, e.getMessage(), e);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            LOGGER.debug("Player logged out: {} ({})", player.getName().getString(), uuid);
            try {
                // Economy data is automatically saved by EconomyManager
                LOGGER.debug("Economy auto-saved for: {}", uuid);
                
                // Save general player data (homes, warps, etc.)
                NeoEssentialsManager.getInstance().savePlayerData(uuid);
                LOGGER.debug("Player data saved for: {}", uuid);
                
                // Auto-restore items if player disconnects with pending /dispose
                DisposeCommand.restorePendingItems(player);
                
                // Clean up LastMessageManager data for /reply functionality
                com.zerog.neoessentials.chat.LastMessageManager.cleanupPlayer(player);
                LOGGER.debug("LastMessageManager cleanup completed for: {}", uuid);
                
                // Clean up AFK movement tracking data (handled by AfkMovementDetector events)
            } catch (Exception e) {
                LOGGER.error("Exception saving player data for: {}: {}", uuid, e.getMessage(), e);
            }
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NeoEssentials loading existing player data...");
        try {
            // Load all existing player data at server startup
            NeoEssentialsManager.getInstance().loadAllPlayerData();
            LOGGER.info("Existing player data loaded successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to load existing player data at startup", e);
        }
        
        // Override vanilla messaging commands after server starts
        try {
            System.out.println("DEBUG: Attempting to override vanilla messaging commands");
            var server = event.getServer();
            var dispatcher = server.getCommands().getDispatcher();
            var rootNode = dispatcher.getRoot();
            
            // Remove vanilla commands that conflict with our custom ones
            rootNode.getChildren().removeIf(node -> {
                String name = node.getName();
                if (name.equals("msg") || name.equals("tell") || name.equals("w")) {
                    System.out.println("DEBUG: Removed vanilla command: " + name);
                    return true;
                }
                return false;
            });
            
            // Re-register our custom commands to ensure they're active
            com.zerog.neoessentials.chat.command.MsgCommand.register(dispatcher);
            System.out.println("DEBUG: Re-registered custom messaging commands after vanilla removal");
            
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to override vanilla commands: " + e.getMessage());
            LOGGER.error("Failed to override vanilla messaging commands", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("NeoEssentials shutting down...");
        
        // Save all player data
        NeoEssentialsManager.getInstance().saveAllPlayerData();
        
        // Shutdown all managers with executor services to prevent resource leaks
        try {
            EconomyManager.getInstance().shutdown();
            PayToggleManager.getInstance().shutdown();
            TransactionHistoryManager.getInstance().shutdown();
        } catch (Exception e) {
            LOGGER.error("Error during manager shutdown", e);
        }
        
        LOGGER.info("NeoEssentials shutdown complete.");
    }
    
    /**
     * Initialize the PlaceholderAPI system with default NeoEssentials placeholders.
     * This makes placeholders available to the chat system and other mods.
     */
    private void initializePlaceholderAPI() {
        LOGGER.info("=== BEGINNING initializePlaceholderAPI METHOD ===");
        try {
            LOGGER.info("*** STARTING PLACEHOLDERAPI INITIALIZATION ***");
            
            // Register the default NeoEssentials placeholder expansion
            com.zerog.neoessentials.api.DefaultPlaceholderExpansion defaultExpansion = 
                new com.zerog.neoessentials.api.DefaultPlaceholderExpansion();
            
            LOGGER.info("Created DefaultPlaceholderExpansion with {} placeholders", 
                defaultExpansion.getPlaceholders().size());
            
            boolean registered = com.zerog.neoessentials.api.PlaceholderAPI.registerExpansion(defaultExpansion);
            
            if (registered) {
                LOGGER.info("*** PlaceholderAPI initialized successfully with {} default placeholders ***", 
                    defaultExpansion.getPlaceholders().size());
                LOGGER.info("Available placeholders: {}", 
                    com.zerog.neoessentials.api.PlaceholderAPI.getRegisteredPlaceholders());
            } else {
                LOGGER.error("*** FAILED to register default placeholder expansion ***");
            }
            
        } catch (Exception e) {
            LOGGER.error("*** PlaceholderAPI INITIALIZATION FAILED ***: {}", e.getMessage(), e);
        }
    }
}