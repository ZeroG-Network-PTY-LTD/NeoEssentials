package com.zerog.neoessentials.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
<<<<<<< HEAD
<<<<<<< HEAD
import com.mojang.brigadier.suggestion.SuggestionProvider;
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
import com.mojang.brigadier.suggestion.SuggestionProvider;
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.TextUtil;
import com.zerog.neoessentials.utils.TimeUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
<<<<<<< HEAD
<<<<<<< HEAD
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
=======
=======
import net.minecraft.commands.SharedSuggestionProvider;
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
<<<<<<< HEAD
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
<<<<<<< HEAD
<<<<<<< HEAD
import java.util.stream.Collectors;

/**
 * Implements comprehensive moderation commands for server administrators.
 * <p>
 * The ModeratorCommands class provides essential tools for maintaining server order
 * and managing player conduct. The commands are designed to provide clear feedback
 * to both moderators and affected players, with appropriate logging of all actions.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>Ban System</b>: Permanent and temporary bans with customizable reasons and durations</li>
 *   <li><b>IP-based Restrictions</b>: Ban IP addresses to prevent ban evasion</li>
 *   <li><b>Kick Function</b>: Remove players with customized messages</li>
 *   <li><b>Mute System</b>: Temporary or permanent chat restrictions</li>
 *   <li><b>List Commands</b>: View currently banned players and IPs</li>
 *   <li><b>Professional Formatting</b>: Clear, color-coded messages for both staff and players</li>
 * </ul>
 * 
 * <p>All commands include proper permission checks and comprehensive logging.</p>
 * 
 * @author ZeroG
 * @since 1.0.0
=======

/**
 * Implements moderator commands like ban, tempban, kick, mute, etc.
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
import java.util.stream.Collectors;

/**
 * Implements moderator commands like ban, tempban, kick, mute, etc.
 * Provides comprehensive moderation tools for server administrators.
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
 */
public class ModeratorCommands {

    // Store muted players with expiry time
    private final Map<UUID, Date> mutedPlayers = new ConcurrentHashMap<>();
    
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
    // Common reason suggestions for moderation actions
    private static final List<String> COMMON_REASONS = Arrays.asList(
        "Breaking server rules", 
        "Inappropriate behavior", 
        "Spamming", 
        "Griefing", 
        "Using forbidden mods/hacks",
        "Offensive language"
    );
    
    // Time duration suggestions for temp bans and mutes
    private static final List<String> TIME_SUGGESTIONS = Arrays.asList(
        "1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d"
    );
    
    // Suggestion providers for common inputs
    private static final SuggestionProvider<CommandSourceStack> REASON_SUGGESTIONS = 
            (context, builder) -> SharedSuggestionProvider.suggest(COMMON_REASONS, builder);
            
    private static final SuggestionProvider<CommandSourceStack> TIME_DURATION_SUGGESTIONS = 
            (context, builder) -> SharedSuggestionProvider.suggest(TIME_SUGGESTIONS, builder);
    
<<<<<<< HEAD
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
    /**
     * Register all moderator commands
     * 
     * @param dispatcher Command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /kick <player> [reason]
        dispatcher.register(Commands.literal("kick")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.kick"))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> kickPlayer(context, "Kicked by admin"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
<<<<<<< HEAD
<<<<<<< HEAD
                    .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                    .suggests(REASON_SUGGESTIONS)
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
                    .executes(context -> kickPlayer(
                        context,
                        StringArgumentType.getString(context, "reason")
                    ))
                )
            )
        );
        
        // /ban <player> [reason]
        dispatcher.register(Commands.literal("ban")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.ban"))
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(context -> banPlayer(context, "Banned by admin"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
<<<<<<< HEAD
<<<<<<< HEAD
                    .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                    .suggests(REASON_SUGGESTIONS)
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
                    .executes(context -> banPlayer(
                        context,
                        StringArgumentType.getString(context, "reason")
                    ))
                )
            )
        );
        
        // /unban <player>
        dispatcher.register(Commands.literal("unban")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.unban"))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> unbanPlayer(context))
            )
        );
        
        // /tempban <player> <time> [reason]
        dispatcher.register(Commands.literal("tempban")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.tempban"))
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .then(Commands.argument("time", StringArgumentType.word())
<<<<<<< HEAD
<<<<<<< HEAD
                    .suggests(TIME_DURATION_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                    .suggests(TIME_DURATION_SUGGESTIONS)
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
                    .executes(context -> tempBanPlayer(
                        context, 
                        StringArgumentType.getString(context, "time"),
                        "Temporarily banned by admin"
                    ))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
<<<<<<< HEAD
<<<<<<< HEAD
                        .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                        .suggests(REASON_SUGGESTIONS)
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
                        .executes(context -> tempBanPlayer(
                            context,
                            StringArgumentType.getString(context, "time"),
                            StringArgumentType.getString(context, "reason")
                        ))
                    )
                )
            )
        );
        
        // /banip <player/address> [reason]
        dispatcher.register(Commands.literal("banip")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.banip"))
            .then(Commands.argument("target", StringArgumentType.word())
                .executes(context -> banIp(context, "IP banned by admin"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
<<<<<<< HEAD
<<<<<<< HEAD
                    .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                    .suggests(REASON_SUGGESTIONS)
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
                    .executes(context -> banIp(
                        context,
                        StringArgumentType.getString(context, "reason")
                    ))
                )
            )
        );
        
        // /unbanip <address>
        dispatcher.register(Commands.literal("unbanip")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.unbanip"))
            .then(Commands.argument("address", StringArgumentType.word())
                .executes(context -> unbanIp(context))
            )
        );
        
        // /mute <player> [time] [reason]
        dispatcher.register(Commands.literal("mute")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.mute"))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> mutePlayer(
                    context,
                    EntityArgument.getPlayer(context, "player"),
                    null,
                    "Muted by admin"
                ))
                .then(Commands.argument("time", StringArgumentType.word())
<<<<<<< HEAD
<<<<<<< HEAD
                    .suggests(TIME_DURATION_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                    .suggests(TIME_DURATION_SUGGESTIONS)
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
                    .executes(context -> mutePlayer(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "time"),
                        "Muted by admin"
                    ))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
<<<<<<< HEAD
<<<<<<< HEAD
                        .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                        .suggests(REASON_SUGGESTIONS)
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
                        .executes(context -> mutePlayer(
                            context,
                            EntityArgument.getPlayer(context, "player"),
                            StringArgumentType.getString(context, "time"),
                            StringArgumentType.getString(context, "reason")
                        ))
                    )
                )
            )
        );
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
        
        // Add /unmute command
        dispatcher.register(Commands.literal("unmute")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.unmute"))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> unmutePlayer(context))
            )
        );
        
        // Add /banlist command
        dispatcher.register(Commands.literal("banlist")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.banlist"))
            .executes(context -> listBans(context, "players"))
            .then(Commands.literal("players")
                .executes(context -> listBans(context, "players"))
            )
            .then(Commands.literal("ips")
                .executes(context -> listBans(context, "ips"))
            )
        );
        
        NeoEssentials.LOGGER.info("Registered enhanced moderator commands");
<<<<<<< HEAD
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
    }
    
    /**
     * Kick a player with a reason
     */
    private int kickPlayer(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
        ServerPlayer source = null;
        try {
            source = context.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // Source is not a player (e.g. console)
        }
<<<<<<< HEAD
        
        // Check for kick exemption
        if (PermissionUtil.hasPermission(player, "essentials.kick.exempt")) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cYou cannot kick this player.")));
=======
        
        if (PermissionUtil.hasPermission((ServerPlayer)player, "essentials.kick.exempt")) {
            context.getSource().sendFailure(Component.literal("You cannot kick this player."));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
        
        // Check for kick exemption
        if (PermissionUtil.hasPermission(player, "essentials.kick.exempt")) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cYou cannot kick this player.")));
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
            return 0;
        }
        
        try {
<<<<<<< HEAD
<<<<<<< HEAD
            String formattedReason = TextUtil.colorize(reason);
            
            // Create a styled kick message
            Component kickMessage = Component.literal(TextUtil.colorize("&c&lYou have been kicked from the server!\n\n"))
                    .append(Component.literal(TextUtil.colorize("&7Reason: &f" + formattedReason + "\n")));
                      // Add kicked by information if source is a player
<<<<<<< HEAD
            if (source != null) {
                Component kickedByInfo = Component.literal(TextUtil.colorize("&7Kicked by: &f" + source.getScoreboardName()));
                kickMessage = Component.empty().append(kickMessage).append(kickedByInfo);
            }
            
            player.connection.disconnect(kickMessage);
            
            // Broadcast kick message to server
            MinecraftServer server = context.getSource().getServer();
            Component broadcastMessage = Component.literal(TextUtil.colorize("&c" + player.getScoreboardName() + " &7was kicked: &f" + formattedReason));
            server.getPlayerList().broadcastSystemMessage(broadcastMessage, false);
            
            // Log kick action
            String sourceString = source != null ? source.getScoreboardName() : "Console";
            NeoEssentials.LOGGER.info("{} kicked {} for: {}", sourceString, player.getScoreboardName(), reason);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to kick player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error kicking player", e);
=======
            String formattedReason = TextUtil.formatText(reason);
            player.connection.disconnect(Component.literal(formattedReason));
=======
            String formattedReason = TextUtil.colorize(reason);
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
            
            // Create a styled kick message
            Component kickMessage = Component.literal(TextUtil.colorize("&c&lYou have been kicked from the server!\n\n"))
                    .append(Component.literal(TextUtil.colorize("&7Reason: &f" + formattedReason + "\n")));
                    
            // Add kicked by information if source is a player
=======
>>>>>>> 175e397 (feat: Enhance kick message formatting and improve unban player functionality with GameProfile support)
            if (source != null) {
                Component kickedByInfo = Component.literal(TextUtil.colorize("&7Kicked by: &f" + source.getScoreboardName()));
                kickMessage = Component.empty().append(kickMessage).append(kickedByInfo);
            }
            
            player.connection.disconnect(kickMessage);
            
            // Broadcast kick message to server
            MinecraftServer server = context.getSource().getServer();
            Component broadcastMessage = Component.literal(TextUtil.colorize("&c" + player.getScoreboardName() + " &7was kicked: &f" + formattedReason));
            server.getPlayerList().broadcastSystemMessage(broadcastMessage, false);
            
            // Log kick action
            String sourceString = source != null ? source.getScoreboardName() : "Console";
            NeoEssentials.LOGGER.info("{} kicked {} for: {}", sourceString, player.getScoreboardName(), reason);
            
            return 1;
        } catch (Exception e) {
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal("Failed to kick player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to kick player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error kicking player", e);
>>>>>>> c24406b (Implement SQLite storage handler and command argument types; enhance tablist management features)
            return 0;
        }
    }
    
    /**
     * Ban a player with a reason
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
     */    
=======
     */
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
    private int banPlayer(CommandContext<CommandSourceStack> context, String reason) {
=======
     */    private int banPlayer(CommandContext<CommandSourceStack> context, String reason) {
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)
=======
     */    
    private int banPlayer(CommandContext<CommandSourceStack> context, String reason) {
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
        try {
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "player");
            
            if (targets.isEmpty()) {
<<<<<<< HEAD
<<<<<<< HEAD
                context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cNo players specified.")));
=======
                context.getSource().sendFailure(Component.literal("No players specified."));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cNo players specified.")));
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
                return 0;
            }
            
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
            int count = 0;
            
            for (GameProfile profile : targets) {
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
                if (profile == null) continue;
                
                // Check for ban exemption
                ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
                if (targetPlayer != null && PermissionUtil.hasPermission(targetPlayer, "essentials.ban.exempt")) {
                    context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                            "&cYou cannot ban &e" + profile.getName() + " &cas they are exempt from bans.")));
<<<<<<< HEAD
                    continue;
                }
                
                // Create ban entry
                UserBanListEntry banEntry = new UserBanListEntry(
                        profile, 
                        new Date(), 
                        context.getSource().getTextName(), 
                        null, 
                        TextUtil.colorize(reason)
                );
                
                banList.add(banEntry);
                count++;
                
                // Kick player if they're online
                if (targetPlayer != null) {
                    // Create a styled ban message
                    Component banMessage = Component.literal(TextUtil.colorize("&c&lYou have been banned from the server!\n\n"))
                            .append(Component.literal(TextUtil.colorize("&7Reason: &f" + reason + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Banned by: &f" + context.getSource().getTextName())));
                    
                    targetPlayer.connection.disconnect(banMessage);
                }
                
                // Log ban action
                NeoEssentials.LOGGER.info("{} banned {} for: {}", 
                        context.getSource().getTextName(), profile.getName(), reason);
            }
              if (count > 0) {
                final int finalCount = count;
                final String plural = count > 1 ? "s" : "";
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&aSuccessfully banned &e" + finalCount + " &aplayer" + plural + ".")), true);
=======
                if (PermissionUtil.hasPermission(profile, "essentials.ban.exempt")) {
=======
                if (PermissionUtil.hasPermission((com.mojang.authlib.GameProfile)profile, "essentials.ban.exempt")) {
>>>>>>> 30e3241 (Refactor code structure for improved readability and maintainability)
                    context.getSource().sendFailure(Component.literal("You cannot ban " + profile.getName() + "."));
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
                    continue;
                }
                
                // Create ban entry
                UserBanListEntry banEntry = new UserBanListEntry(
                        profile, 
                        new Date(), 
                        context.getSource().getTextName(), 
                        null, 
                        TextUtil.colorize(reason)
                );
                
                banList.add(banEntry);
                count++;
                
                // Kick player if they're online
                if (targetPlayer != null) {
                    // Create a styled ban message
                    Component banMessage = Component.literal(TextUtil.colorize("&c&lYou have been banned from the server!\n\n"))
                            .append(Component.literal(TextUtil.colorize("&7Reason: &f" + reason + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Banned by: &f" + context.getSource().getTextName())));
                    
                    targetPlayer.connection.disconnect(banMessage);
                }
                
                // Log ban action
                NeoEssentials.LOGGER.info("{} banned {} for: {}", 
                        context.getSource().getTextName(), profile.getName(), reason);
            }
            
            if (count > 0) {
<<<<<<< HEAD
<<<<<<< HEAD
                context.getSource().sendSuccess(() -> Component.literal("Banned " + count + " players: " + reason), true);
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                final int finalCount = count;
                final String finalReason = reason;
                context.getSource().sendSuccess(() -> Component.literal("Banned " + finalCount + " players: " + finalReason), true);
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)
=======
                String plural = count > 1 ? "s" : "";
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&aSuccessfully banned &e" + count + " &aplayer" + plural + ".")), true);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            }
            
            return count;
        } catch (Exception e) {
<<<<<<< HEAD
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to ban player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error banning player", e);
=======
            context.getSource().sendFailure(Component.literal("Failed to ban player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to ban player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error banning player", e);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            return 0;
        }
    }
    
    /**
     * Unban a player
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
     */    private int unbanPlayer(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
        
        // Convert player name to GameProfile
        GameProfile profile = context.getSource().getServer().getProfileCache()
            .get(playerName)
            .orElse(new GameProfile(null, playerName));
        
        // Check if the player is banned
        if (!banList.isBanned(profile)) {
=======
     */
    private int unbanPlayer(CommandContext<CommandSourceStack> context) {
=======
     */    private int unbanPlayer(CommandContext<CommandSourceStack> context) {
>>>>>>> 175e397 (feat: Enhance kick message formatting and improve unban player functionality with GameProfile support)
        String playerName = StringArgumentType.getString(context, "player");
        UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
        
        // Convert player name to GameProfile
        GameProfile profile = context.getSource().getServer().getProfileCache()
            .get(playerName)
            .orElse(new GameProfile(null, playerName));
        
        // Check if the player is banned
<<<<<<< HEAD
        if (!banList.isBanned(playerName)) {
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
=======
        if (!banList.isBanned(profile)) {
>>>>>>> 175e397 (feat: Enhance kick message formatting and improve unban player functionality with GameProfile support)
            context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                    "&cPlayer &e" + playerName + " &cis not banned.")));
            return 0;
        }
        
<<<<<<< HEAD
        try {
            banList.remove(profile);
            
            // Announce unban action
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&aPlayer &e" + playerName + " &ahas been unbanned.")), true);
                    
            // Log unban action
            NeoEssentials.LOGGER.info("{} unbanned {}", 
                    context.getSource().getTextName(), playerName);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to unban player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error unbanning player", e);
=======
     */
    private int unbanPlayer(CommandContext<CommandSourceStack> context) {
=======
     */    private int unbanPlayer(CommandContext<CommandSourceStack> context) {
>>>>>>> 009105b (fix: Improve unban logic to directly remove banned players by name and streamline teleport command level retrieval)
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
        try {
            banList.remove(playerName);
            
            // Announce unban action
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&aPlayer &e" + playerName + " &ahas been unbanned.")), true);
                    
            // Log unban action
            NeoEssentials.LOGGER.info("{} unbanned {}", 
                    context.getSource().getTextName(), playerName);
            
            return 1;
        } catch (Exception e) {
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal("Failed to unban player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to unban player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error unbanning player", e);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            return 0;
        }
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
     * Temporarily ban a player
=======
     * Temporarily ban a player for a specific duration
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
     * Temporarily ban a player
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
     */
    private int tempBanPlayer(CommandContext<CommandSourceStack> context, String timeStr, String reason) {
        try {
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "player");
            
            if (targets.isEmpty()) {
<<<<<<< HEAD
<<<<<<< HEAD
                context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cNo players specified.")));
                return 0;
            }
            
            // Convert time string to expiration date
            Date expiry = TimeUtil.parseTimeToDate(timeStr);
            if (expiry == null) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                        "&cInvalid time format. Use format like '1d' for one day, '6h' for six hours, etc.")));
                return 0;
            }
            
            long durationMillis = expiry.getTime() - System.currentTimeMillis();
            if (durationMillis <= 0) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cTime duration must be positive.")));
=======
                context.getSource().sendFailure(Component.literal("No players specified."));
                return 0;
            }
            
            // Parse time duration
            Date expires;
            try {
                expires = TimeUtil.parseTimeSpecification(timeStr);
            } catch (IllegalArgumentException e) {
                context.getSource().sendFailure(Component.literal("Invalid time format. Use formats like '1d2h30m' for 1 day, 2 hours, 30 minutes."));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cNo players specified.")));
                return 0;
            }
            
            // Convert time string to expiration date
            Date expiry = TimeUtil.parseTimeToDate(timeStr);
            if (expiry == null) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                        "&cInvalid time format. Use format like '1d' for one day, '6h' for six hours, etc.")));
                return 0;
            }
            
            long durationMillis = expiry.getTime() - System.currentTimeMillis();
            if (durationMillis <= 0) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cTime duration must be positive.")));
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
                return 0;
            }
            
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
            int count = 0;
            
            for (GameProfile profile : targets) {
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
                if (profile == null) continue;
                
                // Check for ban exemption
                ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
                if (targetPlayer != null && PermissionUtil.hasPermission(targetPlayer, "essentials.tempban.exempt")) {
                    context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                            "&cYou cannot ban &e" + profile.getName() + " &cas they are exempt from bans.")));
<<<<<<< HEAD
                    continue;
                }
                
                // Create temporary ban entry
                UserBanListEntry banEntry = new UserBanListEntry(
                        profile, 
                        new Date(), 
                        context.getSource().getTextName(), 
                        expiry, 
                        TextUtil.colorize(reason)
                );
                
                banList.add(banEntry);
                count++;
                
                // Kick player if they're online
                if (targetPlayer != null) {
                    // Format time remaining
                    String formattedTime = TimeUtil.formatDuration(durationMillis);
                    
                    // Create a styled ban message
                    Component banMessage = Component.literal(TextUtil.colorize("&c&lYou have been temporarily banned!\n\n"))
                            .append(Component.literal(TextUtil.colorize("&7Reason: &f" + reason + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Duration: &f" + formattedTime + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Expires: &f" + formatDate(expiry) + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Banned by: &f" + context.getSource().getTextName())));
                    
                    targetPlayer.connection.disconnect(banMessage);
                }
                
                // Log temp ban action
                NeoEssentials.LOGGER.info("{} temporarily banned {} for {} (reason: {})", 
                        context.getSource().getTextName(), profile.getName(), 
                        TimeUtil.formatDuration(durationMillis), reason);
            }
              if (count > 0) {
                final int finalCount = count;
                final String plural = count > 1 ? "s" : "";
                final String formattedTime = TimeUtil.formatDuration(durationMillis);
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&aSuccessfully temporarily banned &e" + finalCount + " &aplayer" + plural + " for &e" + formattedTime + "&a.")), true);
=======
                if (PermissionUtil.hasPermission(profile, "essentials.tempban.exempt")) {
=======
                if (PermissionUtil.hasPermission((com.mojang.authlib.GameProfile)profile, "essentials.tempban.exempt")) {
>>>>>>> 30e3241 (Refactor code structure for improved readability and maintainability)
                    context.getSource().sendFailure(Component.literal("You cannot temp ban " + profile.getName() + "."));
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
                    continue;
                }
                
                // Create temporary ban entry
                UserBanListEntry banEntry = new UserBanListEntry(
                        profile, 
                        new Date(), 
                        context.getSource().getTextName(), 
                        expiry, 
                        TextUtil.colorize(reason)
                );
                
                banList.add(banEntry);
                count++;
                
                // Kick player if they're online
                if (targetPlayer != null) {
                    // Format time remaining
                    String formattedTime = TimeUtil.formatDuration(durationMillis);
                    
                    // Create a styled ban message
                    Component banMessage = Component.literal(TextUtil.colorize("&c&lYou have been temporarily banned!\n\n"))
                            .append(Component.literal(TextUtil.colorize("&7Reason: &f" + reason + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Duration: &f" + formattedTime + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Expires: &f" + formatDate(expiry) + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Banned by: &f" + context.getSource().getTextName())));
                    
                    targetPlayer.connection.disconnect(banMessage);
                }
                
                // Log temp ban action
                NeoEssentials.LOGGER.info("{} temporarily banned {} for {} (reason: {})", 
                        context.getSource().getTextName(), profile.getName(), 
                        TimeUtil.formatDuration(durationMillis), reason);
            }
<<<<<<< HEAD
              if (count > 0) {
                final int finalCount = count;
                final Date finalExpires = expires;
                final String finalReason = reason;
                context.getSource().sendSuccess(() -> 
<<<<<<< HEAD
                    Component.literal("Temporarily banned " + count + " players until " + 
                                 TimeUtil.formatDate(expires) + ": " + reason), true);
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                    Component.literal("Temporarily banned " + finalCount + " players until " + 
                                 TimeUtil.formatDate(finalExpires) + ": " + finalReason), true);
>>>>>>> 9db1c98 (feat: Implement AFK commands and functionality, including auto-AFK detection and player status management)
=======
            
            if (count > 0) {
                String plural = count > 1 ? "s" : "";
                String formattedTime = TimeUtil.formatDuration(durationMillis);
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&aSuccessfully temporarily banned &e" + count + " &aplayer" + plural + " for &e" + formattedTime + "&a.")), true);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            }
            
            return count;
        } catch (Exception e) {
<<<<<<< HEAD
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to temp-ban player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error temp-banning player", e);
=======
            context.getSource().sendFailure(Component.literal("Failed to temp ban player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to temp-ban player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error temp-banning player", e);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            return 0;
        }
    }
    
    /**
     * Ban an IP address
     */
    private int banIp(CommandContext<CommandSourceStack> context, String reason) {
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
        String target = StringArgumentType.getString(context, "target");
        MinecraftServer server = context.getSource().getServer();
        IpBanList ipBanList = server.getPlayerList().getIpBans();
        String ipAddress = target;
        
        // Check if target is a player name or an IP
        if (!target.contains(".")) {
            // It's probably a player name, try to get their IP
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(target);
            if (targetPlayer == null) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                        "&cCould not find player &e" + target + "&c or the input is not a valid IP address.")));
                return 0;
<<<<<<< HEAD
            }
            
            ipAddress = targetPlayer.getIpAddress();
            if (ipAddress == null || ipAddress.isEmpty()) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                        "&cCould not determine IP address for player &e" + target + "&c.")));
                return 0;
            }
        }
        
        try {
            // Create IP ban entry
            IpBanListEntry banEntry = new IpBanListEntry(
                    ipAddress,
                    new Date(),
                    context.getSource().getTextName(), 
                    null,
                    TextUtil.colorize(reason)
=======
        try {
            String target = StringArgumentType.getString(context, "target");
            String ipAddress;
            
            // Check if target is a player or direct IP
            if (target.contains(".")) {
                // It's an IP address
                ipAddress = target;
            } else {
                // Try to get player's IP
                ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(target);
                if (targetPlayer == null) {
                    context.getSource().sendFailure(Component.literal("Player not found and input doesn't look like an IP address."));
                    return 0;
                }
                
                if (PermissionUtil.hasPermission((ServerPlayer)targetPlayer, "essentials.banip.exempt")) {
                    context.getSource().sendFailure(Component.literal("You cannot ban this player's IP."));
                    return 0;
                }
                
                ipAddress = targetPlayer.getIpAddress();
                if (ipAddress == null || ipAddress.isEmpty()) {
                    context.getSource().sendFailure(Component.literal("Could not determine player's IP address."));
                    return 0;
                }
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            }
            
            ipAddress = targetPlayer.getIpAddress();
            if (ipAddress == null || ipAddress.isEmpty()) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                        "&cCould not determine IP address for player &e" + target + "&c.")));
                return 0;
            }
        }
        
        try {
            // Create IP ban entry
            IpBanListEntry banEntry = new IpBanListEntry(
<<<<<<< HEAD
                ipAddress,
                new Date(),
                context.getSource().getTextName(),
                null,
                reason
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                    ipAddress,
                    new Date(),
                    context.getSource().getTextName(), 
                    null,
                    TextUtil.colorize(reason)
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            );
            
            ipBanList.add(banEntry);
            
            // Kick all players with this IP
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            int kickedCount = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ipAddress.equals(player.getIpAddress())) {
                    // Create a styled IP ban message
                    Component banMessage = Component.literal(TextUtil.colorize("&c&lYour IP address has been banned!\n\n"))
                            .append(Component.literal(TextUtil.colorize("&7Reason: &f" + reason + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Banned by: &f" + context.getSource().getTextName())));
                    
                    player.connection.disconnect(banMessage);
                    kickedCount++;
<<<<<<< HEAD
                }
            }
              // Announce the IP ban
            final String finalIpAddress = ipAddress;
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&aSuccessfully banned IP address &e" + finalIpAddress + "&a.")), true);
                    
            if (kickedCount > 0) {
                final int finalKickedCount = kickedCount;
                final String plural = kickedCount > 1 ? "s" : "";
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&e" + finalKickedCount + " &aplayer" + plural + " with this IP " + (finalKickedCount == 1 ? "was" : "were") + " disconnected.")), false);
            }
            
            // Log IP ban action
            NeoEssentials.LOGGER.info("{} banned IP {} for: {}", 
                    context.getSource().getTextName(), ipAddress, reason);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to ban IP: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error banning IP", e);
=======
            List<ServerPlayer> playersToKick = new ArrayList<>();
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                if (player.getIpAddress().equals(ipAddress)) {
                    playersToKick.add(player);
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
                }
            }
            
            // Announce the IP ban
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&aSuccessfully banned IP address &e" + ipAddress + "&a.")), true);
                    
            if (kickedCount > 0) {
                String plural = kickedCount > 1 ? "s" : "";
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&e" + kickedCount + " &aplayer" + plural + " with this IP " + (kickedCount == 1 ? "was" : "were") + " disconnected.")), false);
            }
            
            // Log IP ban action
            NeoEssentials.LOGGER.info("{} banned IP {} for: {}", 
                    context.getSource().getTextName(), ipAddress, reason);
            
            return 1;
        } catch (Exception e) {
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal("Failed to ban IP: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to ban IP: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error banning IP", e);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            return 0;
        }
    }
    
    /**
     * Unban an IP address
<<<<<<< HEAD
<<<<<<< HEAD
     */
    private int unbanIp(CommandContext<CommandSourceStack> context) {
<<<<<<< HEAD
=======
     */
    private int unbanIp(CommandContext<CommandSourceStack> context) {
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
        String ipAddress = StringArgumentType.getString(context, "address");
        IpBanList ipBanList = context.getSource().getServer().getPlayerList().getIpBans();
        
        // Check if the IP is banned
        if (!ipBanList.isBanned(ipAddress)) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                    "&cIP address &e" + ipAddress + " &cis not banned.")));
            return 0;
        }
        
<<<<<<< HEAD
        try {
            ipBanList.remove(ipAddress);
            
            // Announce unban action
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&aIP address &e" + ipAddress + " &ahas been unbanned.")), true);
                    
            // Log unban action
            NeoEssentials.LOGGER.info("{} unbanned IP {}", 
                    context.getSource().getTextName(), ipAddress);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to unban IP: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error unbanning IP", e);
=======
=======
     */    private int unbanIp(CommandContext<CommandSourceStack> context) {
>>>>>>> 4184062 (fix: Add check for banned IP address before unbanning in ModeratorCommands)
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
        try {
            ipBanList.remove(ipAddress);
            
            // Announce unban action
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&aIP address &e" + ipAddress + " &ahas been unbanned.")), true);
                    
            // Log unban action
            NeoEssentials.LOGGER.info("{} unbanned IP {}", 
                    context.getSource().getTextName(), ipAddress);
            
            return 1;
        } catch (Exception e) {
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal("Failed to unban IP: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to unban IP: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error unbanning IP", e);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            return 0;
        }
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
     * Mute a player, either permanently or temporarily
     */
    private int mutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, String timeStr, String reason) {
        try {
            // Check for mute exemption
            if (PermissionUtil.hasPermission(target, "essentials.mute.exempt")) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                        "&cYou cannot mute &e" + target.getScoreboardName() + " &cas they are exempt from mutes.")));
                return 0;
            }
            
            Date expiry = null;
            String durationText = "permanently";
            
            // Parse time if provided
            if (timeStr != null && !timeStr.isEmpty()) {
                expiry = TimeUtil.parseTimeToDate(timeStr);
                if (expiry == null) {
                    context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                            "&cInvalid time format. Use format like '1d' for one day, '6h' for six hours, etc.")));
                    return 0;
                }
                
                long durationMillis = expiry.getTime() - System.currentTimeMillis();
                if (durationMillis <= 0) {
                    context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cTime duration must be positive.")));
                    return 0;
                }
                
                durationText = "for " + TimeUtil.formatDuration(durationMillis);
            }
            
            // Store mute in the map
            mutedPlayers.put(target.getUUID(), expiry);
            
            // Send message to target player
            if (expiry != null) {
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&cYou have been muted " + durationText + ".")));
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&7Reason: &f" + reason)));
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&7Expires: &f" + formatDate(expiry))));
            } else {
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&cYou have been permanently muted.")));
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&7Reason: &f" + reason)));
            }
              // Announce mute to source
            final String playerName = target.getScoreboardName();
            final String finalDurationText = durationText;
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&aPlayer &e" + playerName + " &ahas been muted " + finalDurationText + ".")), true);
                    
            // Log mute action
            NeoEssentials.LOGGER.info("{} muted {} {} for: {}", 
                    context.getSource().getTextName(), target.getScoreboardName(), 
                    durationText, reason);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to mute player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error muting player", e);
=======
     * Mute a player for a specific duration or permanently
=======
     * Mute a player, either permanently or temporarily
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
     */
    private int mutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, String timeStr, String reason) {
        try {
            // Check for mute exemption
            if (PermissionUtil.hasPermission(target, "essentials.mute.exempt")) {
                context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                        "&cYou cannot mute &e" + target.getScoreboardName() + " &cas they are exempt from mutes.")));
                return 0;
            }
            
            Date expiry = null;
            String durationText = "permanently";
            
            // Parse time if provided
            if (timeStr != null && !timeStr.isEmpty()) {
                expiry = TimeUtil.parseTimeToDate(timeStr);
                if (expiry == null) {
                    context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                            "&cInvalid time format. Use format like '1d' for one day, '6h' for six hours, etc.")));
                    return 0;
                }
                
                long durationMillis = expiry.getTime() - System.currentTimeMillis();
                if (durationMillis <= 0) {
                    context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cTime duration must be positive.")));
                    return 0;
                }
                
                durationText = "for " + TimeUtil.formatDuration(durationMillis);
            }
            
            // Store mute in the map
            mutedPlayers.put(target.getUUID(), expiry);
            
            // Send message to target player
            if (expiry != null) {
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&cYou have been muted " + durationText + ".")));
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&7Reason: &f" + reason)));
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&7Expires: &f" + formatDate(expiry))));
            } else {
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&cYou have been permanently muted.")));
                target.sendSystemMessage(Component.literal(TextUtil.colorize(
                        "&7Reason: &f" + reason)));
            }
            
            // Announce mute to source
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&aPlayer &e" + target.getScoreboardName() + " &ahas been muted " + durationText + ".")), true);
                    
            // Log mute action
            NeoEssentials.LOGGER.info("{} muted {} {} for: {}", 
                    context.getSource().getTextName(), target.getScoreboardName(), 
                    durationText, reason);
            
            return 1;
        } catch (Exception e) {
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal("Failed to mute player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to mute player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error muting player", e);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            return 0;
        }
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
     * Unmute a player
     */
    private int unmutePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        if (!mutedPlayers.containsKey(target.getUUID())) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                    "&cPlayer &e" + target.getScoreboardName() + " &cis not muted.")));
            return 0;
        }
        
        mutedPlayers.remove(target.getUUID());
        
        // Send message to target player
        target.sendSystemMessage(Component.literal(TextUtil.colorize(
                "&aYou have been unmuted.")));
        
        // Announce unmute to source
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                "&aPlayer &e" + target.getScoreboardName() + " &ahas been unmuted.")), true);
                
        // Log unmute action
        NeoEssentials.LOGGER.info("{} unmuted {}", 
                context.getSource().getTextName(), target.getScoreboardName());
        
        return 1;
    }
    
    /**
<<<<<<< HEAD
     * Check if a player is muted
     * 
     * @param player The player to check
     * @return True if the player is muted, false otherwise
     */
    public boolean isPlayerMuted(ServerPlayer player) {
        if (player == null) return false;
        
        UUID uuid = player.getUUID();
        if (!mutedPlayers.containsKey(uuid)) return false;
        
        Date expiry = mutedPlayers.get(uuid);
        
        // If expiry is null, the mute is permanent
        if (expiry == null) return true;
        
        // Check if the mute has expired
        if (expiry.before(new Date())) {
            // Mute has expired, remove it
            mutedPlayers.remove(uuid);
=======
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
     * Check if a player is muted
     * 
     * @param player The player to check
     * @return True if the player is muted, false otherwise
     */
    public boolean isPlayerMuted(ServerPlayer player) {
        if (player == null) return false;
        
        UUID uuid = player.getUUID();
        if (!mutedPlayers.containsKey(uuid)) return false;
        
<<<<<<< HEAD
        if (expiryTime.before(new Date())) {
            // Mute expired, remove it
            mutedPlayers.remove(playerId);
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
        Date expiry = mutedPlayers.get(uuid);
        
        // If expiry is null, the mute is permanent
        if (expiry == null) return true;
        
        // Check if the mute has expired
        if (expiry.before(new Date())) {
            // Mute has expired, remove it
            mutedPlayers.remove(uuid);
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
            return false;
        }
        
        return true;
    }
    
    /**
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
     * Get the mute expiry date for a player
     * 
     * @param player The player to check
     * @return The mute expiry date, or null if not muted or muted permanently
<<<<<<< HEAD
     */
    public Date getMuteExpiry(ServerPlayer player) {
        if (player == null) return null;
        return mutedPlayers.get(player.getUUID());
    }
    
    /**
     * List all active bans (players or IPs)
     */
    private int listBans(CommandContext<CommandSourceStack> context, String type) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer player = null;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // Source is not a player (e.g. console)
        }
        
        if ("players".equals(type)) {
            UserBanList banList = server.getPlayerList().getBans();
            List<UserBanListEntry> bans = new ArrayList<>(banList.getEntries());
            
            if (bans.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&eNo players are currently banned.")), false);
                return 1;
            }
            
            int pageSize = 8;
            int totalPages = (int) Math.ceil((double) bans.size() / pageSize);
            
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&e===== &6Banned Players &e(Page 1/" + totalPages + ") =====")), false);
                    
            for (int i = 0; i < Math.min(pageSize, bans.size()); i++) {
                UserBanListEntry ban = bans.get(i);
                
                MutableComponent banEntry = Component.literal(TextUtil.colorize("&7- &c" + ban.getDisplayName()));
                
                // Add hover details
                String hoverText = "&eReason: &f" + ban.getReason() + "\n" +
                                  "&eExpires: &f" + (ban.getExpires() != null ? formatDate(ban.getExpires()) : "Never") + "\n" +
                                  "&eCreated: &f" + formatDate(ban.getCreated());
                
                banEntry = banEntry.withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal(TextUtil.colorize(hoverText)))));
                
                // Add clickable unban option if player viewing
                if (player != null && PermissionUtil.hasPermission(player, "essentials.unban")) {
                    banEntry = banEntry.append(Component.literal(TextUtil.colorize(" &7[&cUnban&7]"))
                            .withStyle(Style.EMPTY
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                                            "/unban " + ban.getDisplayName()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                            Component.literal(TextUtil.colorize("&eClick to unban &f" + ban.getDisplayName()))))));
                }
                
                final Component finalBanEntry = banEntry;
                context.getSource().sendSuccess(() -> finalBanEntry, false);
            }
            
            // If there are more pages, show navigation
            if (totalPages > 1) {
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&7Use &e/banlist players <page> &7to view more bans.")), false);
            }
            
        } else if ("ips".equals(type)) {
            IpBanList ipBanList = server.getPlayerList().getIpBans();
            List<IpBanListEntry> ipBans = new ArrayList<>(ipBanList.getEntries());
            
            if (ipBans.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&eNo IP addresses are currently banned.")), false);
                return 1;
            }
            
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&e===== &6Banned IP Addresses &e(Total: " + ipBans.size() + ") =====")), false);            for (int i = 0; i < Math.min(5, ipBans.size()); i++) {
                IpBanListEntry ban = ipBans.get(i);
                String ipAddress = ban.toString(); // Use toString() which typically returns the IP address
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&7- &c" + ipAddress + " &7(Reason: &f" + ban.getReason() + "&7)")), false);
            }
            
            if (ipBans.size() > 5) {
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&7And " + (ipBans.size() - 5) + " more...")), false);
            }
        }
        
        return 1;
    }
    
    // Helper method to format dates nicely
    private String formatDate(Date date) {
        if (date == null) return "Unknown";
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
=======
     * Get the mute expiry time for a player
     */
    public Date getMuteExpiryTime(UUID playerId) {
        return mutedPlayers.get(playerId);
    }
    
    /**
     * Unmute a player
     */
    public void unmutePlayer(UUID playerId) {
        mutedPlayers.remove(playerId);
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
     */
    public Date getMuteExpiry(ServerPlayer player) {
        if (player == null) return null;
        return mutedPlayers.get(player.getUUID());
>>>>>>> 4b34e9e (Enhance player commands with improved functionality and suggestions)
    }
    
    /**
     * List all active bans (players or IPs)
     */
    private int listBans(CommandContext<CommandSourceStack> context, String type) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer player = null;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // Source is not a player (e.g. console)
        }
        
        if ("players".equals(type)) {
            UserBanList banList = server.getPlayerList().getBans();
            List<UserBanListEntry> bans = new ArrayList<>(banList.getEntries());
            
            if (bans.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&eNo players are currently banned.")), false);
                return 1;
            }
            
            int pageSize = 8;
            int totalPages = (int) Math.ceil((double) bans.size() / pageSize);
            
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&e===== &6Banned Players &e(Page 1/" + totalPages + ") =====")), false);
                    
            for (int i = 0; i < Math.min(pageSize, bans.size()); i++) {
                UserBanListEntry ban = bans.get(i);
                
                MutableComponent banEntry = Component.literal(TextUtil.colorize("&7- &c" + ban.getDisplayName()));
                
                // Add hover details
                String hoverText = "&eReason: &f" + ban.getReason() + "\n" +
                                  "&eExpires: &f" + (ban.getExpires() != null ? formatDate(ban.getExpires()) : "Never") + "\n" +
                                  "&eCreated: &f" + formatDate(ban.getCreated());
                
                banEntry = banEntry.withStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal(TextUtil.colorize(hoverText)))));
                
                // Add clickable unban option if player viewing
                if (player != null && PermissionUtil.hasPermission(player, "essentials.unban")) {
                    banEntry = banEntry.append(Component.literal(TextUtil.colorize(" &7[&cUnban&7]"))
                            .withStyle(Style.EMPTY
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                                            "/unban " + ban.getDisplayName()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                            Component.literal(TextUtil.colorize("&eClick to unban &f" + ban.getDisplayName()))))));
                }
                
                context.getSource().sendSuccess(() -> banEntry, false);
            }
            
            // If there are more pages, show navigation
            if (totalPages > 1) {
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&7Use &e/banlist players <page> &7to view more bans.")), false);
            }
            
        } else if ("ips".equals(type)) {
            IpBanList ipBanList = server.getPlayerList().getIpBans();
            List<IpBanListEntry> ipBans = new ArrayList<>(ipBanList.getEntries());
            
            if (ipBans.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&eNo IP addresses are currently banned.")), false);
                return 1;
            }
            
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                    "&e===== &6Banned IP Addresses &e(Total: " + ipBans.size() + ") =====")), false);
                      for (int i = 0; i < Math.min(5, ipBans.size()); i++) {
                IpBanListEntry ban = ipBans.get(i);
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&7- &c" + ban.getUser() + " &7(Reason: &f" + ban.getReason() + "&7)")), false);
            }
            
            if (ipBans.size() > 5) {
                context.getSource().sendSuccess(() -> Component.literal(TextUtil.colorize(
                        "&7And " + (ipBans.size() - 5) + " more...")), false);
            }
        }
        
        return 1;
    }
    
    // Helper method to format dates nicely
    private String formatDate(Date date) {
        if (date == null) return "Unknown";
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
