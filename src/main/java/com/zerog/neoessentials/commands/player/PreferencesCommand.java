package com.zerog.neoessentials.commands.player;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

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
import com.zerog.neoessentials.util.MessageUtil;

import java.util.Map;

public class PreferencesCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("preferences")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.PLAYER_DEFAULT))
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
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.PLAYER_DEFAULT))
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
            
                Component header = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.header"));
            player.sendSystemMessage(header);
            
            // General Preferences
                Component generalHeader = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.general.header"));
            player.sendSystemMessage(generalHeader);
            
                Component notifications = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.general.notifications", prefs.isReceiveNotifications()));
            player.sendSystemMessage(notifications);
            
                Component announcements = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.general.announcements", prefs.isReceiveAnnouncements()));
            player.sendSystemMessage(announcements);
            
            // GUI Preferences
                Component guiHeader = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.gui.header"));
            player.sendSystemMessage(guiHeader);
            
            
                Component animations = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.gui.animations", prefs.isUseAnimations()));
            player.sendSystemMessage(animations);
            
                Component sounds = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.gui.sounds", prefs.isPlayClickSounds()));
            player.sendSystemMessage(sounds);
            
            // Chat Preferences
                Component chatHeader = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.chat.header"));
            player.sendSystemMessage(chatHeader);
            
                Component format = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.chat.format", prefs.getChatFormat()));
            player.sendSystemMessage(format);
            
                Component privateMessages = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.chat.private_messages", prefs.isEnablePrivateMessages()));
            player.sendSystemMessage(privateMessages);
            
                Component colors = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.chat.colors", prefs.isEnableChatColors()));
            player.sendSystemMessage(colors);
            
                Component timestamps = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.chat.timestamps", prefs.isEnableChatTimestamps()));
            player.sendSystemMessage(timestamps);
            
            return 1;
        } catch (Exception e) {
                Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.error.retrieve"));
            // Error message already declared above, remove duplicate
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
                Component message = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.set.success", key, value));
                player.sendSystemMessage(message);
                return 1;
            } else {
                Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.set.invalid", key, value));
                player.sendSystemMessage(error);
                return 0;
            }
        } catch (Exception e) {
            Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.error.set"));
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
                Component message = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.get.success", key, value));
                player.sendSystemMessage(message);
                return 1;
            } else {
                player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.get.not_found", key)));
                return 0;
            }
        } catch (Exception e) {
            Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.error.get"));
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
                Component message = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.reset.success", key));
                player.sendSystemMessage(message);
                return 1;
            } else {
                Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.reset.invalid", key));
                player.sendSystemMessage(error);
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error resetting preference for player " + player.getName().getString(), e);
            Component error = MessageUtil.format("&cError resetting preference.");
            player.sendSystemMessage(error);
            return 0;
        }
    }

    private static int listPreferences(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        try {
            PlayerData playerData = PlayerDataManager.getInstance().getPlayerData(player.getUUID());
            PlayerPreferences prefs = playerData.getPreferences();
            Component header = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.header"));
            player.sendSystemMessage(header);
            Component generalHeader = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.general.header"));
            player.sendSystemMessage(generalHeader);
            player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.general.receiveNotifications")));
            player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.general.receiveAnnouncements")));
            Component guiHeader = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.gui.header"));
            player.sendSystemMessage(guiHeader);
            player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.gui.useAnimations")));
            player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.gui.playClickSounds")));
            Component chatHeader = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.chat.header"));
            player.sendSystemMessage(chatHeader);
            player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.chat.chatFormat")));
            player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.chat.enablePrivateMessages")));
            player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.chat.enableChatColors")));
            player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.chat.enableChatTimestamps")));
            Map<String, Object> customPrefs = prefs.getCustomPreferences();
            if (!customPrefs.isEmpty()) {
                Component customHeader = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.custom.header"));
                player.sendSystemMessage(customHeader);
                for (String customKey : customPrefs.keySet()) {
                    player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "preferences.list.custom.entry", customKey)));
                }
            }
            return 1;
        } catch (Exception ex) {
            Component error = Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayerOrException(), "preferences.error.list"));
            context.getSource().getPlayerOrException().sendSystemMessage(error);
            return 0;
        }
            
    }

    private static String getPreferenceValue(PlayerPreferences prefs, String key) {
        switch (key.toLowerCase()) {
            case "receivenotifications":
                return String.valueOf(prefs.isReceiveNotifications());
            case "receiveannouncements":
                return String.valueOf(prefs.isReceiveAnnouncements());
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
