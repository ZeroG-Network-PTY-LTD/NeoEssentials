package com.zerog.neoessentials.ui;

import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Provides a system for creating and managing in-game menus through chat.
 * This class handles pagination, clickable buttons, and menu structure.
 */
public class MenuSystem {

    /**
     * Represents a menu item with text, commands, and hover information.
     */
    public static class MenuItem {
        private final MutableComponent text;
        private final String command;
        private final String hoverText;
        private final ClickEvent.Action clickAction;
        
        /**
         * Creates a new menu item that runs a command when clicked.
         * 
         * @param text The text to display
         * @param command The command to run when clicked
         * @param hoverText The text to show on hover
         */
        public MenuItem(String text, String command, String hoverText) {
            this(text, command, hoverText, ClickEvent.Action.RUN_COMMAND);
        }
        
        /**
         * Creates a new menu item with custom click action.
         * 
         * @param text The text to display
         * @param command The command or text for the click action
         * @param hoverText The text to show on hover
         * @param clickAction The click action to perform
         */
        public MenuItem(String text, String command, String hoverText, ClickEvent.Action clickAction) {
            this.text = Component.literal(MessageUtil.formatText(text));
            this.command = command;
            this.hoverText = hoverText;
            this.clickAction = clickAction;
        }
        
        /**
         * Gets the formatted component for this menu item.
         * 
         * @return The formatted component
         */
        public Component getFormattedComponent() {
            // Add hover text
            Component hoverComponent = Component.literal(MessageUtil.formatText(hoverText));
            
            // Create the clickable component
            return text.withStyle(style -> style
                    .withClickEvent(new ClickEvent(clickAction, command))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent)));
        }
    }
    
    /**
     * Builder class for creating and displaying menus.
     */
    public static class MenuBuilder {
        private String title;
        private List<MenuItem> items;
        private int itemsPerPage = 6;
        private boolean showPageNumbers = true;
        private String backCommand = null;
        private String backText = "&7Back";
        private String backHoverText = "&7Click to go back";
        
        /**
         * Sets the title of the menu.
         * 
         * @param title The title to use
         * @return This builder for chaining
         */
        public MenuBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * Sets the items for the menu.
         * 
         * @param items The items to display
         * @return This builder for chaining
         */
        public MenuBuilder items(List<MenuItem> items) {
            this.items = items;
            return this;
        }
        
        /**
         * Sets the number of items to show per page.
         * 
         * @param itemsPerPage The number of items per page
         * @return This builder for chaining
         */
        public MenuBuilder itemsPerPage(int itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
            return this;
        }
        
        /**
         * Sets whether to show page numbers.
         * 
         * @param showPageNumbers Whether to show page numbers
         * @return This builder for chaining
         */
        public MenuBuilder showPageNumbers(boolean showPageNumbers) {
            this.showPageNumbers = showPageNumbers;
            return this;
        }
        
        /**
         * Sets the back button properties.
         * 
         * @param command The command to run when the back button is clicked
         * @param text The text to show for the back button
         * @param hoverText The text to show when hovering over the back button
         * @return This builder for chaining
         */
        public MenuBuilder back(String command, String text, String hoverText) {
            this.backCommand = command;
            this.backText = text;
            this.backHoverText = hoverText;
            return this;
        }
        
        /**
         * Shows the menu to the player.
         * 
         * @param player The player to show the menu to
         * @param page The page number to show
         */
        public void show(ServerPlayer player, int page) {
            // Calculate total pages
            int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
            
            // Ensure page is in valid range
            page = Math.max(1, Math.min(page, totalPages));
            
            // Send header
            String headerText = "&6====== &l" + title + "&r &6======";
            player.sendSystemMessage(Component.literal(MessageUtil.formatText(headerText)));
            
            // Show page numbers if enabled
            if (showPageNumbers && totalPages > 1) {
                player.sendSystemMessage(Component.literal(MessageUtil.formatText(
                        "&7Page &e" + page + "&7/&e" + totalPages)));
            }
            
            // Send items for this page
            int startIndex = (page - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, items.size());
            
            if (items.isEmpty()) {
                // No items to display
                player.sendSystemMessage(Component.literal(MessageUtil.formatText("&7No items to display.")));
            } else {
                // Display items for this page
                for (int i = startIndex; i < endIndex; i++) {
                    MenuItem item = items.get(i);
                    player.sendSystemMessage(item.getFormattedComponent());
                }
            }
            
            // Add navigation if needed
            if (totalPages > 1) {
                MutableComponent navigation = Component.literal("");
                
                // Previous page button
                if (page > 1) {
                    MenuItem prevPageItem = new MenuItem("&8[&aPrevious Page&8] ", 
                            "/menu page " + (page - 1), "&7Click to go to the previous page");
                    navigation.append(prevPageItem.getFormattedComponent()).append(" ");
                }
                
                // Next page button
                if (page < totalPages) {
                    MenuItem nextPageItem = new MenuItem("&8[&aNext Page&8]", 
                            "/menu page " + (page + 1), "&7Click to go to the next page");
                    navigation.append(nextPageItem.getFormattedComponent());
                }
                
                player.sendSystemMessage(navigation);
            }
            
            // Back button if specified
            if (backCommand != null) {
                MenuItem backItem = new MenuItem(backText, backCommand, backHoverText);
                player.sendSystemMessage(backItem.getFormattedComponent());
            }
            
            // Send footer
            String footerText = "&6===================================";
            player.sendSystemMessage(Component.literal(MessageUtil.formatText(footerText)));
        }
    }
    
    /**
     * Creates a new menu builder to start constructing a menu.
     * 
     * @return A new menu builder
     */
    public static MenuBuilder builder() {
        return new MenuBuilder();
    }
}
