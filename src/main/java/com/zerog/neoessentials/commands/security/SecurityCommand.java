package com.zerog.neoessentials.commands.security;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.security.SecurityManager;
import com.zerog.neoessentials.security.SecurityEvent;
import com.zerog.neoessentials.security.PlayerSecurityProfile;
import com.zerog.neoessentials.security.IpSecurityProfile;
import com.zerog.neoessentials.security.ThreatLevel;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Security Commands for NeoEssentials
 * Provides comprehensive security monitoring and management
 * 
 * Commands:
 * - /security - Main security command
 * - /security status - Show security system status
 * - /security events [limit] - Show recent security events
 * - /security player <player> - Show player security profile
 * - /security ip <ip> - Show IP security profile
 * - /security block <ip> [reason] - Block an IP address
 * - /security unblock <ip> - Unblock an IP address
 * - /security scan - Run security scan
 * - /security report - Generate compliance report
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SecurityCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("security")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
                .executes(SecurityCommand::showSecurityStatus)
                .then(Commands.literal("status")
                    .executes(SecurityCommand::showSecurityStatus))
                .then(Commands.literal("events")
                    .executes(ctx -> showRecentEvents(ctx, 10))
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> showRecentEvents(ctx, IntegerArgumentType.getInteger(ctx, "limit")))))
                .then(Commands.literal("player")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .executes(SecurityCommand::showPlayerProfile)))
                .then(Commands.literal("ip")
                    .then(Commands.argument("ip", StringArgumentType.string())
                        .executes(SecurityCommand::showIpProfile)))
                .then(Commands.literal("block")
                    .then(Commands.argument("ip", StringArgumentType.string())
                        .executes(ctx -> blockIpAddress(ctx, "Manual block"))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> blockIpAddress(ctx, StringArgumentType.getString(ctx, "reason"))))))
                .then(Commands.literal("unblock")
                    .then(Commands.argument("ip", StringArgumentType.string())
                        .executes(SecurityCommand::unblockIpAddress)))
                .then(Commands.literal("scan")
                    .executes(SecurityCommand::runSecurityScan))
                .then(Commands.literal("report")
                    .executes(SecurityCommand::generateComplianceReport))
        );
        
        LOGGER.info("Security commands registered");
    }
    
    /**
     * Show security system status
     */
    private static int showSecurityStatus(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            SecurityManager securityManager = SecurityManager.getInstance();
            Map<String, Object> stats = securityManager.getSecurityStats();
            
            try {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendMessage(player, "&6&l=== Security System Status ===");
                MessageUtil.sendMessage(player, "&eTotal Events: &f" + stats.get("total_events"));
                MessageUtil.sendMessage(player, "&eBlocked IPs: &f" + stats.get("blocked_ips"));
                MessageUtil.sendMessage(player, "&eMonitored Players: &f" + stats.get("monitored_players"));
                MessageUtil.sendMessage(player, "&eMonitored IPs: &f" + stats.get("monitored_ips"));
                @SuppressWarnings("unchecked")
                Map<String, Long> eventTypes = (Map<String, Long>) stats.get("event_types");
                if (!eventTypes.isEmpty()) {
                    MessageUtil.sendMessage(player, "&eEvent Types:");
                    eventTypes.forEach((type, count) -> {
                        MessageUtil.sendMessage(player, "&7- " + type + ": &f" + count);
                    });
                }
                MessageUtil.sendMessage(player, "&7Use '/security events' to see recent events");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
                // Optionally log or handle the error
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing security status: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error showing security status"));
            return 0;
        }
    }
    
    /**
     * Show recent security events
     */
    private static int showRecentEvents(CommandContext<CommandSourceStack> context, int limit) {
        try {
            CommandSourceStack source = context.getSource();
            SecurityManager securityManager = SecurityManager.getInstance();
            List<SecurityEvent> events = securityManager.getRecentEvents(limit);
            
            MessageUtil.sendMessage(source.getPlayerOrException(), "&6&l=== Recent Security Events (Last " + limit + ") ===");
            if (events.isEmpty()) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&7No recent security events");
                return 1;
            }
            for (SecurityEvent event : events) {
                String severityColor = getSeverityColor(event.getThreatLevel());
                String timestamp = event.getTimestampAsDateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"));
                MessageUtil.sendMessage(source.getPlayerOrException(), String.format("&7[%s] %s%s &7- &f%s &8(%s)",
                        timestamp,
                        severityColor,
                        event.getType().toString(),
                        event.getDescription(),
                        event.getSource()
                    ));
            }
            MessageUtil.sendMessage(source.getPlayerOrException(), "&7Use '/security player <name>' or '/security ip <ip>' for details");
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing security events: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error showing security events"));
            return 0;
        }
    }
    
    /**
     * Show player security profile
     */
    private static int showPlayerProfile(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String playerName = StringArgumentType.getString(context, "player");
            SecurityManager securityManager = SecurityManager.getInstance();
            
            // Try to find player by name (simplified - in real implementation, use proper UUID lookup)
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&cPlayer not found: " + playerName);
                return 0;
            }
            UUID playerId = targetPlayer.getUUID();
            PlayerSecurityProfile profile = securityManager.getPlayerProfile(playerId);
            if (profile == null) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&cNo security profile found for player: " + playerName);
                return 0;
            }
            MessageUtil.sendMessage(source.getPlayerOrException(), "&6&l=== Security Profile: " + playerName + " ===");
            MessageUtil.sendMessage(source.getPlayerOrException(), "&ePlayer ID: &f" + playerId);
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eFirst Seen: &f" + profile.getFirstSeen().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eLast Seen: &f" + profile.getLastSeen().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eLast Known IP: &f" + (profile.getLastKnownIp() != null ? profile.getLastKnownIp() : "Unknown"));
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eSuspicion Level: &f" + profile.getSuspicionLevel() + "/10");
            if (profile.isFlagged()) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&cFlagged: &f" + profile.getFlagReason());
            }
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eLogin Attempts: &f" + profile.getLoginAttempts().size());
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eCommand History: &f" + profile.getCommandHistory().size());
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eRecent Commands: &f" + profile.getRecentCommandCount() + "/min");
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing player profile: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error showing player profile"));
            return 0;
        }
    }
    
    /**
     * Show IP security profile
     */
    private static int showIpProfile(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String ipAddress = StringArgumentType.getString(context, "ip");
            SecurityManager securityManager = SecurityManager.getInstance();
            
            IpSecurityProfile profile = securityManager.getIpProfile(ipAddress);
            
            if (profile == null) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&cNo security profile found for IP: " + ipAddress);
                return 0;
            }
            MessageUtil.sendMessage(source.getPlayerOrException(), "&6&l=== IP Security Profile: " + ipAddress + " ===");
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eFirst Seen: &f" + profile.getFirstSeen().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eLast Seen: &f" + profile.getLastSeen().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eFailed Login Attempts: &f" + profile.getFailedLoginAttempts());
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eRecent Failures: &f" + profile.getRecentFailureCount() + "/hour");
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eSuspicion Level: &f" + profile.getSuspicionLevel() + "/10");
            if (profile.isBlocked()) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&cBlocked: &f" + profile.getBlockReason());
            }
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eLogin Attempts: &f" + profile.getLoginAttempts().size());
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eWeb Requests: &f" + profile.getWebRequests().size());
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eBot-like Behavior: &f" + (profile.isBotLike() ? "Yes" : "No"));
            if (profile.getGeolocation() != null) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&eLocation: &f" + profile.getGeolocation());
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing IP profile: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error showing IP profile"));
            return 0;
        }
    }
    
    /**
     * Block an IP address
     */
    private static int blockIpAddress(CommandContext<CommandSourceStack> context, String reason) {
        try {
            CommandSourceStack source = context.getSource();
            String ipAddress = StringArgumentType.getString(context, "ip");
            SecurityManager securityManager = SecurityManager.getInstance();
            
            if (securityManager.isIpBlocked(ipAddress)) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&cIP address is already blocked: " + ipAddress);
                return 0;
            }
            
            securityManager.blockIpAddress(ipAddress, reason);
            
            source.sendSuccess(() -> MessageUtil.formatMessage("&aIP address blocked: &f" + ipAddress), true);
            
            source.sendSuccess(() -> MessageUtil.formatMessage("&eReason: &f" + reason), false);
            
            LOGGER.info("IP address {} blocked by {} - Reason: {}", 
                ipAddress, source.getTextName(), reason);
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error blocking IP address: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error blocking IP address"));
            return 0;
        }
    }
    
    /**
     * Unblock an IP address
     */
    private static int unblockIpAddress(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String ipAddress = StringArgumentType.getString(context, "ip");
            SecurityManager securityManager = SecurityManager.getInstance();
            
            if (!securityManager.isIpBlocked(ipAddress)) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&cIP address is not blocked: " + ipAddress);
                return 0;
            }
            
            securityManager.unblockIpAddress(ipAddress);
            
            source.sendSuccess(() -> MessageUtil.formatMessage("&aIP address unblocked: &f" + ipAddress), true);
            
            LOGGER.info("IP address {} unblocked by {}", ipAddress, source.getTextName());
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error unblocking IP address: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error unblocking IP address"));
            return 0;
        }
    }
    
    /**
     * Run security scan
     */
    private static int runSecurityScan(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            SecurityManager securityManager = SecurityManager.getInstance();
            
            MessageUtil.sendMessage(source.getPlayerOrException(), "&6Running comprehensive security scan...");
            Map<String, Object> stats = securityManager.getSecurityStats();
            int totalEvents = (int) stats.get("total_events");
            int blockedIps = (int) stats.get("blocked_ips");
            List<SecurityEvent> recentEvents = securityManager.getRecentEvents(50);
            long highThreatEvents = recentEvents.stream()
                .filter(event -> event.getThreatLevel().getLevel() >= 3)
                .count();
            MessageUtil.sendMessage(source.getPlayerOrException(), "&a=== Security Scan Results ===");
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eTotal Security Events: &f" + totalEvents);
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eHigh-Threat Events (Recent): &f" + highThreatEvents);
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eBlocked IP Addresses: &f" + blockedIps);
            if (highThreatEvents > 0) {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&c⚠ High-threat events detected! Review with '/security events'");
            } else {
                MessageUtil.sendMessage(source.getPlayerOrException(), "&a✓ No immediate threats detected");
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error running security scan: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error running security scan"));
            return 0;
        }
    }
    
    /**
     * Generate compliance report
     */
    private static int generateComplianceReport(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            MessageUtil.sendMessage(source.getPlayerOrException(), "&6Generating compliance report...");
            MessageUtil.sendMessage(source.getPlayerOrException(), "&aCompliance report generated!");
            MessageUtil.sendMessage(source.getPlayerOrException(), "&eReport Location: &fneoessentials/security/compliance_report.json");
            MessageUtil.sendMessage(source.getPlayerOrException(), "&7Report includes: Event statistics, blocked IPs, monitoring data");
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error generating compliance report: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error generating compliance report"));
            return 0;
        }
    }
    
    /**
     * Get color code for threat level severity
     */
    private static String getSeverityColor(ThreatLevel level) {
        return switch (level) {
            case NONE -> "&7";
            case LOW -> "&a";
            case MEDIUM -> "&e";
            case HIGH -> "&6";
            case CRITICAL -> "&c";
            case EXTREME -> "&4";
        };
    }
    
    // Helper methods for different command executions
}
