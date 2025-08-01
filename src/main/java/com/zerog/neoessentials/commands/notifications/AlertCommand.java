package com.zerog.neoessentials.commands.notifications;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.systems.notifications.AlertNotificationSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Alert and Notification Management Commands for NeoEssentials
 * Provides comprehensive alert management and notification control
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class AlertCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertCommand.class);
    
    private static final AlertNotificationSystem alertSystem = AlertNotificationSystem.getInstance();
    
    /**
     * Register alert commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neoalerts")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("help")
                .executes(AlertCommand::showHelp))
            .then(Commands.literal("status")
                .executes(AlertCommand::showAlertStatus))
            .then(Commands.literal("start")
                .executes(AlertCommand::startAlertSystem))
            .then(Commands.literal("stop")
                .executes(AlertCommand::stopAlertSystem))
            .then(Commands.literal("config")
                .executes(AlertCommand::showConfiguration)
                .then(Commands.literal("health-threshold")
                    .then(Commands.argument("threshold", DoubleArgumentType.doubleArg(0.0, 100.0))
                        .executes(ctx -> setHealthThreshold(ctx, DoubleArgumentType.getDouble(ctx, "threshold")))))
                .then(Commands.literal("critical-threshold")
                    .then(Commands.argument("threshold", DoubleArgumentType.doubleArg(0.0, 100.0))
                        .executes(ctx -> setCriticalThreshold(ctx, DoubleArgumentType.getDouble(ctx, "threshold")))))
                .then(Commands.literal("interval")
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 300))
                        .executes(ctx -> setMonitoringInterval(ctx, IntegerArgumentType.getInteger(ctx, "seconds"))))))
            .then(Commands.literal("test")
                .executes(AlertCommand::testAlertSystem))
        );
        
        LOGGER.info("Alert management commands registered successfully");
    }
    
    /**
     * Show alert help menu
     */
    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        sendMessage(ctx.getSource(), "&6&l=== NeoEssentials Alert System ===");
        sendMessage(ctx.getSource(), "&7Comprehensive alert and notification management");
        sendMessage(ctx.getSource(), "");
        sendMessage(ctx.getSource(), "&e/neoalerts status &7- Show alert system status");
        sendMessage(ctx.getSource(), "&e/neoalerts start &7- Start alert monitoring");
        sendMessage(ctx.getSource(), "&e/neoalerts stop &7- Stop alert monitoring");
        sendMessage(ctx.getSource(), "&e/neoalerts config &7- Show current configuration");
        sendMessage(ctx.getSource(), "&e/neoalerts config health-threshold <percent> &7- Set health alert threshold");
        sendMessage(ctx.getSource(), "&e/neoalerts config critical-threshold <percent> &7- Set critical alert threshold");
        sendMessage(ctx.getSource(), "&e/neoalerts config interval <seconds> &7- Set monitoring interval");
        sendMessage(ctx.getSource(), "&e/neoalerts test &7- Send test alerts");
        sendMessage(ctx.getSource(), "");
        sendMessage(ctx.getSource(), "&7Automated monitoring and alerting for enterprise environments!");
        
        return 1;
    }
    
    /**
     * Show alert system status
     */
    private static int showAlertStatus(CommandContext<CommandSourceStack> ctx) {
        try {
            Map<String, Object> stats = alertSystem.getAlertStatistics();
            
            sendMessage(ctx.getSource(), "&6&l=== Alert System Status ===");
            sendMessage(ctx.getSource(), "&eSystem Status: &a" + (alertSystem.isRunning() ? "Running" : "Stopped"));
            sendMessage(ctx.getSource(), "&eActive Alerts: &a" + stats.get("active_alerts"));
            sendMessage(ctx.getSource(), "&eQueued Alerts: &a" + stats.get("queue_size"));
            sendMessage(ctx.getSource(), "&eNotification Channels: &a" + stats.get("channels"));
            sendMessage(ctx.getSource(), "");
            
            sendMessage(ctx.getSource(), "&e&lThresholds:");
            sendMessage(ctx.getSource(), "&aHealth Warning: &7" + stats.get("health_threshold") + "%");
            sendMessage(ctx.getSource(), "&aCritical Alert: &7" + stats.get("critical_threshold") + "%");
            sendMessage(ctx.getSource(), "&aMonitoring Interval: &7" + ((Long)stats.get("monitoring_interval_ms") / 1000) + " seconds");
            sendMessage(ctx.getSource(), "");
            
            if (alertSystem.isRunning()) {
                sendMessage(ctx.getSource(), "&a✓ Alert system is monitoring system health");
                sendMessage(ctx.getSource(), "&7  Notifications will be sent for critical events");
            } else {
                sendMessage(ctx.getSource(), "&c⚠ Alert system is not running");
                sendMessage(ctx.getSource(), "&7  Use &e/neoalerts start &7to begin monitoring");
            }
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError retrieving alert system status: " + e.getMessage());
            LOGGER.error("Error in alert status command", e);
        }
        
        return 1;
    }
    
    /**
     * Start the alert system
     */
    private static int startAlertSystem(CommandContext<CommandSourceStack> ctx) {
        try {
            if (alertSystem.isRunning()) {
                sendMessage(ctx.getSource(), "&eAlert system is already running");
                return 1;
            }
            
            alertSystem.initialize();
            sendMessage(ctx.getSource(), "&a✓ Alert system started successfully");
            sendMessage(ctx.getSource(), "&7  System health monitoring is now active");
            sendMessage(ctx.getSource(), "&7  Notifications will be sent for critical events");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cFailed to start alert system: " + e.getMessage());
            LOGGER.error("Error starting alert system", e);
        }
        
        return 1;
    }
    
    /**
     * Stop the alert system
     */
    private static int stopAlertSystem(CommandContext<CommandSourceStack> ctx) {
        try {
            if (!alertSystem.isRunning()) {
                sendMessage(ctx.getSource(), "&eAlert system is already stopped");
                return 1;
            }
            
            alertSystem.shutdown();
            sendMessage(ctx.getSource(), "&a✓ Alert system stopped successfully");
            sendMessage(ctx.getSource(), "&7  System health monitoring has been disabled");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cFailed to stop alert system: " + e.getMessage());
            LOGGER.error("Error stopping alert system", e);
        }
        
        return 1;
    }
    
    /**
     * Show alert configuration
     */
    private static int showConfiguration(CommandContext<CommandSourceStack> ctx) {
        try {
            sendMessage(ctx.getSource(), "&6&l=== Alert System Configuration ===");
            sendMessage(ctx.getSource(), "&eHealth Warning Threshold: &a" + alertSystem.getHealthThreshold() + "%");
            sendMessage(ctx.getSource(), "&eCritical Alert Threshold: &a" + alertSystem.getCriticalThreshold() + "%");
            sendMessage(ctx.getSource(), "&eMonitoring Interval: &a" + (alertSystem.getMonitoringInterval() / 1000) + " seconds");
            sendMessage(ctx.getSource(), "");
            
            sendMessage(ctx.getSource(), "&e&lConfiguration Commands:");
            sendMessage(ctx.getSource(), "&7/neoalerts config health-threshold <percent>");
            sendMessage(ctx.getSource(), "&7/neoalerts config critical-threshold <percent>");
            sendMessage(ctx.getSource(), "&7/neoalerts config interval <seconds>");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError showing configuration: " + e.getMessage());
            LOGGER.error("Error in configuration command", e);
        }
        
        return 1;
    }
    
    /**
     * Set health threshold
     */
    private static int setHealthThreshold(CommandContext<CommandSourceStack> ctx, double threshold) {
        try {
            alertSystem.setHealthThreshold(threshold);
            sendMessage(ctx.getSource(), "&a✓ Health warning threshold set to " + threshold + "%");
            sendMessage(ctx.getSource(), "&7  Alerts will be sent when system health drops below this level");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cFailed to set health threshold: " + e.getMessage());
            LOGGER.error("Error setting health threshold", e);
        }
        
        return 1;
    }
    
    /**
     * Set critical threshold
     */
    private static int setCriticalThreshold(CommandContext<CommandSourceStack> ctx, double threshold) {
        try {
            alertSystem.setCriticalThreshold(threshold);
            sendMessage(ctx.getSource(), "&a✓ Critical alert threshold set to " + threshold + "%");
            sendMessage(ctx.getSource(), "&7  Critical alerts will be sent when system health drops below this level");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cFailed to set critical threshold: " + e.getMessage());
            LOGGER.error("Error setting critical threshold", e);
        }
        
        return 1;
    }
    
    /**
     * Set monitoring interval
     */
    private static int setMonitoringInterval(CommandContext<CommandSourceStack> ctx, int seconds) {
        try {
            alertSystem.setMonitoringInterval(seconds * 1000L);
            sendMessage(ctx.getSource(), "&a✓ Monitoring interval set to " + seconds + " seconds");
            sendMessage(ctx.getSource(), "&7  System will be checked every " + seconds + " seconds for alerts");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cFailed to set monitoring interval: " + e.getMessage());
            LOGGER.error("Error setting monitoring interval", e);
        }
        
        return 1;
    }
    
    /**
     * Test alert system
     */
    private static int testAlertSystem(CommandContext<CommandSourceStack> ctx) {
        try {
            sendMessage(ctx.getSource(), "&eGenerating test alerts...");
            
            // Send test alerts
            alertSystem.sendAlert(new AlertNotificationSystem.StatusAlert(
                AlertNotificationSystem.AlertLevel.INFO,
                "Test Info Alert",
                "This is a test information alert to verify the notification system",
                "AlertCommand",
                java.time.LocalDateTime.now()
            ));
            
            alertSystem.sendAlert(new AlertNotificationSystem.StatusAlert(
                AlertNotificationSystem.AlertLevel.WARNING,
                "Test Warning Alert",
                "This is a test warning alert to verify notification delivery",
                "AlertCommand",
                java.time.LocalDateTime.now()
            ));
            
            alertSystem.sendAlert(new AlertNotificationSystem.StatusAlert(
                AlertNotificationSystem.AlertLevel.ERROR,
                "Test Error Alert",
                "This is a test error alert to verify critical notification handling",
                "AlertCommand",
                java.time.LocalDateTime.now()
            ));
            
            sendMessage(ctx.getSource(), "&a✓ Test alerts sent successfully");
            sendMessage(ctx.getSource(), "&7  Check console logs and alert files for notifications");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cFailed to send test alerts: " + e.getMessage());
            LOGGER.error("Error sending test alerts", e);
        }
        
        return 1;
    }
    
    // Helper methods
    private static void sendMessage(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(colorize(message)), false);
    }
    
    private static String colorize(String message) {
        return message.replace("&0", "§0").replace("&1", "§1").replace("&2", "§2").replace("&3", "§3")
                     .replace("&4", "§4").replace("&5", "§5").replace("&6", "§6").replace("&7", "§7")
                     .replace("&8", "§8").replace("&9", "§9").replace("&a", "§a").replace("&b", "§b")
                     .replace("&c", "§c").replace("&d", "§d").replace("&e", "§e").replace("&f", "§f")
                     .replace("&l", "§l").replace("&m", "§m").replace("&n", "§n").replace("&o", "§o")
                     .replace("&r", "§r").replace("&k", "§k");
    }
}
