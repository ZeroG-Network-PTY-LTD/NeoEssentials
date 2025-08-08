package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repair command implementation - /repair, /fix
 * Repairs items in hand or all items
 */
public class RepairCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        // /repair [all] [player] - Repair item in hand or all items
        dispatcher.register(Commands.literal("repair")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .executes(ctx -> repairItemInHand(ctx, null))
            .then(Commands.literal("all")
                .executes(ctx -> repairAllItems(ctx, null))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> repairAllItems(ctx, EntityArgument.getPlayer(ctx, "player")))
                )
            )
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> repairItemInHand(ctx, EntityArgument.getPlayer(ctx, "player")))
            )
        );
        
        // /fix - Alias for /repair
        dispatcher.register(Commands.literal("fix")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .executes(ctx -> repairItemInHand(ctx, null))
            .then(Commands.literal("all")
                .executes(ctx -> repairAllItems(ctx, null))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> repairAllItems(ctx, EntityArgument.getPlayer(ctx, "player")))
                )
            )
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> repairItemInHand(ctx, EntityArgument.getPlayer(ctx, "player")))
            )
        );
    }
    
    private static int repairItemInHand(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) throws CommandSyntaxException {
        ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();
        
        if (heldItem.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§cNo item in hand to repair!"), false);
            return 0;
        }
        
        if (!heldItem.isDamageableItem()) {
            context.getSource().sendSuccess(() -> Component.literal("§cItem cannot be repaired!"), false);
            return 0;
        }
        
        if (!heldItem.isDamaged()) {
            context.getSource().sendSuccess(() -> Component.literal("§cItem is already fully repaired!"), false);
            return 0;
        }
        
        // Repair the item
        heldItem.setDamageValue(0);
        
        if (targetPlayer != null && targetPlayer != context.getSource().getPlayerOrException()) {
            context.getSource().sendSuccess(() -> Component.literal("§aRepaired " + player.getName().getString() + "'s item!"), false);
            MessageUtil.sendMessage(targetPlayer, "§aYour item has been repaired!");
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§aItem repaired!"), false);
        }
        
        return 1;
    }
    
    private static int repairAllItems(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) throws CommandSyntaxException {
        ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();
        AtomicInteger repairedCount = new AtomicInteger(0);
        
        // Repair all items in inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
                stack.setDamageValue(0);
                repairedCount.incrementAndGet();
            }
        }
        
        // Repair armor items
        for (ItemStack armorStack : player.getInventory().armor) {
            if (!armorStack.isEmpty() && armorStack.isDamageableItem() && armorStack.isDamaged()) {
                armorStack.setDamageValue(0);
                repairedCount.incrementAndGet();
            }
        }
        
        // Repair offhand item
        ItemStack offhandStack = player.getInventory().offhand.get(0);
        if (!offhandStack.isEmpty() && offhandStack.isDamageableItem() && offhandStack.isDamaged()) {
            offhandStack.setDamageValue(0);
            repairedCount.incrementAndGet();
        }
        
        if (repairedCount.get() == 0) {
            context.getSource().sendSuccess(() -> Component.literal("§cNo items to repair!"), false);
            return 0;
        }
        
        if (targetPlayer != null && targetPlayer != context.getSource().getPlayerOrException()) {
            context.getSource().sendSuccess(() -> Component.literal("§aRepaired " + repairedCount.get() + " items for " + player.getName().getString() + "!"), false);
            MessageUtil.sendMessage(targetPlayer, "§aAll your items have been repaired! (%d items)", repairedCount.get());
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§aRepaired " + repairedCount.get() + " items!"), false);
        }
        
        return repairedCount.get();
    }
}
