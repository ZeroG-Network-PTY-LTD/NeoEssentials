package com.zerog.neoessentials.debug;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TemplateManager;
import com.zerog.neoessentials.config.TablistTomlConfig;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Debugging utility for investigating tablist template issues
 */
public class TablistDebugger {
    
    /**
     * Run a full tablist debug scan and report issues
     * 
     * @param tabManager The TabManager instance
     * @param player The player requesting the debug (for feedback)
     */
    public static void debugTablist(TabManager tabManager, ServerPlayer player) {
        CommandSourceStack source = player.createCommandSourceStack();
        
        try {
            // Report header
            sendMessage(source, "§e===== §6NeoEssentials Tablist Debugger §e=====");
            sendMessage(source, "§7Running full diagnostic scan...");
            
            // Check config loading status
            debugConfig(source);
            
            // Check template files
            debugTemplateFiles(source);
            
            // Check template content
            debugTemplateContent(tabManager.getTemplateManager(), source);
            
            // Report if player-specific headers/footers are enabled
            debugPlayerSpecificSettings(source);
            
            // Test a forced template reload to see if that helps
            forceTemplateReload(tabManager, source);
            
            // Complete
            sendMessage(source, "§e===== §6Debug Complete §e=====");
            sendMessage(source, "§7Debug log also sent to server console");
            
        } catch (Exception e) {
            sendMessage(source, "§cError during debug: " + e.getMessage());
            NeoEssentials.LOGGER.error("Tablist debug error", e);
        }
    }
    
    /**
     * Debug tablist configuration settings
     */
    private static void debugConfig(CommandSourceStack source) {
        sendMessage(source, "§e----- Config Settings -----");
        sendMessage(source, "§7Enable animations: §f" + TablistTomlConfig.ENABLE_ANIMATIONS.get());
        sendMessage(source, "§7Header animation type: §f" + TablistTomlConfig.HEADER_ANIMATION_TYPE.get());
        sendMessage(source, "§7Footer animation type: §f" + TablistTomlConfig.FOOTER_ANIMATION_TYPE.get());
        sendMessage(source, "§7Enable player-specific headers: §f" + TablistTomlConfig.ENABLE_PLAYER_SPECIFIC_HEADERS.get());
        sendMessage(source, "§7Enable player-specific footers: §f" + TablistTomlConfig.ENABLE_PLAYER_SPECIFIC_FOOTERS.get());
        sendMessage(source, "§7Update interval: §f" + TablistTomlConfig.UPDATE_INTERVAL.get() + "ms");
    }
    
    /**
     * Debug template files existence and permissions
     */
    private static void debugTemplateFiles(CommandSourceStack source) {
        sendMessage(source, "§e----- Template Files -----");
        
        Path neoEssentialsDir = Paths.get("neoessentials");
        Path configDir = Paths.get("config", "neoessentials");
        
        Path neoTemplatesJsonFile = neoEssentialsDir.resolve("templates.json");
        Path neoTemplatesYmlFile = neoEssentialsDir.resolve("templates.yml");
        Path configTemplatesFile = configDir.resolve("templates.json");
        
        // Check if directories exist
        if (!Files.exists(neoEssentialsDir)) {
            sendMessage(source, "§cWarning: neoessentials directory does not exist!");
        } else {
            sendMessage(source, "§7neoessentials directory exists: §aYes");
        }
        
        // Check template files
        if (Files.exists(neoTemplatesJsonFile)) {
            sendMessage(source, "§7templates.json exists in neoessentials directory: §aYes");
            debugFileAccess(neoTemplatesJsonFile, source);
        } else {
            sendMessage(source, "§cWarning: templates.json missing from neoessentials directory");
        }
        
        if (Files.exists(neoTemplatesYmlFile)) {
            sendMessage(source, "§7templates.yml exists in neoessentials directory: §aYes");
            debugFileAccess(neoTemplatesYmlFile, source);
        }
        
        if (Files.exists(configTemplatesFile)) {
            sendMessage(source, "§7templates.json exists in config/neoessentials directory: §aYes");
            debugFileAccess(configTemplatesFile, source);
        }
    }
    
    /**
     * Debug file access permissions and size
     */
    private static void debugFileAccess(Path file, CommandSourceStack source) {
        try {
            boolean readable = Files.isReadable(file);
            long size = Files.size(file);
            String timestamp = Files.getLastModifiedTime(file).toString();
            
            sendMessage(source, "  §7File is readable: §f" + (readable ? "Yes" : "No"));
            sendMessage(source, "  §7File size: §f" + size + " bytes");
            sendMessage(source, "  §7Last modified: §f" + timestamp);
        } catch (Exception e) {
            sendMessage(source, "  §cError checking file: " + e.getMessage());
        }
    }
    
    /**
     * Debug template content loaded in memory
     */
    private static void debugTemplateContent(TemplateManager templateManager, CommandSourceStack source) {
        sendMessage(source, "§e----- Template Content -----");
        
        // Check global headers
        List<String> globalHeaders = templateManager.getGlobalHeaders();
        sendMessage(source, "§7Global headers loaded: §f" + (globalHeaders.isEmpty() ? "None" : globalHeaders.size()));
        if (!globalHeaders.isEmpty()) {
            sendMessage(source, "  §7First header: §f" + globalHeaders.get(0));
        }
        
        // Check global footers
        List<String> globalFooters = templateManager.getGlobalFooters();
        sendMessage(source, "§7Global footers loaded: §f" + (globalFooters.isEmpty() ? "None" : globalFooters.size()));
        if (!globalFooters.isEmpty()) {
            sendMessage(source, "  §7First footer: §f" + globalFooters.get(0));
        }
        
        // Check group templates
        Map<String, List<String>> groupHeaders = templateManager.getAllGroupHeaders();
        Map<String, List<String>> groupFooters = templateManager.getAllGroupFooters();
        sendMessage(source, "§7Group headers loaded: §f" + groupHeaders.size() + " groups");
        sendMessage(source, "§7Group footers loaded: §f" + groupFooters.size() + " groups");
        
        // List groups found
        if (!groupHeaders.isEmpty()) {
            sendMessage(source, "§7Groups with headers: §f" + String.join(", ", groupHeaders.keySet()));
        }
        
        if (!groupFooters.isEmpty()) {
            sendMessage(source, "§7Groups with footers: §f" + String.join(", ", groupFooters.keySet()));
        }
    }
    
    /**
     * Debug player-specific settings
     */
    private static void debugPlayerSpecificSettings(CommandSourceStack source) {
        sendMessage(source, "§e----- Player-Specific Settings -----");
        
        // Check if settings are properly enabled/disabled
        boolean headersEnabled = TablistTomlConfig.ENABLE_PLAYER_SPECIFIC_HEADERS.get();
        boolean footersEnabled = TablistTomlConfig.ENABLE_PLAYER_SPECIFIC_FOOTERS.get();
        
        sendMessage(source, "§7Player-specific headers enabled in config: §f" + 
            (headersEnabled ? "§aYes" : "§cNo"));
        
        sendMessage(source, "§7Player-specific footers enabled in config: §f" + 
            (footersEnabled ? "§aYes" : "§cNo"));
        
        if (!headersEnabled && !footersEnabled) {
            sendMessage(source, "§6Recommendation: Enable player-specific headers/footers in config");
        }
    }
    
    /**
     * Force a template reload to see if that fixes the issue
     */
    private static void forceTemplateReload(TabManager tabManager, CommandSourceStack source) {
        sendMessage(source, "§e----- Forcing Template Reload -----");
        
        try {
            TemplateManager templateManager = tabManager.getTemplateManager();
            templateManager.loadTemplates();
            
            // Verify if templates loaded after force reload
            List<String> globalHeaders = templateManager.getGlobalHeaders();
            List<String> globalFooters = templateManager.getGlobalFooters();
            
            sendMessage(source, "§7Force reload completed");
            sendMessage(source, "§7Headers loaded after force: §f" + globalHeaders.size());
            sendMessage(source, "§7Footers loaded after force: §f" + globalFooters.size());
            
            // Quick test of a reload of the TabManager
            tabManager.loadConfig();
            sendMessage(source, "§7TabManager config reloaded");
            
        } catch (Exception e) {
            sendMessage(source, "§cError during force reload: " + e.getMessage());
        }
    }
    
    /**
     * Utility to send a message to both player and console
     */
    private static void sendMessage(CommandSourceStack source, String message) {
        source.sendSystemMessage(Component.literal(message));
        NeoEssentials.LOGGER.info(message.replaceAll("§[0-9a-fk-or]", ""));
    }
}
