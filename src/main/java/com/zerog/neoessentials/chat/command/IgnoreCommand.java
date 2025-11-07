
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.util.MessageUtil;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles the /ignore command for ignoring messages from a player.
 */
public class IgnoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register main command
        registerIgnoreCommand(dispatcher, "ignore");
        // Register alias
        registerIgnoreCommand(dispatcher, "block");
    }
    
    private static void registerIgnoreCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer targetPlayer;
                    try {
                        targetPlayer = EntityArgument.getPlayer(ctx, "target");
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.ignore.player_not_found"));
                        return 0;
                    }
                    String targetName = targetPlayer.getName().getString();
                    
                    // Validate sender
                    ServerPlayer sender = source.getPlayer();
                    if (sender == null) {
                        source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
                        return 0;
                    }
                    
                    // Check if trying to ignore self
                    if (sender.getName().getString().equalsIgnoreCase(targetName)) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.ignore.self"));
                        return 0;
                    }
                    
                    // Check permissions
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    // Check if chat module is enabled
                    if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.ignore.disabled"));
                        return 0;
                    }
                    
                    // Check if individual ignore command is enabled
                    if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("ignore")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.ignore.disabled"));
                        return 0;
                    }
                    
                    // Legacy check for backwards compatibility
                    if (chatManager != null && !chatManager.isIgnoreEnabled()) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.ignore.disabled"));
                        return 0;
                    }
                    
                    // Proper permission validation using PermissionAPI
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.ignore")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.ignore.no_permission"));
                        return 0;
                    }
                    
                    // Check if player is already ignoring target
                    if (com.zerog.neoessentials.chat.IgnoreManager.isIgnoring(sender, targetName)) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.ignore.already_ignored", targetName));
                        return 0;
                    }
                    
                    // Check if target has exempt permission
                    if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(targetPlayer.getUUID(), "neoessentials.chat.ignore.exempt")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.ignore.exempt", targetName));
                        return 0;
                    }
                    
                    com.zerog.neoessentials.chat.IgnoreManager.ignore(sender, targetName);
                    source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ignore.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
