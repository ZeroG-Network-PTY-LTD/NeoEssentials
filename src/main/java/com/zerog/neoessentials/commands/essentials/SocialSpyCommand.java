package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.managers.SocialSpyManager;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.localization.LanguageManager;

public class SocialSpyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("socialspy")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SOCIALSPY))
                .then(Commands.argument("state", StringArgumentType.word())
                    .suggests((c, b) -> { b.suggest("on"); b.suggest("off"); return b.buildFuture(); })
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        boolean state = StringArgumentType.getString(ctx, "state").equalsIgnoreCase("on");
                        SocialSpyManager.toggle(player, state);
                        String msgKey = state ? "neoessentials.socialspy.enabled" : "neoessentials.socialspy.disabled";
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(LanguageManager.getInstance().getMessage(player, msgKey)));
                        return 1;
                    })
                )
        );
    }
}
