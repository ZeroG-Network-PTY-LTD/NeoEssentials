package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;


/**
 * Handles all user-related commands, including /heal, /feed, /fly, etc.
 */
public class UserCommands {
    
    /**
     * Registers all user-related commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /heal command
        dispatcher.register(
            Commands.literal("heal")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.heal"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Heal self
                    healPlayer(player);
                    LanguageUtil.sendMessage(player, "You have been healed.");
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.heal.others"))                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Heal another player
                            healPlayer(target);
                            LanguageUtil.sendMessage(source, "You have healed " + target.getScoreboardName() + ".");
                            LanguageUtil.sendMessage(target, "You have been healed by " + source.getScoreboardName() + ".");
                            
                            return 1;
                        })
                )
        );

        // Register /feed command
        dispatcher.register(
            Commands.literal("feed")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.feed"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Feed self
                    feedPlayer(player);
                    LanguageUtil.sendMessage(player, "Your hunger has been satisfied.");
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.feed.others"))                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Feed another player
                            feedPlayer(target);
                            LanguageUtil.sendMessage(source, "You have fed " + target.getScoreboardName() + ".");
                            LanguageUtil.sendMessage(target, "You have been fed by " + source.getScoreboardName() + ".");
                            
                            return 1;
                        })
                )
        );

        // Register /fly command
        dispatcher.register(
            Commands.literal("fly")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.fly"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Toggle flight for self
                    boolean newFlyingState = !player.getAbilities().mayfly;
                    toggleFlight(player, newFlyingState);
                    
                    LanguageUtil.sendMessage(player, "Flight " + (newFlyingState ? "enabled" : "disabled") + ".");
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.fly.others"))                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Toggle flight for another player
                            boolean newFlyingState = !target.getAbilities().mayfly;
                            toggleFlight(target, newFlyingState);
                            
                            LanguageUtil.sendMessage(source, target.getScoreboardName() + "'s flight " + (newFlyingState ? "enabled" : "disabled") + ".");
                            LanguageUtil.sendMessage(target, "Your flight has been " + (newFlyingState ? "enabled" : "disabled") + " by " + source.getScoreboardName() + ".");
                            
                            return 1;
                        })
                )
        );
        
        // Register gamemode commands
        registerGamemodeCommands(dispatcher);
        
        // Register time and weather commands
        registerWorldCommands(dispatcher);
        
        NeoEssentials.LOGGER.info("Registered user commands");
    }
    
    /**
     * Registers all gamemode-related commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    private void registerGamemodeCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /gmc command (creative mode)
        dispatcher.register(
            Commands.literal("gmc")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.gamemode.creative"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Set gamemode to creative
                    setGameMode(player, net.minecraft.world.level.GameType.CREATIVE);
                    LanguageUtil.sendMessage(player, "Your game mode has been set to Creative Mode.");
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.gamemode.creative.others"))                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Set gamemode to creative for another player
                            setGameMode(target, net.minecraft.world.level.GameType.CREATIVE);
                            LanguageUtil.sendMessage(source, "Set " + target.getScoreboardName() + "'s game mode to Creative Mode.");
                            LanguageUtil.sendMessage(target, "Your game mode has been set to Creative Mode by " + source.getScoreboardName() + ".");
                            
                            return 1;
                        })
                )
        );

        // Register /gms command (survival mode)
        dispatcher.register(
            Commands.literal("gms")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.gamemode.survival"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Set gamemode to survival
                    setGameMode(player, net.minecraft.world.level.GameType.SURVIVAL);
                    LanguageUtil.sendMessage(player, "Your game mode has been set to Survival Mode.");
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.gamemode.survival.others"))                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Set gamemode to survival for another player
                            setGameMode(target, net.minecraft.world.level.GameType.SURVIVAL);
                            LanguageUtil.sendMessage(source, "Set " + target.getScoreboardName() + "'s game mode to Survival Mode.");
                            LanguageUtil.sendMessage(target, "Your game mode has been set to Survival Mode by " + source.getScoreboardName() + ".");
                            
                            return 1;
                        })
                )
        );

        // Register /gmsp command (spectator mode)
        dispatcher.register(
            Commands.literal("gmsp")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.gamemode.spectator"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Set gamemode to spectator
                    setGameMode(player, net.minecraft.world.level.GameType.SPECTATOR);
                    LanguageUtil.sendMessage(player, "Your game mode has been set to Spectator Mode.");
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.gamemode.spectator.others"))                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Set gamemode to spectator for another player
                            setGameMode(target, net.minecraft.world.level.GameType.SPECTATOR);
                            LanguageUtil.sendMessage(source, "Set " + target.getScoreboardName() + "'s game mode to Spectator Mode.");
                            LanguageUtil.sendMessage(target, "Your game mode has been set to Spectator Mode by " + source.getScoreboardName() + ".");
                            
                            return 1;
                        })
                )
        );

        // Register /gma command (adventure mode)
        dispatcher.register(
            Commands.literal("gma")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.gamemode.adventure"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                      // Set gamemode to adventure
                    setGameMode(player, net.minecraft.world.level.GameType.ADVENTURE);
                    LanguageUtil.sendMessage(player, "Your game mode has been set to Adventure Mode.");
                    
                    return 1;
                })
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.gamemode.adventure.others"))
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                              // Set gamemode to adventure for another player
                            setGameMode(target, net.minecraft.world.level.GameType.ADVENTURE);
                            LanguageUtil.sendMessage(source, "Set " + target.getScoreboardName() + "'s game mode to Adventure Mode.");
                            LanguageUtil.sendMessage(target, "Your game mode has been set to Adventure Mode by " + source.getScoreboardName() + ".");
                            
                            return 1;
                        })
                )
        );
    }
    
    /**
     * Registers all world-related commands (time and weather).
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    private void registerWorldCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /time command
        dispatcher.register(
            Commands.literal("time")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.time"))
                .then(
                    Commands.literal("day")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set time to day
                            // TODO: Implement time logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
                .then(
                    Commands.literal("night")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set time to night
                            // TODO: Implement time logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
                .then(
                    Commands.literal("noon")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set time to noon
                            // TODO: Implement time logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
                .then(
                    Commands.literal("midnight")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set time to midnight
                            // TODO: Implement time logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
                .then(
                    Commands.literal("dawn")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set time to dawn
                            // TODO: Implement time logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
                .then(
                    Commands.literal("dusk")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set time to dusk
                            // TODO: Implement time logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
        );

        // Register /weather command
        dispatcher.register(
            Commands.literal("weather")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.weather"))
                .then(
                    Commands.literal("clear")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set weather to clear
                            // TODO: Implement weather logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
                .then(
                    Commands.literal("rain")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set weather to rain
                            // TODO: Implement weather logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
                .then(
                    Commands.literal("thunder")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            
                            // Set weather to thunder
                            // TODO: Implement weather logic
                            LanguageUtil.sendMessage(player, "Command not implemented yet");
                            
                            return 1;
                        })
                )
        );    }
    
    /**
     * Heals a player to full health and removes negative effects
     * 
     * @param player The player to heal
     */
    private void healPlayer(ServerPlayer player) {
        // Set health to max
        player.setHealth(player.getMaxHealth());
        
        // Remove fire
        player.clearFire();
        
        // Set food level to max
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        
        // Remove all negative effects
        player.removeAllEffects();
    }
      /**
     * Feeds a player, setting their hunger to full
     * 
     * @param player The player to feed
     */
    private void feedPlayer(ServerPlayer player) {
        // Set food level to max
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
    }
    
    /**
     * Sets a player's game mode
     * 
     * @param player The player to set game mode for
     * @param gameType The game type to set
     */    private void setGameMode(ServerPlayer player, net.minecraft.world.level.GameType gameType) {
        player.setGameMode(gameType);
    }
    
    /**
     * Toggles flight for a player
     * 
     * @param player The player to toggle flight for
     * @param enabled Whether to enable or disable flight
     */
    private void toggleFlight(ServerPlayer player, boolean enabled) {
        // Set flight ability
        player.getAbilities().mayfly = enabled;
        
        // If disabling flight, also disable flying
        if (!enabled && player.getAbilities().flying) {
            player.getAbilities().flying = false;
        }
        
        // Send abilities update to client
        player.onUpdateAbilities();
    }
}
