
package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuteCommand {
    private static final Map<UUID, MuteData> mutedPlayers = new ConcurrentHashMap<>();
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwy])");
    private static final int MAX_SECONDS = 31536000; // 1 year in seconds

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mute")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> mutePlayer(ctx, EntityArgument.getPlayer(ctx, "player"), 0, "Muted by an operator"))
                .then(Commands.argument("duration", StringArgumentType.word())
                    .executes(ctx -> mutePlayerWithDuration(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "duration"), "Muted by an operator"))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> mutePlayerWithDuration(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "duration"), StringArgumentType.getString(ctx, "reason")))
                    )
                )
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> mutePlayer(ctx, EntityArgument.getPlayer(ctx, "player"), 0, StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
        dispatcher.register(Commands.literal("unmute")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> unmutePlayer(ctx, EntityArgument.getPlayer(ctx, "player")))
            )
        );
        dispatcher.register(Commands.literal("mutelist")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(ctx -> listMutedPlayers(ctx))
        );
    }

    private static int mutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, long durationSeconds, String reason) throws CommandSyntaxException {
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.translatable("neoessentials.mute.player_not_found"));
            return 0;
        }
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            if (executor.getUUID().equals(targetPlayer.getUUID())) {
                context.getSource().sendFailure(Component.translatable("neoessentials.mute.cannot_self"));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            // Command executed from console, which is fine
        }
        UUID playerId = targetPlayer.getUUID();
        String playerName = targetPlayer.getName().getString();
        if (mutedPlayers.containsKey(playerId) && !isExpired(mutedPlayers.get(playerId))) {
            context.getSource().sendFailure(Component.translatable("neoessentials.mute.already_muted", playerName));
            return 0;
        }
        long expirationTime = durationSeconds > 0 ? System.currentTimeMillis() + (durationSeconds * 1000) : 0;
        MuteData muteData = new MuteData(
            reason,
            context.getSource().getTextName(),
            System.currentTimeMillis(),
            expirationTime
        );
        mutedPlayers.put(playerId, muteData);
        String durationText = durationSeconds > 0 ? MessageUtil.formatTime(durationSeconds * 1000) : null;
        if (durationText != null) {
            targetPlayer.sendSystemMessage(Component.translatable("neoessentials.mute.player.temp", durationText, reason));
            context.getSource().sendSuccess(() -> Component.translatable("neoessentials.mute.success.temp", playerName, durationText, reason), true);
        } else {
            targetPlayer.sendSystemMessage(Component.translatable("neoessentials.mute.player", reason));
            context.getSource().sendSuccess(() -> Component.translatable("neoessentials.mute.success", playerName, reason), true);
        }
        return 1;
    }

    private static int mutePlayerWithDuration(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, String durationStr, String reason) throws CommandSyntaxException {
        long seconds = parseDurationFlexible(durationStr);
        if (seconds < 0) {
            context.getSource().sendFailure(Component.translatable("neoessentials.mute.invalid_duration"));
            return 0;
        }
        if (seconds > MAX_SECONDS) {
            context.getSource().sendFailure(Component.translatable("neoessentials.mute.too_long"));
            return 0;
        }
        return mutePlayer(context, targetPlayer, seconds, reason);
    }

    private static long parseDurationFlexible(String input) {
        if (input == null || input.isEmpty()) return 0;
        Matcher matcher = DURATION_PATTERN.matcher(input.toLowerCase());
        long totalSeconds = 0;
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
        return totalSeconds > 0 ? totalSeconds : -1;
    }

    private static int unmutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) throws CommandSyntaxException {
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.translatable("neoessentials.mute.player_not_found"));
            return 0;
        }
        UUID playerId = targetPlayer.getUUID();
        String playerName = targetPlayer.getName().getString();
        if (!mutedPlayers.containsKey(playerId) || isExpired(mutedPlayers.get(playerId))) {
            context.getSource().sendFailure(Component.translatable("neoessentials.mute.not_muted", playerName));
            return 0;
        }
        mutedPlayers.remove(playerId);
        targetPlayer.sendSystemMessage(Component.translatable("neoessentials.mute.player.unmuted"));
        context.getSource().sendSuccess(() -> Component.translatable("neoessentials.mute.success.unmuted", playerName), true);
        return 1;
    }

    private static int listMutedPlayers(CommandContext<CommandSourceStack> context) {
        cleanupExpiredMutes();
        if (mutedPlayers.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("neoessentials.mute.list.none"), false);
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.translatable("neoessentials.mute.list.header"), false);
        mutedPlayers.forEach((playerId, muteData) -> {
            ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayer(playerId);
            String playerName = onlinePlayer != null ? onlinePlayer.getName().getString() : "Unknown Player";
            String timeInfo = muteData.expirationTime > 0 ?
                MessageUtil.formatTime(muteData.expirationTime - System.currentTimeMillis()) :
                "Permanent";
            context.getSource().sendSuccess(() -> Component.translatable("neoessentials.mute.list.entry", playerName, timeInfo, muteData.reason), false);
        });
        return 1;
    }

    public static boolean isPlayerMuted(UUID playerId) {
        MuteData muteData = mutedPlayers.get(playerId);
        return muteData != null && !isExpired(muteData);
    }

    private static boolean isExpired(MuteData muteData) {
        return muteData.expirationTime > 0 && System.currentTimeMillis() > muteData.expirationTime;
    }

    public static void cleanupExpiredMutes() {
        mutedPlayers.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private static class MuteData {
        final String reason;
        final long expirationTime; // 0 = permanent

        MuteData(String reason, String mutedBy, long muteTime, long expirationTime) {
            this.reason = reason;
            this.expirationTime = expirationTime;
        }
    }
}
