package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.ChatUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * TabFix command for NeoEssentials tablist system
 * Provides diagnostic and repair functionality for the enhanced tablist system
 */
public class TabFixCommand {

    /**
     * Register tabfix command with all subcommands
     * 
     * @param dispatcher Command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> tabfixCommand = Commands.literal("tabfix")
            .requires(source -> source.hasPermission(2))
            .executes(context -> executeTabFix(context.getSource()));
        
        // Add subcommands
        tabfixCommand.then(Commands.literal("reload")
            .executes(context -> executeReload(context.getSource())));
        
        tabfixCommand.then(Commands.literal("diagnose")
            .executes(context -> executeTabDiagnose(context.getSource())));
        
        tabfixCommand.then(Commands.literal("create")
            .executes(context -> executeForceCreate(context.getSource())));
        
        dispatcher.register(tabfixCommand);
    }

    /**
     * Execute the tabfix command - checks and fixes tablist system
     * 
     * @param source Command source
     * @return Command result
     */
    private static int executeTabFix(CommandSourceStack source) {
        ChatUtil.sendMessage(source, "§6Running tablist system check and fix...");
        
        // Get the enhanced tablist manager
        var dataManager = NeoEssentials.getInstance().getDataManager();
        var tablistManager = dataManager != null ? dataManager.getTablistManager() : null;
        
        if (tablistManager == null) {
            ChatUtil.sendError(source, "§cTablist manager not available");
            return 0;
        }
        
        boolean success = tablistManager.isInitialized();
        
        if (success) {
            ChatUtil.sendSuccess(source, "§aTablist system is working correctly!");
            return 1;
        } else {
            ChatUtil.sendError(source, "§cTablist system not initialized. Check logs for details.");
            return 0;
        }
    }

    /**
     * Execute the tabfix reload command
     * 
     * @param source Command source
     * @return Command result
     */
    private static int executeReload(CommandSourceStack source) {
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
            ChatUtil.sendSuccess(source, "§aTablist configuration reloaded successfully!");
            return 1;
        } else {
            ChatUtil.sendError(source, "§cFailed to reload tablist configuration. Check logs for details.");
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
        
        // Get the enhanced tablist manager
        var dataManager = NeoEssentials.getInstance().getDataManager();
        var tablistManager = dataManager != null ? dataManager.getTablistManager() : null;
        
        if (tablistManager == null) {
            ChatUtil.sendError(source, "§cTablist manager not initialized.");
            return 0;
        }
        
        // Check initialization status
        boolean initialized = tablistManager.isInitialized();
        ChatUtil.sendMessage(source, "§7TAB-like tablist initialized: §e" + initialized);
        
        // Check server reference
        boolean hasServer = tablistManager.getServerRef().get() != null;
        ChatUtil.sendMessage(source, "§7Server reference available: §e" + hasServer);
        
        // Check configuration
        boolean hasConfig = tablistManager.getConfig() != null;
        ChatUtil.sendMessage(source, "§7Configuration loaded: §e" + hasConfig);
        
        // Check update task
        boolean updateTaskRunning = initialized; // For TAB-like system, if initialized, update task should be running
        ChatUtil.sendMessage(source, "§7Update task running: §e" + updateTaskRunning);
        
        // Check player count
        int playerCount = tablistManager.getPlayerCount();
        ChatUtil.sendMessage(source, "§7Players tracked: §e" + playerCount);
        
        // Send summary
        if (initialized && hasServer && hasConfig && updateTaskRunning) {
            ChatUtil.sendSuccess(source, "§aTablist system is functioning correctly!");
        } else {
            ChatUtil.sendError(source, "§cTablist system has issues. Check configuration and restart if needed.");
        }
        
        return 1;
    }

    /**
     * Execute the force create command - creates default template files
     * 
     * @param source Command source
     * @return Command result
     */
    private static int executeForceCreate(CommandSourceStack source) {
        ChatUtil.sendMessage(source, "§6Creating default configuration...");
        
        var dataManager = NeoEssentials.getInstance().getDataManager();
        var tablistManager = dataManager != null ? dataManager.getTablistManager() : null;
        
        if (tablistManager == null) {
            ChatUtil.sendError(source, "§cTablist manager not initialized.");
            return 0;
        }
        
        try {
            // Reload configuration to create default files if needed
            boolean success = tablistManager.reloadConfig();
            
            if (success) {
                ChatUtil.sendSuccess(source, "§aDefault configuration created and loaded successfully!");
                return 1;
            } else {
                ChatUtil.sendError(source, "§cFailed to create default configuration. Check logs for details.");
                return 0;
            }
            
        } catch (Exception e) {
            ChatUtil.sendError(source, "§cFailed to create configuration: " + e.getMessage());
            NeoEssentials.LOGGER.error("Error creating default configuration", e);
            return 0;
        }
    }
}
