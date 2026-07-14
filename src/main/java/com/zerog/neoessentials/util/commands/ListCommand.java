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
import net.neoforged.fml.ModList;

import java.util.*;

/**
 * Implements the /list command - Shows online players with advanced formatting
 * Includes LuckPerms group integration, AFK status, vanish status, and interactive elements
 */
public class ListCommand {
    
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ListCommand.class);

    /**
     * Register the /list command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager cfg = ConfigManager.getInstance();

        if (cfg.isCommandEnabled("list")) {
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
        } else {
            LOGGER.debug("Skipped registering 'list' command (disabled in config)");
        }

        if (cfg.isCommandEnabled("who")) {
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
        } else {
            LOGGER.debug("Skipped registering 'who' command (disabled in config)");
        }

        // Register /online alias
        if (cfg.isCommandEnabled("online")) {
            dispatcher.register(
                Commands.literal("online")
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
        } else {
            LOGGER.debug("Skipped registering 'online' command (disabled in config)");
        }
    }

    /**
     * Show the online players list with advanced formatting
     * @return 1 (success) - Minecraft command convention requires returning 1 for successful execution
     */
    @SuppressWarnings("SameReturnValue")
    private static int showOnlinePlayersList(CommandSourceStack source, ServerPlayer viewer) {
        PlayerList playerList = source.getServer().getPlayerList();
        List<ServerPlayer> onlinePlayers = new ArrayList<>(playerList.getPlayers());

        // Check if viewer can see vanished players. Must match VanishManager's own notion of
        // visibility (registered as an active see-vanished viewer via /vanish see), not just
        // the permission node — a staff member who has the permission but never toggled
        // see-vanished on would otherwise show vanished players here even though those same
        // players are actually invisible to them in the world.
        boolean canSeeVanished = viewer != null &&
            PermissionValidator.validatePermission(viewer.createCommandSourceStack(), "neoessentials.vanish.see").hasPermission() &&
            com.zerog.neoessentials.moderation.VanishManager.getInstance().canPlayerSeeVanished(viewer.getUUID());

        // Filter out vanished players if viewer can't see them
        if (!canSeeVanished) {
            onlinePlayers = onlinePlayers.stream()
                .filter(player -> !isVanished(player))
                .toList();
        }

        // Send header
        int visibleCount = onlinePlayers.size();
        int totalCount = playerList.getPlayerCount();

        source.sendSuccess(
            () -> MessageUtil.component("commands.neoessentials.list.separator"),
            false
        );

        if (canSeeVanished && visibleCount != totalCount) {
            source.sendSuccess(
                () -> MessageUtil.component("commands.neoessentials.list.header_with_visible", visibleCount, totalCount, playerList.getMaxPlayers()),
                false
            );
        } else {
            source.sendSuccess(
                () -> MessageUtil.component("commands.neoessentials.list.header", visibleCount, playerList.getMaxPlayers()),
                false
            );
        }

        source.sendSuccess(
            () -> MessageUtil.component("commands.neoessentials.list.separator"),
            false
        );

        if (onlinePlayers.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.list.no_players"), false);
            source.sendSuccess(
                () -> MessageUtil.component("commands.neoessentials.list.separator"),
                false
            );
            return 1;
        }

        // Check if LuckPerms is available
        boolean useLuckPerms = ModList.get().isLoaded("luckperms") && isLuckPermsAvailable();

        if (useLuckPerms) {
            // Group players by LuckPerms groups with weight sorting
            displayLuckPermsGroupedList(source, onlinePlayers, canSeeVanished);
        } else {
            // Fallback to simple grouping
            displaySimpleGroupedList(source, onlinePlayers, canSeeVanished);
        }

        // Send footer
        source.sendSuccess(
            () -> MessageUtil.component("commands.neoessentials.list.separator"),
            false
        );

        return 1;
    }

    /**
     * Display players grouped by LuckPerms groups (sorted by weight)
     */
    private static void displayLuckPermsGroupedList(CommandSourceStack source, List<ServerPlayer> players, boolean canSeeVanished) {
        try {
            // Get LuckPerms API
            net.luckperms.api.LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();

            // Create a map of group name to (weight, players list)
            Map<String, GroupInfo> groupedPlayers = new LinkedHashMap<>();

            for (ServerPlayer player : players) {
                try {
                    net.luckperms.api.model.user.User lpUser = luckPerms.getUserManager().getUser(player.getUUID());
                    if (lpUser == null) {
                        // Fallback to "Players" group if user not found
                        groupedPlayers.computeIfAbsent("Players", k -> new GroupInfo(0)).players.add(player);
                        continue;
                    }

                    String primaryGroup = lpUser.getPrimaryGroup();
                    net.luckperms.api.model.group.Group lpGroup = luckPerms.getGroupManager().getGroup(primaryGroup);

                    if (lpGroup == null) {
                        // Fallback if group not found
                        groupedPlayers.computeIfAbsent(primaryGroup, k -> new GroupInfo(0)).players.add(player);
                    } else {
                        // Get the group weight (higher weight = more important)
                        int weight = lpGroup.getWeight().orElse(0);
                        GroupInfo groupInfo = groupedPlayers.computeIfAbsent(primaryGroup, k -> new GroupInfo(weight));
                        groupInfo.players.add(player);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error getting LuckPerms group for player {}: {}", player.getName().getString(), e.getMessage());
                    groupedPlayers.computeIfAbsent("Players", k -> new GroupInfo(0)).players.add(player);
                }
            }

            // Sort groups by weight (highest first)
            List<Map.Entry<String, GroupInfo>> sortedGroups = groupedPlayers.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().weight, a.getValue().weight))
                .toList();

            // Display each group
            for (Map.Entry<String, GroupInfo> entry : sortedGroups) {
                String groupName = entry.getKey();
                GroupInfo groupInfo = entry.getValue();
                List<ServerPlayer> groupPlayers = groupInfo.players;

                // Sort players alphabetically within group
                groupPlayers.sort(Comparator.comparing(p -> p.getName().getString().toLowerCase()));

                // Group header with weight indicator
                String weightIndicator = groupInfo.weight > 0 ? " §8[" + groupInfo.weight + "]" : "";
                MutableComponent groupHeader = (MutableComponent) MessageUtil.component(
                    "commands.neoessentials.list.group_header_weighted", groupName, weightIndicator, groupPlayers.size());
                source.sendSuccess(() -> groupHeader, false);

                // Build player list for this group
                MutableComponent playerLine = Component.literal("  §f");
                for (int i = 0; i < groupPlayers.size(); i++) {
                    ServerPlayer player = groupPlayers.get(i);
                    playerLine.append(createPlayerComponent(player, canSeeVanished));

                    if (i < groupPlayers.size() - 1) {
                        playerLine.append(Component.literal("§7, "));
                    }
                }

                source.sendSuccess(() -> playerLine, false);
            }

            // Send footer with stats
            sendListFooter(source, players, canSeeVanished);

        } catch (Exception e) {
            LOGGER.error("Error displaying LuckPerms grouped list: {}", e.getMessage(), e);
            // Fallback to simple list
            displaySimpleGroupedList(source, players, canSeeVanished);
        }
    }

    /**
     * Display players with simple grouping (fallback when LuckPerms is not available)
     */
    private static void displaySimpleGroupedList(CommandSourceStack source, List<ServerPlayer> players, boolean canSeeVanished) {
        // Group players by status
        Map<String, List<ServerPlayer>> groupedPlayers = new LinkedHashMap<>();

        for (ServerPlayer player : players) {
            String group = getPlayerGroup(player);
            groupedPlayers.computeIfAbsent(group, k -> new ArrayList<>()).add(player);
        }

        // Display each group
        for (Map.Entry<String, List<ServerPlayer>> entry : groupedPlayers.entrySet()) {
            String groupName = entry.getKey();
            List<ServerPlayer> groupPlayers = entry.getValue();

            // Sort players alphabetically
            groupPlayers.sort(Comparator.comparing(p -> p.getName().getString().toLowerCase()));

            // Group header
            MutableComponent groupHeader = (MutableComponent) MessageUtil.component(
                "commands.neoessentials.list.group_header", groupName, groupPlayers.size());
            source.sendSuccess(() -> groupHeader, false);

            // Build player list
            MutableComponent playerLine = Component.literal("  §f");
            for (int i = 0; i < groupPlayers.size(); i++) {
                ServerPlayer player = groupPlayers.get(i);
                playerLine.append(createPlayerComponent(player, canSeeVanished));

                if (i < groupPlayers.size() - 1) {
                    playerLine.append(Component.literal("§7, "));
                }
            }

            source.sendSuccess(() -> playerLine, false);
        }

        // Send footer with stats
        sendListFooter(source, players, canSeeVanished);
    }

    /**
     * Helper class to store group weight and players
     */
    private static class GroupInfo {
        int weight;
        List<ServerPlayer> players = new ArrayList<>();

        GroupInfo(int weight) {
            this.weight = weight;
        }
    }

    /**
     * Create a formatted component for a player entry
     */
    private static MutableComponent createPlayerComponent(ServerPlayer player, boolean canSeeVanished) {
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
            statusIndicators.add(MessageUtil.localize("commands.neoessentials.list.tag_afk"));
        }

        if (isVanished(player) && canSeeVanished) {
            statusIndicators.add(MessageUtil.localize("commands.neoessentials.list.tag_vanished"));
        }

        // Add OP indicator
        if (com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(player, 4)) {
            statusIndicators.add(MessageUtil.localize("commands.neoessentials.list.tag_op"));
        }

        // Add status indicators to name
        if (!statusIndicators.isEmpty()) {
            component.append(Component.literal(" §8[" + String.join("§7,", statusIndicators) + "§8]"));
        }

        // Add hover text with detailed info
        List<Component> hoverLines = new ArrayList<>();
        hoverLines.add(MessageUtil.component("commands.neoessentials.list.hover_player", playerName));
        // Level objects in Minecraft don't need to be closed with try-with-resources
        @SuppressWarnings("resource")
        String worldName = player.level().dimension().identifier().getPath();
        hoverLines.add(MessageUtil.component("commands.neoessentials.list.hover_world", worldName));
        hoverLines.add(MessageUtil.component("commands.neoessentials.list.hover_location",
            (int)player.getX(), (int)player.getY(), (int)player.getZ()));

        // Add LuckPerms group info if available
        String groupInfo = getLuckPermsGroupInfo(player);
        if (groupInfo != null) {
            hoverLines.add(MessageUtil.component("commands.neoessentials.list.hover_group", groupInfo));
        }

        if (isAfk(player)) {
            String reason = getAfkReason(player);
            if (reason != null && !reason.isEmpty()) {
                hoverLines.add(MessageUtil.component("commands.neoessentials.list.hover_afk_reason", reason));
            } else {
                hoverLines.add(MessageUtil.component("commands.neoessentials.list.hover_afk_current"));
            }
        }

        // Get player's ping
        hoverLines.add(MessageUtil.component("commands.neoessentials.list.hover_ping", player.connection.latency()));

        hoverLines.add(Component.literal(""));
        hoverLines.add(MessageUtil.component("commands.neoessentials.list.hover_click_message"));

        MutableComponent hoverText = Component.empty();
        for (int i = 0; i < hoverLines.size(); i++) {
            if (i > 0) hoverText.append("\n");
            hoverText.append(hoverLines.get(i));
        }

        component = component.withStyle(style -> style
            .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, hoverText))
            .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + playerName + " "))
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
            if (isAfk(player)) afkCount++;
            if (isVanished(player)) vanishedCount++;
            if (com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(player, 4)) opCount++;
        }

        // Build status summary
        List<String> statusSummary = new ArrayList<>();

        if (afkCount > 0) {
            statusSummary.add(MessageUtil.localize("commands.neoessentials.list.count_afk", afkCount));
        }

        if (vanishedCount > 0 && canSeeVanished) {
            statusSummary.add(MessageUtil.localize("commands.neoessentials.list.count_vanished", vanishedCount));
        }

        if (opCount > 0) {
            statusSummary.add(MessageUtil.localize("commands.neoessentials.list.count_op", opCount));
        }

        if (!statusSummary.isEmpty()) {
            MutableComponent footer = (MutableComponent) MessageUtil.component(
                "commands.neoessentials.list.footer_status", String.join("§7, ", statusSummary));
            source.sendSuccess(() -> footer, false);
        }
    }

    /**
     * Get LuckPerms group info for hover text
     */
    private static String getLuckPermsGroupInfo(ServerPlayer player) {
        try {
            if (ModList.get().isLoaded("luckperms") && isLuckPermsAvailable()) {
                net.luckperms.api.LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User lpUser = luckPerms.getUserManager().getUser(player.getUUID());

                if (lpUser != null) {
                    String primaryGroup = lpUser.getPrimaryGroup();
                    net.luckperms.api.model.group.Group lpGroup = luckPerms.getGroupManager().getGroup(primaryGroup);

                    if (lpGroup != null) {
                        int weight = lpGroup.getWeight().orElse(0);
                        return primaryGroup + (weight > 0 ? " [" + weight + "]" : "");
                    }

                    return primaryGroup;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting LuckPerms group info for player {}: {}", player.getName().getString(), e.getMessage());
        }

        return null;
    }

    /**
     * Check if LuckPerms API is available
     */
    private static boolean isLuckPermsAvailable() {
        try {
            net.luckperms.api.LuckPermsProvider.get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get a player's display group (fallback when LuckPerms is not available)
     */
    private static String getPlayerGroup(ServerPlayer player) {
        // Check for operator status
        if (com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(player, 4)) {
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
     */
    private static boolean isVanished(ServerPlayer player) {
        if (!ConfigManager.getInstance().isVanishSystemEnabled()) {
            return false;
        }

        try {
            com.zerog.neoessentials.moderation.VanishManager vanishManager =
                com.zerog.neoessentials.moderation.VanishManager.getInstance();
            return vanishManager.isPlayerVanished(player.getUUID());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a player is AFK
     */
    private static boolean isAfk(ServerPlayer player) {
        if (!ConfigManager.isChatEnabled()) {
            return false;
        }

        try {
            com.zerog.neoessentials.chat.AfkManager afkManager =
                com.zerog.neoessentials.chat.AfkManager.getInstance();
            return afkManager.isAfk(player);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get AFK reason for a player
     */
    private static String getAfkReason(ServerPlayer player) {
        if (!ConfigManager.isChatEnabled()) {
            return null;
        }

        try {
            com.zerog.neoessentials.chat.AfkManager afkManager =
                com.zerog.neoessentials.chat.AfkManager.getInstance();
            return afkManager.getAfkReason(player.getUUID());
        } catch (Exception e) {
            return null;
        }
    }
}
