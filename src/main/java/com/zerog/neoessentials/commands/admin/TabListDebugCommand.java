package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.features.TabListManager;
import com.zerog.neoessentials.util.ColorUtil;
import com.zerog.neoessentials.util.DebugUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Debug command for TabList system to help troubleshoot conflicts
 */
public class TabListDebugCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tablistdebug")
            .requires(source -> source.hasPermission(2)) // Admin only
            .then(Commands.literal("status")
                .executes(TabListDebugCommand::showStatus))
            .then(Commands.literal("refresh")
                .executes(TabListDebugCommand::refreshTablist))
            .then(Commands.literal("layout")
                .executes(TabListDebugCommand::showPlayerLayout))
        );
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            TabListManager manager = TabListManager.getInstance();
            if (manager == null) {
                source.sendSuccess(() -> ColorUtil.colorize("&cTabListManager is not initialized!"), false);
                return 0;
            }
            
            String debugInfo = manager.getDebugInfo();
            String[] lines = debugInfo.split("\n");
            
            source.sendSuccess(() -> ColorUtil.colorize("&a&l=== TabList Debug Status ==="), false);
            for (String line : lines) {
                source.sendSuccess(() -> ColorUtil.colorize("&7" + line), false);
            }
            
            // Check for conflicts
            if (manager.hasActiveConfigLayouts()) {
                source.sendSuccess(() -> ColorUtil.colorize("&a✓ Config-based layouts active - no conflicts expected"), false);
            } else {
                source.sendSuccess(() -> ColorUtil.colorize("&e⚠ No config layouts found - check configuration"), false);
            }
            
        } catch (Exception e) {
            source.sendSuccess(() -> ColorUtil.colorize("&cError getting debug info: " + e.getMessage()), false);
            DebugUtil.errorLog("TabListDebugCommand error: " + e.getMessage());
        }
        
        return 1;
    }
    
    private static int refreshTablist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            TabListManager manager = TabListManager.getInstance();
            if (manager == null) {
                source.sendSuccess(() -> ColorUtil.colorize("&cTabListManager is not initialized!"), false);
                return 0;
            }
            
            manager.reloadConfig();
            source.sendSuccess(() -> ColorUtil.colorize("&aTabList configuration reloaded and refreshed for all players"), false);
            
        } catch (Exception e) {
            source.sendSuccess(() -> ColorUtil.colorize("&cError refreshing tablist: " + e.getMessage()), false);
            DebugUtil.errorLog("TabListDebugCommand refresh error: " + e.getMessage());
        }
        
        return 1;
    }
    
    private static int showPlayerLayout(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendSuccess(() -> ColorUtil.colorize("&cThis command can only be used by players"), false);
                return 0;
            }
            
            TabListManager manager = TabListManager.getInstance();
            if (manager == null) {
                source.sendSuccess(() -> ColorUtil.colorize("&cTabListManager is not initialized!"), false);
                return 0;
            }
            
            // Get player's current layout info
            source.sendSuccess(() -> ColorUtil.colorize("&a&l=== Your TabList Layout Info ==="), false);
            source.sendSuccess(() -> ColorUtil.colorize("&7Player: &f" + player.getName().getString()), false);
            
            if (manager.config != null && manager.config.tablist != null && manager.config.tablist.permissionSets != null) {
                source.sendSuccess(() -> ColorUtil.colorize("&7Permission Sets Available:"), false);
                for (var entry : manager.config.tablist.permissionSets.entrySet()) {
                    var permSet = entry.getValue();
                    boolean hasPermission = permSet.permission == null || permSet.permission.isEmpty() || 
                                          player.hasPermissions(2) || // Admin fallback
                                          source.hasPermission(2); // Source permission check
                    
                    String status = hasPermission ? "&a✓" : "&c✗";
                    source.sendSuccess(() -> ColorUtil.colorize("&7  " + status + " &f" + entry.getKey() + 
                        " &7(priority: " + permSet.priority + ", layout: " + permSet.layoutId + ")"), false);
                }
            }
            
        } catch (Exception e) {
            source.sendSuccess(() -> ColorUtil.colorize("&cError getting layout info: " + e.getMessage()), false);
            DebugUtil.errorLog("TabListDebugCommand layout error: " + e.getMessage());
        }
        
        return 1;
    }
}
