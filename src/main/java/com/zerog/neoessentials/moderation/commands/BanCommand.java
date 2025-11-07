package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.moderation.BanManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.stream.Collectors;

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
                .collect(Collectors.toList()),
            builder
        );
    };
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_BANNED_IPS = (ctx, builder) -> {
        BanManager banManager = BanManager.getInstance();
        return SharedSuggestionProvider.suggest(
            banManager.getAllIPBans().stream()
                .map(ban -> ban.ipAddress)
                .collect(Collectors.toList()),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if moderation commands are enabled
        if (!ConfigManager.isModerationEnabled()) {
            return;
        }
        
        // /ban <player> [reason]
        dispatcher.register(Commands.literal("ban")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.ban"))
            .then(Commands.argument("player", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .executes(ctx -> executeBan(ctx, StringArgumentType.getString(ctx, "player"),
                    com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultBanReason()))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeBan(ctx, 
                        ctx.getInput().split(" ", 3)[1], // Get player name 
                        ctx.getInput().split(" ", 3).length > 2 ? ctx.getInput().split(" ", 3)[2] : com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultBanReason()))))
        );

        // /tempban <player> <duration> [reason]
        dispatcher.register(Commands.literal("tempban")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.tempban"))
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

        // /banip <ip> [reason]
        dispatcher.register(Commands.literal("banip")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.banip"))
            .then(Commands.argument("ip", StringArgumentType.word())
                .executes(ctx -> executeBanIP(ctx, 
                    StringArgumentType.getString(ctx, "ip"), com.zerog.neoessentials.config.ConfigManager.getInstance().getDefaultBanReason()))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeBanIP(ctx,
                        StringArgumentType.getString(ctx, "ip"),
                        StringArgumentType.getString(ctx, "reason")))))
        );
        
        // /unban <player>
        dispatcher.register(Commands.literal("unban")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.unban"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(SUGGEST_BANNED_PLAYERS)
                .executes(ctx -> executeUnban(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        
        // /unbanip <ip>
        dispatcher.register(Commands.literal("unbanip")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.unbanip"))
            .then(Commands.argument("ip", StringArgumentType.word())
                .suggests(SUGGEST_BANNED_IPS)
                .executes(ctx -> executeUnbanIP(ctx, StringArgumentType.getString(ctx, "ip"))))
        );
        
        // /banlist [players|ips]
        dispatcher.register(Commands.literal("banlist")
            .requires(source -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                getPlayerUUID(source), "neoessentials.moderation.banlist"))
            .executes(ctx -> executeBanList(ctx, "players"))
            .then(Commands.literal("players")
                .executes(ctx -> executeBanList(ctx, "players")))
            .then(Commands.literal("ips")
                .executes(ctx -> executeBanList(ctx, "ips")))
        );
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
                source.sendSuccess(() -> MessageUtil.success(message), true);
                
                // Broadcast ban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.ban_broadcast", 
                    resolvedName, bannedBy, reason));
                
                LOGGER.info("Player {} banned by {} for: {}", resolvedName, bannedBy, reason);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.ban_failed", resolvedName);
                source.sendFailure(MessageUtil.error(message));
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
                source.sendSuccess(() -> MessageUtil.success(message), true);
                
                // Broadcast ban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.tempban_broadcast", 
                    resolvedName, durationStr, bannedBy, reason));
                
                LOGGER.info("Player {} temp banned by {} for {} - Reason: {}", resolvedName, bannedBy, durationStr, reason);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.ban_failed", resolvedName);
                source.sendFailure(MessageUtil.error(message));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing tempban command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the tempban command."));
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
                source.sendSuccess(() -> MessageUtil.success(message), true);
                
                // Broadcast ban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.banip_broadcast", 
                    ipAddress, bannedBy, reason));
                
                LOGGER.info("IP {} banned by {} for: {}", ipAddress, bannedBy, reason);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.banip_failed", ipAddress);
                source.sendFailure(MessageUtil.error(message));
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
            for (BanManager.BanEntry ban : banManager.getAllPlayerBans()) {
                if (ban.playerName.equalsIgnoreCase(playerName)) {
                    playerId = ban.playerId;
                    resolvedName = ban.playerName;
                    break;
                }
            }
            
            // If not found in bans, try player cache
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
            
            // Unban the player
            boolean success = banManager.unbanPlayer(playerId);
            
            if (success) {
                String message = MessageUtil.localize("neoessentials.moderation.unban_success", resolvedName);
                source.sendSuccess(() -> MessageUtil.success(message), true);
                
                // Broadcast unban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unban_broadcast", 
                    resolvedName, unbannedBy));
                
                LOGGER.info("Player {} unbanned by {}", resolvedName, unbannedBy);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.unban_failed", resolvedName);
                source.sendFailure(MessageUtil.error(message));
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
            boolean success = banManager.unbanIP(ipAddress);
            
            if (success) {
                String message = MessageUtil.localize("neoessentials.moderation.unbanip_success", ipAddress);
                source.sendSuccess(() -> MessageUtil.success(message), true);
                
                // Broadcast unban to all online staff
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unbanip_broadcast", 
                    ipAddress, unbannedBy));
                
                LOGGER.info("IP {} unbanned by {}", ipAddress, unbannedBy);
                return 1;
            } else {
                String message = MessageUtil.localize("neoessentials.moderation.unbanip_failed", ipAddress);
                source.sendFailure(MessageUtil.error(message));
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
                    source.sendSuccess(() -> MessageUtil.info(message), false);
                    return 1;
                }
                
                String header = MessageUtil.localize("neoessentials.moderation.banlist_header_players", bannedPlayers.size());
                source.sendSuccess(() -> MessageUtil.info(header), false);
                
                for (BanManager.BanEntry ban : bannedPlayers) {
                    String expireInfo = ban.expireTime > 0 ? 
                        MessageUtil.localize("neoessentials.moderation.banlist_expires", ban.getFormattedExpireTime()) :
                        MessageUtil.localize("neoessentials.moderation.banlist_permanent");
                    
                    String banInfo = MessageUtil.localize("neoessentials.moderation.banlist_entry_player",
                        ban.playerName, ban.reason, ban.bannedBy, ban.getFormattedBanTime(), expireInfo);
                    source.sendSuccess(() -> MessageUtil.info(banInfo), false);
                }
                
            } else { // "ips"
                var bannedIPs = banManager.getAllIPBans();
                
                if (bannedIPs.isEmpty()) {
                    String message = MessageUtil.localize("neoessentials.moderation.banlist_empty_ips");
                    source.sendSuccess(() -> MessageUtil.info(message), false);
                    return 1;
                }
                
                String header = MessageUtil.localize("neoessentials.moderation.banlist_header_ips", bannedIPs.size());
                source.sendSuccess(() -> MessageUtil.info(header), false);
                
                for (BanManager.IPBanEntry ban : bannedIPs) {
                    String banInfo = MessageUtil.localize("neoessentials.moderation.banlist_entry_ip",
                        ban.ipAddress, ban.reason, ban.bannedBy, ban.getFormattedBanTime());
                    source.sendSuccess(() -> MessageUtil.info(banInfo), false);
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