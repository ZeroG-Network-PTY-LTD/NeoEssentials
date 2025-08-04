package com.zerog.neoessentials.commands.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.discord.DiscordEnhancedIntegration;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Discord commands with rich embed functionality
 * Extends basic Discord integration with advanced features
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordEnhancedCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("discordenhanced")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("status")
                .executes(DiscordEnhancedCommand::showEnhancedStatus))
            .then(Commands.literal("test")
                .executes(DiscordEnhancedCommand::testEnhancedIntegration))
            .then(Commands.literal("playerstats")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(DiscordEnhancedCommand::sendPlayerStats)))
            .then(Commands.literal("economyreport")
                .executes(DiscordEnhancedCommand::sendEconomyReport))
            .then(Commands.literal("serverstatus")
                .then(Commands.argument("status", StringArgumentType.word())
                    .executes(DiscordEnhancedCommand::sendServerStatus)))
            .then(Commands.literal("embed")
                .then(Commands.argument("title", StringArgumentType.greedyString())
                    .executes(DiscordEnhancedCommand::sendCustomEmbed)))
        );
    }
    
    /**
     * Show enhanced Discord integration status
     */
    private static int showEnhancedStatus(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "discordenhanced status",
            "neoessentials.discord.admin",
            (source) -> {
                Map<String, Object> status = DiscordEnhancedIntegration.getStatus();
                
                source.sendSuccess(() -> Component.literal("§b=== Enhanced Discord Integration Status ==="), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Integration: %s", 
                        (Boolean) status.get("enabled") ? "§aEnabled" : "§cDisabled")), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Webhook: %s", 
                        (Boolean) status.get("webhookConfigured") ? "§aConfigured" : "§cNot Configured")), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Validity: %s", 
                        (Boolean) status.get("webhookValid") ? "§aValid" : "§cInvalid")), false);
                
                source.sendSuccess(() -> Component.literal("§7Enhanced Features:"), false);
                @SuppressWarnings("unchecked")
                java.util.List<String> features = (java.util.List<String>) status.get("features");
                for (String feature : features) {
                    source.sendSuccess(() -> Component.literal("§8  - §e" + feature), false);
                }
                
                return 1;
            }
        );
    }
    
    /**
     * Test enhanced Discord integration
     */
    private static int testEnhancedIntegration(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "discordenhanced test",
            "neoessentials.discord.admin",
            (source) -> {
                DiscordEnhancedIntegration.testEnhancedIntegration(source);
                return 1;
            }
        );
    }
    
    /**
     * Send player statistics to Discord
     */
    private static int sendPlayerStats(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "discordenhanced playerstats",
            "neoessentials.discord.admin",
            (source) -> {
                String playerName = StringArgumentType.getString(context, "player");
                ServerPlayer player = ErrorHandlingIntegration.getPlayerSafely(source, playerName);
                
                DiscordEnhancedIntegration.sendPlayerStats(player);
                source.sendSuccess(() -> Component.literal(
                    String.format("§aSent player statistics for §e%s §ato Discord!", player.getName().getString())), false);
                
                return 1;
            }
        );
    }
    
    /**
     * Send economy report to Discord
     */
    private static int sendEconomyReport(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "discordenhanced economyreport",
            "neoessentials.discord.admin",
            (source) -> {
                // Generate sample economy data (in real implementation, get from EconomyManager)
                Map<String, Object> economyData = generateSampleEconomyData();
                
                DiscordEnhancedIntegration.sendEconomyReport(economyData);
                source.sendSuccess(() -> Component.literal("§aSent economy report to Discord!"), false);
                
                return 1;
            }
        );
    }
    
    /**
     * Send server status update to Discord
     */
    private static int sendServerStatus(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "discordenhanced serverstatus",
            "neoessentials.discord.admin",
            (source) -> {
                String status = StringArgumentType.getString(context, "status");
                Map<String, Object> serverInfo = generateServerInfo(source);
                
                DiscordEnhancedIntegration.sendServerStatus(status, serverInfo);
                source.sendSuccess(() -> Component.literal(
                    String.format("§aSent server status update (%s) to Discord!", status)), false);
                
                return 1;
            }
        );
    }
    
    /**
     * Send custom embed to Discord
     */
    private static int sendCustomEmbed(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "discordenhanced embed",
            "neoessentials.discord.admin",
            (source) -> {
                String title = StringArgumentType.getString(context, "title");
                
                DiscordEnhancedIntegration.EmbedBuilder embed = new DiscordEnhancedIntegration.EmbedBuilder()
                    .setTitle(title)
                    .setDescription("Custom embed sent from NeoEssentials")
                    .setColor(new Color(52, 152, 219))
                    .addField("Sent by", getPlayerName(source), true)
                    .addField("Server", "NeoEssentials Server", true)
                    .setTimestamp(java.time.Instant.now());
                
                DiscordEnhancedIntegration.sendCustomNotification(source, embed);
                
                return 1;
            }
        );
    }
    
    /**
     * Generate sample economy data for testing
     */
    private static Map<String, Object> generateSampleEconomyData() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalMoney", 25000.50);
        data.put("averageBalance", 1250.25);
        data.put("richestPlayer", "ExamplePlayer");
        data.put("recentTransactions", 42);
        data.put("activePlayers", 15);
        data.put("economyHealth", "Excellent");
        return data;
    }
    
    /**
     * Generate server information
     */
    private static Map<String, Object> generateServerInfo(CommandSourceStack source) {
        Map<String, Object> info = new HashMap<>();
        info.put("playersOnline", source.getServer().getPlayerCount());
        info.put("maxPlayers", source.getServer().getMaxPlayers());
        info.put("version", "NeoForge 1.21.3");
        info.put("uptime", "2h 15m");
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        info.put("memoryUsage", String.format("%dMB / %dMB", usedMemory, maxMemory));
        
        return info;
    }
    
    /**
     * Get player name from command source
     */
    private static String getPlayerName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        }
        return "Console";
    }
}
