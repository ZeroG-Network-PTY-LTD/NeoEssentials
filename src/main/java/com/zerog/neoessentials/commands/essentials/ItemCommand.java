
package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.MainConfig;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.ItemStackNbtUtil;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

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
        var base = Commands.literal("item")
            .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_ITEM))
            .then(Commands.argument("item", ItemArgument.item(context))
                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, maxAmount))
                    .executes(ctx -> {
                        try {
                            return giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.empty());
                        } catch (CommandSyntaxException e) {
                            ctx.getSource().sendFailure(Component.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                            return 0;
                        }
                    })
                    .then(allowLore ? Commands.argument("lore", StringArgumentType.greedyString())
                        .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_UNLIMITED))
                        .executes(ctx -> {
                            try {
                                return giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(StringArgumentType.getString(ctx, "lore")), Optional.empty());
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendFailure(Component.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                                return 0;
                            }
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                            .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_ALL))
                            .executes(ctx -> {
                                try {
                                    return giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(StringArgumentType.getString(ctx, "lore")), Optional.of(EntityArgument.getPlayer(ctx, "player")));
                                } catch (CommandSyntaxException e) {
                                    ctx.getSource().sendFailure(Component.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                                    return 0;
                                }
                            })
                        )
                    : Commands.argument("player", EntityArgument.player())
                        .requires(src -> PermissionUtil.hasPermission(src, PermissionNodes.GIVE_ALL))
                        .executes(ctx -> {
                            try {
                                return giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.of(EntityArgument.getPlayer(ctx, "player")));
                            } catch (CommandSyntaxException e) {
                                ctx.getSource().sendFailure(Component.translatable("neoessentials.command.invalid_syntax", e.getMessage()));
                                return 0;
                            }
                        })
                    )
                )
            );
        dispatcher.register(base);
        dispatcher.register(Commands.literal("i").redirect(base.build()));
    }

    private static int giveItem(CommandSourceStack src, ItemInput itemRes, int amount, Optional<String> loreOpt, Optional<ServerPlayer> targetOpt) throws CommandSyntaxException {
        ServerPlayer receiver;
        try {
            receiver = targetOpt.orElseGet(() -> {
                try {
                    return src.getPlayerOrException();
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CommandSyntaxException) throw (CommandSyntaxException) e.getCause();
            throw e;
        }
        ItemStack stack = itemRes.createItemStack(amount, false);
        if (loreOpt.isPresent()) {
            // Use translation for lore (if needed, but lore is custom text, so keep as is)
            net.minecraft.nbt.CompoundTag displayTag = new net.minecraft.nbt.CompoundTag();
            ListTag loreList = new ListTag();
            loreList.add(StringTag.valueOf(Component.literal(loreOpt.get()).getString()));
            displayTag.put("Lore", loreList);
            ItemStackNbtUtil.mergeTag(stack, displayTag);
        }
        boolean added = receiver.getInventory().add(stack);
        if (!added) receiver.drop(stack, false);
        String itemName = stack.getHoverName().getString();
        receiver.sendSystemMessage(Component.translatable("neoessentials.item.give", amount, itemName));
        if (targetOpt.isPresent() && targetOpt.get() != src.getPlayerOrException()) {
            src.sendSuccess(() -> Component.translatable("neoessentials.item.give_other", receiver.getName(), amount, itemName), true);
        }
        return 1;
    }
    
    // ...existing code...
    // parseAmount is unused, can be removed or left for future use
    // ...existing code...
}
