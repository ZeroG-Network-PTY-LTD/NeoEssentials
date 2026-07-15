package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.webdashboard.security.DashboardRegistrationManager;
import com.zerog.neoessentials.webdashboard.security.DashboardAccountRegistration;
import com.zerog.neoessentials.webdashboard.security.DiscordAuthProvider;
import com.zerog.neoessentials.webdashboard.security.DiscordUser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command for players to register dashboard accounts
 * Usage:
 * - /dashboardregister start - Start registration process
 * - /dashboardregister complete <username> <password> - Complete registration
 * - /dashboardregister discord - Register using linked Discord account (no password needed)
 * - /dashboardregister status - Check registration status
 */
public class DashboardRegisterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dashboardregister")
            .requires(source -> {
                // Allow console to use the command too for testing
                if (!source.isPlayer()) {
                    return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2); // Op level 2
                }
                // For players, check the dashboard access permission
                return PermissionValidator.validatePermission(source,
                    "neoessentials.dashboard.access").hasPermission();
            })
            .executes(context -> {
                // Default action when just /dashboardregister is used - show help
                CommandSourceStack source = context.getSource();
                source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
                source.sendSuccess(() -> Component.literal("§e§lDashboard Registration"), false);
                source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§7Available commands:"), false);
                source.sendSuccess(() -> Component.literal("  §b/dashboardregister discord §7- Register using Discord §a(recommended)"), false);
                source.sendSuccess(() -> Component.literal("  §e/dashboardregister start §7- Begin manual registration"), false);
                source.sendSuccess(() -> Component.literal("  §e/dashboardregister complete <user> <pass> §7- Finish manual registration"), false);
                source.sendSuccess(() -> Component.literal("  §e/dashboardregister status §7- Check your status"), false);
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§7§oDiscord registration requires an in-game link via Simple Discord Link or Mc2Discord"), false);
                source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
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
     * Register using a Discord account already linked in-game via Simple Discord Link
     * or Mc2Discord. No username/password needed.
     */
    private static int registerWithDiscord(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> Component.literal("§cThis command can only be used by players"), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Check if already registered
        if (manager.isRegistered(player.getUUID())) {
            source.sendSuccess(() -> Component.literal("§e§lINFO: §eYou already have a registered dashboard account!"), false);
            source.sendSuccess(() -> Component.literal("§7Use the §bLogin with Discord §7button on the dashboard to sign in"), false);
            return 0;
        }

        // Require a Discord companion mod (SDLink or Mc2Discord) to be installed and ready
        DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
        if (!discordProvider.isAvailable()) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cDiscord registration is not available"), false);
            source.sendSuccess(() -> Component.literal("§7Simple Discord Link or Mc2Discord is not installed/connected on this server"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboardregister start §7to register manually instead"), false);
            return 0;
        }

        // Look up linked Discord account
        DiscordUser discordUser = discordProvider.getLinkedAccountByUuid(player.getUUID());
        if (discordUser == null || !discordUser.isLinked()) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cNo Discord account linked to your Minecraft account"), false);
            source.sendSuccess(() -> Component.literal("§7Link your Discord first using the §e/link §7command in the Discord server"), false);
            source.sendSuccess(() -> Component.literal("§7Or use §e/dashboardregister start §7to register manually"), false);
            return 0;
        }

        String discordId = discordUser.getDiscordId();
        String discordUsername = discordUser.getDiscordUsername();

        // Attempt registration
        DashboardAccountRegistration registration = manager.registerWithDiscord(
            player.getUUID(), player.getName().getString(), discordId, discordUsername);

        if (registration == null) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cDiscord registration failed!"), false);
            source.sendSuccess(() -> Component.literal("§7Possible reasons:"), false);
            source.sendSuccess(() -> Component.literal("§7- Dashboard username §e" + player.getName().getString() + " §7is already taken"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboardregister start §7to register with a custom username"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal("§a§l✓ Discord Registration Successful!"), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7Dashboard Username: §a" + registration.getDashboardUsername()), false);
        source.sendSuccess(() -> Component.literal("§7Linked Minecraft: §e" + player.getName().getString()), false);
        source.sendSuccess(() -> Component.literal("§7Linked Discord: §b" + discordUsername), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7To log in, use the §b§lLogin with Discord §r§7button on the dashboard"), false);
        source.sendSuccess(() -> Component.literal("§7No password needed — Discord is your login!"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7View the dashboard URL with: §e/dashboard url"), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

        return 1;
    }

    private static int startRegistration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> Component.literal("§cThis command can only be used by players"), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Debug logging
        System.out.println("[DashboardRegister] Player " + player.getName().getString() + " (" + player.getUUID() + ") attempting registration");

        // Check if already registered
        if (manager.isRegistered(player.getUUID())) {
            source.sendSuccess(() -> Component.literal("§e§lINFO: §eYou already have a registered dashboard account!"), false);
            source.sendSuccess(() -> Component.literal("§7Use your dashboard credentials to log in"), false);
            System.out.println("[DashboardRegister] Player already registered");
            return 0;
        }

        // Start registration
        String token = manager.startRegistration(player.getUUID(), player.getName().getString());

        System.out.println("[DashboardRegister] Registration token generated: " + (token != null ? "SUCCESS" : "FAILED"));

        if (token == null) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cFailed to start registration"), false);
            source.sendSuccess(() -> Component.literal("§7Please contact a server administrator"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal("§a§lDashboard Registration Started"), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7Your registration token: §e" + token), false);
        source.sendSuccess(() -> Component.literal("§7This token expires in §c5 minutes"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7To complete registration, use:"), false);
        source.sendSuccess(() -> Component.literal("§b/dashboardregister complete <username> <password>"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§eExample:"), false);
        source.sendSuccess(() -> Component.literal("§7/dashboardregister complete myusername MySecurePass123"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§c§lWARNING: §cPassword must be at least 8 characters"), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

        return 1;
    }

    private static int completeRegistration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> Component.literal("§cThis command can only be used by players"), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        String username = StringArgumentType.getString(context, "username");
        String password = StringArgumentType.getString(context, "password");

        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Check if already registered
        if (manager.isRegistered(player.getUUID())) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cYou already have a registered dashboard account!"), false);
            return 0;
        }

        // Validate username
        if (username.length() < 3 || username.length() > 20) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cUsername must be 3-20 characters"), false);
            return 0;
        }

        // Validate password
        if (password.length() < 8) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cPassword must be at least 8 characters"), false);
            return 0;
        }

        // For security, we need to get the token from the pending registration
        // Since we can't pass it securely, we'll lookup by UUID
        // This requires a small modification to complete registration

        source.sendSuccess(() -> Component.literal("§6Processing registration..."), false);

        // Try to complete registration
        DashboardAccountRegistration registration = completeRegistrationByUuid(
            player.getUUID(), username, password);

        if (registration == null) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cRegistration failed!"), false);
            source.sendSuccess(() -> Component.literal("§7Possible reasons:"), false);
            source.sendSuccess(() -> Component.literal("§7- Registration token expired (5 min limit)"), false);
            source.sendSuccess(() -> Component.literal("§7- Username already taken"), false);
            source.sendSuccess(() -> Component.literal("§7- No registration started"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboardregister start §7to begin"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal("§a§l✓ Registration Successful!"), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7Dashboard Username: §a" + registration.getDashboardUsername()), false);
        source.sendSuccess(() -> Component.literal("§7Linked to: §e" + player.getName().getString()), false);
        source.sendSuccess(() -> Component.literal(""), false);

        // Auto-link Discord if SDLink is available and the player has a linked account
        DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
        if (discordProvider.isAvailable()) {
            DiscordUser discordUser = discordProvider.getLinkedAccountByUuid(player.getUUID());
            if (discordUser != null && discordUser.isLinked()) {
                boolean linked = DashboardRegistrationManager.getInstance().linkDiscordAccount(
                    player.getUUID(), discordUser.getDiscordId(), discordUser.getDiscordUsername());
                if (linked) {
                    source.sendSuccess(() -> Component.literal("§b§l Discord Auto-Linked: §b" + discordUser.getDiscordUsername()), false);
                    source.sendSuccess(() -> Component.literal("§7You can also log in using the §b§lLogin with Discord §r§7button!"), false);
                    source.sendSuccess(() -> Component.literal(""), false);
                }
            }
        }

        source.sendSuccess(() -> Component.literal("§7You can now log in to the dashboard at:"), false);
        source.sendSuccess(() -> Component.literal("§b§n/dashboard url"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§7Use your dashboard username and password to log in"), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

        return 1;
    }

    private static int checkStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> Component.literal("§cThis command can only be used by players"), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal("§e§lDashboard Registration Status"), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        if (manager.isRegistered(player.getUUID())) {
            DashboardAccountRegistration reg = manager.getRegistration(player.getUUID());
            source.sendSuccess(() -> Component.literal("§a§l✓ §aRegistered"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§7Dashboard Username: §a" + reg.getDashboardUsername()), false);
            source.sendSuccess(() -> Component.literal("§7Minecraft Account: §e" + reg.getMinecraftUsername()), false);
            source.sendSuccess(() -> Component.literal("§7Registered: §7" + formatTimestamp(reg.getRegisteredAt())), false);

            if (reg.isDiscordLinked()) {
                source.sendSuccess(() -> Component.literal("§7Discord: §b" + reg.getDiscordUsername() + " §a(Linked)"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§7Discord: §c(Not Linked)"), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cNot Registered"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            // Show Discord tip if SDLink is available and they're linked
            DiscordAuthProvider discordProvider = DiscordAuthProvider.getInstance();
            if (discordProvider.isAvailable() && discordProvider.isAccountLinkedByUuid(player.getUUID())) {
                source.sendSuccess(() -> Component.literal("§b§l TIP: §bYou have Discord linked! Run §e/dashboardregister discord §bto register instantly"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§7Use §e/dashboardregister start §7to register"), false);
                source.sendSuccess(() -> Component.literal("§7Or §b/dashboardregister discord §7if you have Discord linked via SDLink"), false);
            }
        }

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

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
