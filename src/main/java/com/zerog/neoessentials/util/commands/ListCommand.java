package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
// AFK system integration ready when implemented
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements the /list command - Shows online players with advanced formatting
 * Includes AFK status, vanish status, groups, and interactive elements
 */
public class ListCommand {
    
    /**
     * Register the /list command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        boolean enabled = ConfigManager.getInstance().isCommandEnabled("list");
        boolean debug = MessageUtil.isDebugMode();
        if (!enabled) {
            if (debug) {
                org.slf4j.LoggerFactory.getLogger(ListCommand.class).debug("[DEBUG] Skipped registering 'list' and 'who' commands (disabled in config)");
            }
            return;
        }
        dispatcher.register(
            Commands.literal("list")
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.list");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    return showOnlinePlayersList(ctx.getSource(), permResult.hasPermission() ? permResult.getPlayer() : null);
                })
        );
        dispatcher.register(
            Commands.literal("who")
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.list");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    return showOnlinePlayersList(ctx.getSource(), permResult.hasPermission() ? permResult.getPlayer() : null);
                })
        );
        if (debug) {
            org.slf4j.LoggerFactory.getLogger(ListCommand.class).debug("[DEBUG] Registered 'list' and 'who' commands (enabled in config)");
        }
    }
    
    /**
     * Show the online players list with advanced formatting
     */
    private static int showOnlinePlayersList(CommandSourceStack source, ServerPlayer viewer) {
        PlayerList playerList = source.getServer().getPlayerList();
        List<ServerPlayer> onlinePlayers = new ArrayList<>(playerList.getPlayers());
        
        // Check if viewer can see vanished players
        boolean canSeeVanished = viewer != null && 
            PermissionValidator.validatePermission(viewer.createCommandSourceStack(), "neoessentials.vanish.see").hasPermission();
        
        // Filter out vanished players if viewer can't see them
        if (!canSeeVanished) {
            onlinePlayers = onlinePlayers.stream()
                .filter(player -> !isVanished(player))
                .collect(Collectors.toList());
        }
        
        // Sort players by name
        onlinePlayers.sort(Comparator.comparing(player -> player.getName().getString()));
        
        // Send header
        int visibleCount = onlinePlayers.size();
        int totalCount = playerList.getPlayerCount();
        
        if (canSeeVanished && visibleCount != totalCount) {
            source.sendSuccess(
                () -> MessageUtil.success("commands.neoessentials.list.header_with_vanished", 
                    visibleCount, totalCount, playerList.getMaxPlayers()),
                false
            );
        } else {
            source.sendSuccess(
                () -> MessageUtil.success("commands.neoessentials.list.header", 
                    visibleCount, playerList.getMaxPlayers()),
                false
            );
        }
        
        if (onlinePlayers.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.list.no_players"), false);
            return 1;
        }
        
        // Group players by their permission group or status
        Map<String, List<ServerPlayer>> groupedPlayers = new LinkedHashMap<>();
        
        for (ServerPlayer player : onlinePlayers) {
            String group = getPlayerGroup(player);
            groupedPlayers.computeIfAbsent(group, k -> new ArrayList<>()).add(player);
        }
        
        // Display players by groups
        for (Map.Entry<String, List<ServerPlayer>> entry : groupedPlayers.entrySet()) {
            String groupName = entry.getKey();
            List<ServerPlayer> players = entry.getValue();
            
            // Group header
            MutableComponent groupHeader = Component.literal("§6" + groupName + " §7(" + players.size() + "): §f");
            source.sendSuccess(() -> groupHeader, false);
            
            // Players in this group
            List<Component> playerComponents = new ArrayList<>();
            
            for (int i = 0; i < players.size(); i++) {
                ServerPlayer player = players.get(i);
                MutableComponent playerComponent = createPlayerComponent(player, viewer, canSeeVanished);
                
                playerComponents.add(playerComponent);
                
                // Add comma separator except for last player
                if (i < players.size() - 1) {
                    playerComponents.add(Component.literal("§7, "));
                }
            }
            
            // Combine all player components for this group
            MutableComponent groupLine = Component.literal("  ");
            for (Component comp : playerComponents) {
                groupLine.append(comp);
            }
            
            source.sendSuccess(() -> groupLine, false);
        }
        
        // Send footer with additional info
        sendListFooter(source, onlinePlayers, canSeeVanished);
        
        return 1;
    }
    
    /**
     * Create a formatted component for a player entry
     */
    private static MutableComponent createPlayerComponent(ServerPlayer player, ServerPlayer viewer, boolean canSeeVanished) {
        String playerName = player.getName().getString();
        MutableComponent component = Component.literal(playerName);
        
        // Color coding based on status
        if (isVanished(player) && canSeeVanished) {
            component = component.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        } else if (isAfk(player)) {
            component = component.withStyle(ChatFormatting.YELLOW);
        } else {
            component = component.withStyle(ChatFormatting.WHITE);
        }
        
        // Add status indicators
        List<String> statusIndicators = new ArrayList<>();
        
        if (isAfk(player)) {
            statusIndicators.add("§eAFK");
        }
        
        if (isVanished(player) && canSeeVanished) {
            statusIndicators.add("§7V");
        }
        
        // Add OP indicator
        if (player.hasPermissions(4)) {
            statusIndicators.add("§cOP");
        }
        
        // Add status indicators to name
        if (!statusIndicators.isEmpty()) {
            component.append(Component.literal(" §7[" + String.join("§7,", statusIndicators) + "§7]"));
        }
        
        // Add hover text
        List<Component> hoverLines = new ArrayList<>();
        hoverLines.add(Component.literal("§6Player: §f" + playerName));
        hoverLines.add(Component.literal("§6World: §f" + player.level().toString()));
        hoverLines.add(Component.literal("§6Location: §f" + 
            (int)player.getX() + ", " + (int)player.getY() + ", " + (int)player.getZ()));
        
        if (isAfk(player)) {
            String reason = getAfkReason(player);
            if (reason != null && !reason.isEmpty()) {
                hoverLines.add(Component.literal("§6AFK Reason: §f" + reason));
            } else {
                hoverLines.add(Component.literal("§eCurrently AFK"));
            }
        }
        
        // Get player's ping
        hoverLines.add(Component.literal("§6Ping: §f" + player.connection.latency() + "ms"));
        
        hoverLines.add(Component.literal(""));
        hoverLines.add(Component.literal("§7Click to message this player"));
        
        MutableComponent hoverText = Component.empty();
        for (int i = 0; i < hoverLines.size(); i++) {
            if (i > 0) hoverText.append("\n");
            hoverText.append(hoverLines.get(i));
        }
        
        component = component.withStyle(style -> style
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + playerName + " "))
        );
        
        return component;
    }
    
    /**
     * Send footer information
     */
    private static void sendListFooter(CommandSourceStack source, List<ServerPlayer> players, boolean canSeeVanished) {
        // Count different statuses
        int afkCount = 0;
        int vanishedCount = 0;
        int opCount = 0;
        
        for (ServerPlayer player : players) {
            if (isAfk(player)) {
                afkCount++;
            }
            if (isVanished(player)) {
                vanishedCount++;
            }
            if (player.hasPermissions(4)) {
                opCount++;
            }
        }
        
        // Build status summary
        List<String> statusSummary = new ArrayList<>();
        
        if (afkCount > 0) {
            statusSummary.add("§e" + afkCount + " AFK");
        }
        
        if (vanishedCount > 0 && canSeeVanished) {
            statusSummary.add("§7" + vanishedCount + " Vanished");
        }
        
        if (opCount > 0) {
            statusSummary.add("§c" + opCount + " OP");
        }
        
        if (!statusSummary.isEmpty()) {
            MutableComponent footer = Component.literal("§7Status: " + String.join("§7, ", statusSummary));
            source.sendSuccess(() -> footer, false);
        }
        
        // Add legend if there are special statuses
        if (afkCount > 0 || (vanishedCount > 0 && canSeeVanished) || opCount > 0) {
            source.sendSuccess(() -> Component.literal("§7Legend: §eAFK §7- Away from keyboard, §7V §7- Vanished, §cOP §7- Operator"), false);
        }
    }
    
    /**
     * Get a player's display group
     */
    private static String getPlayerGroup(ServerPlayer player) {
        // Check for operator status
        if (player.hasPermissions(4)) {
            return "Operators";
        }
        
        // Check for vanished players
        if (isVanished(player)) {
            return "Staff";
        }
        
        // Check for common staff permissions
        if (PermissionValidator.validatePermission(player.createCommandSourceStack(), "neoessentials.staff").hasPermission()) {
            return "Staff";
        }
        
        // Check for AFK players
        if (isAfk(player)) {
            return "AFK Players";
        }
        
        // Default group
        return "Players";
    }
    
    /**
     * Check if a player is vanished
     * This is a placeholder - would need to integrate with actual vanish system
     */
    private static boolean isVanished(ServerPlayer player) {
        // If vanish system is disabled, always return false
        if (!ConfigManager.getInstance().isVanishSystemEnabled()) {
            return false;
        }
        // Integration ready when vanish system is implemented
        // Currently using permission check as indicator
        return PermissionValidator.validatePermission(player.createCommandSourceStack(), "neoessentials.vanish.active").hasPermission();
    }
    
    /**
     * Check if a player is AFK
     * This is a placeholder - would need to integrate with actual AFK system
     */
    private static boolean isAfk(ServerPlayer player) {
        // Integration ready when AFK system is implemented
        // Currently returns false as placeholder
        return false;
    }
    
    /**
     * Get AFK reason for a player
     * This is a placeholder - would need to integrate with actual AFK system
     */
    private static String getAfkReason(ServerPlayer player) {
        // Integration ready when AFK system is implemented
        // Currently returns null as placeholder
        return null;
    }
}