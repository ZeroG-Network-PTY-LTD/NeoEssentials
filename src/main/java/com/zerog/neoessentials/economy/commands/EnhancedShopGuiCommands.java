package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.gui.EnhancedShopInterface;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced commands for shop GUI interfaces and shop management
 */
public class EnhancedShopGuiCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main shop command - opens global shop GUI
        dispatcher.register(Commands.literal("shop")
            .requires(source -> source.isPlayer())
            .executes(EnhancedShopGuiCommands::openGlobalShop)
            .then(Commands.literal("create")
                .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                    .executes(EnhancedShopGuiCommands::createShopItem)
                    .then(Commands.argument("stock", IntegerArgumentType.integer(1))
                        .executes(EnhancedShopGuiCommands::createShopItemWithStock))))
            .then(Commands.literal("my")
                .executes(EnhancedShopGuiCommands::openPersonalShop))
            .then(Commands.literal("global")
                .executes(EnhancedShopGuiCommands::openGlobalShop))
            .then(Commands.literal("editprice")
                .then(Commands.argument("itemId", StringArgumentType.string())
                    .then(Commands.argument("newPrice", DoubleArgumentType.doubleArg(0.01))
                        .executes(EnhancedShopGuiCommands::editShopItemPrice))))
            .then(Commands.literal("addstock")
                .then(Commands.argument("itemId", StringArgumentType.string())
                    .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                        .executes(EnhancedShopGuiCommands::addShopItemStock))))
            .then(Commands.literal("remove")
                .then(Commands.argument("itemId", StringArgumentType.string())
                    .executes(EnhancedShopGuiCommands::removeShopItem))));
            
        // Alternative GUI command
        dispatcher.register(Commands.literal("shopgui")
            .requires(source -> source.isPlayer())
            .executes(EnhancedShopGuiCommands::openGlobalShop));
        
        // Personal shop command
        dispatcher.register(Commands.literal("myshop")
            .requires(source -> source.isPlayer())
            .executes(EnhancedShopGuiCommands::openPersonalShop));
        
        // Aliases
        dispatcher.register(Commands.literal("sgui").redirect(dispatcher.getRoot().getChild("shop")));
        dispatcher.register(Commands.literal("market").redirect(dispatcher.getRoot().getChild("shop")));
        dispatcher.register(Commands.literal("pshop").redirect(dispatcher.getRoot().getChild("myshop")));
    }
    
    private static int openGlobalShop(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can open the shop GUI"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            NeoEssentials.LOGGER.info("Player {} is opening global shop GUI", player.getName().getString());
            
            if (economyManager == null) {
                source.sendFailure(Component.literal("§cEconomy manager is not initialized"));
                return 0;
            }
            
            if (!economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is disabled"));
                return 0;
            }
            
            EnhancedShopInterface.openShop(player, economyManager);
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open global shop GUI", e);
            context.getSource().sendFailure(Component.literal("§cFailed to open shop interface"));
            return 0;
        }
    }
    
    private static int openPersonalShop(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can open personal shops"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            NeoEssentials.LOGGER.info("Player {} is opening personal shop GUI", player.getName().getString());
            
            if (economyManager == null) {
                source.sendFailure(Component.literal("§cEconomy manager is not initialized"));
                return 0;
            }
            
            if (!economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is disabled"));
                return 0;
            }
            
            EnhancedShopInterface.openPersonalShop(player, economyManager);
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open personal shop GUI", e);
            context.getSource().sendFailure(Component.literal("§cFailed to open personal shop interface"));
            return 0;
        }
    }
    
    private static int createShopItem(CommandContext<CommandSourceStack> context) {
        double price = DoubleArgumentType.getDouble(context, "price");
        return createShopItemInternal(context, price, 1);
    }
    
    private static int createShopItemWithStock(CommandContext<CommandSourceStack> context) {
        double price = DoubleArgumentType.getDouble(context, "price");
        int stock = IntegerArgumentType.getInteger(context, "stock");
        return createShopItemInternal(context, price, stock);
    }
    
    private static int createShopItemInternal(CommandContext<CommandSourceStack> context, double price, int stock) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can create shop items"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null) {
                source.sendFailure(Component.literal("§cEconomy manager is not initialized"));
                return 0;
            }
            
            if (!economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is disabled"));
                return 0;
            }
            
            // Check if player is holding an item
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty()) {
                source.sendFailure(Component.literal("§cYou must be holding an item to create a shop listing"));
                return 0;
            }
            
            // Validate stock doesn't exceed held amount
            if (stock > heldItem.getCount()) {
                source.sendFailure(Component.literal("§cYou don't have enough of this item. You have " + heldItem.getCount() + ", but tried to list " + stock));
                return 0;
            }
            
            // Create shop item
            ShopItem shopItem = new ShopItem.Builder()
                .id(UUID.randomUUID())
                .itemStack(heldItem.copy())
                .type(ShopItem.Type.BUY)
                .buyPrice(BigDecimal.valueOf(price))
                .currency(Currency.createBasic("coins", "Coin", "§6", "Coins")) // Default currency
                .stock(stock)
                .maxStock(stock)
                .createdBy(player.getUUID())
                .createdAt(LocalDateTime.now())
                .description("Player shop item")
                .adminItem(false)
                .build();
            
            ShopManager shopManager = economyManager.getShopManager();
            if (shopManager.addShopItem(shopItem)) {
                // Remove items from player's inventory
                heldItem.shrink(stock);
                
                source.sendSuccess(() -> Component.literal("§aSuccessfully created shop listing for " + stock + "x " + 
                    heldItem.getHoverName().getString() + " at " + price + " coins each"), false);
                
                NeoEssentials.LOGGER.info("Player {} created shop item: {}x {} for {} coins each", 
                    player.getName().getString(), stock, heldItem.getHoverName().getString(), price);
                
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to create shop listing"));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to create shop item", e);
            context.getSource().sendFailure(Component.literal("§cFailed to create shop item"));
            return 0;
        }
    }
    
    /**
     * Edits the price of an existing shop item
     */
    private static int editShopItemPrice(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can edit shop item prices"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is not available"));
                return 0;
            }
            
            String itemIdString = StringArgumentType.getString(context, "itemId");
            double newPrice = DoubleArgumentType.getDouble(context, "newPrice");
            
            // Try to parse the item ID (can be partial)
            UUID itemId = findShopItemByPartialId(economyManager.getShopManager(), player, itemIdString);
            if (itemId == null) {
                source.sendFailure(Component.literal("§cShop item not found or not owned by you"));
                return 0;
            }
            
            Optional<ShopItem> optionalItem = economyManager.getShopManager().getShopItem(itemId);
            if (optionalItem.isEmpty()) {
                source.sendFailure(Component.literal("§cShop item not found"));
                return 0;
            }
            
            ShopItem shopItem = optionalItem.get();
            
            // Verify player owns this item
            if (!player.getUUID().equals(shopItem.getCreatedBy())) {
                source.sendFailure(Component.literal("§cYou can only edit your own shop items"));
                return 0;
            }
            
            // Create updated shop item with new price
            ShopItem updatedItem = new ShopItem.Builder()
                .id(shopItem.getId())
                .itemStack(shopItem.getItemStack())
                .type(shopItem.getType())
                .buyPrice(BigDecimal.valueOf(newPrice))
                .sellPrice(shopItem.getSellPrice())
                .currency(shopItem.getCurrency())
                .stock(shopItem.getStock())
                .maxStock(shopItem.getMaxStock())
                .createdBy(shopItem.getCreatedBy())
                .createdAt(shopItem.getCreatedAt())
                .description(shopItem.getDescription())
                .adminItem(shopItem.isAdminItem())
                .build();
            
            if (economyManager.getShopManager().addShopItem(updatedItem)) {
                source.sendSuccess(() -> Component.literal("§aUpdated price of " + 
                    shopItem.getItemStack().getHoverName().getString() + " to " + newPrice + " coins"), false);
                
                NeoEssentials.LOGGER.info("Player {} updated shop item price: {} to {} coins", 
                    player.getName().getString(), shopItem.getItemStack().getHoverName().getString(), newPrice);
                
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to update shop item price"));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to edit shop item price", e);
            context.getSource().sendFailure(Component.literal("§cFailed to edit shop item price"));
            return 0;
        }
    }
    
    /**
     * Adds stock to an existing shop item
     */
    private static int addShopItemStock(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can add stock to shop items"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is not available"));
                return 0;
            }
            
            String itemIdString = StringArgumentType.getString(context, "itemId");
            int quantity = IntegerArgumentType.getInteger(context, "quantity");
            
            // Try to parse the item ID (can be partial)
            UUID itemId = findShopItemByPartialId(economyManager.getShopManager(), player, itemIdString);
            if (itemId == null) {
                source.sendFailure(Component.literal("§cShop item not found or not owned by you"));
                return 0;
            }
            
            Optional<ShopItem> optionalItem = economyManager.getShopManager().getShopItem(itemId);
            if (optionalItem.isEmpty()) {
                source.sendFailure(Component.literal("§cShop item not found"));
                return 0;
            }
            
            ShopItem shopItem = optionalItem.get();
            
            // Verify player owns this item
            if (!player.getUUID().equals(shopItem.getCreatedBy())) {
                source.sendFailure(Component.literal("§cYou can only manage your own shop items"));
                return 0;
            }
            
            // Check if player is holding matching items
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty() || !ItemStack.isSameItem(heldItem, shopItem.getItemStack())) {
                source.sendFailure(Component.literal("§cYou must be holding the same item type to add stock"));
                return 0;
            }
            
            // Validate quantity doesn't exceed held amount
            if (quantity > heldItem.getCount()) {
                source.sendFailure(Component.literal("§cYou don't have enough items. You have " + 
                    heldItem.getCount() + ", but tried to add " + quantity));
                return 0;
            }
            
            // Create updated shop item with additional stock
            ShopItem updatedItem = shopItem.withStock(shopItem.getStock() + quantity);
            
            if (economyManager.getShopManager().addShopItem(updatedItem)) {
                // Remove items from player's inventory
                heldItem.shrink(quantity);
                
                source.sendSuccess(() -> Component.literal("§aAdded " + quantity + " items to " + 
                    shopItem.getItemStack().getHoverName().getString() + " (new stock: " + 
                    (shopItem.getStock() + quantity) + ")"), false);
                
                NeoEssentials.LOGGER.info("Player {} added {} stock to shop item: {}", 
                    player.getName().getString(), quantity, shopItem.getItemStack().getHoverName().getString());
                
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to add stock to shop item"));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to add shop item stock", e);
            context.getSource().sendFailure(Component.literal("§cFailed to add shop item stock"));
            return 0;
        }
    }
    
    /**
     * Removes a shop item listing
     */
    private static int removeShopItem(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can remove shop items"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is not available"));
                return 0;
            }
            
            String itemIdString = StringArgumentType.getString(context, "itemId");
            
            // Try to parse the item ID (can be partial)
            UUID itemId = findShopItemByPartialId(economyManager.getShopManager(), player, itemIdString);
            if (itemId == null) {
                source.sendFailure(Component.literal("§cShop item not found or not owned by you"));
                return 0;
            }
            
            Optional<ShopItem> optionalItem = economyManager.getShopManager().getShopItem(itemId);
            if (optionalItem.isEmpty()) {
                source.sendFailure(Component.literal("§cShop item not found"));
                return 0;
            }
            
            ShopItem shopItem = optionalItem.get();
            
            // Verify player owns this item
            if (!player.getUUID().equals(shopItem.getCreatedBy())) {
                source.sendFailure(Component.literal("§cYou can only remove your own shop items"));
                return 0;
            }
            
            if (economyManager.getShopManager().removeShopItem(itemId)) {
                // Return any remaining stock to player
                if (shopItem.getStock() > 0) {
                    ItemStack returnItems = shopItem.getItemStack().copy();
                    returnItems.setCount(shopItem.getStock());
                    
                    if (player.getInventory().add(returnItems)) {
                        source.sendSuccess(() -> Component.literal("§aShop listing removed and " + 
                            shopItem.getStock() + " items returned to your inventory"), false);
                    } else {
                        // If inventory is full, drop items
                        player.drop(returnItems, false);
                        source.sendSuccess(() -> Component.literal("§aShop listing removed and " + 
                            shopItem.getStock() + " items dropped (inventory full)"), false);
                    }
                } else {
                    source.sendSuccess(() -> Component.literal("§aShop listing removed successfully"), false);
                }
                
                NeoEssentials.LOGGER.info("Player {} removed shop listing for item {}", 
                    player.getName().getString(), shopItem.getItemStack().getHoverName().getString());
                
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to remove shop listing"));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to remove shop item", e);
            context.getSource().sendFailure(Component.literal("§cFailed to remove shop item"));
            return 0;
        }
    }
    
    /**
     * Helper method to find a shop item by partial ID
     */
    private static UUID findShopItemByPartialId(ShopManager shopManager, ServerPlayer player, String partialId) {
        try {
            // First try to parse as full UUID
            try {
                return UUID.fromString(partialId);
            } catch (IllegalArgumentException ignored) {
                // Not a full UUID, try partial matching
            }
            
            // Search through player's shop items for partial ID match
            return shopManager.getAllItems().stream()
                .filter(item -> player.getUUID().equals(item.getCreatedBy()))
                .filter(item -> item.getId().toString().startsWith(partialId.toLowerCase()))
                .findFirst()
                .map(ShopItem::getId)
                .orElse(null);
                
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error finding shop item by partial ID", e);
            return null;
        }
    }
}
