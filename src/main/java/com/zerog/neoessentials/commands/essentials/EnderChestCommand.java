package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;

public class EnderChestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enderchest")
            .requires(source -> source.hasPermission(2))
            .executes(EnderChestCommand::openEnderChest)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(EnderChestCommand::openEnderChestForPlayer)
            )
        );
        
        // Alternative commands
        dispatcher.register(Commands.literal("ec")
            .requires(source -> source.hasPermission(2))
            .executes(EnderChestCommand::openEnderChest)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(EnderChestCommand::openEnderChestForPlayer)
            )
        );
        
        dispatcher.register(Commands.literal("echest")
            .requires(source -> source.hasPermission(2))
            .executes(EnderChestCommand::openEnderChest)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(EnderChestCommand::openEnderChestForPlayer)
            )
        );
    }

    /**
     * Open ender chest for command sender
     */
    private static int openEnderChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return openEnderChestForPlayer(context.getSource(), player, player);
    }

    /**
     * Open ender chest for specified player
     */
    private static int openEnderChestForPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer opener = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return openEnderChestForPlayer(context.getSource(), opener, target);
    }

    /**
     * Core method to open ender chest for a player
     */
    private static int openEnderChestForPlayer(CommandSourceStack source, ServerPlayer opener, ServerPlayer target) {
        try {
            // Check permission for opening other players' ender chests
            if (opener != target && !source.hasPermission(3)) {
                source.sendFailure(Component.literal("You don't have permission to open other players' ender chests"));
                return 0;
            }
            
            MenuProvider enderChestProvider = new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> ChestMenu.threeRows(
                    windowId, 
                    playerInventory, 
                    target.getEnderChestInventory()
                ),
                Component.translatable("container.enderchest")
            );
            
            opener.openMenu(enderChestProvider);
            
            if (opener != target) {
                source.sendSuccess(() -> Component.literal("Opened " + target.getDisplayName().getString() + "'s ender chest"), true);
            } else {
                source.sendSuccess(() -> Component.literal("Opened ender chest"), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to open ender chest: " + e.getMessage()));
            return 0;
        }
    }
}
