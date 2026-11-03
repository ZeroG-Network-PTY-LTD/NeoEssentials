package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.webdashboard.security.DashboardLoginRequestManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * /dashboardlogin <code> — proves live possession of this Minecraft account to complete a
 * dashboard login attempt (Discord-linked or legacy Minecraft-username auth) started from the
 * browser via POST /api/auth/login-request. Without this step, the dashboard previously minted
 * a real session straight from an unauthenticated username string — anyone could impersonate
 * any staff member by name alone. The code is shown on the dashboard's login page and typed
 * here; the browser then polls GET /api/auth/login-request/status until this redeems it.
 */
public class DashboardLoginCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardLoginCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("dashboardlogin")) {
            return;
        }
        dispatcher.register(Commands.literal("dashboardlogin")
            .then(Commands.argument("code", StringArgumentType.word())
                .executes(ctx -> login(ctx, StringArgumentType.getString(ctx, "code"))))
        );
        NeoLog.debug(LOGGER, LogCategory.COMMANDS, "Registered /dashboardlogin command");
    }

    private static int login(CommandContext<CommandSourceStack> ctx, String code) {
        CommandSourceStack source = ctx.getSource();

        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("§cThis command can only be used by a player."));
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardLoginRequestManager.LinkResultLike result = DashboardLoginRequestManager.getInstance()
            .completeLoginRequest(code.toUpperCase(), player.getUUID(), player.getName().getString());

        if (result.success) {
            source.sendSuccess(() -> Component.literal("§8[§bNE§8] §r§aDashboard login confirmed — check your browser."), false);
            NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/dashboardlogin succeeded for {}", player.getName().getString());
            return 1;
        } else {
            source.sendFailure(Component.literal("§c" + result.message));
            NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/dashboardlogin failed for {}: {}", player.getName().getString(), result.message);
            return 0;
        }
    }
}
