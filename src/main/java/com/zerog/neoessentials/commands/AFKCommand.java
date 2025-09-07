package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.MessageUtil;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AFK Command - Manages away-from-keyboard status
 * 
 * Commands:
 * - /afk - Toggle own AFK status
 * - /afk <player> - Toggle another player's AFK status (admin)
 * - /afk <message> - Set AFK with custom message
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class AFKCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AFKCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.AFK))
            
            // Basic /afk command
            .executes(AFKCommand::executeToggleSelf)
            
            // /afk <player> - Admin command
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.AFK_OTHERS))
                .executes(AFKCommand::executeToggleOther)
            )
        );
    }
    
    /**
     * Toggle own AFK status
     */
    private static int executeToggleSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return toggleAFK(player, player);
        } catch (Exception e) {
            LOGGER.error("Error executing AFK command for self", e);
            context.getSource().sendFailure(Component.literal("An error occurred while toggling AFK status."));
            return 0;
        }
    }
    
    /**
     * Toggle another player's AFK status (admin)
     */
    private static int executeToggleOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer admin = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return toggleAFK(admin, target);
        } catch (Exception e) {
            LOGGER.error("Error executing AFK command for other player", e);
            context.getSource().sendFailure(Component.literal("An error occurred while toggling AFK status."));
            return 0;
        }
    }
    
    /**
     * Toggle AFK status for a player
     */
    private static int toggleAFK(ServerPlayer executor, ServerPlayer target) {
        PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
        PlayerDataManager.PlayerData playerData = playerDataManager.getPlayerData(target.getUUID());
        
        boolean wasAFK = playerData.afk;
        playerData.afk = !wasAFK;
        
        // Update AFK time
        if (!wasAFK) {
            playerData.afkTime = System.currentTimeMillis();
        } else {
            playerData.afkTime = 0;
        }
        
        // Save the data
        playerDataManager.savePlayerData(target.getUUID());
        
        // Send messages
        if (!wasAFK) {
            // Going AFK
            if (executor.equals(target)) {
                MessageUtil.sendMessage(executor, "§aYou are now AFK.");
            } else {
                MessageUtil.sendMessage(executor, "§aSet " + target.getName().getString() + " as AFK.");
            }
            
            // Broadcast to server
            Component broadcastMessage = Component.literal("§7" + target.getName().getString() + " is now AFK");
            target.getServer().getPlayerList().broadcastSystemMessage(broadcastMessage, false);
        } else {
            // Coming back
            if (executor.equals(target)) {
                MessageUtil.sendMessage(executor, "§aWelcome back! You are no longer AFK.");
            } else {
                MessageUtil.sendMessage(executor, "§aRemoved AFK status from " + target.getName().getString() + ".");
            }
            
            // Broadcast to server
            Component broadcastMessage = Component.literal("§7" + target.getName().getString() + " is no longer AFK");
            target.getServer().getPlayerList().broadcastSystemMessage(broadcastMessage, false);
        }
        
        LOGGER.debug("Player {} AFK status changed to: {}", target.getName().getString(), !wasAFK);
        return 1;
    }
}
