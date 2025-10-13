package com.zerog.neoessentials.webdashboard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.webdashboard.WebDashboardServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

/**
 * Command to control the web dashboard server
 * Usage: /dashboard <start|stop|status|port>
 */
public class DashboardCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dashboard")
            .requires(source -> source.hasPermission(3)) // Operator level
            .then(Commands.literal("start")
                .executes(DashboardCommand::startDashboard))
            .then(Commands.literal("stop")
                .executes(DashboardCommand::stopDashboard))
            .then(Commands.literal("status")
                .executes(DashboardCommand::statusDashboard))
            .then(Commands.literal("port")
                .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
                    .executes(DashboardCommand::changeDashboardPort)))
            .executes(DashboardCommand::helpDashboard)
        );
    }
    
    /**
     * Start the dashboard server
     */
    private static int startDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check if web dashboard is enabled in config
        com.zerog.neoessentials.config.ConfigManager configManager = 
            com.zerog.neoessentials.config.ConfigManager.getInstance();
        
        if (!configManager.isWebDashboardEnabled()) {
            source.sendFailure(Component.literal("Web dashboard is disabled in config!")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal("\nEnable it in config.json: modules.webDashboardEnabled = true")
                    .withStyle(ChatFormatting.GRAY)));
            return 0;
        }
        
        WebDashboardServer server = WebDashboardServer.getInstance();
        
        if (server.isRunning()) {
            source.sendFailure(Component.literal("Web dashboard is already running!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        try {
            server.start();
            
            int port = server.getPort();
            String url = "http://localhost:" + port;
            
            Component message = Component.literal("╔════════════════════════════════════════════╗\n")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal("║  Web Dashboard Started Successfully!       ║\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("║  Access at: ")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(url)
                    .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            Component.literal("Click to open dashboard")))))
                .append(Component.literal("       ║\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("╚════════════════════════════════════════════╝")
                    .withStyle(ChatFormatting.GREEN));
            
            source.sendSuccess(() -> message, true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to start web dashboard: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Stop the dashboard server
     */
    private static int stopDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardServer server = WebDashboardServer.getInstance();
        
        if (!server.isRunning()) {
            source.sendFailure(Component.literal("Web dashboard is not running!")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        server.stop();
        source.sendSuccess(() -> Component.literal("Web dashboard stopped successfully")
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
    
    /**
     * Check dashboard status
     */
    private static int statusDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardServer server = WebDashboardServer.getInstance();
        
        if (server.isRunning()) {
            String url = "http://localhost:" + server.getPort();
            
            Component message = Component.literal("Web Dashboard Status: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Running")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("\nAccess at: ")
                    .withStyle(ChatFormatting.GOLD))
                .append(Component.literal(url)
                    .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            Component.literal("Click to open dashboard")))));
            
            source.sendSuccess(() -> message, false);
        } else {
            source.sendSuccess(() -> Component.literal("Web Dashboard Status: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Stopped")
                    .withStyle(ChatFormatting.RED)), false);
        }
        
        return 1;
    }
    
    /**
     * Change dashboard port (requires restart)
     */
    private static int changeDashboardPort(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int newPort = IntegerArgumentType.getInteger(context, "port");
        
        source.sendSuccess(() -> Component.literal("Dashboard port configuration will be available in a future update.")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal("\nRequested port: " + newPort)
                .withStyle(ChatFormatting.GOLD)), false);
        
        return 1;
    }
    
    /**
     * Show help message
     */
    private static int helpDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        Component help = Component.literal("╔════════════════════════════════════════════╗\n")
            .withStyle(ChatFormatting.AQUA)
            .append(Component.literal("║         Web Dashboard Commands             ║\n")
                .withStyle(ChatFormatting.AQUA))
            .append(Component.literal("╠════════════════════════════════════════════╣\n")
                .withStyle(ChatFormatting.AQUA))
            .append(Component.literal("║ /dashboard start  ")
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("- Start the web server    ║\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal("║ /dashboard stop   ")
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("- Stop the web server     ║\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal("║ /dashboard status ")
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("- Check server status     ║\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal("║ /dashboard port <num> ")
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("- Set port (future)  ║\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal("╚════════════════════════════════════════════╝")
                .withStyle(ChatFormatting.AQUA));
        
        source.sendSuccess(() -> help, false);
        return 1;
    }
}
