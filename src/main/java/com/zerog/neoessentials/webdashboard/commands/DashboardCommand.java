package com.zerog.neoessentials.webdashboard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.PermissionValidator.PermissionResult;
import com.zerog.neoessentials.webdashboard.WebDashboardServer;
import com.zerog.neoessentials.webdashboard.security.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
// import net.minecraft.commands.Commands; // Unused after dashboard disable
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Command to control the web dashboard server
 * Usage: /dashboard <start|stop|status|port|temppass>
 * 
 * Permissions:
 * - neoessentials.dashboard.admin - Full dashboard control (start/stop/port)
 * - neoessentials.dashboard.status - View dashboard status
 * - neoessentials.dashboard.temppass - Generate temporary passwords
 */
public class DashboardCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardCommand.class);
    
    // Permission nodes
    private static final String PERM_ADMIN = "neoessentials.dashboard.admin";
    private static final String PERM_STATUS = "neoessentials.dashboard.status";
    private static final String PERM_TEMPPASS = "neoessentials.dashboard.temppass";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Dashboard command registration disabled
        // dispatcher.register(Commands.literal("dashboard")
        //     .requires(source -> source.hasPermission(2)) // Allow if operator, then check specific permissions
        //     .then(Commands.literal("start")
        //         .executes(DashboardCommand::startDashboard))
        //     .then(Commands.literal("stop")
        //         .executes(DashboardCommand::stopDashboard))
        //     .then(Commands.literal("status")
        //         .executes(DashboardCommand::statusDashboard))
        //     .then(Commands.literal("port")
        //         .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
        //             .executes(DashboardCommand::changeDashboardPort)))
        //     .then(Commands.literal("temppass")
        //         .then(Commands.argument("username", net.minecraft.commands.arguments.EntityArgument.player())
        //             .executes(DashboardCommand::generateTempPassword)))
        //     .executes(DashboardCommand::helpDashboard)
        // );
    }
    
    /**
     * Start the dashboard server
     */
    @SuppressWarnings("unused")
    private static int startDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permission
        PermissionResult permResult = PermissionValidator.validatePermission(source, PERM_ADMIN);
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
        // Check if web dashboard is enabled in config
        com.zerog.neoessentials.config.ConfigManager configManager = 
            com.zerog.neoessentials.config.ConfigManager.getInstance();
        
        if (!com.zerog.neoessentials.config.ConfigManager.isWebDashboardEnabled()) {
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
    @SuppressWarnings("unused")
    private static int stopDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permission
        PermissionResult permResult = PermissionValidator.validatePermission(source, PERM_ADMIN);
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
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
    @SuppressWarnings("unused")
    private static int statusDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permission
        PermissionResult permResult = PermissionValidator.validatePermission(source, PERM_STATUS);
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
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
    @SuppressWarnings("unused")
    private static int changeDashboardPort(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permission
        PermissionResult permResult = PermissionValidator.validatePermission(source, PERM_ADMIN);
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
        int newPort = IntegerArgumentType.getInteger(context, "port");
        
        source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.dashboard.port_future"), false);
        source.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.port_requested", newPort))
            .withStyle(ChatFormatting.GOLD));
        
        return 1;
    }
    
    /**
     * Show help message
     */
    @SuppressWarnings("unused")
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
    
    /**
     * Generate temporary password for a player (requires Discord verification and roles)
     */
    @SuppressWarnings("unused")
    private static int generateTempPassword(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permission
        PermissionResult permResult = PermissionValidator.validatePermission(source, PERM_TEMPPASS);
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
        try {
            // Get the target player
            ServerPlayer targetPlayer = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "username");
            String minecraftUsername = targetPlayer.getName().getString();
            
            // Check if Discord authentication is available
            DiscordAuthProvider discordAuth = DiscordAuthProvider.getInstance();
            if (!discordAuth.isAvailable()) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.temppass.discord_not_available"));
                return 0;
            }
            
            // Get linked Discord account
            DiscordUser discordUser = discordAuth.getLinkedAccount(minecraftUsername);
            if (discordUser == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.temppass.player_not_verified", minecraftUsername));
                return 0;
            }
            
            // Check if user has any of the required Discord roles
            DiscordAuthConfig discordConfig = DiscordAuthConfig.load();
            List<String> userRoles = discordUser.getDiscordRoles();
            
            // Debug logging to help diagnose issues
            LOGGER.info("=== Discord Role Check for {} ===", minecraftUsername);
            LOGGER.info("User's Discord Roles: {}", userRoles);
            LOGGER.info("Configured Role Mappings: {}", discordConfig.getRoleMapping().keySet());
            
            boolean hasRole = false;
            for (String roleId : userRoles) {
                if (discordConfig.getRoleMapping().containsKey(roleId)) {
                    hasRole = true;
                    LOGGER.info("✓ Role match found: {} -> {}", roleId, discordConfig.getRoleMapping().get(roleId));
                    break;
                }
            }
            
            if (!hasRole) {
                LOGGER.warn("✗ No matching roles found for player {}", minecraftUsername);
                LOGGER.warn("  Player has roles: {}", userRoles);
                LOGGER.warn("  Config expects one of: {}", discordConfig.getRoleMapping().keySet());
                source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.temppass.no_roles", minecraftUsername));
                return 0;
            }
            
            // Check if dashboard user exists, create if needed
            AuthenticationManager authManager = AuthenticationManager.getInstance();
            User dashboardUser = authManager.getUserByUsername(minecraftUsername);
            
            if (dashboardUser == null) {
                // Create new user with determined role
                String roleId = userRoles.stream()
                    .filter(r -> discordConfig.getRoleMapping().containsKey(r))
                    .findFirst()
                    .orElse(null);
                
                if (roleId == null) {
                    source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.temppass.no_roles", minecraftUsername));
                    return 0;
                }
                
                String roleName = discordConfig.getRoleMapping().get(roleId);
                User.Role userRole = User.Role.valueOf(roleName.toUpperCase());
                
                // Create user with a placeholder password (will be replaced by temp password)
                dashboardUser = authManager.createUser(minecraftUsername, "placeholder12345", 
                    discordUser.getDiscordUsername() + "@discord", userRole);
            }
            
            // Generate temporary password
            String tempPassword = authManager.generateTempPassword(minecraftUsername);
            
            // Send success message with the temporary password
            Component message = Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.temppass.success_header") + "\n")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.temppass.success_title") + "\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.temppass.success_divider") + "\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.temppass.username_line", minecraftUsername) + "\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.temppass.password_line", tempPassword) + "\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.temppass.warning_line") + "\n")
                    .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.temppass.footer"))
                    .withStyle(ChatFormatting.GREEN));
            
            source.sendSuccess(() -> message, false);
            
            // Also send a private message to the target player with their temp password
            targetPlayer.sendSystemMessage(Component.literal("§a§lYou have been granted dashboard access!")
                .append(Component.literal("\n§eUsername: §f" + minecraftUsername))
                .append(Component.literal("\n§eTemporary Password: §f" + tempPassword))
                .append(Component.literal("\n§c§lIMPORTANT: §7You must change this password on first login!")));
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.dashboard.temppass.failed", e.getMessage()));
            return 0;
        }
    }
}
