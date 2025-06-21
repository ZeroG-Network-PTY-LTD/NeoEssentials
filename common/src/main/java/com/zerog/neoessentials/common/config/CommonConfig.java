package com.zerog.neoessentials.common.config;

/**
 * Base configuration class for NeoEssentials that is version-independent.
 * Contains default configuration values that apply across all versions.
 */
public class CommonConfig {
    
    // General settings
    private boolean debugMode = false;
    private String defaultSpawnWorld = "world";
    private int spawnX = 0;
    private int spawnY = 64;
    private int spawnZ = 0;
    private float spawnYaw = 0;
    private float spawnPitch = 0;
    
    // Teleportation settings
    private boolean enableBackCommand = true;
    private int backCommandCooldown = 30;  // seconds
    private int teleportCooldown = 3;      // seconds
    private int teleportWarmup = 3;        // seconds
    private int homesLimit = 3;
    private boolean cancelTeleportOnMove = true;
    private boolean cancelTeleportOnDamage = true;
    
    // Command settings
    private boolean disableVanillaGamemodeCommands = false;
    private boolean disableVanillaTeleportCommands = false;
    private boolean disableVanillaWeatherCommands = false;
    private boolean disableVanillaTimeCommands = false;
    
    // Economy settings
    private boolean enableEconomy = true;
    private String currencySymbol = "$";
    private String currencyNameSingular = "dollar";
    private String currencyNamePlural = "dollars";
    private double startingBalance = 100.0;
    
    // Protection settings
    private boolean preventCreativeModeDrops = true;
    private boolean preventCreativeModeItemStoring = true;
    private boolean lockCreativeModeItems = true;
    
    /**
     * Get whether debug mode is enabled
     * 
     * @return Whether debug mode is enabled
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Set whether debug mode is enabled
     * 
     * @param debugMode Whether debug mode is enabled
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    /**
     * Get the default spawn world
     * 
     * @return The default spawn world name
     */
    public String getDefaultSpawnWorld() {
        return defaultSpawnWorld;
    }
    
    /**
     * Set the default spawn world
     * 
     * @param defaultSpawnWorld The default spawn world name
     */
    public void setDefaultSpawnWorld(String defaultSpawnWorld) {
        this.defaultSpawnWorld = defaultSpawnWorld;
    }
    
    /**
     * Get the spawn X coordinate
     * 
     * @return The spawn X coordinate
     */
    public int getSpawnX() {
        return spawnX;
    }
    
    /**
     * Set the spawn X coordinate
     * 
     * @param spawnX The spawn X coordinate
     */
    public void setSpawnX(int spawnX) {
        this.spawnX = spawnX;
    }
    
    /**
     * Get the spawn Y coordinate
     * 
     * @return The spawn Y coordinate
     */
    public int getSpawnY() {
        return spawnY;
    }
    
    /**
     * Set the spawn Y coordinate
     * 
     * @param spawnY The spawn Y coordinate
     */
    public void setSpawnY(int spawnY) {
        this.spawnY = spawnY;
    }
    
    /**
     * Get the spawn Z coordinate
     * 
     * @return The spawn Z coordinate
     */
    public int getSpawnZ() {
        return spawnZ;
    }
    
    /**
     * Set the spawn Z coordinate
     * 
     * @param spawnZ The spawn Z coordinate
     */
    public void setSpawnZ(int spawnZ) {
        this.spawnZ = spawnZ;
    }
    
    /**
     * Get the spawn yaw angle
     * 
     * @return The spawn yaw angle
     */
    public float getSpawnYaw() {
        return spawnYaw;
    }
    
    /**
     * Set the spawn yaw angle
     * 
     * @param spawnYaw The spawn yaw angle
     */
    public void setSpawnYaw(float spawnYaw) {
        this.spawnYaw = spawnYaw;
    }
    
    /**
     * Get the spawn pitch angle
     * 
     * @return The spawn pitch angle
     */
    public float getSpawnPitch() {
        return spawnPitch;
    }
    
    /**
     * Set the spawn pitch angle
     * 
     * @param spawnPitch The spawn pitch angle
     */
    public void setSpawnPitch(float spawnPitch) {
        this.spawnPitch = spawnPitch;
    }
    
    /**
     * Get whether the back command is enabled
     * 
     * @return Whether the back command is enabled
     */
    public boolean isEnableBackCommand() {
        return enableBackCommand;
    }
    
    /**
     * Set whether the back command is enabled
     * 
     * @param enableBackCommand Whether the back command is enabled
     */
    public void setEnableBackCommand(boolean enableBackCommand) {
        this.enableBackCommand = enableBackCommand;
    }
    
    /**
     * Get the back command cooldown in seconds
     * 
     * @return The back command cooldown in seconds
     */
    public int getBackCommandCooldown() {
        return backCommandCooldown;
    }
    
    /**
     * Set the back command cooldown in seconds
     * 
     * @param backCommandCooldown The back command cooldown in seconds
     */
    public void setBackCommandCooldown(int backCommandCooldown) {
        this.backCommandCooldown = backCommandCooldown;
    }
    
    /**
     * Get the teleport cooldown in seconds
     * 
     * @return The teleport cooldown in seconds
     */
    public int getTeleportCooldown() {
        return teleportCooldown;
    }
    
    /**
     * Set the teleport cooldown in seconds
     * 
     * @param teleportCooldown The teleport cooldown in seconds
     */
    public void setTeleportCooldown(int teleportCooldown) {
        this.teleportCooldown = teleportCooldown;
    }
    
    /**
     * Get the teleport warmup in seconds
     * 
     * @return The teleport warmup in seconds
     */
    public int getTeleportWarmup() {
        return teleportWarmup;
    }
    
    /**
     * Set the teleport warmup in seconds
     * 
     * @param teleportWarmup The teleport warmup in seconds
     */
    public void setTeleportWarmup(int teleportWarmup) {
        this.teleportWarmup = teleportWarmup;
    }
    
    /**
     * Get the homes limit
     * 
     * @return The homes limit
     */
    public int getHomesLimit() {
        return homesLimit;
    }
    
    /**
     * Set the homes limit
     * 
     * @param homesLimit The homes limit
     */
    public void setHomesLimit(int homesLimit) {
        this.homesLimit = homesLimit;
    }
    
    /**
     * Get whether to cancel teleport on move
     * 
     * @return Whether to cancel teleport on move
     */
    public boolean isCancelTeleportOnMove() {
        return cancelTeleportOnMove;
    }
    
    /**
     * Set whether to cancel teleport on move
     * 
     * @param cancelTeleportOnMove Whether to cancel teleport on move
     */
    public void setCancelTeleportOnMove(boolean cancelTeleportOnMove) {
        this.cancelTeleportOnMove = cancelTeleportOnMove;
    }
    
    /**
     * Get whether to cancel teleport on damage
     * 
     * @return Whether to cancel teleport on damage
     */
    public boolean isCancelTeleportOnDamage() {
        return cancelTeleportOnDamage;
    }
    
    /**
     * Set whether to cancel teleport on damage
     * 
     * @param cancelTeleportOnDamage Whether to cancel teleport on damage
     */
    public void setCancelTeleportOnDamage(boolean cancelTeleportOnDamage) {
        this.cancelTeleportOnDamage = cancelTeleportOnDamage;
    }
    
    /**
     * Get whether vanilla gamemode commands should be disabled
     * 
     * @return Whether vanilla gamemode commands should be disabled
     */
    public boolean isDisableVanillaGamemodeCommands() {
        return disableVanillaGamemodeCommands;
    }
    
    /**
     * Set whether vanilla gamemode commands should be disabled
     * 
     * @param disableVanillaGamemodeCommands Whether vanilla gamemode commands should be disabled
     */
    public void setDisableVanillaGamemodeCommands(boolean disableVanillaGamemodeCommands) {
        this.disableVanillaGamemodeCommands = disableVanillaGamemodeCommands;
    }
    
    /**
     * Get whether vanilla teleport commands should be disabled
     * 
     * @return Whether vanilla teleport commands should be disabled
     */
    public boolean isDisableVanillaTeleportCommands() {
        return disableVanillaTeleportCommands;
    }
    
    /**
     * Set whether vanilla teleport commands should be disabled
     * 
     * @param disableVanillaTeleportCommands Whether vanilla teleport commands should be disabled
     */
    public void setDisableVanillaTeleportCommands(boolean disableVanillaTeleportCommands) {
        this.disableVanillaTeleportCommands = disableVanillaTeleportCommands;
    }
    
    /**
     * Get whether vanilla weather commands should be disabled
     * 
     * @return Whether vanilla weather commands should be disabled
     */
    public boolean isDisableVanillaWeatherCommands() {
        return disableVanillaWeatherCommands;
    }
    
    /**
     * Set whether vanilla weather commands should be disabled
     * 
     * @param disableVanillaWeatherCommands Whether vanilla weather commands should be disabled
     */
    public void setDisableVanillaWeatherCommands(boolean disableVanillaWeatherCommands) {
        this.disableVanillaWeatherCommands = disableVanillaWeatherCommands;
    }
    
    /**
     * Get whether vanilla time commands should be disabled
     * 
     * @return Whether vanilla time commands should be disabled
     */
    public boolean isDisableVanillaTimeCommands() {
        return disableVanillaTimeCommands;
    }
    
    /**
     * Set whether vanilla time commands should be disabled
     * 
     * @param disableVanillaTimeCommands Whether vanilla time commands should be disabled
     */
    public void setDisableVanillaTimeCommands(boolean disableVanillaTimeCommands) {
        this.disableVanillaTimeCommands = disableVanillaTimeCommands;
    }
    
    /**
     * Get whether economy is enabled
     * 
     * @return Whether economy is enabled
     */
    public boolean isEnableEconomy() {
        return enableEconomy;
    }
    
    /**
     * Set whether economy is enabled
     * 
     * @param enableEconomy Whether economy is enabled
     */
    public void setEnableEconomy(boolean enableEconomy) {
        this.enableEconomy = enableEconomy;
    }
    
    /**
     * Get the currency symbol
     * 
     * @return The currency symbol
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }
    
    /**
     * Set the currency symbol
     * 
     * @param currencySymbol The currency symbol
     */
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }
    
    /**
     * Get the singular currency name
     * 
     * @return The singular currency name
     */
    public String getCurrencyNameSingular() {
        return currencyNameSingular;
    }
    
    /**
     * Set the singular currency name
     * 
     * @param currencyNameSingular The singular currency name
     */
    public void setCurrencyNameSingular(String currencyNameSingular) {
        this.currencyNameSingular = currencyNameSingular;
    }
    
    /**
     * Get the plural currency name
     * 
     * @return The plural currency name
     */
    public String getCurrencyNamePlural() {
        return currencyNamePlural;
    }
    
    /**
     * Set the plural currency name
     * 
     * @param currencyNamePlural The plural currency name
     */
    public void setCurrencyNamePlural(String currencyNamePlural) {
        this.currencyNamePlural = currencyNamePlural;
    }
    
    /**
     * Get the starting balance
     * 
     * @return The starting balance
     */
    public double getStartingBalance() {
        return startingBalance;
    }
    
    /**
     * Set the starting balance
     * 
     * @param startingBalance The starting balance
     */
    public void setStartingBalance(double startingBalance) {
        this.startingBalance = startingBalance;
    }
    
    /**
     * Get whether to prevent players in creative mode from dropping items
     * 
     * @return Whether to prevent players in creative mode from dropping items
     */
    public boolean isPreventCreativeModeDrops() {
        return preventCreativeModeDrops;
    }
    
    /**
     * Set whether to prevent players in creative mode from dropping items
     * 
     * @param preventCreativeModeDrops Whether to prevent players in creative mode from dropping items
     */
    public void setPreventCreativeModeDrops(boolean preventCreativeModeDrops) {
        this.preventCreativeModeDrops = preventCreativeModeDrops;
    }
    
    /**
     * Get whether to prevent players in creative mode from storing items in containers
     * 
     * @return Whether to prevent players in creative mode from storing items in containers
     */
    public boolean isPreventCreativeModeItemStoring() {
        return preventCreativeModeItemStoring;
    }
    
    /**
     * Set whether to prevent players in creative mode from storing items in containers
     * 
     * @param preventCreativeModeItemStoring Whether to prevent players in creative mode from storing items in containers
     */
    public void setPreventCreativeModeItemStoring(boolean preventCreativeModeItemStoring) {
        this.preventCreativeModeItemStoring = preventCreativeModeItemStoring;
    }
    
    /**
     * Get whether to lock creative mode items
     * 
     * @return Whether to lock creative mode items
     */
    public boolean isLockCreativeModeItems() {
        return lockCreativeModeItems;
    }
    
    /**
     * Set whether to lock creative mode items
     * 
     * @param lockCreativeModeItems Whether to lock creative mode items
     */
    public void setLockCreativeModeItems(boolean lockCreativeModeItems) {
        this.lockCreativeModeItems = lockCreativeModeItems;
    }
}
