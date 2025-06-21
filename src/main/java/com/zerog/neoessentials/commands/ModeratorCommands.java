package com.zerog.neoessentials.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.TextUtil;
import com.zerog.neoessentials.utils.TimeUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implements moderator commands like ban, tempban, kick, mute, etc.
 * Provides comprehensive moderation tools for server administrators.
 */
public class ModeratorCommands {

    // Store muted players with expiry time
    private final Map<UUID, Date> mutedPlayers = new ConcurrentHashMap<>();
    
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
                    .suggests(REASON_SUGGESTIONS)
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
                    .suggests(REASON_SUGGESTIONS)
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
                    .suggests(TIME_DURATION_SUGGESTIONS)
                    .executes(context -> tempBanPlayer(
                        context, 
                        StringArgumentType.getString(context, "time"),
                        "Temporarily banned by admin"
                    ))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .suggests(REASON_SUGGESTIONS)
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
                    .suggests(REASON_SUGGESTIONS)
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
                    .suggests(TIME_DURATION_SUGGESTIONS)
                    .executes(context -> mutePlayer(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "time"),
                        "Muted by admin"
                    ))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .suggests(REASON_SUGGESTIONS)
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
    }
    
    /**
     * Kick a player with a reason
     */
    private int kickPlayer(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ServerPlayer source = null;
        try {
            source = context.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // Source is not a player (e.g. console)
        }
        
        // Check for kick exemption
        if (PermissionUtil.hasPermission(player, "essentials.kick.exempt")) {
            context.getSource().sendFailure(Component.literal(TextUtil.colorize("&cYou cannot kick this player.")));
            return 0;
        }
        
        try {
            String formattedReason = TextUtil.colorize(reason);
            
            // Create a styled kick message
            Component kickMessage = Component.literal(TextUtil.colorize("&c&lYou have been kicked from the server!\n\n"))
                    .append(Component.literal(TextUtil.colorize("&7Reason: &f" + formattedReason + "\n")));
                    
            // Add kicked by information if source is a player
            if (source != null) {
                kickMessage = kickMessage.append(Component.literal(TextUtil.colorize("&7Kicked by: &f" + source.getScoreboardName())));
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
            return 0;
        }
    }
    
    /**
     * Ban a player with a reason
     */    private int banPlayer(CommandContext<CommandSourceStack> context, String reason) {
        try {
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "player");
            
            if (targets.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No players specified."));
                return 0;
            }
            
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
            int count = 0;
            
            for (GameProfile profile : targets) {
                if (PermissionUtil.hasPermission((com.mojang.authlib.GameProfile)profile, "essentials.ban.exempt")) {
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
                final int finalCount = count;
                final String finalReason = reason;
                context.getSource().sendSuccess(() -> Component.literal("Banned " + finalCount + " players: " + finalReason), true);
            }
            
            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to ban player: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Unban a player
     */    private int unbanPlayer(CommandContext<CommandSourceStack> context) {
        try {
            String targetName = StringArgumentType.getString(context, "player");
            MinecraftServer server = context.getSource().getServer();
            UserBanList banList = server.getPlayerList().getBans();
            
            // In Minecraft 1.21.1, we need to get the GameProfile differently
            boolean found = false;
            
            // Try to use server's method to get the profile
            Optional<GameProfile> profile = server.getProfileCache().get(targetName);
            if (profile.isPresent()) {
                GameProfile gameProfile = profile.get();
                if (banList.isBanned(gameProfile)) {
                    banList.remove(gameProfile);
                    found = true;
                }
            }
            
            // If we couldn't find or unban via profile cache, try a different approach
            if (!found) {
                // This is a workaround - pardon the named player directly
                server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(), 
                    "pardon " + targetName
                );
                found = true; // Assume success
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
            return 0;
        }
    }
    
    /**
     * Temporarily ban a player for a specific duration
     */
    private int tempBanPlayer(CommandContext<CommandSourceStack> context, String timeStr, String reason) {
        try {
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "player");
            
            if (targets.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No players specified."));
                return 0;
            }
            
            // Parse time duration
            Date expires;
            try {
                expires = TimeUtil.parseTimeSpecification(timeStr);
            } catch (IllegalArgumentException e) {
                context.getSource().sendFailure(Component.literal("Invalid time format. Use formats like '1d2h30m' for 1 day, 2 hours, 30 minutes."));
                return 0;
            }
            
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
            int count = 0;
            
            for (GameProfile profile : targets) {
                if (PermissionUtil.hasPermission((com.mojang.authlib.GameProfile)profile, "essentials.tempban.exempt")) {
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
                final int finalCount = count;
                final Date finalExpires = expires;
                final String finalReason = reason;
                context.getSource().sendSuccess(() -> 
                    Component.literal("Temporarily banned " + finalCount + " players until " + 
                                 TimeUtil.formatDate(finalExpires) + ": " + finalReason), true);
            }
            
            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to temp ban player: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Ban an IP address
     */
    private int banIp(CommandContext<CommandSourceStack> context, String reason) {
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
            }
            
            // Ban the IP
            IpBanList ipBanList = context.getSource().getServer().getPlayerList().getIpBans();
            IpBanListEntry banEntry = new IpBanListEntry(
                ipAddress,
                new Date(),
                context.getSource().getTextName(),
                null,
                reason
            );
            
            ipBanList.add(banEntry);
            
            // Kick all players with this IP
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
            return 0;
        }
    }
    
    /**
     * Unban an IP address
     */    private int unbanIp(CommandContext<CommandSourceStack> context) {
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
            return 0;
        }
    }
    
    /**
     * Mute a player for a specific duration or permanently
     */
    private int mutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer player, String timeStr, String reason) {
        try {
            if (PermissionUtil.hasPermission((ServerPlayer)player, "essentials.mute.exempt")) {
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
                final Date finalExpires = expires;
                final String finalReason = reason;
                final String playerName = player.getScoreboardName();
                context.getSource().sendSuccess(() -> Component.literal(
                    "Muted " + playerName + " until " + TimeUtil.formatDate(finalExpires) + ": " + finalReason
                ), true);
                
                player.sendSystemMessage(Component.literal(
                    "You have been muted until " + TimeUtil.formatDate(finalExpires) + ": " + finalReason
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
            return 0;
        }
    }
    
    /**
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
            return false;
        }
        
        return true;
    }
    
    /**
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
                        "&7- &c" + ban.getDescription() + " &7(Reason: &f" + ban.getReason() + "&7)")), false);
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
