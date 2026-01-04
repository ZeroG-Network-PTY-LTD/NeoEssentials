
package com.zerog.neoessentials;
import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.core.ManagerRegistry;
import com.zerog.neoessentials.permissions.PermissionSystem;
import net.neoforged.fml.common.Mod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;




@Mod("neoessentials")
public class NeoEssentials {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentials.class);
    
    // Build and version information
    private static final String MOD_VERSION = "1.0.2.3";
    private static final String MOD_NAME = "NeoEssentials";
    private static final String BUILD_NUMBER = readBuildNumber();
    private static final String MINECRAFT_VERSION = "1.21.1";
    private static final String NEOFORGE_VERSION = "21.1.179";
    
    @SuppressWarnings("unused") // modEventBus parameter required by NeoForge @Mod constructor
    public NeoEssentials(IEventBus modEventBus) {
        long startTime = System.currentTimeMillis();
        
        // Enhanced initialization logging with version and build info
        LOGGER.info("╔════════════════════════════════════════════════════════════════╗");
        LOGGER.info("║         {} v{} (Build #{})         ║", MOD_NAME, MOD_VERSION, BUILD_NUMBER);
        LOGGER.info("║    Minecraft {} | NeoForge {}        ║", MINECRAFT_VERSION, NEOFORGE_VERSION);
        LOGGER.info("╚════════════════════════════════════════════════════════════════╝");
        LOGGER.info("");
        LOGGER.info("Initializing {} systems...", MOD_NAME);
        
        // Initialize PlaceholderAPI system
        try {
            LOGGER.info("⚙ Initializing PlaceholderAPI system...");
            initializePlaceholderAPI();
            LOGGER.info("✓ PlaceholderAPI system initialized successfully");
        } catch (Exception e) {
            LOGGER.error("✗ PlaceholderAPI initialization failed: {}", e.getMessage(), e);
        }
        
        // Register all managers with the ManagerRegistry
        try {
            LOGGER.info("⚙ Registering system managers...");
            registerAllManagers();
            LOGGER.info("✓ Registered {} managers across {} categories", 
                ManagerRegistry.getInstance().getManagerCount(),
                ManagerRegistry.getInstance().getManagersByCategory().size());
        } catch (Exception e) {
            LOGGER.error("✗ Manager registration failed: {}", e.getMessage(), e);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("");
        LOGGER.info("✓ {} initialized successfully in {}ms", MOD_NAME, duration);
        LOGGER.info("════════════════════════════════════════════════════════════════");
        LOGGER.info("");
    }
    
    /**
     * Read the build number from build_number.txt resource file.
     * 
     * @return The build number string, or "UNKNOWN" if not found
     */
    private static String readBuildNumber() {
        try (InputStream is = NeoEssentials.class.getResourceAsStream("/build_number.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String buildNumber = reader.lines().collect(Collectors.joining()).trim();
                    return buildNumber.isEmpty() ? "UNKNOWN" : buildNumber;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not read build number: {}", e.getMessage());
        }
        return "UNKNOWN";
    }
    
    /**
     * Register all system managers with the ManagerRegistry for tracking and diagnostics.
     * This allows for centralized monitoring of all manager lifecycle and initialization status.
     * <p>
     * Note: Only managers with getInstance() singleton pattern are registered for initialization tracking.
     * Other managers are instantiated as needed and don't require centralized tracking.
     */
    private void registerAllManagers() {
        ManagerRegistry registry = ManagerRegistry.getInstance();
        
        // Economy Managers
        registry.registerManager("EconomyManager", "economy", 
            com.zerog.neoessentials.economy.managers.EconomyManager.class,
            com.zerog.neoessentials.economy.managers.EconomyManager::getInstance);
        
        // Chat Managers (only singleton managers)
        registry.registerManager("AfkManager", "chat",
            com.zerog.neoessentials.chat.AfkManager.class,
            com.zerog.neoessentials.chat.AfkManager::getInstance);
        
        // Note: MuteManager, SocialSpyManager, LastMessageManager, MsgToggleManager, ChatManager
        // are utility classes without singleton pattern - not registered here
        
        // Moderation Managers
        registry.registerManager("VanishManager", "moderation",
            com.zerog.neoessentials.moderation.VanishManager.class,
            com.zerog.neoessentials.moderation.VanishManager::getInstance);
        registry.registerManager("FreezeManager", "moderation",
            com.zerog.neoessentials.moderation.FreezeManager.class,
            com.zerog.neoessentials.moderation.FreezeManager::getInstance);
        registry.registerManager("JailManager", "moderation",
            com.zerog.neoessentials.moderation.JailManager.class,
            com.zerog.neoessentials.moderation.JailManager::getInstance);
        
        // Teleportation Managers
        registry.registerManager("HomeManager", "teleportation",
            com.zerog.neoessentials.teleportation.HomeManager.class,
            com.zerog.neoessentials.teleportation.HomeManager::getInstance);
        registry.registerManager("WarpManager", "teleportation",
            com.zerog.neoessentials.teleportation.Warp.WarpManager.class,
            com.zerog.neoessentials.teleportation.Warp.WarpManager::getInstance);
        registry.registerManager("SpawnManager", "teleportation",
            com.zerog.neoessentials.teleportation.Spawn.SpawnManager.class,
            com.zerog.neoessentials.teleportation.Spawn.SpawnManager::getInstance);
        
        // Kit Managers
        registry.registerManager("KitManager", "kits",
            com.zerog.neoessentials.kits.KitManager.class,
            com.zerog.neoessentials.kits.KitManager::getInstance);
        
        // Dashboard Managers
        registry.registerManager("AuthenticationManager", "dashboard",
            com.zerog.neoessentials.webdashboard.security.AuthenticationManager.class,
            com.zerog.neoessentials.webdashboard.security.AuthenticationManager::getInstance);
        
        // API Managers
        registry.registerManager("PlaceholderManager", "api",
            com.zerog.neoessentials.api.PlaceholderManager.class,
            com.zerog.neoessentials.api.PlaceholderManager::getInstance);
        
        // Configuration Manager
        registry.registerManager("ConfigManager", "core",
            com.zerog.neoessentials.config.ConfigManager.class,
            com.zerog.neoessentials.config.ConfigManager::getInstance);
        
        // Permission System (special case - initialized in ServerStarting event)
        registry.registerManager("PermissionSystem", "core",
            com.zerog.neoessentials.permissions.PermissionSystem.class);
        
        LOGGER.debug("Manager registration complete - {} managers registered", registry.getManagerCount());
    }
    
    @EventBusSubscriber(modid = "neoessentials", bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            LOGGER.info("════════════════════════════════════════════════════════════════");
            LOGGER.info("Server starting - initializing NeoEssentials systems...");
            LOGGER.info("════════════════════════════════════════════════════════════════");
            
            // Initialize permission system FIRST
            try {
                LOGGER.info("⚙ Initializing Permission System...");
                PermissionSystem.initialize();
                ManagerRegistry.getInstance().markInitialized("PermissionSystem");
                LOGGER.info("✓ Permission System initialized successfully");
            } catch (Exception e) {
                LOGGER.error("✗ CRITICAL: Permission system failed to initialize!", e);
                ManagerRegistry.getInstance().markFailed("PermissionSystem", e.getMessage());
            }
            
            // Display manager registry diagnostics
            try {
                String diagnosticReport = ManagerRegistry.getInstance().generateDiagnosticReport();
                LOGGER.info(diagnosticReport);
                
                // Warn about any failed managers
                int failedCount = ManagerRegistry.getInstance().getFailedCount();
                if (failedCount > 0) {
                    LOGGER.warn("⚠ {} manager(s) failed to initialize - some features may be unavailable", failedCount);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to generate manager diagnostics: {}", e.getMessage());
            }
            
            LOGGER.info("════════════════════════════════════════════════════════════════");
        }
        
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            LOGGER.info("Server started - initializing chat system...");
            
            // Initialize ChatManager
            try {
                com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
                com.google.gson.JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
                com.google.gson.JsonObject chatObj = config.has("chat") ? config.getAsJsonObject("chat") : new com.google.gson.JsonObject();
                com.google.gson.JsonObject commandsObj = config.has("commands") ? config.getAsJsonObject("commands") : new com.google.gson.JsonObject();
                
                // Create new ChatManager instance
                com.zerog.neoessentials.chat.ChatManager chatManager = new com.zerog.neoessentials.chat.ChatManager(chatObj, commandsObj);
                com.zerog.neoessentials.api.ChatAPI.setChatManager(chatManager);
                
                LOGGER.info("ChatManager initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize ChatManager on server start", e);
            }
            
            LOGGER.info("Server started - applying player nicknames...");
            
            // Apply nicknames to all online players
            try {
                com.zerog.neoessentials.util.commands.NickCommand.applyNicknamesToOnlinePlayers(event.getServer());
                LOGGER.info("Player nicknames applied successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to apply player nicknames on server start", e);
            }
        }
        
        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            LOGGER.info("════════════════════════════════════════════════════════════════");
            LOGGER.info("Server stopping - shutting down NeoEssentials systems...");
            LOGGER.info("════════════════════════════════════════════════════════════════");

            // Shutdown Permission System
            try {
                LOGGER.info("Shutting down Permission System...");
                PermissionSystem.shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to save permissions on shutdown", e);
            }

            // Shutdown Economy Managers (these have executors that need proper shutdown)
            try {
                LOGGER.info("Shutting down Economy Manager...");
                com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown Economy Manager", e);
            }

            try {
                LOGGER.info("Shutting down Transaction History Manager...");
                com.zerog.neoessentials.economy.managers.TransactionHistoryManager.getInstance().shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown Transaction History Manager", e);
            }

            try {
                LOGGER.info("Shutting down Pay Toggle Manager...");
                com.zerog.neoessentials.economy.managers.PayToggleManager.getInstance().shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown Pay Toggle Manager", e);
            }

            // Shutdown Chat/AFK Managers
            try {
                LOGGER.info("Shutting down AFK Manager...");
                com.zerog.neoessentials.chat.AfkManager.getInstance().shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown AFK Manager", e);
            }

            try {
                LOGGER.info("Shutting down AFK Movement Detector...");
                com.zerog.neoessentials.chat.handlers.AfkMovementDetector.shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown AFK Movement Detector", e);
            }

            // Shutdown Moderation Managers
            try {
                LOGGER.info("Shutting down Ban Manager scheduler...");
                com.zerog.neoessentials.moderation.BanManager.getInstance().shutdownScheduler();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown Ban Manager", e);
            }

            // Shutdown Teleport Managers
            try {
                LOGGER.info("Shutting down Teleport Request Manager...");
                com.zerog.neoessentials.teleportation.TeleportRequests.TeleportRequestManager.getInstance().shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown Teleport Request Manager", e);
            }

            LOGGER.info("════════════════════════════════════════════════════════════════");
            LOGGER.info("NeoEssentials shutdown complete");
            LOGGER.info("════════════════════════════════════════════════════════════════");

            // Diagnostic: Check for any remaining threads
            try {
                LOGGER.info("Running thread diagnostics...");
                com.zerog.neoessentials.util.ThreadDiagnostics.logNeoEssentialsThreads();
                com.zerog.neoessentials.util.ThreadDiagnostics.logNonDaemonThreads();
            } catch (Exception e) {
                LOGGER.error("Failed to run thread diagnostics", e);
            }
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            LOGGER.info("Registering NeoEssentials commands...");
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
            CommandRegistry registry = CommandRegistry.getInstance();
            
            // Remove vanilla /msg, /tell, /w commands so we can override them
            removeVanillaCommand(dispatcher, "msg");
            removeVanillaCommand(dispatcher, "tell");
            removeVanillaCommand(dispatcher, "w");
            
            registerAllCommands(dispatcher, registry);
        }
        
        /**
         * Remove a vanilla command from the dispatcher to allow overriding
         */
        private static void removeVanillaCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
            try {
                var commands = dispatcher.getRoot().getChildren();
                commands.removeIf(node -> node.getName().equals(commandName));
                LOGGER.debug("Removed vanilla command: /{}", commandName);
            } catch (Exception e) {
                LOGGER.warn("Failed to remove vanilla command /{}: {}", commandName, e.getMessage());
            }
        }
    }
    
    /**
     * All command registration and related logic was previously outside any method, causing syntax errors.
     * It has been moved here for your review. Move/refactor as needed.
     */
    private static void registerAllCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandRegistry registry) {
        // Register the root command first (/neoe and /neoessentials)
        com.zerog.neoessentials.commands.ModRootCommand.register(dispatcher);
        
        // ========== TELEPORTATION COMMANDS ==========
        // Register warp commands
        registry.registerCommand("warp", "Teleport to a warp");
        registry.registerCommand("setwarp", "Create a warp");
        registry.registerCommand("delwarp", "Delete a warp");
        registry.registerCommand("warps", "List all warps");
        com.zerog.neoessentials.commands.teleportation.WarpCommands.register(dispatcher);

        // Register player warp commands if enabled
        if (com.zerog.neoessentials.teleportation.Warp.WarpManager.getInstance().isPlayerWarpsEnabled()) {
            registry.registerCommand("pwarp", "Teleport to your player warp");
            registry.registerCommand("setpwarp", "Create a player warp");
            registry.registerCommand("delpwarp", "Delete a player warp");
            registry.registerCommand("pwarps", "List your player warps");
            com.zerog.neoessentials.commands.teleportation.PwarpCommands.register(dispatcher);
        }
        
        // Register home commands
        registry.registerCommand("home", "Teleport to your home");
        registry.registerCommand("sethome", "Set your home location");
        registry.registerCommand("delhome", "Delete your home");
        registry.registerCommand("deletehome", "Delete your home (alias)");
        registry.registerCommand("homes", "List your homes");
        com.zerog.neoessentials.commands.teleportation.HomeCommands.register(dispatcher);
        
        // Register spawn commands
        registry.registerCommand("spawn", "Teleport to spawn");
        registry.registerCommand("setspawn", "Set spawn location");
        com.zerog.neoessentials.commands.teleportation.SpawnCommands.register(dispatcher);
        
        // Register teleportation request commands
        registry.registerCommand("tpa", "Request to teleport to a player");
        registry.registerCommand("tpahere", "Request a player to teleport to you");
        registry.registerCommand("tpaccept", "Accept a teleport request");
        registry.registerCommand("tpdeny", "Deny a teleport request");
        registry.registerCommand("tpacancel", "Cancel your teleport request");
        com.zerog.neoessentials.teleportation.TeleportRequests.TeleportRequestCommands.register(dispatcher);
        
        // Register admin teleportation commands
        registry.registerCommand("tp", "Teleport to a player or location");
        registry.registerCommand("tphere", "Teleport a player to you");
        registry.registerCommand("tpall", "Teleport all players to you");
        registry.registerCommand("tppos", "Teleport to coordinates");
        registry.registerCommand("tpr", "Random teleportation");
        com.zerog.neoessentials.teleportation.DirectTeleport.DirectTeleportCommands.register(dispatcher);
        
        // Register misc teleportation commands
        registry.registerCommand("back", "Return to previous location");
        registry.registerCommand("top", "Teleport to highest block");
        registry.registerCommand("jump", "Jump through walls");
        registry.registerCommand("jumpto", "Teleport to block you're looking at");
        com.zerog.neoessentials.teleportation.Misc.MiscTeleportCommands.register(dispatcher);

        // ========== ECONOMY COMMANDS ==========
        registry.registerCommand("pay", "Send money to another player");
        registry.registerCommand("balance", "Check your balance");
        registry.registerCommand("bal", "Check your balance (alias)");
        registry.registerCommand("baltop", "View top balances");
        registry.registerCommand("balancetop", "View top balances (alias)");
        registry.registerCommand("eco", "Admin economy commands");
        registry.registerCommand("paytoggle", "Toggle receiving payments");
        registry.registerCommand("pt", "Toggle receiving payments (alias)");
        com.zerog.neoessentials.economy.commands.EconomyCommands.register(dispatcher);

        // ========== MODERATION COMMANDS ==========
        registry.registerCommand("ban", "Ban a player");
        registry.registerCommand("unban", "Unban a player");
        registry.registerCommand("banip", "Ban an IP address");
        registry.registerCommand("unbanip", "Unban an IP address");
        registry.registerCommand("banlist", "List banned players");
        registry.registerCommand("kick", "Kick a player");
        registry.registerCommand("kickall", "Kick all players");
        registry.registerCommand("mute", "Mute a player");
        registry.registerCommand("unmute", "Unmute a player");
        registry.registerCommand("mutelist", "List muted players");
        registry.registerCommand("jail", "Jail a player");
        registry.registerCommand("unjail", "Release a player from jail");
        registry.registerCommand("setjail", "Set jail location");
        registry.registerCommand("jaillist", "List jailed players");
        registry.registerCommand("freeze", "Freeze a player");
        registry.registerCommand("unfreeze", "Unfreeze a player");
        registry.registerCommand("freezeall", "Freeze all players");
        registry.registerCommand("unfreezeall", "Unfreeze all players");
        registry.registerCommand("freezelist", "List frozen players");
        registry.registerCommand("vanish", "Toggle vanish mode");
        registry.registerCommand("v", "Toggle vanish mode (alias)");
        registry.registerCommand("unvanish", "Disable vanish mode");
        registry.registerCommand("vanishlist", "List vanished players");
        com.zerog.neoessentials.moderation.commands.BanCommand.register(dispatcher);
        com.zerog.neoessentials.moderation.commands.KickCommand.register(dispatcher);
        com.zerog.neoessentials.moderation.commands.JailCommand.register(dispatcher);
        com.zerog.neoessentials.moderation.commands.FreezeCommand.register(dispatcher);
        com.zerog.neoessentials.moderation.commands.VanishCommand.register(dispatcher);

        // ========== CHAT/MESSAGING COMMANDS ==========
        registry.registerCommand("msg", "Send a private message");
        registry.registerCommand("message", "Send a private message (alias)");
        registry.registerCommand("tell", "Send a private message (alias)");
        registry.registerCommand("whisper", "Send a private message (alias)");
        registry.registerCommand("w", "Send a private message (alias)");
        registry.registerCommand("reply", "Reply to last private message");
        registry.registerCommand("r", "Reply to last private message (alias)");
        registry.registerCommand("ignore", "Ignore a player");
        registry.registerCommand("unignore", "Unignore a player");
        registry.registerCommand("socialspy", "Spy on private messages");
        registry.registerCommand("msgtoggle", "Toggle receiving private messages");
        registry.registerCommand("mail", "Manage mail messages");
        com.zerog.neoessentials.chat.command.MsgCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.ReplyCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.IgnoreCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.UnignoreCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.SocialSpyCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.MuteCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.UnmuteCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.MuteListCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.MsgToggleCommand.register(dispatcher);

        // ========== PERMISSIONS COMMANDS ==========
        registry.registerCommand("permissions", "Manage permissions");
        registry.registerCommand("pex", "Manage permissions (alias)");
        com.zerog.neoessentials.permissions.command.PermissionsCommand.register(dispatcher);

        // ========== KIT COMMANDS ==========
        registry.registerCommand("kit", "Claim a kit");
        registry.registerCommand("kits", "List available kits");
        registry.registerCommand("listkits", "List available kits (alias)");
        registry.registerCommand("createkit", "Create a new kit");
        registry.registerCommand("delkit", "Delete a kit");
        com.zerog.neoessentials.kits.command.KitCommands.register(dispatcher);

        // ========== UTILITY COMMANDS ==========
        registry.registerCommand("afk", "Toggle AFK status");
        registry.registerCommand("away", "Toggle AFK status (alias)");
        registry.registerCommand("nick", "Change your nickname");
        registry.registerCommand("nickname", "Change your nickname (alias)");
        registry.registerCommand("anvil", "Open portable anvil");
        registry.registerCommand("workbench", "Open portable crafting table");
        registry.registerCommand("book", "Manage books");
        registry.registerCommand("compass", "Show your compass direction");
        registry.registerCommand("direction", "Show your compass direction (alias)");
        registry.registerCommand("crafting", "Open portable crafting table");
        registry.registerCommand("craft", "Open portable crafting table (alias)");
        registry.registerCommand("depth", "Show your depth");
        registry.registerCommand("fletching", "Open portable fletching table");
        registry.registerCommand("getpos", "Get your current position");
        registry.registerCommand("coords", "Get your current position (alias)");
        registry.registerCommand("whereami", "Get your current position (alias)");
        registry.registerCommand("grindstone", "Open portable grindstone");
        registry.registerCommand("helpop", "Request help from staff");
        registry.registerCommand("ac", "Request help from staff (alias)");
        registry.registerCommand("amsg", "Request help from staff (alias)");
        registry.registerCommand("list", "List online players");
        registry.registerCommand("who", "List online players (alias)");
        registry.registerCommand("online", "List online players (alias)");
        registry.registerCommand("mail", "Manage mail messages");
        registry.registerCommand("motd", "View message of the day");
        registry.registerCommand("near", "Find nearby players");
        registry.registerCommand("nearby", "Find nearby players (alias)");
        registry.registerCommand("ping", "Check your ping");
        registry.registerCommand("pong", "Check your ping (alias)");
        registry.registerCommand("realname", "Find player by nickname");
        registry.registerCommand("rules", "View server rules");
        registry.registerCommand("seen", "Check when player was last seen");
        registry.registerCommand("sign", "Edit sign text");
        registry.registerCommand("smithing", "Open portable smithing table");
        registry.registerCommand("stonecutting", "Open portable stonecutter");
        registry.registerCommand("stonecutter", "Open portable stonecutter (alias)");
        registry.registerCommand("suicide", "Kill yourself");
        registry.registerCommand("killme", "Kill yourself (alias)");
        registry.registerCommand("whois", "Get player information");
        registry.registerCommand("info", "Get player information (alias)");
        registry.registerCommand("gms", "Change to survival mode");
        registry.registerCommand("gmc", "Change to creative mode");
        registry.registerCommand("gmsp", "Change to spectator mode");
        registry.registerCommand("gma", "Change to adventure mode");
        
        com.zerog.neoessentials.util.commands.AfkCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.AnvilCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.BookCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.CompassCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.CraftingCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.DepthCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.FletchingCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.GetPosCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.GrindstoneCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.HelpopCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.ListCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.MailCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.MotdCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.NearCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.NickCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.PingCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.RealnameCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.RulesCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.SeenCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.SignCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.SmithingCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.StonecuttingCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.SuicideCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.WhoisCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.GamemodeCommand.register(dispatcher);
        
        // ========== WEB DASHBOARD COMMANDS ==========
        registry.registerCommand("dashboard", "Manage web dashboard");
        com.zerog.neoessentials.webdashboard.commands.DashboardCommand.register(dispatcher);
        
        // ========== ITEM COMMANDS ==========
        registry.registerCommand("repair", "Repair items");
        registry.registerCommand("fix", "Repair items (alias)");
        registry.registerCommand("dispose", "Dispose of items");
        registry.registerCommand("trash", "Dispose of items (alias)");
        registry.registerCommand("powertool", "Bind commands to items");
        registry.registerCommand("pt", "Bind commands to items (alias)");
        registry.registerCommand("enchant", "Enchant items");
        registry.registerCommand("clearinventory", "Clear inventory");
        registry.registerCommand("ci", "Clear inventory (alias)");
        registry.registerCommand("clear", "Clear inventory (alias)");
        com.zerog.neoessentials.items.commands.RepairCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.DisposeCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.PowertoolCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.EnchantCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.ClearInventoryCommand.register(dispatcher);
    }
        /*
         * All command registration and related logic that was previously outside of methods has been moved here as a block comment.
         * Please review and refactor as needed. This preserves all logic for your multi-file mod and ensures the file compiles.
         *
         * (Copy-paste all command registration code blocks here for later refactoring)
         *
         * ...
         * (See previous file version for the full logic)
         */

    /**
     * Initialize the PlaceholderAPI system with default NeoEssentials placeholders.
     * This makes placeholders available to the chat system and allows other mods to
     * register their own placeholders for cross-mod compatibility.
     * 
     * <p>The PlaceholderAPI supports:</p>
     * <ul>
     *   <li>30+ built-in NeoEssentials placeholders (player info, location, economy, etc.)</li>
     *   <li>Dynamic placeholder registration from other mods</li>
     *   <li>Placeholder expansions for organizing related placeholders</li>
     *   <li>Thread-safe placeholder resolution</li>
     * </ul>
     * 
     * <p>External mods can integrate by calling:</p>
     * <pre>{@code
     * PlaceholderAPI.registerPlaceholder("mymod_placeholder", (player, params) -> {
     *     return "value";
     * });
     * }</pre>
     * 
     * @see com.zerog.neoessentials.api.PlaceholderAPI
     * @see com.zerog.neoessentials.api.DefaultPlaceholderExpansion
     */
    private void initializePlaceholderAPI() {
        LOGGER.debug("Initializing PlaceholderAPI system...");
        try {
            // Register the default NeoEssentials placeholder expansion
            com.zerog.neoessentials.api.DefaultPlaceholderExpansion defaultExpansion = 
                new com.zerog.neoessentials.api.DefaultPlaceholderExpansion();
            
            LOGGER.debug("Created DefaultPlaceholderExpansion with {} placeholders", 
                defaultExpansion.getPlaceholders().size());
            
            boolean registered = com.zerog.neoessentials.api.PlaceholderAPI.registerExpansion(defaultExpansion);
            
            if (registered) {
                LOGGER.info("PlaceholderAPI initialized with {} default placeholders", 
                    defaultExpansion.getPlaceholders().size());
                LOGGER.debug("Available placeholders: {}", 
                    com.zerog.neoessentials.api.PlaceholderAPI.getRegisteredPlaceholders());
                
                // Mark PlaceholderManager as initialized
                ManagerRegistry.getInstance().markInitialized("PlaceholderManager");
            } else {
                LOGGER.error("Failed to register default placeholder expansion");
                ManagerRegistry.getInstance().markFailed("PlaceholderManager", 
                    "Failed to register default expansion");
            }
            
        } catch (Exception e) {
            LOGGER.error("PlaceholderAPI initialization failed: {}", e.getMessage(), e);
            ManagerRegistry.getInstance().markFailed("PlaceholderManager", e.getMessage());
        }
    }
}