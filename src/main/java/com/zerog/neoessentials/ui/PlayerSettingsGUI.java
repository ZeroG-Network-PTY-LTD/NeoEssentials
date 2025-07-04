package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.PlayerSettingsManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an interactive GUI for managing player settings and preferences.
 * Uses the MenuSystem to create user-friendly menus for configuration.
 */
public class PlayerSettingsGUI {

    /**
     * Shows the main player settings menu to a player
     * 
     * @param player The player to show the menu to
     */
    public static void showMainMenu(ServerPlayer player) {
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Teleportation settings
        items.add(new MenuSystem.MenuItem(
            "&b📍 Teleportation Settings",
            "/playersettings gui teleport",
            "&7Configure teleportation preferences"
        ));
        
        // Interface and GUI settings
        items.add(new MenuSystem.MenuItem(
            "&d🎨 Interface Settings",
            "/playersettings gui interface",
            "&7Customize your interface preferences"
        ));
        
        // Chat and messaging settings
        items.add(new MenuSystem.MenuItem(
            "&e💬 Chat & Messages",
            "/playersettings gui messages",
            "&7Configure chat and message preferences"
        ));
        
        // Economy settings
        items.add(new MenuSystem.MenuItem(
            "&a💰 Economy Settings",
            "/playersettings gui economy",
            "&7Configure economy and payment preferences"
        ));
        
        // Privacy settings
        items.add(new MenuSystem.MenuItem(
            "&c🔒 Privacy Settings",
            "/playersettings gui privacy",
            "&7Manage your privacy preferences"
        ));
        
        // Advanced/Custom settings
        items.add(new MenuSystem.MenuItem(
            "&f⚙️ Advanced Settings",
            "/playersettings gui advanced",
            "&7Configure advanced and custom settings"
        ));
        
        // Reset to defaults option
        items.add(new MenuSystem.MenuItem(
            "&8🔄 Reset to Defaults",
            "/playersettings reset confirm",
            "&cReset all settings to default values"
        ));
        
        MenuSystem.builder()
            .title("Player Settings")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings", "&7Back to Commands", "&7Return to command-based settings")
            .show(player, 1);
    }
    
    /**
     * Shows the teleportation settings submenu
     * 
     * @param player The player to show the menu to
     */
    public static void showTeleportSettings(ServerPlayer player) {
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Auto-record teleports setting
        String autoRecordStatus = settings.isAutoRecordTeleports() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&b📝 Auto-Record Teleports: " + autoRecordStatus,
            "/playersettings set autoRecordTeleports " + !settings.isAutoRecordTeleports(),
            "&7Toggle automatic recording of teleportation history"
        ));
        
        // Teleport confirmations setting
        String confirmStatus = settings.isTeleportConfirmations() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&e⚠️ Teleport Confirmations: " + confirmStatus,
            "/playersettings set teleportConfirmations " + !settings.isTeleportConfirmations(),
            "&7Toggle confirmation prompts before teleporting"
        ));
        
        // Max teleport history setting
        items.add(new MenuSystem.MenuItem(
            "&d📚 Max History: &f" + settings.getMaxTeleportHistory(),
            "/playersettings gui teleport history",
            "&7Change maximum teleport history entries"
        ));
        
        // Show teleport messages setting
        String messagesStatus = settings.isShowTeleportMessages() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&6📢 Teleport Messages: " + messagesStatus,
            "/playersettings set showTeleportMessages " + !settings.isShowTeleportMessages(),
            "&7Toggle teleportation status messages"
        ));
        
        // Allow teleport requests setting
        String requestsStatus = settings.isAllowTeleportRequests() ? "&aAllowed" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&c🚫 Teleport Requests: " + requestsStatus,
            "/playersettings set allowTeleportRequests " + !settings.isAllowTeleportRequests(),
            "&7Toggle allowing teleport requests from other players"
        ));
        
        MenuSystem.builder()
            .title("Teleportation Settings")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings gui", "&7Back to Main Menu", "&7Return to the main settings menu")
            .show(player, 1);
    }
    
    /**
     * Shows the interface settings submenu
     * 
     * @param player The player to show the menu to
     */
    public static void showInterfaceSettings(ServerPlayer player) {
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Prefer GUI interfaces setting
        String guiStatus = settings.isPreferGUIInterfaces() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&d🎨 Prefer GUI Interfaces: " + guiStatus,
            "/playersettings set preferGUIInterfaces " + !settings.isPreferGUIInterfaces(),
            "&7Toggle preference for graphical vs text interfaces"
        ));
        
        // GUI theme setting
        items.add(new MenuSystem.MenuItem(
            "&e🎭 GUI Theme: &f" + settings.getGuiTheme(),
            "/playersettings gui interface theme",
            "&7Change your interface theme"
        ));
        
        // Show system messages setting
        String systemStatus = settings.isShowSystemMessages() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&6📋 System Messages: " + systemStatus,
            "/playersettings set showSystemMessages " + !settings.isShowSystemMessages(),
            "&7Toggle system status and information messages"
        ));
        
        MenuSystem.builder()
            .title("Interface Settings")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings gui", "&7Back to Main Menu", "&7Return to the main settings menu")
            .show(player, 1);
    }
    
    /**
     * Shows the economy settings submenu
     * 
     * @param player The player to show the menu to
     */
    public static void showEconomySettings(ServerPlayer player) {
        PlayerSettingsManager settingsManager = DataManager.getInstance().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Show economy messages setting
        String economyStatus = settings.isShowEconomyMessages() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&a💰 Economy Messages: " + economyStatus,
            "/playersettings set showEconomyMessages " + !settings.isShowEconomyMessages(),
            "&7Toggle economy transaction and status messages"
        ));
        
        // Auto payment confirmations setting
        String paymentStatus = settings.isAutoPaymentConfirmations() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&e⚠️ Payment Confirmations: " + paymentStatus,
            "/playersettings set autoPaymentConfirmations " + !settings.isAutoPaymentConfirmations(),
            "&7Toggle confirmation prompts for payments"
        ));
        
        // Preferred currency setting
        items.add(new MenuSystem.MenuItem(
            "&b💱 Preferred Currency: &f" + settings.getPreferredCurrency(),
            "/playersettings gui economy currency",
            "&7Change your preferred currency"
        ));
        
        MenuSystem.builder()
            .title("Economy Settings")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings gui", "&7Back to Main Menu", "&7Return to the main settings menu")
            .show(player, 1);
    }
    
    /**
     * Shows the privacy settings submenu
     * 
     * @param player The player to show the menu to
     */
    public static void showPrivacySettings(ServerPlayer player) {
        PlayerSettingsManager settingsManager = DataManager.getInstance().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Show online status setting
        String onlineStatus = settings.isShowOnlineStatus() ? "&aVisible" : "&cHidden";
        items.add(new MenuSystem.MenuItem(
            "&b👀 Online Status: " + onlineStatus,
            "/playersettings set showOnlineStatus " + !settings.isShowOnlineStatus(),
            "&7Toggle visibility of your online status"
        ));
        
        // Allow player info setting
        String infoStatus = settings.isAllowPlayerInfo() ? "&aAllowed" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&e📊 Player Info Access: " + infoStatus,
            "/playersettings set allowPlayerInfo " + !settings.isAllowPlayerInfo(),
            "&7Toggle whether others can view your player information"
        ));
        
        // Allow teleport requests (duplicated from teleport menu for convenience)
        String requestsStatus = settings.isAllowTeleportRequests() ? "&aAllowed" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&c🚫 Teleport Requests: " + requestsStatus,
            "/playersettings set allowTeleportRequests " + !settings.isAllowTeleportRequests(),
            "&7Toggle allowing teleport requests from other players"
        ));
        
        MenuSystem.builder()
            .title("Privacy Settings")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings gui", "&7Back to Main Menu", "&7Return to the main settings menu")
            .show(player, 1);
    }
    
    /**
     * Shows the advanced settings submenu
     * 
     * @param player The player to show the menu to
     */
    public static void showAdvancedSettings(ServerPlayer player) {
        PlayerSettingsManager settingsManager = DataManager.getInstance().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // World change messages setting
        String worldStatus = settings.isShowWorldChangeMessages() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&6🌍 World Change Messages: " + worldStatus,
            "/playersettings set showWorldChangeMessages " + !settings.isShowWorldChangeMessages(),
            "&7Toggle messages when changing between worlds"
        ));
        
        // Auto world spawn setting
        String spawnStatus = settings.isAutoWorldSpawn() ? "&aEnabled" : "&cDisabled";
        items.add(new MenuSystem.MenuItem(
            "&d🏠 Auto World Spawn: " + spawnStatus,
            "/playersettings set autoWorldSpawn " + !settings.isAutoWorldSpawn(),
            "&7Toggle automatic teleport to spawn when changing worlds"
        ));
        
        // View custom settings
        items.add(new MenuSystem.MenuItem(
            "&f⚙️ Custom Settings",
            "/playersettings list custom",
            "&7View and manage custom settings"
        ));
        
        // Export settings
        items.add(new MenuSystem.MenuItem(
            "&b📤 Export Settings",
            "/playersettings export",
            "&7Export your settings to a file"
        ));
        
        // Import settings
        items.add(new MenuSystem.MenuItem(
            "&a📥 Import Settings",
            "/playersettings import",
            "&7Import settings from a file"
        ));
        
        MenuSystem.builder()
            .title("Advanced Settings")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings gui", "&7Back to Main Menu", "&7Return to the main settings menu")
            .show(player, 1);
    }
    
    /**
     * Shows the teleport history limit configuration menu
     * 
     * @param player The player to show the menu to
     */
    public static void showTeleportHistoryConfig(ServerPlayer player) {
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Different history limit options
        int[] options = {10, 25, 50, 100, 250, 500};
        
        for (int option : options) {
            items.add(new MenuSystem.MenuItem(
                "&b📚 " + option + " entries",
                "/playersettings set maxTeleportHistory " + option,
                "&7Set maximum teleport history to " + option + " entries"
            ));
        }
        
        MenuSystem.builder()
            .title("Teleport History Limit")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings gui teleport", "&7Back to Teleport Settings", "&7Return to teleport settings menu")
            .show(player, 1);
    }
    
    /**
     * Shows the theme selection menu
     * 
     * @param player The player to show the menu to
     */
    public static void showThemeSelection(ServerPlayer player) {
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Available themes
        String[] themes = {"default", "dark", "light", "colorful", "minimal", "classic"};
        
        for (String theme : themes) {
            String displayName = theme.substring(0, 1).toUpperCase() + theme.substring(1);
            items.add(new MenuSystem.MenuItem(
                "&e🎭 " + displayName,
                "/playersettings set guiTheme " + theme,
                "&7Switch to the " + displayName + " theme"
            ));
        }
        
        MenuSystem.builder()
            .title("Interface Themes")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings gui interface", "&7Back to Interface Settings", "&7Return to interface settings menu")
            .show(player, 1);
    }
    
    /**
     * Shows the currency selection menu
     * 
     * @param player The player to show the menu to
     */
    public static void showCurrencySelection(ServerPlayer player) {
        List<MenuSystem.MenuItem> items = new ArrayList<>();
        
        // Available currencies (would normally come from economy system)
        String[] currencies = {"default", "gold", "silver", "copper", "emerald", "diamond"};
        
        for (String currency : currencies) {
            String displayName = currency.substring(0, 1).toUpperCase() + currency.substring(1);
            items.add(new MenuSystem.MenuItem(
                "&a💰 " + displayName,
                "/playersettings set preferredCurrency " + currency,
                "&7Set your preferred currency to " + displayName
            ));
        }
        
        MenuSystem.builder()
            .title("Preferred Currency")
            .items(items)
            .itemsPerPage(8)
            .showPageNumbers(false)
            .back("/playersettings gui economy", "&7Back to Economy Settings", "&7Return to economy settings menu")
            .show(player, 1);
    }
}
