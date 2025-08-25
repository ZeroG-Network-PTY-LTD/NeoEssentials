package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.ItemStackNbtUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.localization.LanguageManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
    var mainConfig = com.zerog.neoessentials.config.ConfigurationUnifier.getInstance().getConfigManager().getMainConfig();
    int maxAmount = mainConfig.maxGiveAmount;
    boolean allowEnchantments = mainConfig.allowGiveEnchantments;
        dispatcher.register(
            Commands.literal("give")
                .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give"))
                .then(Commands.argument("item", ItemArgument.item(context))
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, maxAmount))
                        .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.empty(), Optional.empty()))
                        .then(allowEnchantments ? Commands.argument("nbt", CompoundTagArgument.compoundTag())
                            .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give.enchant"))
                            .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(CompoundTagArgument.getCompoundTag(ctx, "nbt")), Optional.empty()))
                            .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give.others"))
                                .executes(ctx -> giveItem(ctx.getSource(), ItemArgument.getItem(ctx, "item"), IntegerArgumentType.getInteger(ctx, "amount"), Optional.of(CompoundTagArgument.getCompoundTag(ctx, "nbt")), Optional.of(EntityArgument.getPlayer(ctx, "player"))))
                            )
                        : Commands.argument("player", EntityArgument.player())
                            .requires(src -> PermissionUtil.hasPermission(src, "neoessentials.give.others"))
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
            src.sendFailure(Component.literal("Could not resolve target player."));
            return 0;
        }
        ItemStack stack;
        try {
            stack = itemRes.createItemStack(amount, false);
        } catch (CommandSyntaxException e) {
            src.sendFailure(Component.literal("Invalid item or NBT data."));
            return 0;
        }
        if (nbtOpt.isPresent()) {
            CompoundTag nbt = nbtOpt.get();
            ItemStackNbtUtil.mergeTag(stack, nbt);
        }
        boolean added = receiver.getInventory().add(stack);
        if (!added) receiver.drop(stack, false);
        String itemName = stack.getHoverName().getString();
        String giveMsg = LanguageManager.getInstance().getMessage(receiver, "neoessentials.give.give", amount, itemName);
        receiver.sendSystemMessage(Component.literal(giveMsg));
        try {
            ServerPlayer srcPlayer = src.getPlayerOrException();
            if (targetOpt.isPresent() && targetOpt.get() != srcPlayer) {
                String srcMsg = LanguageManager.getInstance().getMessage(srcPlayer, "neoessentials.give.give_other", receiver.getName().getString(), amount, itemName);
                src.sendSuccess(() -> Component.literal(srcMsg), true);
            }
        } catch (CommandSyntaxException ignored) {}
        return 1;
    }
}
