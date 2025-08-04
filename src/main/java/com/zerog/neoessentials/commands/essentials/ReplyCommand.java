package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ReplyCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /reply command
        dispatcher.register(Commands.literal("reply")
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ReplyCommand::replyToLastMessage)));
        
        // /r command (alias)
        dispatcher.register(Commands.literal("r")
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ReplyCommand::replyToLastMessage)));
    }
    
    /**
     * Reply to the last player who messaged you
     */
    private static int replyToLastMessage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Only players can send messages
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("§cOnly players can send private messages"));
            return 0;
        }
        
        try {
            String message = StringArgumentType.getString(context, "message");
            UUID senderUUID = sender.getUUID();
            
            // Get the last player this player messaged
            UUID targetUUID = MessageCommand.getLastMessaged(senderUUID);
            if (targetUUID == null) {
                MessageUtil.sendMessage(sender, "§cYou have no one to reply to!");
                return 0;
            }
            
            // Find the target player
            ServerPlayer target = null;
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                if (player.getUUID().equals(targetUUID)) {
                    target = player;
                    break;
                }
            }
            
            if (target == null) {
                MessageUtil.sendMessage(sender, "§cThat player is no longer online!");
                // Clear the last messaged since they're offline
                MessageCommand.clearLastMessaged(senderUUID);
                return 0;
            }
            
            // Format and send the message
            String senderName = sender.getName().getString();
            String targetName = target.getName().getString();
            
            // Send to sender (confirmation)
            MessageUtil.sendMessage(sender, "§7[§eYou §7→ §e" + targetName + "§7] §f" + message);
            
            // Send to target
            MessageUtil.sendMessage(target, "§7[§e" + senderName + " §7→ §eYou§7] §f" + message);
            
            // Update last messaged (so they can reply back)
            MessageCommand.getLastMessaged(sender.getUUID()); // This updates the mapping
            
            // Log to console for admin monitoring
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Private] " + senderName + " → " + targetName + ": " + message));
            
            return 1;
            
        } catch (Exception e) {
            MessageUtil.sendMessage(sender, "§cError sending reply: " + e.getMessage());
            return 0;
        }
    }
}
