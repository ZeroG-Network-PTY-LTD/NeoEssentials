package com.zerog.neoessentials.chat.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.chat.ChatManager;

/**
 * Handles the /socialspy command for toggling message spying for moderators/admins.
 */
public class SocialSpyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("socialspy")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                // ServerPlayer sender = source.getPlayer(); // Unused variable removed
                ChatManager chatManager = ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.isSocialSpyEnabled()) {
                    source.sendFailure(Component.translatable("commands.neoessentials.socialspy.disabled"));
                    return 0;
                }
                if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.socialspy")) {
                    source.sendFailure(Component.translatable("commands.neoessentials.no_permission"));
                    return 0;
                }
                com.zerog.neoessentials.chat.SocialSpyManager.toggleSocialSpy(source.getPlayer());
                source.sendSuccess(() -> Component.translatable("commands.neoessentials.socialspy.toggled"), false);
                return 1;
            })
        );
    }
}
