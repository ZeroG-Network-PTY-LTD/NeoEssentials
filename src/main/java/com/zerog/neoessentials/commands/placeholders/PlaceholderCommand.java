package com.zerog.neoessentials.commands.placeholders;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.placeholders.PlaceholderManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Placeholder command implementation - /placeholder
 * Manages and tests custom placeholders
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class PlaceholderCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("placeholder")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(PlaceholderCommand::showHelp)
            .then(Commands.literal("help")
                .executes(PlaceholderCommand::showHelp))
            .then(Commands.literal("list")
                .executes(PlaceholderCommand::listPlaceholders))
            .then(Commands.literal("test")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(PlaceholderCommand::testPlaceholders)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(PlaceholderCommand::testPlaceholdersForPlayer))))
            .then(Commands.literal("info")
                .then(Commands.argument("placeholder", StringArgumentType.string())
                    .executes(PlaceholderCommand::showPlaceholderInfo)))
            .then(Commands.literal("reload")
                .executes(PlaceholderCommand::reloadPlaceholders))
        );
        
        // Alias commands
        dispatcher.register(Commands.literal("placeholders")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(PlaceholderCommand::showHelp)
            .then(Commands.literal("list")
                .executes(PlaceholderCommand::listPlaceholders))
            .then(Commands.literal("test")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(PlaceholderCommand::testPlaceholders))));
                    
        dispatcher.register(Commands.literal("papi")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .executes(PlaceholderCommand::showHelp)
            .then(Commands.literal("test")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(PlaceholderCommand::testPlaceholders))));
    }
    
    /**
     * Show help menu
     */
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        sendMessage(context.getSource(), "§6§l=== NeoEssentials Placeholder System ===");
        sendMessage(context.getSource(), "§eManage and test custom placeholders");
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§e§lCommands:");
        sendMessage(context.getSource(), "§e/placeholder help §7- Show this help menu");
        sendMessage(context.getSource(), "§e/placeholder list §7- List all available placeholders");
        sendMessage(context.getSource(), "§e/placeholder test <text> §7- Test placeholder processing");
        sendMessage(context.getSource(), "§e/placeholder test <text> <player> §7- Test placeholders for specific player");
        sendMessage(context.getSource(), "§e/placeholder info <placeholder> §7- Get information about a placeholder");
        sendMessage(context.getSource(), "§e/placeholder reload §7- Reload placeholder system");
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§e§lExample Usage:");
        sendMessage(context.getSource(), "§7/placeholder test \"Hello %player_name%, you have %player_health% health!\"");
        sendMessage(context.getSource(), "§7/placeholder test \"Server has %server_players%/%server_max_players% players online\"");
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§7Use §e%placeholder%§7 or §e{placeholder}§7 format in text");
        
        return 1;
    }
    
    /**
     * List all available placeholders
     */
    private static int listPlaceholders(CommandContext<CommandSourceStack> context) {
        PlaceholderManager manager = PlaceholderManager.getInstance();
        Set<String> placeholders = manager.getRegisteredPlaceholders();
        
        sendMessage(context.getSource(), "§6§l=== Available Placeholders (" + placeholders.size() + ") ===");
        sendMessage(context.getSource(), "");
        
        // Group placeholders by category
        sendMessage(context.getSource(), "§e§lPlayer Placeholders:");
        placeholders.stream()
            .filter(p -> p.startsWith("player_"))
            .sorted()
            .forEach(p -> sendMessage(context.getSource(), "§7- §e%" + p + "%"));
        
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§e§lServer Placeholders:");
        placeholders.stream()
            .filter(p -> p.startsWith("server_"))
            .sorted()
            .forEach(p -> sendMessage(context.getSource(), "§7- §e%" + p + "%"));
        
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§e§lWorld Placeholders:");
        placeholders.stream()
            .filter(p -> p.startsWith("world_"))
            .sorted()
            .forEach(p -> sendMessage(context.getSource(), "§7- §e%" + p + "%"));
        
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§e§lTime Placeholders:");
        placeholders.stream()
            .filter(p -> p.contains("time") || p.contains("date"))
            .sorted()
            .forEach(p -> sendMessage(context.getSource(), "§7- §e%" + p + "%"));
        
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§e§lOther Placeholders:");
        placeholders.stream()
            .filter(p -> !p.startsWith("player_") && !p.startsWith("server_") && 
                        !p.startsWith("world_") && !p.contains("time") && !p.contains("date"))
            .sorted()
            .forEach(p -> sendMessage(context.getSource(), "§7- §e%" + p + "%"));
        
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§7Use §e/placeholder test <text>§7 to test placeholders");
        
        return 1;
    }
    
    /**
     * Test placeholder processing for command sender
     */
    private static int testPlaceholders(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String text = StringArgumentType.getString(context, "text");
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        return testPlaceholdersForSpecificPlayer(context, text, player);
    }
    
    /**
     * Test placeholder processing for specified player
     */
    private static int testPlaceholdersForPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String text = StringArgumentType.getString(context, "text");
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        
        return testPlaceholdersForSpecificPlayer(context, text, player);
    }
    
    /**
     * Core placeholder testing method
     */
    private static int testPlaceholdersForSpecificPlayer(CommandContext<CommandSourceStack> context, String text, ServerPlayer player) {
        PlaceholderManager manager = PlaceholderManager.getInstance();
        
        sendMessage(context.getSource(), "§6§l=== Placeholder Test ===");
        sendMessage(context.getSource(), "§7Testing for player: §e" + player.getName().getString());
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§7Original text:");
        sendMessage(context.getSource(), "§f" + text);
        sendMessage(context.getSource(), "");
        sendMessage(context.getSource(), "§7Processed text:");
        
        String processed = manager.processPlaceholders(text, player);
        sendMessage(context.getSource(), "§a" + processed);
        
        return 1;
    }
    
    /**
     * Show information about a specific placeholder
     */
    private static int showPlaceholderInfo(CommandContext<CommandSourceStack> context) {
        String placeholder = StringArgumentType.getString(context, "placeholder");
        PlaceholderManager manager = PlaceholderManager.getInstance();
        
        // Remove % or {} if provided
        String cleanPlaceholder = placeholder.replace("%", "").replace("{", "").replace("}", "");
        
        if (manager.isPlaceholderRegistered(cleanPlaceholder)) {
            sendMessage(context.getSource(), "§6§l=== Placeholder Information ===");
            sendMessage(context.getSource(), "§7Placeholder: §e%" + cleanPlaceholder + "%");
            sendMessage(context.getSource(), "§7Status: §aRegistered");
            sendMessage(context.getSource(), "§7Type: §eBuilt-in");
            sendMessage(context.getSource(), "");
            sendMessage(context.getSource(), "§7Usage examples:");
            sendMessage(context.getSource(), "§f- %" + cleanPlaceholder + "%");
            sendMessage(context.getSource(), "§f- {" + cleanPlaceholder + "}");
            
            // Show sample output if possible
            try {
                if (context.getSource().getPlayer() != null) {
                    String sample = manager.processPlaceholders("%" + cleanPlaceholder + "%", context.getSource().getPlayer());
                    sendMessage(context.getSource(), "");
                    sendMessage(context.getSource(), "§7Sample output: §a" + sample);
                }
            } catch (Exception e) {
                // Ignore errors in sample generation
            }
        } else {
            sendMessage(context.getSource(), "§c§l=== Placeholder Not Found ===");
            sendMessage(context.getSource(), "§7Placeholder: §e%" + cleanPlaceholder + "%");
            sendMessage(context.getSource(), "§7Status: §cNot registered");
            sendMessage(context.getSource(), "");
            sendMessage(context.getSource(), "§7Use §e/placeholder list§7 to see available placeholders");
        }
        
        return 1;
    }
    
    /**
     * Reload placeholder system
     */
    private static int reloadPlaceholders(CommandContext<CommandSourceStack> context) {
        try {
            PlaceholderManager manager = PlaceholderManager.getInstance();
            
            // Reload custom placeholders from config
            manager.reloadCustomPlaceholders();
            
            int count = manager.getPlaceholderCount();
            
            sendMessage(context.getSource(), "§a§l=== Placeholder System Reloaded ===");
            sendMessage(context.getSource(), "§7Reloaded custom placeholders from configuration");
            sendMessage(context.getSource(), "§7Total placeholders available: §e" + count);
            sendMessage(context.getSource(), "§7System status: §aOperational");
            
            LOGGER.info("Placeholder system reloaded by {}", context.getSource().getDisplayName().getString());
            
        } catch (Exception e) {
            sendMessage(context.getSource(), "§c§l=== Reload Failed ===");
            sendMessage(context.getSource(), "§cError: " + e.getMessage());
            LOGGER.error("Failed to reload placeholder system", e);
        }
        
        return 1;
    }
    
    /**
     * Send a formatted message to the command source
     */
    private static void sendMessage(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message
            .replace("&0", "§0").replace("&1", "§1").replace("&2", "§2").replace("&3", "§3")
            .replace("&4", "§4").replace("&5", "§5").replace("&6", "§6").replace("&7", "§7")
            .replace("&8", "§8").replace("&9", "§9").replace("&a", "§a").replace("&b", "§b")
            .replace("&c", "§c").replace("&d", "§d").replace("&e", "§e").replace("&f", "§f")
            .replace("&l", "§l").replace("&m", "§m").replace("&n", "§n").replace("&o", "§o")
            .replace("&r", "§r").replace("&k", "§k")), false);
    }
}
