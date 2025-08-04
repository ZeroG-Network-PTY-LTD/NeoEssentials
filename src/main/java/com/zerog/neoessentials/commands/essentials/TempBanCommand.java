package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.ModerationManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * Temporary ban command implementation - /tempban
 * Temporarily bans players for a specified duration
 */
public class TempBanCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tempban <player> <duration> [reason] - Temporarily ban a player
        dispatcher.register(Commands.literal("tempban")
            .requires(source -> source.hasPermission(3))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                    .executes(context -> tempBanPlayer(context,
                        EntityArgument.getPlayer(context, "player"),
                        IntegerArgumentType.getInteger(context, "duration"),
                        "Temporarily banned by an operator"))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> tempBanPlayer(context,
                            EntityArgument.getPlayer(context, "player"),
                            IntegerArgumentType.getInteger(context, "duration"),
                            StringArgumentType.getString(context, "reason")))
                    )
                )
            )
        );
    }
    
    private static int tempBanPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, int durationMinutes, String reason) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        // Validate duration
        if (durationMinutes < 1) {
            MessageUtil.sendMessage(admin, "&cDuration must be at least 1 minute!");
            return 0;
        }
        
        if (durationMinutes > 525600) { // 1 year in minutes
            MessageUtil.sendMessage(admin, "&cDuration cannot exceed 1 year!");
            return 0;
        }
        
        // Check if trying to ban themselves
        if (target.getUUID().equals(admin.getUUID())) {
            MessageUtil.sendMessage(admin, "&cYou cannot ban yourself!");
            return 0;
        }
        
        boolean success = moderationManager.tempBanPlayer(target, admin, reason, durationMinutes);
        return success ? 1 : 0;
    }
}
