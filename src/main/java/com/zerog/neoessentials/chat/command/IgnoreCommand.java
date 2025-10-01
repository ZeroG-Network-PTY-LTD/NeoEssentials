
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Handles the /ignore command for ignoring messages from a player.
 */
public class IgnoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ignore")
            .then(Commands.argument("target", StringArgumentType.word())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.ignore")) {
                        source.sendFailure(net.minecraft.network.chat.Component.translatable("neoessentials.no_permission"));
                        return 0;
                    }
                    com.zerog.neoessentials.chat.IgnoreManager.ignore(source.getPlayer(), targetName);
                    source.sendSuccess(() -> Component.translatable("neoessentials.ignore.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
