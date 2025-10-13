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
import com.zerog.neoessentials.teleportation.TeleportLocation;
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
            registry.registerCommand("balance", "Display your or another player's balance", "bal", "money");
            registry.registerCommand("pay", "Send money to another player", "p");
            registry.registerCommand("paytoggle", "Toggle accepting payments", "pt");
            registry.registerCommand("eco", "Admin economy commands (give, take, set, history)", "economy");
            registry.registerCommand("baltop", "Display top player balances", "balancetop", "btop");
            LOGGER.info("Economy commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register economy commands", e);
        }
        
        // Permission commands
        try {
            PermissionsCommand.register(dispatcher);
            registry.registerCommand("permissions", "Permission management commands", "pex");
            
            // Register permission bridge commands for tab completion support
            com.zerog.neoessentials.api.permissions.PermissionBridge.registerCommands(dispatcher);
            registry.registerCommand("neoessentials-permissions", "List and export NeoEssentials permissions");
            registry.registerCommand("neoe-perms", "Quick access to NeoEssentials permissions");
            
            // Note: /pex and /permissions commands are already registered by PermissionsCommand.register()
            // which now includes dynamic tab completion from ExternalPermissionProvider
            
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
            registry.registerCommand("enchant", "Enchant items with specific enchantments", "ench");
            
            com.zerog.neoessentials.items.commands.PowertoolCommand.register(dispatcher);
            registry.registerCommand("powertool", "Bind commands to items", "ptool");

            com.zerog.neoessentials.items.commands.PowertoolToggleCommand.register(dispatcher);
            registry.registerCommand("powertooltoggle", "Toggle powertool functionality", "pttoggle");            LOGGER.info("Item commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register item commands", e);
        }
        
        // Chat commands
        try {
            // Register custom messaging commands  
            LOGGER.debug("Registering custom messaging commands");
            
            com.zerog.neoessentials.chat.command.MsgCommand.register(dispatcher);
            registry.registerCommand("msg", "Send private messages to players", "tell", "w", "message", "pm", "whisper");
            
            com.zerog.neoessentials.chat.command.IgnoreCommand.register(dispatcher);
            registry.registerCommand("ignore", "Ignore messages from a player", "block");
            
            com.zerog.neoessentials.chat.command.UnignoreCommand.register(dispatcher);
            registry.registerCommand("unignore", "Stop ignoring a player", "unblock");
            
            com.zerog.neoessentials.chat.command.MuteCommand.register(dispatcher);
            registry.registerCommand("mute", "Mute a player from chat", "silence");
            
            com.zerog.neoessentials.chat.command.UnmuteCommand.register(dispatcher);
            registry.registerCommand("unmute", "Unmute a player", "unsilence");
            
            com.zerog.neoessentials.chat.command.MuteListCommand.register(dispatcher);
            registry.registerCommand("mutelist", "List all muted players", "muted");
            
            com.zerog.neoessentials.chat.command.MsgToggleCommand.register(dispatcher);
            registry.registerCommand("msgtoggle", "Toggle receiving private messages", "togglemsg", "mt");
            
            com.zerog.neoessentials.chat.command.SocialSpyCommand.register(dispatcher);
            registry.registerCommand("socialspy", "Toggle message spying for moderators", "ss", "spy");
            
            com.zerog.neoessentials.chat.command.ReplyCommand.register(dispatcher);
            registry.registerCommand("reply", "Reply to last private message", "r");
            
            LOGGER.info("Chat commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register chat commands", e);
        }
        
        // Moderation commands
        try {
            com.zerog.neoessentials.moderation.commands.BanCommand.register(dispatcher);
            registry.registerCommand("ban", "Ban a player permanently", "pban");
            registry.registerCommand("tempban", "Temporarily ban a player", "tban");
            registry.registerCommand("banip", "Ban an IP address", "ipban");
            registry.registerCommand("unban", "Unban a player", "pardon");
            registry.registerCommand("unbanip", "Unban an IP address", "unipban", "pardonip");
            registry.registerCommand("banlist", "List all banned players and IPs", "banls");
            
            com.zerog.neoessentials.moderation.commands.KickCommand.register(dispatcher);
            registry.registerCommand("kick", "Kick a player from the server");
            registry.registerCommand("kickall", "Kick all players with a reason", "kicka");
            
            com.zerog.neoessentials.moderation.commands.JailCommand.register(dispatcher);
            registry.registerCommand("jail", "Jail a player at the jail location");
            registry.registerCommand("unjail", "Release a player from jail", "unjail");
            registry.registerCommand("setjail", "Set the jail location", "createjail");
            registry.registerCommand("jaillist", "List all jailed players", "jails");
            registry.registerCommand("jailinfo", "Display jail information", "ji");
            
            com.zerog.neoessentials.moderation.commands.VanishCommand.register(dispatcher);
            registry.registerCommand("vanish", "Toggle vanish mode for staff", "v");
            registry.registerCommand("unvanish", "Disable vanish mode", "visible");
            registry.registerCommand("vanishlist", "List all vanished players", "vlist");
            
            com.zerog.neoessentials.moderation.commands.FreezeCommand.register(dispatcher);
            registry.registerCommand("freeze", "Freeze a player in place");
            registry.registerCommand("unfreeze", "Unfreeze a player", "thaw");
            registry.registerCommand("freezeall", "Freeze all players", "freezea");
            registry.registerCommand("unfreezeall", "Unfreeze all players", "thawall");
            registry.registerCommand("freezelist", "List all frozen players", "flist");
            
            LOGGER.info("Moderation commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register moderation commands", e);
        }
        
        // Utility commands
        try {
            com.zerog.neoessentials.util.commands.AfkCommand.register(dispatcher);
            registry.registerCommand("afk", "Toggle AFK status", "away");
            
            com.zerog.neoessentials.util.commands.BookCommand.register(dispatcher);
            registry.registerCommand("book", "Create and edit books", "writebook");
            
            com.zerog.neoessentials.util.commands.MailCommand.register(dispatcher);
            registry.registerCommand("mail", "Send messages to offline players", "message", "letter");
            
            com.zerog.neoessentials.util.commands.MotdCommand.register(dispatcher);
            registry.registerCommand("motd", "Display or set message of the day", "messageoftheday");
            
            com.zerog.neoessentials.util.commands.NearCommand.register(dispatcher);
            registry.registerCommand("near", "Show nearby players", "nearby");
            
            com.zerog.neoessentials.util.commands.NickCommand.register(dispatcher);
            registry.registerCommand("nick", "Set your nickname", "nickname");
            
            com.zerog.neoessentials.util.commands.RealnameCommand.register(dispatcher);
            registry.registerCommand("realname", "Show real name of nicknamed player", "whoami");
            
            com.zerog.neoessentials.util.commands.RulesCommand.register(dispatcher);
            registry.registerCommand("rules", "Display server rules", "rule");
            
            com.zerog.neoessentials.util.commands.SeenCommand.register(dispatcher);
            registry.registerCommand("seen", "Show when player was last online", "lastseen");
            
            com.zerog.neoessentials.util.commands.SignCommand.register(dispatcher);
            registry.registerCommand("sign", "Edit sign text without breaking", "editsign");
            
            com.zerog.neoessentials.util.commands.WhoisCommand.register(dispatcher);
            registry.registerCommand("whois", "Show detailed player information", "who");
            
            LOGGER.info("Utility commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register utility commands", e);
        }
        
        // Kit commands
        try {
            com.zerog.neoessentials.kits.command.KitCommands.register(dispatcher);
            registry.registerCommand("kit", "Use or list available kits");
            registry.registerCommand("createkit", "Create a kit from inventory", "makekit", "addkit");
            registry.registerCommand("delkit", "Delete a kit with confirmation", "deletekit", "removekit", "rkit");
            registry.registerCommand("listkits", "List all kits with details", "kits");
            
            LOGGER.info("Kit commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register kit commands", e);
        }
        
        // Teleportation commands
        try {
            com.zerog.neoessentials.teleportation.TeleportationRegistry.registerCommands(dispatcher);
            
            // Register teleportation commands in the registry
            registry.registerCommand("home", "Teleport to your home", "h");
            registry.registerCommand("sethome", "Set a home location", "createhome");
            registry.registerCommand("delhome", "Delete a home location", "deletehome", "removehome", "rhome");
            registry.registerCommand("homes", "List your homes", "homelist");
            registry.registerCommand("spawn", "Teleport to spawn");
            registry.registerCommand("setspawn", "Set the server spawn location", "createspawn");
            registry.registerCommand("spawninfo", "Display spawn information", "si");
            registry.registerCommand("warp", "Teleport to a warp");
            registry.registerCommand("setwarp", "Create a warp", "createwarp", "addwarp");
            registry.registerCommand("delwarp", "Delete a warp", "deletewarp", "removewarp", "rwarp");
            registry.registerCommand("warps", "List available warps", "warplist");
            registry.registerCommand("tpa", "Request to teleport to a player");
            registry.registerCommand("tpahere", "Request a player to teleport to you", "tphere-request");
            registry.registerCommand("tpaccept", "Accept a teleport request", "tpyes", "tpy");
            registry.registerCommand("tpdeny", "Deny a teleport request", "tpno", "tpn");
            registry.registerCommand("tpcancel", "Cancel your teleport request", "tpcanc");
            registry.registerCommand("tp", "Admin teleport command");
            registry.registerCommand("tphere", "Teleport a player to you");
            registry.registerCommand("tpall", "Teleport all players");
            registry.registerCommand("tpo", "Teleport to offline or online player", "otp", "offlinetp", "tpoff", "tpoffline");
            registry.registerCommand("tpohere", "Teleport player to you (override)", "etpohere");
            registry.registerCommand("back", "Return to previous location", "return", "b");
            
            LOGGER.info("Teleportation commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register teleportation commands", e);
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
                
                // Save player's last location for offline teleportation
                TeleportLocation currentLocation = new TeleportLocation(player);
                NeoEssentialsManager.PlayerData playerData = NeoEssentialsManager.getInstance().getPlayerData(uuid);
                playerData.setLastLocation(currentLocation.toLocationString());
                LOGGER.debug("Last location saved for: {} at {}", player.getName().getString(), currentLocation.getLocationString());
                
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
        
        // Initialize permission system for tab completion
        try {
            com.zerog.neoessentials.api.permissions.PermissionBridge.initialize();
            
            // Initialize external permission provider for PermissionsEX integration
            com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.initialize();
            
            // Get permission count for information
            int totalPermissions = com.zerog.neoessentials.api.permissions.PermissionRegistry.getInstance().getAllPermissions().size();
            com.zerog.neoessentials.api.permissions.PermissionScanner.getInstance().scanForPermissions();
            int discoveredPermissions = com.zerog.neoessentials.api.permissions.PermissionScanner.getInstance().getDiscoveredPermissions().size();
            
            LOGGER.info("Permission system initialized: {} registered + {} discovered = {} total permissions", 
                totalPermissions, discoveredPermissions, totalPermissions + discoveredPermissions);
            
            // Inject permission commands for PermissionsEX integration
            var server = event.getServer();
            var dispatcher = server.getCommands().getDispatcher();
            
            com.zerog.neoessentials.api.permissions.external.PermissionCommandInjector.injectPermissionCommands(dispatcher);
            com.zerog.neoessentials.api.permissions.external.PermissionCommandInjector.registerTestCommand(dispatcher);
            
            // Help message for PermissionsEX users
            LOGGER.info("=== PermissionsEX Integration READY ===");
            LOGGER.info("NeoEssentials provides a working /pex command with tab completion!");
            LOGGER.info("Try: /pex group admin add neoessentials.<TAB> - should show all {} permissions", 
                totalPermissions + discoveredPermissions);
            LOGGER.info("Try: /pex user <name> add neoessentials.<TAB> - should show all permissions");
            LOGGER.info("The fake /pex command was registered during command registration phase");
            LOGGER.info("Use: /neoessentials-permissions group-examples (for group commands)");
            LOGGER.info("Use: /neoessentials-permissions user-examples (for user commands)");
            LOGGER.info("Tab completion works for both: /pex group <name> and /pex user <name>");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize permission system", e);
        }
        
        // Override vanilla messaging commands after server starts
        try {
            LOGGER.debug("Attempting to override vanilla messaging commands");
            var server = event.getServer();
            var dispatcher = server.getCommands().getDispatcher();
            var rootNode = dispatcher.getRoot();
            
            // Remove vanilla commands that conflict with our custom ones
            rootNode.getChildren().removeIf(node -> {
                String name = node.getName();
                if (name.equals("msg") || name.equals("tell") || name.equals("w")) {
                    LOGGER.debug("Removed vanilla command: {}", name);
                    return true;
                }
                return false;
            });
            
            // Re-register our custom commands to ensure they're active
            com.zerog.neoessentials.chat.command.MsgCommand.register(dispatcher);
            LOGGER.debug("Re-registered custom messaging commands after vanilla removal");
            
        } catch (Exception e) {
            LOGGER.error("Failed to override vanilla commands: {}", e.getMessage());
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