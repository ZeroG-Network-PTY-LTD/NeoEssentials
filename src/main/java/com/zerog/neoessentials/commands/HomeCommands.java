package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.HomeManager;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.TeleportUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
<<<<<<< HEAD
<<<<<<< HEAD
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
<<<<<<< HEAD
=======
=======
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> d3089e1 (feat: Update migration tasks and documentation for Home and Warp systems; enhance MessageUtil for improved command interactions)

import java.util.Map;

/**
 * Handles all home-related commands, including /home, /sethome, /delhome, and /homes.
 */
public class HomeCommands {
    
    /**
     * Registers all home-related commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /home command
        dispatcher.register(
            Commands.literal("home")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.home"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Teleport to default home
                    return teleportToHome(player, "home");
                })
                .then(
                    Commands.argument("name", StringArgumentType.word())                        .suggests(TabCompletionUtil.HOME_SUGGESTIONS)
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String homeName = StringArgumentType.getString(context, "name");
                            
                            // Teleport to named home
                            return teleportToHome(player, homeName);
                        })
                )
        );

        // Register /sethome command
        dispatcher.register(
            Commands.literal("sethome")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.sethome"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Set default home
                    return setHome(player, "home");
                })
                .then(
                    Commands.argument("name", StringArgumentType.word())                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String homeName = StringArgumentType.getString(context, "name");
                            
                            // Set named home
                            return setHome(player, homeName);
                        })
                )
        );

        // Register /delhome command
        dispatcher.register(
            Commands.literal("delhome")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.delhome"))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(TabCompletionUtil.HOME_SUGGESTIONS)
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String homeName = StringArgumentType.getString(context, "name");
                            
                            // Delete named home
                            return deleteHome(player, homeName);
                        })
                )
        );

        // Register /homes command
        dispatcher.register(
            Commands.literal("homes")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.homes"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // List homes
                    return listHomes(player);
                })
        );
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
        // Register /homehelp command
        dispatcher.register(
            Commands.literal("homehelp")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.homehelp"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Show home command help
                    return showHomeHelp(player);
                })
        );
        
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
        NeoEssentials.LOGGER.info("Registered home commands");
    }
    
    /**
     * Teleports a player to one of their homes
     * 
     * @param player The player
     * @param homeName The name of the home
     * @return Command result code
     */
    private int teleportToHome(ServerPlayer player, String homeName) {
        // Get the home manager
        HomeManager homeManager = NeoEssentials.getInstance().getDataManager().getHomeManager();
          // Get the home location
        HomeManager.HomeLocation home = homeManager.getHome(player.getUUID(), homeName.toLowerCase());
        
        if (home == null) {
            LanguageUtil.sendErrorMessage(player, "neoessentials.home.not_found", homeName);
            return 0;
        }
        
        // Get the destination level
        ServerLevel level = home.getLevel(player.getServer());
        if (level == null) {
            LanguageUtil.sendErrorMessage(player, "neoessentials.home.dimension_not_found", homeName);
            return 0;
        }
        
        // Teleport the player
        boolean success = TeleportUtil.teleportPlayer(
                player,
                level,
                new net.minecraft.world.phys.Vec3(home.getX(), home.getY(), home.getZ()),
                true
        );
        
        if (success) {
            MessageUtil.sendSuccessMessage(player, "Teleported to home '" + homeName + "'");
            return 1;
        } else {
            MessageUtil.sendErrorMessage(player, "Failed to teleport to home '" + homeName + "'");
            return 0;
        }
    }
    
    /**
     * Sets a home for a player
     * 
     * @param player The player
     * @param homeName The name of the home
     * @return Command result code
     */
    private int setHome(ServerPlayer player, String homeName) {
        // Get the home manager
        HomeManager homeManager = NeoEssentials.getInstance().getDataManager().getHomeManager();
        
        // Check if the player already has this home
        boolean homeExists = homeManager.getHome(player.getUUID(), homeName.toLowerCase()) != null;
        
        // Set the home
        boolean success = homeManager.setHome(player, homeName);
        
        if (success) {
            if (homeExists) {
                MessageUtil.sendSuccessMessage(player, "Updated home '" + homeName + "'");
            } else {
                MessageUtil.sendSuccessMessage(player, "Set home '" + homeName + "'");
            }
            return 1;
        } else {
            MessageUtil.sendErrorMessage(player, "You have reached your home limit");
            return 0;
        }
    }
    
    /**
     * Deletes a home for a player
     * 
     * @param player The player
     * @param homeName The name of the home
     * @return Command result code
     */
    private int deleteHome(ServerPlayer player, String homeName) {
        // Get the home manager
        HomeManager homeManager = NeoEssentials.getInstance().getDataManager().getHomeManager();
        
        // Delete the home
        boolean success = homeManager.deleteHome(player.getUUID(), homeName.toLowerCase());
        
        if (success) {
            MessageUtil.sendSuccessMessage(player, "Deleted home '" + homeName + "'");
            return 1;
        } else {
            MessageUtil.sendErrorMessage(player, "Home '" + homeName + "' not found");
            return 0;
        }
    }
    
    /**
     * Lists all homes for a player
     * 
     * @param player The player
     * @return Command result code
<<<<<<< HEAD
<<<<<<< HEAD
     */    private int listHomes(ServerPlayer player) {
=======
     */
    private int listHomes(ServerPlayer player) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
     */    private int listHomes(ServerPlayer player) {
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
        // Get the home manager
        HomeManager homeManager = NeoEssentials.getInstance().getDataManager().getHomeManager();
          // Get all homes
        Map<String, HomeManager.HomeLocation> homes = homeManager.getHomes(player.getUUID());
        
        if (homes.isEmpty()) {
<<<<<<< HEAD
<<<<<<< HEAD
            MessageUtil.sendMessage(player, "§cYou have no homes set");
            return 0;
        } else {
            // Add a header
            MessageUtil.sendInfo(player, Component.literal("§2§l====== §r§6Your Homes §2§l======"));
            
            // Create a clickable list of homes
            MutableComponent message = Component.literal("§2Your homes: ");
            
            boolean first = true;
            for (Map.Entry<String, HomeManager.HomeLocation> entry : homes.entrySet()) {
                String homeName = entry.getKey();
                HomeManager.HomeLocation location = entry.getValue();
                
                if (!first) {
                    message.append(Component.literal("§7, "));
                }
                
                // Create clickable home name with hover info
                MutableComponent homeComponent = Component.literal("§b" + homeName);
                
                // Add hover text
                MutableComponent hoverText = Component.literal("§eClick to teleport to §b" + homeName);
                hoverText.append(Component.literal("\n§7Dimension: §f" + location.getDimension()));
                hoverText.append(Component.literal("\n§7Location: §f" + 
                    (int)location.getX() + ", " + 
                    (int)location.getY() + ", " + 
                    (int)location.getZ()));
                
                homeComponent = MessageUtil.addHoverText(homeComponent, hoverText);
                
                // Add click event to teleport to the home
                homeComponent = MessageUtil.makeClickableCommand(homeComponent, "/home " + homeName);
                
                message.append(homeComponent);
                first = false;
            }
            
            MessageUtil.sendInfo(player, message);
            
            // Add clickable help button
            MutableComponent helpMessage = Component.literal("§7Type ");
            MutableComponent helpButton = Component.literal("§e/homehelp");
            helpButton = MessageUtil.makeClickableCommand(helpButton, "/homehelp");
            helpButton = MessageUtil.addHoverText(helpButton, Component.literal("§7Click to view home command help"));
            helpMessage.append(helpButton);
            helpMessage.append(Component.literal(" §7for more information."));
            
            MessageUtil.sendInfo(player, helpMessage);
=======
            MessageUtil.sendMessage(player, "You have no homes set");
        } else {
            MessageUtil.sendMessage(player, "Your homes: " + String.join(", ", homes.keySet()));
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            MessageUtil.sendMessage(player, "§cYou have no homes set");
            return 0;
        } else {
            // Add a header
            MessageUtil.sendInfo(player, Component.literal("§2§l====== §r§6Your Homes §2§l======"));
            
            // Create a clickable list of homes
            MutableComponent message = Component.literal("§2Your homes: ");
            
            boolean first = true;
            for (Map.Entry<String, HomeManager.HomeLocation> entry : homes.entrySet()) {
                String homeName = entry.getKey();
                HomeManager.HomeLocation location = entry.getValue();
                
                if (!first) {
                    message.append(Component.literal("§7, "));
                }
                
                // Create clickable home name with hover info
                MutableComponent homeComponent = Component.literal("§b" + homeName);
                
                // Add hover text
                MutableComponent hoverText = Component.literal("§eClick to teleport to §b" + homeName);
                hoverText.append(Component.literal("\n§7Dimension: §f" + location.getDimension()));
                hoverText.append(Component.literal("\n§7Location: §f" + 
                    (int)location.getX() + ", " + 
                    (int)location.getY() + ", " + 
                    (int)location.getZ()));
                
                homeComponent = MessageUtil.addHoverText(homeComponent, hoverText);
                
                // Add click event to teleport to the home
                homeComponent = MessageUtil.makeClickableCommand(homeComponent, "/home " + homeName);
                
                message.append(homeComponent);
                first = false;
            }
            
            MessageUtil.sendInfo(player, message);
            
            // Add clickable help button
            MutableComponent helpMessage = Component.literal("§7Type ");
            MutableComponent helpButton = Component.literal("§e/homehelp");
            helpButton = MessageUtil.makeClickableCommand(helpButton, "/homehelp");
            helpButton = MessageUtil.addHoverText(helpButton, Component.literal("§7Click to view home command help"));
            helpMessage.append(helpButton);
            helpMessage.append(Component.literal(" §7for more information."));
            
            MessageUtil.sendInfo(player, helpMessage);
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
        }
        
        return 1;
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
    
    /**
     * Shows help for home commands
     * 
     * @param player The player to show help to
     * @return Command result code
     */
    private int showHomeHelp(ServerPlayer player) {
        // Header
        MessageUtil.sendInfo(player, Component.literal("§2§l====== §r§6Home Commands §2§l======"));
        
        // Commands list with clickable examples
        MutableComponent homeCmd = Component.literal("§b/home [name]");
        homeCmd = MessageUtil.addHoverText(homeCmd, Component.literal("§7Teleport to your home\n§7If no name is specified, teleports to your default home"));
        MessageUtil.sendInfo(player, homeCmd.append(Component.literal(" §7- Teleport to your home")));
        
        MutableComponent setHomeCmd = Component.literal("§b/sethome [name]");
        setHomeCmd = MessageUtil.addHoverText(setHomeCmd, Component.literal("§7Set a home at your current location\n§7If no name is specified, sets your default home"));
        MessageUtil.sendInfo(player, setHomeCmd.append(Component.literal(" §7- Set a home at your current location")));
        
        MutableComponent delHomeCmd = Component.literal("§b/delhome <name>");
        delHomeCmd = MessageUtil.addHoverText(delHomeCmd, Component.literal("§7Delete one of your homes"));
        MessageUtil.sendInfo(player, delHomeCmd.append(Component.literal(" §7- Delete a home")));
        
        MutableComponent homesCmd = Component.literal("§b/homes");
        homesCmd = MessageUtil.makeClickableCommand(homesCmd, "/homes");
        homesCmd = MessageUtil.addHoverText(homesCmd, Component.literal("§7List all of your homes\n§eClick to execute"));
        MessageUtil.sendInfo(player, homesCmd.append(Component.literal(" §7- List all of your homes")));
        
        return 1;
    }
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
}
