package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.zerog.neoessentials.utils.TextUtil;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Implements commands related to items management like /item (give), /i, /repair, etc.
 */
public class ItemCommands {
    private final CommandBuildContext buildContext;
    private final SimpleCommandExceptionType ITEM_NOT_FOUND = new SimpleCommandExceptionType(
            Component.literal("Item not found. Use the correct item ID or name."));

    public ItemCommands(CommandBuildContext buildContext) {
        this.buildContext = buildContext;
    }

    /**
     * Registers all item-related commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerItemCommand(dispatcher);
        registerRepairCommand(dispatcher);
    }

    /**
     * Registers the item command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerItemCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /item <item> [amount]
        LiteralArgumentBuilder<CommandSourceStack> itemCommand = Commands.literal("item")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.item"))                .then(Commands.argument("item", ItemArgument.item(buildContext))
                        .executes(context -> executeItem(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(context -> executeItem(context, IntegerArgumentType.getInteger(context, "amount")))));

        // /i <item> [amount] - Alias for /item
        LiteralArgumentBuilder<CommandSourceStack> iCommand = Commands.literal("i")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.item"))
                .then(Commands.argument("item", ItemArgument.item(buildContext))
                        .executes(context -> executeItem(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(context -> executeItem(context, IntegerArgumentType.getInteger(context, "amount")))));

        // String version for more flexibility with items that might not be registered properly
        LiteralArgumentBuilder<CommandSourceStack> itemStringCommand = Commands.literal("item")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.item"))
                .then(Commands.argument("itemname", StringArgumentType.string())
                        .executes(context -> executeItemByName(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(context -> executeItemByName(context, 
                                        IntegerArgumentType.getInteger(context, "amount")))));

        LiteralArgumentBuilder<CommandSourceStack> iStringCommand = Commands.literal("i")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.item"))
                .then(Commands.argument("itemname", StringArgumentType.string())
                        .executes(context -> executeItemByName(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(context -> executeItemByName(context, 
                                        IntegerArgumentType.getInteger(context, "amount")))));

        dispatcher.register(itemCommand);
        dispatcher.register(iCommand);
        dispatcher.register(itemStringCommand);
        dispatcher.register(iStringCommand);
    }

    /**
     * Registers the repair command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerRepairCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /repair [hand|all]
        LiteralArgumentBuilder<CommandSourceStack> repairCommand = Commands.literal("repair")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.repair"))
                .executes(this::executeRepairHand)
                .then(Commands.literal("hand")
                        .executes(this::executeRepairHand))
                .then(Commands.literal("all")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.repair.all"))
                        .executes(this::executeRepairAll));

        // Register aliases
        dispatcher.register(repairCommand);
        dispatcher.register(Commands.literal("fix")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.repair"))
                .executes(this::executeRepairHand)
                .then(Commands.literal("hand")
                        .executes(this::executeRepairHand))
                .then(Commands.literal("all")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.repair.all"))
                        .executes(this::executeRepairAll)));
    }

    /**
     * Executes the item command using ItemArgument.
     *
     * @param context The command context
     * @param amount  The amount of items to give
     * @return 1 if successful, 0 otherwise
     */
    private int executeItem(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemInput itemInput = ItemArgument.getItem(context, "item");
        ItemStack stack = itemInput.createItemStack(amount, false);

        boolean itemAdded = player.getInventory().add(stack);
        
        if (itemAdded) {
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aGave &6" + amount + "x &a" + stack.getHoverName().getString() + " &ato " + player.getName().getString())), true);
            return 1;
        } else {
            player.drop(stack, false);
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aInventory full. Dropping &6" + amount + "x &a" + stack.getHoverName().getString())), true);
            return 1;
        }
    }

    /**
     * Executes the item command using a string item name.
     *
     * @param context The command context
     * @param amount  The amount of items to give
     * @return 1 if successful, 0 otherwise
     */    private int executeItemByName(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String itemName = StringArgumentType.getString(context, "itemname");
        
        // Try to parse as a ResourceLocation first
        Item item = null;
        
        try {
            if (itemName.contains(":")) {
                // Try as a fully qualified name (minecraft:stone)
                ResourceLocation resourceLocation = ResourceLocation.parse(itemName);
                item = BuiltInRegistries.ITEM.get(resourceLocation);
            } else {
                // Try as a vanilla name (stone)
                ResourceLocation resourceLocation = ResourceLocation.parse("minecraft:" + itemName);
                item = BuiltInRegistries.ITEM.get(resourceLocation);
                
                // If that doesn't work, search for partial matches
                if (item == Items.AIR) {
                    Optional<Item> foundItem = BuiltInRegistries.ITEM.keySet().stream()
                            .filter(rl -> rl.getPath().contains(itemName.toLowerCase()))
                            .map(rl -> BuiltInRegistries.ITEM.get(rl))
                            .findFirst();
                    
                    if (foundItem.isPresent()) {
                        item = foundItem.get();
                    }
                }
            }
        } catch (Exception e) {
            // Invalid ResourceLocation format, treat as partial name
            Optional<Item> foundItem = BuiltInRegistries.ITEM.keySet().stream()
                    .filter(rl -> rl.getPath().contains(itemName.toLowerCase()))
                    .map(rl -> BuiltInRegistries.ITEM.get(rl))
                    .findFirst();
            
            if (foundItem.isPresent()) {
                item = foundItem.get();
            }
        }
        
        // Check if we found a valid item (not AIR)
        if (item == null || item == Items.AIR) {
            throw ITEM_NOT_FOUND.create();
        }
        
        ItemStack stack = new ItemStack(item, amount);
        boolean itemAdded = player.getInventory().add(stack);
        
        if (itemAdded) {
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aGave &6" + amount + "x &a" + stack.getHoverName().getString() + " &ato " + player.getName().getString())), true);
            return 1;
        } else {
            player.drop(stack, false);
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aInventory full. Dropping &6" + amount + "x &a" + stack.getHoverName().getString())), true);
            return 1;
        }
    }

    /**
     * Executes the repair command for the item in hand.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeRepairHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("neoessentials.commands.repair.no_item"));
            return 0;
        }
        
        if (!stack.isDamageableItem()) {
            context.getSource().sendFailure(Component.translatable("neoessentials.commands.repair.not_damageable"));
            return 0;
        }
        
        stack.setDamageValue(0);
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aRepaired &6" + stack.getHoverName().getString())), true);
        return 1;
    }

    /**
     * Executes the repair command for all items in the player's inventory.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */    private int executeRepairAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        final int[] count = {0}; // Use an array to allow modification in lambda
        
        // Repair main inventory
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                stack.setDamageValue(0);
                count[0]++;
            }
        }
        
        // Repair armor
        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                stack.setDamageValue(0);
                count[0]++;
            }
        }
        
        // Repair offhand
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.isDamageableItem()) {
            offhand.setDamageValue(0);
            count[0]++;
        }
        
        if (count[0] > 0) {
            final int repairedCount = count[0]; // Final copy for lambda
            context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aRepaired &6" + repairedCount + " &aitems")), true);
            return count[0];
        } else {
            context.getSource().sendFailure(Component.translatable("neoessentials.commands.repair.no_items"));
            return 0;
        }
    }
}
