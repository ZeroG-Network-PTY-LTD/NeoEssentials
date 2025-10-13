package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.moderation.VanishManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vanish commands: /vanish, /unvanish, /vanishlist
 */
public class VanishCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanishCommand.class);
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_VANISHED_PLAYERS = (ctx, builder) -> {
        VanishManager vanishManager = VanishManager.getInstance();
        return SharedSuggestionProvider.suggest(
            vanishManager.getVanishedPlayers().stream()
                .map(uuid -> ctx.getSource().getServer().getPlayerList().getPlayer(uuid))
                .filter(player -> player != null)
                .map(player -> player.getName().getString())
                .collect(Collectors.toList()),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /vanish [player]
        dispatcher.register(Commands.literal("vanish")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.vanish"))
            .executes(ctx -> executeToggleVanish(ctx, null))
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    getPlayerUUID(source), "neoessentials.moderation.vanish.others"))
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .executes(ctx -> executeToggleVanish(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        
        // /unvanish [player]
        dispatcher.register(Commands.literal("unvanish")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.vanish"))
            .executes(ctx -> executeUnvanish(ctx, null))
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    getPlayerUUID(source), "neoessentials.moderation.vanish.others"))
                .suggests(SUGGEST_VANISHED_PLAYERS)
                .executes(ctx -> executeUnvanish(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        
        // /vanishlist
        dispatcher.register(Commands.literal("vanishlist")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.vanishlist"))
            .executes(ctx -> executeVanishList(ctx))
        );
    }
    
    private static int executeToggleVanish(CommandContext<CommandSourceStack> ctx, String targetPlayerName) {
        CommandSourceStack source = ctx.getSource();
        String vanishedBy = getCommandSender(source);
        
        try {
            VanishManager vanishManager = VanishManager.getInstance();
            MinecraftServer server = source.getServer();
            
            ServerPlayer targetPlayer;
            if (targetPlayerName == null) {
                // Self-vanish
                if (!(source.getEntity() instanceof ServerPlayer player)) {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.player_only_command"));
                    return 0;
                }
                targetPlayer = player;
            } else {
                // Vanish another player
                targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
                if (targetPlayer == null) {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", targetPlayerName));
                    return 0;
                }
            }
            
            String targetName = targetPlayer.getName().getString();
            UUID targetId = targetPlayer.getUUID();
            
            // Check if already vanished
            boolean isVanished = vanishManager.isPlayerVanished(targetId);
            
            if (isVanished) {
                // Unvanish the player
                boolean success = vanishManager.unvanishPlayer(targetId);
                
                if (success) {
                    if (targetPlayerName == null) {
                        // Self-unvanish
                        String message = MessageUtil.localize("neoessentials.moderation.vanish_disabled_self");
                        source.sendSuccess(() -> MessageUtil.success(message), false);
                    } else {
                        // Unvanish other player
                        String confirmMessage = MessageUtil.localize("neoessentials.moderation.vanish_disabled_other", targetName);
                        source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);
                        
                        String targetMessage = MessageUtil.localize("neoessentials.moderation.vanish_disabled_by", vanishedBy);
                        targetPlayer.sendSystemMessage(MessageUtil.info(targetMessage));
                    }
                    
                    // Broadcast to staff
                    broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.vanish_disabled_broadcast", 
                        targetName, vanishedBy));
                    
                    LOGGER.info("Player {} unvanished by {}", targetName, vanishedBy);
                    return 1;
                } else {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.vanish_failed", targetName));
                    return 0;
                }
                
            } else {
                // Vanish the player
                boolean success = vanishManager.vanishPlayer(targetId, targetName, vanishedBy, targetPlayerName == null);
                
                if (success) {
                    if (targetPlayerName == null) {
                        // Self-vanish
                        String message = MessageUtil.localize("neoessentials.moderation.vanish_enabled_self");
                        source.sendSuccess(() -> MessageUtil.success(message), false);
                    } else {
                        // Vanish other player
                        String confirmMessage = MessageUtil.localize("neoessentials.moderation.vanish_enabled_other", targetName);
                        source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);
                        
                        String targetMessage = MessageUtil.localize("neoessentials.moderation.vanish_enabled_by", vanishedBy);
                        targetPlayer.sendSystemMessage(MessageUtil.info(targetMessage));
                    }
                    
                    // Broadcast to staff
                    broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.vanish_enabled_broadcast", 
                        targetName, vanishedBy));
                    
                    LOGGER.info("Player {} vanished by {}", targetName, vanishedBy);
                    return 1;
                } else {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.vanish_failed", targetName));
                    return 0;
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing vanish command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the vanish command."));
            return 0;
        }
    }
    
    private static int executeUnvanish(CommandContext<CommandSourceStack> ctx, String targetPlayerName) {
        CommandSourceStack source = ctx.getSource();
        String vanishedBy = getCommandSender(source);
        
        try {
            VanishManager vanishManager = VanishManager.getInstance();
            MinecraftServer server = source.getServer();
            
            ServerPlayer targetPlayer;
            if (targetPlayerName == null) {
                // Self-unvanish
                if (!(source.getEntity() instanceof ServerPlayer player)) {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.player_only_command"));
                    return 0;
                }
                targetPlayer = player;
            } else {
                // Unvanish another player
                targetPlayer = server.getPlayerList().getPlayerByName(targetPlayerName);
                if (targetPlayer == null) {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", targetPlayerName));
                    return 0;
                }
            }
            
            String targetName = targetPlayer.getName().getString();
            UUID targetId = targetPlayer.getUUID();
            
            // Check if actually vanished
            if (!vanishManager.isPlayerVanished(targetId)) {
                String message = MessageUtil.localize("neoessentials.moderation.player_not_vanished", targetName);
                source.sendFailure(MessageUtil.error(message));
                return 0;
            }
            
            // Unvanish the player
            boolean success = vanishManager.unvanishPlayer(targetId);
            
            if (success) {
                if (targetPlayerName == null) {
                    // Self-unvanish
                    String message = MessageUtil.localize("neoessentials.moderation.vanish_disabled_self");
                    source.sendSuccess(() -> MessageUtil.success(message), false);
                } else {
                    // Unvanish other player
                    String confirmMessage = MessageUtil.localize("neoessentials.moderation.vanish_disabled_other", targetName);
                    source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);
                    
                    String targetMessage = MessageUtil.localize("neoessentials.moderation.vanish_disabled_by", vanishedBy);
                    targetPlayer.sendSystemMessage(MessageUtil.info(targetMessage));
                }
                
                // Broadcast to staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.vanish_disabled_broadcast", 
                    targetName, vanishedBy));
                
                LOGGER.info("Player {} unvanished by {}", targetName, vanishedBy);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.vanish_failed", targetName));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing unvanish command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the unvanish command."));
            return 0;
        }
    }
    
    private static int executeVanishList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            VanishManager vanishManager = VanishManager.getInstance();
            MinecraftServer server = source.getServer();
            Set<UUID> vanishedPlayers = vanishManager.getVanishedPlayers();
            
            if (vanishedPlayers.isEmpty()) {
                String message = MessageUtil.localize("neoessentials.moderation.vanishlist_empty");
                source.sendSuccess(() -> MessageUtil.info(message), false);
                return 1;
            }
            
            String header = MessageUtil.localize("neoessentials.moderation.vanishlist_header", vanishedPlayers.size());
            source.sendSuccess(() -> MessageUtil.info(header), false);
            
            for (UUID playerId : vanishedPlayers) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    String playerName = player.getName().getString();
                    String vanishInfo = MessageUtil.localize("neoessentials.moderation.vanishlist_entry", playerName);
                    source.sendSuccess(() -> MessageUtil.info(vanishInfo), false);
                } else {
                    // Player offline but still in vanish list
                    String offlineInfo = MessageUtil.localize("neoessentials.moderation.vanishlist_offline", playerId.toString());
                    source.sendSuccess(() -> MessageUtil.warning(offlineInfo), false);
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing vanishlist command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the vanishlist command."));
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