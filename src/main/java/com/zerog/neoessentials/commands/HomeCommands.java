package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.HomeManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Home command implementation
 * Handles /home, /sethome, /delhome commands
 */
public class HomeCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /home [name] - Teleport to home
        dispatcher.register(Commands.literal("home")
            .executes(context -> teleportHome(context, "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> teleportHome(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /sethome [name] - Set a home
        dispatcher.register(Commands.literal("sethome")
            .executes(context -> setHome(context, "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> setHome(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /delhome <name> - Delete a home
        dispatcher.register(Commands.literal("delhome")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> deleteHome(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /homes - List all homes
        dispatcher.register(Commands.literal("homes")
            .executes(HomeCommands::listHomes)
        );
    }
    
    private static int teleportHome(CommandContext<CommandSourceStack> context, String homeName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HomeManager homeManager = HomeManager.getInstance();
        
        boolean success = homeManager.teleportToHome(player, homeName);
        return success ? 1 : 0;
    }
    
    private static int setHome(CommandContext<CommandSourceStack> context, String homeName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HomeManager homeManager = HomeManager.getInstance();
        
        boolean success = homeManager.setHome(player, homeName);
        return success ? 1 : 0;
    }
    
    private static int deleteHome(CommandContext<CommandSourceStack> context, String homeName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HomeManager homeManager = HomeManager.getInstance();
        
        boolean success = homeManager.deleteHome(player, homeName);
        return success ? 1 : 0;
    }
    
    private static int listHomes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HomeManager homeManager = HomeManager.getInstance();
        
        boolean success = homeManager.listHomes(player);
        return success ? 1 : 0;
    }
}
