package com.zerog.neoessentials.commands.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.discord.DiscordInteractiveChat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to manually share ender chest to Discord
 * /dender - Shares your current ender chest contents
 */
public class DiscordEnderChestCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dender")
                .requires(source -> source.isPlayer())
                .executes(DiscordEnderChestCommand::execute));
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            try {
                // Use the DiscordInteractiveChat method to send ender chest to Discord
                DiscordInteractiveChat.sendEnderChestToDiscord(player, "**" + player.getName().getString() + "** is sharing their ender chest:");
                
                player.sendSystemMessage(Component.literal("§aEnder chest shared to Discord successfully!"));
                return 1;
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§cFailed to share ender chest to Discord: " + e.getMessage()));
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
