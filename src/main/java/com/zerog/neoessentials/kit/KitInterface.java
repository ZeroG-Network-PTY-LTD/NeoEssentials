package com.zerog.neoessentials.kit;

import com.zerog.neoessentials.util.GuiUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * GUI interface for kit management and selection
 */
public class KitInterface implements MenuProvider {
    
    private final KitManager kitManager;
    private final ServerPlayer player;
    private ViewMode currentView = ViewMode.MAIN;
    private Kit.KitCategory selectedCategory = null;
    private Kit selectedKit = null;
    
    public enum ViewMode {
        MAIN,           // Main kit overview
        CATEGORY_VIEW,  // View kits by category
        KIT_PREVIEW,    // Preview a specific kit
        PLAYER_STATS    // Player kit statistics
    }
    
    public KitInterface(KitManager kitManager, ServerPlayer player) {
        this.kitManager = kitManager;
        this.player = player;
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Kit Manager");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new KitMenu(containerId, inventory, this);
    }
    
    /**
     * Opens the kit GUI
     */
    public void openGui() {
        player.openMenu(this);
    }
    
    /**
     * Kit menu implementation
     */
    public static class KitMenu extends AbstractContainerMenu {
        
        private final KitInterface kitInterface;
        private final ServerPlayer player;
        
        public KitMenu(int containerId, Inventory inventory, KitInterface kitInterface) {
            super(MenuType.GENERIC_9x6, containerId);
            this.kitInterface = kitInterface;
            this.player = (ServerPlayer) inventory.player;
            
            setupGui();
        }
        
        private void setupGui() {
            switch (kitInterface.currentView) {
                case MAIN -> setupMainView();
                case CATEGORY_VIEW -> setupCategoryView();
                case KIT_PREVIEW -> setupKitPreview();
                case PLAYER_STATS -> setupPlayerStats();
            }
        }
        
        private void setupMainView() {
            clearSlots();
            
            // Title
            setItem(4, GuiUtils.createItem(Items.CHEST, 
                "§6§lKit Manager", 
                "§7Browse and claim kits",
                "§7organized by category"));
            
            // Player statistics
            Map<String, Object> stats = kitInterface.kitManager.getPlayerKitStats(player.getUUID());
            
            setItem(10, GuiUtils.createItem(Items.BOOK, 
                "§e§lYour Kit Statistics", 
                "§7Total Kits: §b" + stats.get("total"),
                "§7Available: §a" + stats.get("available"),
                "§7On Cooldown: §c" + stats.get("cooldown"),
                "§7Used (One-time): §8" + stats.get("used"),
                "",
                "§7Click for detailed view"));
            
            // Category buttons
            int slot = 19;
            for (Kit.KitCategory category : Kit.KitCategory.values()) {
                List<Kit> categoryKits = kitInterface.kitManager.getKitsByCategory(category);
                
                if (!categoryKits.isEmpty()) {
                    int availableCount = 0;
                    int totalCount = categoryKits.size();
                    
                    for (Kit kit : categoryKits) {
                        if (kitInterface.kitManager.canUseKit(player, kit.getName())) {
                            availableCount++;
                        }
                    }
                    
                    ItemStack categoryItem = GuiUtils.createItem(getCategoryItem(category),
                        category.getDisplayName() + " §7(" + availableCount + "/" + totalCount + ")",
                        category.getDescription(),
                        "§7Available kits: §a" + availableCount + "§7/§b" + totalCount,
                        "",
                        "§7Click to browse kits");
                    
                    setItem(slot, categoryItem);
                }
                
                slot++;
                if (slot == 26) slot = 28; // Skip to next row
                if (slot == 35) slot = 37; // Skip to next row
            }
            
            // Quick access buttons
            setItem(45, GuiUtils.createItem(Items.CLOCK, 
                "§e§lCooldown Status", 
                "§7View all kit cooldowns",
                "§7and time remaining"));
            
            setItem(46, GuiUtils.createItem(Items.EMERALD, 
                "§a§lAvailable Now", 
                "§7Show only kits that",
                "§7you can claim right now"));
            
            setItem(47, GuiUtils.createItem(Items.REDSTONE, 
                "§c§lPermission Info", 
                "§7View kit permissions",
                "§7and requirements"));
            
            // Close button
            setItem(49, GuiUtils.createItem(Items.BARRIER, 
                "§c§lClose", 
                "§7Close the kit manager"));
        }
        
        private void setupCategoryView() {
            clearSlots();
            
            if (kitInterface.selectedCategory == null) {
                setupMainView();
                return;
            }
            
            // Title
            setItem(4, GuiUtils.createItem(getCategoryItem(kitInterface.selectedCategory),
                kitInterface.selectedCategory.getDisplayName() + " §7Kits",
                kitInterface.selectedCategory.getDescription()));
            
            // Get kits in this category
            List<Kit> categoryKits = kitInterface.kitManager.getKitsByCategory(kitInterface.selectedCategory);
            
            // Display kits
            int slot = 9;
            for (Kit kit : categoryKits) {
                boolean canUse = kitInterface.kitManager.canUseKit(player, kit.getName());
                boolean onCooldown = kitInterface.kitManager.isOnCooldown(player.getUUID(), kit.getName());
                boolean isUsed = kit.isOneTimeUse() && kitInterface.kitManager.hasUsedKit(player.getUUID(), kit.getName());
                
                ItemStack kitItem = GuiUtils.createItem(getKitStatusItem(kit, canUse, onCooldown, isUsed),
                    (canUse ? "§a" : "§c") + kit.getName(),
                    kit.getFormattedDescription(),
                    "",
                    "§7Items: §b" + kit.getUniqueItems() + " §7types (§b" + kit.getTotalItems() + " §7total)",
                    "§7Cost: " + kit.getCostInfo(),
                    "§7Cooldown: " + kit.getCooldownInfo(),
                    "",
                    getKitStatusText(kit, canUse, onCooldown, isUsed),
                    "",
                    canUse ? "§eLeft Click: §7Claim Kit" : "§cCannot claim this kit",
                    "§eRight Click: §7Preview Items");
                
                setItem(slot, kitItem);
                slot++;
                
                if (slot % 9 == 8) slot += 2; // Skip to next row
                if (slot >= 45) break; // Prevent overflow
            }
            
            // Back button
            setItem(49, GuiUtils.createItem(Items.ARROW,
                "§e§lBack to Categories",
                "§7Return to the main kit menu"));
        }
        
        private void setupKitPreview() {
            clearSlots();
            
            if (kitInterface.selectedKit == null) {
                setupMainView();
                return;
            }
            
            Kit kit = kitInterface.selectedKit;
            boolean canUse = kitInterface.kitManager.canUseKit(player, kit.getName());
            boolean onCooldown = kitInterface.kitManager.isOnCooldown(player.getUUID(), kit.getName());
            boolean isUsed = kit.isOneTimeUse() && kitInterface.kitManager.hasUsedKit(player.getUUID(), kit.getName());
            
            // Title
            setItem(4, GuiUtils.createItem(getKitStatusItem(kit, canUse, onCooldown, isUsed),
                "§6" + kit.getName(),
                kit.getFormattedDescription()));
            
            // Kit information
            setItem(10, GuiUtils.createItem(getCategoryItem(kit.getCategory()),
                "§e§lCategory",
                kit.getCategory().getDisplayName(),
                kit.getCategory().getDescription()));
            
            setItem(12, GuiUtils.createItem(Items.CLOCK,
                "§e§lCooldown",
                kit.getCooldownInfo(),
                kit.isOneTimeUse() ? "§c§lOne-time use only" : "§7Repeatable kit"));
            
            setItem(14, GuiUtils.createItem(Items.EMERALD,
                "§e§lCost",
                kit.getCostInfo(),
                kit.getCost() > 0 ? "§7Deducted from your balance" : "§7Free of charge"));
            
            setItem(16, GuiUtils.createItem(Items.CHEST,
                "§e§lContents",
                "§7Items: §b" + kit.getUniqueItems() + " §7types",
                "§7Total: §b" + kit.getTotalItems() + " §7items"));
            
            // Kit items preview
            List<ItemStack> kitItems = kit.getItems();
            int previewSlot = 19;
            
            for (int i = 0; i < Math.min(kitItems.size(), 18); i++) {
                ItemStack item = kitItems.get(i);
                ItemStack displayItem = item.copy();
                
                setItem(previewSlot, displayItem);
                previewSlot++;
                
                if (previewSlot % 9 == 8) previewSlot += 2; // Skip to next row
                if (previewSlot >= 37) break; // Prevent overflow into action buttons
            }
            
            // Action buttons
            if (canUse) {
                setItem(40, GuiUtils.createItem(Items.LIME_WOOL,
                    "§a§lClaim Kit",
                    "§7Click to claim this kit",
                    "§7Items will be added to your inventory"));
            } else {
                setItem(40, GuiUtils.createItem(Items.RED_WOOL,
                    "§c§lCannot Claim",
                    getKitStatusText(kit, false, onCooldown, isUsed)));
            }
            
            // Cooldown reset (for admins)
            if (player.hasPermissions(2) && onCooldown) {
                setItem(42, GuiUtils.createItem(Items.REDSTONE,
                    "§c§lReset Cooldown",
                    "§7Admin: Reset cooldown for this kit",
                    "§c§lThis is an admin action"));
            }
            
            // Back button
            setItem(49, GuiUtils.createItem(Items.ARROW,
                "§e§lBack",
                "§7Return to kit category"));
        }
        
        private void setupPlayerStats() {
            clearSlots();
            
            // Title
            setItem(4, GuiUtils.createItem(Items.PLAYER_HEAD,
                "§6§lYour Kit Statistics",
                "§7Detailed overview of your kit status"));
            
            Map<String, Object> stats = kitInterface.kitManager.getPlayerKitStats(player.getUUID());
            
            // Overview
            setItem(10, GuiUtils.createItem(Items.BOOK,
                "§e§lOverview",
                "§7Total Kits: §b" + stats.get("total"),
                "§7Available: §a" + stats.get("available"),
                "§7On Cooldown: §c" + stats.get("cooldown"),
                "§7Used (One-time): §8" + stats.get("used")));
            
            // Cooldown details
            setItem(12, GuiUtils.createItem(Items.CLOCK,
                "§c§lCooldown Details",
                "§7Kits currently on cooldown"));
            
            // Show kits on cooldown
            int slot = 19;
            for (Kit kit : kitInterface.kitManager.getAllKits()) {
                if (kitInterface.kitManager.isOnCooldown(player.getUUID(), kit.getName())) {
                    Duration remaining = kitInterface.kitManager.getRemainingCooldown(player.getUUID(), kit.getName());
                    
                    ItemStack cooldownItem = GuiUtils.createItem(Items.RED_WOOL,
                        "§c" + kit.getName(),
                        "§7Time remaining: §e" + formatDuration(remaining),
                        "§7Category: " + kit.getCategory().getDisplayName());
                    
                    setItem(slot, cooldownItem);
                    slot++;
                    
                    if (slot % 9 == 8) slot += 2; // Skip to next row
                    if (slot >= 37) break; // Prevent overflow
                }
            }
            
            // Back button
            setItem(49, GuiUtils.createItem(Items.ARROW,
                "§e§lBack to Main Menu",
                "§7Return to the main kit menu"));
        }
        
        private net.minecraft.world.item.Item getCategoryItem(Kit.KitCategory category) {
            return switch (category) {
                case STARTER -> net.minecraft.world.item.Items.COMPASS;
                case TOOLS -> net.minecraft.world.item.Items.IRON_PICKAXE;
                case COMBAT -> net.minecraft.world.item.Items.DIAMOND_SWORD;
                case BUILDING -> net.minecraft.world.item.Items.BRICKS;
                case FOOD -> net.minecraft.world.item.Items.BREAD;
                case FARMING -> net.minecraft.world.item.Items.WHEAT;
                case MINING -> net.minecraft.world.item.Items.IRON_PICKAXE;
                case EXPLORATION -> net.minecraft.world.item.Items.MAP;
                case PREMIUM -> net.minecraft.world.item.Items.DIAMOND;
                case SPECIAL -> net.minecraft.world.item.Items.NETHER_STAR;
            };
        }
        private net.minecraft.world.item.Item getKitStatusItem(Kit kit, boolean canUse, boolean onCooldown, boolean isUsed) {
            if (canUse) {
                return net.minecraft.world.item.Items.LIME_WOOL;
            } else if (onCooldown) {
                return net.minecraft.world.item.Items.ORANGE_WOOL;
            } else if (isUsed) {
                return net.minecraft.world.item.Items.GRAY_WOOL;
            } else {
                return net.minecraft.world.item.Items.RED_WOOL;
            }
        }
        
        private String getKitStatusText(Kit kit, boolean canUse, boolean onCooldown, boolean isUsed) {
            if (canUse) {
                return "§a✓ Available to claim";
            } else if (onCooldown) {
                Duration remaining = kitInterface.kitManager.getRemainingCooldown(player.getUUID(), kit.getName());
                return "§c⏰ Cooldown: " + formatDuration(remaining);
            } else if (isUsed) {
                return "§8✗ Already used (one-time kit)";
            } else {
                return "§c✗ Cannot use this kit";
            }
        }
        
        private String formatDuration(Duration duration) {
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            
            if (days > 0) {
                return days + "d " + hours + "h " + minutes + "m";
            } else if (hours > 0) {
                return hours + "h " + minutes + "m";
            } else {
                return minutes + "m";
            }
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
