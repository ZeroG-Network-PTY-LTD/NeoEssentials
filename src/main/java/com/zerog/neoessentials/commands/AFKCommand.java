package com.zerog.neoessentials.commands;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.afk.AFKManager;

public class AFKCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
            .executes(ctx -> setAFK(ctx, true))
            .then(Commands.argument("state", BoolArgumentType.bool())
                .executes(ctx -> setAFK(ctx, BoolArgumentType.getBool(ctx, "state"))))
        );
    }

    private static int setAFK(CommandContext<CommandSourceStack> ctx, boolean state) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("You must be a player to use this command."));
            return 0;
        }
        AFKManager.getInstance().setAFK(player, state);
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
            state ? "You are now AFK." : "You are no longer AFK."), false);
        return Command.SINGLE_SUCCESS;
    }
}
