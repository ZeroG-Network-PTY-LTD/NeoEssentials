package com.zerog.neoessentials.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.shops.ShopManager;
import com.zerog.neoessentials.economy.shops.SignShopHandler;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for managing sign shops
 */
public class SignShopCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignShopCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("signshop")
                .requires(source -> hasPermission(source, PermissionNodes.SHOP_SIGN_USE))
                .then(Commands.literal("create")
                        .requires(source -> hasPermission(source, PermissionNodes.SHOP_SIGN_CREATE))
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.argument("buy_price", DoubleArgumentType.doubleArg(0.0))
                                                .executes(ctx -> createSignShop(ctx, 0.0))
                                                .then(Commands.argument("sell_price", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(ctx -> createSignShop(ctx, 
                                                                DoubleArgumentType.getDouble(ctx, "sell_price")))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("adminshop")
                        .requires(source -> hasPermission(source, PermissionNodes.SHOP_ADMIN))
                        .then(Commands.literal("create")
                                .then(Commands.argument("item", ItemArgument.item(context))
                                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 64))
                                                .then(Commands.argument("buy_price", DoubleArgumentType.doubleArg(0.0))
                                                        .executes(ctx -> createSignShop(ctx, 0.0, true))
                                                        .then(Commands.argument("sell_price", DoubleArgumentType.doubleArg(0.0))
                                                                .executes(ctx -> createSignShop(ctx, 
                                                                        DoubleArgumentType.getDouble(ctx, "sell_price"), true))
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .requires(source -> hasPermission(source, PermissionNodes.SHOP_SIGN_ADMIN))
                        .executes(SignShopCommand::removeSignShop)
                )
                .then(Commands.literal("info")
                        .executes(SignShopCommand::getSignShopInfo)
                )
                .then(Commands.literal("list")
                        .requires(source -> hasPermission(source, PermissionNodes.SHOP_ADMIN))
                        .executes(SignShopCommand::listSignShops)
                        .then(Commands.argument("player", StringArgumentType.string())
                                .executes(SignShopCommand::listPlayerSignShops)
                        )
                )
                .then(Commands.literal("help")
                        .executes(SignShopCommand::showHelp)
                )
                .then(Commands.literal("refresh")
                        .requires(source -> hasPermission(source, PermissionNodes.SHOP_ADMIN))
                        .executes(SignShopCommand::refreshAllShopSigns)
                )
        );
    }
    
    /**
     * Create a new sign shop
     */
    private static int createSignShop(CommandContext<CommandSourceStack> context, double sellPrice, boolean isAdminShop) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can create sign shops!"));
            return 0;
        }
        
        try {
            // Get command arguments
            ItemStack item = ItemArgument.getItem(context, "item").createItemStack(1, false);
            int quantity = IntegerArgumentType.getInteger(context, "quantity");
            double buyPrice = DoubleArgumentType.getDouble(context, "buy_price");
            
            // Get the sign the player is looking at
            HitResult hitResult = player.pick(5.0, 0.0f, false);
            if (!(hitResult instanceof BlockHitResult blockHit)) {
                player.sendSystemMessage(Component.literal("§cYou must be looking at a sign!"));
                return 0;
            }
            
            BlockPos signPos = blockHit.getBlockPos();
            BlockState blockState = player.level().getBlockState(signPos);
            
            if (!(blockState.getBlock() instanceof SignBlock)) {
                player.sendSystemMessage(Component.literal("§cYou must be looking at a sign!"));
                return 0;
            }
            
            // Validate prices
            if (buyPrice <= 0 && sellPrice <= 0) {
                player.sendSystemMessage(Component.literal("§cAt least one price (buy or sell) must be greater than 0!"));
                return 0;
            }
            
            // Create the sign shop
            ShopManager shopManager = ShopManager.getInstance();
            if (shopManager == null) {
                player.sendSystemMessage(Component.literal("§cShop system is not available!"));
                LOGGER.error("ShopManager instance is null when trying to create sign shop");
                return 0;
            }
            
            SignShopHandler handler = new SignShopHandler(shopManager);
            
            boolean success = handler.createSignShop(player, signPos, item, buyPrice, sellPrice, quantity, isAdminShop);
            
            if (success) {
                String shopType = isAdminShop ? "admin shop" : "sign shop";
                player.sendSystemMessage(Component.literal("§a" + Character.toUpperCase(shopType.charAt(0)) + shopType.substring(1) + " created successfully!"));
                LOGGER.info("Player {} created {} at {} for item {}", 
                           player.getName().getString(), shopType, signPos, item.getDisplayName().getString());
                return 1;
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to create sign shop!"));
                return 0;
            }
            
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cError creating sign shop: " + e.getMessage()));
            LOGGER.error("Error creating sign shop for player {}", player.getName().getString(), e);
            return 0;
        }
    }
    
    /**
     * Create a new sign shop (backward compatibility overload)
     */
    private static int createSignShop(CommandContext<CommandSourceStack> context, double sellPrice) {
        return createSignShop(context, sellPrice, false);
    }
    
    /**
     * Remove a sign shop
     */
    private static int removeSignShop(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can remove sign shops!"));
            return 0;
        }
        
        // Get the sign the player is looking at
        HitResult hitResult = player.pick(5.0, 0.0f, false);
        if (!(hitResult instanceof BlockHitResult blockHit)) {
            player.sendSystemMessage(Component.literal("§cYou must be looking at a sign shop!"));
            return 0;
        }
        
        BlockPos signPos = blockHit.getBlockPos();
        ShopManager shopManager = ShopManager.getInstance();
        if (shopManager == null) {
            player.sendSystemMessage(Component.literal("§cShop system is not available!"));
            return 0;
        }
        
        // Find the sign shop first to check ownership
        ShopManager.SignShop signShop = shopManager.getSignShops().stream()
                .filter(shop -> shop.getSignPos().equals(signPos))
                .findFirst()
                .orElse(null);
        
        if (signShop == null) {
            player.sendSystemMessage(Component.literal("§cNo sign shop found at this location!"));
            return 0;
        }
        
        // Check if player has permission to remove this shop
        boolean canRemove = false;
        
        // Check if player is shop owner
        if (signShop.getOwnerId().equals(player.getStringUUID())) {
            canRemove = true;
        }
        // Check if player is admin and has admin permissions
        else if (hasPermission(player, PermissionNodes.SHOP_SIGN_ADMIN) || player.hasPermissions(4)) {
            canRemove = true;
        }
        // Check if it's an admin shop and player has admin permissions
        else if ("SERVER".equals(signShop.getOwnerId()) && hasPermission(player, PermissionNodes.SHOP_ADMIN)) {
            canRemove = true;
        }
        
        if (!canRemove) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to remove this shop! You can only remove your own shops."));
            return 0;
        }
        
        // Remove the sign shop
        boolean removed = shopManager.removeSignShop(signPos);
        
        if (removed) {
            String shopType = "SERVER".equals(signShop.getOwnerId()) ? "admin shop" : "sign shop";
            player.sendSystemMessage(Component.literal("§a" + Character.toUpperCase(shopType.charAt(0)) + shopType.substring(1) + " removed successfully!"));
            LOGGER.info("Player {} removed {} at {}", player.getName().getString(), shopType, signPos);
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to remove sign shop!"));
            return 0;
        }
    }
    
    /**
     * Get detailed information about a sign shop with enhanced display
     */
    private static int getSignShopInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can check sign shop info!"));
            return 0;
        }
        
        try {
            // Get the sign the player is looking at
            HitResult hitResult = player.pick(5.0, 0.0f, false);
            if (!(hitResult instanceof BlockHitResult blockHit)) {
                player.sendSystemMessage(Component.literal("§c✗ No Target!")
                    .append(Component.literal("\n§7You must be looking at a sign shop.")));
                return 0;
            }
            
            BlockPos signPos = blockHit.getBlockPos();
            ShopManager shopManager = ShopManager.getInstance();
            if (shopManager == null) {
                player.sendSystemMessage(Component.literal("§c✗ System Error!")
                    .append(Component.literal("\n§7Shop system is not available.")));
                return 0;
            }
            
            // Find the sign shop with enhanced error handling
            ShopManager.SignShop signShop = shopManager.getSignShops().stream()
                    .filter(shop -> shop.getSignPos().equals(signPos))
                    .findFirst()
                    .orElse(null);
            
            if (signShop == null) {
                player.sendSystemMessage(Component.literal("§c✗ Not a Shop!")
                    .append(Component.literal("\n§7No sign shop found at this location."))
                    .append(Component.literal("\n§7Make sure you're looking at a shop sign.")));
                return 0;
            }
            
            // Enhanced shop information display
            boolean isAdminShop = "SERVER".equals(signShop.getOwnerId());
            
            player.sendSystemMessage(Component.literal("§6╔═══════════════════════════════════════╗"));
            player.sendSystemMessage(Component.literal("§6║           §lSHOP INFORMATION§r§6           ║"));
            player.sendSystemMessage(Component.literal("§6╚═══════════════════════════════════════╝"));
            player.sendSystemMessage(Component.literal(""));
            
            // Shop type and ownership
            if (isAdminShop) {
                player.sendSystemMessage(Component.literal("§6✦ Type: §eAdmin Shop §7(Server-owned)"));
            } else {
                player.sendSystemMessage(Component.literal("§9✦ Type: §bPlayer Shop"));
                // Try to get owner name
                String ownerName = signShop.getOwnerId();
                try {
                    java.util.UUID ownerUUID = java.util.UUID.fromString(signShop.getOwnerId());
                    net.minecraft.server.MinecraftServer server = player.getServer();
                    if (server != null) {
                        net.minecraft.server.level.ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(ownerUUID);
                        if (ownerPlayer != null) {
                            ownerName = ownerPlayer.getName().getString();
                        }
                    }
                } catch (Exception e) {
                    // Keep UUID if name lookup fails
                }
                player.sendSystemMessage(Component.literal("§9✦ Owner: §f" + ownerName));
            }
            
            // Item information
            player.sendSystemMessage(Component.literal("§a✦ Item: §f" + signShop.getItem().getDisplayName().getString()));
            player.sendSystemMessage(Component.literal("§a✦ Quantity per Transaction: §f" + signShop.getQuantity()));
            
            // Stock information
            if (isAdminShop) {
                player.sendSystemMessage(Component.literal("§b✦ Stock: §e∞ §7(Unlimited)"));
            } else {
                // Check actual chest stock
                if (signShop.getChestPos() != null) {
                    try {
                        int actualStock = 0;
                        if (player.level().getBlockEntity(signShop.getChestPos()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
                            for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                                ItemStack stack = chestEntity.getItem(i);
                                if (ItemStack.isSameItem(stack, signShop.getItem())) {
                                    actualStock += stack.getCount();
                                }
                            }
                        }
                        
                        String stockStatus = actualStock >= signShop.getQuantity() ? "§a✓ In Stock" : "§c✗ Low Stock";
                        player.sendSystemMessage(Component.literal("§b✦ Stock: §f" + actualStock + " §7(" + stockStatus + "§7)"));
                    } catch (Exception e) {
                        player.sendSystemMessage(Component.literal("§b✦ Stock: §7Could not check chest"));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§b✦ Stock: §c✗ No chest connected"));
                }
            }
            
            // Pricing information
            player.sendSystemMessage(Component.literal(""));
            if (signShop.getBuyPrice() > 0) {
                String costPerItem = String.format("%.2f", signShop.getBuyPrice() / signShop.getQuantity());
                player.sendSystemMessage(Component.literal("§2▸ Buy Price: §a$" + String.format("%.2f", signShop.getBuyPrice()) + 
                    " §7($" + costPerItem + " per item)"));
            } else {
                player.sendSystemMessage(Component.literal("§2▸ Buy Price: §7Not available"));
            }
            
            if (signShop.getSellPrice() > 0) {
                String pricePerItem = String.format("%.2f", signShop.getSellPrice() / signShop.getQuantity());
                player.sendSystemMessage(Component.literal("§4▸ Sell Price: §c$" + String.format("%.2f", signShop.getSellPrice()) + 
                    " §7($" + pricePerItem + " per item)"));
            } else {
                player.sendSystemMessage(Component.literal("§4▸ Sell Price: §7Not available"));
            }
            
            // Location information
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§e✦ Location: §f" + signShop.getSignPos().toShortString()));
            if (signShop.getChestPos() != null) {
                player.sendSystemMessage(Component.literal("§e✦ Connected Chest: §f" + signShop.getChestPos().toShortString()));
            }
            
            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c✗ Error!")
                .append(Component.literal("\n§7Failed to retrieve shop information."))
                .append(Component.literal("\n§7Please try again.")));
            LOGGER.error("Error getting shop info for player {}", player.getName().getString(), e);
            return 0;
        }
    }
    
    /**
     * List all sign shops
     */
    private static int listSignShops(CommandContext<CommandSourceStack> context) {
        ShopManager shopManager = ShopManager.getInstance();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("§cShop system is not available!"));
            return 0;
        }
        
        var signShops = shopManager.getSignShops();
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== All Sign Shops ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§eTotal Sign Shops: §f" + signShops.size()), false);
        
        int count = 0;
        for (ShopManager.SignShop shop : signShops) {
            if (count >= 10) {
                final int remaining = signShops.size() - 10;
                context.getSource().sendSuccess(() -> Component.literal("§7... and " + remaining + " more"), false);
                break;
            }
            
            final String info = String.format("§e%d. §f%s §7at §f%s §7(Stock: %d)",
                    count + 1,
                    shop.getItem().getDisplayName().getString(),
                    shop.getSignPos().toShortString(),
                    shop.getStock()
            );
            
            context.getSource().sendSuccess(() -> Component.literal(info), false);
            count++;
        }
        
        return signShops.size();
    }
    
    /**
     * List sign shops for a specific player
     */
    private static int listPlayerSignShops(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ShopManager shopManager = ShopManager.getInstance();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("§cShop system is not available!"));
            return 0;
        }
        
        var playerShops = shopManager.getSignShops().stream()
                .filter(shop -> {
                    // Check both player name and UUID
                    try {
                        // Try to find player by name first
                        net.minecraft.server.MinecraftServer server = context.getSource().getServer();
                        net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                        if (player != null) {
                            return shop.getOwnerId().equals(player.getStringUUID());
                        }
                        // Fallback: check if the owner ID contains the player name
                        return shop.getOwnerId().equals(playerName);
                    } catch (Exception e) {
                        return shop.getOwnerId().equals(playerName);
                    }
                })
                .toList();
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== Sign Shops for " + playerName + " ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§eShops Found: §f" + playerShops.size()), false);
        
        int count = 0;
        for (ShopManager.SignShop shop : playerShops) {
            final String info = String.format("§e%d. §f%s §7at §f%s §7(Stock: %d)",
                    count + 1,
                    shop.getItem().getDisplayName().getString(),
                    shop.getSignPos().toShortString(),
                    shop.getStock()
            );
            
            context.getSource().sendSuccess(() -> Component.literal(info), false);
            count++;
        }
        
        return playerShops.size();
    }
    
    /**
     * Show enhanced help for sign shop commands with detailed guidance
     */
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6╔═══════════════════════════════════════╗"), false);
        context.getSource().sendSuccess(() -> Component.literal("§6║           §lSIGN SHOP SYSTEM§r§6           ║"), false);
        context.getSource().sendSuccess(() -> Component.literal("§6╚═══════════════════════════════════════╝"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("§a✦ Creating Shops:"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §e/signshop create <item> <qty> <buy_price> [sell_price]"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7└ Create a player shop (look at a sign, need nearby chest)"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7└ Example: §f/signshop create diamond 1 10.0 5.0"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("  §e/signshop adminshop create <item> <qty> <buy_price> [sell_price]"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7└ Create an admin shop (unlimited stock/funds)"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7└ Requires admin permissions"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("§b✦ Managing Shops:"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §e/signshop remove"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7└ Remove a shop (look at the sign, must be owner/admin)"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("  §e/signshop info"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7└ Get detailed information about a shop"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("  §e/signshop list [player]"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7└ List all shops or shops for a specific player"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("  §e/signshop refresh"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7└ Refresh all shop sign displays (admin only)"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("§d✦ Using Shops:"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §a▸ Left Click§7: Buy items from the shop"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §c▸ Shift + Left Click§7: Sell items to the shop"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("§f✦ Shop Types:"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §9[SHOP]§7: Player shop (limited by chest contents)"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §6[ADMIN SHOP]§7: Server shop (unlimited stock/funds)"), false);
        context.getSource().sendSuccess(() -> Component.literal(""), false);
        
        context.getSource().sendSuccess(() -> Component.literal("§e✦ Tips:"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7• Place a chest within 3 blocks of your sign"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7• Buy price must be higher than sell price"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7• Use §f/signshop info§7 to troubleshoot issues"), false);
        context.getSource().sendSuccess(() -> Component.literal("  §7• Stock depends on chest contents for player shops"), false);
        
        return 1;
    }
    
    /**
     * Check if source has permission
     */
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        return PermissionUtil.hasPermission(source, permission);
    }
    
    /**
     * Check if player has permission
     */
    private static boolean hasPermission(ServerPlayer player, String permission) {
        return PermissionUtil.hasPermission(player, permission);
    }
    
    /**
     * Refresh all shop signs to update their display colors based on current stock
     */
    private static int refreshAllShopSigns(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // Use the NEW SignShopHandler for refreshing (has proper admin shop logic)
            int refreshed = com.zerog.neoessentials.economy.shops.SignShopHandler.refreshAllShopSigns(source.getLevel());
            source.sendSuccess(() -> Component.literal("§aRefreshed " + refreshed + " shop signs with proper logic!"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError refreshing shop signs: " + e.getMessage()));
            return 0;
        }
    }
}
