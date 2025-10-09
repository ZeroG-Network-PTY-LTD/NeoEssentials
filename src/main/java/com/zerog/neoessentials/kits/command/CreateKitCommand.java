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
        dispatcher.register(Commands.literal("createkit")
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
            boolean success = kitManager.createKit(kitName, displayName, description, items, 
                                                  cooldownMillis, permission);
            
            if (success) {
                if (isUpdate) {
                    source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.createkit.updated", 
                        kitName, items.size(), formatCooldown(cooldownMillis)), false);
                } else {
                    source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.createkit.created", 
                        kitName, items.size(), formatCooldown(cooldownMillis)), false);
                }
                
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.createkit.permission_hint", 
                    permission), false);
                
                LOGGER.info("Kit '{}' {} by {}", kitName, isUpdate ? "updated" : "created", 
                           player.getName().getString());
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("commands.neoessentials.createkit.failed"));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error creating kit '{}' for player {}: {}", 
                        kitName, player.getName().getString(), e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.createkit.error"));
            return 0;
        }
    }
    
    private static boolean isValidKitName(String name) {
        // Kit names should be alphanumeric with underscores, no spaces, reasonable length
        return name != null && 
               name.length() > 0 && 
               name.length() <= 32 && 
               name.matches("^[a-zA-Z0-9_]+$");
    }
    
    private static String formatCooldown(long millis) {
        if (millis == 0) return "no cooldown";
        
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }
}