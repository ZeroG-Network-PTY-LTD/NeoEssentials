package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.ItemStackNbtUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.localization.LanguageManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Optional;

/**
 * Give command implementation - /give <player> <item> [amount]
 * Gives items to players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class GiveCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        var mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig();
        int maxAmount = mainConfig.maxGiveAmount;
        boolean allowEnchantments = mainConfig.allowGiveEnchantments;
        
        dispatcher.register(
            Commands.literal("give")
                .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give"))
                // /give <item>
                .then(Commands.argument("item", StringArgumentType.string())
                    .executes(ctx -> giveItem(ctx.getSource(), StringArgumentType.getString(ctx, "item"), 1, Optional.empty(), Optional.empty()))
                    // /give <item> <amount>
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, maxAmount))
                        .executes(ctx -> giveItem(ctx.getSource(), StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.empty()))
                        // /give <item> <amount> <player>
                        .then(Commands.argument("player", StringArgumentType.word())
                            .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give.others"))
                            .executes(ctx -> giveItem(ctx.getSource(), StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.of(StringArgumentType.getString(ctx, "player"))))
                            // /give <item> <amount> <player> <nbt> (if enchantments are allowed)
                            .then(allowEnchantments ? Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give.enchant"))
                                .executes(ctx -> giveItem(ctx.getSource(), StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(CompoundTagArgument.getCompoundTag(ctx, "nbt")), Optional.of(StringArgumentType.getString(ctx, "player"))))
                                : null
                            )
                        )
                        // /give <item> <amount> <nbt> (if enchantments are allowed)
                        .then(allowEnchantments ? Commands.argument("nbt", CompoundTagArgument.compoundTag())
                            .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give.enchant"))
                            .executes(ctx -> giveItem(ctx.getSource(), StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(CompoundTagArgument.getCompoundTag(ctx, "nbt")), Optional.empty()))
                            : null
                        )
                    )
                    // /give <item> <player>
                    .then(Commands.argument("player", StringArgumentType.word())
                        .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give.others"))
                        .executes(ctx -> giveItem(ctx.getSource(), StringArgumentType.getString(ctx, "item"), 1, Optional.empty(), Optional.of(StringArgumentType.getString(ctx, "player"))))
                    )
                )
        );
    }

    /**
     * Give an item to a player
     */
    private static int giveItem(CommandSourceStack src, String itemId, int amount, Optional<CompoundTag> nbtOpt, Optional<String> targetPlayerName) {
        ServerPlayer receiver;
        try {
            if (targetPlayerName.isPresent()) {
                // Find target player by name
                receiver = src.getServer().getPlayerList().getPlayerByName(targetPlayerName.get());
                if (receiver == null) {
                    src.sendFailure(Component.literal("Player '" + targetPlayerName.get() + "' not found."));
                    return 0;
                }
            } else {
                // Give to command executor
                receiver = src.getPlayerOrException();
            }
        } catch (CommandSyntaxException e) {
            src.sendFailure(Component.literal("Could not resolve target player."));
            return 0;
        }
        
        // Parse item ID and create ItemStack
        ItemStack stack;
        try {
            // Handle namespace (add minecraft: if not present)
            String fullItemId = itemId.contains(":") ? itemId : "minecraft:" + itemId;
            ResourceLocation itemResource = ResourceLocation.parse(fullItemId);
            Item item = BuiltInRegistries.ITEM.get(itemResource);
            
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                src.sendFailure(Component.literal("Unknown item: " + itemId));
                return 0;
            }
            
            stack = new ItemStack(item, amount);
        } catch (Exception e) {
            src.sendFailure(Component.literal("Invalid item: " + itemId));
            return 0;
        }
        
        // Apply NBT if present
        if (nbtOpt.isPresent()) {
            CompoundTag nbt = nbtOpt.get();
            ItemStackNbtUtil.mergeTag(stack, nbt);
        }
        
        // Give item to player
        boolean added = receiver.getInventory().add(stack);
        if (!added) {
            receiver.drop(stack, false);
        }
        
        // Send success messages
        String itemName = stack.getHoverName().getString();
        String giveMsg = LanguageManager.getInstance().getMessage(receiver, "neoessentials.give.give", amount, itemName);
        receiver.sendSystemMessage(Component.literal(giveMsg));
        
        try {
            ServerPlayer srcPlayer = src.getPlayerOrException();
            if (targetPlayerName.isPresent() && !targetPlayerName.get().equals(srcPlayer.getName().getString())) {
                String srcMsg = LanguageManager.getInstance().getMessage(srcPlayer, "neoessentials.give.give_other", receiver.getName().getString(), amount, itemName);
                src.sendSuccess(() -> Component.literal(srcMsg), true);
            }
        } catch (CommandSyntaxException ignored) {
            // Command source is not a player, send basic success message
            if (targetPlayerName.isPresent()) {
                src.sendSuccess(() -> Component.literal("Gave " + amount + " " + itemName + " to " + receiver.getName().getString()), true);
            }
        }
        
        return 1;
    }
}
