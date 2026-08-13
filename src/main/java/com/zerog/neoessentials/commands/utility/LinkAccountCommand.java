package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.webdashboard.security.MinecraftAccountLinkManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * /linkaccount <code> — lets a player prove ownership of their Minecraft account to an
 * EXISTING dashboard account (however it was created), the reverse direction of
 * /dashboardregister (which creates a new dashboard account starting from an in-game player).
 * The code is generated from the dashboard's own Settings page (POST /api/auth/link-minecraft/start)
 * and typed here to complete the link.
 */
public class LinkAccountCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkAccountCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("linkaccount")) {
            return;
        }
        dispatcher.register(Commands.literal("linkaccount")
            .then(Commands.argument("code", StringArgumentType.word())
                .executes(ctx -> link(ctx, StringArgumentType.getString(ctx, "code"))))
        );
        NeoLog.debug(LOGGER, LogCategory.COMMANDS, "Registered /linkaccount command");
    }

    private static int link(CommandContext<CommandSourceStack> ctx, String code) {
        CommandSourceStack source = ctx.getSource();

        if (!source.isPlayer()) {
            source.sendFailure(Component.literal("§cThis command can only be used by a player."));
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        MinecraftAccountLinkManager.LinkResult result = MinecraftAccountLinkManager.getInstance()
            .completeLink(code.toUpperCase(), player.getUUID(), player.getName().getString());

        if (result.success) {
            source.sendSuccess(() -> Component.literal("§8[§bNE§8] §r§aYour dashboard account is now linked to this Minecraft account."), false);
            NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/linkaccount succeeded for {}", player.getName().getString());
            return 1;
        } else {
            source.sendFailure(Component.literal("§c" + result.message));
            NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/linkaccount failed for {}: {}", player.getName().getString(), result.message);
            return 0;
        }
    }
}
