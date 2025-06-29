package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Comprehensive shop commands for the NeoEssentials economy system.
 * Provides shop creation, management, buying, selling, and administration.
 */
public class ShopCommands {
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("shop")
                .executes(context -> showShopHelp(context.getSource()))
                .then(Commands.literal("help")
                    .executes(context -> showShopHelp(context.getSource())))
                .then(Commands.literal("status")
                    .executes(context -> showShopStatus(context.getSource())))
                
                // Shop Management
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("category", StringArgumentType.string())
                            .suggests(TabCompletionUtil.SHOP_TYPE_SUGGESTIONS) // This gives categories like armor, blocks, etc.
                            .then(Commands.argument("ownership", StringArgumentType.string())
                                .suggests(TabCompletionUtil.SHOP_OWNERSHIP_SUGGESTIONS) // This will give player, server, auction
                                .executes(context -> createShop(context.getSource(),
                                    StringArgumentType.getString(context, "name"),
                                    StringArgumentType.getString(context, "category"),
                                    StringArgumentType.getString(context, "ownership")))))))
                .then(Commands.literal("list")
                    .executes(context -> listShops(context.getSource(), null))
                    .then(Commands.argument("filter", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_TYPE_SUGGESTIONS)
                        .executes(context -> listShops(context.getSource(),
                            StringArgumentType.getString(context, "filter")))))
                .then(Commands.literal("info")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> shopInfo(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                .then(Commands.literal("delete")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> deleteShop(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                
                // Trading Operations
                .then(Commands.literal("buy")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> buyFromShop(context.getSource(),
                                StringArgumentType.getString(context, "shop"),
                                StringArgumentType.getString(context, "item"), 1))
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .suggests(TabCompletionUtil.QUANTITY_SUGGESTIONS)
                                .executes(context -> buyFromShop(context.getSource(),
                                    StringArgumentType.getString(context, "shop"),
                                    StringArgumentType.getString(context, "item"),
                                    IntegerArgumentType.getInteger(context, "quantity")))))))
                .then(Commands.literal("sell")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> sellToShop(context.getSource(),
                                StringArgumentType.getString(context, "shop"),
                                StringArgumentType.getString(context, "item"), 1))
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .suggests(TabCompletionUtil.QUANTITY_SUGGESTIONS)
                                .executes(context -> sellToShop(context.getSource(),
                                    StringArgumentType.getString(context, "shop"),
                                    StringArgumentType.getString(context, "item"),
                                    IntegerArgumentType.getInteger(context, "quantity")))))))
                
                // Shop Management (Owner Operations)
                .then(Commands.literal("stock")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .suggests(TabCompletionUtil.QUANTITY_SUGGESTIONS)
                                .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                    .suggests(TabCompletionUtil.PRICE_SUGGESTIONS)
                                    .executes(context -> stockShop(context.getSource(),
                                        StringArgumentType.getString(context, "shop"),
                                        StringArgumentType.getString(context, "item"),
                                        IntegerArgumentType.getInteger(context, "quantity"),
                                        DoubleArgumentType.getDouble(context, "price"))))))))
                .then(Commands.literal("price")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .then(Commands.argument("buy-price", DoubleArgumentType.doubleArg(0.01))
                                .suggests(TabCompletionUtil.PRICE_SUGGESTIONS)
                                .executes(context -> setItemPrice(context.getSource(),
                                    StringArgumentType.getString(context, "shop"),
                                    StringArgumentType.getString(context, "item"),
                                    DoubleArgumentType.getDouble(context, "buy-price"), -1))
                                .then(Commands.argument("sell-price", DoubleArgumentType.doubleArg(0.01))
                                    .suggests(TabCompletionUtil.PRICE_SUGGESTIONS)
                                    .executes(context -> setItemPrice(context.getSource(),
                                        StringArgumentType.getString(context, "shop"),
                                        StringArgumentType.getString(context, "item"),
                                        DoubleArgumentType.getDouble(context, "buy-price"),
                                        DoubleArgumentType.getDouble(context, "sell-price"))))))))
                .then(Commands.literal("manage")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .executes(context -> manageShop(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                .then(Commands.literal("stats")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .executes(context -> shopStats(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                
                // Additional Features
                .then(Commands.literal("visit")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> visitShop(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                
                // Employee Management
                .then(Commands.literal("employee")
                    .then(Commands.literal("add")
                        .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.string())
                                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                                .then(Commands.argument("role", StringArgumentType.string())
                                    .suggests(TabCompletionUtil.EMPLOYEE_ROLE_SUGGESTIONS)
                                    .executes(context -> addEmployee(context.getSource(),
                                        StringArgumentType.getString(context, "shop"),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "role")))))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.string())
                                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                                .executes(context -> removeEmployee(context.getSource(),
                                    StringArgumentType.getString(context, "shop"),
                                    StringArgumentType.getString(context, "player"))))))
                    .then(Commands.literal("role")
                        .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.string())
                                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                                .then(Commands.argument("role", StringArgumentType.string())
                                    .suggests(TabCompletionUtil.EMPLOYEE_ROLE_SUGGESTIONS)
                                    .executes(context -> changeEmployeeRole(context.getSource(),
                                        StringArgumentType.getString(context, "shop"),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "role")))))))
                    .then(Commands.literal("list")
                        .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                            .executes(context -> listEmployees(context.getSource(),
                                StringArgumentType.getString(context, "shop"))))))
        );
    }
    
    private int showShopHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== NeoEssentials Shop System ===");
            MessageUtil.sendMessage(player, "§e§lShop Management:");
            MessageUtil.sendMessage(player, "§e/shop create <name> <type> §7- Create new shop (player, admin, auction)");
            MessageUtil.sendMessage(player, "§e/shop list [filter] §7- List shops (all, mine, type)");
            MessageUtil.sendMessage(player, "§e/shop info <shop> §7- Show shop information");
            MessageUtil.sendMessage(player, "§e/shop delete <shop> §7- Delete your shop");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e§lTrading:");
            MessageUtil.sendMessage(player, "§e/shop buy <shop> <item> [qty] §7- Buy items from shop");
            MessageUtil.sendMessage(player, "§e/shop sell <shop> <item> [qty] §7- Sell items to shop");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e§lShop Management (Owners):");
            MessageUtil.sendMessage(player, "§e/shop stock <shop> <item> <qty> <price> §7- Stock shop with items");
            MessageUtil.sendMessage(player, "§e/shop price <shop> <item> <buy> [sell] §7- Set item prices");
            MessageUtil.sendMessage(player, "§e/shop manage <shop> §7- Open management interface");
            MessageUtil.sendMessage(player, "§e/shop stats <shop> §7- View shop statistics");
            MessageUtil.sendMessage(player, "§e/shop visit <shop> §7- Teleport to shop");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e§lEmployee Management:");
            MessageUtil.sendMessage(player, "§e/shop employee add <shop> <player> <role> §7- Hire employee");
            MessageUtil.sendMessage(player, "§e/shop employee remove <shop> <player> §7- Fire employee");
            MessageUtil.sendMessage(player, "§e/shop employee role <shop> <player> <role> §7- Change role");
            MessageUtil.sendMessage(player, "§e/shop employee list <shop> §7- List employees");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Categories: §earmor, blocks, food, tools, weapons, magic, redstone, general");
            MessageUtil.sendMessage(player, "§7Shop Types: §ePlayer§7 (player-owned), §eServer§7 (admin), §eAuction§7 (auction house)");
            MessageUtil.sendMessage(player, "§7Employee Roles: §eOwner, Manager, Cashier, Stocker, Sales_Associate, Viewer");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int showShopStatus(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Get player info
            com.zerog.neoessentials.economy.EconomyManager economyManager2 = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            com.zerog.neoessentials.economy.Currency defaultCurrency = 
                com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
            double playerBalance = economyManager2.getBalance(player.getUUID(), defaultCurrency);
            
            List<Shop> playerShops = shopManager.getPlayerShops(player.getUUID());
            int maxShops = shopManager.getMaxShopsPerPlayer();
            double creationFee = shopManager.getShopCreationFee();
            int totalShops = shopManager.getAllShops().size();
            
            MessageUtil.sendMessage(player, "§6=== Your Shop Status ===");
            MessageUtil.sendMessage(player, "§7Player: §e" + player.getDisplayName().getString());
            MessageUtil.sendMessage(player, "§7UUID: §e" + player.getUUID());
            MessageUtil.sendMessage(player, "§7Balance: §e$" + String.format("%.2f", playerBalance));
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Your Shops: §e" + playerShops.size() + "§7/§e" + maxShops);
            MessageUtil.sendMessage(player, "§7Creation Fee: §e$" + String.format("%.2f", creationFee));
            MessageUtil.sendMessage(player, "§7Total System Shops: §e" + totalShops);
            MessageUtil.sendMessage(player, "");
            
            if (playerBalance < creationFee) {
                MessageUtil.sendMessage(player, "§c⚠ Insufficient funds to create a shop!");
                MessageUtil.sendMessage(player, "§7You need §e$" + String.format("%.2f", creationFee - playerBalance) + " §7more.");
            } else if (playerShops.size() >= maxShops) {
                MessageUtil.sendMessage(player, "§c⚠ You have reached the shop limit!");
                MessageUtil.sendMessage(player, "§7Delete a shop to create a new one.");
            } else {
                MessageUtil.sendMessage(player, "§a✓ You can create a shop!");
                MessageUtil.sendMessage(player, "§7Use: §e/shop create <name> <category> player");
            }
            
            if (!playerShops.isEmpty()) {
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§7Your shops:");
                for (Shop shop : playerShops) {
                    String status = shop.isActive() ? "§aActive" : "§cInactive";
                    MessageUtil.sendMessage(player, "  §e" + shop.getName() + " §7[" + shop.getCategory() + "] - " + status);
                }
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int createShop(CommandSourceStack source, String shopName, String category, String ownership) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Validate category
            boolean validCategory = false;
            for (String validCat : TabCompletionUtil.SHOP_TYPES) {
                if (validCat.equalsIgnoreCase(category)) {
                    validCategory = true;
                    break;
                }
            }
            if (!validCategory) {
                MessageUtil.sendErrorMessage(player, "Invalid shop category. Valid categories: " + 
                    String.join(", ", TabCompletionUtil.SHOP_TYPES));
                return 0;
            }
            
            // Parse shop ownership type to ShopType enum
            Shop.ShopType type;
            switch (ownership.toLowerCase()) {
                case "player":
                    type = Shop.ShopType.PLAYER;
                    break;
                case "server":
                case "admin":
                    type = Shop.ShopType.SERVER_SHOP;
                    // Admin shops require permission
                    if (!player.hasPermissions(4)) {
                        MessageUtil.sendErrorMessage(player, "You don't have permission to create server shops.");
                        return 0;
                    }
                    break;
                case "auction":
                    type = Shop.ShopType.AUCTION_HOUSE;
                    break;
                default:
                    MessageUtil.sendErrorMessage(player, "Invalid ownership type. Valid types: player, server, auction");
                    return 0;
            }
            
            MessageUtil.sendMessage(player, "§6Creating shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Category: §e" + category);
            MessageUtil.sendMessage(player, "§7Ownership: §e" + ownership);
            MessageUtil.sendMessage(player, "§7Type: §e" + type.name());
            MessageUtil.sendMessage(player, "§7Location: §e" + player.position());
            MessageUtil.sendMessage(player, "§7Owner: §e" + player.getDisplayName().getString());
            
            // Check shop limits and fees before creation
            List<Shop> playerShops = shopManager.getPlayerShops(player.getUUID());
            int maxShops = shopManager.getMaxShopsPerPlayer();
            double creationFee = shopManager.getShopCreationFee();
            
            MessageUtil.sendMessage(player, "§7Current shops: §e" + playerShops.size() + "§7/§e" + maxShops);
            MessageUtil.sendMessage(player, "§7Creation fee: §e$" + String.format("%.2f", creationFee));
            
            if (playerShops.size() >= maxShops) {
                MessageUtil.sendErrorMessage(player, "You have reached the maximum number of shops (" + maxShops + ").");
                return 0;
            }
            
            // Check player balance (both direct balance and bank accounts)
            com.zerog.neoessentials.economy.EconomyManager economyManager2 = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            com.zerog.neoessentials.economy.Currency defaultCurrency = 
                com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
            BankManager bankManager = economyManager.getBankManager();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured. Please contact an administrator.");
                return 0;
            }
            
            // Get direct balance
            double directBalance = economyManager2.getBalance(player.getUUID(), defaultCurrency);
            
            // Get bank account balances
            List<BankAccount> playerAccounts = bankManager.getPlayerAccounts(player.getUUID());
            double totalBankBalance = 0.0;
            
            MessageUtil.sendMessage(player, "§6Balance Summary:");
            MessageUtil.sendMessage(player, "§7Direct balance: §e$" + String.format("%.2f", directBalance));
            
            for (BankAccount account : playerAccounts) {
                double accountBalance = account.getBalance(defaultCurrency);
                totalBankBalance += accountBalance;
                MessageUtil.sendMessage(player, "§7Bank account " + account.getAccountNumber() + 
                    " (" + account.getAccountType().getDisplayName() + "): §e$" + String.format("%.2f", accountBalance));
            }
            
            double totalAvailable = directBalance + totalBankBalance;
            MessageUtil.sendMessage(player, "§7Total available: §e$" + String.format("%.2f", totalAvailable));
            
            if (totalAvailable < creationFee) {
                MessageUtil.sendErrorMessage(player, "Insufficient funds! You need $" + 
                    String.format("%.2f", creationFee) + " but only have $" + 
                    String.format("%.2f", totalAvailable) + " total (direct + bank accounts).");
                return 0;
            }
            
            // Deduct fee (if any)
            boolean paymentSuccessful = true;
            if (creationFee > 0) {
                double remainingFee = creationFee;
                
                if (directBalance >= remainingFee) {
                    // Pay entirely from direct balance
                    economyManager2.setBalance(player.getUUID(), defaultCurrency, directBalance - remainingFee);
                    MessageUtil.sendMessage(player, "§7Payment: §e$" + String.format("%.2f", remainingFee) + " deducted from direct balance");
                } else {
                    // Use direct balance first, then bank accounts
                    if (directBalance > 0) {
                        economyManager2.setBalance(player.getUUID(), defaultCurrency, 0);
                        remainingFee -= directBalance;
                        MessageUtil.sendMessage(player, "§7Payment: §e$" + String.format("%.2f", directBalance) + " deducted from direct balance");
                    }
                    
                    // Deduct remaining from bank accounts (largest balance first)
                    playerAccounts.sort((a, b) -> Double.compare(b.getBalance(defaultCurrency), a.getBalance(defaultCurrency)));
                    
                    for (BankAccount account : playerAccounts) {
                        if (remainingFee <= 0) break;
                        
                        double accountBalance = account.getBalance(defaultCurrency);
                        if (accountBalance > 0) {
                            double deduction = Math.min(accountBalance, remainingFee);
                            account.withdraw(defaultCurrency, deduction, "Shop creation fee");
                            remainingFee -= deduction;
                            MessageUtil.sendMessage(player, "§7Payment: §e$" + String.format("%.2f", deduction) + 
                                " deducted from account " + account.getAccountNumber());
                        }
                    }
                    
                    paymentSuccessful = (remainingFee <= 0.01); // Allow for small rounding errors
                }
            } else {
                MessageUtil.sendMessage(player, "§7No creation fee required.");
            }
            
            if (!paymentSuccessful) {
                MessageUtil.sendErrorMessage(player, "Payment failed. Please try again.");
                return 0;
            }
            
            // Create the shop (skip payment since we already handled it above)
            Shop shop = shopManager.createShop(player.getUUID(), shopName, shopName, category, type, true);
            if (shop != null) {
                MessageUtil.sendSuccessMessage(player, "Shop created successfully! ID: " + shop.getShopId());
                MessageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
                MessageUtil.sendMessage(player, "§7Use §e/shop manage " + shopName + " §7to configure your shop");
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to create shop. Please check server logs for details.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int listShops(CommandSourceStack source, String filter) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            MessageUtil.sendMessage(player, "§6=== Available Shops ===");
            
            if (filter == null || filter.equals("all")) {
                MessageUtil.sendMessage(player, "§7Showing all shops:");
            } else if (filter.equals("mine")) {
                MessageUtil.sendMessage(player, "§7Showing your shops:");
            } else {
                MessageUtil.sendMessage(player, "§7Showing shops of type: §e" + filter);
            }
            
            // Get shops and display them
            List<Shop> shops;
            if (filter == null || filter.equals("all")) {
                shops = shopManager.getAllShops();
                MessageUtil.sendMessage(player, "§8Debug: Total shops in system: " + shops.size());
            } else if (filter.equals("mine")) {
                shops = shopManager.getPlayerShops(player.getUUID());
                MessageUtil.sendMessage(player, "§8Debug: Your shops: " + shops.size());
            } else {
                shops = shopManager.searchShops(filter, 50);
                MessageUtil.sendMessage(player, "§8Debug: Shops matching '" + filter + "': " + shops.size());
            }
            
            if (shops.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No shops found.");
                if (filter == null || filter.equals("mine")) {
                    MessageUtil.sendMessage(player, "§8Debug: Player UUID: " + player.getUUID());
                    MessageUtil.sendMessage(player, "§8Debug: Shop creation fee: $" + 
                        String.format("%.2f", shopManager.getShopCreationFee()));
                }
                return 1;
            }
            
            // Limit to first 10 shops for readability
            for (Shop shop : shops.stream().limit(10).toList()) {
                String ownerType = shop.getShopType() == Shop.ShopType.SERVER_SHOP || 
                                  shop.getShopType() == Shop.ShopType.ADMIN ? "§cServer" : "§aPlayer";
                String status = shop.isActive() ? "§aActive" : "§cInactive";
                MessageUtil.sendMessage(player, String.format("§e%s §7[%s] §7- %s §7- %s §7- §6ID: %s", 
                    shop.getName(), shop.getCategory(), ownerType, status, shop.getShopId().toString().substring(0, 8)));
            }
            
            if (shops.size() > 10) {
                MessageUtil.sendMessage(player, "§7... and " + (shops.size() - 10) + " more shops");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int shopInfo(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Search for shop by name
            List<Shop> shops = shopManager.searchShops(shopName, 1);
            if (shops.isEmpty()) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = shops.get(0);
            
            // Display shop information
            MessageUtil.sendMessage(player, "§6=== Shop Information: §e" + shop.getShopName() + " §6===");
            MessageUtil.sendMessage(player, "§7Shop ID: §e" + shop.getShopId().toString().substring(0, 8) + "...");
            MessageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
            MessageUtil.sendMessage(player, "§7Type: §e" + shop.getShopType().getDisplayName());
            MessageUtil.sendMessage(player, "§7Status: §" + (shop.isActive() ? "aActive" : "cInactive"));
            MessageUtil.sendMessage(player, "§7Location: §e" + shop.getLocation());
            MessageUtil.sendMessage(player, "§7Items for Sale: §e" + shop.getAvailableItems().size());
            MessageUtil.sendMessage(player, "§7Total Revenue: §e$" + String.format("%.2f", shop.getTotalRevenue()));
            MessageUtil.sendMessage(player, "§7Total Sales: §e" + shop.getTotalSales());
            MessageUtil.sendMessage(player, "§7Customers: §e" + shop.getCustomerCount());
            MessageUtil.sendMessage(player, "§7Created: §e" + new java.util.Date(shop.getCreatedTime()));
            
            // Show available items and prices
            if (!shop.getAvailableItems().isEmpty()) {
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§6Available Items:");
                for (Shop.ShopItem shopItem : shop.getAvailableItems()) {
                    String itemName = shopItem.getItemName();
                    double price = shop.getItemPrice(shopItem.getItemId());
                    double sellPrice = price * 0.7; // 70% sell price
                    int stock = shopItem.getQuantity();
                    MessageUtil.sendMessage(player, String.format("§7  %s: §aBuy $%.2f §7| §eSell $%.2f §7(Stock: %d)", 
                        itemName, price, sellPrice, stock));
                }
            } else {
                MessageUtil.sendMessage(player, "§7No items currently in stock");
            }
            
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Use §e/shop buy " + shopName + " <item> [qty] §7to purchase items");
            MessageUtil.sendMessage(player, "§7Use §e/shop sell " + shopName + " <item> [qty] §7to sell items");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int deleteShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Get shop by searching for it by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 1);
            if (foundShops.isEmpty()) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = foundShops.get(0);
            
            // Verify exact name match
            if (!shop.getShopName().equalsIgnoreCase(shopName)) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            // Check ownership (players can only delete their own shops, admins can delete any)
            boolean isOwner = shop.getOwnerId() != null && shop.getOwnerId().equals(player.getUUID());
            boolean isAdmin = player.hasPermissions(2); // Op level 2 = admin
            
            if (!isOwner && !isAdmin) {
                MessageUtil.sendErrorMessage(player, "You can only delete shops you own.");
                return 0;
            }
            
            // Confirm deletion
            MessageUtil.sendMessage(player, "§6Deleting shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Owner: §e" + (shop.getOwnerId() == null ? "Server" : "Player"));
            MessageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
            
            // Delete the shop
            boolean success = shopManager.deleteShop(shop.getShopId());
            
            if (success) {
                MessageUtil.sendSuccessMessage(player, "Shop '" + shopName + "' deleted successfully!");
                
                // If it was a player shop, could potentially refund part of creation fee
                if (shop.getOwnerId() != null && isOwner) {
                    MessageUtil.sendMessage(player, "§7Shop deletion completed. Items and currency remain with owner.");
                }
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to delete shop. Please try again.");
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int buyFromShop(CommandSourceStack source, String shopName, String itemName, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            BankManager bankManager = economyManager.getBankManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            // Get shop by searching for it by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 1);
            if (foundShops.isEmpty()) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = foundShops.get(0);
            
            // Verify exact name match
            if (!shop.getShopName().equalsIgnoreCase(shopName)) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            // Get item price from shop
            double pricePerItem = shop.getItemPrice(itemName);
            if (pricePerItem <= 0) {
                MessageUtil.sendErrorMessage(player, "Item '" + itemName + "' is not available for purchase in this shop.");
                return 0;
            }
            
            double totalPrice = pricePerItem * quantity;
            
            MessageUtil.sendMessage(player, "§6Purchase Summary:");
            MessageUtil.sendMessage(player, "§7Shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Item: §e" + itemName);
            MessageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
            MessageUtil.sendMessage(player, "§7Price per item: §e" + defaultCurrency.format(pricePerItem));
            MessageUtil.sendMessage(player, "§7Total cost: §e" + defaultCurrency.format(totalPrice));
            
            // Check player balance
            double playerBalance = economyManager.getBalance(player.getUUID(), defaultCurrency);
            if (playerBalance < totalPrice) {
                MessageUtil.sendErrorMessage(player, "Insufficient funds. You need " + 
                    defaultCurrency.format(totalPrice - playerBalance) + " more.");
                return 0;
            }
            
            // Perform the actual transaction through the economy system
            try {
                // For server shops, money goes to void (just deduct from player)
                boolean success;
                
                if (shop.getOwnerId() == null) {
                    // Server shop - just deduct money from player
                    double currentBalance = economyManager.getBalance(player.getUUID(), defaultCurrency);
                    success = economyManager.setBalance(player.getUUID(), defaultCurrency, currentBalance - totalPrice);
                    
                    // Record transaction
                    Transaction transaction = new Transaction(
                        UUID.randomUUID(),       // transaction ID
                        player.getUUID(),        // from player
                        null,                    // to server (null)
                        totalPrice,              // amount
                        defaultCurrency,         // currency
                        "Shop purchase: " + quantity + "x " + itemName + " from " + shopName, // description
                        Transaction.TransactionType.PURCHASE, // type
                        System.currentTimeMillis() // timestamp
                    );
                    economyManager.getTransactionManager().recordTransaction(transaction);
                    
                } else {
                    // Player shop - transfer money to shop owner
                    success = economyManager.transferMoney(
                        player.getUUID(), 
                        shop.getOwnerId(), 
                        totalPrice, 
                        defaultCurrency, 
                        "Shop purchase: " + quantity + "x " + itemName + " from " + shopName
                    );
                }
                
                if (success) {
                    // Record the sale in the shop
                    shop.recordSale(player.getUUID(), itemName, quantity, totalPrice, System.currentTimeMillis());
                    
                    // TODO: Give items to player (requires item registry integration)
                    MessageUtil.sendSuccessMessage(player, "Purchase completed! You bought " + quantity + 
                        " " + itemName + " for " + defaultCurrency.format(totalPrice));
                    MessageUtil.sendMessage(player, "§7New balance: §e" + 
                        defaultCurrency.format(economyManager.getBalance(player.getUUID(), defaultCurrency)));
                    MessageUtil.sendMessage(player, "§c§lNote: §7Item delivery system is in development");
                } else {
                    MessageUtil.sendErrorMessage(player, "Transaction failed. Please try again.");
                }
                
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error processing shop purchase", e);
                MessageUtil.sendErrorMessage(player, "An error occurred while processing your purchase.");
                return 0;
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int sellToShop(CommandSourceStack source, String shopName, String itemName, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            // Get shop by searching for it by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 1);
            if (foundShops.isEmpty()) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = foundShops.get(0);
            
            // Verify exact name match
            if (!shop.getShopName().equalsIgnoreCase(shopName)) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            // Get sell price (typically lower than buy price)
            double buyPrice = shop.getItemPrice(itemName);
            if (buyPrice <= 0) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' doesn't buy '" + itemName + "'.");
                return 0;
            }
            
            // Sell price is typically 60-80% of buy price
            double sellPriceRatio = 0.7; // 70% of buy price
            double pricePerItem = buyPrice * sellPriceRatio;
            double totalPrice = pricePerItem * quantity;
            
            MessageUtil.sendMessage(player, "§6Sale Summary:");
            MessageUtil.sendMessage(player, "§7Shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Item: §e" + itemName);
            MessageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
            MessageUtil.sendMessage(player, "§7Price per item: §e" + defaultCurrency.format(pricePerItem));
            MessageUtil.sendMessage(player, "§7Total payment: §e" + defaultCurrency.format(totalPrice));
            
            // Simplified transaction (in real implementation, would check player inventory)
            try {
                boolean success;
                
                if (shop.getOwnerId() == null) {
                    // Server shop - money comes from void
                    double currentBalance = economyManager.getBalance(player.getUUID(), defaultCurrency);
                    success = economyManager.setBalance(player.getUUID(), defaultCurrency, currentBalance + totalPrice);
                    
                    // Record transaction
                    Transaction transaction = new Transaction(
                        UUID.randomUUID(),       // transaction ID
                        null,                    // from server (null)
                        player.getUUID(),        // to player
                        totalPrice,              // amount
                        defaultCurrency,         // currency
                        "Shop sale: " + quantity + "x " + itemName + " to " + shopName, // description
                        Transaction.TransactionType.SALE, // type
                        System.currentTimeMillis() // timestamp
                    );
                    economyManager.getTransactionManager().recordTransaction(transaction);
                    
                } else {
                    // Player shop - money comes from shop owner
                    success = economyManager.transferMoney(
                        shop.getOwnerId(),
                        player.getUUID(), 
                        totalPrice, 
                        defaultCurrency, 
                        "Shop sale: " + quantity + "x " + itemName + " to " + shopName
                    );
                }
                
                if (success) {
                    // Record the sale in the shop (this is a purchase from the shop's perspective)
                    shop.recordSale(shop.getOwnerId(), itemName, quantity, totalPrice, System.currentTimeMillis());
                    
                    MessageUtil.sendSuccessMessage(player, "Sale completed! You sold " + quantity + 
                        " " + itemName + " for " + defaultCurrency.format(totalPrice));
                    MessageUtil.sendMessage(player, "§7New balance: §e" + 
                        defaultCurrency.format(economyManager.getBalance(player.getUUID(), defaultCurrency)));
                    MessageUtil.sendMessage(player, "§c§lNote: §7Item inventory checking is simplified for development");
                } else {
                    MessageUtil.sendErrorMessage(player, "Transaction failed. Shop owner may not have sufficient funds.");
                }
                
                return success ? 1 : 0;
                
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error processing shop sale", e);
                MessageUtil.sendErrorMessage(player, "An error occurred while processing your sale.");
                return 0;
            }
            
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int stockShop(CommandSourceStack source, String shopName, String itemName, int quantity, double price) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Get shop by searching for it by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 1);
            if (foundShops.isEmpty()) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = foundShops.get(0);
            
            // Verify exact name match
            if (!shop.getShopName().equalsIgnoreCase(shopName)) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            // Check ownership (only shop owners can stock their shops)
            boolean isOwner = shop.getOwnerId() != null && shop.getOwnerId().equals(player.getUUID());
            boolean isAdmin = player.hasPermissions(2); // Admins can stock any shop
            
            if (!isOwner && !isAdmin) {
                MessageUtil.sendErrorMessage(player, "You can only stock shops you own.");
                return 0;
            }
            
            // Validate price
            if (price <= 0) {
                MessageUtil.sendErrorMessage(player, "Price must be greater than 0.");
                return 0;
            }
            
            if (quantity <= 0) {
                MessageUtil.sendErrorMessage(player, "Quantity must be greater than 0.");
                return 0;
            }
            
            // For now, we'll use a simplified item system (just item names)
            // In a full implementation, this would check the player's actual inventory
            MessageUtil.sendMessage(player, "§6Stocking Shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Item: §e" + itemName);
            MessageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
            MessageUtil.sendMessage(player, "§7Price per item: §e$" + String.format("%.2f", price));
            
            // Add item to shop inventory
            boolean success = shop.addItem(itemName, quantity, price, itemName);
            
            if (success) {
                MessageUtil.sendSuccessMessage(player, "Successfully stocked " + quantity + "x " + itemName + 
                    " in shop '" + shopName + "' for $" + String.format("%.2f", price) + " each");
                MessageUtil.sendMessage(player, "§c§lNote: §7Item inventory checking is simplified for development");
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to stock item. Shop may be inactive or price conflicts with shop type.");
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int setItemPrice(CommandSourceStack source, String shopName, String itemName, double buyPrice, double sellPrice) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6Setting Prices for: §e" + itemName);
            MessageUtil.sendMessage(player, "§7Shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Buy Price: §e$" + String.format("%.2f", buyPrice));
            if (sellPrice >= 0) {
                MessageUtil.sendMessage(player, "§7Sell Price: §e$" + String.format("%.2f", sellPrice));
            }
            MessageUtil.sendMessage(player, "§7Note: Price setting is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int manageShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6=== Shop Management: §e" + shopName + " §6===");
            MessageUtil.sendMessage(player, "§7Opening management interface...");
            MessageUtil.sendMessage(player, "§7Note: Shop management interface is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int shopStats(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6=== Shop Statistics: §e" + shopName + " §6===");
            MessageUtil.sendMessage(player, "§7Total Sales: §e$2,450.75");
            MessageUtil.sendMessage(player, "§7Items Sold: §e156");
            MessageUtil.sendMessage(player, "§7Customers: §e23");
            MessageUtil.sendMessage(player, "§7Average Sale: §e$15.71");
            MessageUtil.sendMessage(player, "§7Profit Margin: §e15.2%");
            MessageUtil.sendMessage(player, "§7Shop Rating: §e★★★★☆");
            MessageUtil.sendMessage(player, "§7Note: Shop statistics are in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int visitShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6Teleporting to shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Note: Shop teleportation is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    // Employee Management Methods
    
    private int addEmployee(CommandSourceStack source, String shopName, String playerName, String roleName) {
        try {
            ServerPlayer manager = source.getPlayerOrException();
            
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                MessageUtil.sendMessage(manager, "§cShop not found: " + shopName);
                return 0;
            }
            
            // Parse role
            ShopEmployeeManager.EmployeeRole role;
            try {
                role = ShopEmployeeManager.EmployeeRole.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                MessageUtil.sendMessage(manager, "§cInvalid role: " + roleName);
                MessageUtil.sendMessage(manager, "§7Valid roles: manager, cashier, stocker, sales_associate, viewer");
                return 0;
            }
            
            // Find player by name
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                MessageUtil.sendMessage(manager, "§cPlayer not found: " + playerName);
                return 0;
            }
            
            // Add employee
            if (shop.addEmployee(manager.getUUID(), targetPlayer.getUUID(), targetPlayer.getDisplayName().getString(), role)) {
                MessageUtil.sendMessage(manager, "§aSuccessfully added §e" + playerName + " §aas §e" + role.getDisplayName() + " §ato shop §e" + shopName);
                
                // Notify the new employee
                MessageUtil.sendMessage(targetPlayer, "§aYou have been hired as §e" + role.getDisplayName() + " §aat shop §e" + shopName);
            } else {
                MessageUtil.sendMessage(manager, "§cFailed to add employee. Check permissions and employee status.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int removeEmployee(CommandSourceStack source, String shopName, String playerName) {
        try {
            ServerPlayer manager = source.getPlayerOrException();
            
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                MessageUtil.sendMessage(manager, "§cShop not found: " + shopName);
                return 0;
            }
            
            // Find player by name
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                MessageUtil.sendMessage(manager, "§cPlayer not found: " + playerName);
                return 0;
            }
            
            // Remove employee
            if (shop.removeEmployee(manager.getUUID(), targetPlayer.getUUID())) {
                MessageUtil.sendMessage(manager, "§aSuccessfully removed §e" + playerName + " §afrom shop §e" + shopName);
                
                // Notify the removed employee
                MessageUtil.sendMessage(targetPlayer, "§cYou have been removed from shop §e" + shopName);
            } else {
                MessageUtil.sendMessage(manager, "§cFailed to remove employee. Check permissions or employee status.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int changeEmployeeRole(CommandSourceStack source, String shopName, String playerName, String roleName) {
        try {
            ServerPlayer manager = source.getPlayerOrException();
            
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                MessageUtil.sendMessage(manager, "§cShop not found: " + shopName);
                return 0;
            }
            
            // Parse role
            ShopEmployeeManager.EmployeeRole newRole;
            try {
                newRole = ShopEmployeeManager.EmployeeRole.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                MessageUtil.sendMessage(manager, "§cInvalid role: " + roleName);
                MessageUtil.sendMessage(manager, "§7Valid roles: manager, cashier, stocker, sales_associate, viewer");
                return 0;
            }
            
            // Find player by name
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                MessageUtil.sendMessage(manager, "§cPlayer not found: " + playerName);
                return 0;
            }
            
            // Change role
            if (shop.changeEmployeeRole(manager.getUUID(), targetPlayer.getUUID(), newRole)) {
                MessageUtil.sendMessage(manager, "§aSuccessfully changed §e" + playerName + "§a's role to §e" + newRole.getDisplayName() + " §ain shop §e" + shopName);
                
                // Notify the employee
                MessageUtil.sendMessage(targetPlayer, "§aYour role in shop §e" + shopName + " §ahas been changed to §e" + newRole.getDisplayName());
            } else {
                MessageUtil.sendMessage(manager, "§cFailed to change employee role. Check permissions.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int listEmployees(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                MessageUtil.sendMessage(player, "§cShop not found: " + shopName);
                return 0;
            }
            
            // Check if player has permission to view employees
            if (!shop.hasPermission(player.getUUID(), ShopEmployeeManager.ShopPermission.VIEW_BASIC_INFO)) {
                MessageUtil.sendMessage(player, "§cYou don't have permission to view employees for this shop");
                return 0;
            }
            
            List<ShopEmployeeManager.ShopEmployee> employees = shop.getActiveEmployees();
            
            MessageUtil.sendMessage(player, "§6=== Employees of §e" + shopName + " §6===");
            if (employees.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No employees found");
            } else {
                for (ShopEmployeeManager.ShopEmployee employee : employees) {
                    String roleColor = employee.getRole() == ShopEmployeeManager.EmployeeRole.OWNER ? "§c" : 
                                     employee.getRole() == ShopEmployeeManager.EmployeeRole.MANAGER ? "§6" : "§e";
                    
                    MessageUtil.sendMessage(player, "§7• " + roleColor + employee.getPlayerName() + " §7- " + 
                                          roleColor + employee.getRole().getDisplayName());
                }
            }
            MessageUtil.sendMessage(player, "§7Total employees: §e" + employees.size());
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
}
