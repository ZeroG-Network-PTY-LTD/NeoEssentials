package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.NeoEssentials;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Main configuration manager for NeoEssentials
 * 
 * Handles loading and managing all configuration files including:
 * - Main configuration (features, general settings)
 * - Database configuration
 * - Economy configuration
 * - Home/Warp configurations
 * - Tablist configuration
 * - Discord configuration
 * - Text command files (MOTD, rules, help)
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ConfigManager {
    
    private final Path configDir;
    private final Gson gson;
    
    // Configuration objects
    private MainConfig mainConfig;
    private DatabaseConfig databaseConfig;
    private EconomyConfig economyConfig;
    private HomeConfig homeConfig;
    private WarpConfig warpConfig;
    private KitConfig kitConfig;
    private ModerationConfig moderationConfig;
    private MessagingConfig messagingConfig;
    private TablistConfig tablistConfig;
    private DiscordConfig discordConfig;
    
    // Text command files
    private final Map<String, String> textCommands;
    
    public ConfigManager(Path configDir) {
        this.configDir = configDir;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        this.textCommands = new HashMap<>();
        
        // Ensure config directory exists
        configDir.toFile().mkdirs();
        
        // Load all configurations
        loadConfigurations();
    }
    
    /**
     * Load all configuration files
     */
    private void loadConfigurations() {
        NeoEssentials.LOGGER.info("Loading NeoEssentials configurations...");
        
        try {
            // Load main configuration
            this.mainConfig = loadConfig("main.json", MainConfig.class, MainConfig::createDefault);
            
            // Load database configuration
            this.databaseConfig = loadConfig("database.json", DatabaseConfig.class, DatabaseConfig::createDefault);
            
            // Load economy configuration
            this.economyConfig = loadConfig("economy.json", EconomyConfig.class, EconomyConfig::createDefault);
            
            // Load home configuration
            this.homeConfig = loadConfig("homes.json", HomeConfig.class, HomeConfig::createDefault);
            
            // Load warp configuration
            this.warpConfig = loadConfig("warps.json", WarpConfig.class, WarpConfig::createDefault);
            
            // Load kit configuration
            this.kitConfig = loadConfig("kits.json", KitConfig.class, KitConfig::createDefault);
            
            // Load moderation configuration
            this.moderationConfig = loadConfig("moderation.json", ModerationConfig.class, ModerationConfig::createDefault);
            
            // Load messaging configuration
            this.messagingConfig = loadConfig("messaging.json", MessagingConfig.class, MessagingConfig::createDefault);
            
            // Load tablist configuration
            this.tablistConfig = loadConfig("tablist.json", TablistConfig.class, TablistConfig::createDefault);
            
            // Load Discord configuration
            this.discordConfig = loadConfig("discord.json", DiscordConfig.class, DiscordConfig::createDefault);
            
            // Load text command files
            loadTextCommands();
            
            NeoEssentials.LOGGER.info("All configurations loaded successfully!");
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load configurations", e);
            throw new RuntimeException("Failed to load NeoEssentials configurations", e);
        }
    }
    
    /**
     * Load a configuration file with fallback to default
     */
    private <T> T loadConfig(String filename, Class<T> configClass, ConfigSupplier<T> defaultSupplier) {
        File configFile = configDir.resolve(filename).toFile();
        
        if (!configFile.exists()) {
            // Create default configuration
            T defaultConfig = defaultSupplier.get();
            saveConfig(filename, defaultConfig);
            return defaultConfig;
        }
        
        try {
            String json = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
            return gson.fromJson(json, configClass);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to load config file: {}", filename, e);
            throw new RuntimeException("Failed to load config: " + filename, e);
        }
    }
    
    /**
     * Save a configuration file
     */
    private <T> void saveConfig(String filename, T config) {
        File configFile = configDir.resolve(filename).toFile();
        
        try {
            String json = gson.toJson(config);
            FileUtils.writeStringToFile(configFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save config file: {}", filename, e);
        }
    }
    
    /**
     * Load text command files (MOTD, rules, help, etc.)
     */
    private void loadTextCommands() {
        String[] textFiles = {"motd.txt", "rules.txt", "help.txt", "info.txt"};
        
        for (String filename : textFiles) {
            File textFile = configDir.resolve("text").resolve(filename).toFile();
            
            if (!textFile.exists()) {
                // Create default text files
                createDefaultTextFile(filename);
            }
            
            try {
                String content = FileUtils.readFileToString(textFile, StandardCharsets.UTF_8);
                String commandName = filename.replace(".txt", "");
                textCommands.put(commandName, content);
            } catch (IOException e) {
                NeoEssentials.LOGGER.error("Failed to load text file: {}", filename, e);
            }
        }
    }
    
    /**
     * Create default text command files
     */
    private void createDefaultTextFile(String filename) {
        File textDir = configDir.resolve("text").toFile();
        textDir.mkdirs();
        
        File textFile = new File(textDir, filename);
        
        String content = switch (filename) {
            case "motd.txt" -> """
                &6Welcome to our server!
                &eToday is a great day to play Minecraft!
                &7Type /help for a list of commands.
                &7Current players online: {ONLINE_PLAYERS}
                """;
            case "rules.txt" -> """
                &4Server Rules:
                &71. Be respectful to all players
                &72. No griefing or stealing
                &73. No cheating or hacking
                &74. Keep chat appropriate
                &75. Have fun!
                """;
            case "help.txt" -> """
                &6Available Commands:
                &e/spawn &7- Teleport to spawn
                &e/home &7- Teleport to your home
                &e/sethome &7- Set your home location
                &e/warp <name> &7- Teleport to a warp
                &e/msg <player> <message> &7- Send a private message
                &e/balance &7- Check your balance
                """;
            case "info.txt" -> """
                &6Server Information:
                &eServer Name: {SERVER_NAME}
                &eVersion: {SERVER_VERSION}
                &eOnline Players: {ONLINE_PLAYERS}
                &eMax Players: {MAX_PLAYERS}
                &eUptime: {UPTIME}
                """;
            default -> "Default content for " + filename;
        };
        
        try {
            FileUtils.writeStringToFile(textFile, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create default text file: {}", filename, e);
        }
    }
    
    /**
     * Reload all configurations
     */
    public void reloadConfigurations() {
        NeoEssentials.LOGGER.info("Reloading all configurations...");
        loadConfigurations();
    }
    
    // Getters for configuration objects
    public MainConfig getMainConfig() {
        return mainConfig;
    }
    
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }
    
    public EconomyConfig getEconomyConfig() {
        return economyConfig;
    }
    
    public HomeConfig getHomeConfig() {
        return homeConfig;
    }
    
    public WarpConfig getWarpConfig() {
        return warpConfig;
    }
    
    public KitConfig getKitConfig() {
        return kitConfig;
    }
    
    public ModerationConfig getModerationConfig() {
        return moderationConfig;
    }
    
    public MessagingConfig getMessagingConfig() {
        return messagingConfig;
    }
    
    public TablistConfig getTablistConfig() {
        return tablistConfig;
    }
    
    public DiscordConfig getDiscordConfig() {
        return discordConfig;
    }
    
    public Map<String, String> getTextCommands() {
        return textCommands;
    }
    
    public String getTextCommand(String name) {
        return textCommands.get(name);
    }
    
    @FunctionalInterface
    private interface ConfigSupplier<T> {
        T get();
    }
}
