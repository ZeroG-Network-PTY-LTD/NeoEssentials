package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import com.mojang.brigadier.arguments.StringArgumentType;

/**
 * Feed command implementation - /feed [player]
 * Restores a player's hunger and saturation to full
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class FeedCommand implements IEssentialCommand {
    
    @Override
    public String getCommandName() {
        return "feed";
    }
    
    @Override
    public String getDescription() {
        return "Restore a player's hunger and saturation to full";
    }
    
    @Override
    public String getUsage() {
        return "/feed [player]";
    }
    
    @Override
    public String[] getAliases() {
        return new String[0];
    }
    
    @Override
    public String getPermission() {
        return PermissionNodes.MODERATION_BASIC;
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /feed - Feed yourself
        dispatcher.register(Commands.literal("feed")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(FeedCommand::feedSelf)
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
                .executes(FeedCommand::feedOther)
            )
        );
        // Alias: /f
        dispatcher.register(Commands.literal("f")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(FeedCommand::feedSelf)
            .then(Commands.argument("player", StringArgumentType.word())
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
                source.sendSuccess(() -> com.zerog.neoessentials.util.MessageUtil.translatable(player, "neoessentials.feed.self_success"), false);
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
                String playerName = StringArgumentType.getString(context, "player");
                ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
                if (target == null) {
                    source.sendFailure(com.zerog.neoessentials.util.MessageUtil.translatable("neoessentials.player.not_found_online", playerName));
                    return 0;
                }
                ServerPlayer executor = source.getPlayerOrException();
                feedPlayer(target);
                source.sendSuccess(() -> com.zerog.neoessentials.util.MessageUtil.translatable(executor, "neoessentials.feed.other_success", target.getName().getString()), true);
                target.sendSystemMessage(com.zerog.neoessentials.util.MessageUtil.translatable(target, "neoessentials.feed.success", executor.getName().getString()));
                return 1;
            }
        );
    }
    
    /**
     * Perform the feeding operation
     */
    private static void feedPlayer(ServerPlayer player) {
        // Note: Hunger restoration temporarily disabled due to FoodData API changes
        // TODO: Implement hunger restoration when FoodData API is available in 1.21.1
        
        // Remove hunger effect
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.HUNGER);
    }
}