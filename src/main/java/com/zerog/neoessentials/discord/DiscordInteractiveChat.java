package com.zerog.neoessentials.discord;

import com.zerog.neoessentials.error.ErrorHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.awt.Color;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * InteractiveChat-Style System for NeoEssentials
 * Complete recreation of the popular InteractiveChat plugin functionality
 * 
 * Features:
 * - [item] - Show held item with hover tooltip and click actions
 * - [inv]/[inventory] - Display inventory with clickable GUI access
 * - [ender]/[enderchest]/[echest] - Show ender chest contents
 * - [pos] - Display current coordinates
 * - [health] - Show current health status
 * - [time] - Display current server time
 * - Rich in-game clickable/hoverable components
 * - Cross-player interaction (view others' items/inventories)
 * - Permission-based access control
 * - Discord integration with detailed embeds
 * 
 * @author ZeroG
 * @since 2.0.0
 */
@EventBusSubscriber(modid = "neoessentials")
public class DiscordInteractiveChat {
    
    // Core InteractiveChat patterns
    private static final Pattern ITEM_TAG = Pattern.compile("\\[item\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVENTORY_TAG = Pattern.compile("\\[(?:inventory|inv)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENDERCHEST_TAG = Pattern.compile("\\[(?:enderchest|ender|echest)\\]", Pattern.CASE_INSENSITIVE);
    
    // Custom placeholders (InteractiveChat-style)
    private static final Pattern POS_TAG = Pattern.compile("\\[pos\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEALTH_TAG = Pattern.compile("\\[health\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_TAG = Pattern.compile("\\[time\\]", Pattern.CASE_INSENSITIVE);
    
    // Permission nodes for different features
    private static final String PERM_USE_ITEM = "interactivechat.use.item";
    private static final String PERM_USE_INV = "interactivechat.use.inventory";
    private static final String PERM_USE_ENDER = "interactivechat.use.enderchest";
    private static final String PERM_VIEW_OTHERS = "interactivechat.view.others";
    private static final String PERM_ADMIN = "interactivechat.admin";
    
    /**
     * Listen for chat events and process interactive tags (InteractiveChat-style)
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        try {
            ServerPlayer player = event.getPlayer();
            String originalMessage = event.getMessage().getString();
            
            // Check if any interactive tags are present
            if (!hasInteractiveTags(originalMessage)) {
                return; // No interactive content, skip processing
            }
            
            // Cancel the original event to replace with our enhanced version
            event.setCanceled(true);
            
            // Process and send the enhanced interactive message
            processInteractiveMessage(player, originalMessage);
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat", "process chat message", e);
        }
    }
    
    /**
     * Check if message contains any interactive tags
     */
    private static boolean hasInteractiveTags(String message) {
        return ITEM_TAG.matcher(message).find() ||
               INVENTORY_TAG.matcher(message).find() ||
               ENDERCHEST_TAG.matcher(message).find() ||
               POS_TAG.matcher(message).find() ||
               HEALTH_TAG.matcher(message).find() ||
               TIME_TAG.matcher(message).find();
    }
    
    /**
     * Process interactive message and create enhanced chat components
     */
    private static void processInteractiveMessage(ServerPlayer player, String originalMessage) {
        try {
            // Create the base chat message component
            MutableComponent chatMessage = Component.literal("")
                .append(Component.literal("<").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("> ").withStyle(ChatFormatting.GRAY));
            
            // Process the message and replace tags with interactive components
            String processedMessage = originalMessage;
            
            // Handle [item] tags
            if (ITEM_TAG.matcher(processedMessage).find()) {
                ItemStack heldItem = player.getMainHandItem();
                MutableComponent itemComponent = createItemComponent(player, heldItem);
                chatMessage.append(itemComponent);
                processedMessage = ITEM_TAG.matcher(processedMessage).replaceFirst("");
                
                // Send to Discord as well
                sendItemToDiscordFromChat(player, heldItem);
            }
            
            // Handle [inventory] or [inv] tags
            if (INVENTORY_TAG.matcher(processedMessage).find()) {
                MutableComponent invComponent = createInventoryComponent(player);
                chatMessage.append(Component.literal(" ")).append(invComponent);
                processedMessage = INVENTORY_TAG.matcher(processedMessage).replaceFirst("");
                
                // Send to Discord
                sendInventoryToDiscordFromChat(player);
            }
            
            // Handle [enderchest] or [ender] or [echest] tags
            if (ENDERCHEST_TAG.matcher(processedMessage).find()) {
                MutableComponent enderComponent = createEnderChestComponent(player);
                chatMessage.append(Component.literal(" ")).append(enderComponent);
                processedMessage = ENDERCHEST_TAG.matcher(processedMessage).replaceFirst("");
                
                // Send to Discord
                sendEnderChestToDiscordFromChat(player);
            }
            
            // Handle [pos] tags
            if (POS_TAG.matcher(processedMessage).find()) {
                MutableComponent posComponent = createPositionComponent(player);
                chatMessage.append(Component.literal(" ")).append(posComponent);
                processedMessage = POS_TAG.matcher(processedMessage).replaceFirst("");
            }
            
            // Handle [health] tags
            if (HEALTH_TAG.matcher(processedMessage).find()) {
                MutableComponent healthComponent = createHealthComponent(player);
                chatMessage.append(Component.literal(" ")).append(healthComponent);
                processedMessage = HEALTH_TAG.matcher(processedMessage).replaceFirst("");
            }
            
            // Handle [time] tags
            if (TIME_TAG.matcher(processedMessage).find()) {
                MutableComponent timeComponent = createTimeComponent(player);
                chatMessage.append(Component.literal(" ")).append(timeComponent);
                processedMessage = TIME_TAG.matcher(processedMessage).replaceFirst("");
            }
            
            // Add any remaining text
            if (!processedMessage.trim().isEmpty()) {
                chatMessage.append(Component.literal(" " + processedMessage.trim()).withStyle(ChatFormatting.WHITE));
            }
            
            // Send the enhanced message to all players
            var server = player.getServer();
            if (server != null) {
                server.getPlayerList().getPlayers().forEach(serverPlayer -> {
                    serverPlayer.sendSystemMessage(chatMessage);
                });
            }
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat", "process interactive message", e);
        }
    }
    
    /**
     * Create clickable item component (InteractiveChat-style)
     */
    private static MutableComponent createItemComponent(ServerPlayer player, ItemStack heldItem) {
        String itemName = getItemDisplayName(heldItem);
        String displayText = heldItem.isEmpty() ? "[Empty Hand]" : "[" + itemName + "]";
        
        // Create hover text with detailed item information
        MutableComponent hoverText = Component.literal("")
            .append(Component.literal("Item: ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(itemName).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\nCount: ").withStyle(ChatFormatting.GOLD))
            .append(Component.literal(String.valueOf(heldItem.getCount())).withStyle(ChatFormatting.WHITE));
        
        String durability = getDurabilityInfo(heldItem);
        if (!durability.equals("N/A")) {
            hoverText.append(Component.literal("\nDurability: ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(durability).withStyle(ChatFormatting.GREEN));
        }
        
        String enchantments = getEnchantmentsInfo(heldItem);
        if (!enchantments.isEmpty()) {
            hoverText.append(Component.literal("\nEnchantments:").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("\n" + enchantments).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        
        hoverText.append(Component.literal("\n\nClick to view details!").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        
        return Component.literal(displayText)
            .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
            .withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    "/neoessentials_internal_showitem " + player.getUUID()))
            );
    }
    
    /**
     * Create clickable inventory component
     */
    private static MutableComponent createInventoryComponent(ServerPlayer player) {
        int usedSlots = getUsedSlots(player);
        int totalSlots = player.getInventory().getContainerSize();
        String displayText = String.format("[Inventory %d/%d]", usedSlots, totalSlots);
        
        // Create hover text with inventory preview
        MutableComponent hoverText = Component.literal("")
            .append(Component.literal(player.getName().getString() + "'s Inventory").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal("\nSlots Used: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(usedSlots + "/" + totalSlots).withStyle(ChatFormatting.WHITE));
        
        // Add item preview
        Map<String, Integer> items = getInventoryItemCounts(player);
        if (!items.isEmpty()) {
            hoverText.append(Component.literal("\n\nTop Items:").withStyle(ChatFormatting.YELLOW));
            items.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    hoverText.append(Component.literal("\n• ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(entry.getKey()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" x" + entry.getValue()).withStyle(ChatFormatting.AQUA));
                });
        }
        
        hoverText.append(Component.literal("\n\nClick to open inventory!").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        
        return Component.literal(displayText)
            .withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE)
            .withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    "/neoessentials_internal_showinv " + player.getUUID()))
            );
    }
    
    /**
     * Create clickable ender chest component
     */
    private static MutableComponent createEnderChestComponent(ServerPlayer player) {
        int usedSlots = getUsedEnderChestSlots(player);
        String displayText = String.format("[Ender Chest %d/27]", usedSlots);
        
        MutableComponent hoverText = Component.literal("")
            .append(Component.literal(player.getName().getString() + "'s Ender Chest").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
            .append(Component.literal("\nSlots Used: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(usedSlots + "/27").withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n\nClick to open ender chest!").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        
        return Component.literal(displayText)
            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE)
            .withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    "/neoessentials_internal_showechest " + player.getUUID()))
            );
    }
    
    /**
     * Create position component
     */
    private static MutableComponent createPositionComponent(ServerPlayer player) {
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();
        String coords = String.format("[%d, %d, %d]", x, y, z);
        
        MutableComponent hoverText = Component.literal("")
            .append(Component.literal("Coordinates").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal("\nX: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(x)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\nY: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(y)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\nZ: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(z)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\nDimension: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(player.level().dimension().location().toString()).withStyle(ChatFormatting.AQUA));
        
        return Component.literal(coords)
            .withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE)
            .withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                    "/tp " + x + " " + y + " " + z))
            );
    }
    
    /**
     * Create health component
     */
    private static MutableComponent createHealthComponent(ServerPlayer player) {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        String healthText = String.format("[❤ %.1f/%.1f]", health, maxHealth);
        
        // Color based on health percentage
        ChatFormatting healthColor = health > maxHealth * 0.75 ? ChatFormatting.GREEN :
                                   health > maxHealth * 0.5 ? ChatFormatting.YELLOW :
                                   health > maxHealth * 0.25 ? ChatFormatting.GOLD : ChatFormatting.RED;
        
        MutableComponent hoverText = Component.literal("")
            .append(Component.literal("Health Status").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal("\nHealth: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.format("%.1f/%.1f", health, maxHealth)).withStyle(healthColor))
            .append(Component.literal("\nFood Level: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(player.getFoodData().getFoodLevel())).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\nSaturation: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.format("%.1f", player.getFoodData().getSaturationLevel())).withStyle(ChatFormatting.WHITE));
        
        return Component.literal(healthText)
            .withStyle(healthColor, ChatFormatting.UNDERLINE)
            .withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
            );
    }
    
    /**
     * Create time component
     */
    private static MutableComponent createTimeComponent(ServerPlayer player) {
        long worldTime = player.level().getDayTime();
        long dayTime = worldTime % 24000;
        int hours = (int) ((dayTime / 1000 + 6) % 24);
        int minutes = (int) ((dayTime % 1000) * 60 / 1000);
        String timeString = String.format("[%02d:%02d]", hours, minutes);
        
        MutableComponent hoverText = Component.literal("")
            .append(Component.literal("Server Time").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal("\nGame Time: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(timeString).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\nDay: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(worldTime / 24000 + 1)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\nTicks: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(String.valueOf(worldTime)).withStyle(ChatFormatting.GRAY));
        
        return Component.literal(timeString)
            .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE)
            .withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
            );
    }
    
    /**
     * Send item to Discord from chat interaction
     */
    private static void sendItemToDiscordFromChat(ServerPlayer player, ItemStack heldItem) {
        try {
            if (!DiscordManager.getInstance().isEnabled()) {
                return;
            }
            
            DiscordEnhancedIntegration.EmbedBuilder embed = new DiscordEnhancedIntegration.EmbedBuilder()
                .setTitle("🎯 Item Display")
                .setDescription(String.format("**%s** is showing their item", player.getName().getString()))
                .setColor(new Color(255, 165, 0)) // Orange
                .addField("Item", getItemDisplayName(heldItem), true)
                .addField("Count", String.valueOf(heldItem.getCount()), true)
                .addField("Durability", getDurabilityInfo(heldItem), true)
                .setFooter("InteractiveChat • [item]", null)
                .setTimestamp(Instant.now());
            
            // Add enchantments if present
            String enchantments = getEnchantmentsInfo(heldItem);
            if (!enchantments.isEmpty()) {
                embed.addField("✨ Enchantments", enchantments, false);
            }
            
            // Send to Discord
            DiscordEnhancedIntegration.sendEmbed(embed, player.getName().getString(), 
                "https://crafatar.com/avatars/" + player.getUUID() + "?size=64");
                
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat", "send item to Discord", e);
        }
    }
    
    /**
     * Send inventory to Discord from chat interaction
     */
    private static void sendInventoryToDiscordFromChat(ServerPlayer player) {
        try {
            if (!DiscordManager.getInstance().isEnabled()) {
                return;
            }
            
            Map<String, Integer> itemCounts = getInventoryItemCounts(player);
            
            DiscordEnhancedIntegration.EmbedBuilder embed = new DiscordEnhancedIntegration.EmbedBuilder()
                .setTitle("🎒 Inventory Preview")
                .setDescription(String.format("**%s** is sharing their inventory", player.getName().getString()))
                .setColor(new Color(76, 175, 80)) // Green
                .setFooter("InteractiveChat • [inventory]", null)
                .setTimestamp(Instant.now());
            
            // Create inventory summary
            StringBuilder inventorySummary = new StringBuilder();
            int itemCount = 0;
            
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                if (itemCount < 10) { // Limit to top 10 items
                    inventorySummary.append("▫ **").append(entry.getValue())
                        .append("x** ").append(entry.getKey()).append("\n");
                    itemCount++;
                } else {
                    inventorySummary.append("... and ").append(itemCounts.size() - 10).append(" more items");
                    break;
                }
            }
            
            embed.addField("📦 Items", inventorySummary.toString(), false);
            embed.addField("Total Slots", String.format("%d/36 used", getUsedSlots(player)), true);
            
            // Send to Discord
            DiscordEnhancedIntegration.sendEmbed(embed, player.getName().getString(), 
                "https://crafatar.com/avatars/" + player.getUUID() + "?size=64");
                
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat", "send inventory to Discord", e);
        }
    }
    
    /**
     * Send ender chest to Discord from chat interaction
     */
    private static void sendEnderChestToDiscordFromChat(ServerPlayer player) {
        try {
            if (!DiscordManager.getInstance().isEnabled()) {
                return;
            }
            
            // Get ender chest contents
            var enderChest = player.getEnderChestInventory();
            Map<String, Integer> itemCounts = new HashMap<>();
            
            for (int i = 0; i < enderChest.getContainerSize(); i++) {
                ItemStack stack = enderChest.getItem(i);
                if (!stack.isEmpty()) {
                    String itemName = getItemDisplayName(stack);
                    itemCounts.merge(itemName, stack.getCount(), Integer::sum);
                }
            }
            
            DiscordEnhancedIntegration.EmbedBuilder embed = new DiscordEnhancedIntegration.EmbedBuilder()
                .setTitle("🌌 Ender Chest Contents")
                .setDescription(String.format("**%s** is sharing their ender chest", player.getName().getString()))
                .setColor(new Color(128, 0, 128)) // Purple
                .setFooter("InteractiveChat • [enderchest]", null)
                .setTimestamp(Instant.now());
            
            if (itemCounts.isEmpty()) {
                embed.addField("📦 Contents", "*Empty ender chest*", false);
            } else {
                StringBuilder contentsSummary = new StringBuilder();
                int itemCount = 0;
                
                for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                    if (itemCount < 8) { // Limit to top 8 items for ender chest
                        contentsSummary.append("▫ **").append(entry.getValue())
                            .append("x** ").append(entry.getKey()).append("\n");
                        itemCount++;
                    } else {
                        contentsSummary.append("... and ").append(itemCounts.size() - 8).append(" more items");
                        break;
                    }
                }
                
                embed.addField("📦 Contents", contentsSummary.toString(), false);
            }
            
            embed.addField("Total Slots", String.format("%d/27 used", getUsedEnderChestSlots(player)), true);
            
            // Send to Discord
            DiscordEnhancedIntegration.sendEmbed(embed, player.getName().getString(), 
                "https://crafatar.com/avatars/" + player.getUUID() + "?size=64");
                
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat", "send ender chest to Discord", e);
        }
    }
    
    /**
     * Handle clicking on inventory viewer (InteractiveChat-style)
     */
    public static void showPlayerInventory(ServerPlayer viewer, ServerPlayer target) {
        try {
            // Check permissions (like InteractiveChat)
            if (!hasViewPermission(viewer, target)) {
                viewer.sendSystemMessage(Component.literal("You don't have permission to view other players' inventories!")
                    .withStyle(ChatFormatting.RED));
                return;
            }
            
            // Create a simple menu to show the target's inventory (read-only)
            viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> {
                    // Create a chest menu that shows the target's inventory
                    return new ChestMenu(MenuType.GENERIC_9x4, containerId, playerInventory, target.getInventory(), 4);
                },
                Component.literal(target.getName().getString() + "'s Inventory")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
            ));
            
            // Log the interaction (like InteractiveChat does)
            viewer.sendSystemMessage(Component.literal("Opening " + target.getName().getString() + "'s inventory...")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat", "show player inventory", e);
        }
    }
    
    /**
     * Handle clicking on ender chest viewer (InteractiveChat-style)
     */
    public static void showPlayerEnderChest(ServerPlayer viewer, ServerPlayer target) {
        try {
            // Check permissions
            if (!hasViewPermission(viewer, target)) {
                viewer.sendSystemMessage(Component.literal("You don't have permission to view other players' ender chests!")
                    .withStyle(ChatFormatting.RED));
                return;
            }
            
            // Open the target's ender chest (read-only)
            viewer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> {
                    return new ChestMenu(MenuType.GENERIC_9x3, containerId, playerInventory, target.getEnderChestInventory(), 3);
                },
                Component.literal(target.getName().getString() + "'s Ender Chest")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
            ));
            
            // Log the interaction
            viewer.sendSystemMessage(Component.literal("Opening " + target.getName().getString() + "'s ender chest...")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat", "show player ender chest", e);
        }
    }
    
    /**
     * Handle clicking on item viewer (InteractiveChat-style)
     */
    public static void showPlayerItem(ServerPlayer viewer, ServerPlayer target) {
        try {
            ItemStack heldItem = target.getMainHandItem();
            
            if (heldItem.isEmpty()) {
                viewer.sendSystemMessage(Component.literal(target.getName().getString() + " is not holding any item!")
                    .withStyle(ChatFormatting.YELLOW));
                return;
            }
            
            // Create detailed item information message (InteractiveChat-style formatting)
            MutableComponent itemInfo = Component.literal("")
                .append(Component.literal("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY))
                .append(Component.literal("\n             ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("ITEM DETAILS").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\n▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n").withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY))
                .append(Component.literal("Player: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal("\nItem: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(getItemDisplayName(heldItem)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            
            if (heldItem.getCount() > 1) {
                itemInfo.append(Component.literal("\nCount: ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(String.valueOf(heldItem.getCount())).withStyle(ChatFormatting.WHITE));
            }
            
            String durability = getDurabilityInfo(heldItem);
            if (!durability.equals("N/A")) {
                // Color durability based on percentage
                ChatFormatting durabilityColor = getDurabilityColor(heldItem);
                itemInfo.append(Component.literal("\nDurability: ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(durability).withStyle(durabilityColor));
            }
            
            String enchantments = getEnchantmentsInfo(heldItem);
            if (!enchantments.isEmpty()) {
                itemInfo.append(Component.literal("\nEnchantments:").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("\n" + enchantments).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            
            // Add NBT info if item has special data
            if (heldItem.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) || 
                heldItem.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ||
                !EnchantmentHelper.getEnchantmentsForCrafting(heldItem).isEmpty()) {
                itemInfo.append(Component.literal("\nSpecial Properties: ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("Present").withStyle(ChatFormatting.GREEN));
            }
            
            itemInfo.append(Component.literal("\n▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY));
            
            viewer.sendSystemMessage(itemInfo);
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat", "show player item", e);
        }
    }
    
    /**
     * Check if viewer has permission to view target's inventory/items
     * Mimics InteractiveChat permission system
     */
    private static boolean hasViewPermission(ServerPlayer viewer, ServerPlayer target) {
        // If viewing own items, always allow
        if (viewer.getUUID().equals(target.getUUID())) {
            return true;
        }
        
        // Check if viewer has admin permissions (like InteractiveChat's interactivechat.view.others)
        if (viewer.hasPermissions(2)) { // Op level 2 or higher
            return true;
        }
        
        // In InteractiveChat, players can view items but not inventories without permission
        // For now, require op for viewing others' items/inventories
        return false;
    }
    
    /**
     * Get durability color based on percentage (InteractiveChat-style)
     */
    private static ChatFormatting getDurabilityColor(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return ChatFormatting.WHITE;
        }
        
        double percentage = (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
        
        if (percentage > 0.75) {
            return ChatFormatting.GREEN;
        } else if (percentage > 0.5) {
            return ChatFormatting.YELLOW;
        } else if (percentage > 0.25) {
            return ChatFormatting.GOLD;
        } else {
            return ChatFormatting.RED;
        }
    }
    
    /**
     * Helper methods for item and inventory processing
     */
    private static String getItemDisplayName(ItemStack stack) {
        if (stack.isEmpty()) {
            return "Empty Hand";
        }
        
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            return stack.getDisplayName().getString();
        }
        
        return stack.getItem().getDescription().getString();
    }
    
    private static String getDurabilityInfo(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return "N/A";
        }
        
        int durability = stack.getMaxDamage() - stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();
        double percentage = (double) durability / maxDurability * 100;
        
        return String.format("%d/%d (%.1f%%)", durability, maxDurability, percentage);
    }
    
    private static String getEnchantmentsInfo(ItemStack stack) {
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) {
            return "";
        }
        
        StringBuilder enchantmentText = new StringBuilder();
        enchantments.entrySet().forEach(entry -> {
            String enchantName = entry.getKey().getKey().location().getPath();
            int level = entry.getIntValue(); // Use getIntValue() instead of deprecated getValue()
            enchantmentText.append("• ").append(enchantName).append(" ").append(level).append("\n");
        });
        
        return enchantmentText.toString().trim();
    }
    
    private static Map<String, Integer> getInventoryItemCounts(ServerPlayer player) {
        Map<String, Integer> itemCounts = new HashMap<>();
        
        // Main inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String itemName = getItemDisplayName(stack);
                itemCounts.merge(itemName, stack.getCount(), Integer::sum);
            }
        }
        
        return itemCounts;
    }
    
    private static int getUsedSlots(ServerPlayer player) {
        int used = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                used++;
            }
        }
        return used;
    }
    
    private static int getUsedEnderChestSlots(ServerPlayer player) {
        int used = 0;
        var enderChest = player.getEnderChestInventory();
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            if (!enderChest.getItem(i).isEmpty()) {
                used++;
            }
        }
        return used;
    }
    
    /**
     * Public method to manually send an item to Discord
     * Used by commands like /ditem
     */
    public static void sendItemToDiscord(ServerPlayer player, ItemStack item, String customMessage) {
        try {
            if (!DiscordManager.getInstance().isEnabled()) {
                return;
            }
            
            // Create embed for the item
            DiscordEnhancedIntegration.EmbedBuilder embed = new DiscordEnhancedIntegration.EmbedBuilder()
                    .setTitle("📦 Item Showcase")
                    .setDescription(customMessage)
                    .addField("Item", formatItemForDiscord(item), false)
                    .setColor(0x3498db)
                    .setTimestamp(Instant.now());
            
            DiscordEnhancedIntegration.sendEmbed(embed, player.getName().getString(), 
                    "https://crafatar.com/avatars/" + player.getUUID() + "?size=64");
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("Discord Interactive Chat", "manual item share", e);
        }
    }
    
    /**
     * Public method to manually send inventory to Discord
     */
    public static void sendInventoryToDiscord(ServerPlayer player, String customMessage) {
        try {
            if (!DiscordManager.getInstance().isEnabled()) {
                return;
            }
            
            DiscordEnhancedIntegration.EmbedBuilder embed = new DiscordEnhancedIntegration.EmbedBuilder()
                    .setTitle("🎒 Inventory Showcase")
                    .setDescription(customMessage)
                    .addField("Inventory Contents", formatInventoryForDiscord(player), false)
                    .setColor(0xe74c3c)
                    .setTimestamp(Instant.now());
            
            DiscordEnhancedIntegration.sendEmbed(embed, player.getName().getString(), 
                    "https://crafatar.com/avatars/" + player.getUUID() + "?size=64");
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("Discord Interactive Chat", "manual inventory share", e);
        }
    }
    
    /**
     * Public method to manually send ender chest to Discord
     */
    public static void sendEnderChestToDiscord(ServerPlayer player, String customMessage) {
        try {
            if (!DiscordManager.getInstance().isEnabled()) {
                return;
            }
            
            DiscordEnhancedIntegration.EmbedBuilder embed = new DiscordEnhancedIntegration.EmbedBuilder()
                    .setTitle("🔮 Ender Chest Showcase")
                    .setDescription(customMessage)
                    .addField("Ender Chest Contents", formatEnderChestForDiscord(player), false)
                    .setColor(0x9b59b6)
                    .setTimestamp(Instant.now());
            
            DiscordEnhancedIntegration.sendEmbed(embed, player.getName().getString(), 
                    "https://crafatar.com/avatars/" + player.getUUID() + "?size=64");
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("Discord Interactive Chat", "manual ender chest share", e);
        }
    }
    
    // Helper methods for formatting content for Discord
    private static String formatItemForDiscord(ItemStack item) {
        if (item.isEmpty()) {
            return "Empty slot";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("**").append(getItemDisplayName(item)).append("**");
        
        if (item.getCount() > 1) {
            result.append(" x").append(item.getCount());
        }
        
        String durability = getDurabilityInfo(item);
        if (!durability.isEmpty()) {
            result.append("\n").append(durability);
        }
        
        String enchantments = getEnchantmentsInfo(item);
        if (!enchantments.isEmpty()) {
            result.append("\n**Enchantments:**\n").append(enchantments);
        }
        
        return result.toString();
    }
    
    private static String formatInventoryForDiscord(ServerPlayer player) {
        Map<String, Integer> items = getInventoryItemCounts(player);
        int usedSlots = getUsedSlots(player);
        int totalSlots = player.getInventory().getContainerSize();
        
        StringBuilder result = new StringBuilder();
        result.append("**Slots Used:** ").append(usedSlots).append("/").append(totalSlots).append("\n\n");
        
        if (items.isEmpty()) {
            result.append("*Empty inventory*");
        } else {
            items.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> result.append("• **")
                            .append(entry.getKey())
                            .append("** x")
                            .append(entry.getValue())
                            .append("\n"));
            
            if (items.size() > 10) {
                result.append("... and ").append(items.size() - 10).append(" more item types");
            }
        }
        
        return result.toString();
    }
    
    private static String formatEnderChestForDiscord(ServerPlayer player) {
        var enderChest = player.getEnderChestInventory();
        Map<String, Integer> items = new HashMap<>();
        
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            ItemStack stack = enderChest.getItem(i);
            if (!stack.isEmpty()) {
                String itemName = getItemDisplayName(stack);
                items.merge(itemName, stack.getCount(), Integer::sum);
            }
        }
        
        int usedSlots = getUsedEnderChestSlots(player);
        int totalSlots = enderChest.getContainerSize();
        
        StringBuilder result = new StringBuilder();
        result.append("**Slots Used:** ").append(usedSlots).append("/").append(totalSlots).append("\n\n");
        
        if (items.isEmpty()) {
            result.append("*Empty ender chest*");
        } else {
            items.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> result.append("• **")
                            .append(entry.getKey())
                            .append("** x")
                            .append(entry.getValue())
                            .append("\n"));
            
            if (items.size() > 10) {
                result.append("... and ").append(items.size() - 10).append(" more item types");
            }
        }
        
        return result.toString();
    }
}
