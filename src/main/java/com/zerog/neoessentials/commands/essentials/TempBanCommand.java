package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.localization.LanguageManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.ModerationManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Temporary ban command implementation - /tempban
 * Temporarily bans players for a specified duration
 */
public class TempBanCommand {
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwy])");
    private static final int MAX_SECONDS = 31536000; // 1 year in seconds

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tempban <player> <duration> [reason] - Temporarily ban a player
        dispatcher.register(Commands.literal("tempban")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("duration", StringArgumentType.word())
                    .executes(context -> tempBanPlayer(context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "duration"),
                        "Temporarily banned by an operator"))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> tempBanPlayer(context,
                            EntityArgument.getPlayer(context, "player"),
                            StringArgumentType.getString(context, "duration"),
                            StringArgumentType.getString(context, "reason")))
                    )
                )
            )
        );
    }

    private static int tempBanPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, String durationStr, String reason) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        int seconds = parseDuration(durationStr);
        if (seconds < 1) {
            String msg = LanguageManager.getInstance().getMessage(admin, "neoessentials.tempban.invalid_duration");
            MessageUtil.sendMessage(admin, msg);
            return 0;
        }
        if (seconds > MAX_SECONDS) {
            String msg = LanguageManager.getInstance().getMessage(admin, "neoessentials.tempban.too_long");
            MessageUtil.sendMessage(admin, msg);
            return 0;
        }
        if (target.getUUID().equals(admin.getUUID())) {
            String msg = LanguageManager.getInstance().getMessage(admin, "neoessentials.tempban.cannot_self");
            MessageUtil.sendMessage(admin, msg);
            return 0;
        }
        boolean success = moderationManager.tempBanPlayer(target, admin, reason, seconds);
        return success ? 1 : 0;
    }

    private static int parseDuration(String input) {
        Matcher matcher = DURATION_PATTERN.matcher(input);
        int totalSeconds = 0;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2)) {
                case "s": totalSeconds += value; break;
                case "m": totalSeconds += value * 60; break;
                case "h": totalSeconds += value * 3600; break;
                case "d": totalSeconds += value * 86400; break;
                case "w": totalSeconds += value * 604800; break;
                case "y": totalSeconds += value * 31536000; break;
            }
        }
        return totalSeconds;
    }
}
