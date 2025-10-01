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
        dispatcher.register(Commands.literal("reply")
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    String message = StringArgumentType.getString(ctx, "message");
                    ServerPlayer sender = source.getPlayer();
                    ServerPlayer target = com.zerog.neoessentials.chat.LastMessageManager.getLastMessager(sender);
                    if (target == null) {
                        source.sendFailure(Component.translatable("neoessentials.reply.no_target"));
                        return 0;
                    }
                    // Permissions and mute/ignore checks
                    com.zerog.neoessentials.chat.ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isReplyEnabled()) {
                        source.sendFailure(Component.translatable("neoessentials.reply.disabled"));
                        return 0;
                    }
                    if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.reply")) {
                        source.sendFailure(Component.translatable("neoessentials.no_permission"));
                        return 0;
                    }
                    if (com.zerog.neoessentials.api.ChatAPI.isMutedOrIgnored(sender, target)) {
                        source.sendFailure(Component.translatable("neoessentials.reply.muted_or_ignored", target.getName()));
                        return 0;
                    }
                    // Send reply messages
                    Component msgToTarget = Component.translatable("neoessentials.reply.format.to", sender.getName(), message);
                    Component msgToSender = Component.translatable("neoessentials.reply.format.from", target.getName(), message);
                    target.sendSystemMessage(msgToTarget);
                    sender.sendSystemMessage(msgToSender);
                    com.zerog.neoessentials.api.ChatAPI.broadcastSocialSpy(sender, target, message);
                    // Update last message sender for both players
                    com.zerog.neoessentials.chat.LastMessageManager.setLastMessager(target, sender);
                    return 1;
                })
            )
        );
    }
}
