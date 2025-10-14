package com.zerog.neoessentials.webdashboard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
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
            source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.disabled"));
            source.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.disabled_hint"))
                .withStyle(ChatFormatting.GRAY));
            return 0;
        }
        
        WebDashboardServer server = WebDashboardServer.getInstance();
        
        if (server.isRunning()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.already_running"));
            return 0;
        }
        
        try {
            server.start();
            
            int port = server.getPort();
            String url = "http://localhost:" + port;
            
            Component message = Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.started_header") + "\n")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.started_title") + "\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.started_access"))
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(url)
                    .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            MessageUtil.component("commands.neoessentials.dashboard.click_to_open")))))
                .append(Component.literal("       ║\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.started_footer"))
                    .withStyle(ChatFormatting.GREEN));
            
            source.sendSuccess(() -> message, true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.start_failed", e.getMessage()));
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
            source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.not_running"));
            return 0;
        }
        
        server.stop();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.dashboard.stopped"), true);
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
            
            Component message = Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.status_label"))
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.status_running"))
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal("\n" + MessageUtil.localize("commands.neoessentials.dashboard.status_access"))
                    .withStyle(ChatFormatting.GOLD))
                .append(Component.literal(url)
                    .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            MessageUtil.component("commands.neoessentials.dashboard.click_to_open")))));
            
            source.sendSuccess(() -> message, false);
        } else {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.status_label"))
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.status_stopped"))
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
        
        source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.dashboard.port_future"), false);
        source.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.port_requested", newPort))
            .withStyle(ChatFormatting.GOLD));
        
        return 1;
    }
    
    /**
     * Show help message
     */
    private static int helpDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        Component help = Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_header") + "\n")
            .withStyle(ChatFormatting.AQUA)
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_title") + "\n")
                .withStyle(ChatFormatting.AQUA))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_divider") + "\n")
                .withStyle(ChatFormatting.AQUA))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_start"))
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_start_desc") + "\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_stop"))
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_stop_desc") + "\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_status"))
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_status_desc") + "\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_port"))
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_port_desc") + "\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.help_footer"))
                .withStyle(ChatFormatting.AQUA));
        
        source.sendSuccess(() -> help, false);
        return 1;
    }
}
