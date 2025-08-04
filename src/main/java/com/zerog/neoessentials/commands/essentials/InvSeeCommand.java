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
import net.minecraft.world.inventory.MenuType;

public class InvSeeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invsee")
            .requires(source -> source.hasPermission(3))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(InvSeeCommand::openPlayerInventory)
            )
        );
        
        // Alternative commands
        dispatcher.register(Commands.literal("openinv")
            .requires(source -> source.hasPermission(3))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(InvSeeCommand::openPlayerInventory)
            )
        );
        
        dispatcher.register(Commands.literal("oi")
            .requires(source -> source.hasPermission(3))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(InvSeeCommand::openPlayerInventory)
            )
        );
    }

    /**
     * Open specified player's inventory
     */
    private static int openPlayerInventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer opener = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return openInventoryForPlayer(context.getSource(), opener, target);
    }

    /**
     * Core method to open a player's inventory for another player
     */
    private static int openInventoryForPlayer(CommandSourceStack source, ServerPlayer opener, ServerPlayer target) {
        try {
            // Don't allow opening own inventory
            if (opener == target) {
                source.sendFailure(Component.literal("You cannot open your own inventory with this command"));
                return 0;
            }
            
            MenuProvider inventoryProvider = new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new ChestMenu(
                    MenuType.GENERIC_9x6, 
                    windowId, 
                    playerInventory, 
                    target.getInventory(), 
                    6
                ),
                Component.literal(target.getDisplayName().getString() + "'s Inventory")
            );
            
            opener.openMenu(inventoryProvider);
            
            source.sendSuccess(() -> Component.literal("Opened " + target.getDisplayName().getString() + "'s inventory"), true);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to open inventory: " + e.getMessage()));
            return 0;
        }
    }
}
