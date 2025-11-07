package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.moderation.FreezeManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Freeze commands: /freeze, /unfreeze, /freezeall, /unfreezeall, /freezelist
 */
public class FreezeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreezeCommand.class);
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_FROZEN_PLAYERS = (ctx, builder) -> {
        FreezeManager freezeManager = FreezeManager.getInstance();
        return SharedSuggestionProvider.suggest(
            freezeManager.getAllFrozenPlayers().stream()
                .map(freeze -> freeze.playerName)
                .collect(Collectors.toList()),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Enforce moderationEnabled config
        if (!com.zerog.neoessentials.config.ConfigManager.isModerationEnabled()) {
            return;
        }
        // /freeze <player> [reason]
        dispatcher.register(Commands.literal("freeze")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.freeze"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .executes(ctx -> executeFreeze(ctx, 
                    StringArgumentType.getString(ctx, "player"),
                    com.zerog.neoessentials.config.ConfigManager.getDefaultFreezeReason()))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeFreeze(ctx,
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "reason")))))
        );

        // /unfreeze <player>
        dispatcher.register(Commands.literal("unfreeze")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.unfreeze"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(SUGGEST_FROZEN_PLAYERS)
                .executes(ctx -> executeUnfreeze(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        
        // /freezeall [reason]
        dispatcher.register(Commands.literal("freezeall")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.freezeall"))
            .executes(ctx -> executeFreezeAll(ctx, com.zerog.neoessentials.config.ConfigManager.getDefaultFreezeReason()))
            .then(Commands.argument("reason", StringArgumentType.greedyString())
                .executes(ctx -> executeFreezeAll(ctx, StringArgumentType.getString(ctx, "reason"))))
        );
        
        // /unfreezeall
        dispatcher.register(Commands.literal("unfreezeall")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.unfreezeall"))
            .executes(ctx -> executeUnfreezeAll(ctx))
        );
        
        // /freezelist
        dispatcher.register(Commands.literal("freezelist")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.freezelist"))
            .executes(ctx -> executeFreezeList(ctx))
        );
    }
    
    private static int executeFreeze(CommandContext<CommandSourceStack> ctx, String playerName, String reason) {
        CommandSourceStack source = ctx.getSource();
        String frozenBy = getCommandSender(source);

        try {
            FreezeManager freezeManager = FreezeManager.getInstance();
            MinecraftServer server = source.getServer();

            // Enforce maxFreezeReason length from config
            int maxReasonLen = com.zerog.neoessentials.config.ConfigManager.getMaxFreezeReasonLength();
            if (reason != null && reason.length() > maxReasonLen) {
                String msg = MessageUtil.localize("neoessentials.moderation.reason_too_long", maxReasonLen);
                source.sendFailure(MessageUtil.error(msg));
                return 0;
            }

            // Find the target player
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }

            String targetName = targetPlayer.getName().getString();
            UUID targetId = targetPlayer.getUUID();

            // Check if already frozen
            if (freezeManager.isPlayerFrozen(targetId)) {
                String message = MessageUtil.localize("neoessentials.moderation.player_already_frozen", targetName);
                source.sendFailure(MessageUtil.error(message));
                return 0;
            }

            // Freeze the player
            boolean success = freezeManager.freezePlayer(targetName, targetId, reason, frozenBy);

            if (success) {
                String confirmMessage = MessageUtil.localize("neoessentials.moderation.freeze_success", targetName, reason);
                source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);

                // Notify the target player (config-driven message)
                String template = com.zerog.neoessentials.config.ConfigManager.getFreezeMessage();
                String targetMessage;
                if (template.equals("neoessentials.moderation.frozen_message")) {
                    targetMessage = MessageUtil.localize(template, reason, frozenBy);
                } else {
                    targetMessage = template.replace("{reason}", reason != null ? reason : "")
                                         .replace("{freezer}", frozenBy != null ? frozenBy : "");
                }
                targetPlayer.sendSystemMessage(MessageUtil.warning(targetMessage));

                // Broadcast freeze to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.freeze_broadcast", 
                    targetName, frozenBy, reason));

                LOGGER.info("Player {} frozen by {} for: {}", targetName, frozenBy, reason);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.freeze_failed", targetName);
                source.sendFailure(MessageUtil.error(message));
                return 0;
            }

        } catch (Exception e) {
            LOGGER.error("Error executing freeze command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the freeze command."));
            return 0;
        }
    }
    
    private static int executeUnfreeze(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();
        String unfrozenBy = getCommandSender(source);
        
        try {
            FreezeManager freezeManager = FreezeManager.getInstance();
            MinecraftServer server = source.getServer();
            
            // Resolve player UUID
            UUID playerId = null;
            String resolvedName = playerName;
            
            // First check if it's a frozen player
            for (FreezeManager.FreezeEntry freeze : freezeManager.getAllFrozenPlayers()) {
                if (freeze.playerName.equalsIgnoreCase(playerName)) {
                    playerId = freeze.playerId;
                    resolvedName = freeze.playerName;
                    break;
                }
            }
            
            // If not found in frozen list, try online players
            if (playerId == null) {
                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                if (player != null) {
                    playerId = player.getUUID();
                    resolvedName = player.getName().getString();
                }
            }
            
            if (playerId == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }
            
            // Check if actually frozen
            if (!freezeManager.isPlayerFrozen(playerId)) {
                String message = MessageUtil.localize("neoessentials.moderation.player_not_frozen", resolvedName);
                source.sendFailure(MessageUtil.error(message));
                return 0;
            }
            
            // Unfreeze the player
            boolean success = freezeManager.unfreezePlayer(playerId);
            
            if (success) {
                String confirmMessage = MessageUtil.localize("neoessentials.moderation.unfreeze_success", resolvedName);
                source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);
                
                // Notify the target player if online (config-driven message)
                ServerPlayer targetPlayer = server.getPlayerList().getPlayer(playerId);
                if (targetPlayer != null) {
                    String template = com.zerog.neoessentials.config.ConfigManager.getUnfreezeMessage();
                    String targetMessage;
                    if (template.equals("neoessentials.moderation.unfrozen_message")) {
                        targetMessage = MessageUtil.localize(template, unfrozenBy);
                    } else {
                        targetMessage = template.replace("{unfreezer}", unfrozenBy != null ? unfrozenBy : "Staff");
                    }
                    targetPlayer.sendSystemMessage(MessageUtil.success(targetMessage));
                }
                
                // Broadcast unfreeze to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unfreeze_broadcast", 
                    resolvedName, unfrozenBy));
                
                LOGGER.info("Player {} unfrozen by {}", resolvedName, unfrozenBy);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.unfreeze_failed", resolvedName);
                source.sendFailure(MessageUtil.error(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing unfreeze command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the unfreeze command."));
            return 0;
        }
    }
    
    private static int executeFreezeAll(CommandContext<CommandSourceStack> ctx, String reason) {
        CommandSourceStack source = ctx.getSource();
        String frozenBy = getCommandSender(source);

        try {
            FreezeManager freezeManager = FreezeManager.getInstance();
            MinecraftServer server = source.getServer();

            // Enforce maxFreezeReason length from config
            int maxReasonLen = com.zerog.neoessentials.config.ConfigManager.getMaxFreezeReasonLength();
            if (reason != null && reason.length() > maxReasonLen) {
                String msg = MessageUtil.localize("neoessentials.moderation.reason_too_long", maxReasonLen);
                source.sendFailure(MessageUtil.error(msg));
                return 0;
            }

            List<ServerPlayer> playersToFreeze = server.getPlayerList().getPlayers().stream()
                .filter(player -> {
                    // Don't freeze the command sender
                    if (source.getEntity() instanceof ServerPlayer commandSender) {
                        if (player.getUUID().equals(commandSender.getUUID())) {
                            return false;
                        }
                    }
                    // Don't freeze already frozen players
                    return !freezeManager.isPlayerFrozen(player.getUUID());
                })
                .collect(Collectors.toList());

            if (playersToFreeze.isEmpty()) {
                String message = MessageUtil.localize("neoessentials.moderation.freezeall_no_players");
                source.sendSuccess(() -> MessageUtil.warning(message), false);
                return 1;
            }

            int frozenCount = 0;
            for (ServerPlayer player : playersToFreeze) {
                boolean success = freezeManager.freezePlayer(
                    player.getName().getString(), 
                    player.getUUID(), 
                    reason, 
                    frozenBy
                );

                if (success) {
                    frozenCount++;

                    // Notify the frozen player
                    String targetMessage = MessageUtil.localize("neoessentials.moderation.freeze_notification", frozenBy, reason);
                    player.sendSystemMessage(MessageUtil.warning(targetMessage));
                }
            }

            String confirmMessage = MessageUtil.localize("neoessentials.moderation.freezeall_success", frozenCount, reason);
            source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);

            // Broadcast to staff
            broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.freezeall_broadcast", 
                frozenCount, frozenBy, reason));

            LOGGER.info("{} players frozen by {} for: {}", frozenCount, frozenBy, reason);
            return 1;

        } catch (Exception e) {
            LOGGER.error("Error executing freezeall command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the freezeall command."));
            return 0;
        }
    }
    
    private static int executeUnfreezeAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String unfrozenBy = getCommandSender(source);
        
        try {
            FreezeManager freezeManager = FreezeManager.getInstance();
            MinecraftServer server = source.getServer();
            
            List<FreezeManager.FreezeEntry> frozenPlayers = freezeManager.getAllFrozenPlayers();
            
            if (frozenPlayers.isEmpty()) {
                String message = MessageUtil.localize("neoessentials.moderation.unfreezeall_no_players");
                source.sendSuccess(() -> MessageUtil.warning(message), false);
                return 1;
            }
            
            int unfrozenCount = 0;
            for (FreezeManager.FreezeEntry freeze : frozenPlayers) {
                boolean success = freezeManager.unfreezePlayer(freeze.playerId);
                
                if (success) {
                    unfrozenCount++;
                    
                    // Notify the unfrozen player if online
                    ServerPlayer player = server.getPlayerList().getPlayer(freeze.playerId);
                    if (player != null) {
                        String targetMessage = MessageUtil.localize("neoessentials.moderation.unfreeze_notification", unfrozenBy);
                        player.sendSystemMessage(MessageUtil.success(targetMessage));
                    }
                }
            }
            
            String confirmMessage = MessageUtil.localize("neoessentials.moderation.unfreezeall_success", unfrozenCount);
            source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);
            
            // Broadcast to staff
            broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unfreezeall_broadcast", 
                unfrozenCount, unfrozenBy));
            
            LOGGER.info("{} players unfrozen by {}", unfrozenCount, unfrozenBy);
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing unfreezeall command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the unfreezeall command."));
            return 0;
        }
    }
    
    private static int executeFreezeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            FreezeManager freezeManager = FreezeManager.getInstance();
            List<FreezeManager.FreezeEntry> frozenPlayers = freezeManager.getAllFrozenPlayers();
            
            if (frozenPlayers.isEmpty()) {
                String message = MessageUtil.localize("neoessentials.moderation.freezelist_empty");
                source.sendSuccess(() -> MessageUtil.info(message), false);
                return 1;
            }
            
            String header = MessageUtil.localize("neoessentials.moderation.freezelist_header", frozenPlayers.size());
            source.sendSuccess(() -> MessageUtil.info(header), false);
            
            for (FreezeManager.FreezeEntry freeze : frozenPlayers) {
                String freezeInfo = MessageUtil.localize("neoessentials.moderation.freezelist_entry",
                    freeze.playerName, freeze.reason, freeze.frozenBy, freeze.getFormattedFreezeTime());
                source.sendSuccess(() -> MessageUtil.info(freezeInfo), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing freezelist command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the freezelist command."));
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