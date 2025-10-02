package com.zerog.neoessentials;
import com.zerog.neoessentials.commands.ModRootCommand;
import com.zerog.neoessentials.items.commands.dispose;
import com.zerog.neoessentials.economy.commands.EconomyCommands;
import com.zerog.neoessentials.economy.EconomyManager;
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
import com.google.gson.JsonParser;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.api.ChatAPI;


import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
        ensureGlobalConfig();
        ensureEconomyConfig();
        ensurePermissionsConfig();
        ensureServerLangFile();
        // Initialize the core manager
        NeoEssentialsManager.getInstance();

    // Suppress unused field warning for chatManager (placeholder for future integration)
    assert chatManager != null || true;

        // --- Permissions module config ---
        boolean permissionsEnabled = true;
        String integration = "auto";
        try {
            File configFile = new File("config/neoessentials/config.json");
            if (configFile.exists()) {
                String json = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                // Read modules.permissionsEnabled
                if (obj.has("modules")) {
                    JsonObject modulesObj = obj.getAsJsonObject("modules");
                    if (modulesObj.has("permissionsEnabled")) {
                        permissionsEnabled = modulesObj.get("permissionsEnabled").getAsBoolean();
                    }
                }
                // Read permissions.integration
                if (obj.has("permissions")) {
                    JsonObject permObj = obj.getAsJsonObject("permissions");
                    if (permObj.has("integration")) integration = permObj.get("integration").getAsString();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read permissions config: {}", e.getMessage(), e);
        }

    if (permissionsEnabled) {
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
    // Removed DataComponentType registration for server-only compatibility

        // Load chat config and commands config for ChatManager
        try {
            File configFile = new File("config/neoessentials/config.json");
            if (configFile.exists()) {
                String json = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                JsonObject chatObj = obj.has("chat") ? obj.getAsJsonObject("chat") : new JsonObject();
                JsonObject commandsObj = obj.has("commands") ? obj.getAsJsonObject("commands") : new JsonObject();
                chatManager = new ChatManager(chatObj, commandsObj);
                
                // Set the ChatManager in ChatAPI for global access
                ChatAPI.setChatManager(chatManager);
                LOGGER.info("ChatManager initialized with chat-format: {}", chatManager.getChatFormat());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load chat config: {}", e.getMessage(), e);
        }

        // Register chat event handler for message formatting
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.zerog.neoessentials.chat.ChatHandler.class);

        // --- Chat event listeners ---
        // All chat event logic (join/quit, AFK, death, etc.) is handled via event handlers below.
        // Suggestions for future improvements:
        //   - Localize all user-facing messages (see en_us.json)
        //   - Add advanced formatting (hover/click events, color codes)
        //   - Integrate with external chat plugins (e.g., DiscordSRV)
        //   - Add runtime config reload support
        //   - Add more AFK and advanced chat event logic as needed
}

    private void ensureGlobalConfig() {
        File configFile = new File("config/neoessentials/config.json");
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = NeoEssentials.class.getClassLoader().getResourceAsStream("data/config.json")) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(configFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to extract global config", e);
            }
        }
    }

    private void ensureEconomyConfig() {
        File configFile = new File("config/neoessentials/economy.json");
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = NeoEssentials.class.getClassLoader().getResourceAsStream("data/economy.json")) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(configFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to extract permissions config", e);
            }
        }
    }

    private void ensurePermissionsConfig() {
        File configFile = new File("config/neoessentials/permissions.json");
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = NeoEssentials.class.getClassLoader().getResourceAsStream("data/permissions.json")) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(configFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to extract permissions config template", e);
            }
        }
    }

    private void ensureServerLangFile() {
        try {
            File serverLangDir = new File("neoessentials/lang");
            if (!serverLangDir.exists()) serverLangDir.mkdirs();
            File serverLangFile = new File(serverLangDir, "en_us.json");
            if (!serverLangFile.exists()) {
                // Try to copy from mod jar resources
                try (InputStream in = NeoEssentials.class.getResourceAsStream("/data/lang/en_us.json")) {
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

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        LOGGER.info("Registering NeoEssentials commands...");
        
        // Test commands first
        try {
            com.zerog.neoessentials.test.TestCommand.register(dispatcher);
            LOGGER.info("Test commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register test commands", e);
        }
        
        // Economy commands
        try {
            EconomyCommands.register(dispatcher);
            LOGGER.info("Economy commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register economy commands", e);
        }
        
        // Permission commands
        PermissionsCommand.register(dispatcher);
        
        // Root commands
        ModRootCommand.register(dispatcher);
        
        // Item commands
        com.zerog.neoessentials.items.commands.repair.register(dispatcher);
        com.zerog.neoessentials.items.commands.dispose.register(dispatcher);
        com.zerog.neoessentials.items.commands.clearinvintory.register(dispatcher);
        com.zerog.neoessentials.items.commands.EnchantCommand.register(dispatcher);
        com.zerog.neoessentials.items.commands.powertool.register(dispatcher);
        com.zerog.neoessentials.items.commands.powertooltoggl.register(dispatcher);
        
        // Chat commands
        com.zerog.neoessentials.chat.command.MsgCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.IgnoreCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.UnignoreCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.MuteCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.UnmuteCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.MuteListCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.MsgToggleCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.SocialSpyCommand.register(dispatcher);
        com.zerog.neoessentials.chat.command.ReplyCommand.register(dispatcher);
        
        // Utility commands
        com.zerog.neoessentials.utils.commands.AfkCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            LOGGER.debug("Player logged in: {} ({})", player.getName().getString(), uuid);
            try {
                // Load economy data
                EconomyManager.getInstance().loadPlayerEconomy(uuid);
                LOGGER.debug("Economy loaded for: {}", uuid);
                
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
                // Save economy data
                EconomyManager.getInstance().savePlayerEconomy(uuid);
                LOGGER.debug("Economy saved for: {}", uuid);
                
                // Save general player data (homes, warps, etc.)
                NeoEssentialsManager.getInstance().savePlayerData(uuid);
                LOGGER.debug("Player data saved for: {}", uuid);
                
                // Auto-restore items if player disconnects with pending /dispose
                dispose.restorePendingItems(player);
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
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("NeoEssentials shutting down...");
        
        // Save all player data
        EconomyManager.getInstance().saveAllPlayerEconomy();
        NeoEssentialsManager.getInstance().saveAllPlayerData();
        
        // Shutdown executor services and clean up resources
        EconomyManager.getInstance().shutdown();
        LOGGER.info("NeoEssentials shutdown complete.");
    }
}