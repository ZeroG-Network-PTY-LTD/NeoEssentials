package com.zerog.neoessentials.economy;
import com.zerog.neoessentials.localization.LanguageManager;

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
                    context.getSource().sendFailure(MessageUtil.translatable("neoessentials.shop.command.players_only"));
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
                        context.getSource().sendFailure(MessageUtil.translatable("neoessentials.shop.command.players_only"));
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
                        context.getSource().sendFailure(MessageUtil.translatable("neoessentials.shop.command.players_only"));
                    }
                    return 1;
                }));
        
        dispatcher.register(command);
    }
    
    private static void showShopHelp(ServerPlayer player) {
        var lang = LanguageManager.getInstance();
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.header"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.intro"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.blank1"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.createshop"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.list"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.remove"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.blank2"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.howto"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.step1"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.step2"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.step3"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.step4"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.blank3"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.features"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.feature.buy"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.feature.sell"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.feature.click"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.help.feature.offline"));
    }
    
    private static void listPlayerShops(ServerPlayer player) {
        var lang = LanguageManager.getInstance();
    MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.list.header"));
        var shopManager = com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
        if (shopManager == null) {
            MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.list.not_initialized"));
            return;
        }
        var shops = shopManager.getPlayerShops().stream()
            .filter(shop -> shop.getOwnerId().equals(player.getUUID().toString()))
            .toList();
        if (shops.isEmpty()) {
            MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.list.empty"));
            return;
        }
        for (var shop : shops) {
            MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.list.entry",
                shop.getName(), shop.getCategory(), shop.getLocation(), shop.isActive() ? lang.getMessage(player, "neoessentials.shop.list.active.yes") : lang.getMessage(player, "neoessentials.shop.list.active.no"), shop.getTransactions()));
        }
    }
    
    private static void showRemoveHelp(ServerPlayer player) {
        var lang = LanguageManager.getInstance();
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.remove.header"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.remove.intro"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.remove.step1"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.remove.step2"));
        MessageUtil.sendMessage(player, lang.getMessage(player, "neoessentials.shop.remove.note"));
    }
}
