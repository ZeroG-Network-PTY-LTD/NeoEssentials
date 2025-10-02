package com.zerog.neoessentials.test;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("neotest")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("NeoEssentials Test Command Working!"), false);
                    return 1;
                })
        );
        
        dispatcher.register(
            Commands.literal("baltest")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Balance Test: 100 coins"), false);
                    return 1;
                })
        );
    }
}