package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.HomeManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Home command implementation
 * Handles /home, /sethome, /delhome commands with proper permission checking
 */
public class HomeCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /home [name] - Teleport to home
        dispatcher.register(Commands.literal("home")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.HOME))
            .executes(context -> teleportHome(context, "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> teleportHome(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /sethome [name] - Set a home
        dispatcher.register(Commands.literal("sethome")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.HOME_SET))
            .executes(context -> setHome(context, "home"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> setHome(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /delhome <name> - Delete a home
        dispatcher.register(Commands.literal("delhome")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.HOME_DELETE))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> deleteHome(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /homes - List all homes
        dispatcher.register(Commands.literal("homes")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.HOME_LIST))
            .executes(HomeCommands::listHomes)
        );
    }
    
    private static int teleportHome(CommandContext<CommandSourceStack> context, String homeName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.HOME)) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to use homes!");
            return 0;
        }
        
        HomeManager homeManager = HomeManager.getInstance();
        boolean success = homeManager.teleportToHome(player, homeName);
        return success ? 1 : 0;
    }
    
    private static int setHome(CommandContext<CommandSourceStack> context, String homeName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.HOME_SET)) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to set homes!");
            return 0;
        }
        
        HomeManager homeManager = HomeManager.getInstance();
        boolean success = homeManager.setHome(player, homeName);
        return success ? 1 : 0;
    }
    
    private static int deleteHome(CommandContext<CommandSourceStack> context, String homeName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.HOME_DELETE)) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to delete homes!");
            return 0;
        }
        
        HomeManager homeManager = HomeManager.getInstance();
        boolean success = homeManager.deleteHome(player, homeName);
        return success ? 1 : 0;
    }
    
    private static int listHomes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.HOME_LIST)) {
            MessageUtil.sendMessage(player, "&cYou don't have permission to list homes!");
            return 0;
        }
        
        HomeManager homeManager = HomeManager.getInstance();
        boolean success = homeManager.listHomes(player);
        return success ? 1 : 0;
    }
}
