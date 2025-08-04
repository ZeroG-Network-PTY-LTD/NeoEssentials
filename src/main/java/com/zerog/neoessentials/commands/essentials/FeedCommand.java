package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * Feed command implementation - /feed [player]
 * Restores a player's hunger and saturation to full
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class FeedCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /feed - Feed yourself
        dispatcher.register(Commands.literal("feed")
            .requires(source -> source.hasPermission(2))
            .executes(FeedCommand::feedSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(FeedCommand::feedOther)
            )
        );
    }
    
    /**
     * Feed the command executor
     */
    private static int feedSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        feedPlayer(player);
        
        context.getSource().sendSuccess(() -> Component.literal("§aYour hunger has been satisfied!"), false);
        return 1;
    }
    
    /**
     * Feed another player
     */
    private static int feedOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ServerPlayer executor = context.getSource().getPlayerOrException();
        
        feedPlayer(target);
        
        // Send confirmation to both players
        context.getSource().sendSuccess(() -> Component.literal("§aYou have fed " + target.getName().getString() + "!"), true);
        target.sendSystemMessage(Component.literal("§aYour hunger has been satisfied by " + executor.getName().getString() + "!"));
        
        return 1;
    }
    
    /**
     * Perform the feeding operation
     */
    private static void feedPlayer(ServerPlayer player) {
        // Restore full hunger
        player.getFoodData().setFoodLevel(20);
        
        // Restore full saturation
        player.getFoodData().setSaturation(20.0f);
        
        // Remove hunger effect
        player.removeEffect(MobEffects.HUNGER);
    }
}
