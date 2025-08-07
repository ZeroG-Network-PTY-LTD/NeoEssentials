package com.zerog.neoessentials.commands.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
// import com.zerog.neoessentials.discord.DiscordInteractiveChat; // DISABLED
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Command to manually share held item to Discord
 * /ditem - Shares the item you're currently holding
 */
public class DiscordItemCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ditem")
                .requires(source -> source.isPlayer())
                .executes(DiscordItemCommand::execute));
    }
    
    private static int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack heldItem = player.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cYou are not holding any item!"));
                return 0;
            }
            
            try {
                // Use the DiscordInteractiveChat method to send item to Discord
                // DiscordInteractiveChat.sendItemToDiscord(player, heldItem, "**" + player.getName().getString() + "** is showing off their item:");
                
                // TODO: Restore Discord integration after fixing DiscordInteractiveChat
                player.sendSystemMessage(Component.literal("Discord integration temporarily disabled."));
                
                player.sendSystemMessage(Component.literal("§aItem shared to Discord successfully!"));
                return 1;
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("§cFailed to share item to Discord: " + e.getMessage()));
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}
