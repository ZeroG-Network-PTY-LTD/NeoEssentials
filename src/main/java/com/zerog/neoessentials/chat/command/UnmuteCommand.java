
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Handles the /unmute command for unmuting a player.
 */
public class UnmuteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unmute")
            .then(Commands.argument("target", StringArgumentType.word())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    ServerPlayer sender = source.getPlayer(); // Used in future implementation
                    assert sender != null || true; // Suppress unused variable warning
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.unmute")) {
                        source.sendFailure(net.minecraft.network.chat.Component.translatable("neoessentials.no_permission"));
                        return 0;
                    }
                    com.zerog.neoessentials.chat.MuteManager.unmute(sender, targetName);
                    source.sendSuccess(() -> Component.translatable("neoessentials.unmute.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
