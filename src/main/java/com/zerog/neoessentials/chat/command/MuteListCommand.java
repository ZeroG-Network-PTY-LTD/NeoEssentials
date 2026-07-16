
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.util.MessageUtil;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Handles the /mutelist command for listing all muted players.
 */
public class MuteListCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mutelist")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();

                // Sender may be console/RCON — allowed, same as every other moderation command.
                net.minecraft.server.level.ServerPlayer sender = source.getPlayer();

                // Check if command is enabled
                ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.isMuteListEnabled()) {
                    source.sendFailure(MessageUtil.error("commands.neoessentials.mutelist.disabled"));
                    return 0;
                }

                // Check permissions (console always passes)
                if (sender != null && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.mute")) {
                    source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                    return 0;
                }
                
                java.util.List<String> muted = new java.util.ArrayList<>(com.zerog.neoessentials.chat.MuteManager.getMutedPlayers());
                if (muted.isEmpty()) {
                    source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.mutelist.empty"), false);
                } else {
                    String mutedList = String.join(", ", muted);
                    source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.mutelist.list", mutedList), false);
                }
                return 1;
            })
        );
    }
}
