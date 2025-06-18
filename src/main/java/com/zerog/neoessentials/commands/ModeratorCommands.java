package com.zerog.neoessentials.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
<<<<<<< HEAD
import com.mojang.brigadier.suggestion.SuggestionProvider;
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.TextUtil;
import com.zerog.neoessentials.utils.TimeUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
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
 */
public class ModeratorCommands {

    // Store muted players with expiry time
    private final Map<UUID, Date> mutedPlayers = new ConcurrentHashMap<>();
    
<<<<<<< HEAD
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
    
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
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
                    .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
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
                    .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
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
                    .suggests(TIME_DURATION_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
                    .executes(context -> tempBanPlayer(
                        context, 
                        StringArgumentType.getString(context, "time"),
                        "Temporarily banned by admin"
                    ))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
<<<<<<< HEAD
                        .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
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
                    .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
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
                    .suggests(TIME_DURATION_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
                    .executes(context -> mutePlayer(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "time"),
                        "Muted by admin"
                    ))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
<<<<<<< HEAD
                        .suggests(REASON_SUGGESTIONS)
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
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
=======
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
    }
    
    /**
     * Kick a player with a reason
     */
    private int kickPlayer(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
<<<<<<< HEAD
        ServerPlayer source = null;
        try {
            source = context.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // Source is not a player (e.g. console)
        }
        
        // Check for kick exemption
        if (PermissionUtil.hasPermission(player, "essentials.kick.exempt")) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cYou cannot kick this player.")));
=======
        
        if (PermissionUtil.hasPermission(player, "essentials.kick.exempt")) {
            context.getSource().sendFailure(Component.literal("You cannot kick this player."));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return 0;
        }
        
        try {
<<<<<<< HEAD
            String formattedReason = TextUtil.colorize(reason);
            
            // Create a styled kick message
            Component kickMessage = Component.literal(TextUtil.colorize("&c&lYou have been kicked from the server!\n\n"))
                    .append(Component.literal(TextUtil.colorize("&7Reason: &f" + formattedReason + "\n")));
                      // Add kicked by information if source is a player
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
            
            // Broadcast kick message
            MinecraftServer server = context.getSource().getServer();
            server.getPlayerList().broadcastSystemMessage(
                Component.literal(player.getScoreboardName() + " was kicked: " + formattedReason),
                false
            );
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to kick player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return 0;
        }
    }
    
    /**
     * Ban a player with a reason
<<<<<<< HEAD
     */    
=======
     */
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
    private int banPlayer(CommandContext<CommandSourceStack> context, String reason) {
        try {
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "player");
            
            if (targets.isEmpty()) {
<<<<<<< HEAD
                context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cNo players specified.")));
=======
                context.getSource().sendFailure(Component.literal("No players specified."));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
                return 0;
            }
            
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
            int count = 0;
            
            for (GameProfile profile : targets) {
<<<<<<< HEAD
                if (profile == null) continue;
                
                // Check for ban exemption
                ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
                if (targetPlayer != null && PermissionUtil.hasPermission(targetPlayer, "essentials.ban.exempt")) {
                    context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                            "&cYou cannot ban &e" + profile.getName() + " &cas they are exempt from bans.")));
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
                    context.getSource().sendFailure(Component.literal("You cannot ban " + profile.getName() + "."));
                    continue;
                }
                
                UserBanListEntry banEntry = new UserBanListEntry(
                    profile,
                    new Date(),
                    context.getSource().getTextName(),
                    null,
                    reason
                );
                
                banList.add(banEntry);
                
                // Kick the player if online
                ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
                if (player != null) {
                    player.connection.disconnect(Component.literal("Banned: " + reason));
                }
                
                count++;
            }
            
            if (count > 0) {
                context.getSource().sendSuccess(() -> Component.literal("Banned " + count + " players: " + reason), true);
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            }
            
            return count;
        } catch (Exception e) {
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to ban player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error banning player", e);
=======
            context.getSource().sendFailure(Component.literal("Failed to ban player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return 0;
        }
    }
    
    /**
     * Unban a player
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
            context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                    "&cPlayer &e" + playerName + " &cis not banned.")));
            return 0;
        }
        
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
        try {
            String targetName = StringArgumentType.getString(context, "player");
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
            
            // We need to find the game profile in the ban list
            boolean found = false;
            
            // Iterate through banned users to find matching name
            for (UserBanListEntry entry : banList.getEntries()) {
                if (entry.getUser().getName().equalsIgnoreCase(targetName)) {
                    banList.remove(entry.getUser());
                    found = true;
                    break;
                }
            }
            
            if (found) {
                context.getSource().sendSuccess(() -> Component.literal("Unbanned player: " + targetName), true);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("Player not found in ban list: " + targetName));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to unban player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return 0;
        }
    }
    
    /**
<<<<<<< HEAD
     * Temporarily ban a player
=======
     * Temporarily ban a player for a specific duration
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
     */
    private int tempBanPlayer(CommandContext<CommandSourceStack> context, String timeStr, String reason) {
        try {
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "player");
            
            if (targets.isEmpty()) {
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
                return 0;
            }
            
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
            int count = 0;
            
            for (GameProfile profile : targets) {
<<<<<<< HEAD
                if (profile == null) continue;
                
                // Check for ban exemption
                ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
                if (targetPlayer != null && PermissionUtil.hasPermission(targetPlayer, "essentials.tempban.exempt")) {
                    context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                            "&cYou cannot ban &e" + profile.getName() + " &cas they are exempt from bans.")));
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
                    context.getSource().sendFailure(Component.literal("You cannot temp ban " + profile.getName() + "."));
                    continue;
                }
                
                UserBanListEntry banEntry = new UserBanListEntry(
                    profile,
                    new Date(),
                    context.getSource().getTextName(),
                    expires,
                    reason + " (until " + TimeUtil.formatDate(expires) + ")"
                );
                
                banList.add(banEntry);
                
                // Kick the player if online
                ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
                if (player != null) {
                    player.connection.disconnect(Component.literal("Temporarily banned until " + TimeUtil.formatDate(expires) + ": " + reason));
                }
                
                count++;
            }
            
            if (count > 0) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("Temporarily banned " + count + " players until " + 
                                 TimeUtil.formatDate(expires) + ": " + reason), true);
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            }
            
            return count;
        } catch (Exception e) {
<<<<<<< HEAD
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cFailed to temp-ban player: " + e.getMessage())));
            NeoEssentials.LOGGER.error("Error temp-banning player", e);
=======
            context.getSource().sendFailure(Component.literal("Failed to temp ban player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return 0;
        }
    }
    
    /**
     * Ban an IP address
     */
    private int banIp(CommandContext<CommandSourceStack> context, String reason) {
<<<<<<< HEAD
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
                
                if (PermissionUtil.hasPermission(targetPlayer, "essentials.banip.exempt")) {
                    context.getSource().sendFailure(Component.literal("You cannot ban this player's IP."));
                    return 0;
                }
                
                ipAddress = targetPlayer.getIpAddress();
                if (ipAddress == null || ipAddress.isEmpty()) {
                    context.getSource().sendFailure(Component.literal("Could not determine player's IP address."));
                    return 0;
                }
            }
            
            // Ban the IP
            IpBanList ipBanList = context.getSource().getServer().getPlayerList().getIpBans();
            IpBanListEntry banEntry = new IpBanListEntry(
                ipAddress,
                new Date(),
                context.getSource().getTextName(),
                null,
                reason
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            );
            
            ipBanList.add(banEntry);
            
            // Kick all players with this IP
<<<<<<< HEAD
            int kickedCount = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ipAddress.equals(player.getIpAddress())) {
                    // Create a styled IP ban message
                    Component banMessage = Component.literal(TextUtil.colorize("&c&lYour IP address has been banned!\n\n"))
                            .append(Component.literal(TextUtil.colorize("&7Reason: &f" + reason + "\n")))
                            .append(Component.literal(TextUtil.colorize("&7Banned by: &f" + context.getSource().getTextName())));
                    
                    player.connection.disconnect(banMessage);
                    kickedCount++;
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
                }
            }
            
            for (ServerPlayer player : playersToKick) {
                player.connection.disconnect(Component.literal("Your IP address has been banned: " + reason));
            }
            
            context.getSource().sendSuccess(() -> Component.literal("Banned IP address: " + ipAddress), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to ban IP: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return 0;
        }
    }
    
    /**
     * Unban an IP address
<<<<<<< HEAD
     */
    private int unbanIp(CommandContext<CommandSourceStack> context) {
<<<<<<< HEAD
        String ipAddress = StringArgumentType.getString(context, "address");
        IpBanList ipBanList = context.getSource().getServer().getPlayerList().getIpBans();
        
        // Check if the IP is banned
        if (!ipBanList.isBanned(ipAddress)) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize(
                    "&cIP address &e" + ipAddress + " &cis not banned.")));
            return 0;
        }
        
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
        try {
            String ipAddress = StringArgumentType.getString(context, "address");
            IpBanList ipBanList = context.getSource().getServer().getPlayerList().getIpBans();
            
            // Check if the IP is banned before removal
            if (ipBanList.isBanned(ipAddress)) {
                ipBanList.remove(ipAddress);
                context.getSource().sendSuccess(() -> Component.literal("Unbanned IP address: " + ipAddress), true);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("IP address not found in ban list: " + ipAddress));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to unban IP: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return 0;
        }
    }
    
    /**
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
     */
    private int mutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer player, String timeStr, String reason) {
        try {
            if (PermissionUtil.hasPermission(player, "essentials.mute.exempt")) {
                context.getSource().sendFailure(Component.literal("You cannot mute this player."));
                return 0;
            }
            
            Date expires = null;
            if (timeStr != null && !timeStr.isEmpty()) {
                try {
                    expires = TimeUtil.parseTimeSpecification(timeStr);
                } catch (IllegalArgumentException e) {
                    context.getSource().sendFailure(
                        Component.literal("Invalid time format. Use formats like '1d2h30m' for 1 day, 2 hours, 30 minutes.")
                    );
                    return 0;
                }
            }
            
            // Store in the muted players map
            mutedPlayers.put(player.getUUID(), expires);
            
            if (expires != null) {
                context.getSource().sendSuccess(() -> Component.literal(
                    "Muted " + player.getScoreboardName() + " until " + TimeUtil.formatDate(expires) + ": " + reason
                ), true);
                
                player.sendSystemMessage(Component.literal(
                    "You have been muted until " + TimeUtil.formatDate(expires) + ": " + reason
                ));
            } else {
                context.getSource().sendSuccess(() -> Component.literal(
                    "Permanently muted " + player.getScoreboardName() + ": " + reason
                ), true);
                
                player.sendSystemMessage(Component.literal(
                    "You have been permanently muted: " + reason
                ));
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to mute player: " + e.getMessage()));
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return 0;
        }
    }
    
    /**
<<<<<<< HEAD
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
     * Check if a player is muted
     */
    public boolean isPlayerMuted(UUID playerId) {
        if (!mutedPlayers.containsKey(playerId)) {
            return false;
        }
        
        Date expiryTime = mutedPlayers.get(playerId);
        if (expiryTime == null) {
            // Permanent mute
            return true;
        }
        
        if (expiryTime.before(new Date())) {
            // Mute expired, remove it
            mutedPlayers.remove(playerId);
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
            return false;
        }
        
        return true;
    }
    
    /**
<<<<<<< HEAD
     * Get the mute expiry date for a player
     * 
     * @param player The player to check
     * @return The mute expiry date, or null if not muted or muted permanently
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
    }
}
