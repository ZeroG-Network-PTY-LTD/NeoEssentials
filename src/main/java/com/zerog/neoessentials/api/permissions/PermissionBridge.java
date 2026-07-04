package com.zerog.neoessentials.api.permissions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Permission bridge that integrates NeoEssentials permissions with external permission plugins.
 * This class provides commands and utilities to help server administrators work with permissions.
 */
public class PermissionBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionBridge.class);
    
    /**
     * Register permission-related commands
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register command to list all NeoEssentials permissions
        dispatcher.register(Commands.literal("neoessentials-permissions")
            .requires(source -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 4)) // Op level 4 required
            .executes(ctx -> {
                listAllPermissions(ctx.getSource());
                return 1;
            })
            .then(Commands.literal("export")
                .then(Commands.argument("format", StringArgumentType.word())
                    .suggests(getFormatSuggestions())
                    .executes(ctx -> {
                        String format = StringArgumentType.getString(ctx, "format");
                        exportPermissions(ctx.getSource(), format);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("search")
                .then(Commands.argument("query", StringArgumentType.greedyString())
                    .suggests(PermissionTabCompleter.NEOESSENTIALS_PERMISSIONS)
                    .executes(ctx -> {
                        String query = StringArgumentType.getString(ctx, "query");
                        searchPermissions(ctx.getSource(), query);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("category")
                .then(Commands.argument("category", StringArgumentType.word())
                    .suggests(getCategorySuggestions())
                    .executes(ctx -> {
                        String categoryName = StringArgumentType.getString(ctx, "category");
                        showCategoryPermissions(ctx.getSource(), categoryName);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("refresh")
                .requires(source -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 4))
                .executes(ctx -> {
                    refreshPermissions(ctx.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("scan")
                .requires(source -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 4))
                .executes(ctx -> {
                    scanPermissions(ctx.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("discovered")
                .executes(ctx -> {
                    showDiscoveredPermissions(ctx.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("pex-help")
                .executes(ctx -> {
                    showPermissionsEXHelp(ctx.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("list-all")
                .executes(ctx -> {
                    listAllPermissionsForTabCompletion(ctx.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("group-examples")
                .executes(ctx -> {
                    showGroupExamples(ctx.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("user-examples")
                .executes(ctx -> {
                    showUserExamples(ctx.getSource());
                    return 1;
                })
            )
        );
        
        // Register a helper command that external plugins can use
        dispatcher.register(Commands.literal("neoe-perms")
            .requires(source -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 4))
            .executes(ctx -> {
                listAllPermissions(ctx.getSource());
                return 1;
            })
        );
    }
    
    /**
     * Suggestion provider for export formats
     */
    private static SuggestionProvider<CommandSourceStack> getFormatSuggestions() {
        return (ctx, builder) -> SharedSuggestionProvider.suggest(
            List.of("yaml", "json", "txt", "pex", "luckperms"), builder);
    }
    
    /**
     * Suggestion provider for categories
     */
    private static SuggestionProvider<CommandSourceStack> getCategorySuggestions() {
        return (ctx, builder) -> {
            List<String> categories = List.of();
            for (PermissionRegistry.PermissionCategory category : PermissionRegistry.PermissionCategory.values()) {
                categories = new java.util.ArrayList<>(categories);
                categories.add(category.getKey());
            }
            return SharedSuggestionProvider.suggest(categories, builder);
        };
    }
    
    /**
     * List all permissions to the command source
     */
    private static void listAllPermissions(CommandSourceStack source) {
        PermissionRegistry registry = PermissionRegistry.getInstance();
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.list_header"), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.list_total", registry.getAllPermissions().size()), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        for (PermissionRegistry.PermissionCategory category : PermissionRegistry.PermissionCategory.values()) {
            var categoryPerms = registry.getPermissionsByCategory(category);
            if (categoryPerms.isEmpty()) continue;
            
            source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.permissions.category_header", category.getDescription(), categoryPerms.size()), false);
            
            categoryPerms.stream()
                .sorted()
                .limit(5) // Show only first 5 to avoid spam
                .forEach(perm -> {
                    PermissionRegistry.PermissionInfo info = registry.getPermissionInfo(perm);
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.permission_entry", perm, info.getDescription()), false);
                });
            
            if (categoryPerms.size() > 5) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.more_permissions", categoryPerms.size() - 5), false);
            }
            source.sendSuccess(() -> Component.literal(""), false);
        }
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.export_help"), false);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.search_help"), false);
    }
    
    /**
     * Export permissions to a file
     */
    private static void exportPermissions(CommandSourceStack source, String format) {
        try {
            String filename = "neoessentials-permissions." + format.toLowerCase();
            File file = new File(filename);
            
            PermissionRegistry registry = PermissionRegistry.getInstance();
            
            switch (format.toLowerCase()) {
                case "yaml", "yml" -> exportAsYaml(file, registry);
                case "json" -> exportAsJson(file, registry);
                case "txt", "text" -> exportAsText(file, registry);
                case "pex", "permissionsex" -> exportAsPEX(file, registry);
                case "luckperms", "lp" -> exportAsLuckPerms(file, registry);
                default -> {
                    source.sendFailure(MessageUtil.error("commands.neoessentials.permissions.unsupported_format", format));
                    return;
                }
            }
            
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.export_success", file.getAbsolutePath()), false);
            
        } catch (IOException e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.permissions.export_failed", e.getMessage()));
            LOGGER.error("Failed to export permissions", e);
        }
    }
    
    /**
     * Search permissions by query
     */
    private static void searchPermissions(CommandSourceStack source, String query) {
        PermissionRegistry registry = PermissionRegistry.getInstance();
        List<String> matches = registry.getPermissionsStartingWith(query.toLowerCase());
        
        if (matches.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.permissions.no_matches", query));
            return;
        }
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.search_results", query, matches.size()), false);
        
        matches.stream()
            .limit(20) // Limit to avoid spam
            .forEach(perm -> {
                PermissionRegistry.PermissionInfo info = registry.getPermissionInfo(perm);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.search_entry", perm, info.getDescription()), false);
            });
        
        if (matches.size() > 20) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.more_matches", matches.size() - 20), false);
        }
    }
    
    /**
     * Refresh permissions by re-scanning the codebase
     */
    private static void refreshPermissions(CommandSourceStack source) {
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.refreshing"), false);
        
        try {
            PermissionRegistry registry = PermissionRegistry.getInstance();
            int beforeCount = registry.getAllPermissions().size();
            
            registry.refreshPermissions();
            
            int afterCount = registry.getAllPermissions().size();
            int newCount = afterCount - beforeCount;
            
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.refresh_completed"), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.refresh_stats", beforeCount, afterCount, newCount), false);
            
            // Re-initialize tab completer with new permissions
            PermissionTabCompleter.initialize();
            
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.permissions.refresh_error", e.getMessage()));
            LOGGER.error("Error refreshing permissions", e);
        }
    }
    
    /**
     * Manually scan for permissions and show report
     */
    private static void scanPermissions(CommandSourceStack source) {
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.scanning"), false);
        
        try {
            PermissionScanner scanner = PermissionScanner.getInstance();
            scanner.scanForPermissions();
            
            var discovered = scanner.getDiscoveredPermissions();
            var dynamicPrefixes = scanner.getDynamicPermissionPrefixes();
            var byCategory = scanner.getPermissionsByCategory();
            
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.scan_completed"), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.scan_stats", discovered.size(), dynamicPrefixes.size()), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            // Show breakdown by category
            for (var entry : byCategory.entrySet()) {
                String category = entry.getKey();
                var perms = entry.getValue();
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.scan.category_summary", category.toUpperCase(), perms.size()), false);
            }
            
            if (!dynamicPrefixes.isEmpty()) {
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.scan.dynamic_prefixes_header"), false);
                dynamicPrefixes.stream().sorted().limit(10).forEach(prefix -> 
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.scan.dynamic_prefix", prefix), false)
                );
            }
            
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.permissions.scan.error", e.getMessage()));
            LOGGER.error("Error scanning permissions", e);
        }
    }
    
    /**
     * Show discovered permissions separate from manual ones
     */
    private static void showDiscoveredPermissions(CommandSourceStack source) {
        try {
            PermissionRegistry registry = PermissionRegistry.getInstance();
            var discovered = registry.getAutoDiscoveredPermissions();
            
            if (discovered.isEmpty()) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.permissions.discovered.none_found"));
                return;
            }
            
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.discovered.header", discovered.size()), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.discovered.description"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            // Group by category
            Map<String, List<String>> byCategory = new HashMap<>();
            for (String perm : discovered) {
                String[] parts = perm.split("\\.");
                String category = parts.length >= 2 ? parts[1] : "unknown";
                byCategory.computeIfAbsent(category, k -> new java.util.ArrayList<>()).add(perm);
            }
            
            for (Map.Entry<String, List<String>> entry : byCategory.entrySet()) {
                String category = entry.getKey();
                List<String> perms = entry.getValue();
                
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.discovered.category_header", category.toUpperCase(), perms.size()), false);
                
                perms.stream().sorted().limit(8).forEach(perm -> 
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.discovered.permission_entry", perm), false)
                );
                
                if (perms.size() > 8) {
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.discovered.more_count", (perms.size() - 8)), false);
                }
                source.sendSuccess(() -> Component.literal(""), false);
            }
            
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.permissions.discovered.error", e.getMessage()));
            LOGGER.error("Error getting discovered permissions", e);
        }
    }
    
    /**
     * Show permissions for a specific category
     */
    private static void showCategoryPermissions(CommandSourceStack source, String categoryName) {
        PermissionRegistry registry = PermissionRegistry.getInstance();
        
        PermissionRegistry.PermissionCategory foundCategory = null;
        for (PermissionRegistry.PermissionCategory cat : PermissionRegistry.PermissionCategory.values()) {
            if (cat.getKey().equalsIgnoreCase(categoryName)) {
                foundCategory = cat;
                break;
            }
        }
        
        if (foundCategory == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.permissions.category.unknown", categoryName));
            return;
        }
        
        final PermissionRegistry.PermissionCategory category = foundCategory;
        var categoryPerms = registry.getPermissionsByCategory(category);
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.category.header", category.getDescription(), categoryPerms.size()), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        categoryPerms.stream()
            .sorted()
            .forEach(perm -> {
                PermissionRegistry.PermissionInfo info = registry.getPermissionInfo(perm);
                String defaultStr = info.getDefaultValue() ? "§a✓" : "§c✗";
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.category.permission_details", perm, defaultStr, info.getDescription()), false);
            });
    }
    
    /**
     * Export permissions as YAML (for various plugins)
     */
    private static void exportAsYaml(File file, PermissionRegistry registry) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("# NeoEssentials Permission Nodes\n");
            writer.write("# Generated automatically - " + new java.util.Date() + "\n");
            writer.write("# Total permissions: " + registry.getAllPermissions().size() + "\n\n");
            
            writer.write("permissions:\n");
            
            for (String permission : registry.getAllPermissions().stream().sorted().toList()) {
                PermissionRegistry.PermissionInfo info = registry.getPermissionInfo(permission);
                writer.write("  \"" + permission + "\":\n");
                writer.write("    description: \"" + info.getDescription() + "\"\n");
                writer.write("    default: " + info.getDefaultValue() + "\n");
                writer.write("    category: \"" + info.getCategory().getKey() + "\"\n");
            }
        }
    }
    
    /**
     * Export permissions as JSON
     */
    private static void exportAsJson(File file, PermissionRegistry registry) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\n");
            writer.write("  \"_metadata\": {\n");
            writer.write("    \"generated\": \"" + new java.util.Date() + "\",\n");
            writer.write("    \"total\": " + registry.getAllPermissions().size() + ",\n");
            writer.write("    \"mod\": \"NeoEssentials\"\n");
            writer.write("  },\n");
            writer.write("  \"permissions\": {\n");
            
            List<String> permissions = registry.getAllPermissions().stream().sorted().toList();
            for (int i = 0; i < permissions.size(); i++) {
                String permission = permissions.get(i);
                PermissionRegistry.PermissionInfo info = registry.getPermissionInfo(permission);
                
                writer.write("    \"" + permission + "\": {\n");
                writer.write("      \"description\": \"" + info.getDescription() + "\",\n");
                writer.write("      \"default\": " + info.getDefaultValue() + ",\n");
                writer.write("      \"category\": \"" + info.getCategory().getKey() + "\"\n");
                writer.write("    }");
                
                if (i < permissions.size() - 1) writer.write(",");
                writer.write("\n");
            }
            
            writer.write("  }\n");
            writer.write("}\n");
        }
    }
    
    /**
     * Export permissions as plain text
     */
    private static void exportAsText(File file, PermissionRegistry registry) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            List<String> exportLines = registry.exportPermissions();
            for (String line : exportLines) {
                writer.write(line + "\n");
            }
        }
    }
    
    /**
     * Export permissions for PermissionsEX
     */
    private static void exportAsPEX(File file, PermissionRegistry registry) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("# PermissionsEX configuration for NeoEssentials\n");
            writer.write("# This file contains ALL NeoEssentials permissions for tab completion\n");
            writer.write("# Import this into your PermissionsEX configuration\n\n");
            
            // Include auto-discovered permissions
            PermissionScanner scanner = PermissionScanner.getInstance();
            scanner.scanForPermissions();
            
            // Get all permissions (registered + discovered)
            Set<String> allPermissions = new HashSet<>(registry.getAllPermissions());
            allPermissions.addAll(scanner.getDiscoveredPermissions());
            
            writer.write("# ============================================\n");
            writer.write("# INDIVIDUAL PERMISSIONS FOR TAB COMPLETION\n");
            writer.write("# ============================================\n");
            writer.write("# Copy these to your permissions.yml or use them with /pex commands\n");
            writer.write("# Both USER and GROUP commands are supported:\n");
            writer.write("# - /pex group <group> add <permission>\n");
            writer.write("# - /pex user <user> add <permission>\n\n");
            
            for (PermissionRegistry.PermissionCategory category : PermissionRegistry.PermissionCategory.values()) {
                var categoryPerms = allPermissions.stream()
                    .filter(perm -> categorizePermission(perm) == category)
                    .sorted()
                    .toList();
                    
                if (categoryPerms.isEmpty()) continue;
                
                writer.write("# " + category.getDescription() + " Permissions (" + categoryPerms.size() + " total)\n");
                for (String permission : categoryPerms) {
                    PermissionRegistry.PermissionInfo info = registry.getPermissionInfo(permission);
                    String description = info != null ? info.getDescription() : "Auto-discovered permission";
                    writer.write(permission + " # " + description + "\n");
                }
                writer.write("\n");
            }
            
            writer.write("# ===============================\n");
            writer.write("# WILDCARD PERMISSIONS\n");
            writer.write("# ===============================\n");
            writer.write("# Use these for broader permission grants\n");
            writer.write("neoessentials.* # All NeoEssentials permissions\n");
            for (PermissionRegistry.PermissionCategory category : PermissionRegistry.PermissionCategory.values()) {
                writer.write("neoessentials." + category.getKey() + ".* # All " + category.getDescription() + " permissions\n");
            }
            
            writer.write("\n# ===============================\n");
            writer.write("# GROUP USAGE EXAMPLES\n");
            writer.write("# ===============================\n");
            writer.write("# Full admin access:\n");
            writer.write("# /pex group admin add neoessentials.*\n");
            writer.write("\n# Moderator permissions:\n");
            writer.write("# /pex group moderator add neoessentials.teleport.*\n");
            writer.write("# /pex group moderator add neoessentials.chat.*\n");
            writer.write("# /pex group moderator add neoessentials.admin.permissions\n");
            writer.write("\n# Basic player permissions:\n");
            writer.write("# /pex group player add neoessentials.teleport.home.home\n");
            writer.write("# /pex group player add neoessentials.teleport.home.set\n");
            writer.write("# /pex group player add neoessentials.teleport.home.delete\n");
            writer.write("# /pex group player add neoessentials.teleport.home.list\n");
            writer.write("# /pex group player add neoessentials.economy.balance\n");
            writer.write("# /pex group player add neoessentials.economy.pay\n");
            writer.write("# /pex group player add neoessentials.kits.starter\n");
            writer.write("# /pex group player add neoessentials.chat.msg\n");
            writer.write("# /pex group player add neoessentials.chat.reply\n");
            writer.write("\n# VIP player permissions:\n");
            writer.write("# /pex group vip add neoessentials.teleport.spawn.*\n");
            writer.write("# /pex group vip add neoessentials.teleport.warp.*\n");
            writer.write("# /pex group vip add neoessentials.kits.*\n");
            writer.write("# /pex group vip add neoessentials.utility.*\n");
            
            writer.write("\n# ===============================\n");
            writer.write("# USER USAGE EXAMPLES\n");
            writer.write("# ===============================\n");
            writer.write("# Grant specific permissions to individual users:\n");
            writer.write("# /pex user PlayerName add neoessentials.teleport.admin.tp\n");
            writer.write("# /pex user PlayerName add neoessentials.teleport.admin.tphere\n");
            writer.write("# /pex user PlayerName add neoessentials.economy.eco.give\n");
            writer.write("# /pex user PlayerName add neoessentials.admin.reload\n");
            
            writer.write("\n# ===============================\n");
            writer.write("# QUICK GROUP SETUP COMMANDS\n");
            writer.write("# ===============================\n");
            writer.write("# Copy and paste these command blocks:\n\n");
            
            writer.write("# Create Admin Group:\n");
            writer.write("# /pex group admin create\n");
            writer.write("# /pex group admin add neoessentials.*\n\n");
            
            writer.write("# Create Moderator Group:\n");
            writer.write("# /pex group moderator create\n");
            writer.write("# /pex group moderator add neoessentials.teleport.*\n");
            writer.write("# /pex group moderator add neoessentials.chat.*\n");
            writer.write("# /pex group moderator add neoessentials.admin.permissions\n\n");
            
            writer.write("# Create Player Group:\n");
            writer.write("# /pex group player create\n");
            writer.write("# /pex group player add neoessentials.teleport.home.*\n");
            writer.write("# /pex group player add neoessentials.teleport.spawn.spawn\n");
            writer.write("# /pex group player add neoessentials.economy.balance\n");
            writer.write("# /pex group player add neoessentials.economy.pay\n");
            writer.write("# /pex group player add neoessentials.kits.starter\n");
            writer.write("# /pex group player add neoessentials.chat.msg\n");
            writer.write("# /pex group player add neoessentials.chat.reply\n\n");
            
            writer.write("# Create VIP Group:\n");
            writer.write("# /pex group vip create\n");
            writer.write("# /pex group vip add neoessentials.teleport.*\n");
            writer.write("# /pex group vip add neoessentials.economy.*\n");
            writer.write("# /pex group vip add neoessentials.kits.*\n");
            writer.write("# /pex group vip add neoessentials.utility.*\n");
            
            writer.write("\n# ===============================\n");
            writer.write("# FOR PERMISSIONSEX TAB COMPLETION\n");
            writer.write("# ===============================\n");
            writer.write("# To enable tab completion for both user and group commands:\n");
            writer.write("# 1. Ensure permissions are registered in your PermissionsEX configuration\n");
            writer.write("# 2. Add these permissions to at least one group\n");
            writer.write("# 3. Use the commands above to pre-register permissions\n");
            writer.write("# 4. Tab completion should work for:\n");
            writer.write("#    - /pex group <tab> add neoessentials.<tab>\n");
            writer.write("#    - /pex user <tab> add neoessentials.<tab>\n");
            writer.write("# 5. If still not working, try /pex reload after adding permissions\n");
        }
    }
    
    /**
     * Categorize permission for export (helper method)
     */
    private static PermissionRegistry.PermissionCategory categorizePermission(String permission) {
        String[] parts = permission.split("\\.");
        
        if (parts.length >= 2) {
            String category = parts[1].toLowerCase();
            
            switch (category) {
                case "economy", "eco", "balance" -> {
                    return PermissionRegistry.PermissionCategory.ECONOMY;
                }
                case "teleport", "tp", "spawn", "home", "warp" -> {
                    return PermissionRegistry.PermissionCategory.TELEPORT;
                }
                case "chat", "msg", "message", "social" -> {
                    return PermissionRegistry.PermissionCategory.CHAT;
                }
                case "kits", "kit" -> {
                    return PermissionRegistry.PermissionCategory.KITS;
                }
                case "moderation", "mod", "mute", "ban", "kick", "freeze", "jail", "vanish" -> {
                    return PermissionRegistry.PermissionCategory.MODERATION;
                }
                case "admin", "administration" -> {
                    return PermissionRegistry.PermissionCategory.ADMIN;
                }
                case "utility", "utilities", "util" -> {
                    return PermissionRegistry.PermissionCategory.MISC;
                }
                default -> {
                    return PermissionRegistry.PermissionCategory.MISC;
                }
            }
        }
        
        return PermissionRegistry.PermissionCategory.MISC;
    }
    
    /**
     * Export permissions for LuckPerms
     */
    private static void exportAsLuckPerms(File file, PermissionRegistry registry) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("# LuckPerms commands for NeoEssentials permissions\n");
            writer.write("# Run these commands in your server console\n\n");
            
            writer.write("# Create NeoEssentials permission groups\n");
            for (PermissionRegistry.PermissionCategory category : PermissionRegistry.PermissionCategory.values()) {
                var categoryPerms = registry.getPermissionsByCategory(category);
                if (categoryPerms.isEmpty()) continue;
                
                String groupName = "neoessentials_" + category.getKey();
                writer.write("/lp creategroup " + groupName + "\n");
                writer.write("/lp group " + groupName + " meta setdisplayname \"&6NeoEssentials " + category.getDescription() + "\"\n");
                
                for (String permission : categoryPerms.stream().sorted().toList()) {
                    writer.write("/lp group " + groupName + " permission set " + permission + " true\n");
                }
                writer.write("\n");
            }
        }
    }
    
    /**
     * Get all permissions as a list (for external use including PermissionsEX tab completion)
     */
    public static List<String> getAllPermissions() {
        PermissionRegistry registry = PermissionRegistry.getInstance();
        PermissionScanner scanner = PermissionScanner.getInstance();
        
        // Ensure we have the latest discovered permissions
        scanner.scanForPermissions();
        
        // Combine registered and discovered permissions
        Set<String> allPermissions = new HashSet<>(registry.getAllPermissions());
        allPermissions.addAll(scanner.getDiscoveredPermissions());
        
        return allPermissions.stream().sorted().toList();
    }
    
    /**
     * Show help for PermissionsEX integration
     */
    private static void showPermissionsEXHelp(CommandSourceStack source) {
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.help.pex.title"), false);
        source.sendSuccess(() -> Component.literal("§eThe issue you're experiencing is that PermissionsEX only shows"), false);
        source.sendSuccess(() -> Component.literal("§ewildcard permissions (*.teleport.*) in tab completion, not"), false);
        source.sendSuccess(() -> Component.literal("§eindividual permissions. Here's how to fix it:"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§a1. Export permissions for PEX:"), false);
        source.sendSuccess(() -> Component.literal("§f   /neoessentials-permissions export pex"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§a2. List all individual permissions:"), false);
        source.sendSuccess(() -> Component.literal("§f   /neoessentials-permissions list-all"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§a3. Use the exported file or copy permissions manually"), false);
        source.sendSuccess(() -> Component.literal("§f   Check: neoessentials-permissions.pex"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§a4. PermissionsEX Group Commands:"), false);
        source.sendSuccess(() -> Component.literal("§f   /pex group admin add neoessentials.*"), false);
        source.sendSuccess(() -> Component.literal("§f   /pex group moderator add neoessentials.teleport.*"), false);
        source.sendSuccess(() -> Component.literal("§f   /pex group player add neoessentials.teleport.home.home"), false);
        source.sendSuccess(() -> Component.literal("§f   /pex group player add neoessentials.economy.balance"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§a5. PermissionsEX User Commands:"), false);
        source.sendSuccess(() -> Component.literal("§f   /pex user [username] add neoessentials.teleport.admin.tp"), false);
        source.sendSuccess(() -> Component.literal("§f   /pex user [username] add neoessentials.kits.starter"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§c6. Typical issue: PermissionsEX tab completion only shows"), false);
        source.sendSuccess(() -> Component.literal("§c   permissions it knows about. Individual permissions need"), false);
        source.sendSuccess(() -> Component.literal("§c   to be registered with the permission system first."), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§b7. Recommended Permission Groups:"), false);
        source.sendSuccess(() -> Component.literal("§f   - Admin: neoessentials.*"), false);
        source.sendSuccess(() -> Component.literal("§f   - Moderator: neoessentials.teleport.*, neoessentials.chat.*"), false);
        source.sendSuccess(() -> Component.literal("§f   - Player: neoessentials.teleport.home.*, neoessentials.economy.*"), false);
    }
    
    /**
     * List all permissions in a format suitable for PermissionsEX tab completion
     */
    private static void listAllPermissionsForTabCompletion(CommandSourceStack source) {
        PermissionRegistry registry = PermissionRegistry.getInstance();
        PermissionScanner scanner = PermissionScanner.getInstance();
        
        // Force a fresh scan
        scanner.scanForPermissions();
        
        // Get all permissions
        Set<String> allPermissions = new HashSet<>(registry.getAllPermissions());
        allPermissions.addAll(scanner.getDiscoveredPermissions());
        
        source.sendSuccess(() -> Component.literal("§6┌─ ALL NEOESSENTIALS PERMISSIONS ─┐"), false);
        source.sendSuccess(() -> Component.literal("§eTotal: " + allPermissions.size() + " permissions"), false);
        source.sendSuccess(() -> Component.literal("§eCopy these for PermissionsEX commands:"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        // Group by category for better organization
        Map<PermissionRegistry.PermissionCategory, List<String>> grouped = new HashMap<>();
        
        for (String permission : allPermissions) {
            PermissionRegistry.PermissionCategory category = categorizePermission(permission);
            grouped.computeIfAbsent(category, k -> new java.util.ArrayList<>()).add(permission);
        }
        
        for (PermissionRegistry.PermissionCategory category : PermissionRegistry.PermissionCategory.values()) {
            List<String> categoryPerms = grouped.get(category);
            if (categoryPerms == null || categoryPerms.isEmpty()) continue;
            
            categoryPerms.sort(String::compareTo);
            
            source.sendSuccess(() -> Component.literal("§a" + category.getDescription() + ":"), false);
            for (String permission : categoryPerms) {
                source.sendSuccess(() -> Component.literal("§7  " + permission), false);
            }
            source.sendSuccess(() -> Component.literal(""), false);
        }
        
        source.sendSuccess(() -> Component.literal("§6=== WILDCARD PERMISSIONS ==="), false);
        source.sendSuccess(() -> Component.literal("§7  neoessentials.*"), false);
        for (PermissionRegistry.PermissionCategory category : PermissionRegistry.PermissionCategory.values()) {
            source.sendSuccess(() -> Component.literal("§7  neoessentials." + category.getKey() + ".*"), false);
        }
        
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§eUse: §f/pex group <group> add <permission>"), false);
        source.sendSuccess(() -> Component.literal("§eExport: §f/neoessentials-permissions export pex"), false);
    }
    
    /**
     * Show PermissionsEX group command examples
     */
    private static void showGroupExamples(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6┌─ PermissionsEX Group Examples ─┐"), false);
        source.sendSuccess(() -> Component.literal("§eUse these commands to set up permission groups:"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§a▶ Admin Group (Full Access):"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group admin create"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group admin add neoessentials.*"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§a▶ Moderator Group:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group moderator create"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group moderator add neoessentials.teleport.*"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group moderator add neoessentials.chat.*"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group moderator add neoessentials.admin.permissions"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§a▶ Player Group (Basic):"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group player create"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group player add neoessentials.teleport.home.home"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group player add neoessentials.teleport.home.set"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group player add neoessentials.teleport.spawn.spawn"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group player add neoessentials.economy.balance"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group player add neoessentials.economy.pay"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group player add neoessentials.chat.msg"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group player add neoessentials.chat.reply"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§a▶ VIP Group:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group vip create"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group vip add neoessentials.teleport.*"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group vip add neoessentials.economy.*"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group vip add neoessentials.kits.*"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group vip add neoessentials.utility.*"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§b▶ Tab completion should work for:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group <groupname> add neoessentials.<TAB>"), false);
        source.sendSuccess(() -> Component.literal("§f/pex group <groupname> remove neoessentials.<TAB>"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§c▶ If tab completion doesn't work:"), false);
        source.sendSuccess(() -> Component.literal("§f1. Run: /neoessentials-permissions export pex"), false);
        source.sendSuccess(() -> Component.literal("§f2. Add at least one permission to any group"), false);
        source.sendSuccess(() -> Component.literal("§f3. Run: /pex reload"), false);
    }
    
    /**
     * Show PermissionsEX user command examples
     */
    private static void showUserExamples(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6┌─ PermissionsEX User Examples ─┐"), false);
        source.sendSuccess(() -> Component.literal("§eUse these commands to grant permissions to specific users:"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§a▶ Admin Permissions for Users:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.teleport.admin.tp"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.teleport.admin.tphere"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.teleport.admin.tpall"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.economy.eco.give"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.admin.reload"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§a▶ Moderator Permissions for Users:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.teleport.admin.tpo"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.chat.socialspy"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.chat.mute"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§a▶ Special Permissions for Users:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.teleport.home.others"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.economy.balance.others"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.kits.starter.nocooldown"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§a▶ Utility Permissions for Users:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.utility.repair"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.utility.afk"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName add neoessentials.utility.dispose"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§b▶ Tab completion should work for:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user <username> add neoessentials.<TAB>"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user <username> remove neoessentials.<TAB>"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        source.sendSuccess(() -> Component.literal("§c▶ Remove permissions from users:"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName remove neoessentials.teleport.admin.tp"), false);
        source.sendSuccess(() -> Component.literal("§f/pex user PlayerName remove neoessentials.*"), false);
    }
    
    /**
     * Initialize the permission bridge
     */
    public static void initialize() {
        LOGGER.info("Initializing NeoEssentials Permission Bridge...");
        
        // Initialize the registry and tab completer
        try {
            PermissionTabCompleter.initialize();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize tab completer", e);
        }
        
        LOGGER.info("Permission Bridge initialized with {} permissions available for tab completion", 
                   PermissionRegistry.getInstance().getAllPermissions().size());
    }
}