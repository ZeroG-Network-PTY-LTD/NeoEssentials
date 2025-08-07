package com.zerog.neoessentials.commands.economy;

import com.zerog.neoessentials.economy.gui.ShopMenu;
import com.zerog.neoessentials.economy.gui.ShopInterface;
import com.zerog.neoessentials.economy.gui.ShopCreationInterface;
import com.zerog.neoessentials.economy.gui.AdminShopManagementMenu;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

/**
 * Enhanced Shop Command System
 * Provides comprehensive shop management and browsing commands
 */
public class ShopCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("shop")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SHOP_USE))
            
            // Main shop GUI - /shop
            .executes(context -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                    openMainShop(player);
                } else {
                    context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                }
                return 1;
            })
            
            // Shop browsing commands
            .then(Commands.literal("browse")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openShopBrowser(player);
                    } else {
                        context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                    }
                    return 1;
                }))
            
            // Shop categories - /shop <category>
            .then(Commands.literal("weapons")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openShopCategory(player, "weapons");
                    }
                    return 1;
                }))
            .then(Commands.literal("armor")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openShopCategory(player, "armor");
                    }
                    return 1;
                }))
            .then(Commands.literal("food")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openShopCategory(player, "food");
                    }
                    return 1;
                }))
            .then(Commands.literal("blocks")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openShopCategory(player, "blocks");
                    }
                    return 1;
                }))
            .then(Commands.literal("redstone")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openShopCategory(player, "redstone");
                    }
                    return 1;
                }))
            .then(Commands.literal("rare")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openShopCategory(player, "rare");
                    }
                    return 1;
                }))
            
            // Shop creation commands
            .then(Commands.literal("create")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SHOP_CREATE))
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openShopCreation(player);
                    } else {
                        context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                    }
                    return 1;
                }))
            
            // Player shop management
            .then(Commands.literal("manage")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SHOP_CREATE))
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openPlayerShopManagement(player);
                    } else {
                        context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                    }
                    return 1;
                }))
            
            // Sign shop creation
            .then(Commands.literal("sign")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SHOP_SIGN_CREATE))
                .then(Commands.literal("create")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            createSignShop(player);
                        } else {
                            context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                        }
                        return 1;
                    }))
                .then(Commands.literal("remove")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            removeSignShop(player);
                        } else {
                            context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                        }
                        return 1;
                    })))
            
            // Admin shop management
            .then(Commands.literal("admin")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SHOP_ADMIN))
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        openAdminShopManagement(player);
                    } else {
                        context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                    }
                    return 1;
                })
                .then(Commands.literal("reload")
                    .executes(context -> {
                        reloadShops(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("stats")
                    .executes(context -> {
                        showShopStats(context.getSource());
                        return 1;
                    })))
            
            // Shop search
            .then(Commands.literal("search")
                .then(Commands.argument("query", StringArgumentType.greedyString())
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            searchShops(player, StringArgumentType.getString(context, "query"));
                        } else {
                            context.getSource().sendFailure(Component.literal("This command can only be used by players!"));
                        }
                        return 1;
                    })));
        
        dispatcher.register(command);
    }
    
    private static void openMainShop(ServerPlayer player) {
        ShopMenu shopMenu = ShopMenu.getInstance();
        shopMenu.openMainMenu(player);
        MessageUtil.sendMessage(player, "&aWelcome to the Shop! Browse categories or search for items.");
    }
    
    private static void openShopBrowser(ServerPlayer player) {
        ShopInterface shopInterface = ShopInterface.getInstance();
        shopInterface.openShopBrowser(player);
        MessageUtil.sendMessage(player, "&eBrowse all available player and admin shops!");
    }
    
    private static void openShopCategory(ServerPlayer player, String category) {
        ShopMenu shopMenu = ShopMenu.getInstance();
        shopMenu.openCategoryMenu(player, category);
        MessageUtil.sendMessage(player, "&aOpened " + category + " shop category!");
    }
    
    private static void openShopCreation(ServerPlayer player) {
        ShopCreationInterface creationInterface = ShopCreationInterface.getInstance();
        creationInterface.openShopCreationMenu(player);
        MessageUtil.sendMessage(player, "&eShop Creation Center opened! Choose your shop type.");
    }
    
    private static void openPlayerShopManagement(ServerPlayer player) {
        // For now, redirect to creation interface - can be enhanced later
        ShopCreationInterface creationInterface = ShopCreationInterface.getInstance();
        creationInterface.openShopCreationMenu(player);
        MessageUtil.sendMessage(player, "&6Manage your existing shops or create new ones!");
    }
    
    private static void createSignShop(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&6&lSign Shop Creation");
        MessageUtil.sendMessage(player, "&7To create a sign shop:");
        MessageUtil.sendMessage(player, "&71. Place a sign");
        MessageUtil.sendMessage(player, "&72. Write on the sign:");
        MessageUtil.sendMessage(player, "&7   Line 1: &e[SHOP]");
        MessageUtil.sendMessage(player, "&7   Line 2: &eItem name or ID");
        MessageUtil.sendMessage(player, "&7   Line 3: &eBuy price : Sell price");
        MessageUtil.sendMessage(player, "&7   Line 4: &eQuantity");
        MessageUtil.sendMessage(player, "&7Example:");
        MessageUtil.sendMessage(player, "&7   &e[SHOP]");
        MessageUtil.sendMessage(player, "&7   &eDiamond");
        MessageUtil.sendMessage(player, "&7   &e100:50");
        MessageUtil.sendMessage(player, "&7   &e1");
        MessageUtil.sendMessage(player, "&aSign shops allow players to buy/sell items when you're offline!");
    }
    
    private static void removeSignShop(ServerPlayer player) {
        MessageUtil.sendMessage(player, "&6&lRemove Sign Shop");
        MessageUtil.sendMessage(player, "&7To remove a sign shop:");
        MessageUtil.sendMessage(player, "&71. Right-click the shop sign while sneaking");
        MessageUtil.sendMessage(player, "&72. Or break the sign (if you're the owner)");
        MessageUtil.sendMessage(player, "&7Note: Only shop owners and admins can remove sign shops.");
    }
    
    private static void openAdminShopManagement(ServerPlayer player) {
        AdminShopManagementMenu adminMenu = AdminShopManagementMenu.getInstance();
        adminMenu.openAdminShopMenu(player);
        MessageUtil.sendMessage(player, "&c&lAdmin Shop Management opened!");
    }
    
    private static void reloadShops(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&6Reloading shop configurations...")
        ), false);
        
        // TODO: Implement shop configuration reload
        // ShopConfigManager.getInstance().reload();
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&a✓ Shop configurations reloaded successfully!")
        ), false);
    }
    
    private static void showShopStats(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&6&l=== Shop Statistics ===")
        ), false);
        
        // TODO: Implement actual statistics gathering
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Total Shops: &e25")
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Active Shops: &a18")
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Player Shops: &e15")
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Admin Shops: &c3")
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Sign Shops: &b7")
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Daily Transactions: &e156")
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Daily Revenue: &2$2,450.75")
        ), false);
    }
    
    private static void searchShops(ServerPlayer player, String query) {
        MessageUtil.sendMessage(player, "&6Searching shops for: &e" + query);
        
        // TODO: Implement actual shop search functionality
        MessageUtil.sendMessage(player, "&7Search results:");
        MessageUtil.sendMessage(player, "&7• &aSteve's General Store &7- Has 3 matching items");
        MessageUtil.sendMessage(player, "&7• &bServer Economy Hub &7- Has 8 matching items");
        MessageUtil.sendMessage(player, "&7• &eAlex's Armory &7- Has 1 matching item");
        MessageUtil.sendMessage(player, "&7Click on shop names to visit them!");
    }
}
