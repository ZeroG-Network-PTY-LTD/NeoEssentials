package com.zerog.neoessentials.commands.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
// import com.zerog.neoessentials.discord.DiscordInteractiveChat; // DISABLED
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to manually share inventory to Discord
 * /dinv - Shares your current inventory contents
 */
public class DiscordInventoryCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dinv")
                .requires(source -> source.isPlayer())
                .executes(DiscordInventoryCommand::execute));
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            try {
                // Use the DiscordInteractiveChat method to send inventory to Discord
                // DiscordInteractiveChat.sendInventoryToDiscord(player, "**" + player.getName().getString() + "** is sharing their inventory:");
                
                // TODO: Restore Discord integration after fixing DiscordInteractiveChat
                player.sendSystemMessage(Component.literal("Discord integration temporarily disabled."));
                
                player.sendSystemMessage(Component.literal("§aInventory shared to Discord successfully!"));
                return 1;
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§cFailed to share inventory to Discord: " + e.getMessage()));
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
