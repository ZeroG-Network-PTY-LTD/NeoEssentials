package com.zerog.neoessentials.config;

import com.zerog.neoessentials.NeoEssentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration settings for the tablist functionality.
 */
public class TablistConfig {
    // Update frequency (milliseconds)
    private long updateInterval = 2000;
    
    // Server name for placeholders
    private String serverName = "NeoEssentials Server";
    
    // Time format for %time% placeholder
    private String timeFormat = "HH:mm:ss";
    
    // Header templates with placeholders
    private List<String> headers = new ArrayList<>();
    
    // Footer templates with placeholders
    private List<String> footers = new ArrayList<>();
<<<<<<< HEAD
      // Sorting options
=======
    
    // Sorting options
>>>>>>> 552699e (feat: Add TablistConfig and TablistManager for custom tablist functionality)
    private boolean enableSorting = true;
    private String sortType = "name";  // Can be name, rank, or playtime
    
    // Display options
    private boolean showEconomyInTablist = true;
<<<<<<< HEAD
    private boolean enablePlayerSpecificHeaders = true;
    private boolean enablePlayerSpecificFooters = true;
=======
>>>>>>> 552699e (feat: Add TablistConfig and TablistManager for custom tablist functionality)
    
    // Path to config file
    private static final String CONFIG_FILE = "neoessentials/tablist.json";
    
    /**
     * Creates a new tablist config with default values
     */
    public TablistConfig() {
        setDefaults();
    }
    
    /**
     * Sets default values for the config
     */
    private void setDefaults() {
<<<<<<< HEAD
    // Default headers with more variety like BungeeTablistPlus
        headers.addAll(Arrays.asList(
            "&6&lWelcome to &e&l%server_name%",
            "&e&lPlayers Online: &a%online_players%&e/&a%max_players%",
            "&b&lServer TPS: &a%server_tps%",
            "&d&l%server_name% &f- &6The Best Minecraft Server",
            "&6&l━━━━━━━━━━━━━━━━━━━━━━━",
            "&e&l%server_name% &7- &fTime: &a%time%"
        ));
          // Default footers with enhanced styling like BungeeTablistPlus
        footers.addAll(Arrays.asList(
            "&6&l━━━━━━━━━━━━━━━━━━━━━━━",
            "&7&lWebsite: &b&nwww.example.com",
            "&7&lDiscord: &b&ndiscord.gg/example",
            "&7&lCurrent Time: &a%time% &7| &7&lOnline: &a%online_players% &7players",
            "&e&lStore: &b&nstore.example.com &7- &6Support the server!",
            "&d&lTPS: &a%server_tps% &7| &c&lPing: &a%ping%ms"
=======
        // Default headers
        headers.addAll(Arrays.asList(
            "&6Welcome to &l%server_name%",
            "&ePlayers Online: &a%online_players%&e/&a%max_players%",
            "&bServer TPS: &a%server_tps%"
        ));
        
        // Default footers
        footers.addAll(Arrays.asList(
            "&7Website: &fwww.example.com",
            "&7Discord: &fdiscord.gg/example",
            "&7Current Time: &f%time%"
>>>>>>> 552699e (feat: Add TablistConfig and TablistManager for custom tablist functionality)
        ));
    }
    
    /**
     * Loads the config from disk
     * 
     * @return True if loaded successfully, false if not
     */
    public boolean load() {
        File configFile = new File(CONFIG_FILE);
        
        if (!configFile.exists()) {
            NeoEssentials.LOGGER.info("Tablist config file not found, creating default");
            setDefaults();
            save();
            return true;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            TablistConfig loaded = gson.fromJson(reader, TablistConfig.class);
            
            // Copy values from loaded config
            this.updateInterval = loaded.updateInterval;
            this.serverName = loaded.serverName;
            this.timeFormat = loaded.timeFormat;
            this.headers = loaded.headers;
            this.footers = loaded.footers;
            this.enableSorting = loaded.enableSorting;
            this.sortType = loaded.sortType;
            this.showEconomyInTablist = loaded.showEconomyInTablist;
            
            NeoEssentials.LOGGER.info("Loaded tablist config from disk");
            return true;
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            NeoEssentials.LOGGER.error("Error loading tablist config", e);
            return false;
        }
    }
    
    /**
     * Saves the config to disk
     * 
     * @return True if saved successfully, false if not
     */
    public boolean save() {
        // Create parent directory if it doesn't exist
        File configFile = new File(CONFIG_FILE);
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(this, writer);
            
            NeoEssentials.LOGGER.info("Saved tablist config to disk");
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving tablist config", e);
            return false;
        }
    }
    
    /**
     * Gets the update interval in milliseconds
     * 
     * @return The update interval
     */
    public long getUpdateInterval() {
        return updateInterval;
    }
    
    /**
     * Sets the update interval in milliseconds
     * 
     * @param updateInterval The new update interval
     */
    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = Math.max(1000, updateInterval);
    }
    
    /**
     * Gets the server name for placeholders
     * 
     * @return The server name
     */
    public String getServerName() {
        return serverName;
    }
    
    /**
     * Sets the server name for placeholders
     * 
     * @param serverName The new server name
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    /**
     * Gets the time format for %time% placeholder
     * 
     * @return The time format
     */
    public String getTimeFormat() {
        return timeFormat;
    }
    
    /**
     * Sets the time format for %time% placeholder
     * 
     * @param timeFormat The new time format
     */
    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }
    
    /**
     * Gets the list of header templates
     * 
     * @return The header templates
     */
    public List<String> getHeaders() {
        return headers;
    }
    
    /**
     * Sets the list of header templates
     * 
     * @param headers The new header templates
     */
    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }
    
    /**
     * Gets the list of footer templates
     * 
     * @return The footer templates
     */
    public List<String> getFooters() {
        return footers;
    }
    
    /**
     * Sets the list of footer templates
     * 
     * @param footers The new footer templates
     */
    public void setFooters(List<String> footers) {
        this.footers = footers;
    }
    
    /**
     * Checks if sorting is enabled
     * 
     * @return True if sorting is enabled, false otherwise
     */
    public boolean isEnableSorting() {
        return enableSorting;
    }
    
    /**
     * Sets whether sorting is enabled
     * 
     * @param enableSorting Whether sorting should be enabled
     */
    public void setEnableSorting(boolean enableSorting) {
        this.enableSorting = enableSorting;
    }
    
    /**
     * Gets the sort type
     * 
     * @return The sort type (name, rank, or playtime)
     */
    public String getSortType() {
        return sortType;
    }
    
    /**
     * Sets the sort type
     * 
     * @param sortType The new sort type (name, rank, or playtime)
     */
    public void setSortType(String sortType) {
        if (sortType.equals("name") || sortType.equals("rank") || sortType.equals("playtime")) {
            this.sortType = sortType;
        } else {
            this.sortType = "name";
        }
    }
    
    /**
     * Checks if economy info should be shown in the tablist
     * 
     * @return True if economy info should be shown, false otherwise
     */
    public boolean isShowEconomyInTablist() {
        return showEconomyInTablist;
    }
    
    /**
     * Sets whether economy info should be shown in the tablist
     * 
     * @param showEconomyInTablist Whether economy info should be shown
     */
    public void setShowEconomyInTablist(boolean showEconomyInTablist) {
        this.showEconomyInTablist = showEconomyInTablist;
    }
<<<<<<< HEAD
    
    /**
     * Checks if player-specific headers are enabled
     * 
     * @return True if player-specific headers are enabled, false otherwise
     */
    public boolean isEnablePlayerSpecificHeaders() {
        return enablePlayerSpecificHeaders;
    }
    
    /**
     * Sets whether player-specific headers are enabled
     * 
     * @param enablePlayerSpecificHeaders Whether player-specific headers should be enabled
     */
    public void setEnablePlayerSpecificHeaders(boolean enablePlayerSpecificHeaders) {
        this.enablePlayerSpecificHeaders = enablePlayerSpecificHeaders;
    }
    
    /**
     * Checks if player-specific footers are enabled
     * 
     * @return True if player-specific footers are enabled, false otherwise
     */
    public boolean isEnablePlayerSpecificFooters() {
        return enablePlayerSpecificFooters;
    }
    
    /**
     * Sets whether player-specific footers are enabled
     * 
     * @param enablePlayerSpecificFooters Whether player-specific footers should be enabled
     */
    public void setEnablePlayerSpecificFooters(boolean enablePlayerSpecificFooters) {
        this.enablePlayerSpecificFooters = enablePlayerSpecificFooters;
    }
=======
>>>>>>> 552699e (feat: Add TablistConfig and TablistManager for custom tablist functionality)
}
