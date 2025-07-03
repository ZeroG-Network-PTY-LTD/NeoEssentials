package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.ColorUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.server.command.EnumArgument;

import java.util.Collection;

/**
 * Commands for item enhancement and manipulation.
 * 
 * <p>This class provides commands for enhancing and manipulating items in various ways:
 * <ul>
 *   <li>{@code /more} - Fill item stacks to maximum or specified amount</li>
 *   <li>{@code /itemname} - Rename items with color code support</li>
 *   <li>{@code /iname} - Alias for itemname</li>
 *   <li>{@code /hat} - Wear held item as a hat</li>
 *   <li>{@code /skull} - Get player skull heads</li>
 *   <li>{@code /head} - Alias for skull</li>
 * </ul>
 * 
 * <p>All commands include proper permission checking and error handling.
 * Color codes are supported using the § symbol for item names.
 * 
 * @author ZeroG
 * @since 1.0.2.95
 */
public class ItemEnhancementCommands {
    
    /**
     * Registers all item enhancement commands with the command dispatcher.
     * 
     * @param dispatcher The command dispatcher to register commands with
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerMoreCommand(dispatcher);
        registerItemNameCommands(dispatcher);
        registerHatCommand(dispatcher);
        registerSkullCommands(dispatcher);
    }
    
    /**
     * Registers the /more command and its variants.
     * 
     * @param dispatcher The command dispatcher
     */
    private static void registerMoreCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("more")
            .requires(source -> hasPermission(source, "neoessentials.more"))
            .executes(context -> executeMore(context, -1)) // Fill to max
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                .suggests(TabCompletionUtil.STACK_AMOUNT_SUGGESTIONS)
                .executes(context -> executeMore(context, IntegerArgumentType.getInteger(context, "amount")))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> hasPermission(source, "neoessentials.more.others"))
                    .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                    .executes(context -> executeMoreForPlayer(context, 
                        IntegerArgumentType.getInteger(context, "amount"),
                        EntityArgument.getPlayer(context, "player")))
                )
            )
        );
    }
    
    /**
     * Registers the /itemname and /iname commands.
     * 
     * @param dispatcher The command dispatcher
     */
    private static void registerItemNameCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /itemname command
        dispatcher.register(Commands.literal("itemname")
            .requires(source -> hasPermission(source, "neoessentials.itemname"))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(TabCompletionUtil.ITEM_NAME_SUGGESTIONS)
                .executes(context -> executeItemName(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /iname alias
        dispatcher.register(Commands.literal("iname")
            .requires(source -> hasPermission(source, "neoessentials.itemname"))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(TabCompletionUtil.ITEM_NAME_SUGGESTIONS)
                .executes(context -> executeItemName(context, StringArgumentType.getString(context, "name")))
            )
        );
    }
    
    /**
     * Registers the /hat command.
     * 
     * @param dispatcher The command dispatcher
     */
    private static void registerHatCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hat")
            .requires(source -> hasPermission(source, "neoessentials.hat"))
            .executes(context -> executeHat(context))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> hasPermission(source, "neoessentials.hat.others"))
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .executes(context -> executeHatForPlayer(context, EntityArgument.getPlayer(context, "player")))
            )
        );
    }
    
    /**
     * Registers the /skull and /head commands.
     * 
     * @param dispatcher The command dispatcher
     */
    private static void registerSkullCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /skull command
        dispatcher.register(Commands.literal("skull")
            .requires(source -> hasPermission(source, "neoessentials.skull"))
            .executes(context -> executeSkull(context, null)) // Own skull
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .executes(context -> executeSkull(context, GameProfileArgument.getGameProfiles(context, "player")))
            )
        );
        
        // /head alias
        dispatcher.register(Commands.literal("head")
            .requires(source -> hasPermission(source, "neoessentials.skull"))
            .executes(context -> executeSkull(context, null)) // Own skull
            .then(Commands.argument("player", GameProfileArgument.gameProfile())
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .executes(context -> executeSkull(context, GameProfileArgument.getGameProfiles(context, "player")))
            )
        );
    }
    
    /**
     * Executes the /more command.
     * 
     * @param context The command context
     * @param amount The amount to fill to, or -1 for max stack size
     * @return Command result
     */
    private static int executeMore(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return executeMoreForPlayer(context, amount, player);
    }
    
    /**
     * Executes the /more command for a specific player.
     * 
     * @param context The command context
     * @param amount The amount to fill to, or -1 for max stack size
     * @param target The target player
     * @return Command result
     */
    private static int executeMoreForPlayer(CommandContext<CommandSourceStack> context, int amount, ServerPlayer target) {
        try {
            ItemStack heldItem = target.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§cYou must be holding an item to use this command."));
                return 0;
            }
            
            int maxStackSize = heldItem.getMaxStackSize();
            int targetAmount = (amount == -1) ? maxStackSize : Math.min(amount, maxStackSize);
            
            if (heldItem.getCount() >= targetAmount) {
                context.getSource().sendFailure(Component.literal("§cItem stack is already at or above the requested amount."));
                return 0;
            }
            
            int oldCount = heldItem.getCount();
            heldItem.setCount(targetAmount);
            
            // Send success message
            String itemName = heldItem.getHoverName().getString();
            Component message = Component.literal("§aFilled §e" + itemName + "§a from §e" + oldCount + "§a to §e" + targetAmount + "§a.");
            
            if (context.getSource().getEntity() instanceof ServerPlayer executor && !executor.equals(target)) {
                executor.sendSystemMessage(Component.literal("§aFilled §e" + target.getDisplayName().getString() + "§a's §e" + itemName + "§a to §e" + targetAmount + "§a."));
            }
            
            target.sendSystemMessage(message);
            
            // Log the command usage
            NeoEssentials.LOGGER.info("Player {} used /more command on {} (amount: {}, target: {})", 
                context.getSource().getTextName(), itemName, targetAmount, target.getDisplayName().getString());
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing more command: " + e.getMessage()));
            NeoEssentials.LOGGER.error("Error in /more command", e);
            return 0;
        }
    }
    
    /**
     * Executes the /itemname command.
     * 
     * @param context The command context
     * @param name The new name for the item
     * @return Command result
     */
    private static int executeItemName(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        try {
            ItemStack heldItem = player.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§cYou must be holding an item to use this command."));
                return 0;
            }
            
            // Process color codes
            String coloredName = ColorUtils.processColorCodes(name);
            
            // Set the custom name
            heldItem.set(DataComponents.CUSTOM_NAME, Component.literal(coloredName));
            
            // Send success message
            context.getSource().sendSuccess(
                () -> Component.literal("§aItem renamed to: " + coloredName),
                false
            );
            
            // Log the command usage
            NeoEssentials.LOGGER.info("Player {} renamed item to: {}", 
                context.getSource().getTextName(), coloredName);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError renaming item: " + e.getMessage()));
            NeoEssentials.LOGGER.error("Error in /itemname command", e);
            return 0;
        }
    }
    
    /**
     * Executes the /hat command.
     * 
     * @param context The command context
     * @return Command result
     */
    private static int executeHat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return executeHatForPlayer(context, player);
    }
    
    /**
     * Executes the /hat command for a specific player.
     * 
     * @param context The command context
     * @param target The target player
     * @return Command result
     */
    private static int executeHatForPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        try {
            ItemStack heldItem = target.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                context.getSource().sendFailure(Component.literal("§cYou must be holding an item to use this command."));
                return 0;
            }
            
            // Get current helmet
            ItemStack currentHelmet = target.getItemBySlot(EquipmentSlot.HEAD);
            
            // Swap held item with helmet slot
            target.setItemSlot(EquipmentSlot.HEAD, heldItem.copy());
            
            // Put helmet in hand (or empty if no helmet)
            if (!currentHelmet.isEmpty()) {
                target.setItemSlot(EquipmentSlot.MAINHAND, currentHelmet);
            } else {
                target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            
            // Send success message
            String itemName = heldItem.getHoverName().getString();
            Component message = Component.literal("§aYou are now wearing §e" + itemName + "§a on your head!");
            
            if (context.getSource().getEntity() instanceof ServerPlayer executor && !executor.equals(target)) {
                executor.sendSystemMessage(Component.literal("§aSet §e" + target.getDisplayName().getString() + "§a's hat to §e" + itemName + "§a."));
            }
            
            target.sendSystemMessage(message);
            
            // Log the command usage
            NeoEssentials.LOGGER.info("Player {} used /hat command with {} (target: {})", 
                context.getSource().getTextName(), itemName, target.getDisplayName().getString());
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing hat command: " + e.getMessage()));
            NeoEssentials.LOGGER.error("Error in /hat command", e);
            return 0;
        }
    }
    
    /**
     * Executes the /skull command.
     * 
     * @param context The command context
     * @param profiles The player profiles to get skulls for, or null for own skull
     * @return Command result
     */
    private static int executeSkull(CommandContext<CommandSourceStack> context, Collection<com.mojang.authlib.GameProfile> profiles) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        try {
            com.mojang.authlib.GameProfile targetProfile;
            
            if (profiles == null || profiles.isEmpty()) {
                // Use own profile
                targetProfile = player.getGameProfile();
            } else {
                // Use first profile from collection
                targetProfile = profiles.iterator().next();
            }
            
            // Create player skull
            ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
            
            // Set the skull owner using ResolvableProfile
            skull.set(DataComponents.PROFILE, new ResolvableProfile(targetProfile));
            
            // Give the skull to the player
            boolean success = player.getInventory().add(skull);
            
            if (success) {
                context.getSource().sendSuccess(
                    () -> Component.literal("§aGave you §e" + targetProfile.getName() + "§a's skull!"),
                    false
                );
                
                // Log the command usage
                NeoEssentials.LOGGER.info("Player {} received skull of {}", 
                    context.getSource().getTextName(), targetProfile.getName());
                
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§cYour inventory is full!"));
                return 0;
            }
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError creating skull: " + e.getMessage()));
            NeoEssentials.LOGGER.error("Error in /skull command", e);
            return 0;
        }
    }
    
    /**
     * Checks if a command source has the specified permission.
     * 
     * @param source The command source
     * @param permission The permission node to check
     * @return true if the source has permission, false otherwise
     */
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        try {
            if (source.hasPermission(4)) { // OP level 4
                return true;
            }
            
            // Check with CommandManager permission system
            return CommandManager.hasPermission(source, permission);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking permission {}: {}", permission, e.getMessage());
            return false;
        }
    }
}
