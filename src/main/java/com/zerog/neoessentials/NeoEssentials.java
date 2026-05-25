package com.zerog.neoessentials;
import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigSplitter;
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
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import com.zerog.neoessentials.permissions.NeoEssentialsPermissionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import com.zerog.neoessentials.util.MessageUtil;



@Mod("neoessentials")
public class NeoEssentials {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentials.class);
    
    // Build and version information
    private static final String MOD_VERSION = "1.0.2.6";
    private static final String MOD_NAME = "NeoEssentials";
    private static final String BUILD_NUMBER = readBuildNumber();
    private static final String MINECRAFT_VERSION = "1.21.1-26.1.2";
    private static final String NEOFORGE_VERSION = "21.1.179+";

    @SuppressWarnings("unused") // modEventBus parameter required by NeoForge @Mod constructor
    public NeoEssentials(IEventBus modEventBus) {
        long startTime = System.currentTimeMillis();

        // ShopEntityRegistry uses @EventBusSubscriber — no manual registration needed.
        // (Previously registered a custom EntityType here, which caused client disconnects
        //  with "unknown registry key: neoessentials:shop_npc". Shop NPCs now use vanilla
        //  ArmorStand entities; interaction is handled via PlayerInteractEvent.EntityInteract.)

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
        
        // Ensure custom language file is present
        MessageUtil.ensureCustomLanguageFile();

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
        
        // Shop Managers
        registry.registerManager("ShopManager", "shop",
            com.zerog.neoessentials.shop.ShopManager.class,
            com.zerog.neoessentials.shop.ShopManager::getInstance);
        registry.registerManager("ShopEntityManager", "shop",
            com.zerog.neoessentials.shop.entity.ShopEntityManager.class,
            com.zerog.neoessentials.shop.entity.ShopEntityManager::getInstance);

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

        /**
         * Register NeoEssentials as an available NeoForge permission handler.
         *
         * <p>This fires on the NeoForge game event bus BEFORE the server starts,
         * during NeoForge's internal permission API initialisation.  We:
         * <ol>
         *   <li>Register the {@code neoessentials:handler} factory so admins can
         *       select it explicitly in {@code config/neoforge-server.toml}.</li>
         *   <li>When no competing permission mod (LuckPerms / FTB Ranks) is
         *       loaded AND the config still has the default NeoForge handler, we
         *       automatically switch to {@code neoessentials:handler}.  This makes
         *       permissions stored in {@code permissions.json} take effect for
         *       ALL installed mods (e.g. WorldEdit, WTHIT, etc.) out of the box.</li>
         * </ol>
         * </p>
         */
        @SubscribeEvent
        public static void onPermissionGatherHandler(PermissionGatherEvent.Handler event) {
            // 1. Register NeoEssentials' handler as an option
            event.addPermissionHandler(
                    NeoEssentialsPermissionHandler.IDENTIFIER,
                    NeoEssentialsPermissionHandler::new);
            LOGGER.debug("[Permissions] Registered NeoEssentials permission handler: {}",
                    NeoEssentialsPermissionHandler.IDENTIFIER);

            // 2. Auto-activate when no competing permission mod is present
            boolean luckPermsPresent = net.neoforged.fml.ModList.get().isLoaded("luckperms");
            boolean ftbRanksPresent  = net.neoforged.fml.ModList.get().isLoaded("ftbranks");
            if (!luckPermsPresent && !ftbRanksPresent) {
                try {
                    String current = net.neoforged.neoforge.common.NeoForgeConfig.SERVER.permissionHandler.get();
                    // Only switch if the server is still on the default NeoForge handler
                    if ("neoforge:default_handler".equals(current)) {
                        net.neoforged.neoforge.common.NeoForgeConfig.SERVER.permissionHandler.set(
                                NeoEssentialsPermissionHandler.IDENTIFIER.toString());
                        LOGGER.info("[Permissions] Auto-activated NeoEssentials permission handler " +
                                "(neoessentials:handler).  External mod permissions in permissions.json " +
                                "will now apply to ALL installed mods.  To revert, set " +
                                "'permissionHandler = \"neoforge:default_handler\"' in config/neoforge-server.toml.");
                    }
                } catch (Exception e) {
                    LOGGER.warn("[Permissions] Could not auto-configure NeoForge permission handler: {}", e.getMessage());
                }
            }
        }

        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            LOGGER.info("════════════════════════════════════════════════════════════════");
            LOGGER.info("Server starting - initializing NeoEssentials systems...");
            LOGGER.info("════════════════════════════════════════════════════════════════");
            
            // Check for config splitting opportunity
            try {
                ConfigSplitter.checkAndPromptMigration();
            } catch (Exception e) {
                LOGGER.debug("Config split check failed: {}", e.getMessage());
            }
            
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

            // Initialize Vault API (after permissions, before chat/economy features)
            try {
                LOGGER.info("⚙ Initializing Vault API...");
                com.zerog.neoessentials.vault.VaultManager.initialize();
                LOGGER.info("✓ Vault API initialized successfully");
            } catch (Exception e) {
                LOGGER.error("✗ Vault API initialization failed: {}", e.getMessage(), e);
            }

            // Initialize ChestShop system
            try {
                LOGGER.info("⚙ Initializing ChestShop system...");
                com.zerog.neoessentials.shop.ShopManager.getInstance().initialize();
                ManagerRegistry.getInstance().markInitialized("ShopManager");
                LOGGER.info("✓ ChestShop system initialized ({} shop(s) loaded)",
                    com.zerog.neoessentials.shop.ShopManager.getInstance().getShopCount());
            } catch (Exception e) {
                LOGGER.error("✗ ChestShop system failed to initialize: {}", e.getMessage(), e);
                ManagerRegistry.getInstance().markFailed("ShopManager", e.getMessage());
            }

            // Initialize NPC shop system
            try {
                LOGGER.info("⚙ Initializing NPC Shop system...");
                com.zerog.neoessentials.shop.entity.ShopEntityManager.getInstance().initialize();
                ManagerRegistry.getInstance().markInitialized("ShopEntityManager");
                LOGGER.info("✓ NPC Shop system initialized ({} NPC shop(s) loaded)",
                    com.zerog.neoessentials.shop.entity.ShopEntityManager.getInstance().getShopCount());
            } catch (Exception e) {
                LOGGER.error("✗ NPC Shop system failed to initialize: {}", e.getMessage(), e);
                ManagerRegistry.getInstance().markFailed("ShopEntityManager", e.getMessage());
            }

            // Initialize Auction House system
            try {
                LOGGER.info("⚙ Initializing Auction House system...");
                com.zerog.neoessentials.auctionhouse.AuctionHouseManager.getInstance()
                        .initialize(event.getServer());
                LOGGER.info("✓ Auction House initialized successfully");
            } catch (Exception e) {
                LOGGER.error("✗ Auction House failed to initialize: {}", e.getMessage(), e);
            }

            // Initialize dynamic pricing engine
            try {
                LOGGER.info("⚙ Initializing Shop PricingEngine...");
                com.zerog.neoessentials.shop.pricing.PricingEngine.getInstance().loadConfig();
                LOGGER.info("✓ Shop PricingEngine initialized ({} rule(s) active, enabled={})",
                    com.zerog.neoessentials.shop.pricing.PricingEngine.getInstance().getRuleCount(),
                    com.zerog.neoessentials.shop.pricing.PricingEngine.getInstance().isEnabled());
            } catch (Exception e) {
                LOGGER.error("✗ Shop PricingEngine failed to initialize: {}", e.getMessage(), e);
            }

            // Initialize Hologram system
            try {
                LOGGER.info("⚙ Initializing Hologram system...");
                com.zerog.neoessentials.hologram.HologramManager.getInstance().initialize();
                ManagerRegistry.getInstance().markInitialized("HologramManager");
                LOGGER.info("✓ Hologram system initialized ({} hologram(s) loaded)",
                    com.zerog.neoessentials.hologram.HologramManager.getInstance().getAllHolograms().size());
            } catch (Exception e) {
                LOGGER.error("✗ Hologram system failed to initialize: {}", e.getMessage(), e);
                ManagerRegistry.getInstance().markFailed("HologramManager", e.getMessage());
            }

            // Initialize custom language system
            try {
                LOGGER.info("⚙ Initializing custom language system...");
                com.zerog.neoessentials.i18n.CustomLanguageManager.getInstance().initialize();
                LOGGER.info("✓ Custom language system initialized successfully");
            } catch (Exception e) {
                LOGGER.error("✗ Custom language system failed to initialize!", e);
            }

            // Initialize custom badge images (Phase 3)
            try {
                LOGGER.info("⚙ Loading custom badge images...");
                com.zerog.neoessentials.chat.BadgeManager.getInstance().loadCustomBadgeImages();
                LOGGER.info("✓ Badge images loaded successfully");
            } catch (Exception e) {
                LOGGER.warn("⚠ Failed to load badge images: {}", e.getMessage());
                // Non-critical, continue
            }

            // Initialize resource pack system (Phase 3)
            try {
                LOGGER.info("⚙ Initializing resource pack system...");
                com.zerog.neoessentials.resourcepack.ResourcePackManager.getInstance().initialize();
                LOGGER.info("✓ Resource pack system initialized");
            } catch (Exception e) {
                LOGGER.warn("⚠ Failed to initialize resource pack system: {}", e.getMessage());
                // Non-critical, continue
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

            // Set server reference for Auction House component serialiser (registry ops)
            try {
                com.zerog.neoessentials.auctionhouse.AuctionHouseManager.getInstance()
                        .setServer(event.getServer());
            } catch (Exception e) {
                LOGGER.error("Failed to set AuctionHouse server reference", e);
            }

            // Initialize chat integration adapters (SDLink, DCIntegration, DiscordSRV, etc.)
            try {
                com.zerog.neoessentials.integrations.ChatIntegrationManager.initialize();
            } catch (Exception e) {
                LOGGER.error("Failed to initialize chat integration adapters", e);
            }

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

            // Initialize AfkManager configuration from config file
            try {
                com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
                com.google.gson.JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
                com.google.gson.JsonObject afkObj = config.has("afk") ? config.getAsJsonObject("afk") : new com.google.gson.JsonObject();
                com.zerog.neoessentials.chat.AfkManager.getInstance().loadConfiguration(afkObj);
                LOGGER.info("AfkManager configuration loaded successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize AfkManager configuration on server start", e);
            }

            LOGGER.info("Server started - applying player nicknames...");
            
            // Apply nicknames to all online players
            try {
                com.zerog.neoessentials.util.commands.NickCommand.applyNicknamesToOnlinePlayers(event.getServer());
                LOGGER.info("Player nicknames applied successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to apply player nicknames on server start", e);
            }

            // Initialize Tablist system
            try {
                com.zerog.neoessentials.tablist.TablistManager.getInstance().loadConfig();
                LOGGER.info("TablistManager initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize TablistManager: {}", e.getMessage());
            }

            // Spawn holograms in their respective levels and start the scheduler
            try {
                net.minecraft.server.MinecraftServer mcServer = event.getServer();
                for (net.minecraft.server.level.ServerLevel level : mcServer.getAllLevels()) {
                    String dimKey = com.zerog.neoessentials.hologram.HologramRenderer.dimensionKey(level);
                    com.zerog.neoessentials.hologram.HologramRenderer.spawnAllForWorld(level, dimKey);
                }
                com.zerog.neoessentials.hologram.HologramScheduler.start();
                LOGGER.info("✓ Holograms spawned and scheduler started ({} hologram(s)).",
                    com.zerog.neoessentials.hologram.HologramManager.getInstance().getAllHolograms().size());
            } catch (Exception e) {
                LOGGER.error("Failed to spawn holograms / start scheduler: {}", e.getMessage(), e);
            }
        }
        
        @SubscribeEvent
        public static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            // Check if we should notify admins about config splitting
            if (ConfigSplitter.shouldNotifyAdmins() && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                // Check if player has permission (OP or wildcard permission)
                if (player.hasPermissions(4) ||
                    com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "*") ||
                    com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.*") ||
                    com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.admin.*")) {

                    // Mark that we've notified admins (only show once per server start)
                    ConfigSplitter.markAdminsNotified();

                    // Send notification after a short delay to ensure player is fully connected.
                    // The sleep MUST happen on a background thread — sleeping inside server.execute()
                    // would block the Minecraft main tick thread for 2 seconds, causing a server freeze.
                    net.minecraft.server.MinecraftServer server = player.getServer();
                    if (server != null) {
                        Thread notifyThread = new Thread(() -> {
                            try {
                                Thread.sleep(2000); // 2 second delay (off the server thread)
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                            // Marshal message sending back onto the server thread
                            server.execute(() -> {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l════════════════════════════════════════════════════════════════"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e§l                    CONFIG SPLITTING AVAILABLE"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l════════════════════════════════════════════════════════════════"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Your config.json file is large and could be easier to manage!"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7NeoEssentials can split it into smaller, focused files."));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a✓ Easier to edit §7- Each system in its own file"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a✓ Safer §7- Automatic backup before splitting"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a✓ Organized §7- Find settings faster"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a✓ Reversible §7- Keep backup to restore anytime"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eRun: §b/neoessentials config split §eto enable"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l════════════════════════════════════════════════════════════════"));
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(""));
                            });
                        }, "NeoEssentials-AdminNotify");
                        notifyThread.setDaemon(true);
                        notifyThread.start();
                    }
                }
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

            // Shutdown Auction House system
            try {
                LOGGER.info("Shutting down Auction House system...");
                com.zerog.neoessentials.auctionhouse.AuctionHouseManager.getInstance().shutdown();
                LOGGER.info("✓ Auction House system shutdown.");
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown Auction House system", e);
            }

            // Shutdown Vault API
            try {
                com.zerog.neoessentials.vault.VaultManager.shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown Vault API", e);
            }

            // Shutdown ChestShop system
            try {
                com.zerog.neoessentials.shop.ShopManager.getInstance().shutdown();
                LOGGER.info("✓ ChestShop system shutdown.");
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown ChestShop system", e);
            }

            // Shutdown NPC Shop system
            try {
                com.zerog.neoessentials.shop.entity.ShopEntityManager.getInstance().shutdown();
                LOGGER.info("✓ NPC Shop system shutdown.");
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown NPC Shop system", e);
            }

            // Shutdown Hologram system
            try {
                com.zerog.neoessentials.hologram.HologramScheduler.stop();
                com.zerog.neoessentials.hologram.HologramManager.getInstance().shutdown();
                LOGGER.info("✓ Hologram system shutdown.");
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown Hologram system", e);
            }

            // Shutdown Chat/AFK Managers
            try {
                LOGGER.info("Shutting down chat integration adapters...");
                com.zerog.neoessentials.integrations.ChatIntegrationManager.shutdown();
            } catch (Exception e) {
                LOGGER.error("Failed to shutdown chat integration adapters", e);
            }

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
            // DISABLED: Thread diagnostics can potentially interfere with shutdown
            // Uncomment for debugging if needed
            /*
            try {
                LOGGER.info("Running thread diagnostics...");
                com.zerog.neoessentials.util.ThreadDiagnostics.logNeoEssentialsThreads();
                com.zerog.neoessentials.util.ThreadDiagnostics.logNonDaemonThreads();
            } catch (Exception e) {
                LOGGER.error("Failed to run thread diagnostics", e);
            }
            */
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
        registry.registerCommand("tpr", "Random teleportation", "randomtp", "randomteleport");
        com.zerog.neoessentials.teleportation.DirectTeleport.DirectTeleportCommands.register(dispatcher);
        
        // Register root aliases for random teleport
        registry.registerCommand("neoe tpr", "Random teleportation (alias)");
        registry.registerCommand("neoe randomtp", "Random teleportation (alias)");
        registry.registerCommand("neoe randomteleport", "Random teleportation (alias)");

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
        registry.registerCommand("tempban", "Temporarily ban a player");
        registry.registerCommand("tempbanip", "Temporarily ban an IP address");
        registry.registerCommand("kick", "Kick a player");
        registry.registerCommand("kickall", "Kick all players");
        registry.registerCommand("mute", "Mute a player");
        registry.registerCommand("unmute", "Unmute a player");
        registry.registerCommand("mutelist", "List muted players");
        registry.registerCommand("jail", "Jail a player");
        registry.registerCommand("jailfor", "Jail a player for a set duration");
        registry.registerCommand("unjail", "Release a player from jail");
        registry.registerCommand("setjail", "Set jail location");
        registry.registerCommand("deljail", "Delete a jail location");
        registry.registerCommand("jaillist", "List all jail locations");
        registry.registerCommand("jailinfo", "Show info about a jail");
        registry.registerCommand("jails", "List all jail locations (alias)");
        registry.registerCommand("togglejail", "Toggle a player's jail state");
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
        registry.registerCommand("warn", "Issue a warning to a player");
        registry.registerCommand("warnings", "View warnings for a player");
        registry.registerCommand("clearwarnings", "Clear all warnings for a player");
        registry.registerCommand("removewarn", "Remove a specific warning by ID");
        com.zerog.neoessentials.moderation.commands.WarnCommand.register(dispatcher);

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

        // Register channel commands (dynamically from config)
        com.zerog.neoessentials.chat.commands.ChannelCommands.register(dispatcher);

        // ========== LANGUAGE COMMANDS ==========
        registry.registerCommand("language", "Manage custom language files");
        com.zerog.neoessentials.commands.LanguageCommand.register(dispatcher);

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
        registry.registerCommand("kitreset", "Reset a kit cooldown");
        com.zerog.neoessentials.kits.command.KitCommands.register(dispatcher);

        // ========== UTILITY COMMANDS ==========
        registry.registerCommand("afk", "Toggle AFK status");
        registry.registerCommand("away", "Toggle AFK status (alias)");
        registry.registerCommand("help", "Show available commands");
        registry.registerCommand("?", "Show available commands (alias)");
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
        
        com.zerog.neoessentials.inventory.InventoryViewCommands.register(dispatcher);
        com.zerog.neoessentials.util.commands.AfkCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.HelpCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.AnvilCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.BookCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.CompassCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.CraftingCommand.register(dispatcher);
        com.zerog.neoessentials.util.commands.DepthCommand.register(dispatcher);
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
        com.zerog.neoessentials.commands.utility.DashboardCommand.register(dispatcher);
        registry.registerCommand("dashboardregister", "Register a dashboard account");
        com.zerog.neoessentials.commands.utility.DashboardRegisterCommand.register(dispatcher);

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
        registry.registerCommand("invsee", "View another player's inventory");
        registry.registerCommand("inv", "View another player's inventory (alias)");
        registry.registerCommand("invseeedit", "View and edit another player's inventory");
        registry.registerCommand("enderchest", "View another player's ender chest");
        registry.registerCommand("ec", "View another player's ender chest (alias)");
        registry.registerCommand("enderchestedit", "View and edit another player's ender chest");
        registry.registerCommand("ecedit", "View and edit another player's ender chest (alias)");
        registry.registerCommand("condense", "Compact items to their block forms");
        registry.registerCommand("showkit", "Preview kit contents without claiming");
        registry.registerCommand("powertoollist", "List all active powertool bindings");
        registry.registerCommand("ptlist", "List all active powertool bindings (alias)");
        registry.registerCommand("customtext", "Display a custom server text page");
        registry.registerCommand("ctext", "Display a custom server text page (alias)");
        registry.registerCommand("payconfirmtoggle", "Toggle payment confirmation prompts");
        registry.registerCommand("ciconfirmtoggle", "Toggle /ci confirmation prompts");
        registry.registerCommand("clearinventoryconfirmtoggle", "Toggle /ci confirmation prompts (alias)");
        registry.registerCommand("item", "Give yourself an item by name");
        registry.registerCommand("i", "Give yourself an item by name (alias)");
        registry.registerCommand("rtoggle", "Toggle /r reply-to-sender direction");
        com.zerog.neoessentials.items.commands.RepairCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.DisposeCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.PowertoolCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.EnchantCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.ClearInventoryCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.MiscItemCommands.register(dispatcher);

        // ========== WORTH / SELL COMMANDS ==========
        registry.registerCommand("worth", "Check the sell value of an item");
        registry.registerCommand("sell", "Sell items for money");
        registry.registerCommand("setworth", "Set the sell price of an item");
        com.zerog.neoessentials.economy.worth.WorthManager.getInstance().initialize();
        com.zerog.neoessentials.economy.worth.WorthCommand.register(dispatcher);
        com.zerog.neoessentials.economy.worth.SellCommand.register(dispatcher);

        // ========== PLAYER STATE / ADMIN TOOL COMMANDS ==========
        registry.registerCommand("fly", "Toggle flight mode");
        registry.registerCommand("god", "Toggle god mode");
        registry.registerCommand("heal", "Restore player health and hunger");
        registry.registerCommand("feed", "Restore player hunger");
        registry.registerCommand("speed", "Set walk or fly speed");
        registry.registerCommand("ext", "Extinguish a player");
        registry.registerCommand("extinguish", "Extinguish a player (alias)");
        registry.registerCommand("burn", "Set a player on fire");
        registry.registerCommand("give", "Give items to a player");
        registry.registerCommand("more", "Fill held stack to max");
        registry.registerCommand("hat", "Wear held item as helmet");
        registry.registerCommand("exp", "Manage player experience");
        registry.registerCommand("xp", "Manage player experience (alias)");
        registry.registerCommand("sudo", "Run a command as another player");
        registry.registerCommand("playtime", "Check player play time");
        com.zerog.neoessentials.util.commands.PlayerStateCommands.register(dispatcher);

        // ========== SERVER ADMIN COMMANDS ==========
        registry.registerCommand("broadcast", "Broadcast a message to all players");
        registry.registerCommand("bc", "Broadcast a message (alias)");
        registry.registerCommand("announce", "Broadcast a message (alias)");
        registry.registerCommand("time", "Get or set world time");
        registry.registerCommand("day", "Set time to day");
        registry.registerCommand("night", "Set time to night");
        registry.registerCommand("weather", "Set world weather");
        registry.registerCommand("sun", "Set weather to clear");
        registry.registerCommand("storm", "Set weather to storm");
        registry.registerCommand("thunder", "Set weather to thunder");
        registry.registerCommand("kill", "Kill a player");
        registry.registerCommand("gamemode", "Change player gamemode");
        registry.registerCommand("tpo", "Teleport override (bypass tptoggle)");
        registry.registerCommand("tpohere", "Bring player here override");
        registry.registerCommand("tpoffline", "Teleport to offline player's last position");
        com.zerog.neoessentials.util.commands.ServerAdminCommands.register(dispatcher);

        // ========== UTILITY COMMANDS ==========
        registry.registerCommand("ptime", "Set per-player time override");
        registry.registerCommand("pweather", "Set per-player weather override");
        registry.registerCommand("effect", "Apply potion effects to players");
        registry.registerCommand("spawnmob", "Spawn entities");
        registry.registerCommand("mob", "Spawn entities (alias)");
        registry.registerCommand("unlimited", "Toggle unlimited item use");
        registry.registerCommand("condense", "Condense items to storage blocks");
        com.zerog.neoessentials.util.commands.UtilityCommands.register(dispatcher);

        // ========== ITEM CUSTOMISATION & MISC COMMANDS ==========
        registry.registerCommand("me", "Broadcast an action message");
        registry.registerCommand("tptoggle", "Toggle teleport request acceptance");
        registry.registerCommand("gc", "Show server memory and TPS info");
        registry.registerCommand("mem", "Show server memory info (alias)");
        registry.registerCommand("lightning", "Strike lightning at a player");
        registry.registerCommand("smite", "Strike lightning (alias)");
        registry.registerCommand("skull", "Get a player head item");
        registry.registerCommand("itemname", "Rename held item");
        registry.registerCommand("rename", "Rename held item (alias)");
        registry.registerCommand("itemlore", "Edit held item lore");
        registry.registerCommand("remove", "Remove entities in radius");
        registry.registerCommand("loom", "Open portable loom");
        registry.registerCommand("cartography", "Open portable cartography table");
        registry.registerCommand("cartographytable", "Open portable cartography table (alias)");
        com.zerog.neoessentials.util.commands.ItemCustomisationCommands.register(dispatcher);

        // ========== WORLD INTERACTION & FUN COMMANDS ==========
        registry.registerCommand("fireball", "Shoot a projectile");
        registry.registerCommand("tree", "Grow a tree at look target");
        registry.registerCommand("bigtree", "Grow a large tree (alias)");
        registry.registerCommand("break", "Break the looked-at block");
        registry.registerCommand("ice", "Freeze a player");
        registry.registerCommand("bottom", "Teleport to the bottom of the world");
        registry.registerCommand("tpaall", "Send tpa-here to all online players");
        registry.registerCommand("broadcastworld", "Broadcast to players in your world");
        registry.registerCommand("bcastworld", "Broadcast to world (alias)");
        com.zerog.neoessentials.util.commands.WorldInteractionCommands.register(dispatcher);

        // ========== PLAYER INFO (msgtoggle only — all other commands registered above) ==========
        // /msgtoggle is solely owned by PlayerInfoCommands; all other player-info commands
        // (near, ping, seen, whois, realname, suicide, motd, rules, etc.) are registered
        // by their own dedicated command classes above and must NOT be re-registered here.
        com.zerog.neoessentials.util.commands.PlayerInfoCommands.register(dispatcher);

        // /warpinfo → WarpCommands, /world+/spawner+/recipe → ServerAdminCommands
        // (renamehome → HomeCommands, tpauto → MiscTeleportCommands — both registered in their own register() calls above)
        com.zerog.neoessentials.commands.teleportation.WarpCommands.registerWarpInfoCommand(dispatcher);
        com.zerog.neoessentials.util.commands.ServerAdminCommands.registerWorldCommands(dispatcher);

        registry.registerCommand("renamehome", "Rename a home");
        registry.registerCommand("warpinfo", "Show info about a warp");
        registry.registerCommand("world", "Teleport to a world/dimension");
        registry.registerCommand("spawner", "Change a spawner type");
        registry.registerCommand("recipe", "Show/unlock recipe for an item");
        registry.registerCommand("tpauto", "Auto-accept all teleport requests");

        // ========== FUN / MISCELLANEOUS COMMANDS ==========
        registry.registerCommand("firework", "Edit or fire held firework rockets");
        registry.registerCommand("fw", "Edit or fire held firework rockets (alias)");
        registry.registerCommand("nuke", "Rain TNT on a player");
        registry.registerCommand("antioch", "Spawn lit TNT at your look target ( easter egg)");
        registry.registerCommand("kittycannon", "Launch an exploding baby cat ");
        registry.registerCommand("beezooka", "Launch angry bees ");
        registry.registerCommand("itemdb", "Look up item registry info");
        registry.registerCommand("potion", "Edit potion effects on held potion item");
        registry.registerCommand("info", "Show server info/MOTD");
        registry.registerCommand("rest", "Reset your sleep timer (prevent phantoms)");
        registry.registerCommand("backup", "Trigger a server world save and backup");
        com.zerog.neoessentials.util.commands.FunCommands.register(dispatcher);

        // ========== VAULT API COMMANDS ==========
        registry.registerCommand("vault", "NeoEssentials Vault API info and management");
        com.zerog.neoessentials.vault.command.VaultCommand.register(dispatcher);

        // ========== PLACEHOLDER COMMANDS ==========
        registry.registerCommand("placeholder", "Inspect and test the NeoEssentials placeholder system");
        com.zerog.neoessentials.commands.utility.PlaceholderCommand.register(dispatcher);

        // ========== CHEST SHOP COMMANDS ==========
        registry.registerCommand("chestshop", "Sign-based chest shop system");
        registry.registerCommand("cshop", "Sign-based chest shop (alias)");
        com.zerog.neoessentials.shop.commands.ShopCommand.register(dispatcher);

        // ========== NPC SHOP COMMANDS ==========
        registry.registerCommand("npcshop", "NPC entity shop management");
        com.zerog.neoessentials.shop.commands.NpcShopCommand.register(dispatcher);

        // ========== HOLOGRAM COMMANDS ==========
        registry.registerCommand("hologram", "Manage holographic displays", "holo");
        com.zerog.neoessentials.hologram.command.HologramCommand.register(dispatcher);

        // ========== AUCTION HOUSE COMMANDS ==========
        registry.registerCommand("ah", "Auction House — buy and sell items", "auctionhouse");
        com.zerog.neoessentials.auctionhouse.command.AuctionHouseCommand.register(dispatcher);
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

