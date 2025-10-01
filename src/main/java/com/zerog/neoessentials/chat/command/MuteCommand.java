
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Handles the /mute command for muting a player.
 */
public class MuteCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mute")
            .then(Commands.argument("target", StringArgumentType.word())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    // ServerPlayer sender = source.getPlayer(); // Unused variable removed
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.mute")) {
                        source.sendFailure(net.minecraft.network.chat.Component.translatable("neoessentials.no_permission"));
                        return 0;
                    }
                    com.zerog.neoessentials.chat.MuteManager.mute(source.getPlayer(), targetName);
                    source.sendSuccess(() -> Component.translatable("neoessentials.mute.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
