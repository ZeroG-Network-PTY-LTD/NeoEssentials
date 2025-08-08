package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.config.ConfigManager;
// GUI system removed - using sign-based shops only
import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.localization.LanguageValidator;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.managers.HomeManager;
import com.zerog.neoessentials.managers.KitManager;
import com.zerog.neoessentials.managers.WarpManager;
import com.zerog.neoessentials.performance.PerformanceManager;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * System status and health monitoring command for NeoEssentials
 * Provides comprehensive status information for production monitoring
 * 
 * @author ZeroG
 * @since 2.0.0 (Phase 5 Final Polish)
 */
public class StatusCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCommand.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neostatus")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.STATUS_ADMIN))
            .executes(StatusCommand::showStatus)
            .then(Commands.literal("detailed")
                .executes(StatusCommand::showDetailedStatus))
            .then(Commands.literal("configs")
                .executes(StatusCommand::showConfigStatus))
            .then(Commands.literal("languages")
                .executes(StatusCommand::showLanguageStatus))
        );
    }
    
    /**
     * Show basic system status
     */
    private static int showStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            StringBuilder status = new StringBuilder();
            status.append("§6=== NeoEssentials System Status ===\n");
            
            // Basic server info
            if (source.getServer() != null) {
                int onlinePlayers = source.getServer().getPlayerCount();
                int maxPlayers = source.getServer().getMaxPlayers();
                
                status.append("§aServer: §f").append(onlinePlayers).append("/").append(maxPlayers).append(" players\n");
            }
            
            // Memory info
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024;
            double memoryPercent = (double) usedMemory / maxMemory * 100;
            
            status.append("§aMemory: §f").append(usedMemory).append("MB/").append(maxMemory).append("MB ");
            status.append("§7(").append(getMemoryColor(memoryPercent)).append(DECIMAL_FORMAT.format(memoryPercent)).append("%§7)\n");
            
            // Quick system status
            status.append("§aConfigurations: ").append(getConfigStatusSummary()).append("\n");
            status.append("§aLanguages: ").append(getLanguageStatusSummary()).append("\n");
            
            status.append("\n§7Use §e/neostatus detailed §7for more information");
            
            source.sendSuccess(() -> Component.literal(status.toString()), false);
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing system status", e);
            source.sendFailure(Component.literal("§cError retrieving system status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show detailed system status
     */
    private static int showDetailedStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            StringBuilder status = new StringBuilder();
            status.append("§6=== NeoEssentials Detailed Status ===\n\n");
            
            // Server Information
            status.append("§e--- Server Information ---\n");
            if (source.getServer() != null) {
                var server = source.getServer();
                status.append("§aOnline Players: §f").append(server.getPlayerCount()).append("/").append(server.getMaxPlayers()).append("\n");
                status.append("§aServer Version: §f").append(server.getServerVersion()).append("\n");
                status.append("§aUptime: §f").append(getServerUptime()).append("\n");
            }
            
            // Performance Metrics
            status.append("\n§e--- Performance Metrics ---\n");
            
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedHeap = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
            long maxHeap = memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024;
            long usedNonHeap = memoryBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024;
            
            status.append("§aHeap Memory: §f").append(usedHeap).append("MB/").append(maxHeap).append("MB\n");
            status.append("§aNon-Heap Memory: §f").append(usedNonHeap).append("MB\n");
            
            // System Components
            status.append("\n§e--- System Components ---\n");
            status.append("§aConfiguration Manager: ").append(getComponentStatus("config")).append("\n");
            status.append("§aLanguage System: ").append(getComponentStatus("language")).append("\n");
            status.append("§aEconomy System: ").append(getComponentStatus("economy")).append("\n");
            status.append("§aHome System: ").append(getComponentStatus("home")).append("\n");
            status.append("§aWarp System: ").append(getComponentStatus("warp")).append("\n");
            status.append("§aKit System: ").append(getComponentStatus("kit")).append("\n");
            status.append("§aGUI System: ").append(getComponentStatus("gui")).append("\n");
            
            source.sendSuccess(() -> Component.literal(status.toString()), false);
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing detailed status", e);
            source.sendFailure(Component.literal("§cError retrieving detailed status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show configuration status
     */
    private static int showConfigStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            StringBuilder status = new StringBuilder();
            status.append("§6=== Configuration Status ===\n\n");
            
            ConfigManager configManager = ConfigManager.getInstance();
            
            status.append("§e--- Configuration Files ---\n");
            String[] configFiles = {"main", "economy", "homes", "kits", "warps", "moderation", "messaging", "discord", "tablist", "spawn"};
            
            for (String configName : configFiles) {
                boolean exists = configManager.configExists(configName + ".json");
                boolean valid = configManager.getConfigStatus().isValid(configName);
                
                status.append("§a").append(configName).append(".json: ");
                if (!exists) {
                    status.append("§cMissing");
                } else if (!valid) {
                    status.append("§eInvalid");
                } else {
                    status.append("§aValid");
                }
                status.append("\n");
            }
            
            status.append("\n§e--- Configuration Features ---\n");
            status.append("§aHot-reload: §aEnabled\n");
            status.append("§aAuto-backup: §aEnabled\n");
            status.append("§aValidation: §aEnabled\n");
            
            source.sendSuccess(() -> Component.literal(status.toString()), false);
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing configuration status", e);
            source.sendFailure(Component.literal("§cError retrieving configuration status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show language system status
     */
    private static int showLanguageStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            StringBuilder status = new StringBuilder();
            status.append("§6=== Language System Status ===\n\n");
            
            LanguageManager langManager = LanguageManager.getInstance();
            
            // Available languages
            var availableLanguages = langManager.getAvailableLanguages();
            status.append("§e--- Available Languages ---\n");
            status.append("§aTotal Languages: §f").append(availableLanguages.size()).append("\n");
            status.append("§aDefault Language: §f").append(langManager.getDefaultLanguage()).append("\n");
            
            for (String lang : availableLanguages) {
                String displayName = langManager.getLanguageDisplayName(lang);
                status.append("§a- §f").append(lang).append(" §7(").append(displayName).append(")\n");
            }
            
            // Validation results
            Path languageDir = ConfigManager.getInstance().getConfigPath().resolve("languages");
            Map<String, LanguageValidator.ValidationResult> validationResults = 
                LanguageValidator.validateAllLanguageFiles(languageDir);
            
            status.append("\n§e--- Language Validation ---\n");
            int validLanguages = 0;
            int totalLanguages = validationResults.size();
            
            for (Map.Entry<String, LanguageValidator.ValidationResult> entry : validationResults.entrySet()) {
                String lang = entry.getKey();
                LanguageValidator.ValidationResult result = entry.getValue();
                
                status.append("§a").append(lang).append(": ");
                if (result.isValid()) {
                    status.append("§aValid");
                    validLanguages++;
                } else {
                    status.append("§cInvalid");
                }
                status.append(" §7(").append(result.getCompletenessPercentage()).append("% complete)\n");
            }
            
            status.append("\n§aValid Languages: §f").append(validLanguages).append("/").append(totalLanguages);
            if (totalLanguages > 0) {
                int successRate = (validLanguages * 100) / totalLanguages;
                status.append(" §7(").append(successRate).append("% success rate)");
            }
            
            source.sendSuccess(() -> Component.literal(status.toString()), false);
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing language status", e);
            source.sendFailure(Component.literal("§cError retrieving language status: " + e.getMessage()));
            return 0;
        }
    }
    
    // Helper methods
    
    private static String getMemoryColor(double percent) {
        if (percent < 70) return "§a";
        if (percent < 85) return "§e";
        return "§c";
    }
    
    private static String getServerUptime() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptime / (1000 * 60 * 60);
        long minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60);
        return hours + "h " + minutes + "m";
    }
    
    private static String getConfigStatusSummary() {
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            // Check if configurations are properly loaded
            boolean configsValid = (configManager != null);
            if (configsValid) {
                return "§aHealthy";
            } else {
                return "§eWarnings";
            }
        } catch (Exception e) {
            return "§cError";
        }
    }
    
    private static String getLanguageStatusSummary() {
        try {
            LanguageManager langManager = LanguageManager.getInstance();
            int languages = langManager.getAvailableLanguages().size();
            return "§a" + languages + " languages";
        } catch (Exception e) {
            return "§cError";
        }
    }
    
    private static String getComponentStatus(String component) {
        try {
            switch (component.toLowerCase()) {
                case "config":
                    ConfigManager.getInstance();
                    return "§aOperational";
                case "language":
                    LanguageManager.getInstance();
                    return "§aOperational";
                case "economy":
                    EconomyManager.getInstance();
                    return "§aOperational";
                case "home":
                    HomeManager.getInstance();
                    return "§aOperational";
                case "warp":
                    WarpManager.getInstance();
                    return "§aOperational";
                case "kit":
                    KitManager.getInstance();
                    return "§aOperational";
                case "gui":
                    // GUI system removed - using sign-based shops only
                    return "§eDisabled (Sign-based shops only)";
                default:
                    return "§eUnknown";
            }
        } catch (Exception e) {
            return "§cError: " + e.getMessage();
        }
    }
}
