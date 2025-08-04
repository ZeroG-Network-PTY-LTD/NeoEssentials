package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.managers.HomeManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Home command implementation for NeoEssentials
 * Allows players to set and teleport to home locations
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class HomeCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
            .executes(HomeCommand::teleportToDefaultHome)
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(HomeCommand::teleportToNamedHome))
        );
        
        dispatcher.register(Commands.literal("sethome")
            .executes(HomeCommand::setDefaultHome)
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(HomeCommand::setNamedHome))
        );
        
        dispatcher.register(Commands.literal("delhome")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(HomeCommand::deleteHome))
        );
        
        dispatcher.register(Commands.literal("homes")
            .executes(HomeCommand::listHomes)
        );
    }
    
    private static int teleportToDefaultHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        HomeManager homeManager = HomeManager.getInstance();
        homeManager.teleportToHome(player, "home");
        
        return 1;
    }
    
    private static int teleportToNamedHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String homeName = StringArgumentType.getString(context, "name");
        
        HomeManager homeManager = HomeManager.getInstance();
        homeManager.teleportToHome(player, homeName);
        
        return 1;
    }
    
    private static int setDefaultHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        HomeManager homeManager = HomeManager.getInstance();
        homeManager.setHome(player, "home");
        
        return 1;
    }
    
    private static int setNamedHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String homeName = StringArgumentType.getString(context, "name");
        
        HomeManager homeManager = HomeManager.getInstance();
        homeManager.setHome(player, homeName);
        
        return 1;
    }
    
    private static int deleteHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String homeName = StringArgumentType.getString(context, "name");
        
        HomeManager homeManager = HomeManager.getInstance();
        homeManager.deleteHome(player, homeName);
        
        return 1;
    }
    
    private static int listHomes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        HomeManager homeManager = HomeManager.getInstance();
        homeManager.listHomes(player);
        
        return 1;
    }
}
