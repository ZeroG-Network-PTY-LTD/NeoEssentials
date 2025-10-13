package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

/**
 * Implements the /ping command - Shows player connection latency
 * Displays ping in milliseconds with connection quality indicators
 */
public class PingCommand {
    
    /**
     * Register the /ping command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("ping")) return;
        
        dispatcher.register(
            Commands.literal("ping")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /ping [player] - Show ping for another player
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.ping.others");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        showPingInfo(ctx.getSource(), target, permResult.getPlayer());
                        return 1;
                    })
                )
                // /ping - Show own ping
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.ping");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    showPingInfo(ctx.getSource(), player, player);
                    return 1;
                })
        );
    }
    
    /**
     * Display ping information for a player
     */
    private static void showPingInfo(CommandSourceStack source, ServerPlayer target, ServerPlayer requester) {
        int ping = target.connection.latency();
        String qualityDescription = getPingQuality(ping);
        
        if (target == requester) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ping.self", ping, qualityDescription), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ping.other", 
                target.getName().getString(), ping, qualityDescription), false);
        }
        
        // Additional information based on ping quality
        if (ping > 300) {
            source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.ping.high_warning"), false);
        } else if (ping > 150) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.ping.moderate_info"), false);
        } else if (ping < 50) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ping.excellent_info"), false);
        }
    }
    
    /**
     * Get connection quality description based on ping
     */
    private static String getPingQuality(int ping) {
        if (ping < 30) return "Excellent";
        if (ping < 60) return "Very Good";
        if (ping < 100) return "Good";
        if (ping < 150) return "Fair";
        if (ping < 250) return "Poor";
        if (ping < 400) return "Very Poor";
        return "Terrible";
    }
    

}