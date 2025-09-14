package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.MainConfig;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/**
 * Item command implementation - /item, /i
 * Allows spawning items for players
 */
public class ItemCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        MainConfig.ItemManagementConfig itemConfig = ConfigManager.getInstance().getMainConfig().itemManagement;
        int maxAmount = itemConfig.maxStackSize;
        boolean allowLore = itemConfig.allowEnchantments; // Use config-driven option for lore/enchantments
        
        // /item <item> <amount> [lore] [player]
        dispatcher.register(Commands.literal("item")
            .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_ITEM))
            .then(Commands.argument("item", StringArgumentType.word())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, maxAmount))
                    .executes(ctx -> {
                        try {
                            String itemName = StringArgumentType.getString(ctx, "item");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            return giveItem(ctx.getSource(), itemName, amount, Optional.empty(), Optional.empty());
                        } catch (CommandSyntaxException e) {
                            ctx.getSource().sendFailure(MessageUtil.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                            return 0;
                        }
                    })
                    .then(allowLore ? Commands.argument("lore", StringArgumentType.greedyString())
                        .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_UNLIMITED))
                        .executes(ctx -> {
                            try {
                                String itemName = StringArgumentType.getString(ctx, "item");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                String lore = StringArgumentType.getString(ctx, "lore");
                                return giveItem(ctx.getSource(), itemName, amount, Optional.of(lore), Optional.empty());
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendFailure(MessageUtil.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                                return 0;
                            }
                        })
                        .then(Commands.argument("player", StringArgumentType.word())
                            .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_ALL))
                            .executes(ctx -> {
                                try {
                                    String itemName = StringArgumentType.getString(ctx, "item");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    String lore = StringArgumentType.getString(ctx, "lore");
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    return giveItem(ctx.getSource(), itemName, amount, Optional.of(lore), Optional.of(playerName));
                                } catch (CommandSyntaxException e) {
                                    ctx.getSource().sendFailure(MessageUtil.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                                    return 0;
                                }
                            })
                        )
                    : Commands.argument("player", StringArgumentType.word())
                        .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_ALL))
                        .executes(ctx -> {
                            try {
                                String itemName = StringArgumentType.getString(ctx, "item");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                String playerName = StringArgumentType.getString(ctx, "player");
                                return giveItem(ctx.getSource(), itemName, amount, Optional.empty(), Optional.of(playerName));
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendFailure(MessageUtil.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                                return 0;
                            }
                        })
                    )
                )
            )
        );
        
        // Register /i alias
        dispatcher.register(Commands.literal("i")
            .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_ITEM))
            .then(Commands.argument("item", StringArgumentType.word())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, maxAmount))
                    .executes(ctx -> {
                        try {
                            String itemName = StringArgumentType.getString(ctx, "item");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            return giveItem(ctx.getSource(), itemName, amount, Optional.empty(), Optional.empty());
                        } catch (CommandSyntaxException e) {
                            ctx.getSource().sendFailure(MessageUtil.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                            return 0;
                        }
                    })
                )
            )
        );
    }

    private static int giveItem(CommandSourceStack src, String itemName, int amount, Optional<String> loreOpt, Optional<String> targetPlayerName) throws CommandSyntaxException {
        // Resolve the target player
        ServerPlayer receiver;
        if (targetPlayerName.isPresent()) {
            receiver = src.getServer().getPlayerList().getPlayerByName(targetPlayerName.get());
            if (receiver == null) {
                src.sendFailure(MessageUtil.translatable("neoessentials.player.not_found", targetPlayerName.get()));
                return 0;
            }
        } else {
            receiver = src.getPlayerOrException();
        }

        // Parse item name to get the Item
        Item item;
        try {
            ResourceLocation itemLocation = ResourceLocation.parse(itemName);
            item = BuiltInRegistries.ITEM.get(itemLocation);
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                // Try without namespace if it failed
                if (!itemName.contains(":")) {
                    itemLocation = ResourceLocation.parse("minecraft:" + itemName);
                    item = BuiltInRegistries.ITEM.get(itemLocation);
                }
                if (item == null || item == net.minecraft.world.item.Items.AIR) {
                    src.sendFailure(MessageUtil.translatable("neoessentials.item.not_found", itemName));
                    return 0;
                }
            }
        } catch (Exception e) {
            src.sendFailure(MessageUtil.translatable("neoessentials.item.invalid_name", itemName));
            return 0;
        }

        // Create the ItemStack
        ItemStack stack = new ItemStack(item, amount);
        
        // Apply lore if provided - using KitItem's approach and ItemStackNbtUtil
        if (loreOpt.isPresent()) {
            String loreText = loreOpt.get();
            String[] loreLines = loreText.split("\\|");
            CompoundTag tag = new CompoundTag();
            CompoundTag display = new CompoundTag();
            if (loreLines.length > 0) {
                StringBuilder loreJson = new StringBuilder("[");
                for (int i = 0; i < loreLines.length; i++) {
                    if (i > 0) loreJson.append(",");
                    loreJson.append("{\"text\":\"").append(loreLines[i]).append("\"}");
                }
                loreJson.append("]");
                display.putString("Lore", loreJson.toString());
            }
            tag.put("display", display);
            com.zerog.neoessentials.util.ItemStackNbtUtil.mergeTag(stack, tag);
        }

        // Give item to player
        boolean added = receiver.getInventory().add(stack);
        if (!added) {
            receiver.drop(stack, false);
        }

        // Send success messages
        String itemDisplayName = stack.getHoverName().getString();
        receiver.sendSystemMessage(MessageUtil.translatable(receiver, "neoessentials.item.give", amount, itemDisplayName));
        
        if (targetPlayerName.isPresent() && !targetPlayerName.get().equals(src.getTextName())) {
            src.sendSuccess(() -> MessageUtil.translatable("neoessentials.item.give_other", receiver.getName(), amount, itemDisplayName), true);
        }
        
        return 1;
    }
    
    // ...existing code...
}