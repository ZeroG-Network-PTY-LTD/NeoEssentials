package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles all kit-related commands
 */
public class KitCommands {
    
    /**
     * Register all kit-related commands
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /kit <name> - Claim a kit
        dispatcher.register(
            Commands.literal("kit")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.kit"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeKit)
                )
                .executes(this::executeKitList)
        );
        
        // /kits - List all available kits
        dispatcher.register(
            Commands.literal("kits")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.kits"))
                .executes(this::executeKitList)
        );
        
        // /createkit <name> <cooldown> - Create a kit with your current inventory
        dispatcher.register(
            Commands.literal("createkit")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.createkit"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("cooldown", LongArgumentType.longArg(0))
                        .executes(this::executeCreateKit)
                    )
                    .executes(context -> executeCreateKit(context, 0)) // Default cooldown of 0
                )
        );
        
        // /deletekit <name> - Delete a kit
        dispatcher.register(
            Commands.literal("deletekit")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.deletekit"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeDeleteKit)
                )
        );
        
        // /givekit <player> <kit> - Give a kit to another player
        dispatcher.register(
            Commands.literal("givekit")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.givekit"))
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                    .then(Commands.argument("kit", StringArgumentType.word())
                        .executes(this::executeGiveKit)
                    )
                )
        );
    }
    
    /**
     * Execute the /kit command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        KitManager.Kit kit = kitManager.getKit(kitName);
        
        if (kit == null) {
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' not found"));
            return 0;
        }
        
        // Check if player can use kit (permissions and cooldown)
        if (!kitManager.canUseKit(player, kitName)) {
            long cooldown = kitManager.getRemainingCooldown(player, kitName);
            
            if (cooldown > 0) {
                String timeStr = formatTime(cooldown);
                context.getSource().sendFailure(Component.literal("You must wait " + timeStr + " before using this kit again"));
            } else {
                context.getSource().sendFailure(Component.literal("You don't have permission to use this kit"));
            }
            
            return 0;
        }
        
        // Give the kit to the player
        boolean success = kitManager.giveKit(player, kitName);
        
        if (success) {
            MutableComponent message = Component.literal("You received kit '" + kitName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to give kit '" + kitName + "'"));
            return 0;
        }
    }
    
    /**
     * Execute the /kits command to list all available kits
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeKitList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        Map<String, KitManager.Kit> kits = kitManager.getAllKits();
        
        if (kits.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No kits available"));
            return 0;
        }
        
        MutableComponent message = Component.literal("Available kits: ");
        
        boolean first = true;
        for (String kitName : kits.keySet()) {
            if (!first) {
                message.append(Component.literal(", "));
            }
            
            // Check if the player can use this kit (permissions)
            if (kitManager.canUseKit(player, kitName)) {
                // Check cooldown
                long cooldown = kitManager.getRemainingCooldown(player, kitName);
                
                if (cooldown > 0) {
                    // On cooldown - show in red with cooldown time
                    String timeStr = formatTime(cooldown);
                    message.append(Component.literal(kitName + " (" + timeStr + ")").withStyle(net.minecraft.ChatFormatting.RED));
                } else {
                    // Available - show in green
                    message.append(Component.literal(kitName).withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            } else {
                // No permission - show in gray
                message.append(Component.literal(kitName).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            
            first = false;
        }
        
        MessageUtil.sendInfo(player, message);
        return 1;
    }
    
    /**
     * Format a time in seconds to a human-readable string
     * 
     * @param seconds The time in seconds
     * @return A formatted time string
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            
            if (remainingSeconds == 0) {
                return minutes + "m";
            } else {
                return minutes + "m " + remainingSeconds + "s";
            }
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            
            if (remainingMinutes == 0) {
                return hours + "h";
            } else {
                return hours + "h " + remainingMinutes + "m";
            }
        }
    }
    
    /**
     * Execute the /createkit command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeCreateKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCreateKit(context, LongArgumentType.getLong(context, "cooldown"));
    }
    
    /**
     * Execute the /createkit command with a specified cooldown
     * 
     * @param context The command context
     * @param cooldown The cooldown in seconds
     * @return Command result
     */
    private int executeCreateKit(CommandContext<CommandSourceStack> context, long cooldown) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        
        // Check if kit already exists
        if (kitManager.getKit(kitName) != null) {
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' already exists. Delete it first if you want to replace it."));
            return 0;
        }
        
        // Get all items from the player's inventory
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().items) {
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }
        
        if (items.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Your inventory is empty. Cannot create an empty kit."));
            return 0;
        }
        
        // Create the kit
        KitManager.Kit kit = kitManager.createKit(kitName, cooldown, "neoessentials.kit." + kitName.toLowerCase(), items);
        
        MutableComponent message = Component.literal("Created kit '" + kitName + "' with " + items.size() + " items");
        if (cooldown > 0) {
            message.append(Component.literal(" and a cooldown of " + formatTime(cooldown)));
        }
        
        MessageUtil.sendSuccess(player, message);
        return 1;
    }
    
    /**
     * Execute the /deletekit command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeDeleteKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        boolean success = kitManager.deleteKit(kitName);
        
        if (success) {
            MutableComponent message = Component.literal("Deleted kit '" + kitName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' not found"));
            return 0;
        }
    }
    
    /**
     * Execute the /givekit command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeGiveKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
        String kitName = StringArgumentType.getString(context, "kit");
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        KitManager.Kit kit = kitManager.getKit(kitName);
        
        if (kit == null) {
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' not found"));
            return 0;
        }
        
        // Give the kit to the target player, bypassing cooldown
        boolean success = kitManager.giveKit(target, kitName);
        
        if (success) {
            MutableComponent messageToAdmin = Component.literal("Gave kit '" + kitName + "' to " + target.getScoreboardName());
            MessageUtil.sendSuccess(player, messageToAdmin);
            
            MutableComponent messageToTarget = Component.literal("You received kit '" + kitName + "' from " + player.getScoreboardName());
            MessageUtil.sendInfo(target, messageToTarget);
            
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Failed to give kit '" + kitName + "' to " + target.getScoreboardName()));
            return 0;
        }
    }
}
