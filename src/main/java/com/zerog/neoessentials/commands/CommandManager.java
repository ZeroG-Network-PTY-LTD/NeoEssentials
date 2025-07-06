package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Manages registration and execution of all NeoEssentials commands.
 * <p>
 * The CommandManager is the central coordinator for all mod commands. It handles:
 * <ul>
 *   <li>Initializing all command classes</li>
 *   <li>Registering commands with Minecraft's command system</li>
 *   <li>Managing command permissions and execution</li>
 *   <li>Providing access to command functionality for other parts of the mod</li>
 * </ul>
 * All commands are registered during server startup via the RegisterCommandsEvent.
 * </p>
 * 
 * @author ZeroG
 * @since 1.0.0
 */

public class CommandManager {    // Command classes
    private final TeleportCommands teleportCommands;
    private final DirectTeleportCommands directTeleportCommands;
    private final TeleportBookmarkCommands teleportBookmarkCommands;
    private final WorldManagementCommands worldManagementCommands;
    private final PlayerStatsCommands playerStatsCommands;
    private final AdvancedUtilityCommands advancedUtilityCommands;
    private final WorldCommands worldCommands;
    private final HomeCommands homeCommands;
    private final UserCommands userCommands;
    private final WarpCommands warpCommands;
    private final KitCommands kitCommands;
    private final TimeAndWeatherCommands timeAndWeatherCommands;
    private final InventoryCommands inventoryCommands;
    private final PlayerCommands playerCommands;
    private final MessageCommands messageCommands;
    private final ModeratorCommands moderatorCommands;
    private final AfkCommands afkCommands;
    private final UtilityCommands utilityCommands;    
    private final UICommands uiCommands;
    private final JailCommands jailCommands;
    private final PowerToolCommands powerToolCommands;
    private final MailCommands mailCommands;
    private final AdminPanelCommand adminPanelCommand;
    private final TablistCommand tablistCommand;
    // SignEditCommands is now fully static
    // InventoryManagementCommands is now fully static
    private final ItemEnhancementCommands itemEnhancementCommands;
    
    // Debug command disabled while developing TablistFix
      // NeoEssentials main reference - disabled while developing TablistFix
    // private final NeoEssentials mod;
      public CommandManager() {
        // mod reference removed while disabling debug commands
        
        teleportCommands = new TeleportCommands();
        directTeleportCommands = new DirectTeleportCommands();
        teleportBookmarkCommands = new TeleportBookmarkCommands();
        worldManagementCommands = new WorldManagementCommands();
        playerStatsCommands = new PlayerStatsCommands();
        advancedUtilityCommands = new AdvancedUtilityCommands();
        worldCommands = new WorldCommands();
        homeCommands = new HomeCommands();
        userCommands = new UserCommands();
        warpCommands = new WarpCommands();
        kitCommands = new KitCommands();
        timeAndWeatherCommands = new TimeAndWeatherCommands();
        inventoryCommands = new InventoryCommands();
        playerCommands = new PlayerCommands();
        messageCommands = new MessageCommands();
        moderatorCommands = new ModeratorCommands();
        afkCommands = new AfkCommands();
        utilityCommands = new UtilityCommands();
        uiCommands = new UICommands();
        jailCommands = new JailCommands();
        powerToolCommands = new PowerToolCommands();
        mailCommands = new MailCommands();
        adminPanelCommand = new AdminPanelCommand();
        tablistCommand = new TablistCommand();
        // SignEditCommands is now fully static - no instance needed
        // InventoryManagementCommands is now fully static - no instance needed
        itemEnhancementCommands = new ItemEnhancementCommands();
        
        // Debug commands will be initialized later when TABLikeTablistManager is available
        // ItemCommands needs CommandBuildContext which is only available during register event
    }
    
    /**
     * Registers all commands with the server.
     */
    public void registerCommands() {
        NeoEssentials.LOGGER.info("Registering NeoEssentials commands");
    }
    
    /**
     * Event handler for command registration.
     * 
     * @param event The register commands event
     */    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NeoEssentials.LOGGER.info("Registering NeoEssentials commands");
        
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Register all command categories directly since we're an instance
        registerAllCommands(dispatcher);
        
        // Register ItemCommands with the CommandBuildContext from the event
        ItemCommands itemCommands = new ItemCommands(event.getBuildContext());
        itemCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered item commands");
          // Debug command temporarily disabled while developing TablistFix
        /*
        if (mod != null && mod.getTablistManager() != null) {
            // Debug command code goes here when re-enabled
        } else {
            NeoEssentials.LOGGER.warn("Could not register tablist debug command: TABLikeTablistManager not available");
        }
        */
    }
      /**
     * Registers all command categories with the dispatcher.
     * 
     * @param dispatcher The command dispatcher
     */    private void registerAllCommands(CommandDispatcher<CommandSourceStack> dispatcher) {        // Register teleport commands
        teleportCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered teleport commands");
        
        // Register direct teleport commands
        directTeleportCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered direct teleport commands");
        
        // Register teleport bookmark commands
        teleportBookmarkCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered teleport bookmark commands");
        
        // Register world management commands
        worldManagementCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered world management commands");
        
        // Register player statistics commands
        playerStatsCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered player statistics commands");
        
        // Register advanced utility commands
        advancedUtilityCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered advanced utility commands");
        
        // Register player settings commands
        PlayerSettingsCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered player settings commands");
        
        // Register world management commands
        worldCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered world management commands");
        
        // Register home commands
        homeCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered home commands");
        
        // Register user commands
        userCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered user commands");
        
        // Register warp commands
        warpCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered warp commands");
        
        // Register kit commands
        kitCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered kit commands");
          // Register time and weather commands
        timeAndWeatherCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered time and weather commands");
        
        // Register inventory commands
        inventoryCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered inventory commands");
        
        // Register player commands
        playerCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered player commands");
        
        // Register message commands
        messageCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered message commands");
          // Register moderator commands
        moderatorCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered moderator commands");
          // Register AFK commands
        afkCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered AFK commands");
        
        // Register utility commands
        utilityCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered utility commands");
        
        // Register UI commands
        uiCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered UI commands");
        
        // Register jail commands
        jailCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered jail commands");
        
        // Register powertool commands
        powerToolCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered powertool commands");
          // Register mail commands
        mailCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered mail commands");
          // Register admin panel commands
        adminPanelCommand.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered admin panel commands");
            // Register tablist commands
        tablistCommand.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered tablist commands");
        
        // Register permission commands
        PermissionCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered permission commands");
        
        // Register tabfix commands
        TabFixCommand.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered tablist fix commands");
        
        // Register sign edit commands
        // TODO: Fix SignEditCommands compilation issue
        // SignEditCommands.register(dispatcher);
        // NeoEssentials.LOGGER.info("Registered sign edit commands");
        
        // Register inventory management commands
        InventoryManagementCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered inventory management commands");
        
        // Register item enhancement commands
        ItemEnhancementCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered item enhancement commands");
        
        // Register gamemode commands
        GamemodeCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered gamemode commands");
        
        // Register spawn commands
        SpawnCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered spawn commands");
        
        // Register permission commands
        PermissionCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered permission commands");
        
        // Register economy commands
        com.zerog.neoessentials.economy.commands.EconomyCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered economy commands");
        
        // Register shop commands  
        com.zerog.neoessentials.economy.commands.ShopCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered shop commands");
        
        // Register auction commands
        com.zerog.neoessentials.economy.commands.AuctionCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered auction commands");
        
        // Register GUI commands
        com.zerog.neoessentials.economy.commands.ShopGuiCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered shop GUI commands");
        
        com.zerog.neoessentials.economy.commands.AuctionGuiCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered auction GUI commands");
        
        // Note: ItemCommands require CommandBuildContext which is not available here
        // In a full implementation, you would need to get the CommandBuildContext properly
        
        // For now, we'll skip registering ItemCommands until we can find a proper way
        // to get the CommandBuildContext
        NeoEssentials.LOGGER.info("Skipping ItemCommands registration due to CommandBuildContext requirements");
    }
    
    /**
     * Gets the teleport commands instance
     * 
     * @return The teleport commands
     */
    public TeleportCommands getTeleportCommands() {
        return teleportCommands;
    }
    
    /**
     * Gets the home commands instance
     * 
     * @return The home commands
     */
    public HomeCommands getHomeCommands() {
        return homeCommands;
    }
    
    /**
     * Gets the user commands instance
     * 
     * @return The user commands
     */    
    public UserCommands getUserCommands() {
        return userCommands;
    }
      /**
     * Gets the warp commands instance
     * 
     * @return The warp commands
     */
    public WarpCommands getWarpCommands() {
        return warpCommands;
    }
      /**
     * Gets the kit commands instance
     * 
     * @return The kit commands
     */
    /**
     * Static method to check if a source has a permission.
     * This is a convenience method for commands to use.
     *
     * @param source The command source to check
     * @param permission The permission node to check for
     * @return True if the source has permission, false otherwise
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        return com.zerog.neoessentials.utils.PermissionUtil.hasPermission(source, permission);
    }
    
    /**
     * Gets the kit commands instance
     * 
     * @return The kit commands
     */
    public KitCommands getKitCommands() {
        return kitCommands;
    }
    
    /**
     * Gets the time and weather commands instance
     * 
     * @return The time and weather commands
     */
    public TimeAndWeatherCommands getTimeAndWeatherCommands() {
        return timeAndWeatherCommands;    }
    
    /**
     * Gets the player commands instance
     * 
     * @return The player commands
     */
    public PlayerCommands getPlayerCommands() {
        return playerCommands;
    }
    
    /**
     * Gets the message commands instance
     * 
     * @return The message commands
     */
    public MessageCommands getMessageCommands() {
        return messageCommands;
    }
    
    /**
     * Gets the moderator commands instance
     * 
     * @return The moderator commands
     */
    public ModeratorCommands getModeratorCommands() {
        return moderatorCommands;
    }
      /**
     * Gets the AFK commands instance
     * 
     * @return The AFK commands
     */
    public AfkCommands getAfkCommands() {
        return afkCommands;
    }
    
    /**
     * Gets the utility commands instance
     * 
     * @return The utility commands
     */
    public UtilityCommands getUtilityCommands() {
        return utilityCommands;
    }
    
    /**
     * Gets the UI commands instance
     * 
     * @return The UI commands
     */
    public UICommands getUICommands() {
        return uiCommands;
    }
    
    /**
     * Gets the jail commands instance
     * 
     * @return The jail commands
     */
    public JailCommands getJailCommands() {
        return jailCommands;
    }
    
    /**
     * Gets the powertool commands instance
     * 
     * @return The powertool commands
     */
    public PowerToolCommands getPowerToolCommands() {
        return powerToolCommands;
    }
    
    /**
     * Gets the mail commands instance
     * 
     * @return The mail commands
     */
    public MailCommands getMailCommands() {
        return mailCommands;
    }
    

}
