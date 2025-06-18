package com.zerog.neoessentials.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.TextUtil;
import com.zerog.neoessentials.utils.TimeUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Implements moderator commands like ban, tempban, kick, mute, etc.
 */
public class ModeratorCommands {

    // Store muted players with expiry time
    private final Map<UUID, Date> mutedPlayers = new ConcurrentHashMap<>();
    
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
                    .executes(context -> tempBanPlayer(
                        context, 
                        StringArgumentType.getString(context, "time"),
                        "Temporarily banned by admin"
                    ))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
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
                    .executes(context -> mutePlayer(
                        context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "time"),
                        "Muted by admin"
                    ))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
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
    }
    
    /**
     * Kick a player with a reason
     */
    private int kickPlayer(CommandContext<CommandSourceStack> context, String reason) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        
        if (PermissionUtil.hasPermission(player, "essentials.kick.exempt")) {
            context.getSource().sendFailure(Component.literal("You cannot kick this player."));
            return 0;
        }
        
        try {
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
            return 0;
        }
    }
    
    /**
     * Ban a player with a reason
     */
    private int banPlayer(CommandContext<CommandSourceStack> context, String reason) {
        try {
            Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(context, "player");
            
            if (targets.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No players specified."));
                return 0;
            }
            
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
            int count = 0;
            
            for (GameProfile profile : targets) {
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
            }
            
            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to ban player: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Unban a player
     */
    private int unbanPlayer(CommandContext<CommandSourceStack> context) {
        try {
            String targetName = StringArgumentType.getString(context, "player");
            UserBanList banList = context.getSource().getServer().getPlayerList().getBans();
              // Use removeByName instead of iterating through the entries
            boolean found = false;
            try {
                // Try to unban by name
                if (banList.isBanned(targetName)) {
                    banList.remove(targetName);
                    found = true;
                }
            } catch (Exception ex) {
                // Some versions might not support isBanned by name
                NeoEssentials.LOGGER.error("Error checking ban by name", ex);
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
}
