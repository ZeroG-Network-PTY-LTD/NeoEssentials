package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.data.Kit;
import com.zerog.neoessentials.data.KitItem;
import com.zerog.neoessentials.managers.KitManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.localization.LanguageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kit administration commands for NeoEssentials
 * Provides in-game kit creation, editing, and management with live JSON updates
 * 
 * Commands:
 * - /kitadmin create <name> [displayName] - Create a new kit
 * - /kitadmin delete <name> - Delete a kit
 * - /kitadmin edit <name> - Edit kit properties
 * - /kitadmin list - List all kits
 * - /kitadmin info <name> - Show kit information
 * - /kitadmin items <name> add|remove|clear - Manage kit items
 * - /kitadmin reload - Reload kits from storage
 * - /kitadmin save - Force save all kits
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class KitAdminCommand {
    
    private static final SuggestionProvider<CommandSourceStack> KIT_SUGGESTIONS = (context, builder) -> {
        KitManager kitManager = KitManager.getInstance();
        List<String> kitNames = kitManager.getAllKits().stream()
                .map(Kit::getName)
                .collect(Collectors.toList());
        return SharedSuggestionProvider.suggest(kitNames, builder);
    };
    
    private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggestResource(
            BuiltInRegistries.ITEM.keySet(), builder);
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kitadmin")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.admin.kit"))
            
            // /kitadmin create <name> [displayName] [description]
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> createKit(ctx, StringArgumentType.getString(ctx, "name"), null, null))
                    .then(Commands.argument("displayName", StringArgumentType.greedyString())
                        .executes(ctx -> createKit(ctx, StringArgumentType.getString(ctx, "name"), 
                                StringArgumentType.getString(ctx, "displayName"), null)))))
            
            // /kitadmin delete <name>
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(KIT_SUGGESTIONS)
                    .executes(ctx -> deleteKit(ctx, StringArgumentType.getString(ctx, "name")))))
            
            // /kitadmin list
            .then(Commands.literal("list")
                .executes(KitAdminCommand::listKits))
            
            // /kitadmin info <name>
            .then(Commands.literal("info")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(KIT_SUGGESTIONS)
                    .executes(ctx -> showKitInfo(ctx, StringArgumentType.getString(ctx, "name")))))
            
            // /kitadmin edit <name> <property> <value>
            .then(Commands.literal("edit")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(KIT_SUGGESTIONS)
                    .then(Commands.literal("displayName")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(ctx -> editKitProperty(ctx, "displayName", StringArgumentType.getString(ctx, "name"), 
                                    StringArgumentType.getString(ctx, "value")))))
                    .then(Commands.literal("description")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(ctx -> editKitProperty(ctx, "description", StringArgumentType.getString(ctx, "name"), 
                                    StringArgumentType.getString(ctx, "value")))))
                    .then(Commands.literal("cooldown")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                            .executes(ctx -> editKitProperty(ctx, "cooldown", StringArgumentType.getString(ctx, "name"), 
                                    String.valueOf(IntegerArgumentType.getInteger(ctx, "seconds"))))))
                    .then(Commands.literal("cost")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                            .executes(ctx -> editKitProperty(ctx, "cost", StringArgumentType.getString(ctx, "name"), 
                                    String.valueOf(DoubleArgumentType.getDouble(ctx, "amount"))))))
                    .then(Commands.literal("permission")
                        .then(Commands.argument("permission", StringArgumentType.greedyString())
                            .executes(ctx -> editKitProperty(ctx, "permission", StringArgumentType.getString(ctx, "name"), 
                                    StringArgumentType.getString(ctx, "permission")))))
                    .then(Commands.literal("enabled")
                        .then(Commands.literal("true")
                            .executes(ctx -> editKitProperty(ctx, "enabled", StringArgumentType.getString(ctx, "name"), "true")))
                        .then(Commands.literal("false")
                            .executes(ctx -> editKitProperty(ctx, "enabled", StringArgumentType.getString(ctx, "name"), "false"))))
                    .then(Commands.literal("oneTimeOnly")
                        .then(Commands.literal("true")
                            .executes(ctx -> editKitProperty(ctx, "oneTimeOnly", StringArgumentType.getString(ctx, "name"), "true")))
                        .then(Commands.literal("false")
                            .executes(ctx -> editKitProperty(ctx, "oneTimeOnly", StringArgumentType.getString(ctx, "name"), "false"))))))
            
            // /kitadmin items <name> <action>
            .then(Commands.literal("items")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(KIT_SUGGESTIONS)
                    .then(Commands.literal("add")
                        .executes(ctx -> addItemFromInventory(ctx, StringArgumentType.getString(ctx, "name")))
                        .then(Commands.argument("item", StringArgumentType.word())
                            .suggests(ITEM_SUGGESTIONS)
                            .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> addKitItem(ctx, StringArgumentType.getString(ctx, "name"), 
                                        StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "count"))))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("item", StringArgumentType.word())
                            .suggests(ITEM_SUGGESTIONS)
                            .executes(ctx -> removeKitItem(ctx, StringArgumentType.getString(ctx, "name"), 
                                    StringArgumentType.getString(ctx, "item")))))
                    .then(Commands.literal("clear")
                        .executes(ctx -> clearKitItems(ctx, StringArgumentType.getString(ctx, "name"))))
                    .then(Commands.literal("list")
                        .executes(ctx -> listKitItems(ctx, StringArgumentType.getString(ctx, "name"))))))
            
            // /kitadmin reload
            .then(Commands.literal("reload")
                .executes(KitAdminCommand::reloadKits))
            
            // /kitadmin save
            .then(Commands.literal("save")
                .executes(KitAdminCommand::saveKits))
        );
    }
    
    /**
     * Create a new kit
     */
    private static int createKit(CommandContext<CommandSourceStack> context, String name, String displayName, String description) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        if (kitManager.getKit(name) != null) {
            MessageUtil.sendMessage(player, "§cKit '" + name + "' already exists!");
            return 0;
        }
        
        String actualDisplayName = displayName != null ? displayName : name;
        String actualDescription = description != null ? description : "Kit created by " + player.getName().getString();
        
        boolean success = kitManager.createKit(name, actualDisplayName, actualDescription, player.getName().getString());
        
        if (success) {
            MessageUtil.sendMessage(player, "§aKit '" + name + "' created successfully!");
            MessageUtil.sendMessage(player, "§7Use §e/kitadmin edit " + name + " <property> <value>§7 to configure it.");
            MessageUtil.sendMessage(player, "§7Use §e/kitadmin items " + name + " add§7 to add items from your inventory.");
            return 1;
        } else {
            MessageUtil.sendMessage(player, "§cFailed to create kit '" + name + "'!");
            return 0;
        }
    }
    
    /**
     * Delete a kit
     */
    private static int deleteKit(CommandContext<CommandSourceStack> context, String name) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        Kit kit = kitManager.getKit(name);
        if (kit == null) {
            MessageUtil.sendMessage(player, "§cKit '" + name + "' does not exist!");
            return 0;
        }
        
        boolean success = kitManager.deleteKit(name);
        
        if (success) {
            MessageUtil.sendMessage(player, "§aKit '" + name + "' deleted successfully!");
            return 1;
        } else {
            MessageUtil.sendMessage(player, "§cFailed to delete kit '" + name + "'!");
            return 0;
        }
    }
    
    /**
     * List all kits
     */
    private static int listKits(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        List<Kit> kits = kitManager.getAllKits();
        
        if (kits.isEmpty()) {
            MessageUtil.sendMessage(player, "§7No kits available.");
            return 0;
        }
        
        MessageUtil.sendMessage(player, "§6=== Available Kits (" + kits.size() + ") ===");
        
        for (Kit kit : kits) {
            String status = kit.isEnabled() ? "§a✓" : "§c✗";
            String cost = kit.hasCost() ? String.format(" §7(Cost: §e%.2f§7)", kit.getCost()) : "";
            String cooldown = kit.getCooldown() > 0 ? String.format(" §7(Cooldown: §e%ds§7)", kit.getCooldown()) : "";
            String permission = kit.requiresPermission() ? " §7(§c*§7)" : "";
            
            MessageUtil.sendMessage(player, String.format("%s §e%s §7- %s%s%s%s", 
                    status, kit.getName(), kit.getDescription(), cost, cooldown, permission));
        }
        
        MessageUtil.sendMessage(player, "§7Legend: §a✓§7=Enabled §c✗§7=Disabled §c*§7=Permission required");
        return 1;
    }
    
    /**
     * Show detailed kit information
     */
    private static int showKitInfo(CommandContext<CommandSourceStack> context, String name) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        Kit kit = kitManager.getKit(name);
        if (kit == null) {
            MessageUtil.sendMessage(player, "§cKit '" + name + "' does not exist!");
            return 0;
        }
        
        MessageUtil.sendMessage(player, "§6=== Kit Information: " + kit.getName() + " ===");
        MessageUtil.sendMessage(player, "§7Display Name: §e" + kit.getDisplayName());
        MessageUtil.sendMessage(player, "§7Description: §f" + kit.getDescription());
        MessageUtil.sendMessage(player, "§7Enabled: " + (kit.isEnabled() ? "§aYes" : "§cNo"));
        MessageUtil.sendMessage(player, "§7Cooldown: §e" + kit.getCooldown() + " seconds");
        MessageUtil.sendMessage(player, "§7Cost: §e" + kit.getCost());
        MessageUtil.sendMessage(player, "§7Permission: §e" + (kit.getPermission() != null ? kit.getPermission() : "None"));
        MessageUtil.sendMessage(player, "§7One Time Only: " + (kit.isOneTimeOnly() ? "§aYes" : "§cNo"));
        MessageUtil.sendMessage(player, "§7Created By: §e" + (kit.getCreatedBy() != null ? kit.getCreatedBy() : "Unknown"));
        
        if (kit.hasItems()) {
            MessageUtil.sendMessage(player, "§7Items (" + kit.getItems().size() + "):");
            for (int i = 0; i < Math.min(kit.getItems().size(), 5); i++) {
                KitItem item = kit.getItems().get(i);
                MessageUtil.sendMessage(player, "  §7- §e" + item.getCount() + "x " + item.getItemId());
            }
            if (kit.getItems().size() > 5) {
                MessageUtil.sendMessage(player, "  §7... and " + (kit.getItems().size() - 5) + " more items");
            }
        } else {
            MessageUtil.sendMessage(player, "§7Items: §cNone");
        }
        
        if (kit.hasCommands()) {
            MessageUtil.sendMessage(player, "§7Commands (" + kit.getCommands().size() + "):");
            for (int i = 0; i < Math.min(kit.getCommands().size(), 3); i++) {
                MessageUtil.sendMessage(player, "  §7- §e" + kit.getCommands().get(i));
            }
            if (kit.getCommands().size() > 3) {
                MessageUtil.sendMessage(player, "  §7... and " + (kit.getCommands().size() - 3) + " more commands");
            }
        }
        
        return 1;
    }
    
    /**
     * Edit kit property
     */
    private static int editKitProperty(CommandContext<CommandSourceStack> context, String property, String kitName, String value) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            MessageUtil.sendMessage(player, "§cKit '" + kitName + "' does not exist!");
            return 0;
        }
        
        try {
            switch (property.toLowerCase()) {
                case "displayname":
                    kit.setDisplayName(value);
                    break;
                case "description":
                    kit.setDescription(value);
                    break;
                case "cooldown":
                    kit.setCooldown(Long.parseLong(value));
                    break;
                case "cost":
                    kit.setCost(Double.parseDouble(value));
                    break;
                case "permission":
                    kit.setPermission(value.equals("none") ? null : value);
                    break;
                case "enabled":
                    kit.setEnabled(Boolean.parseBoolean(value));
                    break;
                case "onetimeonly":
                    kit.setOneTimeOnly(Boolean.parseBoolean(value));
                    break;
                default:
                    MessageUtil.sendMessage(player, "§cUnknown property: " + property);
                    return 0;
            }
            
            boolean success = kitManager.updateKit(kit);
            if (success) {
                MessageUtil.sendMessage(player, "§aUpdated kit '" + kitName + "' property '" + property + "' to: §e" + value);
                return 1;
            } else {
                MessageUtil.sendMessage(player, "§cFailed to update kit property!");
                return 0;
            }
            
        } catch (Exception e) {
            MessageUtil.sendMessage(player, "§cInvalid value for property '" + property + "': " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Add item from player's hand to kit
     */
    private static int addItemFromInventory(CommandContext<CommandSourceStack> context, String kitName) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            MessageUtil.sendMessage(player, "§cKit '" + kitName + "' does not exist!");
            return 0;
        }
        
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            MessageUtil.sendMessage(player, "§cYou must be holding an item!");
            return 0;
        }
        
        try {
            KitItem kitItem = KitItem.fromItemStack(heldItem);
            if (kitItem == null) {
                MessageUtil.sendMessage(player, "§cFailed to convert item!");
                return 0;
            }
            
            List<KitItem> items = kit.getItems();
            if (items == null) {
                items = new ArrayList<>();
            } else {
                items = new ArrayList<>(items); // Create mutable copy
            }
            
            items.add(kitItem);
            kit.setItems(items);
            
            boolean success = kitManager.updateKit(kit);
            if (success) {
                MessageUtil.sendMessage(player, "§aAdded §e" + heldItem.getCount() + "x " + 
                        BuiltInRegistries.ITEM.getKey(heldItem.getItem()) + "§a to kit '" + kitName + "'!");
                return 1;
            } else {
                MessageUtil.sendMessage(player, "§cFailed to update kit!");
                return 0;
            }
            
        } catch (Exception e) {
            MessageUtil.sendMessage(player, "§cError adding item: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Add specific item to kit
     */
    private static int addKitItem(CommandContext<CommandSourceStack> context, String kitName, String itemId, int count) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            MessageUtil.sendMessage(player, "§cKit '" + kitName + "' does not exist!");
            return 0;
        }
        
        try {
            // Validate item ID
            ResourceLocation itemResource;
            if (itemId.contains(":")) {
                String[] parts = itemId.split(":");
                itemResource = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            } else {
                itemResource = ResourceLocation.fromNamespaceAndPath("minecraft", itemId);
            }
            if (!BuiltInRegistries.ITEM.containsKey(itemResource)) {
                MessageUtil.sendMessage(player, "§cInvalid item ID: " + itemId);
                return 0;
            }
            
            KitItem kitItem = new KitItem(itemId, count);
            
            List<KitItem> items = kit.getItems();
            if (items == null) {
                items = new ArrayList<>();
            } else {
                items = new ArrayList<>(items); // Create mutable copy
            }
            
            items.add(kitItem);
            kit.setItems(items);
            
            boolean success = kitManager.updateKit(kit);
            if (success) {
                MessageUtil.sendMessage(player, "§aAdded §e" + count + "x " + itemId + "§a to kit '" + kitName + "'!");
                return 1;
            } else {
                MessageUtil.sendMessage(player, "§cFailed to update kit!");
                return 0;
            }
            
        } catch (Exception e) {
            MessageUtil.sendMessage(player, "§cError adding item: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Remove item from kit
     */
    private static int removeKitItem(CommandContext<CommandSourceStack> context, String kitName, String itemId) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            MessageUtil.sendMessage(player, "§cKit '" + kitName + "' does not exist!");
            return 0;
        }
        
        List<KitItem> items = kit.getItems();
        if (items == null || items.isEmpty()) {
            MessageUtil.sendMessage(player, "§cKit '" + kitName + "' has no items!");
            return 0;
        }
        
        boolean removed = items.removeIf(item -> item.getItemId().equals(itemId));
        
        if (removed) {
            kit.setItems(items);
            boolean success = kitManager.updateKit(kit);
            if (success) {
                MessageUtil.sendMessage(player, "§aRemoved §e" + itemId + "§a from kit '" + kitName + "'!");
                return 1;
            } else {
                MessageUtil.sendMessage(player, "§cFailed to update kit!");
                return 0;
            }
        } else {
            MessageUtil.sendMessage(player, "§cItem '" + itemId + "' not found in kit '" + kitName + "'!");
            return 0;
        }
    }
    
    /**
     * Clear all items from kit
     */
    private static int clearKitItems(CommandContext<CommandSourceStack> context, String kitName) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            MessageUtil.sendMessage(player, "§cKit '" + kitName + "' does not exist!");
            return 0;
        }
        
        kit.setItems(new ArrayList<>());
        boolean success = kitManager.updateKit(kit);
        
        if (success) {
            MessageUtil.sendMessage(player, "§aCleared all items from kit '" + kitName + "'!");
            return 1;
        } else {
            MessageUtil.sendMessage(player, "§cFailed to update kit!");
            return 0;
        }
    }
    
    /**
     * List items in kit
     */
    private static int listKitItems(CommandContext<CommandSourceStack> context, String kitName) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            MessageUtil.sendMessage(player, "§cKit '" + kitName + "' does not exist!");
            return 0;
        }
        
        if (!kit.hasItems()) {
            MessageUtil.sendMessage(player, "§7Kit '" + kitName + "' has no items.");
            return 0;
        }
        
        MessageUtil.sendMessage(player, "§6=== Items in Kit '" + kitName + "' ===");
        for (int i = 0; i < kit.getItems().size(); i++) {
            KitItem item = kit.getItems().get(i);
            MessageUtil.sendMessage(player, String.format("§7%d. §e%dx %s", 
                    i + 1, item.getCount(), item.getItemId()));
        }
        
        return 1;
    }
    
    /**
     * Reload kits from storage
     */
    private static int reloadKits(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        try {
            kitManager.reloadKits();
            MessageUtil.sendMessage(player, "§aKits reloaded successfully from storage!");
            return 1;
        } catch (Exception e) {
            MessageUtil.sendMessage(player, "§cFailed to reload kits: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Force save all kits
     */
    private static int saveKits(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
        
        try {
            boolean success = kitManager.saveAllKits();
            if (success) {
                MessageUtil.sendMessage(player, "§aKits saved successfully to storage!");
                return 1;
            } else {
                MessageUtil.sendMessage(player, "§cFailed to save kits!");
                return 0;
            }
        } catch (Exception e) {
            MessageUtil.sendMessage(player, "§cError saving kits: " + e.getMessage());
            return 0;
        }
    }
}
