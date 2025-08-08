package com.zerog.neoessentials.commands.economy;

import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/**
 * Sign Shop Command System
 * Provides sign-based shop management commands only
 */
public class ShopCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("shop")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SHOP_USE))
            
            // Shop help - /shop
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    showShopHelp(player);
                } else {
                    context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                }
                return 1;
            })
            
            // Sign shop creation help
            .then(Commands.literal("help")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        showShopHelp(player);
                    }
                    return 1;
                }))
            
            // List player's shops
            .then(Commands.literal("list")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SHOP_CREATE))
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        listPlayerShops(player);
                    } else {
                        context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                    }
                    return 1;
                }))
            
            // Remove player's shops
            .then(Commands.literal("remove")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SHOP_CREATE))
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        showRemoveHelp(player);
                    } else {
                        context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                    }
                    return 1;
                }));
        
        dispatcher.register(command);
    }
    
    private static void showShopHelp(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&6&l=== Sign Shop System ===");
        MessageUtil.sendMessage(player, "&7Create player-owned shops using signs!");
        MessageUtil.sendMessage(player, "");
        MessageUtil.sendMessage(player, "&e/createshop &7- Create a new sign shop");
        MessageUtil.sendMessage(player, "&e/shop list &7- List your shops");
        MessageUtil.sendMessage(player, "&e/shop remove &7- Remove shop instructions");
        MessageUtil.sendMessage(player, "");
        MessageUtil.sendMessage(player, "&7&lHow to create a sign shop:");
        MessageUtil.sendMessage(player, "&71. Place a &echest &7where you want to store items");
        MessageUtil.sendMessage(player, "&72. Place a &esign &7within 3 blocks of the chest");
        MessageUtil.sendMessage(player, "&73. Use &e/createshop &7while looking at the sign");
        MessageUtil.sendMessage(player, "&74. Follow the prompts to set prices");
        MessageUtil.sendMessage(player, "");
        MessageUtil.sendMessage(player, "&7&lShop Features:");
        MessageUtil.sendMessage(player, "&7• Players buy from your shop, you get money");
        MessageUtil.sendMessage(player, "&7• Players sell to your shop, items go in chest");
        MessageUtil.sendMessage(player, "&7• Right-click to buy, shift+right-click to sell");
        MessageUtil.sendMessage(player, "&7• Shops work even when you're offline!");
    }
    
    private static void listPlayerShops(ServerPlayer player) {
        // TODO: Implement actual shop listing
        MessageUtil.sendMessage(player, "&6&lYour Sign Shops:");
        MessageUtil.sendMessage(player, "&7Use &e/createshop &7to create your first shop!");
        MessageUtil.sendMessage(player, "&7Shops will be listed here once created.");
    }
    
    private static void showRemoveHelp(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&6&lRemoving Sign Shops:");
        MessageUtil.sendMessage(player, "&7To remove a sign shop:");
        MessageUtil.sendMessage(player, "&71. &eBreak the sign &7(only works if you own it)");
        MessageUtil.sendMessage(player, "&72. The shop will be automatically removed");
        MessageUtil.sendMessage(player, "&7Note: Items in the connected chest remain yours");
    }
}
