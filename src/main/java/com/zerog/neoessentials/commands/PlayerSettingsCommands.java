package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.PlayerSettingsManager;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles player settings and preferences commands
 */
public class PlayerSettingsCommands {
    
    /**
     * Registers the player settings commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // Main settings command
        dispatcher.register(Commands.literal("settings")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.settings"))
            .executes(PlayerSettingsCommands::showSettingsMenu)
            .then(Commands.literal("help")
                .executes(PlayerSettingsCommands::showSettingsHelp)
            )
            .then(Commands.literal("reset")
                .executes(PlayerSettingsCommands::resetSettings)
            )
            .then(Commands.literal("export")
                .executes(PlayerSettingsCommands::exportSettings)
            )
            .then(Commands.literal("teleport")
                .then(Commands.literal("auto-record")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setAutoRecordTeleports)
                    )
                    .executes(PlayerSettingsCommands::toggleAutoRecordTeleports)
                )
                .then(Commands.literal("confirmations")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setTeleportConfirmations)
                    )
                    .executes(PlayerSettingsCommands::toggleTeleportConfirmations)
                )
                .then(Commands.literal("history-limit")
                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                        .executes(PlayerSettingsCommands::setHistoryLimit)
                    )
                    .executes(PlayerSettingsCommands::showHistoryLimit)
                )
                .then(Commands.literal("messages")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setTeleportMessages)
                    )
                    .executes(PlayerSettingsCommands::toggleTeleportMessages)
                )
            )
            .then(Commands.literal("gui")
                .then(Commands.literal("prefer")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setPreferGUI)
                    )
                    .executes(PlayerSettingsCommands::togglePreferGUI)
                )
                .then(Commands.literal("theme")
                    .then(Commands.argument("theme", StringArgumentType.string())
                        .executes(PlayerSettingsCommands::setGUITheme)
                    )
                    .executes(PlayerSettingsCommands::showGUITheme)
                )
            )
            .then(Commands.literal("messages")
                .then(Commands.literal("economy")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setEconomyMessages)
                    )
                    .executes(PlayerSettingsCommands::toggleEconomyMessages)
                )
                .then(Commands.literal("system")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setSystemMessages)
                    )
                    .executes(PlayerSettingsCommands::toggleSystemMessages)
                )
            )
            .then(Commands.literal("economy")
                .then(Commands.literal("auto-confirm")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setAutoPaymentConfirmations)
                    )
                    .executes(PlayerSettingsCommands::toggleAutoPaymentConfirmations)
                )
                .then(Commands.literal("currency")
                    .then(Commands.argument("currency", StringArgumentType.string())
                        .executes(PlayerSettingsCommands::setPreferredCurrency)
                    )
                    .executes(PlayerSettingsCommands::showPreferredCurrency)
                )
            )
            .then(Commands.literal("privacy")
                .then(Commands.literal("teleport-requests")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setAllowTeleportRequests)
                    )
                    .executes(PlayerSettingsCommands::toggleAllowTeleportRequests)
                )
                .then(Commands.literal("online-status")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setShowOnlineStatus)
                    )
                    .executes(PlayerSettingsCommands::toggleShowOnlineStatus)
                )
                .then(Commands.literal("player-info")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(PlayerSettingsCommands::setAllowPlayerInfo)
                    )
                    .executes(PlayerSettingsCommands::toggleAllowPlayerInfo)
                )
            )
        );
        
        // Preference aliases
        dispatcher.register(Commands.literal("prefs")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.settings"))
            .executes(PlayerSettingsCommands::showSettingsMenu)
        );
        
        dispatcher.register(Commands.literal("preferences")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.settings"))
            .executes(PlayerSettingsCommands::showSettingsMenu)
        );
    }
    
    /**
     * Shows the main settings menu
     */
    private static int showSettingsMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        LanguageUtil.sendMessage(player, "settings.menu.header");
        LanguageUtil.sendMessage(player, "");
        
        // Teleport settings
        LanguageUtil.sendMessage(player, "settings.menu.teleport.header");
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isAutoRecordTeleports());
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isTeleportConfirmations());
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.getMaxTeleportHistory());
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isShowTeleportMessages());
        LanguageUtil.sendMessage(player, "");
        
        // GUI settings
        LanguageUtil.sendMessage(player, "settings.menu.gui.header");
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isPreferGUIInterfaces());
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.getGuiTheme());
        LanguageUtil.sendMessage(player, "");
        
        // Message settings
        LanguageUtil.sendMessage(player, "settings.menu.messages.header");
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isShowEconomyMessages());
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isShowSystemMessages());
        LanguageUtil.sendMessage(player, "");
        
        // Economy settings
        LanguageUtil.sendMessage(player, "settings.menu.economy.header");
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isAutoPaymentConfirmations());
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.getPreferredCurrency());
        LanguageUtil.sendMessage(player, "");
        
        // Privacy settings
        LanguageUtil.sendMessage(player, "settings.menu.privacy.header");
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isAllowTeleportRequests());
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isShowOnlineStatus());
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.isAllowPlayerInfo());
        LanguageUtil.sendMessage(player, "");
        
        LanguageUtil.sendMessage(player, "settings.menu.footer");
        
        return 1;
    }
    
    /**
     * Shows settings help
     */
    private static int showSettingsHelp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        LanguageUtil.sendMessage(player, "settings.help.header");
        LanguageUtil.sendMessage(player, "settings.help.usage");
        LanguageUtil.sendMessage(player, "settings.help.examples");
        LanguageUtil.sendMessage(player, "settings.help.footer");
        
        return 1;
    }
    
    /**
     * Resets player settings to defaults
     */
    private static int resetSettings(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        
        settingsManager.updatePlayerSettings(player, new PlayerSettingsManager.PlayerSettings());
        LanguageUtil.sendSuccessMessage(player, "settings.reset.success");
        
        return 1;
    }
    
    /**
     * Exports player settings
     */
    private static int exportSettings(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        // Create a summary of current settings
        StringBuilder export = new StringBuilder();
        export.append("=== Player Settings Export ===\n");
        export.append("Teleport Settings:\n");
        export.append("  Auto-record: ").append(settings.isAutoRecordTeleports()).append("\n");
        export.append("  Confirmations: ").append(settings.isTeleportConfirmations()).append("\n");
        export.append("  History Limit: ").append(settings.getMaxTeleportHistory()).append("\n");
        export.append("  Messages: ").append(settings.isShowTeleportMessages()).append("\n");
        export.append("GUI Settings:\n");
        export.append("  Prefer GUI: ").append(settings.isPreferGUIInterfaces()).append("\n");
        export.append("  Theme: ").append(settings.getGuiTheme()).append("\n");
        export.append("Message Settings:\n");
        export.append("  Economy: ").append(settings.isShowEconomyMessages()).append("\n");
        export.append("  System: ").append(settings.isShowSystemMessages()).append("\n");
        export.append("Economy Settings:\n");
        export.append("  Auto-confirm: ").append(settings.isAutoPaymentConfirmations()).append("\n");
        export.append("  Currency: ").append(settings.getPreferredCurrency()).append("\n");
        export.append("Privacy Settings:\n");
        export.append("  Teleport Requests: ").append(settings.isAllowTeleportRequests()).append("\n");
        export.append("  Online Status: ").append(settings.isShowOnlineStatus()).append("\n");
        export.append("  Player Info: ").append(settings.isAllowPlayerInfo()).append("\n");
        
        LanguageUtil.sendMessage(player, "settings.export.header");
        for (String line : export.toString().split("\n")) {
            LanguageUtil.sendMessage(player, line);
        }
        
        return 1;
    }
    
    // Individual setting methods
    private static int setAutoRecordTeleports(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setAutoRecordTeleports(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aAuto-record teleports set to: " + enabled);
        return 1;
    }
    
    private static int toggleAutoRecordTeleports(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isAutoRecordTeleports();
        settings.setAutoRecordTeleports(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    private static int setTeleportConfirmations(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setTeleportConfirmations(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int toggleTeleportConfirmations(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isTeleportConfirmations();
        settings.setTeleportConfirmations(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    private static int setHistoryLimit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int limit = IntegerArgumentType.getInteger(context, "limit");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setMaxTeleportHistory(limit);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + limit);
        return 1;
    }
    
    private static int showHistoryLimit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.getMaxTeleportHistory());
        return 1;
    }
    
    private static int setTeleportMessages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setShowTeleportMessages(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int toggleTeleportMessages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isShowTeleportMessages();
        settings.setShowTeleportMessages(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    // GUI Settings
    private static int setPreferGUI(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setPreferGUIInterfaces(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int togglePreferGUI(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isPreferGUIInterfaces();
        settings.setPreferGUIInterfaces(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    private static int setGUITheme(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String theme = StringArgumentType.getString(context, "theme");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setGuiTheme(theme);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + theme);
        return 1;
    }
    
    private static int showGUITheme(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.getGuiTheme());
        return 1;
    }
    
    // Message Settings
    private static int setEconomyMessages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setShowEconomyMessages(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int toggleEconomyMessages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isShowEconomyMessages();
        settings.setShowEconomyMessages(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    private static int setSystemMessages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setShowSystemMessages(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int toggleSystemMessages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isShowSystemMessages();
        settings.setShowSystemMessages(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    // Economy Settings
    private static int setAutoPaymentConfirmations(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setAutoPaymentConfirmations(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int toggleAutoPaymentConfirmations(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isAutoPaymentConfirmations();
        settings.setAutoPaymentConfirmations(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    private static int setPreferredCurrency(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String currency = StringArgumentType.getString(context, "currency");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setPreferredCurrency(currency);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + currency);
        return 1;
    }
    
    private static int showPreferredCurrency(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        
        LanguageUtil.sendMessage(player, "§7Current value: " + settings.getPreferredCurrency());
        return 1;
    }
    
    // Privacy Settings
    private static int setAllowTeleportRequests(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setAllowTeleportRequests(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int toggleAllowTeleportRequests(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isAllowTeleportRequests();
        settings.setAllowTeleportRequests(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    private static int setShowOnlineStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setShowOnlineStatus(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int toggleShowOnlineStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isShowOnlineStatus();
        settings.setShowOnlineStatus(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
    
    private static int setAllowPlayerInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        settings.setAllowPlayerInfo(enabled);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + enabled);
        return 1;
    }
    
    private static int toggleAllowPlayerInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        PlayerSettingsManager settingsManager = NeoEssentials.getInstance().getDataManager().getPlayerSettingsManager();
        PlayerSettingsManager.PlayerSettings settings = settingsManager.getPlayerSettings(player);
        boolean newValue = !settings.isAllowPlayerInfo();
        settings.setAllowPlayerInfo(newValue);
        settingsManager.updatePlayerSettings(player, settings);
        
        LanguageUtil.sendMessage(player, "§aSetting updated: " + newValue);
        return 1;
    }
}
