package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.DataManagerHooks;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TablistMigrationManager;
import com.zerog.neoessentials.utils.ChatUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Command to fix and diagnose tablist issues
 */
public class TabFixCommand {
    
    /**
     * Register the command
     * 
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> tabFixCommand = 
            Commands.literal("tabfix")
                .requires(source -> source.hasPermission(2)) // Operator permission level
                .executes(context -> executeTabFix(context.getSource()))
                .then(Commands.literal("reload")
                    .executes(context -> executeTabReload(context.getSource()))
                )
                .then(Commands.literal("diagnose")
                    .executes(context -> executeTabDiagnose(context.getSource()))
                )
                .then(Commands.literal("createtemplates")
                    .executes(context -> executeForceCreate(context.getSource()))
                );
        
        dispatcher.register(tabFixCommand);
    }
    
    /**
     * Execute the tabfix command
     * 
     * @param source Command source
     * @return Command result
     */
    private static int executeTabFix(CommandSourceStack source) {
        ChatUtil.sendMessage(source, "§6Running tablist system check and fix...");
        
        // Apply migration to ensure we're using the new TabManager
        TablistMigrationManager.applyMigration();
        
        // Make sure the TabManager is initialized
        boolean success = DataManagerHooks.ensureTabManagerInitialized();
        
        if (success) {
            ChatUtil.sendSuccess(source, "§aTablist system fixed successfully!");
            return 1;
        } else {
            ChatUtil.sendError(source, "§cTablist fix failed. Check logs for details.");
            return 0;
        }
    }
    
    /**
     * Execute the tabfix reload command
     * 
     * @param source Command source
     * @return Command result
     */
    private static int executeTabReload(CommandSourceStack source) {
        ChatUtil.sendMessage(source, "§6Reloading tablist templates...");
        
        // Get the enhanced tablist manager
        var dataManager = NeoEssentials.getInstance().getDataManager();
        var tablistManager = dataManager != null ? dataManager.getTablistManager() : null;
        
        if (tablistManager == null) {
            ChatUtil.sendError(source, "§cTablist manager not available");
            return 0;
        }
        
        // Reload configuration
        boolean success = tablistManager.reloadConfig();
        
        if (success) {
            ChatUtil.sendSuccess(source, "§aTablist templates reloaded successfully!");
            return 1;
        } else {
            ChatUtil.sendError(source, "§cFailed to reload templates. Check logs for details.");
            return 0;
        }
    }
    
    /**
     * Execute the tabfix diagnose command
     * 
     * @param source Command source
     * @return Command result
     */
    private static int executeTabDiagnose(CommandSourceStack source) {
        ChatUtil.sendMessage(source, "§6Diagnosing tablist system...");
        
        // Get the TabManager instance
        TabManager tabManager = DataManagerHooks.getTabManager();
        
        if (tabManager == null) {
            ChatUtil.sendError(source, "§cTabManager not initialized. Run /tabfix to fix this issue.");
            return 0;
        }
        
        // Check initialization status
        boolean initialized = tabManager.isInitialized();
        ChatUtil.sendMessage(source, "§7TabManager initialized: §e" + initialized);
        
        // Check server reference
        boolean hasServer = tabManager.hasServerReference();
        ChatUtil.sendMessage(source, "§7TabManager server reference: §e" + hasServer);
        
        // Check template status
        boolean templatesLoaded = tabManager.hasTemplates();
        ChatUtil.sendMessage(source, "§7Templates loaded: §e" + templatesLoaded);
        
        // Detailed template file check
        ChatUtil.sendMessage(source, "§6Checking template files...");
        
        // Check neoessentials directory
        Path neoDir = java.nio.file.Paths.get("neoessentials");
        boolean neoDirExists = java.nio.file.Files.exists(neoDir);
        ChatUtil.sendMessage(source, "§7neoessentials/ directory exists: §e" + neoDirExists);
        
        if (neoDirExists) {
            // Check for templates.yml
            Path neoYamlPath = neoDir.resolve("templates.yml");
            boolean neoYamlExists = java.nio.file.Files.exists(neoYamlPath);
            ChatUtil.sendMessage(source, "§7neoessentials/templates.yml exists: §e" + neoYamlExists);
            
            // Check for templates.json
            Path neoJsonPath = neoDir.resolve("templates.json");
            boolean neoJsonExists = java.nio.file.Files.exists(neoJsonPath);
            ChatUtil.sendMessage(source, "§7neoessentials/templates.json exists: §e" + neoJsonExists);
            
            // If either file exists, try to read its size
            if (neoYamlExists) {
                try {
                    long size = java.nio.file.Files.size(neoYamlPath);
                    ChatUtil.sendMessage(source, "§7templates.yml size: §e" + size + " bytes");
                } catch (Exception e) {
                    ChatUtil.sendMessage(source, "§cError reading templates.yml: " + e.getMessage());
                }
            }
            
            if (neoJsonExists) {
                try {
                    long size = java.nio.file.Files.size(neoJsonPath);
                    ChatUtil.sendMessage(source, "§7templates.json size: §e" + size + " bytes");
                } catch (Exception e) {
                    ChatUtil.sendMessage(source, "§cError reading templates.json: " + e.getMessage());
                }
            }
        } else {
            // Try to create the directory
            try {
                java.nio.file.Files.createDirectories(neoDir);
                ChatUtil.sendMessage(source, "§aCreated neoessentials/ directory");
            } catch (Exception e) {
                ChatUtil.sendMessage(source, "§cFailed to create neoessentials/ directory: " + e.getMessage());
            }
        }
        
        // Check config/neoessentials directory
        Path configDir = java.nio.file.Paths.get("config", "neoessentials");
        boolean configDirExists = java.nio.file.Files.exists(configDir);
        ChatUtil.sendMessage(source, "§7config/neoessentials/ directory exists: §e" + configDirExists);
        
        if (configDirExists) {
            // Check for templates.yml
            Path configYamlPath = configDir.resolve("templates.yml");
            boolean configYamlExists = java.nio.file.Files.exists(configYamlPath);
            ChatUtil.sendMessage(source, "§7config/neoessentials/templates.yml exists: §e" + configYamlExists);
            
            // Check for templates.json
            Path configJsonPath = configDir.resolve("templates.json");
            boolean configJsonExists = java.nio.file.Files.exists(configJsonPath);
            ChatUtil.sendMessage(source, "§7config/neoessentials/templates.json exists: §e" + configJsonExists);
        }
        
        // Check update task
        boolean updateTaskRunning = tabManager.isUpdateTaskRunning();
        ChatUtil.sendMessage(source, "§7Update task running: §e" + updateTaskRunning);
        
        // Check player count
        int playerCount = tabManager.getPlayerCount();
        ChatUtil.sendMessage(source, "§7Players tracked: §e" + playerCount);
        
        // Send summary
        if (initialized && hasServer && templatesLoaded && updateTaskRunning) {
            ChatUtil.sendSuccess(source, "§aTablist system is functioning correctly!");
        } else {
            ChatUtil.sendError(source, "§cTablist system has issues. Run /tabfix to attempt automatic repair.");
        }
        
        return 1;
    }
    
    /**
     * Force creation of template files
     * 
     * @param source Command source
     * @return Command result
     */
    private static int executeForceCreate(CommandSourceStack source) {
        ChatUtil.sendMessage(source, "§6Forcefully creating template files...");
        
        TabManager tabManager = DataManagerHooks.getTabManager();
        
        if (tabManager == null) {
            ChatUtil.sendError(source, "§cTabManager not initialized. Run /tabfix first.");
            return 0;
        }
        
        try {
            // Create neoessentials directory if it doesn't exist
            Path neoDir = java.nio.file.Paths.get("neoessentials");
            if (!java.nio.file.Files.exists(neoDir)) {
                java.nio.file.Files.createDirectories(neoDir);
                ChatUtil.sendMessage(source, "§aCreated neoessentials/ directory");
            }
            
            // Create templates.yml in the neoessentials directory
            Path templatesPath = neoDir.resolve("templates.yml");
            
            // Create a basic YAML template
            String yamlContent = "# NeoEssentials Tablist Templates\n" +
                "# Generated: " + java.time.LocalDateTime.now().toString() + "\n\n" +
                "templates:\n" +
                "  headers:\n" +
                "    - \"&6&l✦ &b&lNeoEssentials Server &6&l✦\"\n" +
                "    - \"&eWelcome, &a{player_name}&e!\"\n" +
                "    - \"&eOnline players: &a{online_players}/{max_players}\"\n" +
                "    - \"&eServer time: &a{time}\"\n\n" +
                "  footers:\n" +
                "    - \"&eBalance: &a{balance} coins\"\n" +
                "    - \"&eWebsite: &awww.example.com\"\n" +
                "    - \"&eThanks for playing!\"\n" +
                "    - \"&eServer TPS: &a{tps} &7| &eMemory: &a{memory_percent}%\"\n\n" +
                "groups:\n" +
                "  admin:\n" +
                "    headers:\n" +
                "      - \"&4&l★ &c&lAdmin Panel &4&l★\"\n" +
                "      - \"&cServer TPS: &f{tps} &7| &cMemory: &f{memory_percent}%\"\n" +
                "    footers:\n" +
                "      - \"&cAdmin Command Help: &f/neoessentials help\"\n" +
                "      - \"&cServer uptime: &f{uptime}\"\n\n" +
                "  default:\n" +
                "    headers:\n" +
                "      - \"&6&l⚜ &e&lWelcome &6&l⚜\"\n" +
                "      - \"&eWelcome to the server, &6{player_name}&e!\"\n" +
                "    footers:\n" +
                "      - \"&6Balance: &e{balance} coins\"\n" +
                "      - \"&6Use &e/help &6for a list of commands\"";
            
            // Write the template file
            java.nio.file.Files.writeString(templatesPath, yamlContent, java.nio.charset.StandardCharsets.UTF_8);
            ChatUtil.sendSuccess(source, "§aCreated templates.yml file in neoessentials directory");
            
            // Reload configuration
            var dataManager = NeoEssentials.getInstance().getDataManager();
            var tablistManager = dataManager != null ? dataManager.getTablistManager() : null;
            boolean reloadSuccess = tablistManager != null && tablistManager.reloadConfig();
            if (reloadSuccess) {
                ChatUtil.sendSuccess(source, "§aTemplate system reloaded successfully!");
            } else {
                ChatUtil.sendError(source, "§cTemplate reload failed after creation.");
            }
            
            return 1;
            
        } catch (Exception e) {
            ChatUtil.sendError(source, "§cFailed to create templates: " + e.getMessage());
            NeoEssentials.LOGGER.error("Error creating template files", e);
            return 0;
        }
    }
}
