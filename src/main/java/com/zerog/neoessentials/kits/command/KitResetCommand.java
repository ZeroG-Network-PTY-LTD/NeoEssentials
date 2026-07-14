package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.kits.KitManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * /kitreset <kit> [player]
 *
 * Port of EssentialsX Commandkitreset:
 *  - /kitreset <kit>             → reset own kit cooldown
 *  - /kitreset <kit> <player>    → reset another player's cooldown (neoessentials.kitreset.others)
 *  - Console support
 */
public class KitResetCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitResetCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.isKitSystemEnabled()) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("kitreset")) return;

        dispatcher.register(Commands.literal("kitreset")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null
                    || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.kitreset");
            })
            // /kitreset <kitname>
            .then(Commands.argument("kitname", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    KitManager.getInstance().getKitNames(), builder))
                // /kitreset <kitname>  (self)
                .executes(ctx -> executeReset(ctx,
                    StringArgumentType.getString(ctx, "kitname"), null))
                // /kitreset <kitname> <player>  (others)
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerNames(), builder))
                    .requires(src -> {
                        var p = src.getPlayer();
                        return p == null
                            || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.kitreset.others");
                    })
                    .executes(ctx -> executeReset(ctx,
                        StringArgumentType.getString(ctx, "kitname"),
                        StringArgumentType.getString(ctx, "target")))
                )
            )
        );
    }

    private static int executeReset(CommandContext<CommandSourceStack> ctx,
                                    String kitName, String targetName) {
        var source = ctx.getSource();
        var sender = source.getPlayer();

        // Verify kit exists
        if (KitManager.getInstance().getKit(kitName) == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.kits.not_found", kitName));
            return 0;
        }

        // Resolve target
        ServerPlayer target;
        if (targetName != null) {
            target = source.getServer().getPlayerList().getPlayerByName(targetName);
            if (target == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
                return 0;
            }
        } else {
            if (sender == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.kits.console_needs_target"));
                return 0;
            }
            target = sender;
        }

        // Reset cooldown (Essentials: target.setKitTimestamp(kitName, 0)) AND use count —
        // previously only the cooldown was cleared, so a player who'd hit a kit's maxUses cap
        // stayed permanently blocked afterward with no command able to clear it. "/kitreset"
        // conceptually means "let them use it again", which requires clearing both blockers.
        KitManager.getInstance().resetCooldown(target.getUUID(), kitName);
        KitManager.getInstance().resetUsage(target.getUUID(), kitName);

        final String tName = target.getName().getString();
        if (sender != null && target.getUUID().equals(sender.getUUID())) {
            // Self reset
            source.sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.kits.reset_self", kitName), false);
        } else {
            // Other reset (Essentials: kitResetOther)
            source.sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.kits.reset_other", kitName, tName), true);
            target.sendSystemMessage(MessageUtil.info(
                "commands.neoessentials.kits.reset_notify", kitName));
        }

        LOGGER.info("{} reset kit cooldown '{}' for {}",
            sender != null ? sender.getName().getString() : "Console",
            kitName, tName);
        return 1;
    }
}

