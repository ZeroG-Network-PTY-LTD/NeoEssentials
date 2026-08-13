package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.webdashboard.security.DashboardRegistrationManager;
import com.zerog.neoessentials.webdashboard.security.DashboardAccountRegistration;
import com.zerog.neoessentials.webdashboard.security.DiscordAuthProvider;
import com.zerog.neoessentials.webdashboard.security.DiscordUser;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for players to register dashboard accounts
 * Usage:
 * - /dashboardregister start - Start registration process
 * - /dashboardregister complete <username> <password> - Complete registration
 * - /dashboardregister discord - Register using linked Discord account (no password needed)
 * - /dashboardregister status - Check registration status
 */
public class DashboardRegisterCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardRegisterCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("dashboardregister")) {
            return;
        }
        dispatcher.register(Commands.literal("dashboardregister")
            .requires(source -> {
                // Allow console to use the command too for testing
                if (!source.isPlayer()) {
                    return source.hasPermission(2); // Op level 2
                }
                // For players, check the dashboard access permission
                return PermissionValidator.validatePermission(source,
                    "neoessentials.dashboard.access").hasPermission();
            })
            .executes(context -> {
                // Default action when just /dashboardregister is used - show help
                CommandSourceStack source = context.getSource();
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.help_title"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.help_available_commands"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.help_discord_line"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.help_start_line"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.help_complete_line"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.help_status_line"), false);
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.help_footer_note"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
                return 1;
            })
            .then(Commands.literal("discord")
                .executes(DashboardRegisterCommand::registerWithDiscord))
            .then(Commands.literal("start")
                .executes(DashboardRegisterCommand::startRegistration))
            .then(Commands.literal("complete")
                .then(Commands.argument("username", StringArgumentType.word())
                    .then(Commands.argument("password", StringArgumentType.greedyString())
                        .executes(DashboardRegisterCommand::completeRegistration))))
            .then(Commands.literal("status")
                .executes(DashboardRegisterCommand::checkStatus))
        );
    }

    /**
     * Register using linked Discord account via Simple Discord Link (SDLink).
     * No username/password needed — the player logs in via the Discord OAuth2 button.
     */
    private static int registerWithDiscord(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.players_only"), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Check if already registered
        if (manager.isRegistered(player.getUUID())) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.already_registered_info"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.already_registered_hint_discord"), false);
            return 0;
        }

        // Require a Discord companion mod (SDLink, Mc2Discord, or DCIntegration) to be installed and ready
        DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
        if (!discordProvider.isAvailable()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_unavailable"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_sdlink_missing"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.hint_manual_instead"), false);
            return 0;
        }

        // Look up linked Discord account
        DiscordUser discordUser = discordProvider.getLinkedAccountByUuid(player.getUUID());
        if (discordUser == null || !discordUser.isLinked()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_not_linked"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_link_hint"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.hint_manual_alt"), false);
            return 0;
        }

        String discordId = discordUser.getDiscordId();
        String discordUsername = discordUser.getDiscordUsername();

        // Attempt registration
        DashboardAccountRegistration registration = manager.registerWithDiscord(
            player.getUUID(), player.getName().getString(), discordId, discordUsername);

        if (registration == null) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_reg_failed"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.possible_reasons_header"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.username_taken_reason", player.getName().getString()), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.hint_custom_username"), false);
            return 0;
        }

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_success_title"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.dashboard_username_line", registration.getDashboardUsername()), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.linked_minecraft_line", player.getName().getString()), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.linked_discord_line", discordUsername), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.login_discord_hint"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.no_password_note"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.view_url_hint"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);

        return 1;
    }

    private static int startRegistration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.players_only"), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        NeoLog.debug(LOGGER, LogCategory.COMMANDS, "Player {} ({}) attempting dashboard registration", player.getName().getString(), player.getUUID());

        // Check if already registered
        if (manager.isRegistered(player.getUUID())) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.already_registered_info"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.already_registered_hint_creds"), false);
            NeoLog.debug(LOGGER, LogCategory.COMMANDS, "Dashboard registration aborted — player {} already registered", player.getName().getString());
            return 0;
        }

        // Start registration
        String token = manager.startRegistration(player.getUUID(), player.getName().getString());

        NeoLog.debug(LOGGER, LogCategory.COMMANDS, "Dashboard registration token generated for {}: {}", player.getName().getString(), token != null ? "SUCCESS" : "FAILED");

        if (token == null) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.reg_start_failed"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.contact_admin_hint"), false);
            return 0;
        }

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.reg_started_title"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.reg_token_line", token), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.token_expiry_note"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.complete_instructions_hint"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.complete_command_syntax"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.example_label"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.example_command"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.password_length_warning"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);

        return 1;
    }

    private static int completeRegistration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.players_only"), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        String username = StringArgumentType.getString(context, "username");
        String password = StringArgumentType.getString(context, "password");

        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Check if already registered
        if (manager.isRegistered(player.getUUID())) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.already_registered_error"), false);
            return 0;
        }

        // Validate username
        if (username.length() < 3 || username.length() > 20) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.username_length_error"), false);
            return 0;
        }

        // Validate password
        if (password.length() < 8) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.password_length_error"), false);
            return 0;
        }

        // For security, we need to get the token from the pending registration
        // Since we can't pass it securely, we'll lookup by UUID
        // This requires a small modification to complete registration

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.processing_registration"), false);

        // Try to complete registration
        DashboardAccountRegistration registration = completeRegistrationByUuid(
            player.getUUID(), username, password);

        if (registration == null) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.reg_failed_generic"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.possible_reasons_header"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.reason_token_expired"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.reason_username_taken"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.reason_no_registration"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.hint_start_to_begin"), false);
            return 0;
        }

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.reg_success_title"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.dashboard_username_line", registration.getDashboardUsername()), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.linked_to_line", player.getName().getString()), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // Auto-link Discord if SDLink is available and the player has a linked account
        DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
        if (discordProvider.isAvailable()) {
            DiscordUser discordUser = discordProvider.getLinkedAccountByUuid(player.getUUID());
            if (discordUser != null && discordUser.isLinked()) {
                boolean linked = DashboardRegistrationManager.getInstance().linkDiscordAccount(
                    player.getUUID(), discordUser.getDiscordId(), discordUser.getDiscordUsername());
                if (linked) {
                    source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_autolinked_line", discordUser.getDiscordUsername()), false);
                    source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_autolinked_hint"), false);
                    source.sendSuccess(() -> Component.literal(""), false);
                }
            }
        }

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.login_now_hint"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.dashboard_url_placeholder"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.use_creds_hint"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);

        return 1;
    }

    private static int checkStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.players_only"), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.status_title"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        if (manager.isRegistered(player.getUUID())) {
            DashboardAccountRegistration reg = manager.getRegistration(player.getUUID());
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.registered_label"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.dashboard_username_line", reg.getDashboardUsername()), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.minecraft_account_line", reg.getMinecraftUsername()), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.registered_at_line", formatTimestamp(reg.getRegisteredAt())), false);

            if (reg.isDiscordLinked()) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_linked_line", reg.getDiscordUsername()), false);
            } else {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_not_linked_line"), false);
            }
        } else {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.not_registered_label"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            // Show Discord tip if SDLink is available and they're linked
            DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
            if (discordProvider.isAvailable() && discordProvider.isAccountLinkedByUuid(player.getUUID())) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.discord_tip"), false);
            } else {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.hint_register"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.hint_register_discord_alt"), false);
            }
        }

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboardregister.separator"), false);

        return 1;
    }

    /**
     * Helper method to complete registration by UUID
     * This allows us to avoid passing the token through chat
     */
    private static DashboardAccountRegistration completeRegistrationByUuid(
            java.util.UUID playerUuid, String username, String password) {

        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Find pending registration by UUID
        // We need to add this method to DashboardRegistrationManager
        return manager.completeRegistrationByUuid(playerUuid, username, password);
    }

    /**
     * Format timestamp for display
     */
    private static String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
}
