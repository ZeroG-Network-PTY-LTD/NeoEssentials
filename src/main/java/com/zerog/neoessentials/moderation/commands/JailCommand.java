package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.moderation.JailManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Jail commands: /jail, /unjail, /setjail, /jaillist, /jailinfo
 */
public class JailCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(JailCommand.class);
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JAILED_PLAYERS = (ctx, builder) -> {
        JailManager jailManager = JailManager.getInstance();
        return SharedSuggestionProvider.suggest(
            jailManager.getAllJailedPlayers().stream()
                .map(jail -> jail.playerName)
                .collect(Collectors.toList()),
            builder
        );
    };
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JAIL_NAMES = (ctx, builder) -> {
        JailManager jailManager = JailManager.getInstance();
        return SharedSuggestionProvider.suggest(
            jailManager.getAllJailLocations().stream()
                .map(jail -> jail.name)
                .collect(Collectors.toList()),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /jail <player> <jail> [reason]
        dispatcher.register(Commands.literal("jail")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.jail"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .then(Commands.argument("jail", StringArgumentType.word())
                    .suggests(SUGGEST_JAIL_NAMES)
                    .executes(ctx -> executeJail(ctx, 
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "jail"),
                        "No reason provided"))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> executeJail(ctx,
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "jail"),
                            StringArgumentType.getString(ctx, "reason"))))))
        );
        
        // /unjail <player>
        dispatcher.register(Commands.literal("unjail")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.unjail"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(SUGGEST_JAILED_PLAYERS)
                .executes(ctx -> executeUnjail(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        
        // /setjail <name>
        dispatcher.register(Commands.literal("setjail")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.setjail"))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> executeSetJail(ctx, StringArgumentType.getString(ctx, "name"))))
        );
        
        // /jaillist
        dispatcher.register(Commands.literal("jaillist")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.jaillist"))
            .executes(ctx -> executeJailList(ctx))
        );
        
        // /jailinfo [jail]
        dispatcher.register(Commands.literal("jailinfo")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.jailinfo"))
            .executes(ctx -> executeJailInfo(ctx, null))
            .then(Commands.argument("jail", StringArgumentType.word())
                .suggests(SUGGEST_JAIL_NAMES)
                .executes(ctx -> executeJailInfo(ctx, StringArgumentType.getString(ctx, "jail"))))
        );
    }
    
    private static int executeJail(CommandContext<CommandSourceStack> ctx, String playerName, String jailName, String reason) {
        CommandSourceStack source = ctx.getSource();
        String jailedBy = getCommandSender(source);
        
        try {
            JailManager jailManager = JailManager.getInstance();
            MinecraftServer server = source.getServer();
            
            // Check if jail exists
            if (jailManager.getJailLocation(jailName) == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_not_found", jailName));
                return 0;
            }
            
            // Resolve player UUID
            UUID playerId = null;
            String resolvedName = playerName;
            
            // Try to find online player first
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(playerName)) {
                    playerId = player.getUUID();
                    resolvedName = player.getName().getString();
                    break;
                }
            }
            
            // If not online, try to get from player cache
            if (playerId == null) {
                var profile = server.getProfileCache().get(playerName);
                if (profile.isPresent()) {
                    playerId = profile.get().getId();
                    resolvedName = profile.get().getName();
                }
            }
            
            if (playerId == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }
            
            // Jail the player
            boolean success = jailManager.jailPlayer(resolvedName, playerId, reason, jailedBy, jailName);
            
            if (success) {
                String confirmMessage = MessageUtil.localize("neoessentials.moderation.jail_success", resolvedName, jailName, reason);
                source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);
                
                // Broadcast jail to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.jail_broadcast", 
                    resolvedName, jailName, jailedBy, reason));
                
                LOGGER.info("Player {} jailed by {} in {} for: {}", resolvedName, jailedBy, jailName, reason);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.jail_failed", resolvedName);
                source.sendFailure(MessageUtil.error(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing jail command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the jail command."));
            return 0;
        }
    }
    
    private static int executeUnjail(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();
        String unjailedBy = getCommandSender(source);
        
        try {
            JailManager jailManager = JailManager.getInstance();
            MinecraftServer server = source.getServer();
            
            // Try to resolve player UUID
            UUID playerId = null;
            String resolvedName = playerName;
            
            // First check if it's a jailed player
            for (JailManager.JailEntry jail : jailManager.getAllJailedPlayers()) {
                if (jail.playerName.equalsIgnoreCase(playerName)) {
                    playerId = jail.playerId;
                    resolvedName = jail.playerName;
                    break;
                }
            }
            
            // If not found in jails, try player cache
            if (playerId == null) {
                var profile = server.getProfileCache().get(playerName);
                if (profile.isPresent()) {
                    playerId = profile.get().getId();
                    resolvedName = profile.get().getName();
                }
            }
            
            if (playerId == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }
            
            // Unjail the player
            boolean success = jailManager.unjailPlayer(playerId);
            
            if (success) {
                String confirmMessage = MessageUtil.localize("neoessentials.moderation.unjail_success", resolvedName);
                source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);
                
                // Broadcast unjail to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unjail_broadcast", 
                    resolvedName, unjailedBy));
                
                LOGGER.info("Player {} unjailed by {}", resolvedName, unjailedBy);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.unjail_failed", resolvedName);
                source.sendFailure(MessageUtil.error(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing unjail command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the unjail command."));
            return 0;
        }
    }
    
    private static int executeSetJail(CommandContext<CommandSourceStack> ctx, String jailName) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            // Must be executed by a player
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_only_command"));
                return 0;
            }
            
            JailManager jailManager = JailManager.getInstance();
            
            BlockPos position = player.blockPosition();
            String dimension = player.level().dimension().location().toString();
            String createdBy = player.getName().getString();
            
            boolean success = jailManager.setJailLocation(jailName, position, dimension, createdBy);
            
            if (success) {
                String message = MessageUtil.localize("neoessentials.moderation.setjail_success", jailName, 
                    position.getX(), position.getY(), position.getZ());
                source.sendSuccess(() -> MessageUtil.success(message), true);
                
                LOGGER.info("Jail '{}' set at {} by {}", jailName, position, createdBy);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.setjail_failed", jailName));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing setjail command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the setjail command."));
            return 0;
        }
    }
    
    private static int executeJailList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            JailManager jailManager = JailManager.getInstance();
            var jailedPlayers = jailManager.getAllJailedPlayers();
            
            if (jailedPlayers.isEmpty()) {
                String message = MessageUtil.localize("neoessentials.moderation.jaillist_empty");
                source.sendSuccess(() -> MessageUtil.info(message), false);
                return 1;
            }
            
            String header = MessageUtil.localize("neoessentials.moderation.jaillist_header", jailedPlayers.size());
            source.sendSuccess(() -> MessageUtil.info(header), false);
            
            for (JailManager.JailEntry jail : jailedPlayers) {
                String jailInfo = MessageUtil.localize("neoessentials.moderation.jaillist_entry",
                    jail.playerName, jail.jailName, jail.reason, jail.jailedBy, jail.getFormattedJailTime());
                source.sendSuccess(() -> MessageUtil.info(jailInfo), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing jaillist command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the jaillist command."));
            return 0;
        }
    }
    
    private static int executeJailInfo(CommandContext<CommandSourceStack> ctx, String jailName) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            JailManager jailManager = JailManager.getInstance();
            
            if (jailName == null) {
                // Show all jail locations
                var jailLocations = jailManager.getAllJailLocations();
                
                if (jailLocations.isEmpty()) {
                    String message = MessageUtil.localize("neoessentials.moderation.jailinfo_no_jails");
                    source.sendSuccess(() -> MessageUtil.warning(message), false);
                    return 1;
                }
                
                String message = MessageUtil.localize("neoessentials.moderation.jailinfo_all_header");
                source.sendSuccess(() -> MessageUtil.warning(message), false);
                
                for (JailManager.JailLocation jail : jailLocations) {
                    String locationInfo = MessageUtil.localize("neoessentials.moderation.jailinfo_location",
                        jail.name, jail.position.getX(), jail.position.getY(), jail.position.getZ(), 
                        jail.dimension, jail.createdBy, jail.getFormattedCreatedTime());
                    source.sendSuccess(() -> MessageUtil.info(locationInfo), false);
                }
                
                String countInfo = MessageUtil.localize("neoessentials.moderation.jailinfo_count", jailLocations.size());
                source.sendSuccess(() -> MessageUtil.info(countInfo), false);
                
            } else {
                // Show specific jail info
                JailManager.JailLocation jail = jailManager.getJailLocation(jailName);
                
                if (jail == null) {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_not_found", jailName));
                    return 0;
                }
                
                String locationInfo = MessageUtil.localize("neoessentials.moderation.jailinfo_specific",
                    jail.name, jail.position.getX(), jail.position.getY(), jail.position.getZ(), 
                    jail.dimension, jail.createdBy, jail.getFormattedCreatedTime());
                source.sendSuccess(() -> MessageUtil.info(locationInfo), false);
                
                // Show how many players are in this jail
                long playersInJail = jailManager.getAllJailedPlayers().stream()
                    .filter(j -> j.jailName.equals(jailName))
                    .count();
                
                if (playersInJail > 0) {
                    String playerInfo = MessageUtil.localize("neoessentials.moderation.jailinfo_players", playersInJail);
                    source.sendSuccess(() -> MessageUtil.info(playerInfo), false);
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing jailinfo command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the jailinfo command."));
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