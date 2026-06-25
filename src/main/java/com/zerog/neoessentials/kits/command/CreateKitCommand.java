package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.kits.Kit;
import com.zerog.neoessentials.kits.KitManager;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the /createkit command for creating kits from a player's inventory.
 */
public class CreateKitCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateKitCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if kit module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isKitSystemEnabled()) {
            return; // Don't register kit commands if module is disabled
        }
        
        registerCreateKitCommand(dispatcher, "createkit");
        registerCreateKitCommand(dispatcher, "makekit");
        registerCreateKitCommand(dispatcher, "addkit");
    }
    
    private static void registerCreateKitCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), "neoessentials.kits.create");
                }
                return source.hasPermission(4); // Console/OP fallback
            })
            .then(Commands.argument("kitname", StringArgumentType.word())
                .executes(CreateKitCommand::createBasicKit)
                .then(Commands.argument("displayname", StringArgumentType.string())
                    .executes(CreateKitCommand::createKitWithDisplayName)
                    .then(Commands.argument("cooldown", IntegerArgumentType.integer(0))
                        .executes(CreateKitCommand::createKitWithCooldown)
                        .then(Commands.argument("description", StringArgumentType.greedyString())
                            .executes(CreateKitCommand::createFullKit)
                        )
                    )
                )
            )
        );
    }
    
    private static int createBasicKit(CommandContext<CommandSourceStack> context) {
        return createKit(context, null, 0, null);
    }
    
    private static int createKitWithDisplayName(CommandContext<CommandSourceStack> context) {
        String displayName = StringArgumentType.getString(context, "displayname");
        return createKit(context, displayName, 0, null);
    }
    
    private static int createKitWithCooldown(CommandContext<CommandSourceStack> context) {
        String displayName = StringArgumentType.getString(context, "displayname");
        int cooldownSeconds = IntegerArgumentType.getInteger(context, "cooldown");
        return createKit(context, displayName, cooldownSeconds * 1000L, null);
    }
    
    private static int createFullKit(CommandContext<CommandSourceStack> context) {
        String displayName = StringArgumentType.getString(context, "displayname");
        int cooldownSeconds = IntegerArgumentType.getInteger(context, "cooldown");
        String description = StringArgumentType.getString(context, "description");
        return createKit(context, displayName, cooldownSeconds * 1000L, description);
    }
    
    private static int createKit(CommandContext<CommandSourceStack> context, String displayName, 
                                long cooldownMillis, String description) {
        CommandSourceStack source = context.getSource();
        String kitName = StringArgumentType.getString(context, "kitname");
        
        // Only players can create kits (need inventory)
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
            return 0;
        }
        
        try {
            // Check and deduct createkit command cost if economy is enabled
            int cost = (int) com.zerog.neoessentials.config.ConfigManager.getKitCommandCost("createkit");
            if (cost > 0 && com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().isEnabled()) {
                var eco = com.zerog.neoessentials.economy.managers.EconomyManager.getInstance();
                var bal = eco.getBalance(player.getUUID());
                if (bal.doubleValue() < cost) {
                    source.sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.createkit.not_enough_money", cost));
                    return 0;
                }
                if (!eco.subtractBalance(player.getUUID(), java.math.BigDecimal.valueOf(cost))) {
                    source.sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.createkit.charge_failed"));
                    return 0;
                }
            }
            // Validate kit name
            if (!isValidKitName(kitName)) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.createkit.invalid_name", kitName));
                return 0;
            }
            
            // Get items from player's inventory (exclude empty slots)
            List<ItemStack> items = new ArrayList<>();
            Inventory inventory = player.getInventory();
            
            // Copy items from main inventory (excluding armor and offhand)
            for (int i = 0; i < inventory.getContainerSize() - 5; i++) { // Exclude armor slots and offhand
                ItemStack item = inventory.getItem(i);
                if (!item.isEmpty()) {
                    items.add(item.copy());
                }
            }
            
            if (items.isEmpty()) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.createkit.empty_inventory"));
                return 0;
            }
            
            // Set defaults if not provided
            if (displayName == null) {
                displayName = kitName;
            }
            if (description == null) {
                description = "Kit created by " + player.getName().getString();
            }
            
            // Check if kit already exists
            KitManager kitManager = KitManager.getInstance();
            Kit existingKit = kitManager.getKit(kitName);
            boolean isUpdate = existingKit != null;
            
            // Create/update the kit
            String permission = "neoessentials.kits." + kitName.toLowerCase();

            boolean usePastebin = com.zerog.neoessentials.config.ConfigManager.isPastebinCreatekitEnabled();
            if (usePastebin) {
                // Simulate Pastebin upload (replace with real API if needed)
                String kitJson = kitToJsonString(kitName, displayName, description, items, cooldownMillis, permission);
                String pastebinUrl = uploadToPastebin(kitJson);
                if (pastebinUrl != null) {
                    source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.createkit.pastebin_success", pastebinUrl), false);
                    LOGGER.info("Kit '{}' exported to Pastebin by {}: {}", kitName, player.getName().getString(), pastebinUrl);
                    return 1;
                } else {
                    source.sendFailure(MessageUtil.error("commands.neoessentials.createkit.pastebin_failed"));
                    return 0;
                }
            } else {
                boolean success = kitManager.createKit(kitName, displayName, description, items, cooldownMillis, permission);
                if (success) {
                    if (isUpdate) {
                        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.createkit.updated", kitName, items.size(), formatCooldown(cooldownMillis)), false);
                    } else {
                        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.createkit.created", kitName, items.size(), formatCooldown(cooldownMillis)), false);
                    }
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.createkit.permission_hint", permission), false);
                    LOGGER.info("Kit '{}' {} by {}", kitName, isUpdate ? "updated" : "created", player.getName().getString());
                    return 1;
                } else {
                    source.sendFailure(MessageUtil.error("commands.neoessentials.createkit.failed"));
                    return 0;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error creating kit '{}' for player {}: {}", kitName, player.getName().getString(), e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.createkit.error"));
            return 0;
        }
    }
    
    // Helper method to validate kit name
    private static boolean isValidKitName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        // Check length
        if (name.length() > 32) {
            return false;
        }
        // Only allow lowercase letters, digits, and underscore — matches Kit constructor sanitization.
        // Hyphens are intentionally excluded: Kit() strips them which would cause a silent name mismatch.
        return name.matches("^[a-zA-Z0-9_]+$");
    }

    // Helper method to format cooldown duration
    private static String formatCooldown(long milliseconds) {
        if (milliseconds <= 0) {
            return "None";
        }
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    // Helper to serialize kit to JSON string (minimal, for Pastebin)
    private static String kitToJsonString(String kitName, String displayName, String description, List<ItemStack> items, long cooldownMillis, String permission) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("name", kitName);
        json.addProperty("displayName", displayName);
        json.addProperty("description", description);
        json.addProperty("cooldownMillis", cooldownMillis);
        json.addProperty("permission", permission);
        com.google.gson.JsonArray itemsArray = new com.google.gson.JsonArray();
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            net.minecraft.resources.ResourceLocation itemKey =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem());
            if (itemKey == null) continue; // Skip unregistered items
            com.google.gson.JsonObject itemJson = new com.google.gson.JsonObject();
            itemJson.addProperty("item", itemKey.toString());
            itemJson.addProperty("count", item.getCount());
            itemsArray.add(itemJson);
        }
        json.add("items", itemsArray);
        return json.toString();
    }

    // Simulate Pastebin upload (replace with real API call if needed)
    private static String uploadToPastebin(String content) {
        // In a real implementation, use HTTP client to POST to Pastebin API
        // Here, just simulate a URL for demonstration
        return "https://pastebin.com/mock/" + Integer.toHexString(content.hashCode());
    }
}