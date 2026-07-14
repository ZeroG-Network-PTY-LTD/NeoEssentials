
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import com.zerog.neoessentials.util.MessageUtil;

/**
 * Handles the /unmute command for unmuting a player.
 */
public class UnmuteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if chat module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
            return;
        }

        // Check if the unmute command is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("unmute")) {
            return;
        }

        dispatcher.register(Commands.literal("unmute")
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer targetPlayer;
                    try {
                        targetPlayer = EntityArgument.getPlayer(ctx, "target");
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unmute.player_not_found"));
                        return 0;
                    }
                    String targetName = targetPlayer.getName().getString();
                    
                    // Validate sender
                    ServerPlayer sender = source.getPlayer();
                    if (sender == null) {
                        source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
                        return 0;
                    }
                    
                    // Check if chat module is enabled
                    if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unmute.disabled"));
                        return 0;
                    }
                    
                    // Check if individual unmute command is enabled
                    if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("unmute")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unmute.disabled"));
                        return 0;
                    }
                    
                    // Legacy check for backwards compatibility
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isUnmuteEnabled()) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unmute.disabled"));
                        return 0;
                    }
                    
                    // Check permissions
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.mute")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    
                    // Check if player is actually muted
                    if (!com.zerog.neoessentials.chat.MuteManager.getMutedPlayers().contains(targetName.toLowerCase())) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.unmute.not_muted", targetName));
                        return 0;
                    }
                    
                    com.zerog.neoessentials.chat.MuteManager.unmute(targetName, sender.getName().getString());
                    // Notify Discord integrations
                    try {
                        com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastMuteEvent(targetPlayer, null, false);
                    } catch (Exception ignored) {}
                    source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.unmute.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
