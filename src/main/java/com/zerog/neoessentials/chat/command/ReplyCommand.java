package com.zerog.neoessentials.chat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Handles the /reply command for replying to the last private message sender.
 */
public class ReplyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with vanilla aliases to override vanilla behavior
        registerCommand(dispatcher, "reply");
        registerCommand(dispatcher, "r");
    }
    
    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    String message = StringArgumentType.getString(ctx, "message");
                    
                    // Validate sender
                    ServerPlayer sender = source.getPlayer();
                    if (sender == null) {
                        source.sendFailure(Component.translatable("neoessentials.error.no_server"));
                        return 0;
                    }
                    
                    // Find target from last message history
                    ServerPlayer target = com.zerog.neoessentials.chat.LastMessageManager.getLastMessager(sender);
                    if (target == null) {
                        source.sendFailure(Component.translatable("commands.neoessentials.reply.no_target"));
                        return 0;
                    }
                    
                    // Check if target is still online
                    if (!target.getServer().getPlayerList().getPlayers().contains(target)) {
                        source.sendFailure(Component.translatable("commands.neoessentials.reply.target_offline"));
                        return 0;
                    }
                    
                    // Permissions and mute/ignore checks
                    com.zerog.neoessentials.chat.ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isReplyEnabled()) {
                        source.sendFailure(Component.translatable("commands.neoessentials.reply.disabled"));
                        return 0;
                    }
                    
                    // Proper permission validation using PermissionAPI
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.reply")) {
                        source.sendFailure(Component.translatable("commands.neoessentials.reply.no_permission"));
                        return 0;
                    }
                    
                    if (com.zerog.neoessentials.api.ChatAPI.isMutedOrIgnored(sender, target)) {
                        source.sendFailure(Component.translatable("commands.neoessentials.reply.muted_or_ignored", target.getName()));
                        return 0;
                    }
                    
                    // Send reply messages using vanilla formatting
                    Component msgToTarget = Component.translatable("commands.message.display.incoming", sender.getDisplayName(), message);
                    Component msgToSender = Component.translatable("commands.message.display.outgoing", target.getDisplayName(), message);
                    target.sendSystemMessage(msgToTarget);
                    sender.sendSystemMessage(msgToSender);
                    
                    // Update last message sender for both players
                    com.zerog.neoessentials.chat.LastMessageManager.setLastMessager(target, sender);
                    com.zerog.neoessentials.chat.LastMessageManager.setLastMessager(sender, target);
                    
                    com.zerog.neoessentials.api.ChatAPI.broadcastSocialSpy(sender, target, message);
                    return 1;
                })
            )
        );
    }
}
