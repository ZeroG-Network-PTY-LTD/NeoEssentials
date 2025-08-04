package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Give command implementation - /give <player> <item> [amount]
 * Gives items to players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class GiveCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        // /give <player> <item> [amount]
        dispatcher.register(Commands.literal("give")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("item", ItemArgument.item(context))
                    .executes(ctx -> giveItem(ctx, 1))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> giveItem(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
                    )
                )
            )
        );
    }
    
    /**
     * Give an item to a player
     */
    private static int giveItem(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ItemInput itemInput = ItemArgument.getItem(context, "item");
        
        // Create the item stack
        ItemStack itemStack = itemInput.createItemStack(amount, false);
        
        // Try to add the item to the player's inventory
        boolean success = target.getInventory().add(itemStack);
        
        String itemName = itemStack.getDisplayName().getString();
        
        if (success) {
            // Send success message to executor
            context.getSource().sendSuccess(() -> Component.literal(
                "§aGave " + amount + " " + itemName + " to " + target.getName().getString()), true);
            
            // Send notification to target player
            target.sendSystemMessage(Component.literal(
                "§aYou received " + amount + " " + itemName + " from " + 
                context.getSource().getDisplayName().getString()));
        } else {
            // Player's inventory is full, drop the item
            target.drop(itemStack, false);
            
            // Send message about dropped item
            context.getSource().sendSuccess(() -> Component.literal(
                "§eGave " + amount + " " + itemName + " to " + target.getName().getString() + 
                " (dropped because inventory was full)"), true);
            
            // Notify target player
            target.sendSystemMessage(Component.literal(
                "§eReceived " + amount + " " + itemName + " from " + 
                context.getSource().getDisplayName().getString() + " (dropped because inventory was full)"));
        }
        
        return 1;
    }
}
