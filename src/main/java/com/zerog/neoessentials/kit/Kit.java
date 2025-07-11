package com.zerog.neoessentials.kit;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a kit with items and cooldown management
 */
public class Kit {
    
    private final String name;
    private final List<ItemStack> items;
    private final Duration cooldown;
    private final Set<String> permissions;
    private final String description;
    private final KitCategory category;
    private final boolean oneTimeUse;
    private final int cost;
    private final boolean enabled;
    private final Map<String, String> metadata;
    
    public Kit(Builder builder) {
        this.name = builder.name;
        this.items = new ArrayList<>(builder.items);
        this.cooldown = builder.cooldown;
        this.permissions = new HashSet<>(builder.permissions);
        this.description = builder.description;
        this.category = builder.category;
        this.oneTimeUse = builder.oneTimeUse;
        this.cost = builder.cost;
        this.enabled = builder.enabled;
        this.metadata = new HashMap<>(builder.metadata);
    }
    
    /**
     * Kit categories for organization
     */
    public enum KitCategory {
        STARTER("§a", "Starter", "🎯", "Beginner kits for new players"),
        TOOLS("§6", "Tools", "⚒️", "Tool kits for various activities"),
        COMBAT("§c", "Combat", "⚔️", "Combat equipment and weapons"),
        BUILDING("§b", "Building", "🏗️", "Building materials and blocks"),
        FOOD("§e", "Food", "🍞", "Food and sustenance kits"),
        FARMING("§2", "Farming", "🌾", "Farming supplies and seeds"),
        MINING("§8", "Mining", "⛏️", "Mining equipment and materials"),
        EXPLORATION("§5", "Exploration", "🗺️", "Items for exploration and adventure"),
        PREMIUM("§d", "Premium", "💎", "Premium kits with special items"),
        SPECIAL("§f", "Special", "⭐", "Special event or seasonal kits");
        
        private final String colorCode;
        private final String displayName;
        private final String emoji;
        private final String description;
        
        KitCategory(String colorCode, String displayName, String emoji, String description) {
            this.colorCode = colorCode;
            this.displayName = displayName;
            this.emoji = emoji;
            this.description = description;
        }
        
        public String getColorCode() { return colorCode; }
        public String getDisplayName() { return colorCode + displayName; }
        public String getEmoji() { return emoji; }
        public String getDescription() { return "§7" + description; }
        
        public String getPrefix() {
            return colorCode + emoji + " ";
        }
    }
    
    /**
     * Builder pattern for creating kits
     */
    public static class Builder {
        private String name;
        private List<ItemStack> items = new ArrayList<>();
        private Duration cooldown = Duration.ZERO;
        private Set<String> permissions = new HashSet<>();
        private String description = "";
        private KitCategory category = KitCategory.STARTER;
        private boolean oneTimeUse = false;
        private int cost = 0;
        private boolean enabled = true;
        private Map<String, String> metadata = new HashMap<>();
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder addItem(ItemStack item) {
            this.items.add(item.copy());
            return this;
        }
        
        public Builder addItems(List<ItemStack> items) {
            for (ItemStack item : items) {
                this.items.add(item.copy());
            }
            return this;
        }
        
        public Builder setCooldown(Duration cooldown) {
            this.cooldown = cooldown;
            return this;
        }
        
        public Builder setCooldown(long minutes) {
            this.cooldown = Duration.ofMinutes(minutes);
            return this;
        }
        
        public Builder addPermission(String permission) {
            this.permissions.add(permission);
            return this;
        }
        
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }
        
        public Builder setCategory(KitCategory category) {
            this.category = category;
            return this;
        }
        
        public Builder setOneTimeUse(boolean oneTimeUse) {
            this.oneTimeUse = oneTimeUse;
            return this;
        }
        
        public Builder setCost(int cost) {
            this.cost = cost;
            return this;
        }
        
        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public Builder setMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Kit build() {
            return new Kit(this);
        }
    }
    
    // Getters
    public String getName() { return name; }
    public List<ItemStack> getItems() { return new ArrayList<>(items); }
    public Duration getCooldown() { return cooldown; }
    public Set<String> getPermissions() { return new HashSet<>(permissions); }
    public String getDescription() { return description; }
    public KitCategory getCategory() { return category; }
    public boolean isOneTimeUse() { return oneTimeUse; }
    public int getCost() { return cost; }
    public boolean isEnabled() { return enabled; }
    public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Gets the display name with category prefix
     */
    public String getDisplayName() {
        return category.getPrefix() + name;
    }
    
    /**
     * Gets formatted description
     */
    public String getFormattedDescription() {
        if (description == null || description.isEmpty()) {
            return "§7No description available";
        }
        return "§f" + description;
    }
    
    /**
     * Gets cooldown information as formatted string
     */
    public String getCooldownInfo() {
        if (cooldown.equals(Duration.ZERO)) {
            return "§aNo cooldown";
        }
        
        long minutes = cooldown.toMinutes();
        long hours = cooldown.toHours();
        long days = cooldown.toDays();
        
        if (days > 0) {
            return "§e" + days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return "§e" + hours + " hour" + (hours > 1 ? "s" : "");
        } else {
            return "§e" + minutes + " minute" + (minutes > 1 ? "s" : "");
        }
    }
    
    /**
     * Gets cost information as formatted string
     */
    public String getCostInfo() {
        if (cost <= 0) {
            return "§aFree";
        }
        return "§6$" + cost;
    }
    
    /**
     * Checks if a player has permission to use this kit
     */
    public boolean hasPermission(ServerPlayer player) {
        if (permissions.isEmpty()) return true;
        
        // Check if player has any of the required permissions
        for (String permission : permissions) {
            if (hasPermission(player, permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Helper method to check individual permission
     */
    private boolean hasPermission(ServerPlayer player, String permission) {
        // Implementation depends on your permission system
        // For now, return true for ops
        return player.hasPermissions(2);
    }
    
    /**
     * Gets the total number of items in this kit
     */
    public int getTotalItems() {
        return items.stream().mapToInt(ItemStack::getCount).sum();
    }
    
    /**
     * Gets unique item types in this kit
     */
    public int getUniqueItems() {
        return items.size();
    }
    
    /**
     * Creates a preview of the kit items
     */
    public List<String> getItemPreview() {
        List<String> preview = new ArrayList<>();
        
        for (ItemStack item : items) {
            String itemName = item.getDisplayName().getString();
            int count = item.getCount();
            
            if (count > 1) {
                preview.add("§7- §b" + count + "x §f" + itemName);
            } else {
                preview.add("§7- §f" + itemName);
            }
        }
        
        return preview;
    }
    
    @Override
    public String toString() {
        return "Kit{" +
                "name='" + name + '\'' +
                ", items=" + items.size() +
                ", cooldown=" + cooldown +
                ", category=" + category +
                ", cost=" + cost +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Kit kit = (Kit) o;
        return Objects.equals(name, kit.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
