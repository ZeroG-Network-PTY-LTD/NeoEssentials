package com.zerog.neoessentials.chat.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.util.MessageUtil;

/**
 * Handles the /socialspy command for toggling message spying for moderators/admins.
 */
public class SocialSpyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("socialspy")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                
                // Validate sender
                ServerPlayer sender = source.getPlayer();
                if (sender == null) {
                    source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
                    return 0;
                }
                
                ChatManager chatManager = ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.isSocialSpyEnabled()) {
                    sender.sendSystemMessage(MessageUtil.error("commands.neoessentials.socialspy.disabled"));
                    return 0;
                }
                
                // Check if player has socialspy permission
                if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.socialspy")) {
                    // Debug: Check if the key exists
                    MessageUtil.debugKey("commands.neoessentials.socialspy.no_permission");
                    sender.sendSystemMessage(MessageUtil.error("commands.neoessentials.socialspy.no_permission"));
                    return 0;
                }
                
                com.zerog.neoessentials.chat.SocialSpyManager.toggleSocialSpy(sender);
                boolean isNowEnabled = com.zerog.neoessentials.chat.SocialSpyManager.hasSocialSpy(sender);
                
                if (isNowEnabled) {
                    sender.sendSystemMessage(MessageUtil.success("commands.neoessentials.socialspy.enabled"));
                } else {
                    sender.sendSystemMessage(MessageUtil.success("commands.neoessentials.socialspy.disabled_status"));
                }
                return 1;
            })
        );
    }
}
