package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.moderation.JailManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.util.InputValidator;

import java.util.List;
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
        // Enforce moderationEnabled and jailSystemEnabled config
        if (!com.zerog.neoessentials.config.ConfigManager.isModerationEnabled()
            || !com.zerog.neoessentials.moderation.JailManager.isJailSystemEnabled()) {
            return;
        }
        // /jail <player> <jail> [reason]
        dispatcher.register(Commands.literal("jail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jail").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .then(Commands.argument("jail", StringArgumentType.word())
                    .suggests(SUGGEST_JAIL_NAMES)
                    .executes(ctx -> {
                        String defaultReason = com.zerog.neoessentials.config.ConfigManager.getInstance()
                            .getConfig("config.json")
                            .has("moderation") && com.zerog.neoessentials.config.ConfigManager.getInstance()
                            .getConfig("config.json").getAsJsonObject("moderation")
                            .has("jailSettings") && com.zerog.neoessentials.config.ConfigManager.getInstance()
                            .getConfig("config.json").getAsJsonObject("moderation")
                            .getAsJsonObject("jailSettings").has("defaultJailReason")
                            ? com.zerog.neoessentials.config.ConfigManager.getInstance()
                                .getConfig("config.json").getAsJsonObject("moderation")
                                .getAsJsonObject("jailSettings").get("defaultJailReason").getAsString()
                            : "Jailed by an operator";
                        return executeJail(ctx,
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "jail"),
                            defaultReason, 0L);
                    })
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> executeJail(ctx,
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "jail"),
                            StringArgumentType.getString(ctx, "reason"),
                            0L))
                    )
                )
            )
        );

        // /jailfor <player> <jail> <duration> [reason]  — timed jail (Essentials: sendtemp pattern)
        dispatcher.register(Commands.literal("jailfor")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jail").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .then(Commands.argument("jail", StringArgumentType.word())
                    .suggests(SUGGEST_JAIL_NAMES)
                    .then(Commands.argument("duration", StringArgumentType.word())
                        .executes(ctx -> {
                            long dur = com.zerog.neoessentials.util.commands.MailCommand.parseDuration(
                                StringArgumentType.getString(ctx, "duration"));
                            if (dur < 0) {
                                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.jail.invalid_duration",
                                    StringArgumentType.getString(ctx, "duration")));
                                return 0;
                            }
                            String defaultReason = "Jailed by an operator";
                            return executeJail(ctx,
                                StringArgumentType.getString(ctx, "player"),
                                StringArgumentType.getString(ctx, "jail"),
                                defaultReason, dur);
                        })
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                long dur = com.zerog.neoessentials.util.commands.MailCommand.parseDuration(
                                    StringArgumentType.getString(ctx, "duration"));
                                if (dur < 0) {
                                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.jail.invalid_duration",
                                        StringArgumentType.getString(ctx, "duration")));
                                    return 0;
                                }
                                return executeJail(ctx,
                                    StringArgumentType.getString(ctx, "player"),
                                    StringArgumentType.getString(ctx, "jail"),
                                    StringArgumentType.getString(ctx, "reason"), dur);
                            })
                        )
                    )
                )
            )
        );
        
        // /unjail <player>
        dispatcher.register(Commands.literal("unjail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.unjail").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(SUGGEST_JAILED_PLAYERS)
                .executes(ctx -> executeUnjail(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        
        // /setjail <name>
        dispatcher.register(Commands.literal("setjail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.setjail").hasPermission())
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> executeSetJail(ctx, StringArgumentType.getString(ctx, "name"))))
        );
        
        // /jaillist
        dispatcher.register(Commands.literal("jaillist")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jaillist").hasPermission())
            .executes(ctx -> executeJailList(ctx))
        );
        
        // /jailinfo [jail]
        dispatcher.register(Commands.literal("jailinfo")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jailinfo").hasPermission())
            .executes(ctx -> executeJailInfo(ctx, null))
            .then(Commands.argument("jail", StringArgumentType.word())
                .suggests(SUGGEST_JAIL_NAMES)
                .executes(ctx -> executeJailInfo(ctx, StringArgumentType.getString(ctx, "jail"))))
        );

        // /deljail <name>  — Essentials: Commanddeljail
        dispatcher.register(Commands.literal("deljail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.setjail").hasPermission())
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(SUGGEST_JAIL_NAMES)
                .executes(ctx -> executeDelJail(ctx, StringArgumentType.getString(ctx, "name"))))
        );

        // /jails — alias for /jaillist (Essentials: Commandjails)
        dispatcher.register(Commands.literal("jails")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jaillist").hasPermission())
            .executes(ctx -> executeJailList(ctx))
        );

        // /togglejail <player> — toggle a player's jail state (Essentials: Commandtogglejail)
        dispatcher.register(Commands.literal("togglejail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jail").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .executes(ctx -> executeToggleJail(ctx, StringArgumentType.getString(ctx, "player"))))
        );
    }

    private static int executeToggleJail(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();
        JailManager jailManager = JailManager.getInstance();
        MinecraftServer server = source.getServer();

        // Resolve player
        UUID playerId = null;
        String resolvedName = playerName;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(playerName)) {
                playerId = p.getUUID();
                resolvedName = p.getName().getString();
                break;
            }
        }
        if (playerId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        boolean isJailed = jailManager.isPlayerJailed(playerId);
        if (isJailed) {
            boolean ok = jailManager.unjailPlayer(playerId);
            if (ok) {
                final String name = resolvedName;
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.jail.unjail_success", name), true);
                return 1;
            }
        } else {
            List<JailManager.JailLocation> locations = jailManager.getAllJailLocations();
            if (locations.isEmpty()) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.jail.no_locations"));
                return 0;
            }
            String jailName = locations.get(0).name;
            boolean ok = jailManager.jailPlayer(resolvedName, playerId, "Toggled by staff", getCommandSender(source), jailName, 0L);
            if (ok) {
                final String name = resolvedName;
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.jail.jail_success", name, jailName), true);
                return 1;
            }
        }
        source.sendFailure(MessageUtil.error("commands.neoessentials.jail.toggle_failed", resolvedName));
        return 0;
    }


    private static int executeJail(CommandContext<CommandSourceStack> ctx, String playerName, String jailName, String reason, long durationMillis) {
        CommandSourceStack source = ctx.getSource();
        String jailedBy = getCommandSender(source);
        try {
            // Validate reason length and content
            InputValidator.ValidationResult reasonResult = InputValidator.validateReason(reason);
            if (!reasonResult.isValid()) {
                source.sendFailure(MessageUtil.error("Invalid reason: " + reasonResult.getErrorMessage()));
                return 0;
            }
            reason = (String) reasonResult.getValue();

            JailManager jailManager = JailManager.getInstance();
            MinecraftServer server = source.getServer();

            // Enforce requireJailLocation config: must have at least one jail location set
            boolean requireJailLocation = com.zerog.neoessentials.config.ConfigManager.isRequireJailLocationEnabled();
            if (requireJailLocation && jailManager.getAllJailLocations().isEmpty()) {
                source.sendFailure(MessageUtil.error("No jail locations are set. Please set a jail location before jailing players."));
                return 0;
            }
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
            boolean success = jailManager.jailPlayer(resolvedName, playerId, reason, jailedBy, jailName, durationMillis);

            if (success) {
                String confirmMessage = MessageUtil.localize("neoessentials.moderation.jail_success", resolvedName, jailName, reason);
                source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);

                // Broadcast jail to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.jail_broadcast", 
                    resolvedName, jailName, jailedBy, reason));

                LOGGER.info("Player {} jailed by {} in {} for: {}", resolvedName, jailedBy, jailName, reason);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_failed", resolvedName));
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
            
            // Try to find the player's UUID
            UUID playerId = null;
            String resolvedName = playerName;
            
            // First, try online players
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);
            if (onlinePlayer != null) {
                playerId = onlinePlayer.getUUID();
                resolvedName = onlinePlayer.getName().getString();
            } else {
                // Try player cache
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

            // Check if player is actually jailed
            if (!jailManager.isPlayerJailed(playerId)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_jailed", resolvedName));
                return 0;
            }

            // Unjail the player
            boolean success = jailManager.unjailPlayer(playerId);

            if (success) {
                String confirmMessage = MessageUtil.localize("neoessentials.moderation.unjail_success", resolvedName, unjailedBy);
                source.sendSuccess(() -> MessageUtil.success(confirmMessage), true);

                // Broadcast unjail to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unjail_broadcast", 
                    resolvedName, unjailedBy));

                LOGGER.info("Player {} unjailed by {}", resolvedName, unjailedBy);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.unjail_failed", resolvedName));
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
    
    private static int executeDelJail(CommandContext<CommandSourceStack> ctx, String jailName) {
        CommandSourceStack source = ctx.getSource();
        try {
            JailManager jailManager = JailManager.getInstance();
            if (jailManager.getJailLocation(jailName) == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_not_found", jailName));
                return 0;
            }
            // Check if any players are currently in this jail
            long inmates = jailManager.getAllJailedPlayers().stream()
                .filter(j -> j.jailName.equals(jailName)).count();
            jailManager.removeJailLocation(jailName);
            String msg = MessageUtil.localize("commands.neoessentials.jail.deljail_success", jailName);
            source.sendSuccess(() -> MessageUtil.success(msg), true);
            if (inmates > 0) {
                String warn = MessageUtil.localize("commands.neoessentials.jail.deljail_had_inmates", inmates);
                source.sendSuccess(() -> MessageUtil.warning(warn), false);
            }
            LOGGER.info("Jail location '{}' deleted by {}", jailName, getCommandSender(source));
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing deljail command", e);
            source.sendFailure(MessageUtil.error("An error occurred while deleting the jail."));
            return 0;
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