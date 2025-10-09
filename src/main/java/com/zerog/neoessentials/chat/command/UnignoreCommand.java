
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.util.MessageUtil;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles the /unignore command for removing a player from the ignore list.
 */
public class UnignoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unignore")
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer targetPlayer;
                    try {
                        targetPlayer = EntityArgument.getPlayer(ctx, "target");
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unignore.player_not_found"));
                        return 0;
                    }
                    String targetName = targetPlayer.getName().getString();
                    
                    // Validate sender
                    ServerPlayer sender = source.getPlayer();
                    if (sender == null) {
                        source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
                        return 0;
                    }
                    
                    // Check permissions
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isUnignoreEnabled()) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unignore.disabled"));
                        return 0;
                    }
                    
                    // Proper permission validation using PermissionAPI
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.ignore")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unignore.no_permission"));
                        return 0;
                    }
                    
                    // Check if player is actually ignoring the target
                    if (!com.zerog.neoessentials.chat.IgnoreManager.isIgnoring(sender, targetName)) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unignore.not_ignored", targetName));
                        return 0;
                    }
                    
                    com.zerog.neoessentials.chat.IgnoreManager.unignore(sender, targetName);
                    source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.unignore.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
