package com.zerog.neoessentials.chat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.chat.PlayerChatFormatManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * /chatformat command — per-player chat format management.
 *
 * <pre>
 *   /chatformat set   &lt;player&gt; &lt;format&gt;  — assign a custom format
 *   /chatformat clear &lt;player&gt;           — remove the custom format
 *   /chatformat check &lt;player&gt;           — show the active override
 *   /chatformat list                     — list all overrides
 *   /chatformat reload                   — reload from disk
 * </pre>
 *
 * All subcommands require {@code neoessentials.chat.format.set}.
 */
public class ChatFormatCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatFormatCommand.class);
    private static final String PERM = "neoessentials.chat.format.set";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if chat module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
            LOGGER.debug("Chat module is disabled, skipping chatformat command registration");
            return;
        }

        // Check if the chatformat command is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("chatformat")) {
            LOGGER.debug("chatformat command is disabled, skipping registration");
            return;
        }

        dispatcher.register(Commands.literal("chatformat")
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("format", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            if (!PermissionValidator.validatePermission(source, PERM).hasPermission()) return 0;
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            String format = StringArgumentType.getString(ctx, "format");
                            PlayerChatFormatManager.getInstance().setFormat(target.getUUID(), format);
                            source.sendSuccess(() -> Component.literal(
                                "§aSet custom chat format for §e" + target.getName().getString() + "§a."
                            ), true);
                            LOGGER.info("[ChatFormat] {} set format for {} -> {}",
                                source.getTextName(), target.getName().getString(), format);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("clear")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        if (!PermissionValidator.validatePermission(source, PERM).hasPermission()) return 0;
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        boolean had = PlayerChatFormatManager.getInstance().clearFormat(target.getUUID());
                        if (had) {
                            source.sendSuccess(() -> Component.literal(
                                "§aCleared custom chat format for §e" + target.getName().getString() + "§a."
                            ), true);
                        } else {
                            source.sendFailure(Component.literal(
                                "§e" + target.getName().getString() + " §chad no custom chat format."
                            ));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("check")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        if (!PermissionValidator.validatePermission(source, PERM).hasPermission()) return 0;
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        String fmt = PlayerChatFormatManager.getInstance().getFormat(target.getUUID());
                        if (fmt != null) {
                            source.sendSuccess(() -> Component.literal(
                                "§7Format for §e" + target.getName().getString() + "§7: §f" + fmt
                            ), false);
                        } else {
                            source.sendSuccess(() -> Component.literal(
                                "§e" + target.getName().getString()
                                    + " §7has no custom format (uses group/default)."
                            ), false);
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if (!PermissionValidator.validatePermission(source, PERM).hasPermission()) return 0;
                    PlayerChatFormatManager mgr = PlayerChatFormatManager.getInstance();
                    int count = mgr.getCount();
                    if (count == 0) {
                        source.sendSuccess(() -> Component.literal(
                            "§7No per-player chat format overrides are set."
                        ), false);
                        return 1;
                    }
                    source.sendSuccess(() -> Component.literal(
                        "§7Per-player chat format overrides (§e" + count + "§7):"
                    ), false);
                    net.minecraft.server.MinecraftServer server = source.getServer();
                    if (server != null) {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            String pFmt = mgr.getFormat(p.getUUID());
                            if (pFmt != null) {
                                final String pName = p.getName().getString();
                                final String pFmtFinal = pFmt;
                                source.sendSuccess(() -> Component.literal(
                                    "§e  " + pName + "§7: §f" + pFmtFinal
                                ), false);
                            }
                        }
                    }
                    return 1;
                })
            )
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    if (!PermissionValidator.validatePermission(source, PERM).hasPermission()) return 0;
                    PlayerChatFormatManager.getInstance().load();
                    int count = PlayerChatFormatManager.getInstance().getCount();
                    source.sendSuccess(() -> Component.literal(
                        "§aReloaded player chat formats. §e" + count + " §aoverride(s) active."
                    ), true);
                    return 1;
                })
            )
        );
    }
}

