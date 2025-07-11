package com.zerog.neoessentials.gui;

import com.zerog.neoessentials.data.EnhancedHome;
import com.zerog.neoessentials.managers.EnhancedHomeManager;
import com.zerog.neoessentials.util.GuiUtils;
import com.zerog.neoessentials.util.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Enhanced home management GUI interface
 */
public class EnhancedHomeInterface implements MenuProvider {
    
    private final EnhancedHomeManager homeManager;
    private final ServerPlayer player;
    private ViewMode currentView = ViewMode.MAIN;
    private EnhancedHome.HomeCategory selectedCategory = null;
    private EnhancedHome selectedHome = null;
    
    public enum ViewMode {
        MAIN,           // Main home overview
        CATEGORY_VIEW,  // View homes by category
        HOME_DETAILS,   // Detailed view of a specific home
        CREATE_HOME,    // Create new home form
        EDIT_HOME       // Edit existing home
    }
    
    public EnhancedHomeInterface(EnhancedHomeManager homeManager, ServerPlayer player) {
        this.homeManager = homeManager;
        this.player = player;
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Enhanced Home Management");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EnhancedHomeMenu(containerId, inventory, this);
    }
    
    /**
     * Opens the home management GUI
     */
    public void openGui() {
        player.openMenu(this);
    }
    
    /**
     * Enhanced home menu implementation
     */
    public static class EnhancedHomeMenu extends AbstractContainerMenu {
        
        private final EnhancedHomeInterface homeInterface;
        private final ServerPlayer player;
        
        public EnhancedHomeMenu(int containerId, Inventory inventory, EnhancedHomeInterface homeInterface) {
            super(MenuType.GENERIC_9x6, containerId);
            this.homeInterface = homeInterface;
            this.player = (ServerPlayer) inventory.player;
            
            setupGui();
        }
        
        private void setupGui() {
            switch (homeInterface.currentView) {
                case MAIN -> setupMainView();
                case CATEGORY_VIEW -> setupCategoryView();
                case HOME_DETAILS -> setupHomeDetailsView();
                case CREATE_HOME -> setupCreateHomeView();
                case EDIT_HOME -> setupEditHomeView();
            }
        }
        
        private void setupMainView() {
            // Clear all slots
            clearSlots();
            
            // Title
            setItem(4, GuiUtils.createItem(Items.COMPASS, 
                "§6§lEnhanced Home Management", 
                "§7Manage your homes with categories",
                "§7and advanced features"));
            
            // Player statistics
            Map<EnhancedHome.HomeCategory, Integer> stats = homeInterface.homeManager.getPlayerHomeStats(player.getUUID());
            int totalHomes = homeInterface.homeManager.getTotalPlayerHomes(player.getUUID());
            int maxHomes = homeInterface.homeManager.getMaxPlayerHomes();
            
            setItem(10, GuiUtils.createItem(Items.BOOK, 
                "§e§lHome Statistics", 
                "§7Total Homes: §b" + totalHomes + "§7/§b" + maxHomes,
                "§7Click to view details"));
            
            // Category buttons
            int slot = 19;
            for (EnhancedHome.HomeCategory category : EnhancedHome.HomeCategory.values()) {
                int count = stats.getOrDefault(category, 0);
                int limit = homeInterface.homeManager.getCategoryLimit(category);
                
                ItemStack categoryItem = GuiUtils.createItem(getCategoryItem(category),
                    category.getDisplayName() + " §7(" + count + "/" + limit + ")",
                    category.getDescription(),
                    "§7Click to view homes in this category");
                
                setItem(slot, categoryItem);
                slot++;
                
                if (slot == 26) slot = 28; // Skip to next row
                if (slot == 35) slot = 37; // Skip to next row
            }
            
            // Quick action buttons
            setItem(45, GuiUtils.createItem(Items.EMERALD, 
                "§a§lCreate New Home", 
                "§7Create a new home at your",
                "§7current location"));
            
            setItem(46, GuiUtils.createItem(Items.ENDER_PEARL, 
                "§b§lPublic Homes", 
                "§7Browse public homes created",
                "§7by other players"));
            
            setItem(47, GuiUtils.createItem(Items.REDSTONE, 
                "§c§lManage Permissions", 
                "§7Set home permissions and",
                "§7sharing options"));
            
            // Navigation
            setItem(49, GuiUtils.createItem(Items.BARRIER, 
                "§c§lClose", 
                "§7Close this menu"));
        }
        
        private void setupCategoryView() {
            clearSlots();
            
            if (homeInterface.selectedCategory == null) {
                setupMainView();
                return;
            }
            
            // Title
            setItem(4, GuiUtils.createItem(getCategoryItem(homeInterface.selectedCategory),
                homeInterface.selectedCategory.getDisplayName() + " §7Homes",
                homeInterface.selectedCategory.getDescription()));
            
            // Get homes in this category
            List<EnhancedHome> homes = homeInterface.homeManager.getPlayerHomesByCategory(
                player.getUUID(), homeInterface.selectedCategory);
            
            // Display homes
            int slot = 9;
            for (EnhancedHome home : homes) {
                ItemStack homeItem = GuiUtils.createItem(Items.PLAYER_HEAD,
                    "§6" + home.getName(),
                    home.getFormattedDescription(),
                    home.getUsageStats(),
                    "§7Location: §b" + home.getDimension().location(),
                    "§7Public: " + (home.isPublic() ? "§aYes" : "§cNo"),
                    "",
                    "§eLeft Click: §7Teleport",
                    "§eRight Click: §7Edit",
                    "§eShift+Right Click: §7Delete");
                
                setItem(slot, homeItem);
                slot++;
                
                if (slot % 9 == 8) slot += 2; // Skip to next row
                if (slot >= 45) break; // Prevent overflow
            }
            
            // Add new home button
            if (homeInterface.homeManager.canCreateHomeInCategory(player.getUUID(), homeInterface.selectedCategory)) {
                setItem(45, GuiUtils.createItem(Items.EMERALD,
                    "§a§lCreate New " + homeInterface.selectedCategory.getDisplayName() + " Home",
                    "§7Create a new home in this category"));
            }
            
            // Back button
            setItem(49, GuiUtils.createItem(Items.ARROW,
                "§e§lBack to Main Menu",
                "§7Return to the main home menu"));
        }
        
        private void setupHomeDetailsView() {
            clearSlots();
            
            if (homeInterface.selectedHome == null) {
                setupMainView();
                return;
            }
            
            EnhancedHome home = homeInterface.selectedHome;
            
            // Title
            setItem(4, GuiUtils.createItem(Items.PLAYER_HEAD,
                "§6" + home.getName(),
                home.getFormattedDescription()));
            
            // Home information
            setItem(10, GuiUtils.createItem(Items.COMPASS,
                "§e§lLocation",
                "§7Dimension: §b" + home.getDimension().location(),
                "§7Position: §b" + home.getPosition().toShortString(),
                "§7Created: §b" + home.getCreated().format(
                    java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))));
            
            setItem(12, GuiUtils.createItem(getCategoryItem(home.getCategory()),
                "§e§lCategory",
                home.getCategory().getDisplayName(),
                home.getCategory().getDescription()));
            
            setItem(14, GuiUtils.createItem(Items.CLOCK,
                "§e§lUsage Statistics",
                home.getUsageStats()));
            
            setItem(16, GuiUtils.createItem(home.isPublic() ? Items.EMERALD : Items.REDSTONE,
                "§e§lVisibility",
                home.isPublic() ? "§aPublic" : "§cPrivate",
                home.isPublic() ? "§7Other players can use this home" : "§7Only you can use this home"));
            
            // Action buttons
            setItem(28, GuiUtils.createItem(Items.ENDER_PEARL,
                "§b§lTeleport",
                "§7Teleport to this home"));
            
            setItem(30, GuiUtils.createItem(Items.WRITABLE_BOOK,
                "§e§lEdit Home",
                "§7Edit description, category, and settings"));
            
            setItem(32, GuiUtils.createItem(Items.REDSTONE,
                "§c§lDelete Home",
                "§7Permanently delete this home",
                "§c§lWARNING: This cannot be undone!"));
            
            setItem(34, GuiUtils.createItem(Items.PLAYER_HEAD,
                "§d§lManage Permissions",
                "§7Set who can use this home"));
            
            // Back button
            setItem(49, GuiUtils.createItem(Items.ARROW,
                "§e§lBack",
                "§7Return to previous menu"));
        }
        
        private void setupCreateHomeView() {
            clearSlots();
            
            // Title
            setItem(4, GuiUtils.createItem(Items.EMERALD,
                "§a§lCreate New Home",
                "§7Set up a new home at your current location"));
            
            // Category selection
            setItem(10, GuiUtils.createItem(Items.BOOK,
                "§e§lSelect Category",
                "§7Choose a category for your new home"));
            
            // Category buttons
            int slot = 19;
            for (EnhancedHome.HomeCategory category : EnhancedHome.HomeCategory.values()) {
                if (homeInterface.homeManager.canCreateHomeInCategory(player.getUUID(), category)) {
                    ItemStack categoryItem = GuiUtils.createItem(getCategoryItem(category),
                        category.getDisplayName(),
                        category.getDescription(),
                        "§7Click to select this category");
                    
                    setItem(slot, categoryItem);
                }
                slot++;
                
                if (slot == 26) slot = 28; // Skip to next row
                if (slot == 35) slot = 37; // Skip to next row
            }
            
            // Back button
            setItem(49, GuiUtils.createItem(Items.ARROW,
                "§e§lBack",
                "§7Return to main menu"));
        }
        
        private void setupEditHomeView() {
            clearSlots();
            
            if (homeInterface.selectedHome == null) {
                setupMainView();
                return;
            }
            
            // Implementation for editing home properties
            // This would include name editing, description editing, category change, etc.
            
            // Back button
            setItem(49, GuiUtils.createItem(Items.ARROW,
                "§e§lBack",
                "§7Return to home details"));
        }
        
        private net.minecraft.world.item.Item getCategoryItem(EnhancedHome.HomeCategory category) {
            return switch (category) {
                case GENERAL -> net.minecraft.world.item.Items.COMPASS;
                case BASE -> net.minecraft.world.item.Items.BEACON;
                case FARM -> net.minecraft.world.item.Items.WHEAT;
                case MINE -> net.minecraft.world.item.Items.IRON_PICKAXE;
                case SHOP -> net.minecraft.world.item.Items.EMERALD;
                case BUILD -> net.minecraft.world.item.Items.BRICKS;
                case ADVENTURE -> net.minecraft.world.item.Items.DIAMOND_SWORD;
                case TRANSPORT -> net.minecraft.world.item.Items.MINECART;
                case FRIEND -> net.minecraft.world.item.Items.PLAYER_HEAD;
                case TEMP -> net.minecraft.world.item.Items.CLOCK;
            };
        }
        
        private void clearSlots() {
            // Clear all slots in the container
            for (int i = 0; i < 54; i++) {
                setItem(i, ItemStack.EMPTY);
            }
        }
        
        private void setItem(int slot, ItemStack item) {
            // Implementation would set the item in the container
            // This is simplified for this example
        }
        
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }
        
        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
