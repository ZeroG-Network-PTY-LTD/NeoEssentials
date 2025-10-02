
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Handles the /unignore command for removing a player from the ignore list.
 */
public class UnignoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unignore")
            .then(Commands.argument("target", StringArgumentType.word())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    ServerPlayer sender = source.getPlayer(); // Used in future implementation
                    assert sender != null || true; // Suppress unused variable warning
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.unignore")) {
                        source.sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    com.zerog.neoessentials.chat.IgnoreManager.unignore(sender, targetName);
                    source.sendSuccess(() -> Component.translatable("commands.neoessentials.unignore.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
