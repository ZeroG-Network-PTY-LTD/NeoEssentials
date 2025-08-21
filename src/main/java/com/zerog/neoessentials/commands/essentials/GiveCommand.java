package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.config.ModConfig;
import com.zerog.neoessentials.localization.LanguageManager;

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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.commands.arguments.CompoundTagArgument;
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
        int maxAmount = ModConfig.get().giveMaxAmount();
        boolean allowEnchantments = ModConfig.get().giveAllowEnchantments();
        dispatcher.register(
            Commands.literal("give")
                .requires(src -> PermissionUtil.hasPermission(src, "yourmod.give"))
                .then(Commands.argument("item", ItemArgument.item(context))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, maxAmount))
                        .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.empty()))
                        .then(allowEnchantments ? Commands.argument("nbt", CompoundTagArgument.compoundTag())
                            .requires(src -> PermissionUtil.hasPermission(src, "yourmod.give.enchant"))
                            .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(CompoundTagArgument.getCompoundTag(ctx, "nbt")), Optional.empty()))
                            .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> PermissionUtil.hasPermission(src, "yourmod.give.others"))
                                .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(CompoundTagArgument.getCompoundTag(ctx, "nbt")), Optional.of(EntityArgument.getPlayer(ctx, "player"))))
                            )
                        : Commands.argument("player", EntityArgument.player())
                            .requires(src -> PermissionUtil.hasPermission(src, "yourmod.give.others"))
                            .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.of(EntityArgument.getPlayer(ctx, "player"))))
                        )
                    )
                )
        );
    }

    /**
     * Give an item to a player
     */
    private static int giveItem(CommandSourceStack src, ItemInput itemRes, int amount, Optional<CompoundTag> nbtOpt, Optional<ServerPlayer> targetOpt) {
        ServerPlayer receiver = targetOpt.orElseGet(src::getPlayerOrException);
        ItemStack stack = itemRes.createItemStack(amount, false);
        nbtOpt.ifPresent(stack::setTag);
        boolean added = receiver.getInventory().add(stack);
        if (!added) receiver.drop(stack, false);
        String itemName = stack.getHoverName().getString();
        String giveMsg = LanguageManager.getInstance().getMessage(receiver, "neoessentials.give.give", amount, itemName);
        receiver.sendSystemMessage(Component.literal(giveMsg));
        if (targetOpt.isPresent() && targetOpt.get() != src.getPlayerOrException()) {
            String srcMsg = LanguageManager.getInstance().getMessage(src.getPlayerOrException(), "neoessentials.give.give_other", receiver.getName().getString(), amount, itemName);
            src.sendSuccess(() -> Component.literal(srcMsg), true);
        }
        return 1;
    }
}
