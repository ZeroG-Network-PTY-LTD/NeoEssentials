package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.DiscordConfig;
import com.zerog.neoessentials.discord.DiscordManager;
import com.zerog.neoessentials.integrations.DiscordWebhookIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discord Management Command for NeoEssentials
 * Complete Discord integration management and configuration
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordManagementCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordManagementCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("discordmanage")
            .requires(source -> source.hasPermission(4)) // Admin only
            .executes(DiscordManagementCommand::showStatus)
            .then(Commands.literal("status")
                .executes(DiscordManagementCommand::showStatus))
            .then(Commands.literal("enable")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(DiscordManagementCommand::setWebhookEnabled)))
            .then(Commands.literal("webhook")
                .then(Commands.argument("url", StringArgumentType.greedyString())
                    .executes(DiscordManagementCommand::setWebhookUrl)))
            .then(Commands.literal("test")
                .executes(DiscordManagementCommand::testIntegration))
            .then(Commands.literal("reload")
                .executes(DiscordManagementCommand::reloadConfig))
            .then(Commands.literal("setup")
                .executes(DiscordManagementCommand::showSetupGuide))
        );
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ConfigManager configManager = ConfigManager.getInstance();
        DiscordConfig config = configManager.getDiscordConfig();
        DiscordManager discordManager = DiscordManager.getInstance();
        DiscordWebhookIntegration webhookIntegration = DiscordWebhookIntegration.getInstance();
        
        source.sendSuccess(() -> Component.literal("=== Discord Integration Status ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        // Overall status
        if (config.webhooks.enabled && !config.webhooks.chatWebhookUrl.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Overall Status: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("ENABLED").withStyle(ChatFormatting.GREEN)), false);
        } else {
            source.sendSuccess(() -> Component.literal("Overall Status: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("DISABLED").withStyle(ChatFormatting.RED)), false);
        }
        
        // Configuration details
        source.sendSuccess(() -> Component.literal("Webhook Enabled: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.valueOf(config.webhooks.enabled))
                .withStyle(config.webhooks.enabled ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        
        // Webhook URL (masked for security)
        final String maskedUrl;
        if (!config.webhooks.chatWebhookUrl.isEmpty()) {
            String url = config.webhooks.chatWebhookUrl;
            if (url.length() > 15) {
                maskedUrl = "***" + url.substring(url.length() - 12);
            } else {
                maskedUrl = "***configured***";
            }
        } else {
            maskedUrl = "Not configured";
        }
        source.sendSuccess(() -> Component.literal("Webhook URL: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(maskedUrl).withStyle(ChatFormatting.YELLOW)), false);
        
        // Integration status
        source.sendSuccess(() -> Component.literal("Discord Manager: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(discordManager.isEnabled() ? "ENABLED" : "DISABLED")
                .withStyle(discordManager.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        
        source.sendSuccess(() -> Component.literal("Webhook Integration: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(webhookIntegration.isEnabled() ? "ENABLED" : "DISABLED")
                .withStyle(webhookIntegration.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        
        // Commands
        source.sendSuccess(() -> Component.literal("Commands:")
            .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("  /discordmanage setup - Show setup guide")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /discordmanage webhook <url> - Set webhook URL")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /discordmanage enable <true/false> - Enable/disable")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /discordmanage test - Test integration")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("  /discordmanage reload - Reload configuration")
            .withStyle(ChatFormatting.WHITE), false);
        
        return 1;
    }
    
    private static int setWebhookEnabled(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ConfigManager configManager = ConfigManager.getInstance();
        DiscordConfig config = configManager.getDiscordConfig();
        
        config.webhooks.enabled = enabled;
        configManager.saveAll();
        configManager.reloadDiscordConfig();
        
        if (enabled) {
            if (config.webhooks.chatWebhookUrl.isEmpty()) {
                source.sendFailure(Component.literal("✗ Cannot enable Discord integration without webhook URL!")
                    .withStyle(ChatFormatting.RED));
                source.sendSuccess(() -> Component.literal("Use: /discordmanage webhook <url>")
                    .withStyle(ChatFormatting.YELLOW), false);
                return 0;
            }
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
        ConfigManager configManager = ConfigManager.getInstance();
        DiscordConfig config = configManager.getDiscordConfig();
        
        // Validate webhook URL
        if (!url.startsWith("https://discord.com/api/webhooks/") && 
            !url.startsWith("https://discordapp.com/api/webhooks/")) {
            source.sendFailure(Component.literal("✗ Invalid Discord webhook URL!")
                .withStyle(ChatFormatting.RED));
            source.sendSuccess(() -> Component.literal("URL must start with: https://discord.com/api/webhooks/")
                .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        
        config.configureWebhook(url);
        configManager.saveAll();
        configManager.reloadDiscordConfig();
        
        source.sendSuccess(() -> Component.literal("✓ Discord webhook URL configured and integration enabled")
            .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("Use '/discordmanage test' to verify the connection")
            .withStyle(ChatFormatting.GRAY), false);
        
        return 1;
    }
    
    private static int testIntegration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DiscordWebhookIntegration webhookIntegration = DiscordWebhookIntegration.getInstance();
        
        if (!webhookIntegration.isEnabled()) {
            source.sendFailure(Component.literal("✗ Discord integration is not enabled!")
                .withStyle(ChatFormatting.RED));
            source.sendSuccess(() -> Component.literal("Use '/discordmanage setup' for configuration help")
                .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("Testing Discord integration...")
            .withStyle(ChatFormatting.YELLOW), false);
        
        try {
            String adminName = source.getDisplayName().getString();
            webhookIntegration.sendCustomNotification(
                "🔧 Integration Test", 
                "This is a test message from **" + adminName + "** via NeoEssentials Discord integration!", 
                5793266 // Purple color
            );
            
            source.sendSuccess(() -> Component.literal("✓ Test message sent successfully!")
                .withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("Check your Discord channel to verify the message was received")
                .withStyle(ChatFormatting.GRAY), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("✗ Test failed: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            LOGGER.error("Discord integration test failed", e);
        }
        
        return 1;
    }
    
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ConfigManager configManager = ConfigManager.getInstance();
        
        try {
            configManager.reloadDiscordConfig();
            source.sendSuccess(() -> Component.literal("✓ Discord configuration reloaded")
                .withStyle(ChatFormatting.GREEN), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("✗ Failed to reload configuration: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            LOGGER.error("Failed to reload Discord configuration", e);
        }
        
        return 1;
    }
    
    private static int showSetupGuide(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Discord Integration Setup Guide ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        source.sendSuccess(() -> Component.literal("1. Create a Discord Webhook:")
            .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("   • Go to your Discord server settings")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("   • Navigate to Integrations > Webhooks")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("   • Click 'New Webhook' or 'Create Webhook'")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("   • Choose the channel for server notifications")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("   • Copy the webhook URL")
            .withStyle(ChatFormatting.WHITE), false);
        
        source.sendSuccess(() -> Component.literal("2. Configure NeoEssentials:")
            .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("   • Run: /discordmanage webhook <your_webhook_url>")
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("   • Test: /discordmanage test")
            .withStyle(ChatFormatting.YELLOW), false);
        
        source.sendSuccess(() -> Component.literal("3. Features enabled:")
            .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("   • Player join/leave notifications")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("   • Server start/stop notifications")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("   • Admin command notifications")
            .withStyle(ChatFormatting.WHITE), false);
        source.sendSuccess(() -> Component.literal("   • Custom notifications via commands")
            .withStyle(ChatFormatting.WHITE), false);
        
        source.sendSuccess(() -> Component.literal("Need help? Check the discord.json config file in:")
            .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("config/neoessentials/discord.json")
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
}
