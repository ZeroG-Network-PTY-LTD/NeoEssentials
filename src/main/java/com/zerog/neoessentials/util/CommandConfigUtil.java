package com.zerog.neoessentials.util;

import com.zerog.neoessentials.config.CommandsConfig;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.MainConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for checking command and feature configuration status
 * Provides centralized validation for both module-level and command-level settings
 */
public class CommandConfigUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandConfigUtil.class);
    
    /**
     * Check if a command is enabled in the configuration
     * @param commandName The name of the command to check
     * @return true if the command is enabled, false otherwise
     */
    public static boolean isCommandEnabled(String commandName) {
        ConfigManager configManager = ConfigManager.getInstance();
        CommandsConfig commandsConfig = configManager.getCommandsConfig();
        
        CommandsConfig.CommandConfig commandConfig = commandsConfig.commands.get(commandName);
        if (commandConfig == null) {
            LOGGER.warn("Command '{}' not found in commands config, defaulting to enabled", commandName);
            return true; // Default to enabled for unknown commands
        }
        
        return commandConfig.enabled;
    }
    
    /**
     * Check if a module is enabled in the main configuration
     * @param moduleName The module to check (homes, economy, warps, etc.)
     * @return true if the module is enabled, false otherwise
     */
    public static boolean isModuleEnabled(String moduleName) {
        ConfigManager configManager = ConfigManager.getInstance();
        MainConfig.Modules modules = configManager.getMainConfig().modules;
        
        return switch (moduleName.toLowerCase()) {
            case "homes", "home" -> modules.homes;
            case "economy", "eco" -> modules.economy;
            case "warps", "warp" -> modules.warps;
            case "kits", "kit" -> modules.kits;
            case "chat" -> modules.chat;
            case "spawn" -> modules.spawn;
            case "moderation", "mod" -> modules.moderation;
            default -> {
                LOGGER.warn("Unknown module '{}', defaulting to enabled", moduleName);
                yield true;
            }
        };
    }
    
    /**
     * Check if both the command and its associated module are enabled
     * @param commandName The command name
     * @param moduleName The module name (can be null if no module association)
     * @return true if both are enabled, false otherwise
     */
    public static boolean isFeatureEnabled(String commandName, String moduleName) {
        // Check command-level configuration
        if (!isCommandEnabled(commandName)) {
            return false;
        }
        
        // Check module-level configuration if a module is specified
        if (moduleName != null && !moduleName.isEmpty()) {
            return isModuleEnabled(moduleName);
        }
        
        return true;
    }
    
    /**
     * Send a disabled message to a player and return false
     * @param source The command source
     * @param featureName The name of the disabled feature
     * @return always returns false for convenient use in command methods
     */
    public static boolean sendDisabledMessage(CommandSourceStack source, String featureName) {
        try {
            if (source.getEntity() instanceof ServerPlayer player) {
                MessageUtil.sendMessage(player, "&cThe " + featureName + " feature is currently disabled.");
            } else {
                source.sendFailure(MessageUtil.component("&cThe " + featureName + " feature is currently disabled."));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send disabled message for feature: {}", featureName, e);
        }
        return false;
    }
    
    /**
     * Validate command execution with automatic disabled message
     * @param source The command source
     * @param commandName The command name
     * @param moduleName The module name (can be null)
     * @param featureName Display name for the feature
     * @return true if the command should execute, false if disabled
     */
    public static boolean validateCommandExecution(CommandSourceStack source, String commandName, String moduleName, String featureName) {
        if (!isFeatureEnabled(commandName, moduleName)) {
            return sendDisabledMessage(source, featureName);
        }
        return true;
    }
    
    /**
     * Check if economy system is enabled (convenience method)
     */
    public static boolean isEconomyEnabled() {
        return isModuleEnabled("economy");
    }
    
    /**
     * Check if home system is enabled (convenience method)
     */
    public static boolean isHomesEnabled() {
        return isModuleEnabled("homes");
    }
    
    /**
     * Check if warp system is enabled (convenience method)
     */
    public static boolean isWarpsEnabled() {
        return isModuleEnabled("warps");
    }
    
    /**
     * Check if kit system is enabled (convenience method)
     */
    public static boolean isKitsEnabled() {
        return isModuleEnabled("kits");
    }
    
    /**
     * Check if spawn system is enabled (convenience method)
     */
    public static boolean isSpawnEnabled() {
        return isModuleEnabled("spawn");
    }
    
    /**
     * Check if moderation system is enabled (convenience method)
     */
    public static boolean isModerationEnabled() {
        return isModuleEnabled("moderation");
    }
    
    /**
     * Check if chat system is enabled (convenience method)
     */
    public static boolean isChatEnabled() {
        return isModuleEnabled("chat");
    }
}
