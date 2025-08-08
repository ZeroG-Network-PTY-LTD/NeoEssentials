package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.notifications.NotificationEvent;
import com.zerog.neoessentials.notifications.NotificationManager;
import com.zerog.neoessentials.notifications.channels.NotificationChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Notification command implementation - /notifications
 * Manages notification system and sends test notifications
 */
public class NotificationCommand {
    
    private static final SuggestionProvider<CommandSourceStack> CHANNEL_SUGGESTIONS = 
        (context, builder) -> {
            try {
                NotificationManager manager = NotificationManager.getInstance();
                return SharedSuggestionProvider.suggest(
                    manager.getChannels().keySet().stream(), builder
                );
            } catch (Exception e) {
                return SharedSuggestionProvider.suggest(new String[]{"log", "discord", "email"}, builder);
            }
        };
    
    private static final SuggestionProvider<CommandSourceStack> EVENT_TYPE_SUGGESTIONS = 
        (context, builder) -> SharedSuggestionProvider.suggest(
            new String[]{"PLAYER_JOIN", "PLAYER_LEAVE", "SERVER_START", "SERVER_STOP", 
                        "SECURITY_ALERT", "PERFORMANCE_ALERT", "ERROR_ALERT"}, 
            builder
        );
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /notifications - Main notification management command
        dispatcher.register(Commands.literal("notifications")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .executes(ctx -> showStatus(ctx))
            .then(Commands.literal("status")
                .executes(ctx -> showStatus(ctx))
            )
            .then(Commands.literal("test")
                .executes(ctx -> sendTestNotification(ctx, null))
                .then(Commands.argument("channel", StringArgumentType.word())
                    .suggests(CHANNEL_SUGGESTIONS)
                    .executes(ctx -> sendTestNotification(ctx, StringArgumentType.getString(ctx, "channel")))
                )
            )
            .then(Commands.literal("send")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests(EVENT_TYPE_SUGGESTIONS)
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> sendCustomNotification(ctx, 
                            StringArgumentType.getString(ctx, "type"),
                            StringArgumentType.getString(ctx, "message")))
                    )
                )
            )
            .then(Commands.literal("channels")
                .executes(ctx -> listChannels(ctx))
            )
        );
        
        // /notify - Alias for notifications
        dispatcher.register(Commands.literal("notify")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .executes(ctx -> showStatus(ctx))
            .then(Commands.literal("test")
                .executes(ctx -> sendTestNotification(ctx, null))
            )
        );
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        try {
            NotificationManager manager = NotificationManager.getInstance();
            
            context.getSource().sendSuccess(() -> Component.literal("§a=== Notification System Status ==="), false);
            
            Map<String, NotificationChannel> channels = manager.getChannels();
            context.getSource().sendSuccess(() -> Component.literal("§7Available Channels: " + channels.size()), false);
            
            for (Map.Entry<String, NotificationChannel> entry : channels.entrySet()) {
                String name = entry.getKey();
                NotificationChannel channel = entry.getValue();
                String status = channel.isEnabled() ? "§aEnabled" : "§cDisabled";
                context.getSource().sendSuccess(() -> Component.literal("  §7- " + name + ": " + status), false);
            }
            
            context.getSource().sendSuccess(() -> Component.literal("§7Enabled Events: " + manager.getEnabledEvents().size()), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cNotification system not initialized: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listChannels(CommandContext<CommandSourceStack> context) {
        try {
            NotificationManager manager = NotificationManager.getInstance();
            Map<String, NotificationChannel> channels = manager.getChannels();
            
            context.getSource().sendSuccess(() -> Component.literal("§a=== Notification Channels ==="), false);
            
            for (Map.Entry<String, NotificationChannel> entry : channels.entrySet()) {
                String name = entry.getKey();
                NotificationChannel channel = entry.getValue();
                
                String status = channel.isEnabled() ? "§aOnline" : "§cOffline";
                context.getSource().sendSuccess(() -> Component.literal("§7" + name + " (" + channel.getChannelName() + "): " + status), false);
                
                // Show supported events
                StringBuilder events = new StringBuilder("§7  Supports: ");
                for (NotificationEvent.Type eventType : NotificationEvent.Type.values()) {
                    if (channel.supportsEventType(eventType)) {
                        events.append(eventType.name().replace("_", " ")).append(", ");
                    }
                }
                String eventStr = events.toString();
                if (eventStr.endsWith(", ")) {
                    eventStr = eventStr.substring(0, eventStr.length() - 2);
                }
                final String finalEventStr = eventStr;
                context.getSource().sendSuccess(() -> Component.literal(finalEventStr), false);
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError listing channels: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int sendTestNotification(CommandContext<CommandSourceStack> context, String channelName) throws CommandSyntaxException {
        try {
            NotificationManager manager = NotificationManager.getInstance();
            
            String executor = "Console";
            try {
                ServerPlayer player = context.getSource().getPlayerOrException();
                executor = player.getName().getString();
            } catch (CommandSyntaxException e) {
                // Command executed from console
            }
            
            NotificationEvent event = NotificationEvent.builder()
                .type(NotificationEvent.Type.CUSTOM)
                .title("Test Notification")
                .message("This is a test notification sent by " + executor)
                .playerName(executor)
                .timestamp(System.currentTimeMillis())
                .severity(NotificationEvent.Severity.INFO)
                .metadata("source", "command")
                .metadata("channel_target", channelName != null ? channelName : "all")
                .build();
            
            if (channelName != null) {
                // Send to specific channel
                NotificationChannel channel = manager.getChannels().get(channelName.toLowerCase());
                if (channel == null) {
                    context.getSource().sendFailure(Component.literal("§cChannel not found: " + channelName));
                    return 0;
                }
                
                if (!channel.isEnabled()) {
                    context.getSource().sendFailure(Component.literal("§cChannel is disabled: " + channelName));
                    return 0;
                }
                
                try {
                    channel.sendNotification(event);
                    context.getSource().sendSuccess(() -> Component.literal("§aTest notification sent to " + channelName + " channel"), false);
                } catch (Exception e) {
                    context.getSource().sendFailure(Component.literal("§cFailed to send to " + channelName + ": " + e.getMessage()));
                    return 0;
                }
            } else {
                // Send to all channels
                manager.sendNotification(event);
                context.getSource().sendSuccess(() -> Component.literal("§aTest notification sent to all enabled channels"), false);
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError sending test notification: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int sendCustomNotification(CommandContext<CommandSourceStack> context, String typeStr, String message) throws CommandSyntaxException {
        try {
            NotificationManager manager = NotificationManager.getInstance();
            
            // Parse event type
            NotificationEvent.Type eventType;
            try {
                eventType = NotificationEvent.Type.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                context.getSource().sendFailure(Component.literal("§cInvalid event type: " + typeStr));
                return 0;
            }
            
            String executor = "Console";
            try {
                ServerPlayer player = context.getSource().getPlayerOrException();
                executor = player.getName().getString();
            } catch (CommandSyntaxException e) {
                // Command executed from console
            }
            
            NotificationEvent event = NotificationEvent.builder()
                .type(eventType)
                .title("Custom " + eventType.name().replace("_", " "))
                .message(message)
                .playerName(executor)
                .timestamp(System.currentTimeMillis())
                .severity(NotificationEvent.Severity.INFO)
                .metadata("source", "custom_command")
                .metadata("executor", executor)
                .build();
            
            manager.sendNotification(event);
            context.getSource().sendSuccess(() -> Component.literal("§aCustom notification sent: " + eventType.name()), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError sending custom notification: " + e.getMessage()));
            return 0;
        }
    }
}
