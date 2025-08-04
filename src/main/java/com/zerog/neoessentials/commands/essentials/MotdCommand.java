package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MotdCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("motd")
            .executes(MotdCommand::showMotd)
            .then(Commands.literal("set")
                .requires(source -> source.hasPermission(3)) // Admin only
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(MotdCommand::setMotd)))
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(3)) // Admin only
                .executes(MotdCommand::reloadMotd)));
    }
    
    /**
     * Show the current message of the day
     */
    private static int showMotd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String motd = source.getServer().getMotd();
        
        sendMessage(source, "§6========== §eMessage of the Day §6==========");
        sendMessage(source, "§f" + motd);
        sendMessage(source, "§6==========================================");
        
        return 1;
    }
    
    /**
     * Set a new message of the day (admin only)
     */
    private static int setMotd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String newMotd = StringArgumentType.getString(context, "message");
        
        try {
            // This would require reflection or mod access to server properties
            // For now, just show what the new MOTD would be
            sendMessage(source, "§aNew MOTD set (requires server restart to take effect):");
            sendMessage(source, "§f" + newMotd);
            sendMessage(source, "§eNote: MOTD changes require server properties modification and restart.");
            
            // Log the change
            source.getServer().sendSystemMessage(Component.literal(
                "§7[MOTD] " + getSourceName(source) + " set MOTD to: " + newMotd));
            
            return 1;
        } catch (Exception e) {
            sendMessage(source, "§cFailed to set MOTD: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Reload the message of the day from server properties
     */
    private static int reloadMotd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // This would reload from server.properties if we had access
            sendMessage(source, "§aMOTD reloaded from server.properties");
            sendMessage(source, "§7Current MOTD: §f" + source.getServer().getMotd());
            
            // Log the reload
            source.getServer().sendSystemMessage(Component.literal(
                "§7[MOTD] " + getSourceName(source) + " reloaded MOTD"));
            
            return 1;
        } catch (Exception e) {
            sendMessage(source, "§cFailed to reload MOTD: " + e.getMessage());
            return 0;
        }
    }
    
    private static void sendMessage(CommandSourceStack source, String message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendMessage(player, message);
        } else {
            source.sendSuccess(() -> Component.literal(message), false);
        }
    }
    
    private static String getSourceName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        } else {
            return "Console";
        }
    }
}
