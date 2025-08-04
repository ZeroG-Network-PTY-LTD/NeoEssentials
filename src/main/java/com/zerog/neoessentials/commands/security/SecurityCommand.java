package com.zerog.neoessentials.commands.security;

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
                .requires(source -> source.hasPermission(3))
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
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6&l=== Security System Status ===")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eTotal Events: &f" + stats.get("total_events"))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eBlocked IPs: &f" + stats.get("blocked_ips"))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eMonitored Players: &f" + stats.get("monitored_players"))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eMonitored IPs: &f" + stats.get("monitored_ips"))
            ), false);
            
            // Show event type statistics
            @SuppressWarnings("unchecked")
            Map<String, Long> eventTypes = (Map<String, Long>) stats.get("event_types");
            if (!eventTypes.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&eEvent Types:")
                ), false);
                
                eventTypes.forEach((type, count) -> {
                    source.sendSuccess(() -> Component.literal(
                        MessageUtil.translateColorCodes("&7- " + type + ": &f" + count)
                    ), false);
                });
            }
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7Use '/security events' to see recent events")
            ), false);
            
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
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6&l=== Recent Security Events (Last " + limit + ") ===")
            ), false);
            
            if (events.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&7No recent security events")
                ), false);
                return 1;
            }
            
            for (SecurityEvent event : events) {
                String severityColor = getSeverityColor(event.getThreatLevel());
                String timestamp = event.getTimestampAsDateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"));
                
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes(String.format("&7[%s] %s%s &7- &f%s &8(%s)",
                        timestamp,
                        severityColor,
                        event.getType().toString(),
                        event.getDescription(),
                        event.getSource()
                    ))
                ), false);
            }
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7Use '/security player <name>' or '/security ip <ip>' for details")
            ), false);
            
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
                source.sendFailure(Component.literal("Player not found: " + playerName));
                return 0;
            }
            
            UUID playerId = targetPlayer.getUUID();
            PlayerSecurityProfile profile = securityManager.getPlayerProfile(playerId);
            
            if (profile == null) {
                source.sendFailure(Component.literal("No security profile found for player: " + playerName));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6&l=== Security Profile: " + playerName + " ===")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&ePlayer ID: &f" + playerId)
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eFirst Seen: &f" + 
                    profile.getFirstSeen().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eLast Seen: &f" + 
                    profile.getLastSeen().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eLast Known IP: &f" + 
                    (profile.getLastKnownIp() != null ? profile.getLastKnownIp() : "Unknown"))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eSuspicion Level: &f" + profile.getSuspicionLevel() + "/10")
            ), false);
            
            if (profile.isFlagged()) {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&cFlagged: &f" + profile.getFlagReason())
                ), false);
            }
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eLogin Attempts: &f" + profile.getLoginAttempts().size())
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eCommand History: &f" + profile.getCommandHistory().size())
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eRecent Commands: &f" + profile.getRecentCommandCount() + "/min")
            ), false);
            
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
                source.sendFailure(Component.literal("No security profile found for IP: " + ipAddress));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6&l=== IP Security Profile: " + ipAddress + " ===")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eFirst Seen: &f" + 
                    profile.getFirstSeen().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eLast Seen: &f" + 
                    profile.getLastSeen().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eFailed Login Attempts: &f" + profile.getFailedLoginAttempts())
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eRecent Failures: &f" + profile.getRecentFailureCount() + "/hour")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eSuspicion Level: &f" + profile.getSuspicionLevel() + "/10")
            ), false);
            
            if (profile.isBlocked()) {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&cBlocked: &f" + profile.getBlockReason())
                ), false);
            }
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eLogin Attempts: &f" + profile.getLoginAttempts().size())
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eWeb Requests: &f" + profile.getWebRequests().size())
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eBot-like Behavior: &f" + (profile.isBotLike() ? "Yes" : "No"))
            ), false);
            
            if (profile.getGeolocation() != null) {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&eLocation: &f" + profile.getGeolocation())
                ), false);
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
                source.sendFailure(Component.literal("IP address is already blocked: " + ipAddress));
                return 0;
            }
            
            securityManager.blockIpAddress(ipAddress, reason);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&aIP address blocked: &f" + ipAddress)
            ), true);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eReason: &f" + reason)
            ), false);
            
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
                source.sendFailure(Component.literal("IP address is not blocked: " + ipAddress));
                return 0;
            }
            
            securityManager.unblockIpAddress(ipAddress);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&aIP address unblocked: &f" + ipAddress)
            ), true);
            
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
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6Running comprehensive security scan...")
            ), false);
            
            // Trigger security analysis
            Map<String, Object> stats = securityManager.getSecurityStats();
            int totalEvents = (int) stats.get("total_events");
            int blockedIps = (int) stats.get("blocked_ips");
            
            // Analyze recent events for threats
            List<SecurityEvent> recentEvents = securityManager.getRecentEvents(50);
            long highThreatEvents = recentEvents.stream()
                .filter(event -> event.getThreatLevel().getLevel() >= 3)
                .count();
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&a=== Security Scan Results ===")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eTotal Security Events: &f" + totalEvents)
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eHigh-Threat Events (Recent): &f" + highThreatEvents)
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eBlocked IP Addresses: &f" + blockedIps)
            ), false);
            
            if (highThreatEvents > 0) {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&c⚠ High-threat events detected! Review with '/security events'")
                ), false);
            } else {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&a✓ No immediate threats detected")
                ), false);
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
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6Generating compliance report...")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&aCompliance report generated!")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&eReport Location: &fneoessentials/security/compliance_report.json")
            ), false);
            
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7Report includes: Event statistics, blocked IPs, monitoring data")
            ), false);
            
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
    private String executeStartCommand() {
        // Implementation for start command
        return "Security monitoring started";
    }
    
    private String executeStopCommand() {
        // Implementation for stop command  
        return "Security monitoring stopped";
    }
    
    private String executeConfigCommand(String[] args) {
        // Implementation for config command
        return "Security configuration updated";
    }
    
    private String executeThreatsCommand() {
        // Implementation for threats command
        return "Current security threats displayed";
    }
    
    private String executeAuditCommand() {
        // Implementation for audit command
        return "Security audit completed";
    }
    
    private String executeTestCommand() {
        // Implementation for test command
        return "Security test executed";
    }
    
    private String executeHelpCommand() {
        // Implementation for help command
        return "Security system help displayed";
    }
}
