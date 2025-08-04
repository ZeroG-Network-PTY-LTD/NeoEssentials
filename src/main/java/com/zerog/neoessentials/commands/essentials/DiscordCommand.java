package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.integrations.DiscordWebhookIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Discord webhook management command - /discord
 * Manages Discord webhook integration
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class DiscordCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("discord")
            .requires(source -> source.hasPermission(4)) // Admin only
            .executes(DiscordCommand::showStatus)
            .then(Commands.literal("status")
                .executes(DiscordCommand::showStatus))
            .then(Commands.literal("test")
                .executes(DiscordCommand::testWebhook))
            .then(Commands.literal("enable")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(DiscordCommand::setEnabled)))
            .then(Commands.literal("webhook")
                .then(Commands.argument("url", StringArgumentType.greedyString())
                    .executes(DiscordCommand::setWebhookUrl)))
            .then(Commands.literal("notify")
                .then(Commands.literal("start")
                    .executes(DiscordCommand::notifyServerStart))
                .then(Commands.literal("stop")
                    .executes(DiscordCommand::notifyServerStop))
                .then(Commands.literal("custom")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(DiscordCommand::sendCustomNotification))))
        );
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DiscordWebhookIntegration discord = DiscordWebhookIntegration.getInstance();
        
        source.sendSuccess(() -> Component.literal("=== Discord Integration Status ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        if (discord.isEnabled()) {
            source.sendSuccess(() -> Component.literal("Status: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("ENABLED").withStyle(ChatFormatting.GREEN)), false);
        } else {
            source.sendSuccess(() -> Component.literal("Status: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("DISABLED").withStyle(ChatFormatting.RED)), false);
        }
        
        source.sendSuccess(() -> Component.literal("Webhook URL: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(discord.getMaskedWebhookUrl()).withStyle(ChatFormatting.YELLOW)), false);
        
        source.sendSuccess(() -> Component.literal("Commands:")
            .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("  /discord test - Test webhook connection")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /discord enable <true/false> - Enable/disable integration")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /discord webhook <url> - Set webhook URL")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /discord notify custom <message> - Send custom notification")
            .withStyle(ChatFormatting.WHITE), false);
        
        return 1;
    }
    
    private static int testWebhook(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DiscordWebhookIntegration discord = DiscordWebhookIntegration.getInstance();
        
        source.sendSuccess(() -> Component.literal("Testing Discord webhook...")
            .withStyle(ChatFormatting.YELLOW), false);
        
        if (discord.testWebhook()) {
            source.sendSuccess(() -> Component.literal("✓ Webhook test successful!")
                .withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendFailure(Component.literal("✗ Webhook test failed. Check console for details.")
                .withStyle(ChatFormatting.RED));
        }
        
        return 1;
    }
    
    private static int setEnabled(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        DiscordWebhookIntegration discord = DiscordWebhookIntegration.getInstance();
        
        discord.updateConfiguration(discord.getMaskedWebhookUrl().equals("Not configured") ? "" : "configured", enabled);
        
        if (enabled) {
            source.sendSuccess(() -> Component.literal("✓ Discord integration enabled")
                .withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendSuccess(() -> Component.literal("✓ Discord integration disabled")
                .withStyle(ChatFormatting.YELLOW), false);
        }
        
        return 1;
    }
    
    private static int setWebhookUrl(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String url = StringArgumentType.getString(context, "url");
        DiscordWebhookIntegration discord = DiscordWebhookIntegration.getInstance();
        
        if (!url.startsWith("https://discord.com/api/webhooks/") && !url.startsWith("https://discordapp.com/api/webhooks/")) {
            source.sendFailure(Component.literal("✗ Invalid Discord webhook URL. Must start with https://discord.com/api/webhooks/")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        discord.updateConfiguration(url, true);
        
        source.sendSuccess(() -> Component.literal("✓ Discord webhook URL updated and integration enabled")
            .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("Use '/discord test' to verify the connection")
            .withStyle(ChatFormatting.GRAY), false);
        
        return 1;
    }
    
    private static int notifyServerStart(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DiscordWebhookIntegration discord = DiscordWebhookIntegration.getInstance();
        
        discord.sendServerStartNotification();
        source.sendSuccess(() -> Component.literal("✓ Server start notification sent to Discord")
            .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }
    
    private static int notifyServerStop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DiscordWebhookIntegration discord = DiscordWebhookIntegration.getInstance();
        
        discord.sendServerStopNotification();
        source.sendSuccess(() -> Component.literal("✓ Server stop notification sent to Discord")
            .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }
    
    private static int sendCustomNotification(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String message = StringArgumentType.getString(context, "message");
        DiscordWebhookIntegration discord = DiscordWebhookIntegration.getInstance();
        
        String adminName = source.getDisplayName().getString();
        discord.sendCustomNotification("Admin Notification", 
            "Message from **" + adminName + "**: " + message, 
            3447003); // Blue color
        
        source.sendSuccess(() -> Component.literal("✓ Custom notification sent to Discord")
            .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }
}
