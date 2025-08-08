package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/**
 * Item command implementation - /item, /i
 * Allows spawning items for players
 */
public class ItemCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        // /item <item> [amount] [player] - Give item to player
        dispatcher.register(Commands.literal("item")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.GIVE_ITEM))
            .then(Commands.argument("item", ItemArgument.item(context))
                .executes(ctx -> giveItem(ctx, 1, null))
                .then(Commands.argument("amount", StringArgumentType.word())
                    .executes(ctx -> giveItem(ctx, parseAmount(ctx, "amount"), null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> giveItem(ctx, parseAmount(ctx, "amount"), EntityArgument.getPlayer(ctx, "player")))
                    )
                )
            )
        );
        
        // /i - Alias for /item
        dispatcher.register(Commands.literal("i")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.GIVE_ITEM))
            .then(Commands.argument("item", ItemArgument.item(context))
                .executes(ctx -> giveItem(ctx, 1, null))
                .then(Commands.argument("amount", StringArgumentType.word())
                    .executes(ctx -> giveItem(ctx, parseAmount(ctx, "amount"), null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> giveItem(ctx, parseAmount(ctx, "amount"), EntityArgument.getPlayer(ctx, "player")))
                    )
                )
            )
        );
    }
    
    private static int giveItem(CommandContext<CommandSourceStack> context, int amount, ServerPlayer target) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = target != null ? target : source.getPlayerOrException();
        
        try {
            ItemInput itemInput = ItemArgument.getItem(context, "item");
            ItemStack itemStack = itemInput.createItemStack(amount, false);
            
            // Add item to player inventory
            if (player.getInventory().add(itemStack)) {
                // Success - item added
                String itemName = itemStack.getDisplayName().getString();
                String message = target != null ?
                    MessageUtil.replacePlaceholders("&aGave {0} x{1} to {2}", itemName, amount, target.getName().getString()) :
                    MessageUtil.replacePlaceholders("&aGave {0} x{1}", itemName, amount);
                
                source.sendSuccess(() -> Component.literal(MessageUtil.translateColorCodes(message)), false);
                
                if (target != null && target != source.getEntity()) {
                    MessageUtil.sendMessage(target, 
                        MessageUtil.replacePlaceholders("&aReceived {0} x{1} from {2}", 
                            itemName, amount, source.getTextName()));
                }
                
                return 1;
            } else {
                // Inventory full
                source.sendFailure(Component.literal(
                    MessageUtil.translateColorCodes("&cPlayer's inventory is full!")
                ));
                return 0;
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                MessageUtil.translateColorCodes("&cInvalid item or amount!")
            ));
            return 0;
        }
    }
    
    private static int parseAmount(CommandContext<CommandSourceStack> context, String argumentName) {
        try {
            String amountStr = StringArgumentType.getString(context, argumentName);
            int amount = Integer.parseInt(amountStr);
            return Math.max(1, Math.min(amount, 64)); // Clamp between 1 and 64
        } catch (NumberFormatException e) {
            return 1; // Default to 1 if parsing fails
        }
    }
}
