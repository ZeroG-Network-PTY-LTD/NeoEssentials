package com.zerog.neoessentials.chat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.chat.ChatManager;
import net.minecraft.server.MinecraftServer;

/**
 * Handles the /msg command for private messaging between players.
 */
public class MsgCommand {

    /**
     * Registers the /msg command with the dispatcher.
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("msg")
            .then(Commands.argument("target", StringArgumentType.word())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        String targetName = StringArgumentType.getString(ctx, "target");
                        String message = StringArgumentType.getString(ctx, "message");
                        ServerPlayer sender = source.getPlayer();
                        MinecraftServer server = sender.getServer();
                        if (server == null) {
                            source.sendFailure(Component.translatable("neoessentials.error.no_server"));
                            return 0;
                        }
                        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
                        if (target == null) {
                            source.sendFailure(Component.translatable("commands.neoessentials.msg.not_found", targetName));
                            return 0;
                        }
                        ChatManager chatManager = ChatAPI.getChatManager();
                        if (chatManager != null && !chatManager.isMsgEnabled()) {
                            source.sendFailure(Component.translatable("commands.neoessentials.msg.disabled"));
                            return 0;
                        }
                        if (chatManager != null && !chatManager.hasChatPermission("neoessentials.msg")) {
                            source.sendFailure(Component.translatable("commands.neoessentials.msg.no_permission"));
                            return 0;
                        }
                        // --- Mute/ignore check ---
                        if (ChatAPI.isMutedOrIgnored(sender, target)) {
                            source.sendFailure(Component.translatable("commands.neoessentials.msg.muted_or_ignored", target.getName()));
                            return 0;
                        }
                        // Send message
                        Component msgToTarget = Component.translatable("commands.neoessentials.msg.format.to", sender.getName(), message);
                        Component msgToSender = Component.translatable("commands.neoessentials.msg.format.from", target.getName(), message);
                        target.sendSystemMessage(msgToTarget);
                        sender.sendSystemMessage(msgToSender);
                        // --- SocialSpy integration ---
                        ChatAPI.broadcastSocialSpy(sender, target, message);
                        // Advanced formatting, plugin hooks can be added here if needed
                        return 1;
                    })
                )
            )
        );
    }
}
