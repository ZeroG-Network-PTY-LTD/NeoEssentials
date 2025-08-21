package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.config.ModConfig;
import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
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
        int maxAmount = ModConfig.get().itemMaxAmount();
        boolean allowLore = ModConfig.get().itemAllowLore();
        // /item <item> <amount> [lore] [player]
        var base = Commands.literal("item")
            .requires(src -> PermissionUtil.hasPermission(src, "yourmod.item"))
            .then(Commands.argument("item", ItemArgument.item(context))
                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, maxAmount))
                    .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.empty()))
                    .then(allowLore ? Commands.argument("lore", StringArgumentType.greedyString())
                        .requires(src -> PermissionUtil.hasPermission(src, "yourmod.item.lore"))
                        .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(StringArgumentType.getString(ctx, "lore")), Optional.empty()))
                        .then(Commands.argument("player", EntityArgument.player())
                            .requires(src -> PermissionUtil.hasPermission(src, "yourmod.item.others"))
                            .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(StringArgumentType.getString(ctx, "lore")), Optional.of(EntityArgument.getPlayer(ctx, "player"))))
                        )
                    : Commands.argument("player", EntityArgument.player())
                        .requires(src -> PermissionUtil.hasPermission(src, "yourmod.item.others"))
                        .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.of(EntityArgument.getPlayer(ctx, "player"))))
                    )
                )
            );
        dispatcher.register(base);
        dispatcher.register(Commands.literal("i").redirect(base.build()));
    }

    private static int giveItem(CommandSourceStack src, ItemInput itemRes, int amount, Optional<String> loreOpt, Optional<ServerPlayer> targetOpt) throws CommandSyntaxException {
        ServerPlayer receiver = targetOpt.orElseGet(src::getPlayerOrException);
        ItemStack stack = itemRes.createItemStack(amount, false);
        if (loreOpt.isPresent()) {
            Component loreText = Component.literal(loreOpt.get());
            ListTag loreList = new ListTag();
            loreList.add(StringTag.valueOf(loreText.getString()));
            stack.getOrCreateTagElement("display").put("Lore", loreList);
        }
        boolean added = receiver.getInventory().add(stack);
        if (!added) receiver.drop(stack, false);
        String itemName = stack.getHoverName().getString();
        String giveMsg = LanguageManager.getInstance().getMessage(receiver, "neoessentials.item.give", amount, itemName);
        receiver.sendSystemMessage(Component.literal(giveMsg));
        if (targetOpt.isPresent() && targetOpt.get() != src.getPlayerOrException()) {
            String srcMsg = LanguageManager.getInstance().getMessage(src.getPlayerOrException(), "neoessentials.item.give_other", receiver.getName().getString(), amount, itemName);
            src.sendSuccess(() -> Component.literal(srcMsg), true);
        }
        return 1;
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
