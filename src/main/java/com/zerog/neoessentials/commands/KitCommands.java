package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles all kit-related commands
 */
public class KitCommands {
    
    /**
     * Register all kit-related commands
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NeoEssentials.LOGGER.info("Registering kit commands");
        
        // /kit <n> - Claim a kit
        dispatcher.register(
            Commands.literal("kit")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.kit"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeKit)
                )
                .executes(this::executeKitList)
        );
        
        // /kits - List all available kits
        dispatcher.register(
            Commands.literal("kits")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.kit.list"))
                .executes(this::executeKitList)
        );
          // /createkit <name> [cooldown] [price] - Create a kit with your current inventory
        dispatcher.register(
            Commands.literal("createkit")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.kit.create"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("cooldown", LongArgumentType.longArg(0))
                        .then(Commands.argument("price", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0))
                            .executes(context -> {
                                long cooldown = LongArgumentType.getLong(context, "cooldown");
                                double price = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "price");
                                return executeCreateKitWithPrice(context, cooldown, price);
                            })
                        )
                        .executes(this::executeCreateKit)
                    )
                    .executes(context -> executeCreateKit(context, 0)) // Default cooldown of 0
                )
        );
        
        // /deletekit <n> - Delete a kit
        dispatcher.register(
            Commands.literal("deletekit")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.kit.delete"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeDeleteKit)
                )
        );
        
        // /givekit <player> <kit> - Give a kit to another player
        dispatcher.register(
            Commands.literal("givekit")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.kit.give"))
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                    .then(Commands.argument("kit", StringArgumentType.word())
                        .executes(this::executeGiveKit)
                    )
                )
        );
        
        // /previewkit <name> - Preview the items in a kit
        dispatcher.register(
            Commands.literal("previewkit")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.kit.preview"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executePreviewKit)
                )
        );
        
        NeoEssentials.LOGGER.info("Kit commands registered successfully");
    }
    
    /**
     * Execute the /kit command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to claim kit '{}'", player.getScoreboardName(), kitName);
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        if (kitManager == null) {
            NeoEssentials.LOGGER.error("KitManager is null when executing /kit command");
            context.getSource().sendFailure(Component.literal("Kit system is not available"));
            return 0;
        }
        
        KitManager.Kit kit = kitManager.getKit(kitName);
        
        if (kit == null) {
            NeoEssentials.LOGGER.debug("Kit '{}' not found for player {}", kitName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' not found"));
            return 0;
        }
          // Check if player can use kit (permissions)
        if (!kitManager.canUseKit(player, kitName, false)) {
            long cooldown = kitManager.getRemainingCooldown(player, kitName);
            
            if (cooldown > 0) {
                String timeStr = formatTime(cooldown);
                NeoEssentials.LOGGER.debug("Player {} must wait {} before using kit '{}'", 
                    player.getScoreboardName(), timeStr, kitName);
                context.getSource().sendFailure(Component.literal("You must wait " + timeStr + " before using this kit again"));
            } else {
                NeoEssentials.LOGGER.debug("Player {} doesn't have permission for kit '{}'", 
                    player.getScoreboardName(), kitName);
                context.getSource().sendFailure(Component.literal("You don't have permission to use this kit"));
            }
            
            return 0;
        }
        
        // Check if player has enough money for the kit
        if (kit.getPrice() > 0) {
            var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
            if (economyManager != null) {
                double balance = economyManager.getBalance(player.getUUID());
                if (balance < kit.getPrice()) {
                    String formattedPrice = economyManager.formatCurrency(kit.getPrice());
                    String formattedBalance = economyManager.formatCurrency(balance);
                    NeoEssentials.LOGGER.debug("Player {} doesn't have enough money for kit '{}' (has {}, needs {})",
                        player.getScoreboardName(), kitName, formattedBalance, formattedPrice);
                    context.getSource().sendFailure(Component.literal("You need " + formattedPrice + 
                        " to purchase this kit (you have " + formattedBalance + ")"));
                    return 0;
                }
            }
        }
        
        // Give the kit to the player
        boolean success = kitManager.giveKit(player, kitName);
        
        if (success) {
            NeoEssentials.LOGGER.info("Player {} claimed kit '{}'", player.getScoreboardName(), kitName);
            MutableComponent message = Component.literal("You received kit '" + kitName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
            NeoEssentials.LOGGER.error("Failed to give kit '{}' to player {}", kitName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Failed to give kit '" + kitName + "'"));
            return 0;
        }
    }
    
    /**
     * Execute the /kits command to list all available kits
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeKitList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        NeoEssentials.LOGGER.debug("Player {} is requesting kit list", player.getScoreboardName());
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        if (kitManager == null) {
            NeoEssentials.LOGGER.error("KitManager is null when executing /kits command");
            context.getSource().sendFailure(Component.literal("Kit system is not available"));
            return 0;
        }
        
        Map<String, KitManager.Kit> kits = kitManager.getAllKits();
        
        if (kits.isEmpty()) {
            NeoEssentials.LOGGER.debug("No kits found for player {}", player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("No kits available"));
            return 0;
        }
        
        MutableComponent message = Component.literal("Available kits: ");
        
        boolean first = true;
        for (String kitName : kits.keySet()) {
            if (!first) {
                message.append(Component.literal(", "));
            }                // Get the kit
                KitManager.Kit kit = kitManager.getKit(kitName);
                
                // Check if the player can use this kit (permissions)
                if (kitManager.canUseKit(player, kitName)) {
                    // Check cooldown
                    long cooldown = kitManager.getRemainingCooldown(player, kitName);
                    
                    // Check for price
                    boolean canAfford = true;
                    String priceInfo = "";
                    
                    if (kit.getPrice() > 0) {
                        var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
                        double balance = economyManager.getBalance(player.getUUID());
                        canAfford = balance >= kit.getPrice();
                        priceInfo = " (" + economyManager.formatCurrency(kit.getPrice()) + ")";
                    }
                    
                    if (cooldown > 0) {
                        // On cooldown - show in red with cooldown time
                        String timeStr = formatTime(cooldown);
                        message.append(Component.literal(kitName + " (" + timeStr + ")" + priceInfo).withStyle(net.minecraft.ChatFormatting.RED));
                    } else if (!canAfford) {
                        // Can't afford - show in yellow
                        message.append(Component.literal(kitName + priceInfo).withStyle(net.minecraft.ChatFormatting.YELLOW));
                    } else {
                        // Available - show in green
                        message.append(Component.literal(kitName + priceInfo).withStyle(net.minecraft.ChatFormatting.GREEN));
                    }
                } else {
                    // No permission - show in gray
                    message.append(Component.literal(kitName).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            
            first = false;
        }
        
        NeoEssentials.LOGGER.debug("Sending kit list ({} kits) to player {}", kits.size(), player.getScoreboardName());
        MessageUtil.sendInfo(player, message);
        return 1;
    }
    
    /**
     * Format a time in seconds to a human-readable string
     * 
     * @param seconds The time in seconds
     * @return A formatted time string
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            
            if (remainingSeconds == 0) {
                return minutes + "m";
            } else {
                return minutes + "m " + remainingSeconds + "s";
            }
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            
            if (remainingMinutes == 0) {
                return hours + "h";
            } else {
                return hours + "h " + remainingMinutes + "m";
            }
        }
    }
    
    /**
     * Execute the /createkit command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeCreateKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCreateKit(context, LongArgumentType.getLong(context, "cooldown"));
    }
    
    /**
     * Execute the /createkit command with a specified cooldown
     * 
     * @param context The command context
     * @param cooldown The cooldown in seconds
     * @return Command result
     */
    private int executeCreateKit(CommandContext<CommandSourceStack> context, long cooldown) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to create kit '{}' with cooldown {}s", 
            player.getScoreboardName(), kitName, cooldown);
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        if (kitManager == null) {
            NeoEssentials.LOGGER.error("KitManager is null when executing /createkit command");
            context.getSource().sendFailure(Component.literal("Kit system is not available"));
            return 0;
        }
        
        // Check if kit already exists
        if (kitManager.getKit(kitName) != null) {
            NeoEssentials.LOGGER.debug("Kit '{}' already exists, cannot be created by {}", 
                kitName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' already exists. Delete it first if you want to replace it."));
            return 0;
        }
          // Get all items from the player's inventory
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().items) {
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }
        
        // Also include armor and offhand items
        for (ItemStack item : player.getInventory().armor) {
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }
        
        if (!player.getInventory().offhand.get(0).isEmpty()) {
            items.add(player.getInventory().offhand.get(0).copy());
        }
        
        if (items.isEmpty()) {
            NeoEssentials.LOGGER.debug("Player {} has empty inventory, cannot create kit", player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Your inventory is empty. Cannot create an empty kit."));
            return 0;
        }
          // Create the kit with appropriate permission node
        String permission = "neoessentials.command.kit." + kitName.toLowerCase();
        KitManager.Kit kit = kitManager.createKit(kitName, cooldown, permission, items);
        
        NeoEssentials.LOGGER.info("Player {} created kit '{}' with {} items and {}s cooldown", 
            player.getScoreboardName(), kitName, items.size(), cooldown);
        
        MutableComponent message = Component.literal("Created kit '" + kitName + "' with " + items.size() + " items");
        if (cooldown > 0) {
            message.append(Component.literal(" and a cooldown of " + formatTime(cooldown)));
        }
        
        MessageUtil.sendSuccess(player, message);
        return 1;
    }
    
    /**
     * Execute the /deletekit command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeDeleteKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to delete kit '{}'", 
            player.getScoreboardName(), kitName);
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        if (kitManager == null) {
            NeoEssentials.LOGGER.error("KitManager is null when executing /deletekit command");
            context.getSource().sendFailure(Component.literal("Kit system is not available"));
            return 0;
        }
        
        boolean success = kitManager.deleteKit(kitName);
        
        if (success) {
            NeoEssentials.LOGGER.info("Player {} deleted kit '{}'", player.getScoreboardName(), kitName);
            MutableComponent message = Component.literal("Deleted kit '" + kitName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
            NeoEssentials.LOGGER.debug("Kit '{}' not found for deletion by {}", kitName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' not found"));
            return 0;
        }
    }
      /**
     * Execute the /givekit command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeGiveKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
        String kitName = StringArgumentType.getString(context, "kit");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to give kit '{}' to player {}", 
            player.getScoreboardName(), kitName, target.getScoreboardName());
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        if (kitManager == null) {
            NeoEssentials.LOGGER.error("KitManager is null when executing /givekit command");
            context.getSource().sendFailure(Component.literal("Kit system is not available"));
            return 0;
        }
        
        KitManager.Kit kit = kitManager.getKit(kitName);
        
        if (kit == null) {
            NeoEssentials.LOGGER.debug("Kit '{}' not found for /givekit by {}", kitName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' not found"));
            return 0;
        }
        
        // Force give the kit to the target player by bypassing permission and cooldown checks
        try {
            boolean success = forceGiveKit(target, kitManager, kitName);
            
            if (success) {
                NeoEssentials.LOGGER.info("Player {} gave kit '{}' to player {}", 
                    player.getScoreboardName(), kitName, target.getScoreboardName());
                    
                MutableComponent messageToAdmin = Component.literal("Gave kit '" + kitName + "' to " + target.getScoreboardName());
                MessageUtil.sendSuccess(player, messageToAdmin);
                
                MutableComponent messageToTarget = Component.literal("You received kit '" + kitName + "' from " + player.getScoreboardName());
                MessageUtil.sendInfo(target, messageToTarget);
                
                return 1;
            } else {
                NeoEssentials.LOGGER.error("Failed to give kit '{}' to player {} by {}", 
                    kitName, target.getScoreboardName(), player.getScoreboardName());
                    
                context.getSource().sendFailure(Component.literal("Failed to give kit '" + kitName + "' to " + target.getScoreboardName()));
                return 0;
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error giving kit '{}' to player {}: {}", 
                kitName, target.getScoreboardName(), e.getMessage());
            context.getSource().sendFailure(Component.literal("Error giving kit: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Force give a kit to a player, bypassing permission and cooldown checks
     * 
     * @param player The target player
     * @param kitManager The kit manager
     * @param kitName The name of the kit
     * @return True if successful, false otherwise
     */
    private boolean forceGiveKit(ServerPlayer player, KitManager kitManager, String kitName) {
        KitManager.Kit kit = kitManager.getKit(kitName);
        
        if (kit == null || player == null) {
            return false;
        }
        
        // Create and give items to the player directly, bypassing permission and cooldown checks
        for (KitManager.ItemDefinition itemDef : kit.getItemDefinitions()) {
            try {
                // Try to get the item from its ID
                net.minecraft.resources.ResourceLocation resourceLocation = 
                    net.minecraft.resources.ResourceLocation.tryParse(itemDef.getItemId());
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resourceLocation);
                
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(item, itemDef.getCount());
                    
                    // Give item to player
                    if (!player.getInventory().add(itemStack)) {
                        // If inventory is full, drop the item
                        player.drop(itemStack, false);
                    }
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error giving item from kit: {}", e.getMessage());
            }
        }
        
        return true;
    }
    
    /**
     * Execute the /createkit command with a specified cooldown and price
     * 
     * @param context The command context
     * @param cooldown The cooldown in seconds
     * @param price The price of the kit
     * @return Command result
     */
    private int executeCreateKitWithPrice(CommandContext<CommandSourceStack> context, long cooldown, double price) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to create kit '{}' with cooldown {}s and price {}", 
            player.getScoreboardName(), kitName, cooldown, price);
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        if (kitManager == null) {
            NeoEssentials.LOGGER.error("KitManager is null when executing /createkit command");
            context.getSource().sendFailure(Component.literal("Kit system is not available"));
            return 0;
        }
        
        // Check if kit already exists
        if (kitManager.getKit(kitName) != null) {
            NeoEssentials.LOGGER.debug("Kit '{}' already exists, cannot be created by {}", 
                kitName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' already exists. Delete it first if you want to replace it."));
            return 0;
        }
        
        // Get all items from the player's inventory
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().items) {
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }
        
        // Also include armor and offhand items
        for (ItemStack item : player.getInventory().armor) {
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }
        
        if (!player.getInventory().offhand.get(0).isEmpty()) {
            items.add(player.getInventory().offhand.get(0).copy());
        }
        
        if (items.isEmpty()) {
            NeoEssentials.LOGGER.debug("Player {} has empty inventory, cannot create kit", player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Your inventory is empty. Cannot create an empty kit."));
            return 0;
        }
        
        // Create the kit with appropriate permission node and price
        String permission = "neoessentials.command.kit." + kitName.toLowerCase();
        KitManager.Kit kit = kitManager.createKit(kitName, cooldown, permission, price, items);
        
        NeoEssentials.LOGGER.info("Player {} created kit '{}' with {} items, {}s cooldown, and price {}", 
            player.getScoreboardName(), kitName, items.size(), cooldown, price);
        
        MutableComponent message = Component.literal("Created kit '" + kitName + "' with " + items.size() + " items");
        
        if (cooldown > 0) {
            message.append(Component.literal(", " + formatTime(cooldown) + " cooldown"));
        }
        
        if (price > 0) {
            String formattedPrice = NeoEssentials.getInstance().getDataManager().getEconomyManager().formatCurrency(price);
            message.append(Component.literal(", price: " + formattedPrice));
        }
        
        MessageUtil.sendSuccess(player, message);
        return 1;
    }
    
    /**
     * Execute the /previewkit command to preview a kit
     * 
     * @param context The command context
     * @return Command result
     */
    private int executePreviewKit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String kitName = StringArgumentType.getString(context, "name");
        
        NeoEssentials.LOGGER.debug("Player {} is previewing kit '{}'", player.getScoreboardName(), kitName);
        
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        if (kitManager == null) {
            NeoEssentials.LOGGER.error("KitManager is null when executing /previewkit command");
            context.getSource().sendFailure(Component.literal("Kit system is not available"));
            return 0;
        }
        
        KitManager.Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            NeoEssentials.LOGGER.debug("Kit '{}' not found for player {}", kitName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Kit '" + kitName + "' not found"));
            return 0;
        }
        
        // Send kit header
        MutableComponent header = Component.literal("§6§l=== Kit: §r§e" + kit.getName() + "§6§l ===");
        player.sendSystemMessage(header);
        
        // Display kit info (cooldown, permission, price)
        StringBuilder infoBuilder = new StringBuilder("§7Info: ");
        
        if (kit.getCooldown() > 0) {
            infoBuilder.append("§eCooldown: §f").append(formatTime(kit.getCooldown()));
        }
        
        if (kit.getPrice() > 0) {
            if (kit.getCooldown() > 0) infoBuilder.append("§7, ");
            var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
            String formattedPrice = economyManager.formatCurrency(kit.getPrice());
            double balance = economyManager.getBalance(player.getUUID());
            boolean canAfford = balance >= kit.getPrice();
            
            infoBuilder.append("§ePrice: ").append(canAfford ? "§a" : "§c").append(formattedPrice);
        }
        
        player.sendSystemMessage(Component.literal(infoBuilder.toString()));
        
        // Display items
        player.sendSystemMessage(Component.literal("§7Items:"));
        List<KitManager.ItemDefinition> itemDefs = kit.getItemDefinitions();
        
        if (itemDefs.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo items in this kit"));
        } else {
            // Calculate how many "special" slots are used (armor, offhand)
            int specialSlots = 0;
            for (KitManager.ItemDefinition itemDef : itemDefs) {
                String itemId = itemDef.getItemId();
                if (itemId.contains("helmet") || itemId.contains("chestplate") || 
                    itemId.contains("leggings") || itemId.contains("boots") ||
                    itemId.contains("shield") || itemId.endsWith("_head")) {
                    specialSlots++;
                }
            }
            
            player.sendSystemMessage(Component.literal("§7Total items: §f" + itemDefs.size() + 
                " §7(approximately §f" + (itemDefs.size() - specialSlots) + " §7inventory slots)"));
            
            // List some of the notable items
            int displayLimit = 10;
            int displayed = 0;
            
            for (KitManager.ItemDefinition itemDef : itemDefs) {
                if (displayed >= displayLimit) {
                    int remaining = itemDefs.size() - displayed;
                    if (remaining > 0) {
                        player.sendSystemMessage(Component.literal("§7...and §f" + remaining + " §7more items"));
                    }
                    break;
                }
                
                try {
                    String itemId = itemDef.getItemId();
                    ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
                    Item item = BuiltInRegistries.ITEM.get(resourceLocation);
                    
                    if (item != null && item != Items.AIR) {
                        String itemName = item.getDescription().getString();
                        player.sendSystemMessage(Component.literal("§8- §f" + itemDef.getCount() + "x §e" + itemName));
                        displayed++;
                    }
                } catch (Exception e) {
                    // Skip problematic items
                }
            }
        }
        
        // Display footer
        if (kitManager.canUseKit(player, kitName, true)) {
            player.sendSystemMessage(Component.literal("§aYou can use this kit. Type §e/kit " + kitName + "§a to claim it."));
        } else {
            long cooldown = kitManager.getRemainingCooldown(player, kitName);
            if (cooldown > 0) {
                String timeStr = formatTime(cooldown);
                player.sendSystemMessage(Component.literal("§cYou must wait §e" + timeStr + "§c before using this kit again."));
            } else if (kit.getPrice() > 0) {
                var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
                if (economyManager.getBalance(player.getUUID()) < kit.getPrice()) {
                    String formattedPrice = economyManager.formatCurrency(kit.getPrice());
                    player.sendSystemMessage(Component.literal("§cYou don't have enough money to buy this kit. Price: §e" + formattedPrice));
                } else {
                    player.sendSystemMessage(Component.literal("§cYou don't have permission to use this kit."));
                }
            } else {
                player.sendSystemMessage(Component.literal("§cYou don't have permission to use this kit."));
            }
        }
        
        return 1;
    }
}
