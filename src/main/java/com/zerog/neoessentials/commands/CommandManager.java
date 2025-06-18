package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.NeoEssentials;
<<<<<<< HEAD
<<<<<<< HEAD
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
=======
import com.zerog.neoessentials.utils.PermissionUtil;
import net.minecraft.commands.CommandBuildContext;
=======
>>>>>>> 2a4d122 (feat: Refactor CommandManager to streamline command registration and add utility/UI command accessors)
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Manages registration and execution of all NeoEssentials commands.
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
 */

public class CommandManager {    // Command classes
    private final TeleportCommands teleportCommands;
    private final HomeCommands homeCommands;
    private final EconomyCommands economyCommands;
    private final UserCommands userCommands;
    private final WarpCommands warpCommands;
    private final KitCommands kitCommands;
    private final TimeAndWeatherCommands timeAndWeatherCommands;
<<<<<<< HEAD
<<<<<<< HEAD
    private final InventoryCommands inventoryCommands;
    private final PlayerCommands playerCommands;
    private final MessageCommands messageCommands;
    private final ModeratorCommands moderatorCommands;
<<<<<<< HEAD
<<<<<<< HEAD
    private final AfkCommands afkCommands;
    private final UtilityCommands utilityCommands;    
    private final UICommands uiCommands;
    private final JailCommands jailCommands;
    private final PowerToolCommands powerToolCommands;
    private final MailCommands mailCommands;
    private final AdminPanelCommand adminPanelCommand;    public CommandManager() {
=======
    
    public CommandManager() {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
    private final InventoryCommands inventoryCommands;
    private final PlayerCommands playerCommands;
    private final MessageCommands messageCommands;
<<<<<<< HEAD
      public CommandManager() {
>>>>>>> bac244b (Implement messaging and player state commands)
=======
    private final ModeratorCommands moderatorCommands;      public CommandManager() {
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
    private final AfkCommands afkCommands;    public CommandManager() {
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)
=======
    private final AfkCommands afkCommands;
    private final UtilityCommands utilityCommands;    
    private final UICommands uiCommands;
    private final JailCommands jailCommands;
    private final PowerToolCommands powerToolCommands;
    private final MailCommands mailCommands;

    public CommandManager() {
>>>>>>> e5d4bb8 (feat: Add UI command functionality for various crafting interfaces including workbench, anvil, and stonecutter)
        teleportCommands = new TeleportCommands();
        homeCommands = new HomeCommands();
        economyCommands = new EconomyCommands();
        userCommands = new UserCommands();
        warpCommands = new WarpCommands();
        kitCommands = new KitCommands();
        timeAndWeatherCommands = new TimeAndWeatherCommands();
<<<<<<< HEAD
<<<<<<< HEAD
        inventoryCommands = new InventoryCommands();
        playerCommands = new PlayerCommands();
        messageCommands = new MessageCommands();
        moderatorCommands = new ModeratorCommands();
        afkCommands = new AfkCommands();
<<<<<<< HEAD
<<<<<<< HEAD
        utilityCommands = new UtilityCommands();
        uiCommands = new UICommands();
        jailCommands = new JailCommands();
        powerToolCommands = new PowerToolCommands();
        mailCommands = new MailCommands();
        adminPanelCommand = new AdminPanelCommand();
        // ItemCommands needs CommandBuildContext which is only available during register event
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        inventoryCommands = new InventoryCommands();
        playerCommands = new PlayerCommands();
        messageCommands = new MessageCommands();
<<<<<<< HEAD
        playerCommands = new PlayerCommands();
        messageCommands = new MessageCommands();
>>>>>>> bac244b (Implement messaging and player state commands)
=======
        moderatorCommands = new ModeratorCommands();
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)
=======
        utilityCommands = new UtilityCommands();
        uiCommands = new UICommands();
        jailCommands = new JailCommands();
        powerToolCommands = new PowerToolCommands();
        mailCommands = new MailCommands();
        // ItemCommands needs CommandBuildContext which is only available during register event
>>>>>>> e5d4bb8 (feat: Add UI command functionality for various crafting interfaces including workbench, anvil, and stonecutter)
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
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 734727c (feat: Enhance command registration and execution with improved error handling and user feedback)
        
        // Register ItemCommands with the CommandBuildContext from the event
        ItemCommands itemCommands = new ItemCommands(event.getBuildContext());
        itemCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered item commands");
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 734727c (feat: Enhance command registration and execution with improved error handling and user feedback)
    }
      /**
     * Registers all command categories with the dispatcher.
     * 
     * @param dispatcher The command dispatcher
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
     */    private void registerAllCommands(CommandDispatcher<CommandSourceStack> dispatcher) {// Register teleport commands
=======
     */    private void registerAllCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Get the server for CommandBuildContext
        MinecraftServer server = null;
        try {
            // Try to get the server from the event
            server = NeoEssentials.getServer();
        } catch (Exception e) {
            // Server might not be available yet
            NeoEssentials.LOGGER.error("Failed to get server for command registration", e);
        }        // Register teleport commands
>>>>>>> e5d4bb8 (feat: Add UI command functionality for various crafting interfaces including workbench, anvil, and stonecutter)
=======
     */    private void registerAllCommands(CommandDispatcher<CommandSourceStack> dispatcher) {// Register teleport commands
>>>>>>> 2a4d122 (feat: Refactor CommandManager to streamline command registration and add utility/UI command accessors)
        teleportCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered teleport commands");
        
        // Register home commands
        homeCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered home commands");
        
        // Register economy commands
        economyCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered economy commands");
        
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
<<<<<<< HEAD
<<<<<<< HEAD
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
        
<<<<<<< HEAD
<<<<<<< HEAD
        // Register powertool commands
        powerToolCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered powertool commands");
<<<<<<< HEAD
          // Register mail commands
        mailCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered mail commands");
        
        // Register admin panel commands
        adminPanelCommand.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered admin panel commands");
=======
        // Register power tool commands
        powerToolCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered power tool commands");
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
        // Register powertool commands
        powerToolCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered powertool commands");
>>>>>>> dc2fbaa (fix: Correct spelling of "powertool" in command registration logs)
=======
        
        // Register mail commands
        mailCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered mail commands");
>>>>>>> 59f36fa (feat: Add Mail functionality with commands and player notifications)
          // Note: ItemCommands require CommandBuildContext which is not available here
        // In a full implementation, you would need to get the CommandBuildContext properly
        
        // For now, we'll skip registering ItemCommands until we can find a proper way
        // to get the CommandBuildContext
        NeoEssentials.LOGGER.info("Skipping ItemCommands registration due to CommandBuildContext requirements");
=======
     */    private void registerAllCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register teleport commands
=======
     */    private void registerAllCommands(CommandDispatcher<CommandSourceStack> dispatcher) {        // Register teleport commands
>>>>>>> 0e64616 (chore: Update build number to 12 and timestamp in buildnumber.properties; enhance logging in command registration and warp management)
        teleportCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered teleport commands");
        
        // Register home commands
        homeCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered home commands");
        
        // Register economy commands
        economyCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered economy commands");
        
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
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        NeoEssentials.LOGGER.info("Registered time and weather commands");
<<<<<<< HEAD
>>>>>>> 0e64616 (chore: Update build number to 12 and timestamp in buildnumber.properties; enhance logging in command registration and warp management)
=======
        
        // Register inventory commands
        inventoryCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered inventory commands");
        
        // Register player commands
        playerCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered player commands");
        
        // Register message commands
        messageCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered message commands");
>>>>>>> bac244b (Implement messaging and player state commands)
=======
        
        // Register moderator commands
        moderatorCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered moderator commands");
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
          // Register moderator commands
        moderatorCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered moderator commands");
          // Register AFK commands
        afkCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered AFK commands");
<<<<<<< HEAD
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)
=======
        
        // Register utility commands
        utilityCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered utility commands");
        
        // Register UI commands
        uiCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered UI commands");
          // Note: ItemCommands require CommandBuildContext which is not available here
        // In a full implementation, you would need to get the CommandBuildContext properly
        
<<<<<<< HEAD
        // Create and register item commands (needs CommandBuildContext)
        CommandBuildContext buildContext = CommandBuildContext.simple(((CommandSourceStack)(Object)dispatcher).getServer().registryAccess(), 
                ((CommandSourceStack)(Object)dispatcher).getServer().getWorldData().getDataConfiguration());
        itemCommands = new ItemCommands(buildContext);
        itemCommands.register(dispatcher);
        NeoEssentials.LOGGER.info("Registered item commands");
>>>>>>> e5d4bb8 (feat: Add UI command functionality for various crafting interfaces including workbench, anvil, and stonecutter)
=======
        // For now, we'll skip registering ItemCommands until we can find a proper way
        // to get the CommandBuildContext
        NeoEssentials.LOGGER.info("Skipping ItemCommands registration due to CommandBuildContext requirements");
>>>>>>> 2a4d122 (feat: Refactor CommandManager to streamline command registration and add utility/UI command accessors)
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
     * Gets the economy commands instance
     * 
     * @return The economy commands
     */
    public EconomyCommands getEconomyCommands() {
        return economyCommands;
    }
      /**
     * Gets the user commands instance
     * 
     * @return The user commands
     */    public UserCommands getUserCommands() {
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
<<<<<<< HEAD
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
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
    public KitCommands getKitCommands() {
        return kitCommands;
    }
    
    /**
     * Gets the time and weather commands instance
     * 
     * @return The time and weather commands
     */
    public TimeAndWeatherCommands getTimeAndWeatherCommands() {
<<<<<<< HEAD
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
=======
        return timeAndWeatherCommands;
    }    /**
     * Checks if a player has permission for a specific command.
     * Integrates with LuckPerms or FTB Ranks if available.
     *
     * @param source The command source
     * @param permission The permission to check
     * @return True if the player has permission, false otherwise
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        // Delegate to PermissionUtil
        return com.zerog.neoessentials.utils.PermissionUtil.hasPermission(source, permission);
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
    }
    
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
