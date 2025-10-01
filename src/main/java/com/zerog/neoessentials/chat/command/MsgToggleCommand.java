package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.MsgToggleManager;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.chat.ChatManager;

/**
 * Handles the /msgtoggle command for toggling private message reception.
 */
public class MsgToggleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("msgtoggle")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                ServerPlayer sender = source.getPlayer();
                ChatManager chatManager = ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.isMsgToggleEnabled()) {
                    source.sendFailure(Component.translatable("neoessentials.msgtoggle.disabled"));
                    return 0;
                }
                if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.msgtoggle")) {
                    source.sendFailure(Component.translatable("neoessentials.no_permission"));
                    return 0;
                }
                MsgToggleManager.toggleMsg(sender);
                source.sendSuccess(() -> Component.translatable("neoessentials.msgtoggle.toggled"), false);
                return 1;
            })
        );
    }
}
