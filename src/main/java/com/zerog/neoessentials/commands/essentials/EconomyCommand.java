package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

public class EconomyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("economy")
                .then(Commands.literal("balance")
                    .executes(ctx -> EconomyManager.getInstance().showBalance(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(src -> PermissionUtil.hasPermissionOrOp(src, PermissionNodes.ECO_BALANCE))
                        .executes(ctx -> EconomyManager.getInstance().showBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                    )
                )
                .then(Commands.literal("pay")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> EconomyManager.getInstance().pay(ctx.getSource(),
                                ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "target"),
                                DoubleArgumentType.getDouble(ctx, "amount")))
                        )
                    )
                )
                .then(Commands.literal("give")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .requires(src -> PermissionUtil.hasPermissionOrOp(src, PermissionNodes.ECO_GIVE))
                            .executes(ctx -> EconomyManager.getInstance().give(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "target"),
                                DoubleArgumentType.getDouble(ctx, "amount")))
                        )
                    )
                )
                .then(Commands.literal("take")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .requires(src -> PermissionUtil.hasPermissionOrOp(src, PermissionNodes.ECO_TAKE))
                            .executes(ctx -> EconomyManager.getInstance().take(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "target"),
                                DoubleArgumentType.getDouble(ctx, "amount")))
                        )
                    )
                )
        );
    }
}
