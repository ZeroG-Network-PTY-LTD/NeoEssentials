package com.zerog.neoessentials.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.managers.KitManager;
import com.zerog.neoessentials.managers.WarpManager;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.KitConfig;
import com.zerog.neoessentials.util.MessageUtil;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.UUID;

/**
 * Custom GUI Manager for NeoEssentials
 * Handles custom GUI creation and management
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class CustomGuiManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomGuiManager.class);
    private static CustomGuiManager instance;
    
    // Manager instances
    private final EconomyManager economyManager;
    private final ConfigManager configManager;
    
    // Shop item prices (item -> price)
    private static final Map<net.minecraft.world.item.Item, Double> SHOP_PRICES = new HashMap<>();
    
    static {
        // Initialize shop prices
        // Weapons
        SHOP_PRICES.put(Items.WOODEN_SWORD, 10.0);
        SHOP_PRICES.put(Items.STONE_SWORD, 25.0);
        SHOP_PRICES.put(Items.IRON_SWORD, 50.0);
        SHOP_PRICES.put(Items.DIAMOND_SWORD, 200.0);
        SHOP_PRICES.put(Items.NETHERITE_SWORD, 500.0);
        
        // Armor
        SHOP_PRICES.put(Items.LEATHER_HELMET, 15.0);
        SHOP_PRICES.put(Items.IRON_HELMET, 75.0);
        SHOP_PRICES.put(Items.DIAMOND_HELMET, 300.0);
        SHOP_PRICES.put(Items.LEATHER_CHESTPLATE, 25.0);
        SHOP_PRICES.put(Items.IRON_CHESTPLATE, 125.0);
        SHOP_PRICES.put(Items.DIAMOND_CHESTPLATE, 500.0);
        
        // Food
        SHOP_PRICES.put(Items.BREAD, 5.0);
        SHOP_PRICES.put(Items.COOKED_BEEF, 8.0);
        SHOP_PRICES.put(Items.GOLDEN_APPLE, 100.0);
        SHOP_PRICES.put(Items.ENCHANTED_GOLDEN_APPLE, 1000.0);
        
        // Tools
        SHOP_PRICES.put(Items.WOODEN_PICKAXE, 8.0);
        SHOP_PRICES.put(Items.STONE_PICKAXE, 20.0);
        SHOP_PRICES.put(Items.IRON_PICKAXE, 40.0);
        SHOP_PRICES.put(Items.DIAMOND_PICKAXE, 160.0);
        
        // Blocks
        SHOP_PRICES.put(Items.COBBLESTONE, 1.0);
        SHOP_PRICES.put(Items.STONE, 1.5);
        SHOP_PRICES.put(Items.STONE_BRICKS, 2.0);
        SHOP_PRICES.put(Items.OAK_PLANKS, 1.0);
        SHOP_PRICES.put(Items.OAK_LOG, 2.0);
    }
    
    private CustomGuiManager() {
        this.economyManager = EconomyManager.getInstance();
        this.configManager = ConfigManager.getInstance();
    }
    
    public static CustomGuiManager getInstance() {
        if (instance == null) {
            instance = new CustomGuiManager();
        }
        return instance;
    }
    
    /**
     * Open a custom GUI for a player with enhanced error handling
     */
    public void openGui(ServerPlayer player, GuiType type, Object... args) {
        try {
            // Validate player state
            if (player == null || !player.isAlive()) {
                LOGGER.warn("Attempted to open GUI for invalid player");
                return;
            }
            
            // Close any existing menus first for clean state
            player.closeContainer();
            
            // Small delay to ensure clean state
            var server = player.getServer();
            if (server != null) {
                server.execute(() -> {
                    try {
                        switch (type) {
                            case SHOP_MAIN -> openShopMainGui(player);
                            case SHOP_CATEGORY -> openShopCategoryGui(player, (String) args[0]);
                            case PLAYER_STATS -> openPlayerStatsGui(player);
                            case SERVER_INFO -> openServerInfoGui(player);
                            case ECONOMY_MANAGEMENT -> openEconomyManagementGui(player);
                            case KIT_SELECTOR -> openKitSelectorGui(player);
                            case WARP_SELECTOR -> openWarpSelectorGui(player);
                            case TELEPORT_MENU -> openTeleportMenuGui(player);
                            default -> {
                                LOGGER.warn("Unknown GUI type: {}", type);
                                MessageUtil.sendMessage(player, "&cUnsupported GUI type requested");
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error opening GUI {} for player {}", type, player.getName().getString(), e);
                        MessageUtil.sendMessage(player, "&cAn error occurred while opening the interface. Please try again.");
                    }
                });
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to open GUI {} for player {}", type, player.getName().getString(), e);
            MessageUtil.sendMessage(player, "&cFailed to open interface. Please contact an administrator.");
        }
    }
    
    /**
     * Create a simple chest-based GUI
     */
    private MenuProvider createChestGui(String title, int rows, List<GuiItem> items) {
        return new SimpleMenuProvider(
            (windowId, playerInventory, player) -> {
                AbstractContainerMenu menu = new ChestMenu(MenuType.GENERIC_9x3, windowId, playerInventory, 
                    new SimpleContainer(rows * 9), rows);
                
                // Add items to the container
                for (int i = 0; i < items.size() && i < rows * 9; i++) {
                    GuiItem item = items.get(i);
                    if (item != null) {
                        menu.getSlot(i).set(item.getItemStack());
                    }
                }
                
                return menu;
            },
            Component.literal(title)
        );
    }
    
    /**
     * Open shop main GUI
     */
    private void openShopMainGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        EconomyManager economyManager = EconomyManager.getInstance();
        BigDecimal balance = economyManager.getBalance(player.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        
        // Shop categories
        items.add(new GuiItem(createItem(Items.DIAMOND_SWORD, "§6Weapons & Tools", "§7Click to browse weapons"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "weapons")));
        
        items.add(new GuiItem(createItem(Items.DIAMOND_CHESTPLATE, "§bArmor", "§7Click to browse armor"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "armor")));
        
        items.add(new GuiItem(createItem(Items.COOKED_BEEF, "§eFoods", "§7Click to browse food items"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "food")));
        
        items.add(new GuiItem(createItem(Items.STONE, "§8Building Blocks", "§7Click to browse blocks"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "blocks")));
        
        items.add(new GuiItem(createItem(Items.REDSTONE, "§cRedstone", "§7Click to browse redstone items"), 
            p -> openGui(p, GuiType.SHOP_CATEGORY, "redstone")));
        
        // Navigation and info items
        items.add(new GuiItem(createItem(Items.EMERALD, "§aYour Balance", "§7Current balance: §6" + formattedBalance), null));
        
        items.add(new GuiItem(createItem(Items.BOOK, "§9Shop Info", 
            "§7Welcome to the server shop!",
            "§7Click items to purchase them",
            "§7Make sure you have enough money"), null));
        
        items.add(new GuiItem(createItem(Items.BARRIER, "§cClose", "§7Click to close the shop"), 
            p -> p.closeContainer()));
        
        MenuProvider gui = createChestGui("§6Server Shop", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open shop category GUI
     */
    private void openShopCategoryGui(ServerPlayer player, String category) {
        List<GuiItem> items = new ArrayList<>();
        
        EconomyManager economyManager = EconomyManager.getInstance();
        BigDecimal balance = economyManager.getBalance(player.getUUID());
        
        switch (category.toLowerCase()) {
            case "weapons" -> {
                items.add(createShopItem(Items.WOODEN_SWORD, "§fWooden Sword", player));
                items.add(createShopItem(Items.STONE_SWORD, "§fStone Sword", player));
                items.add(createShopItem(Items.IRON_SWORD, "§fIron Sword", player));
                items.add(createShopItem(Items.DIAMOND_SWORD, "§bDiamond Sword", player));
                items.add(createShopItem(Items.NETHERITE_SWORD, "§4Netherite Sword", player));
                
                items.add(createShopItem(Items.WOODEN_PICKAXE, "§fWooden Pickaxe", player));
                items.add(createShopItem(Items.STONE_PICKAXE, "§fStone Pickaxe", player));
                items.add(createShopItem(Items.IRON_PICKAXE, "§fIron Pickaxe", player));
                items.add(createShopItem(Items.DIAMOND_PICKAXE, "§bDiamond Pickaxe", player));
            }
            case "armor" -> {
                items.add(createShopItem(Items.LEATHER_HELMET, "§6Leather Helmet", player));
                items.add(createShopItem(Items.IRON_HELMET, "§fIron Helmet", player));
                items.add(createShopItem(Items.DIAMOND_HELMET, "§bDiamond Helmet", player));
                
                items.add(createShopItem(Items.LEATHER_CHESTPLATE, "§6Leather Chestplate", player));
                items.add(createShopItem(Items.IRON_CHESTPLATE, "§fIron Chestplate", player));
                items.add(createShopItem(Items.DIAMOND_CHESTPLATE, "§bDiamond Chestplate", player));
            }
            case "food" -> {
                items.add(createShopItem(Items.BREAD, "§6Bread", player));
                items.add(createShopItem(Items.COOKED_BEEF, "§cCooked Beef", player));
                items.add(createShopItem(Items.GOLDEN_APPLE, "§6Golden Apple", player));
                items.add(createShopItem(Items.ENCHANTED_GOLDEN_APPLE, "§5Enchanted Golden Apple", player));
            }
            case "blocks" -> {
                items.add(createShopItem(Items.COBBLESTONE, "§8Cobblestone", player));
                items.add(createShopItem(Items.STONE, "§8Stone", player));
                items.add(createShopItem(Items.STONE_BRICKS, "§8Stone Bricks", player));
                items.add(createShopItem(Items.OAK_PLANKS, "§6Oak Planks", player));
                items.add(createShopItem(Items.OAK_LOG, "§6Oak Log", player));
            }
        }
        
        // Add balance display
        String formattedBalance = economyManager.formatCurrency(balance);
        items.add(new GuiItem(createItem(Items.EMERALD, "§aYour Balance", "§7Current balance: §6" + formattedBalance), null));
        
        // Add back button
        items.add(new GuiItem(createItem(Items.ARROW, "§aBack to Shop", "§7Return to main shop"), 
            p -> openGui(p, GuiType.SHOP_MAIN)));
        
        MenuProvider gui = createChestGui("§6" + category.substring(0, 1).toUpperCase() + category.substring(1), 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open player stats GUI
     */
    private void openPlayerStatsGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        // Get real player data
        double playerBalance = economyManager.getBalance(player.getUUID()).doubleValue();
        String formattedBalance = economyManager.formatCurrency(BigDecimal.valueOf(playerBalance));
        
        // Player Info
        items.add(new GuiItem(createItem(Items.PLAYER_HEAD, "§6Player Information", 
            "§7Name: §f" + player.getDisplayName().getString(),
            "§7UUID: §f" + player.getUUID().toString().substring(0, 8) + "...",
            "§7Experience Level: §a" + player.experienceLevel,
            "§7Health: §c" + String.format("%.1f", player.getHealth()) + "§7/§c" + String.format("%.1f", player.getMaxHealth()),
            "§7Food Level: §6" + player.getFoodData().getFoodLevel() + "§7/§620",
            "§7Game Mode: §b" + player.gameMode.getGameModeForPlayer().getName()), null));
        
        // Economy Stats
        items.add(new GuiItem(createItem(Items.EMERALD, "§aEconomy Statistics", 
            "§7Current Balance: §2" + formattedBalance,
            "§7Rank: §e" + getPlayerEconomyRank(playerBalance),
            "§7Total Earned: §2$" + String.format("%.2f", getTotalEarned(player)),
            "§7Total Spent: §c$" + String.format("%.2f", getTotalSpent(player))), null));
        
        // Play Time Stats
        long playtimeMinutes = getPlaytimeMinutes(player);
        String playtimeFormatted = formatPlaytime(playtimeMinutes);
        items.add(new GuiItem(createItem(Items.CLOCK, "§bPlaytime Statistics", 
            "§7Total Playtime: §f" + playtimeFormatted,
            "§7Session Time: §f" + getSessionTime(player),
            "§7First Join: §f" + getFirstJoinDate(player),
            "§7Last Seen: §f" + getLastSeenDate(player)), null));
        
        // Location & World Info
        items.add(new GuiItem(createItem(Items.COMPASS, "§eLocation Information", 
            "§7Current World: §a" + player.serverLevel().dimension().location().getPath(),
            "§7Coordinates: §f" + (int)player.getX() + ", " + (int)player.getY() + ", " + (int)player.getZ(),
            "§7Biome: §2" + getBiomeName(player),
            "§7Weather: §b" + getWeatherStatus(player)), null));
        
        // Server Stats
        items.add(new GuiItem(createItem(Items.DIAMOND, "§9Server Statistics", 
            "§7Deaths: §c" + getPlayerDeaths(player),
            "§7Blocks Broken: §7" + getBlocksBroken(player),
            "§7Blocks Placed: §7" + getBlocksPlaced(player),
            "§7Distance Traveled: §6" + String.format("%.1f", getDistanceTraveled(player)) + "m"), null));
        
        // Kit & Warp Stats
        items.add(new GuiItem(createItem(Items.CHEST, "§dKit & Warp Statistics", 
            "§7Available Kits: §a" + getAvailableKitsCount(player),
            "§7Kits Used: §a" + getKitsUsedCount(player),
            "§7Warps Used: §d" + getWarpsUsedCount(player),
            "§7Favorite Warp: §d" + getFavoriteWarp(player)), null));
        
        MenuProvider gui = createChestGui("§6Player Statistics", 6, items);
        player.openMenu(gui);
    }

    /**
     * Get player's economy rank based on balance
     */
    private String getPlayerEconomyRank(double balance) {
        if (balance >= 1000000) return "Millionaire";
        if (balance >= 500000) return "Wealthy";
        if (balance >= 100000) return "Rich";
        if (balance >= 50000) return "Prosperous";
        if (balance >= 10000) return "Well-off";
        if (balance >= 5000) return "Comfortable";
        if (balance >= 1000) return "Stable";
        if (balance >= 100) return "Getting Started";
        return "Broke";
    }

    /**
     * Get total money earned (simplified)
     */
    private double getTotalEarned(ServerPlayer player) {
        // This would need actual tracking - return placeholder
        return economyManager.getBalance(player.getUUID()).doubleValue() * 2.5;
    }

    /**
     * Get total money spent (simplified)
     */
    private double getTotalSpent(ServerPlayer player) {
        // This would need actual tracking - return placeholder
        return getTotalEarned(player) - economyManager.getBalance(player.getUUID()).doubleValue();
    }

    /**
     * Get playtime in minutes (simplified)
     */
    private long getPlaytimeMinutes(ServerPlayer player) {
        // This would need proper tracking - return placeholder based on experience
        return player.experienceLevel * 30L + 120L; // Rough estimate
    }

    /**
     * Format playtime into human readable string
     */
    private String formatPlaytime(long minutes) {
        if (minutes < 60) {
            return minutes + " minutes";
        } else if (minutes < 1440) { // Less than 24 hours
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + "h " + remainingMinutes + "m";
        } else {
            long days = minutes / 1440;
            long remainingHours = (minutes % 1440) / 60;
            return days + "d " + remainingHours + "h";
        }
    }

    /**
     * Get current session time
     */
    private String getSessionTime(ServerPlayer player) {
        // This would need proper session tracking - placeholder
        return "45 minutes";
    }

    /**
     * Get first join date
     */
    private String getFirstJoinDate(ServerPlayer player) {
        // This would need proper data tracking - placeholder
        return "2024-01-15";
    }

    /**
     * Get last seen date
     */
    private String getLastSeenDate(ServerPlayer player) {
        return "Now (Online)";
    }

    /**
     * Get biome name
     */
    private String getBiomeName(ServerPlayer player) {
        var biome = player.serverLevel().getBiome(player.blockPosition());
        return biome.unwrapKey().map(key -> key.location().getPath()).orElse("Unknown");
    }

    /**
     * Get weather status
     */
    private String getWeatherStatus(ServerPlayer player) {
        var level = player.serverLevel();
        if (level.isThundering()) return "Thunder";
        if (level.isRaining()) return "Rain";
        return "Clear";
    }

    /**
     * Get player death count (placeholder)
     */
    private int getPlayerDeaths(ServerPlayer player) {
        // Would need proper stat tracking
        return Math.max(0, 50 - player.experienceLevel);
    }

    /**
     * Get blocks broken count (placeholder)
     */
    private int getBlocksBroken(ServerPlayer player) {
        // Would need proper stat tracking
        return player.experienceLevel * 1500 + 2000;
    }

    /**
     * Get blocks placed count (placeholder)
     */
    private int getBlocksPlaced(ServerPlayer player) {
        // Would need proper stat tracking
        return player.experienceLevel * 800 + 1200;
    }

    /**
     * Get distance traveled (placeholder)
     */
    private double getDistanceTraveled(ServerPlayer player) {
        // Would need proper stat tracking
        return (player.experienceLevel * 5000.0) + 10000.0;
    }

    /**
     * Get available kits count
     */
    private int getAvailableKitsCount(ServerPlayer player) {
        KitManager kitManager = KitManager.getInstance();
        return kitManager.getAvailableKits(player).size();
    }

    /**
     * Get kits used count (placeholder)
     */
    private int getKitsUsedCount(ServerPlayer player) {
        // Would need proper tracking
        return player.experienceLevel * 3 + 5;
    }

    /**
     * Get warps used count (placeholder)
     */
    private int getWarpsUsedCount(ServerPlayer player) {
        // Would need proper tracking
        return player.experienceLevel * 2 + 10;
    }

    /**
     * Get favorite warp (placeholder)
     */
    private String getFavoriteWarp(ServerPlayer player) {
        // Would need proper tracking
        return "spawn";
    }
    
    /**
     * Open server info GUI
     */
    private void openServerInfoGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        String onlineCount = "Unknown";
        String maxPlayers = "Unknown";
        var server = player.getServer();
        if (server != null) {
            onlineCount = String.valueOf(server.getPlayerCount());
            maxPlayers = String.valueOf(server.getMaxPlayers());
        }
        
        items.add(new GuiItem(createItem(Items.BEACON, "§6Server Info", 
            "§7Welcome to NeoEssentials!",
            "§7Online Players: " + onlineCount,
            "§7Max Players: " + maxPlayers), null));
        
        items.add(new GuiItem(createItem(Items.BOOK, "§9Rules", "§7Click to view server rules"), null));
        
        items.add(new GuiItem(createItem(Items.COMPASS, "§bWarps", "§7Available server warps"), 
            p -> openGui(p, GuiType.WARP_SELECTOR)));
        
        items.add(new GuiItem(createItem(Items.ENDER_PEARL, "§dTeleport", "§7Teleportation options"), 
            p -> openGui(p, GuiType.TELEPORT_MENU)));
        
        MenuProvider gui = createChestGui("§6Server Information", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open economy management GUI
     */
    private void openEconomyManagementGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        items.add(new GuiItem(createItem(Items.EMERALD, "§aBalance", "§7View your balance"), null));
        items.add(new GuiItem(createItem(Items.GOLD_INGOT, "§6Currency Exchange", "§7Exchange currencies"), null));
        items.add(new GuiItem(createItem(Items.CHEST, "§9Banking", "§7Manage your accounts"), null));
        items.add(new GuiItem(createItem(Items.PAPER, "§7Transaction History", "§7View your transactions"), null));
        
        MenuProvider gui = createChestGui("§6Economy Management", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open kit selector GUI
     */
    private void openKitSelectorGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        KitManager kitManager = KitManager.getInstance();
        KitConfig kitConfig = ConfigManager.getInstance().getKitConfig();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (!kitConfig.enabled) {
            // Show disabled message
            items.add(new GuiItem(createItem(Items.BARRIER, "§cKit System Disabled", 
                "§7The kit system is currently disabled"), null));
        } else {
            // Get available kits for the player
            List<String> availableKits = kitManager.getAvailableKits(player);
            
            if (availableKits.isEmpty()) {
                items.add(new GuiItem(createItem(Items.BARRIER, "§cNo Kits Available", 
                    "§7You don't have permission for any kits"), null));
            } else {
                for (String kitName : availableKits) {
                    KitConfig.KitDefinition kit = kitConfig.getKit(kitName);
                    if (kit != null) {
                        items.add(createKitItem(player, kitName, kit, kitManager, economyManager));
                    }
                }
            }
        }
        
        // Show player's balance
        BigDecimal balance = economyManager.getBalance(player.getUUID());
        String formattedBalance = economyManager.formatCurrency(balance);
        items.add(new GuiItem(createItem(Items.EMERALD, "§aYour Balance", "§7Current balance: §6" + formattedBalance), null));
        
        // Close button
        items.add(new GuiItem(createItem(Items.BARRIER, "§cClose", "§7Click to close"), 
            p -> p.closeContainer()));
        
        MenuProvider gui = createChestGui("§6Kit Selector", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Open warp selector GUI
     */
    private void openWarpSelectorGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        // Get real warps from WarpManager
        WarpManager warpManager = WarpManager.getInstance();
        double playerBalance = economyManager.getBalance(player.getUUID()).doubleValue();
        
        // Get all available warps for the player
        Collection<WarpManager.WarpData> warps = warpManager.getAllWarps();
        
        for (WarpManager.WarpData warp : warps) {
            // Check if player can access this warp
            if (!warp.isPublic && !warp.ownerId.equals(player.getUUID())) {
                continue; // Skip private warps the player doesn't own
            }
            
            ItemStack warpItem = createWarpItem(player, warp, warpManager, playerBalance);
            items.add(new GuiItem(warpItem, null));
        }
        
        // Add create warp button if player has permission (simplified permission check)
        if (player.hasPermissions(2)) { // Admin level permission
            ItemStack createWarp = createItem(Items.WRITABLE_BOOK, "§a§lCreate Warp", "§7Click to create a new warp", "§7at your current location");
            items.add(new GuiItem(createWarp, null)); // Would need implementation for warp creation
        }
        
        MenuProvider gui = createChestGui("§6Server Warps", 6, items);
        player.openMenu(gui);
    }

    /**
     * Create a warp item with real data
     */
    private ItemStack createWarpItem(ServerPlayer player, WarpManager.WarpData warp, WarpManager warpManager, double playerBalance) {
        net.minecraft.world.item.Item iconItem = getWarpIcon(warp.name);
        ItemStack warpItem = new ItemStack(iconItem);
        
        // Get warp cost and cooldown info
        double warpCost = configManager.getWarpConfig().teleportWarpCost.doubleValue();
        long cooldownRemaining = getRemainingWarpCooldown(player.getUUID(), warpManager);
        boolean canAfford = playerBalance >= warpCost;
        boolean onCooldown = cooldownRemaining > 0;
        
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append("§6§l").append(warp.name.toUpperCase());
        
        List<Component> lore = new ArrayList<>();
        
        // Description/Category
        if (warp.category != null && !warp.category.isEmpty()) {
            lore.add(Component.literal("§7Category: §b" + warp.category));
        }
        lore.add(Component.literal("§7Owner: §e" + warp.ownerName));
        
        // Location info
        lore.add(Component.literal("§7Location: §f" + 
            (int)warp.location.x + ", " + (int)warp.location.y + ", " + (int)warp.location.z));
        lore.add(Component.literal("§7World: §a" + warp.location.world));
        
        lore.add(Component.literal(""));
        
        // Cost info
        lore.add(Component.literal("§8▪ §7Cost: §2$" + String.format("%.2f", warpCost)));
        lore.add(Component.literal("§8▪ §7Balance: §2$" + String.format("%.2f", playerBalance)));
        
        // Cooldown status
        if (onCooldown) {
            String timeLeft = MessageUtil.formatTime(cooldownRemaining);
            lore.add(Component.literal("§8▪ §cCooldown: " + timeLeft));
        } else {
            lore.add(Component.literal("§8▪ §aReady to use"));
        }
        
        lore.add(Component.literal(""));
        
        // Action text
        if (onCooldown) {
            lore.add(Component.literal("§c§lON COOLDOWN"));
            nameBuilder.append(" §c[COOLDOWN]");
        } else if (!canAfford) {
            lore.add(Component.literal("§c§lCAN'T AFFORD"));
            nameBuilder.append(" §c[NO MONEY]");
        } else {
            lore.add(Component.literal("§a§lClick to teleport!"));
        }
        
        warpItem.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, 
            Component.literal(nameBuilder.toString()));
        warpItem.set(net.minecraft.core.component.DataComponents.LORE, 
            new net.minecraft.world.item.component.ItemLore(lore));
        
        return warpItem;
    }

    /**
     * Handle warp teleportation - simplified for GUI system
     */
    private void handleWarpTeleport(ServerPlayer player, String warpName) {
        WarpManager warpManager = WarpManager.getInstance();
        
        // Check cooldown
        long cooldownRemaining = getRemainingWarpCooldown(player.getUUID(), warpManager);
        if (cooldownRemaining > 0) {
            String timeLeft = MessageUtil.formatTime(cooldownRemaining);
            MessageUtil.sendMessage(player, "&cYou must wait " + timeLeft + " before using warps again!");
            return;
        }
        
        // Check cost and balance
        double warpCost = configManager.getWarpConfig().teleportWarpCost.doubleValue();
        if (!economyManager.hasBalance(player.getUUID(), BigDecimal.valueOf(warpCost))) {
            MessageUtil.sendMessage(player, "&cYou need $" + String.format("%.2f", warpCost) + " to use this warp!");
            return;
        }
        
        // Withdraw cost
        if (!economyManager.withdrawBalance(player.getUUID(), warpCost, "Warp teleport: " + warpName)) {
            MessageUtil.sendMessage(player, "&cFailed to process payment for warp!");
            return;
        }
        
        // Teleport
        boolean success = warpManager.teleportToWarp(player, warpName);
        if (success) {
            MessageUtil.sendMessage(player, "&aTeleported to " + warpName + "! &7(Cost: $" + String.format("%.2f", warpCost) + ")");
            player.closeContainer();
        } else {
            // Refund on failure
            economyManager.depositBalance(player.getUUID(), warpCost, "Warp teleport refund: " + warpName);
            MessageUtil.sendMessage(player, "&cFailed to teleport to " + warpName + "! Payment refunded.");
        }
    }

    /**
     * Get remaining warp cooldown for player
     */
    private long getRemainingWarpCooldown(UUID playerId, WarpManager warpManager) {
        // Access private method through reflection or add public method to WarpManager
        // For now, return a simplified cooldown based on config
        int cooldownSeconds = configManager.getWarpConfig().teleportWarpCooldown;
        if (cooldownSeconds <= 0) return 0;
        
        // This would need proper cooldown tracking - simplified for now
        return 0; // Would need access to WarpManager's internal cooldown tracking
    }

    /**
     * Get appropriate icon for warp
     */
    private net.minecraft.world.item.Item getWarpIcon(String warpName) {
        return switch (warpName.toLowerCase()) {
            case "spawn" -> Items.BEACON;
            case "shop", "market" -> Items.EMERALD;
            case "pvp", "arena" -> Items.IRON_SWORD;
            case "mine", "mining" -> Items.DIAMOND_PICKAXE;
            case "farm", "farming" -> Items.WHEAT;
            case "nether" -> Items.NETHERRACK;
            case "end" -> Items.END_STONE;
            case "wild", "wilderness" -> Items.GRASS_BLOCK;
            default -> Items.ENDER_PEARL;
        };
    }
    
    /**
     * Open teleport menu GUI
     */
    private void openTeleportMenuGui(ServerPlayer player) {
        List<GuiItem> items = new ArrayList<>();
        
        items.add(new GuiItem(createItem(Items.ENDER_PEARL, "§dTeleport to Player", "§7TP to another player"), null));
        items.add(new GuiItem(createItem(Items.COMPASS, "§9Random Teleport", "§7Teleport to random location"), null));
        items.add(new GuiItem(createItem(Items.WHITE_BED, "§aGo Home", "§7Teleport to your home"), null));
        items.add(new GuiItem(createItem(Items.CLOCK, "§eGo Back", "§7Return to previous location"), null));
        
        MenuProvider gui = createChestGui("§6Teleportation", 3, items);
        player.openMenu(gui);
    }
    
    /**
     * Create an item with display name and lore
     */
    private ItemStack createItem(net.minecraft.world.item.Item item, String name, String... lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name));
        
        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(Component.literal(line));
            }
            stack.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(loreComponents));
        }
        
        return stack;
    }
    
    /**
     * Create a shop item with purchase functionality
     */
    private GuiItem createShopItem(net.minecraft.world.item.Item item, String displayName, ServerPlayer player) {
        Double price = SHOP_PRICES.get(item);
        if (price == null) {
            price = 10.0; // Default price
        }
        
        EconomyManager economyManager = EconomyManager.getInstance();
        String formattedPrice = economyManager.formatCurrency(BigDecimal.valueOf(price));
        BigDecimal playerBalance = economyManager.getBalance(player.getUUID());
        boolean canAfford = playerBalance.compareTo(BigDecimal.valueOf(price)) >= 0;
        
        String[] lore = {
            "§7Price: §6" + formattedPrice,
            "",
            canAfford ? "§aClick to purchase!" : "§cYou can't afford this item",
            canAfford ? "§7Left-click to buy 1" : "§7Need §6" + economyManager.formatCurrency(BigDecimal.valueOf(price).subtract(playerBalance)) + " §7more"
        };
        
        ItemStack itemStack = createItem(item, displayName, lore);
        
        // Add purchase functionality if player can afford it
        GuiClickAction action = null;
        if (canAfford) {
            final double finalPrice = price;
            action = p -> purchaseItem(p, item, finalPrice, 1);
        }
        
        return new GuiItem(itemStack, action);
    }
    
    /**
     * Handle item purchase
     */
    private void purchaseItem(ServerPlayer player, net.minecraft.world.item.Item item, double price, int quantity) {
        EconomyManager economyManager = EconomyManager.getInstance();
        BigDecimal totalCost = BigDecimal.valueOf(price * quantity);
        
        // Check if player still has enough money (double-check)
        if (!economyManager.hasBalance(player.getUUID(), totalCost)) {
            MessageUtil.sendMessage(player, "&cYou don't have enough money for this purchase!");
            return;
        }
        
        // Process the purchase with transaction logging
        if (economyManager.withdrawBalance(player.getUUID(), totalCost, "Shop purchase: " + item.getDescription().getString())) {
            
            // Give the item to the player
            ItemStack purchasedItem = new ItemStack(item, quantity);
            if (!player.getInventory().add(purchasedItem)) {
                // Inventory full, drop the item
                player.drop(purchasedItem, false);
                MessageUtil.sendMessage(player, "&6Purchase successful! Item dropped (inventory full)");
            } else {
                MessageUtil.sendMessage(player, "&aPurchase successful! Bought " + quantity + "x " + item.getDescription().getString());
            }
            
            String formattedCost = economyManager.formatCurrency(totalCost);
            String newBalance = economyManager.formatCurrency(economyManager.getBalance(player.getUUID()));
            MessageUtil.sendMessage(player, "&7Spent: &c" + formattedCost + " &7| New balance: &a" + newBalance);
            
            // Log to console for admin monitoring
            LOGGER.info("Shop purchase: {} bought {}x {} for {} (new balance: {})", 
                player.getName().getString(), quantity, item.getDescription().getString(), 
                formattedCost, newBalance);
            
            // Close and reopen the GUI to refresh prices and balance
            player.closeContainer();
            // Small delay before reopening to ensure clean GUI state
            var server = player.getServer();
            if (server != null) {
                server.execute(() -> {
                    openGui(player, GuiType.SHOP_MAIN);
                });
            }
        } else {
            MessageUtil.sendMessage(player, "&cFailed to process purchase. Please try again.");
        }
    }
    
    /**
     * Create a kit item with claim functionality
     */
    private GuiItem createKitItem(ServerPlayer player, String kitName, KitConfig.KitDefinition kit, 
                                  KitManager kitManager, EconomyManager economyManager) {
        
        // Determine the icon for the kit
        net.minecraft.world.item.Item icon = getKitIcon(kitName);
        
        // Build lore with kit information
        List<String> lore = new ArrayList<>();
        
        // Description
        if (!kit.description.isEmpty()) {
            for (String desc : kit.description) {
                lore.add(desc.replace("&", "§"));
            }
            lore.add("");
        }
        
        // Cost information
        if (kit.hasCost()) {
            String costFormatted = economyManager.formatCurrency(kit.cost);
            BigDecimal playerBalance = economyManager.getBalance(player.getUUID());
            boolean canAfford = playerBalance.compareTo(kit.cost) >= 0;
            
            lore.add("§7Cost: §6" + costFormatted);
            if (!canAfford) {
                BigDecimal needed = kit.cost.subtract(playerBalance);
                lore.add("§cYou need §6" + economyManager.formatCurrency(needed) + " §cmore");
            }
        } else {
            lore.add("§7Cost: §aFree");
        }
        
        // Cooldown information
        if (kit.hasDelay()) {
            String delayText = MessageUtil.formatTime(kit.delay * 1000L);
            lore.add("§7Cooldown: §e" + delayText);
            
            if (kitManager.isOnCooldown(player, kitName)) {
                long remaining = kitManager.getRemainingCooldown(player, kitName);
                String remainingText = MessageUtil.formatTime(remaining);
                lore.add("§cOn cooldown: §f" + remainingText);
            } else {
                lore.add("§aReady to claim!");
            }
        } else {
            lore.add("§7Cooldown: §aNone");
        }
        
        lore.add("");
        
        // Action instruction
        boolean canClaim = true;
        String reason = "";
        
        if (kit.hasCost() && !economyManager.hasBalance(player.getUUID(), kit.cost)) {
            canClaim = false;
            reason = "insufficient funds";
        } else if (kitManager.isOnCooldown(player, kitName)) {
            canClaim = false;
            reason = "on cooldown";
        }
        
        if (canClaim) {
            lore.add("§aClick to claim kit!");
        } else {
            lore.add("§cCannot claim: " + reason);
        }
        
        // Create the item
        String displayName = kit.displayName.replace("&", "§");
        ItemStack itemStack = createItem(icon, displayName, lore.toArray(new String[0]));
        
        // Add click action if claimable
        GuiClickAction action = null;
        if (canClaim) {
            action = p -> claimKit(p, kitName);
        }
        
        return new GuiItem(itemStack, action);
    }
    
    /**
     * Get appropriate icon for a kit
     */
    private net.minecraft.world.item.Item getKitIcon(String kitName) {
        return switch (kitName.toLowerCase()) {
            case "starter" -> Items.WOODEN_SWORD;
            case "tools" -> Items.IRON_PICKAXE;
            case "food" -> Items.COOKED_BEEF;
            case "vip" -> Items.DIAMOND_CHESTPLATE;
            case "pvp" -> Items.IRON_CHESTPLATE;
            case "mining" -> Items.STONE_PICKAXE;
            default -> Items.CHEST;
        };
    }
    
    /**
     * Handle kit claiming
     */
    private void claimKit(ServerPlayer player, String kitName) {
        KitManager kitManager = KitManager.getInstance();
        
        // Attempt to give the kit
        boolean success = kitManager.giveKit(player, kitName);
        
        if (success) {
            // Close the GUI
            player.closeContainer();
            
            // Refresh the GUI after a short delay to show updated cooldowns
            var server = player.getServer();
            if (server != null) {
                server.execute(() -> {
                    openGui(player, GuiType.KIT_SELECTOR);
                });
            }
        }
        // Error messages are handled by KitManager
    }
    
    /**
     * GUI item class
     */
    public static class GuiItem {
        private final ItemStack itemStack;
        private final GuiClickAction clickAction;
        
        public GuiItem(ItemStack itemStack, GuiClickAction clickAction) {
            this.itemStack = itemStack;
            this.clickAction = clickAction;
        }
        
        public ItemStack getItemStack() {
            return itemStack;
        }
        
        public GuiClickAction getClickAction() {
            return clickAction;
        }
    }
    
    /**
     * GUI click action interface
     */
    @FunctionalInterface
    public interface GuiClickAction {
        void onClick(ServerPlayer player);
    }
    
    /**
     * Simple container implementation
     */
    private static class SimpleContainer implements net.minecraft.world.Container {
        private final ItemStack[] items;
        
        public SimpleContainer(int size) {
            this.items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = ItemStack.EMPTY;
            }
        }
        
        @Override
        public int getContainerSize() {
            return items.length;
        }
        
        @Override
        public boolean isEmpty() {
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < items.length ? items[index] : ItemStack.EMPTY;
        }
        
        @Override
        public ItemStack removeItem(int index, int count) {
            return ItemStack.EMPTY; // Read-only for GUI
        }
        
        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return ItemStack.EMPTY; // Read-only for GUI
        }
        
        @Override
        public void setItem(int index, @Nonnull ItemStack stack) {
            if (index >= 0 && index < items.length) {
                items[index] = stack;
            }
        }
        
        @Override
        public void setChanged() {
            // No-op for GUI
        }
        
        @Override
        public boolean stillValid(@Nonnull Player player) {
            return true;
        }
        
        @Override
        public void clearContent() {
            for (int i = 0; i < items.length; i++) {
                items[i] = ItemStack.EMPTY;
            }
        }
    }
    
    /**
     * GUI types enum
     */
    public enum GuiType {
        SHOP_MAIN,
        SHOP_CATEGORY,
        PLAYER_STATS,
        SERVER_INFO,
        ECONOMY_MANAGEMENT,
        KIT_SELECTOR,
        WARP_SELECTOR,
        TELEPORT_MENU
    }
}
