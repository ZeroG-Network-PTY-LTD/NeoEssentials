
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Handles the /mutelist command for listing all muted players.
 */
public class MuteListCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mutelist")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                // ServerPlayer sender = source.getPlayer(); // Unused variable removed
                // Implement actual mute list retrieval
                ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.mutelist")) {
                    source.sendFailure(net.minecraft.network.chat.Component.translatable("neoessentials.no_permission"));
                    return 0;
                }
                java.util.List<String> muted = new java.util.ArrayList<>(com.zerog.neoessentials.chat.MuteManager.getMutedPlayers());
                String mutedList = muted.isEmpty() ? "<none>" : String.join(", ", muted);
                source.sendSuccess(() -> Component.translatable("neoessentials.mutelist.list", mutedList), false);
                return 1;
            })
        );
    }
}
