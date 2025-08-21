package com.zerog.neoessentials.commands.essentials;
import java.util.UUID;
import java.util.Optional;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;

/**
 * Seen command implementation for NeoEssentials
 * Shows when a player was last seen online
 * 
 * Commands:
 * - /seen <player> - Check when a player was last online
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SeenCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seen")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ESSENTIALS_USE))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((context, builder) -> {
                    // Suggest all players (online and offline from user cache)
                    return SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerList().getPlayers()
                            .stream().map(p -> p.getName().getString()), 
                        builder
                    );
                })
                .executes(SeenCommand::checkPlayerSeen)));
    }
    
    /**
     * Execute /seen <player> command to check when player was last online
     */
    private static int checkPlayerSeen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "player");
        var source = context.getSource();
        var server = source.getServer();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);

        if (onlinePlayer != null) {
            source.sendSuccess(() -> Component.literal("§a" + playerName + " is currently online!"), false);
            source.sendSuccess(() -> Component.literal("§7Location: §b" + onlinePlayer.level().dimension().location()), false);
            source.sendSuccess(() -> Component.literal("§7Game Mode: §6" + onlinePlayer.gameMode.getGameModeForPlayer().getName()), false);
            return 1;
        }

        // Try to resolve UUID from name
        UUID targetUuid = null;
        var userCache = server.getProfileCache();
        if (userCache != null) {
            var gameProfile = userCache.get(playerName);
            if (gameProfile.isPresent()) {
                targetUuid = gameProfile.get().getId();
            }
        }
        if (targetUuid == null) {
            // Try to parse as UUID
            try {
                targetUuid = UUID.fromString(playerName);
            } catch (IllegalArgumentException ignored) {}
        }
        if (targetUuid == null) {
            source.sendFailure(Component.literal("§cPlayer or UUID not found."));
            return 0;
        }

        // Use LastSeenManager for last seen lookup
        var mgr = com.zerog.neoessentials.managers.LastSeenManager.getInstance();
        Optional<Long> lastSeenOpt = mgr.getLastSeen(targetUuid);
        if (lastSeenOpt.isPresent()) {
            java.time.Instant instant = java.time.Instant.ofEpochMilli(lastSeenOpt.get());
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM).withZone(java.time.ZoneId.systemDefault());
            String time = fmt.format(instant);
            source.sendSuccess(() -> Component.literal("§e" + playerName + " was last seen: §a" + time), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cNo data for " + playerName));
            return 0;
        }
    }
    
    /**
     * Format duration in a human-readable way
     */
}
