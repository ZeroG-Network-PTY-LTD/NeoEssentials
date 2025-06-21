package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.PermissionUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Manages the admin panel UI and its various sections.
 */
public class AdminPanel {

    /**
     * Displays the economy management panel.
     *
     * @param player The player viewing the panel
     */
    public static void displayEconomyPanel(ServerPlayer player) {
        // Header
        sendHeader(player, "Economy Management");
        
        // Get economy manager for data
        EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
        
        // Economy statistics
        double totalCurrency = economyManager.getTotalCurrency();
        int totalAccounts = economyManager.getTotalAccounts();
        
        Component statsLine = Component.literal(TextUtil.formatText("&7Total Currency: &a" + 
                String.format("%.2f", totalCurrency) + " &7| Active Accounts: &a" + totalAccounts));
        player.sendSystemMessage(statsLine);
        
        // Economy actions
        sendSpacer(player);
        player.sendSystemMessage(Component.literal(TextUtil.formatText("&6Economy Actions:")));
        
        // Top balances button
        displayActionButton(player, "&aView Top Balances", "/eco baltop", 
                "&7Click to view the top player balances");
        
        // Set player balance button
        displayActionButton(player, "&eSet Player Balance", "/eco set <player> <amount>", 
                "&7Click to set a player's balance\n&7Usage: /eco set <player> <amount>");
        
        // Give currency button
        displayActionButton(player, "&2Give Currency", "/eco give <player> <amount>", 
                "&7Click to give currency to a player\n&7Usage: /eco give <player> <amount>");
        
        // Take currency button
        displayActionButton(player, "&cTake Currency", "/eco take <player> <amount>", 
                "&7Click to take currency from a player\n&7Usage: /eco take <player> <amount>");
        
        // Reset player button
        displayActionButton(player, "&4Reset Player", "/eco reset <player>", 
                "&7Click to reset a player's balance\n&7Usage: /eco reset <player>");
        
        // Recent transactions button
        displayActionButton(player, "&9View Recent Transactions", "/eco transactions", 
                "&7Click to view recent economic transactions");
        
        // Back button
        sendSpacer(player);
        displayActionButton(player, "&7Back to Main Menu", "/adminpanel", 
                "&7Click to return to the main admin panel");
        
        sendFooter(player);
    }
    
    /**
     * Displays the kit management panel.
     *
     * @param player The player viewing the panel
     */
    public static void displayKitsPanel(ServerPlayer player) {
        // Header
        sendHeader(player, "Kit Management");
        
        // Get kit manager for data
        KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
        
        // Kit statistics
        int totalKits = kitManager.getKits().size();
        
        Component statsLine = Component.literal(TextUtil.formatText("&7Total Kits: &a" + totalKits));
        player.sendSystemMessage(statsLine);
        
        // Kit list
        if (totalKits > 0) {
            sendSpacer(player);
            player.sendSystemMessage(Component.literal(TextUtil.formatText("&6Available Kits:")));
            
            List<String> kitNames = new ArrayList<>(kitManager.getKits().keySet());
            Collections.sort(kitNames);
            
            for (String kitName : kitNames) {
                KitManager.Kit kit = kitManager.getKit(kitName);
                if (kit != null) {
                    // Create a button for each kit with preview and edit options
                    MutableComponent kitButton = Component.literal(TextUtil.formatText("&8- &b" + kitName));
                    
                    // Add hover text with kit info
                    String hoverText = "&7Kit: &b" + kitName + 
                            "\n&7Cooldown: &e" + kit.cooldown() + "s" +
                            (kit.price() > 0 ? "\n&7Price: &6" + String.format("%.2f", kit.price()) : "") +
                            "\n&7Permission: &eneoessentials.kit." + kitName;
                    
                    Component hoverComponent = Component.literal(TextUtil.formatText(hoverText));
                    kitButton = kitButton.withStyle(style -> style.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT, hoverComponent)));
                    
                    player.sendSystemMessage(kitButton);
                    
                    // Add action buttons for the kit
                    MutableComponent actions = Component.literal("  ");
                    
                    // Preview button
                    MutableComponent previewBtn = Component.literal(TextUtil.formatText("&8[&aPreview&8]"));
                    previewBtn = previewBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND, "/kit preview " + kitName)));
                    previewBtn = previewBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Preview kit contents")))));
                    actions = actions.append(previewBtn).append(" ");
                    
                    // Edit button
                    MutableComponent editBtn = Component.literal(TextUtil.formatText("&8[&eEdit&8]"));
                    editBtn = editBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND, "/kit edit " + kitName)));
                    editBtn = editBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Edit kit properties")))));
                    actions = actions.append(editBtn).append(" ");
                    
                    // Delete button
                    MutableComponent deleteBtn = Component.literal(TextUtil.formatText("&8[&cDelete&8]"));
                    deleteBtn = deleteBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND, "/kit delete " + kitName)));
                    deleteBtn = deleteBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Delete this kit")))));
                    actions = actions.append(deleteBtn);
                    
                    player.sendSystemMessage(actions);
                }
            }
        }
        
        // Kit actions
        sendSpacer(player);
        player.sendSystemMessage(Component.literal(TextUtil.formatText("&6Kit Actions:")));
        
        // Create kit button
        displayActionButton(player, "&aCreate New Kit", "/createkit", 
                "&7Click to create a new kit");
        
        // Kit usage statistics button (placeholder for future implementation)
        displayActionButton(player, "&9View Kit Usage Stats", "/kit stats", 
                "&7Click to view kit usage statistics");
        
        // Back button
        sendSpacer(player);
        displayActionButton(player, "&7Back to Main Menu", "/adminpanel", 
                "&7Click to return to the main admin panel");
        
        sendFooter(player);
    }
    
    /**
     * Displays the warp management panel.
     *
     * @param player The player viewing the panel
     */
    public static void displayWarpsPanel(ServerPlayer player) {
        // Header
        sendHeader(player, "Warp Management");
        
        // Get warp data
        int totalWarps = NeoEssentials.getInstance().getDataManager().getAllWarps().size();
        
        Component statsLine = Component.literal(TextUtil.formatText("&7Total Warps: &a" + totalWarps));
        player.sendSystemMessage(statsLine);
        
        // Warp list
        if (totalWarps > 0) {
            sendSpacer(player);
            player.sendSystemMessage(Component.literal(TextUtil.formatText("&6Available Warps:")));
            
            List<String> warpNames = new ArrayList<>();
            NeoEssentials.getInstance().getDataManager().getAllWarps().forEach(warp -> warpNames.add(warp.getName()));
            Collections.sort(warpNames);
            
            for (String warpName : warpNames) {
                // Create a button for each warp with teleport and edit options
                MutableComponent warpButton = Component.literal(TextUtil.formatText("&8- &d" + warpName));
                
                // Add hover text
                Component hoverComponent = Component.literal(TextUtil.formatText("&7Click for warp options"));
                warpButton = warpButton.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, hoverComponent)));
                
                player.sendSystemMessage(warpButton);
                
                // Add action buttons for the warp
                MutableComponent actions = Component.literal("  ");
                
                // Teleport button
                MutableComponent teleportBtn = Component.literal(TextUtil.formatText("&8[&aTeleport&8]"));
                teleportBtn = teleportBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/warp " + warpName)));
                teleportBtn = teleportBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Teleport to this warp")))));
                actions = actions.append(teleportBtn).append(" ");
                
                // Edit button
                MutableComponent editBtn = Component.literal(TextUtil.formatText("&8[&eEdit&8]"));
                editBtn = editBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND, "/warp edit " + warpName)));
                editBtn = editBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Edit warp properties")))));
                actions = actions.append(editBtn).append(" ");
                
                // Delete button
                MutableComponent deleteBtn = Component.literal(TextUtil.formatText("&8[&cDelete&8]"));
                deleteBtn = deleteBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/delwarp " + warpName)));
                deleteBtn = deleteBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Delete this warp")))));
                actions = actions.append(deleteBtn);
                
                player.sendSystemMessage(actions);
            }
        }
        
        // Warp actions
        sendSpacer(player);
        player.sendSystemMessage(Component.literal(TextUtil.formatText("&6Warp Actions:")));
        
        // Create warp button
        displayActionButton(player, "&aCreate New Warp", "/setwarp <name>", 
                "&7Click to create a new warp at your location\n&7Usage: /setwarp <name>");
        
        // Back button
        sendSpacer(player);
        displayActionButton(player, "&7Back to Main Menu", "/adminpanel", 
                "&7Click to return to the main admin panel");
        
        sendFooter(player);
    }
    
    /**
     * Displays the player management panel.
     *
     * @param player The player viewing the panel
     */
    public static void displayPlayersPanel(ServerPlayer player) {
        // Header
        sendHeader(player, "Player Management");
        
        // Get server for player data
        MinecraftServer server = player.getServer();
        if (server == null) {
            player.sendSystemMessage(Component.literal(TextUtil.formatText("&cError: Could not access server instance.")));
            return;
        }
        
        // Player statistics
        int onlinePlayers = server.getPlayerCount();
        int maxPlayers = server.getMaxPlayers();
        
        Component statsLine = Component.literal(TextUtil.formatText("&7Online Players: &a" + 
                onlinePlayers + "&7/&a" + maxPlayers));
        player.sendSystemMessage(statsLine);
        
        // Online player list
        if (onlinePlayers > 0) {
            sendSpacer(player);
            player.sendSystemMessage(Component.literal(TextUtil.formatText("&6Online Players:")));
            
            List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
            players.sort(Comparator.comparing(ServerPlayer::getScoreboardName));
            
            for (ServerPlayer onlinePlayer : players) {
                String playerName = onlinePlayer.getScoreboardName();
                UUID playerUuid = onlinePlayer.getUUID();
                
                // Create a button for each player with management options
                MutableComponent playerButton = Component.literal(TextUtil.formatText("&8- &e" + playerName));
                
                // Add hover text with player info
                String hoverText = "&7Player: &e" + playerName + 
                        "\n&7UUID: &7" + playerUuid +
                        "\n&7Health: &c" + Math.round(onlinePlayer.getHealth()) + "/" + Math.round(onlinePlayer.getMaxHealth()) +
                        "\n&7Dimension: &b" + onlinePlayer.level().dimension().location().toString();
                
                Component hoverComponent = Component.literal(TextUtil.formatText(hoverText));
                playerButton = playerButton.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, hoverComponent)));
                
                player.sendSystemMessage(playerButton);
                
                // Add action buttons for the player
                MutableComponent actions = Component.literal("  ");
                
                // Teleport to button
                MutableComponent tpToBtn = Component.literal(TextUtil.formatText("&8[&aTp To&8]"));
                tpToBtn = tpToBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/tp " + playerName)));
                tpToBtn = tpToBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Teleport to this player")))));
                actions = actions.append(tpToBtn).append(" ");
                
                // Teleport here button
                MutableComponent tpHereBtn = Component.literal(TextUtil.formatText("&8[&aTp Here&8]"));
                tpHereBtn = tpHereBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/tphere " + playerName)));
                tpHereBtn = tpHereBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Teleport this player to you")))));
                actions = actions.append(tpHereBtn).append(" ");
                
                // Heal button
                MutableComponent healBtn = Component.literal(TextUtil.formatText("&8[&aHeal&8]"));
                healBtn = healBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/heal " + playerName)));
                healBtn = healBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7Heal this player")))));
                actions = actions.append(healBtn).append(" ");
                
                // Economy button
                MutableComponent ecoBtn = Component.literal(TextUtil.formatText("&8[&6Eco&8]"));
                ecoBtn = ecoBtn.withStyle(style -> style.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND, "/eco balance " + playerName)));
                ecoBtn = ecoBtn.withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, Component.literal(TextUtil.formatText("&7View player's economy details")))));
                actions = actions.append(ecoBtn);
                
                player.sendSystemMessage(actions);
            }
        }
        
        // Player actions
        sendSpacer(player);
        player.sendSystemMessage(Component.literal(TextUtil.formatText("&6Player Actions:")));
        
        // Send message to all button
        displayActionButton(player, "&aSend Global Message", "/broadcast <message>", 
                "&7Click to send a message to all players\n&7Usage: /broadcast <message>");
        
        // Back button
        sendSpacer(player);
        displayActionButton(player, "&7Back to Main Menu", "/adminpanel", 
                "&7Click to return to the main admin panel");
        
        sendFooter(player);
    }
    
    /**
     * Sends a header for an admin panel section.
     *
     * @param player The player to send the header to
     * @param title The title of the section
     */
    private static void sendHeader(ServerPlayer player, String title) {
        Component header = Component.literal(TextUtil.formatText("&6====== &lNeoEssentials " + title + "&r &6======"));
        player.sendSystemMessage(header);
    }
    
    /**
     * Sends a footer for an admin panel section.
     *
     * @param player The player to send the footer to
     */
    private static void sendFooter(ServerPlayer player) {
        Component footer = Component.literal(TextUtil.formatText("&6==================================="));
        player.sendSystemMessage(footer);
    }
    
    /**
     * Sends a spacer line.
     *
     * @param player The player to send the spacer to
     */
    private static void sendSpacer(ServerPlayer player) {
        player.sendSystemMessage(Component.empty());
    }
    
    /**
     * Displays an action button in the admin panel.
     *
     * @param player The player to show the button to
     * @param title The title of the button
     * @param command The command to run when clicked
     * @param hoverText The hover text to display
     */
    private static void displayActionButton(ServerPlayer player, String title, String command, String hoverText) {
        MutableComponent buttonText = Component.literal(TextUtil.formatText("&8[&r " + title + " &8]"));
        Component hoverComponent = Component.literal(TextUtil.formatText(hoverText));
        
        // Determine if we should suggest or run the command
        ClickEvent.Action clickAction = command.contains("<") ? 
                ClickEvent.Action.SUGGEST_COMMAND : ClickEvent.Action.RUN_COMMAND;
        
        // Make the button clickable and add hover text
        Component clickableButton = Component.literal("➤ ").append(buttonText)
                .withStyle(style -> style.withClickEvent(new ClickEvent(clickAction, command)))
                .withStyle(style -> style.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, hoverComponent)));
        
        player.sendSystemMessage(clickableButton);
    }
    
    /**
     * Utility class for text formatting.
     */
    private static class TextUtil {
        /**
         * Formats text with color codes.
         * 
         * @param text Text with & color codes
         * @return Formatted text
         */
        public static String formatText(String text) {
            char colorChar = '&';
            char[] array = text.toCharArray();
            
            for (int i = 0; i < array.length - 1; i++) {
                if (array[i] == colorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(array[i + 1]) > -1) {
                    array[i] = '§';
                    array[i + 1] = Character.toLowerCase(array[i + 1]);
                }
            }
            
            return new String(array);
        }
    }
}
