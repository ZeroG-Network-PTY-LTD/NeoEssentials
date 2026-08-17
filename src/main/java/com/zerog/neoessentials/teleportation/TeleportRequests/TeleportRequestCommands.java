package com.zerog.neoessentials.teleportation.TeleportRequests;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands for teleportation requests: /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel
 */
public class TeleportRequestCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportRequestCommands.class);
    
    // Permission nodes for teleport requests
    private static final String PERMISSION_TPA = "neoessentials.teleport.request.tpa";
    private static final String PERMISSION_TPAHERE = "neoessentials.teleport.request.tpahere";
    private static final String PERMISSION_ACCEPT = "neoessentials.teleport.request.accept";
    private static final String PERMISSION_DENY = "neoessentials.teleport.request.deny";
    private static final String PERMISSION_CANCEL = "neoessentials.teleport.request.cancel";
    
    // Suggestion provider for online players
    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            context.getSource().getServer().getPlayerList().getPlayers().stream()
                .map(player -> player.getName().getString()),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        // Only register if teleportation module is enabled
        if (config.isTeleportationEnabled()) {
            // Register individual teleport request commands based on their command settings
            if (config.isCommandEnabled("tpa")) {
                // /tpa <player> - Request to teleport to another player
                dispatcher.register(
                    Commands.literal("tpa")
                        .requires(source -> {
                            if (source.getEntity() instanceof ServerPlayer player) {
                                boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TPA);
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "[TPA] Checking permission {} for {}: {}", PERMISSION_TPA, player.getName().getString(), hasPerm);
                                return hasPerm;
                            }
                            return false; // Console can't use teleport requests
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                            .suggests(ONLINE_PLAYERS)
                            .executes(context -> executeTpa(context))
                        )
                );
            }
            
            if (config.isCommandEnabled("tpahere")) {
                // /tpahere <player> - Request another player to teleport to you
                dispatcher.register(
                    Commands.literal("tpahere")
                        .requires(source -> {
                            if (source.getEntity() instanceof ServerPlayer player) {
                                boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TPAHERE);
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "[TPAHERE] Checking permission {} for {}: {}", PERMISSION_TPAHERE, player.getName().getString(), hasPerm);
                                return hasPerm;
                            }
                            return false; // Console can't use teleport requests
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                            .suggests(ONLINE_PLAYERS)
                            .executes(context -> executeTpaHere(context))
                        )
                );
            }
            
            if (config.isCommandEnabled("tpaccept")) {
                // /tpaccept - Accept a pending teleport request
                dispatcher.register(
                    Commands.literal("tpaccept")
                        .requires(source -> {
                            if (source.getEntity() instanceof ServerPlayer player) {
                                boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_ACCEPT);
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "[TPACCEPT] Checking permission {} for {}: {}", PERMISSION_ACCEPT, player.getName().getString(), hasPerm);
                                return hasPerm;
                            }
                            return false; // Console can't use teleport requests
                        })
                        .executes(context -> executeTpAccept(context))
                );
            }
            
            if (config.isCommandEnabled("tpdeny")) {
                // /tpdeny - Deny a pending teleport request
                dispatcher.register(
                    Commands.literal("tpdeny")
                        .requires(source -> {
                            if (source.getEntity() instanceof ServerPlayer player) {
                                boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_DENY);
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "[TPDENY] Checking permission {} for {}: {}", PERMISSION_DENY, player.getName().getString(), hasPerm);
                                return hasPerm;
                            }
                            return false; // Console can't use teleport requests
                        })
                        .executes(context -> executeTpDeny(context))
                );
            }
            
            if (config.isCommandEnabled("tpcancel")) {
                // /tpcancel - Cancel your sent teleport request
                dispatcher.register(
                    Commands.literal("tpcancel")
                        .requires(source -> {
                            if (source.getEntity() instanceof ServerPlayer player) {
                                boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_CANCEL);
                                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "[TPCANCEL] Checking permission {} for {}: {}", PERMISSION_CANCEL, player.getName().getString(), hasPerm);
                                return hasPerm;
                            }
                            return false; // Console can't use teleport requests
                        })
                        .executes(context -> executeTpCancel(context))
                );
            }
            
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Registered enabled teleport request commands");
        }
    }
    
    /**
     * Execute /tpa command
     */
    private static int executeTpa(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer requester = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            // Prevent self-teleport
            if (requester.getUUID().equals(target.getUUID())) {
                requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.self"));
                return 0;
            }
            
            // Send the request
            TeleportRequestManager manager = TeleportRequestManager.getInstance();
            boolean success = manager.sendTeleportRequest(requester, target, TeleportRequestType.TPA);
            
            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /tpa", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /tpa command", e);
            return 0;
        }
    }
    
    /**
     * Execute /tpahere command
     */
    private static int executeTpaHere(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer requester = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            // Prevent self-teleport
            if (requester.getUUID().equals(target.getUUID())) {
                requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.self"));
                return 0;
            }
            
            // Send the request
            TeleportRequestManager manager = TeleportRequestManager.getInstance();
            boolean success = manager.sendTeleportRequest(requester, target, TeleportRequestType.TPAHERE);
            
            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /tpahere", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /tpahere command", e);
            return 0;
        }
    }
    
    /**
     * Execute /tpaccept command
     */
    private static int executeTpAccept(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer teleportedPlayer = context.getSource().getPlayerOrException();
            TeleportRequestManager manager = TeleportRequestManager.getInstance();

            // NOTE: Do NOT save back location here.
            // TeleportRequestManager.executeTeleportRequest() saves the back location
            // for the correct player (the one actually being teleported).
            // For /tpa: the requester is teleported, NOT the acceptor.
            // For /tpahere: the acceptor is teleported — handled by the Manager.
            // Saving back for the acceptor here would overwrite their back location
            // with their current (unchanged) position, breaking /back for them.

            boolean success = manager.acceptTeleportRequest(teleportedPlayer);

            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /tpaccept", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /tpaccept command", e);
            return 0;
        }
    }
    
    /**
     * Execute /tpdeny command
     */
    private static int executeTpDeny(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TeleportRequestManager manager = TeleportRequestManager.getInstance();
            boolean success = manager.denyTeleportRequest(player);
            
            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /tpdeny", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /tpdeny command", e);
            return 0;
        }
    }
    
    /**
     * Execute /tpcancel command
     */
    private static int executeTpCancel(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            TeleportRequestManager manager = TeleportRequestManager.getInstance();
            boolean success = manager.cancelTeleportRequest(player);
            
            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /tpcancel", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /tpcancel command", e);
            return 0;
        }
    }
}