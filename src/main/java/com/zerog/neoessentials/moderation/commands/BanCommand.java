package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.moderation.BanManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Ban system commands: /ban, /tempban, /banip, /unban, /unbanip, /banlist
 */
public class BanCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanCommand.class);
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_BANNED_PLAYERS = (ctx, builder) -> {
        BanManager banManager = BanManager.getInstance();
        return SharedSuggestionProvider.suggest(
            banManager.getAllPlayerBans().stream()
                .map(ban -> ban.playerName)
                .toList(),
            builder
        );
    };
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_BANNED_IPS = (ctx, builder) -> {
        BanManager banManager = BanManager.getInstance();
        return SharedSuggestionProvider.suggest(
            banManager.getAllIPBans().stream()
                .map(ban -> ban.ipAddress)
                .toList(),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if moderation commands are enabled
        if (!ConfigManager.isModerationEnabled()) {
            return;
        }
        
        // /ban <player> [reason]
        if (ConfigManager.getInstance().isCommandEnabled("ban")) {
        dispatcher.register(Commands.literal("ban")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.ban").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .executes(ctx -> executeBan(ctx, StringArgumentType.getString(ctx, "player"),
                    com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultBanReason()))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeBan(ctx,
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "reason")))))
        );
        }

        // /tempban <player> <duration> [reason]
        if (ConfigManager.getInstance().isCommandEnabled("tempban")) {
        dispatcher.register(Commands.literal("tempban")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.tempban").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .then(Commands.argument("duration", StringArgumentType.word())
                    .executes(ctx -> executeTempBan(ctx,
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "duration"),
                        com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultBanReason()))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> executeTempBan(ctx,
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "duration"),
                            StringArgumentType.getString(ctx, "reason"))))))
        );
        }

        // /banip <ip> [reason]
        if (ConfigManager.getInstance().isCommandEnabled("banip")) {
        dispatcher.register(Commands.literal("banip")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.banip").hasPermission())
            .then(Commands.argument("ip", StringArgumentType.word())
                .executes(ctx -> executeBanIP(ctx,
                    StringArgumentType.getString(ctx, "ip"), com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultBanReason()))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeBanIP(ctx,
                        StringArgumentType.getString(ctx, "ip"),
                        StringArgumentType.getString(ctx, "reason")))))
        );
        }

        // /unban <player>
        if (ConfigManager.getInstance().isCommandEnabled("unban")) {
        dispatcher.register(Commands.literal("unban")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.unban").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(SUGGEST_BANNED_PLAYERS)
                .executes(ctx -> executeUnban(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        }

        // /unbanip <ip>
        if (ConfigManager.getInstance().isCommandEnabled("unbanip")) {
        dispatcher.register(Commands.literal("unbanip")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.unbanip").hasPermission())
            .then(Commands.argument("ip", StringArgumentType.word())
                .suggests(SUGGEST_BANNED_IPS)
                .executes(ctx -> executeUnbanIP(ctx, StringArgumentType.getString(ctx, "ip"))))
        );
        }

        // /banlist [players|ips]
        if (ConfigManager.getInstance().isCommandEnabled("banlist")) {
        dispatcher.register(Commands.literal("banlist")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.banlist").hasPermission())
            .executes(ctx -> executeBanList(ctx, "players"))
            .then(Commands.literal("players")
                .executes(ctx -> executeBanList(ctx, "players")))
            .then(Commands.literal("ips")
                .executes(ctx -> executeBanList(ctx, "ips")))
        );
        }

        // /tempbanip <ip> <duration> [reason]  — Essentials: Commandtempbanip
        if (ConfigManager.getInstance().isCommandEnabled("tempbanip")) {
        dispatcher.register(Commands.literal("tempbanip")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.tempbanip").hasPermission())
            .then(Commands.argument("ip", StringArgumentType.word())
                .then(Commands.argument("duration", StringArgumentType.word())
                    .executes(ctx -> executeTempBanIP(ctx,
                        StringArgumentType.getString(ctx, "ip"),
                        StringArgumentType.getString(ctx, "duration"),
                        com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultBanReason()))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> executeTempBanIP(ctx,
                            StringArgumentType.getString(ctx, "ip"),
                            StringArgumentType.getString(ctx, "duration"),
                            StringArgumentType.getString(ctx, "reason"))))))
        );
        }
    }

    private static int executeBan(CommandContext<CommandSourceStack> ctx, String playerName, String reason) {
        CommandSourceStack source = ctx.getSource();
        String bannedBy = getCommandSender(source);
        
        try {
            BanManager banManager = BanManager.getInstance();
            
            // Resolve player UUID
            MinecraftServer server = source.getServer();
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
            
            // Ban the player
            boolean success = banManager.banPlayer(resolvedName, playerId, reason, bannedBy);
            
            if (success) {
                String message = MessageUtil.localize("neoessentials.moderation.ban_success", resolvedName, reason);
                source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                
                // Broadcast ban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.ban_broadcast", 
                    resolvedName, bannedBy, reason), senderId(source));
                
                LOGGER.info("Player {} banned by {} for: {}", resolvedName, bannedBy, reason);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.ban_failed", resolvedName);
                source.sendFailure(MessageUtil.coloredText(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing ban command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the ban command."));
            return 0;
        }
    }
    
    private static int executeTempBan(CommandContext<CommandSourceStack> ctx, String playerName, String duration, String reason) {
        CommandSourceStack source = ctx.getSource();
        String bannedBy = getCommandSender(source);
        String durationStr = duration;
        
        try {
            BanManager banManager = BanManager.getInstance();
            
            // Parse duration
            long durationMillis = BanManager.parseDuration(duration);
            if (durationMillis <= 0) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.invalid_duration", duration));
                return 0;
            }
            
            // Resolve player UUID
            MinecraftServer server = source.getServer();
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
            
            // Temp ban the player
            boolean success = banManager.tempBanPlayer(resolvedName, playerId, reason, bannedBy, durationMillis);
            
            if (success) {
                String message = MessageUtil.localize("neoessentials.moderation.tempban_success", resolvedName, durationStr, reason);
                source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                
                // Broadcast ban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.tempban_broadcast", 
                    resolvedName, durationStr, bannedBy, reason), senderId(source));
                
                LOGGER.info("Player {} temp banned by {} for {} - Reason: {}", resolvedName, bannedBy, durationStr, reason);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.ban_failed", resolvedName);
                source.sendFailure(MessageUtil.coloredText(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing tempban command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the tempban command."));
            return 0;
        }
    }
    
    private static int executeTempBanIP(CommandContext<CommandSourceStack> ctx, String ipAddress, String durationStr, String reason) {
        CommandSourceStack source = ctx.getSource();
        String bannedBy = getCommandSender(source);
        try {
            BanManager banManager = BanManager.getInstance();
            MinecraftServer server = source.getServer();

            // Parse duration using BanManager's built-in parser
            long durationMillis = BanManager.parseDuration(durationStr);
            if (durationMillis <= 0) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.invalid_duration", durationStr));
                return 0;
            }

            // Validate IP format
            if (!isValidIP(ipAddress)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.invalid_ip", ipAddress));
                return 0;
            }

            boolean success = banManager.tempBanIP(ipAddress, reason, bannedBy, durationMillis);
            if (success) {
                String formattedDur = BanManager.formatDuration(durationMillis);
                String msg = MessageUtil.localize("neoessentials.moderation.tempbanip_success", ipAddress, formattedDur, reason);
                source.sendSuccess(() -> MessageUtil.coloredText(msg), false);
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.tempbanip_broadcast",
                    ipAddress, formattedDur, bannedBy, reason), senderId(source));
                LOGGER.info("IP {} temp-banned by {} for {} - Reason: {}", ipAddress, bannedBy, formattedDur, reason);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.banip_failed", ipAddress));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error executing tempbanip command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the tempbanip command."));
            return 0;
        }
    }

    private static int executeBanIP(CommandContext<CommandSourceStack> ctx, String ipAddress, String reason) {
        CommandSourceStack source = ctx.getSource();
        String bannedBy = getCommandSender(source);
        
        try {
            BanManager banManager = BanManager.getInstance();
            MinecraftServer server = source.getServer();
            
            // Validate IP format (basic validation)
            if (!isValidIP(ipAddress)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.invalid_ip", ipAddress));
                return 0;
            }
            
            // Ban the IP
            boolean success = banManager.banIP(ipAddress, reason, bannedBy);
            
            if (success) {
                String message = MessageUtil.localize("neoessentials.moderation.banip_success", ipAddress, reason);
                source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                
                // Broadcast ban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.banip_broadcast", 
                    ipAddress, bannedBy, reason), senderId(source));
                
                LOGGER.info("IP {} banned by {} for: {}", ipAddress, bannedBy, reason);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.banip_failed", ipAddress);
                source.sendFailure(MessageUtil.coloredText(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing banip command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the banip command."));
            return 0;
        }
    }
    
    private static int executeUnban(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();
        String unbannedBy = getCommandSender(source);
        
        try {
            BanManager banManager = BanManager.getInstance();
            MinecraftServer server = source.getServer();
            
            // Try to resolve player UUID
            UUID playerId = null;
            String resolvedName = playerName;
            
            // First check if it's a banned player
            List<BanManager.BanEntry> allBans = banManager.getAllPlayerBans();
            LOGGER.info("Checking {} active bans for player '{}'", allBans.size(), playerName);
            
            for (BanManager.BanEntry ban : allBans) {
                LOGGER.debug("Checking ban: {} (UUID: {})", ban.playerName, ban.playerId);
                if (ban.playerName.equalsIgnoreCase(playerName)) {
                    playerId = ban.playerId;
                    resolvedName = ban.playerName;
                    LOGGER.info("Found banned player: {} with UUID {}", resolvedName, playerId);
                    break;
                }
            }
            
            // If not found in bans, try player cache
            if (playerId == null) {
                LOGGER.info("Player '{}' not found in ban list, checking player cache", playerName);
                var profile = server.getProfileCache().get(playerName);
                if (profile.isPresent()) {
                    playerId = profile.get().getId();
                    resolvedName = profile.get().getName();
                    LOGGER.info("Found player in cache: {} with UUID {}", resolvedName, playerId);
                }
            }
            
            if (playerId == null) {
                LOGGER.warn("Could not find player '{}' in bans or player cache", playerName);
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }
            
            // Check if player is actually banned before trying to unban
            if (!banManager.isPlayerBanned(playerId)) {
                LOGGER.info("Player {} ({}) is not currently banned", resolvedName, playerId);
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_banned", resolvedName));
                return 0;
            }
            
            // Unban the player
            boolean success = banManager.unbanPlayer(playerId, unbannedBy);
            
            if (success) {
                String message = MessageUtil.localize("neoessentials.moderation.unban_success", resolvedName);
                source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                
                // Broadcast unban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unban_broadcast", 
                    resolvedName, unbannedBy), senderId(source));
                
                LOGGER.info("Player {} unbanned by {}", resolvedName, unbannedBy);
                return 1;
            } else {
                LOGGER.error("Failed to unban player {} ({}): unbanPlayer returned false", resolvedName, playerId);
                String message = MessageUtil.localize("neoessentials.moderation.unban_failed", resolvedName);
                source.sendFailure(MessageUtil.coloredText(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing unban command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the unban command."));
            return 0;
        }
    }
    
    private static int executeUnbanIP(CommandContext<CommandSourceStack> ctx, String ipAddress) {
        CommandSourceStack source = ctx.getSource();
        String unbannedBy = getCommandSender(source);
        
        try {
            BanManager banManager = BanManager.getInstance();
            MinecraftServer server = source.getServer();
            
            // Unban the IP
            boolean success = banManager.unbanIP(ipAddress, unbannedBy);
            
            if (success) {
                String message = MessageUtil.localize("neoessentials.moderation.unbanip_success", ipAddress);
                source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                
                // Broadcast unban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unbanip_broadcast", 
                    ipAddress, unbannedBy), senderId(source));
                
                LOGGER.info("IP {} unbanned by {}", ipAddress, unbannedBy);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.unbanip_failed", ipAddress);
                source.sendFailure(MessageUtil.coloredText(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing unbanip command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the unbanip command."));
            return 0;
        }
    }
    
    private static int executeBanList(CommandContext<CommandSourceStack> ctx, String type) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            BanManager banManager = BanManager.getInstance();
            
            if ("players".equals(type)) {
                var bannedPlayers = banManager.getAllPlayerBans();
                
                if (bannedPlayers.isEmpty()) {
                    String message = MessageUtil.localize("neoessentials.moderation.banlist_empty_players");
                    source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                    return 1;
                }
                
                String header = MessageUtil.localize("neoessentials.moderation.banlist_header_players", bannedPlayers.size());
                source.sendSuccess(() -> MessageUtil.coloredText(header), false);
                
                for (BanManager.BanEntry ban : bannedPlayers) {
                    String expireInfo = ban.expireTime > 0 ? 
                        MessageUtil.localize("neoessentials.moderation.banlist_expires", ban.getFormattedExpireTime()) :
                        MessageUtil.localize("neoessentials.moderation.banlist_permanent");
                    
                    // Args were previously (playerName, reason, bannedBy, ...) but the template
                    // is "{0} (by {1}, reason: {2}, ...)" — reason and bannedBy were swapped,
                    // so staff saw the ban REASON where "by <staff>" was expected and vice versa.
                    String banInfo = MessageUtil.localize("neoessentials.moderation.banlist_entry_player",
                        ban.playerName, ban.bannedBy, ban.reason, ban.getFormattedBanTime(), expireInfo);
                    source.sendSuccess(() -> MessageUtil.coloredText(banInfo), false);
                }
                
            } else { // "ips"
                var bannedIPs = banManager.getAllIPBans();
                
                if (bannedIPs.isEmpty()) {
                    String message = MessageUtil.localize("neoessentials.moderation.banlist_empty_ips");
                    source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                    return 1;
                }
                
                String header = MessageUtil.localize("neoessentials.moderation.banlist_header_ips", bannedIPs.size());
                source.sendSuccess(() -> MessageUtil.coloredText(header), false);
                
                for (BanManager.IPBanEntry ban : bannedIPs) {
                    // Same reason/bannedBy swap as banlist_entry_player above — fixed.
                    String banInfo = MessageUtil.localize("neoessentials.moderation.banlist_entry_ip",
                        ban.ipAddress, ban.bannedBy, ban.reason, ban.getFormattedBanTime());
                    source.sendSuccess(() -> MessageUtil.coloredText(banInfo), false);
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing banlist command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the banlist command."));
            return 0;
        }
    }
    
    private static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /** The command sender's player UUID, or {@code null} if run from console/command block. */
    private static java.util.UUID senderId(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
    }

    private static void broadcastToStaff(MinecraftServer server, String message) {
        broadcastToStaff(server, message, null);
    }

    /**
     * @param excludeId skipped if non-null — used so the command sender, who already got
     *                  their own personal confirmation message, does not also get this
     *                  near-duplicate staff-wide broadcast just because they also qualify.
     */
    private static void broadcastToStaff(MinecraftServer server, String message, java.util.UUID excludeId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (excludeId != null && player.getUUID().equals(excludeId)) continue;
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    player.getUUID(), "neoessentials.moderation.notifications")) {
                player.sendSystemMessage(MessageUtil.coloredText(message));
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