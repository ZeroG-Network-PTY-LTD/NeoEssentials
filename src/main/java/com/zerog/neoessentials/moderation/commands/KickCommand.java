package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.util.InputValidator;

import java.util.UUID;

/**
 * Kick commands: /kick, /kickall
 */
public class KickCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(KickCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // /kick <player> [reason]
        dispatcher.register(Commands.literal("kick")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.kick").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .executes(ctx -> executeKick(ctx, StringArgumentType.getString(ctx, "player"), "Kicked by an operator"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeKick(ctx, 
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "reason")))))
        );
        
        // /kickall [reason]
        dispatcher.register(Commands.literal("kickall")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.kickall").hasPermission())
            .executes(ctx -> executeKickAll(ctx, "Server maintenance"))
            .then(Commands.argument("reason", StringArgumentType.greedyString())
                .executes(ctx -> executeKickAll(ctx, StringArgumentType.getString(ctx, "reason"))))
        );
    }
    
    private static int executeKick(CommandContext<CommandSourceStack> ctx, String playerName, String reason) {
        CommandSourceStack source = ctx.getSource();
        String kickedBy = getCommandSender(source);
        
        try {
            // Validate reason length and content
            InputValidator.ValidationResult reasonResult = InputValidator.validateReason(reason);
            if (!reasonResult.isValid()) {
                source.sendFailure(MessageUtil.error("Invalid reason: " + reasonResult.getErrorMessage()));
                return 0;
            }
            reason = (String) reasonResult.getValue();

            MinecraftServer server = source.getServer();

            // Find the player
            ServerPlayer targetPlayer = null;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(playerName)) {
                    targetPlayer = player;
                    break;
                }
            }

            if (targetPlayer == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }

            // Check if trying to kick self (console can kick anyone)
            if (source.getEntity() instanceof ServerPlayer sourcePlayer) {
                if (sourcePlayer.equals(targetPlayer)) {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.cannot_kick_self"));
                    return 0;
                }
            }

            String playerDisplayName = targetPlayer.getName().getString();


            // Kick the player using config-driven message
            String kickMessageTemplate = com.zerog.neoessentials.config.ConfigManager.getKickMessage();
            String kickMessage = kickMessageTemplate
                .replace("{reason}", reason)
                .replace("{kicker}", kickedBy);
            targetPlayer.connection.disconnect(Component.literal(kickMessage));

            String confirmMessage = MessageUtil.localize("neoessentials.moderation.kick_success", playerDisplayName, reason);
            source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);



            // Broadcast kick to all players if enabled
            String broadcastMsg = MessageUtil.localize("neoessentials.moderation.kick_broadcast", 
                playerDisplayName, kickedBy, reason);
            if (com.zerog.neoessentials.config.ConfigManager.isBroadcastKicksEnabled()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.sendSystemMessage(MessageUtil.info(broadcastMsg));
                }
            }
            // Notify staff if enabled (independent of broadcastKicks)
            if (com.zerog.neoessentials.config.ConfigManager.isNotifyStaffOnKickEnabled()) {
                broadcastToStaff(server, broadcastMsg);
            }

            if (com.zerog.neoessentials.config.ConfigManager.isLogKickActionsEnabled()) {
                LOGGER.info("Player {} kicked by {} for: {}", playerDisplayName, kickedBy, reason);
            }
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing kick command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the kick command."));
            return 0;
        }
    }
    
    private static int executeKickAll(CommandContext<CommandSourceStack> ctx, String reason) {
        CommandSourceStack source = ctx.getSource();
        String kickedBy = getCommandSender(source);
        
        try {
            MinecraftServer server = source.getServer();
            
            // Get all players except the command sender (if it's a player)
            var playersToKick = server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.equals(source.getEntity()))
                .toList();
            
            if (playersToKick.isEmpty()) {
                String message = MessageUtil.localize("neoessentials.moderation.kickall_no_players");
                source.sendSuccess(() -> MessageUtil.info(message), false);
                return 1;
            }
            

            // Kick all players using config-driven message
            String kickAllMessageTemplate = com.zerog.neoessentials.config.ConfigManager.getKickAllMessage();
            String kickAllMessage = kickAllMessageTemplate
                .replace("{reason}", reason)
                .replace("{kicker}", kickedBy);
            for (ServerPlayer player : playersToKick) {
                player.connection.disconnect(Component.literal(kickAllMessage));
            }
            
            String confirmMessage = MessageUtil.localize("neoessentials.moderation.kickall_success", playersToKick.size(), reason);
            source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);
            
            if (com.zerog.neoessentials.config.ConfigManager.isLogKickActionsEnabled()) {
                LOGGER.info("Kicked {} players by {} for: {}", playersToKick.size(), kickedBy, reason);
            }
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing kickall command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the kickall command."));
            return 0;
        }
    }
    
    private static void broadcastToStaff(MinecraftServer server, String message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    player.getUUID(), "neoessentials.moderation.notifications")) {
                player.sendSystemMessage(MessageUtil.info(message));
            }
        }
    }
    
    private static String getCommandSender(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        }
        return "Console";
    }
    
    private static UUID getPlayerUUID(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return null; // Console
    }
}