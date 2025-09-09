package com.zerog.neoessentials.debug;

import com.zerog.neoessentials.config.CommandsConfig;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.MainConfig;
import com.zerog.neoessentials.util.CommandConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug utility to check configuration status
 * Helps identify configuration loading and enforcement issues
 */
public class ConfigDebugUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigDebugUtil.class);
    
    /**
     * Print comprehensive configuration status to logs
     */
    public static void debugConfigStatus() {
        LOGGER.info("=== NeoEssentials Configuration Debug ===");
        
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            
            // Check if config manager is initialized
            if (configManager == null) {
                LOGGER.error("ConfigManager instance is null!");
                return;
            }
            
            // Check main config
            MainConfig mainConfig = configManager.getMainConfig();
            if (mainConfig != null && mainConfig.modules != null) {
                LOGGER.info("Module Status:");
                LOGGER.info("  - Homes: {}", mainConfig.modules.homes);
                LOGGER.info("  - Economy: {}", mainConfig.modules.economy);
                LOGGER.info("  - Warps: {}", mainConfig.modules.warps);
                LOGGER.info("  - Kits: {}", mainConfig.modules.kits);
                LOGGER.info("  - Chat: {}", mainConfig.modules.chat);
                LOGGER.info("  - Spawn: {}", mainConfig.modules.spawn);
                LOGGER.info("  - Moderation: {}", mainConfig.modules.moderation);
            } else {
                LOGGER.error("MainConfig or modules is null!");
            }
            
            // Check commands config
            CommandsConfig commandsConfig = configManager.getCommandsConfig();
            if (commandsConfig != null && commandsConfig.commands != null) {
                LOGGER.info("Commands Config Status:");
                LOGGER.info("  - Total commands configured: {}", commandsConfig.commands.size());
                
                // Check key commands
                String[] keyCommands = {"heal", "feed", "fly", "home", "spawn", "balance", "pay"};
                for (String cmd : keyCommands) {
                    CommandsConfig.CommandConfig cmdConfig = commandsConfig.commands.get(cmd);
                    if (cmdConfig != null) {
                        LOGGER.info("  - Command '{}': enabled={}, cost={}, cooldown={}", 
                            cmd, cmdConfig.enabled, cmdConfig.cost, cmdConfig.cooldown);
                    } else {
                        LOGGER.warn("  - Command '{}': NOT FOUND in config", cmd);
                    }
                }
            } else {
                LOGGER.error("CommandsConfig or commands map is null!");
            }
            
            // Test CommandConfigUtil functions
            LOGGER.info("CommandConfigUtil Test Results:");
            String[] testCommands = {"heal", "feed", "fly", "home", "balance"};
            for (String cmd : testCommands) {
                boolean enabled = CommandConfigUtil.isCommandEnabled(cmd);
                LOGGER.info("  - CommandConfigUtil.isCommandEnabled('{}') = {}", cmd, enabled);
            }
            
            String[] testModules = {"homes", "economy", "warps", "kits"};
            for (String module : testModules) {
                boolean enabled = CommandConfigUtil.isModuleEnabled(module);
                LOGGER.info("  - CommandConfigUtil.isModuleEnabled('{}') = {}", module, enabled);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during configuration debug:", e);
        }
        
        LOGGER.info("=== End Configuration Debug ===");
    }
    
    /**
     * Debug a specific command's configuration
     */
    public static void debugCommand(String commandName) {
        LOGGER.info("=== Debug Command: {} ===", commandName);
        
        try {
            // Check if command is enabled via CommandConfigUtil
            boolean cmdEnabled = CommandConfigUtil.isCommandEnabled(commandName);
            LOGGER.info("CommandConfigUtil.isCommandEnabled('{}') = {}", commandName, cmdEnabled);
            
            // Check raw config
            ConfigManager configManager = ConfigManager.getInstance();
            CommandsConfig commandsConfig = configManager.getCommandsConfig();
            
            if (commandsConfig != null && commandsConfig.commands != null) {
                CommandsConfig.CommandConfig cmdConfig = commandsConfig.commands.get(commandName);
                if (cmdConfig != null) {
                    LOGGER.info("Raw config for '{}': enabled={}, cost={}, cooldown={}, warmup={}, permission={}", 
                        commandName, cmdConfig.enabled, cmdConfig.cost, cmdConfig.cooldown, 
                        cmdConfig.warmup, cmdConfig.permission);
                } else {
                    LOGGER.warn("Command '{}' not found in commands config map", commandName);
                    LOGGER.info("Available commands in config: {}", commandsConfig.commands.keySet());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error debugging command '{}':", commandName, e);
        }
        
        LOGGER.info("=== End Debug Command: {} ===", commandName);
    }
}
