package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageCommand {
    
    // Store last messaged player for /reply command
    private static final Map<UUID, UUID> lastMessaged = new HashMap<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /msg command
        dispatcher.register(Commands.literal("msg")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(MessageCommand::sendMessage))));
        
        // /tell command (alias)
        dispatcher.register(Commands.literal("tell")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(MessageCommand::sendMessage))));
        
        // /w command (alias) 
        dispatcher.register(Commands.literal("w")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(MessageCommand::sendMessage))));
        
        // /whisper command (alias)
        dispatcher.register(Commands.literal("whisper")
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(MessageCommand::sendMessage))));
    }
    
    /**
     * Send a private message between players
     */
    private static int sendMessage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Only players can send messages
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("§cOnly players can send private messages"));
            return 0;
        }
        
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            String message = StringArgumentType.getString(context, "message");
            
            // Check if target is the sender
            if (target.getUUID().equals(sender.getUUID())) {
                MessageUtil.sendMessage(sender, "§cYou cannot send a message to yourself!");
                return 0;
            }
            
            // Check if target is vanished (basic check)
            // Note: This would require integration with vanish system for full functionality
            
            // Format and send the message
            String senderName = sender.getName().getString();
            String targetName = target.getName().getString();
            
            // Send to sender (confirmation)
            MessageUtil.sendMessage(sender, "§7[§eYou §7→ §e" + targetName + "§7] §f" + message);
            
            // Send to target
            MessageUtil.sendMessage(target, "§7[§e" + senderName + " §7→ §eYou§7] §f" + message);
            
            // Store for reply functionality
            lastMessaged.put(sender.getUUID(), target.getUUID());
            lastMessaged.put(target.getUUID(), sender.getUUID());
            
            // Log to console for admin monitoring
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Private] " + senderName + " → " + targetName + ": " + message));
            
            return 1;
            
        } catch (Exception e) {
            MessageUtil.sendMessage(sender, "§cError sending message: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get the last player that this player messaged (for reply functionality)
     */
    public static UUID getLastMessaged(UUID playerUUID) {
        return lastMessaged.get(playerUUID);
    }
    
    /**
     * Clear last messaged data for a player (called when they leave)
     */
    public static void clearLastMessaged(UUID playerUUID) {
        lastMessaged.remove(playerUUID);
        // Also clear any references to this player in others' last messaged
        lastMessaged.entrySet().removeIf(entry -> entry.getValue().equals(playerUUID));
    }
}
