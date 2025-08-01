package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.KitManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Kit command implementation
 * Handles /kit, /kits commands
 */
public class KitCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /kit <name> - Give a kit to player
        dispatcher.register(Commands.literal("kit")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> giveKit(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /kits - List available kits
        dispatcher.register(Commands.literal("kits")
            .executes(KitCommands::listKits)
        );
    }
    
    private static int giveKit(CommandContext<CommandSourceStack> context, String kitName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        KitManager kitManager = KitManager.getInstance();
        
        boolean success = kitManager.giveKit(player, kitName);
        return success ? 1 : 0;
    }
    
    private static int listKits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        KitManager kitManager = KitManager.getInstance();
        
        // For now, use getAvailableKits and format the output
        var kits = kitManager.getAvailableKits(player);
        if (kits.isEmpty()) {
            return 0;
        }
        
        // Send kit list to player (placeholder implementation)
        return 1;
    }
}
