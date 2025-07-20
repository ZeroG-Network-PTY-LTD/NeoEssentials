package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Spawn command implementation for NeoEssentials
 * Handles spawn teleportation and spawn setting
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SpawnCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn")
            .executes(SpawnCommand::teleportToSpawn)
        );
        
        dispatcher.register(Commands.literal("setspawn")
            .requires(source -> source.hasPermission(3)) // Op level 3
            .executes(SpawnCommand::setSpawn)
        );
    }
    
    private static int teleportToSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // TODO: Implement spawn teleportation logic
        player.sendSystemMessage(Component.literal("§6[NeoEssentials] §eTeleporting to spawn..."));
        
        return 1;
    }
    
    private static int setSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // TODO: Implement spawn setting logic
        player.sendSystemMessage(Component.literal("§6[NeoEssentials] §aSpawn location set!"));
        
        return 1;
    }
}
