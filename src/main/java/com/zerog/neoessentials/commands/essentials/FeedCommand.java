package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
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
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(FeedCommand::feedSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
                .executes(FeedCommand::feedOther)
            )
        );
    }
    
    /**
     * Feed the command executor
     */
    private static int feedSelf(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "feed self",
            "neoessentials.feed", 
            (source) -> {
                ServerPlayer player = source.getPlayerOrException();
                feedPlayer(player);
                
                source.sendSuccess(() -> Component.literal("§a🍖 Your hunger has been satisfied! You feel full and energized."), false);
                return 1;
            }
        );
    }
    
    /**
     * Feed another player
     */
    private static int feedOther(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "feed other",
            "neoessentials.feed.others", 
            (source) -> {
                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                ServerPlayer executor = source.getPlayerOrException();
                
                feedPlayer(target);
                
                // Send confirmation to both players
                source.sendSuccess(() -> Component.literal("§a🍖 You have fed " + target.getName().getString() + "! They are now fully satisfied."), true);
                target.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "command.feed.success", executor.getName().getString())));
                
                return 1;
            }
        );
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
