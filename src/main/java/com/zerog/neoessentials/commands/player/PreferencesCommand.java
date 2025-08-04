package com.zerog.neoessentials.commands.player;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zerog.neoessentials.player.PlayerDataManager;
import com.zerog.neoessentials.player.PlayerData;
import com.zerog.neoessentials.player.PlayerPreferences;
import com.zerog.neoessentials.util.MessageUtils;

import java.util.Map;

public class PreferencesCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("preferences")
            .requires(source -> source.hasPermission(0))
            .executes(PreferencesCommand::showPreferences)
            .then(Commands.literal("set")
                .then(Commands.argument("key", StringArgumentType.string())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes(PreferencesCommand::setPreference))))
            .then(Commands.literal("get")
                .then(Commands.argument("key", StringArgumentType.string())
                    .executes(PreferencesCommand::getPreference)))
            .then(Commands.literal("reset")
                .then(Commands.argument("key", StringArgumentType.string())
                    .executes(PreferencesCommand::resetPreference)))
            .then(Commands.literal("list")
                .executes(PreferencesCommand::listPreferences)));

        dispatcher.register(Commands.literal("prefs")
            .requires(source -> source.hasPermission(0))
            .executes(PreferencesCommand::showPreferences)
            .then(Commands.literal("set")
                .then(Commands.argument("key", StringArgumentType.string())
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes(PreferencesCommand::setPreference))))
            .then(Commands.literal("get")
                .then(Commands.argument("key", StringArgumentType.string())
                    .executes(PreferencesCommand::getPreference)))
            .then(Commands.literal("reset")
                .then(Commands.argument("key", StringArgumentType.string())
                    .executes(PreferencesCommand::resetPreference)))
            .then(Commands.literal("list")
                .executes(PreferencesCommand::listPreferences)));
    }

    private static int showPreferences(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            PlayerPreferences prefs = playerData.getPreferences();
            
            Component header = MessageUtils.format("&6&l==== &eYour Preferences &6&l====");
            player.sendSystemMessage(header);
            
            // General Preferences
            Component generalHeader = MessageUtils.format("&6&lGeneral:");
            player.sendSystemMessage(generalHeader);
            
            Component notifications = MessageUtils.format("  &eReceive Notifications: &f" + 
                (prefs.isReceiveNotifications() ? "&aEnabled" : "&cDisabled"));
            player.sendSystemMessage(notifications);
            
            Component announcements = MessageUtils.format("  &eReceive Announcements: &f" + 
                (prefs.isReceiveAnnouncements() ? "&aEnabled" : "&cDisabled"));
            player.sendSystemMessage(announcements);
            
            // GUI Preferences
            Component guiHeader = MessageUtils.format("&6&lGUI:");
            player.sendSystemMessage(guiHeader);
            
            Component theme = MessageUtils.format("  &eGUI Theme: &f" + prefs.getGuiTheme());
            player.sendSystemMessage(theme);
            
            Component animations = MessageUtils.format("  &eUse Animations: &f" + 
                (prefs.isUseAnimations() ? "&aEnabled" : "&cDisabled"));
            player.sendSystemMessage(animations);
            
            Component sounds = MessageUtils.format("  &ePlay Click Sounds: &f" + 
                (prefs.isPlayClickSounds() ? "&aEnabled" : "&cDisabled"));
            player.sendSystemMessage(sounds);
            
            // Chat Preferences
            Component chatHeader = MessageUtils.format("&6&lChat:");
            player.sendSystemMessage(chatHeader);
            
            Component format = MessageUtils.format("  &eChat Format: &f" + prefs.getChatFormat());
            player.sendSystemMessage(format);
            
            Component privateMessages = MessageUtils.format("  &ePrivate Messages: &f" + 
                (prefs.isEnablePrivateMessages() ? "&aEnabled" : "&cDisabled"));
            player.sendSystemMessage(privateMessages);
            
            Component colors = MessageUtils.format("  &eChat Colors: &f" + 
                (prefs.isEnableChatColors() ? "&aEnabled" : "&cDisabled"));
            player.sendSystemMessage(colors);
            
            Component timestamps = MessageUtils.format("  &eChat Timestamps: &f" + 
                (prefs.isEnableChatTimestamps() ? "&aEnabled" : "&cDisabled"));
            player.sendSystemMessage(timestamps);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing preferences for player " + player.getName().getString(), e);
            Component error = MessageUtils.format("&cError retrieving preferences.");
            player.sendSystemMessage(error);
            return 0;
        }
    }

    private static int setPreference(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            PlayerPreferences prefs = playerData.getPreferences();
            
            boolean success = setPreferenceValue(prefs, key, value);
            
            if (success) {
                PlayerDataManager.getInstance().savePlayerData(playerData);
                
                Component message = MessageUtils.format("&aPreference &e" + key + " &aset to &f" + value);
                player.sendSystemMessage(message);
                return 1;
            } else {
                Component error = MessageUtils.format("&cInvalid preference key or value: &f" + key + " = " + value);
                player.sendSystemMessage(error);
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error setting preference for player " + player.getName().getString(), e);
            Component error = MessageUtils.format("&cError setting preference.");
            player.sendSystemMessage(error);
            return 0;
        }
    }

    private static int getPreference(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, "key");
        
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            PlayerPreferences prefs = playerData.getPreferences();
            
            String value = getPreferenceValue(prefs, key);
            
            if (value != null) {
                Component message = MessageUtils.format("&ePreference &f" + key + " &eis set to: &f" + value);
                player.sendSystemMessage(message);
                return 1;
            } else {
                Component error = MessageUtils.format("&cPreference &f" + key + " &cnot found.");
                player.sendSystemMessage(error);
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error getting preference for player " + player.getName().getString(), e);
            Component error = MessageUtils.format("&cError getting preference.");
            player.sendSystemMessage(error);
            return 0;
        }
    }

    private static int resetPreference(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, "key");
        
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            PlayerPreferences prefs = playerData.getPreferences();
            
            boolean success = resetPreferenceValue(prefs, key);
            
            if (success) {
                PlayerDataManager.getInstance().savePlayerData(playerData);
                
                Component message = MessageUtils.format("&aPreference &e" + key + " &areset to default value.");
                player.sendSystemMessage(message);
                return 1;
            } else {
                Component error = MessageUtils.format("&cInvalid preference key: &f" + key);
                player.sendSystemMessage(error);
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error resetting preference for player " + player.getName().getString(), e);
            Component error = MessageUtils.format("&cError resetting preference.");
            player.sendSystemMessage(error);
            return 0;
        }
    }

    private static int listPreferences(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            PlayerPreferences prefs = playerData.getPreferences();
            
            Component header = MessageUtils.format("&6&l==== &eAvailable Preferences &6&l====");
            player.sendSystemMessage(header);
            
            Component generalHeader = MessageUtils.format("&6&lGeneral:");
            player.sendSystemMessage(generalHeader);
            player.sendSystemMessage(MessageUtils.format("  &ereceiveNotifications &7- &fEnable/disable notifications"));
            player.sendSystemMessage(MessageUtils.format("  &ereceiveAnnouncements &7- &fEnable/disable announcements"));
            
            Component guiHeader = MessageUtils.format("&6&lGUI:");
            player.sendSystemMessage(guiHeader);
            player.sendSystemMessage(MessageUtils.format("  &eguiTheme &7- &fSet GUI theme (dark/light/auto)"));
            player.sendSystemMessage(MessageUtils.format("  &euseAnimations &7- &fEnable/disable GUI animations"));
            player.sendSystemMessage(MessageUtils.format("  &eplayClickSounds &7- &fEnable/disable click sounds"));
            
            Component chatHeader = MessageUtils.format("&6&lChat:");
            player.sendSystemMessage(chatHeader);
            player.sendSystemMessage(MessageUtils.format("  &echatFormat &7- &fSet chat format"));
            player.sendSystemMessage(MessageUtils.format("  &eenablePrivateMessages &7- &fEnable/disable private messages"));
            player.sendSystemMessage(MessageUtils.format("  &eenableChatColors &7- &fEnable/disable chat colors"));
            player.sendSystemMessage(MessageUtils.format("  &eenableChatTimestamps &7- &fEnable/disable timestamps"));
            
            // Show custom preferences
            Map<String, Object> customPrefs = prefs.getCustomPreferences();
            if (!customPrefs.isEmpty()) {
                Component customHeader = MessageUtils.format("&6&lCustom:");
                player.sendSystemMessage(customHeader);
                for (String customKey : customPrefs.keySet()) {
                    player.sendSystemMessage(MessageUtils.format("  &e" + customKey + " &7- &fCustom preference"));
                }
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error listing preferences for player " + player.getName().getString(), e);
            Component error = MessageUtils.format("&cError listing preferences.");
            player.sendSystemMessage(error);
            return 0;
        }
    }

    private static String getPreferenceValue(PlayerPreferences prefs, String key) {
        switch (key.toLowerCase()) {
            case "receivenotifications":
                return String.valueOf(prefs.isReceiveNotifications());
            case "receiveannouncements":
                return String.valueOf(prefs.isReceiveAnnouncements());
            case "guitheme":
                return prefs.getGuiTheme();
            case "useanimations":
                return String.valueOf(prefs.isUseAnimations());
            case "playclicksounds":
                return String.valueOf(prefs.isPlayClickSounds());
            case "chatformat":
                return prefs.getChatFormat();
            case "enableprivatemessages":
                return String.valueOf(prefs.isEnablePrivateMessages());
            case "enablechatcolors":
                return String.valueOf(prefs.isEnableChatColors());
            case "enablechattimestamps":
                return String.valueOf(prefs.isEnableChatTimestamps());
            case "enableteleporteffects":
                return String.valueOf(prefs.isEnableTeleportEffects());
            case "enableteleportsounds":
                return String.valueOf(prefs.isEnableTeleportSounds());
            case "autoaccepttpa":
                return String.valueOf(prefs.isAutoAcceptTPA());
            case "enableeconomynotifications":
                return String.valueOf(prefs.isEnableEconomyNotifications());
            default:
                Object custom = prefs.getCustomPreference(key);
                return custom != null ? String.valueOf(custom) : null;
        }
    }

    private static boolean setPreferenceValue(PlayerPreferences prefs, String key, String value) {
        try {
            switch (key.toLowerCase()) {
                case "receivenotifications":
                    prefs.setReceiveNotifications(Boolean.parseBoolean(value));
                    return true;
                case "receiveannouncements":
                    prefs.setReceiveAnnouncements(Boolean.parseBoolean(value));
                    return true;
                case "guitheme":
                    prefs.setGuiTheme(value);
                    return true;
                case "useanimations":
                    prefs.setUseAnimations(Boolean.parseBoolean(value));
                    return true;
                case "playclicksounds":
                    prefs.setPlayClickSounds(Boolean.parseBoolean(value));
                    return true;
                case "chatformat":
                    prefs.setChatFormat(value);
                    return true;
                case "enableprivatemessages":
                    prefs.setEnablePrivateMessages(Boolean.parseBoolean(value));
                    return true;
                case "enablechatcolors":
                    prefs.setEnableChatColors(Boolean.parseBoolean(value));
                    return true;
                case "enablechattimestamps":
                    prefs.setEnableChatTimestamps(Boolean.parseBoolean(value));
                    return true;
                case "enableteleporteffects":
                    prefs.setEnableTeleportEffects(Boolean.parseBoolean(value));
                    return true;
                case "enableteleportsounds":
                    prefs.setEnableTeleportSounds(Boolean.parseBoolean(value));
                    return true;
                case "autoaccepttpa":
                    prefs.setAutoAcceptTPA(Boolean.parseBoolean(value));
                    return true;
                case "enableeconomynotifications":
                    prefs.setEnableEconomyNotifications(Boolean.parseBoolean(value));
                    return true;
                default:
                    // Custom preference
                    Object convertedValue;
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        convertedValue = Boolean.parseBoolean(value);
                    } else {
                        try {
                            convertedValue = Integer.parseInt(value);
                        } catch (NumberFormatException e1) {
                            try {
                                convertedValue = Double.parseDouble(value);
                            } catch (NumberFormatException e2) {
                                convertedValue = value;
                            }
                        }
                    }
                    prefs.setCustomPreference(key, convertedValue);
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean resetPreferenceValue(PlayerPreferences prefs, String key) {
        switch (key.toLowerCase()) {
            case "receivenotifications":
                prefs.setReceiveNotifications(true);
                return true;
            case "receiveannouncements":
                prefs.setReceiveAnnouncements(true);
                return true;
            case "guitheme":
                prefs.setGuiTheme("auto");
                return true;
            case "useanimations":
                prefs.setUseAnimations(true);
                return true;
            case "playclicksounds":
                prefs.setPlayClickSounds(true);
                return true;
            case "chatformat":
                prefs.setChatFormat("default");
                return true;
            case "enableprivatemessages":
                prefs.setEnablePrivateMessages(true);
                return true;
            case "enablechatcolors":
                prefs.setEnableChatColors(true);
                return true;
            case "enablechattimestamps":
                prefs.setEnableChatTimestamps(false);
                return true;
            case "enableteleporteffects":
                prefs.setEnableTeleportEffects(true);
                return true;
            case "enableteleportsounds":
                prefs.setEnableTeleportSounds(true);
                return true;
            case "autoaccepttpa":
                prefs.setAutoAcceptTPA(false);
                return true;
            case "enableeconomynotifications":
                prefs.setEnableEconomyNotifications(true);
                return true;
            default:
                Object existing = prefs.getCustomPreference(key);
                if (existing != null) {
                    prefs.removeCustomPreference(key);
                    return true;
                } else {
                    return false;
                }
        }
    }
}
