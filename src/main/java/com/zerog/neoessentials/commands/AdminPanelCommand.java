package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.AdminPanel;
import com.zerog.neoessentials.utils.MessageUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Implements commands for accessing the admin panel interface.
 */
public class AdminPanelCommand {

    /**
     * Registers all admin panel commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main admin panel command
        LiteralArgumentBuilder<CommandSourceStack> adminPanelCommand = Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel"))
                .executes(this::executeAdminPanel);

        // Register aliases
        dispatcher.register(adminPanelCommand);
        dispatcher.register(Commands.literal("ap")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel"))
                .executes(this::executeAdminPanel));
        
        // Admin panel sections
        registerSectionCommands(dispatcher);
    }
    
    /**
     * Registers commands for specific admin panel sections.
     * 
     * @param dispatcher The command dispatcher
     */
    private void registerSectionCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /adminpanel economy - Opens the economy section of the admin panel
        dispatcher.register(Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.economy"))
                .then(Commands.literal("economy")
                        .executes(this::executeEconomyPanel)));
                        
        // /adminpanel kits - Opens the kits section of the admin panel
        dispatcher.register(Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.kits"))
                .then(Commands.literal("kits")
                        .executes(this::executeKitsPanel)));
                        
        // /adminpanel warps - Opens the warps section of the admin panel
        dispatcher.register(Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.warps"))
                .then(Commands.literal("warps")
                        .executes(this::executeWarpsPanel)));
                        
        // /adminpanel players - Opens the players section of the admin panel
        dispatcher.register(Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.players"))
                .then(Commands.literal("players")
                        .executes(this::executePlayersPanel)));
    }

    /**
     * Executes the main admin panel command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeAdminPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Check if player has permission
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the admin panel.");
            return 0;
        }
        
        // Open the main admin panel
        displayMainAdminPanel(player);
        
        return 1;
    }
    
    /**
     * Displays the main admin panel interface with clickable options.
     * 
     * @param player The player to show the panel to
     */
    private void displayMainAdminPanel(ServerPlayer player) {
        NeoEssentials.LOGGER.info("Displaying admin panel for player: {}", player.getScoreboardName());
        
        // Create the header
        Component header = Component.literal(MessageUtil.translateColorCodes("&6====== &lNeoEssentials Admin Panel&r &6======"));
        player.sendSystemMessage(header);
        
        // Create clickable sections based on permissions
        if (CommandManager.hasPermission(player, "neoessentials.adminpanel.economy")) {
            displaySectionButton(player, "&2Economy Management", "/adminpanel economy", 
                    "&7Click to manage economy settings, view transactions,\n&7set balances, and view leaderboards.");
        }
        
        if (CommandManager.hasPermission(player, "neoessentials.adminpanel.kits")) {
            displaySectionButton(player, "&3Kit Management", "/adminpanel kits", 
                    "&7Click to manage kits, create new kits,\n&7edit existing kits, and view usage statistics.");
        }
        
        if (CommandManager.hasPermission(player, "neoessentials.adminpanel.warps")) {
            displaySectionButton(player, "&5Warp Management", "/adminpanel warps", 
                    "&7Click to manage warps, create new warps,\n&7edit existing warps, and set permissions.");
        }
        
        if (CommandManager.hasPermission(player, "neoessentials.adminpanel.players")) {
            displaySectionButton(player, "&6Player Management", "/adminpanel players", 
                    "&7Click to manage players, view online players,\n&7check player stats, and perform admin actions.");
        }
        
        // Create footer
        Component footer = Component.literal(MessageUtil.translateColorCodes("&6==================================="));
        player.sendSystemMessage(footer);
    }
    
    /**
     * Displays a clickable button for an admin panel section.
     * 
     * @param player The player to show the button to
     * @param title The title of the section
     * @param command The command to run when clicked
     * @param hoverText The hover text to display
     */
    private void displaySectionButton(ServerPlayer player, String title, String command, String hoverText) {
        Component buttonText = Component.literal(MessageUtil.translateColorCodes("&8[&r " + title + " &8]"));
        Component hoverComponent = Component.literal(MessageUtil.translateColorCodes(hoverText));
        
        // Make the button clickable and add hover text
        Component clickableButton = MessageUtil.makeClickableCommand(
                (Component.literal("➤ ").append(buttonText)).copy(), command)
                .withStyle(style -> style.withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, 
                        hoverComponent)));
        
        player.sendSystemMessage(clickableButton);
    }
    
    /**
     * Executes the economy panel command.
     * 
     * @param context The command context
     * @return 1 if successful
     */
    private int executeEconomyPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel.economy")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the economy admin panel.");
            return 0;
        }
        
        // Display economy management options
        AdminPanel.displayEconomyPanel(player);
        
        return 1;
    }
    
    /**
     * Executes the kits panel command.
     * 
     * @param context The command context
     * @return 1 if successful
     */
    private int executeKitsPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel.kits")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the kit admin panel.");
            return 0;
        }
        
        // Display kit management options
        AdminPanel.displayKitsPanel(player);
        
        return 1;
    }
    
    /**
     * Executes the warps panel command.
     * 
     * @param context The command context
     * @return 1 if successful
     */
    private int executeWarpsPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel.warps")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the warps admin panel.");
            return 0;
        }
        
        // Display warp management options
        AdminPanel.displayWarpsPanel(player);
        
        return 1;
    }
    
    /**
     * Executes the players panel command.
     * 
     * @param context The command context
     * @return 1 if successful
     */
    private int executePlayersPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel.players")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the players admin panel.");
            return 0;
        }
        
        // Display player management options
        AdminPanel.displayPlayersPanel(player);
        
        return 1;
    }
}
