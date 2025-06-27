package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.TextUtil;
import com.zerog.neoessentials.utils.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Commands to manage tablist functionality
 */
public class TablistCommand {

    /**
     * Register the command
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("tablist")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tablist"))
            .then(Commands.literal("reload")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tablist.reload"))
                .executes(TablistCommand::executeReload))
            .then(Commands.literal("reset")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tablist.reset"))
                .executes(TablistCommand::executeReset))
            .then(Commands.literal("debug")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tablist.debug"))
                .executes(TablistCommand::executeDebug))
            .executes(TablistCommand::executeHelp);
        
        dispatcher.register(command);
    }
    
    /**
     * Execute the reload command - reloads tablist configuration
     *
     * @param context The command context
     * @return The result
     * @throws CommandSyntaxException If there's a syntax error
     */    private static int executeReload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Get tablist manager
        var dataManager = NeoEssentials.getInstance().getDataManager();
        if (dataManager == null) {
            source.sendFailure(Component.literal(TextUtil.colorize("&cError: Data manager not initialized")));
            return 0;
        }
        
        var tablistManager = dataManager.getTablistManager();
        if (tablistManager == null) {
            source.sendFailure(Component.literal(TextUtil.colorize("&cError: Tablist manager not initialized")));
            return 0;
        }
        
        // Reload the tablist templates using DataManagerHooks
        boolean success = com.zerog.neoessentials.ui.tab.DataManagerHooks.reloadTemplates();
        
        if (success) {
            source.sendSuccess(() -> Component.literal(TextUtil.colorize("&aTablist configuration reloaded successfully")), true);
            return 1;
        } else {
            source.sendFailure(Component.literal(TextUtil.colorize("&cFailed to reload tablist configuration")));
            return 0;
        }
    }
    
    /**
     * Execute the reset command - resets tablist configuration to defaults
     *
     * @param context The command context
     * @return The result
     * @throws CommandSyntaxException If there's a syntax error
     */    private static int executeReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Get tablist manager
        var dataManager = NeoEssentials.getInstance().getDataManager();
        if (dataManager == null) {
            source.sendFailure(Component.literal(TextUtil.colorize("&cError: Data manager not initialized")));
            return 0;
        }
        
        var tablistManager = dataManager.getTablistManager();
        if (tablistManager == null) {
            source.sendFailure(Component.literal(TextUtil.colorize("&cError: Tablist manager not initialized")));
            return 0;
        }
        
        // Force extract default config and reload
        boolean forceCreateSuccess = tablistManager.getTemplateManager().createDefaultTemplatesFile();
        boolean reloadSuccess = com.zerog.neoessentials.ui.tab.DataManagerHooks.reloadTemplates();
        
        if (forceCreateSuccess && reloadSuccess) {
            source.sendSuccess(() -> Component.literal(TextUtil.colorize("&aTablist configuration reset to defaults and reloaded successfully")), true);
            return 1;
        } else {
            source.sendFailure(Component.literal(TextUtil.colorize("&cFailed to reset tablist configuration")));
            return 0;
        }
    }
    
    /**
     * Execute the debug command - toggles tablist debug mode
     *
     * @param context The command context
     * @return The result
     * @throws CommandSyntaxException If there's a syntax error
     */
    private static int executeDebug(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        boolean isDebug = !NeoEssentials.LOGGER.isDebugEnabled();
        
        // Toggle debug mode
        java.util.logging.Logger.getLogger(NeoEssentials.MODID).setLevel(
            isDebug ? java.util.logging.Level.FINE : java.util.logging.Level.INFO
        );
        
        source.sendSuccess(() -> Component.literal(TextUtil.colorize(
            isDebug ? "&aTablist debug mode enabled" : "&7Tablist debug mode disabled"
        )), true);
        
        return 1;
    }
    
    /**
     * Execute the help command
     *
     * @param context The command context
     * @return The result
     * @throws CommandSyntaxException If there's a syntax error
     */
    private static int executeHelp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        boolean isAdmin = PermissionUtil.hasPermission(source, "neoessentials.command.tablist.reload");
        
        source.sendSuccess(() -> Component.literal(TextUtil.colorize("&2==== &a&lNeoEssentials Tablist Commands &2====")), false);
        
        if (isAdmin) {
            source.sendSuccess(() -> Component.literal(TextUtil.colorize("&a/tablist reload &7- Reload the tablist configuration")), false);
            source.sendSuccess(() -> Component.literal(TextUtil.colorize("&a/tablist reset &7- Reset tablist config to defaults")), false);
            source.sendSuccess(() -> Component.literal(TextUtil.colorize("&a/tablist debug &7- Toggle tablist debug mode")), false);
        }
        
        source.sendSuccess(() -> Component.literal(TextUtil.colorize("&7Available placeholders: %online%, %max%, %time%, %uptime%, %memory_used%, %tps%, etc.")), false);
        
        return 1;
    }
}
