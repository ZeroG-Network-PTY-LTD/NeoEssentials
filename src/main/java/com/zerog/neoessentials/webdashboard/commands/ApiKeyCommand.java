package com.zerog.neoessentials.webdashboard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.ChatComponentUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.webdashboard.security.ApiKeyManager;
import com.zerog.neoessentials.webdashboard.security.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * /apikey — manage long-lived API keys for external server-to-server integrations (the
 * external dashboard's own backend). Deliberately console/in-game only by default (not just
 * REST-manageable) so an operator can always create/revoke a key even if every dashboard
 * session is locked out or the dashboard itself is down.
 *
 *   /apikey create <label> [role]   — role defaults to ADMIN; prints the full token ONCE
 *   /apikey list                    — shows label/role/last-used, never the secret
 *   /apikey revoke <id>
 */
public class ApiKeyCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyCommand.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("apikey")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.dashboard.apikeys").hasPermission())
            .then(Commands.literal("create")
                .then(Commands.argument("label", StringArgumentType.word())
                    .executes(ctx -> create(ctx, StringArgumentType.getString(ctx, "label"), User.Role.ADMIN))
                    .then(Commands.argument("role", StringArgumentType.word())
                        .executes(ctx -> create(ctx,
                            StringArgumentType.getString(ctx, "label"),
                            parseRole(ctx, StringArgumentType.getString(ctx, "role")))))))
            .then(Commands.literal("list").executes(ApiKeyCommand::list))
            .then(Commands.literal("revoke")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> revoke(ctx, StringArgumentType.getString(ctx, "id")))))
        );
    }

    private static User.Role parseRole(CommandContext<CommandSourceStack> ctx, String raw) {
        try {
            return User.Role.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("§cUnknown role '" + raw + "' — expected ADMIN, OPERATOR, MODERATOR, or VIEWER. Defaulting to ADMIN."));
            return User.Role.ADMIN;
        }
    }

    private static int create(CommandContext<CommandSourceStack> ctx, String label, User.Role role) {
        CommandSourceStack source = ctx.getSource();
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Creating API key '{}' with role {} (requested by {})",
            label, role, source.getTextName());
        String token = ApiKeyManager.getInstance().createKey(label, role);

        source.sendSuccess(() -> Component.literal("§8[§bNE§8] §r§aAPI key '" + label + "' created (role: " + role + ")."), false);
        source.sendSuccess(() -> Component.literal("§7Token (copy this now — it will §cnever be shown again§7):"), false);
        source.sendSuccess(() -> ChatComponentUtil.createCopyableSecret("API key '" + label + "'", token), false);
        source.sendSuccess(() -> Component.literal("§7Give this to the external dashboard's server config, never to a browser/frontend."), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var keys = ApiKeyManager.getInstance().getAllKeys();
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Listing {} API key(s) (requested by {})", keys.size(), source.getTextName());

        if (keys.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7No API keys have been created."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§8[§bNE§8] §r§f" + keys.size() + " API key(s):"), false);
        for (var k : keys) {
            String lastUsed = k.lastUsedAt == 0 ? "never" : TIME_FORMAT.format(Instant.ofEpochMilli(k.lastUsedAt));
            source.sendSuccess(() -> Component.literal(String.format("§7  [%s] §f%s §7— role: %s, %s, last used: %s",
                k.id, k.label, k.role, k.enabled ? "§aenabled" : "§cdisabled", lastUsed)), false);
        }
        return 1;
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx, String id) {
        CommandSourceStack source = ctx.getSource();
        boolean removed = ApiKeyManager.getInstance().revoke(id);

        if (removed) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "API key '{}' revoked (requested by {})", id, source.getTextName());
            source.sendSuccess(() -> Component.literal("§8[§bNE§8] §r§aAPI key '" + id + "' revoked."), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cNo API key found with id '" + id + "'. Use /apikey list to see valid ids."));
            return 0;
        }
    }
}
