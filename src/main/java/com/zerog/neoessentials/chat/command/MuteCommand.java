
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.zerog.neoessentials.util.MessageUtil;

/**
 * Handles the /mute command for muting a player.
 */
public class MuteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mute")
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> executeMute(ctx, ""))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeMute(ctx, StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
    }
    
    private static int executeMute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String reason) {
        CommandSourceStack source = ctx.getSource();
        net.minecraft.server.level.ServerPlayer targetPlayer;
        try {
            targetPlayer = EntityArgument.getPlayer(ctx, "target");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.player_not_found"));
            return 0;
        }
        String targetName = targetPlayer.getName().getString();
        
        // Validate sender
        net.minecraft.server.level.ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
            return 0;
        }
        
        // Check if command is enabled
        ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
        if (chatManager != null && !chatManager.isMuteEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.disabled"));
            return 0;
        }
        
        // Check permissions
        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.mute")) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
        // Check if trying to mute self
        if (sender.getName().getString().equalsIgnoreCase(targetName)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.self"));
            return 0;
        }
        
        // Check if target has exempt permission
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(targetPlayer.getUUID(), "neoessentials.chat.mute.exempt")) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.exempt", targetName));
            return 0;
        }
        
        com.zerog.neoessentials.chat.MuteManager.mute(sender, targetName);
        if (reason.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.mute.success", targetName), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.mute.success_with_reason", targetName, reason), false);
        }
        return 1;
    }
}
