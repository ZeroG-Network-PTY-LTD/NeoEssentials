package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

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
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

public class AnvilCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("anvil")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ANVIL))
            .executes(AnvilCommand::openAnvil)
        );
    }

    /**
     * Open anvil for command sender
     */
    private static int openAnvil(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MenuProvider anvilProvider = new SimpleMenuProvider(
            (windowId, playerInventory, playerEntity) -> new AnvilMenu(
                windowId,
                playerInventory,
                ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.repair")
        );
        player.openMenu(anvilProvider);
        context.getSource().sendSuccess(() -> Component.literal("Opened anvil"), false);
        return 1;
    }

    /**
     * Open anvil for specified player
     */
    private static int openAnvilForPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        return openAnvilForPlayer(context.getSource(), player);
    }

    /**
     * Core method to open anvil for a player
     */
    private static int openAnvilForPlayer(CommandSourceStack source, ServerPlayer player) {
        try {
            MenuProvider anvilProvider = new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new AnvilMenu(
                    windowId, 
                    playerInventory, 
                    ContainerLevelAccess.create(player.level(), player.blockPosition())
                ),
                Component.translatable("container.repair")
            );
            
            player.openMenu(anvilProvider);
            
            if (source.getEntity() != player) {
                source.sendSuccess(() -> Component.literal("Opened anvil for " + player.getDisplayName().getString()), true);
            } else {
                source.sendSuccess(() -> Component.literal("Opened anvil"), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "command.anvil.failed", e.getMessage())));
            return 0;
        }
    }
}
