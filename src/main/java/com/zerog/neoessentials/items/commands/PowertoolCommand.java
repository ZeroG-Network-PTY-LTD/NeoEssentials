package com.zerog.neoessentials.items.commands;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides powertool functionality to bind commands to items for quick execution.
 * 
 * <p>Commands:</p>
 * <ul>
 *   <li>/powertool &lt;command&gt; - Bind command to item in hand</li>
 *   <li>/powertool remove - Remove powertool from item in hand</li>
 *   <li>/powertool list - List all your powertools</li>
 *   <li>/powertool @p &lt;command&gt; - Create targeting command (executes on all other players)</li>
 *   <li>/ptool - Alias for /powertool</li>
 *   <li>/pt - Short alias for /powertool</li>
 * </ul>
 * 
 * <p>Permissions:</p>
 * <ul>
 *   <li>neoessentials.item.powertool - Bind commands to items</li>
 *   <li>neoessentials.command.target - Use @p targeting feature</li>
 * </ul>
 * 
 * <p>Configuration:</p>
 * <ul>
 *   <li>commands.powertool.enabled - Enable/disable command</li>
 * </ul>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Bind any command to an item TYPE (not slot) for right-click execution</li>
 *   <li>Advanced @p targeting executes command on all other online players</li>
 *   <li>Server-side storage prevents client tampering</li>
 *   <li>Supports placeholder substitution ({player} in commands)</li>
 *   <li>Audit logging for assignments and executions</li>
 *   <li>Remove powertools with /powertool remove</li>
 *   <li>List all powertools with /powertool list</li>
 * </ul>
 */
public class PowertoolCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowertoolCommand.class);
    
// Server-side powertool assignments: player UUID -> item ID -> command
    private static final Map<java.util.UUID, Map<String, String>> POWERS = new HashMap<>();

    // Per-player powertool toggle (true = enabled, default true)
    private static final java.util.Set<java.util.UUID> ptDisabled = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /** Returns true if powertools are globally enabled for this player. */
    public static boolean isPowertoolEnabled(java.util.UUID uuid) {
        return !ptDisabled.contains(uuid);
    }

    /** Returns a read-only snapshot of all powertools for a given player UUID. */
    public static Map<String, String> getPlayerPowertools(java.util.UUID playerUUID) {
        Map<String, String> powers = POWERS.get(playerUUID);
        return powers != null ? java.util.Collections.unmodifiableMap(powers) : java.util.Collections.emptyMap();
    }

    /**
     * Register the /powertool and /pt commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager cfg = ConfigManager.getInstance();
        if (cfg.isCommandEnabled("powertool")) {
            registerPowertoolCommand(dispatcher, "powertool");
        }
        if (cfg.isCommandEnabled("ptool")) {
            registerPowertoolCommand(dispatcher, "ptool");
        }
        if (cfg.isCommandEnabled("pt")) {
            registerPtAlias(dispatcher);
        }
        registerPowertoolToggle(dispatcher);
    }

    /** /powertooltoggle and /ptt — globally enable/disable all powertools for this player. Gated independently. */
    private static void registerPowertoolToggle(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (ConfigManager.getInstance().isCommandEnabled("powertooltoggle")) {
        dispatcher.register(Commands.literal("powertooltoggle")
            .requires(cs -> cs.getEntity() instanceof ServerPlayer)
            .executes(ctx -> {
                ServerPlayer player = (ServerPlayer) ctx.getSource().getEntity();
                if (player == null) return 0;
                java.util.UUID uuid = player.getUUID();
                boolean nowEnabled;
                if (ptDisabled.contains(uuid)) {
                    ptDisabled.remove(uuid);
                    nowEnabled = true;
                } else {
                    if (!POWERS.containsKey(uuid) || POWERS.get(uuid).isEmpty()) {
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.powertooltoggle.none"));
                        return 0;
                    }
                    ptDisabled.add(uuid);
                    nowEnabled = false;
                }
                final boolean fe = nowEnabled;
                player.sendSystemMessage(MessageUtil.success(
                    fe ? "commands.neoessentials.powertooltoggle.enabled"
                       : "commands.neoessentials.powertooltoggle.disabled"));
                LOGGER.info("Player {} {} all powertools via /powertooltoggle",
                    player.getName().getString(), nowEnabled ? "enabled" : "disabled");
                return 1;
            })
        );
        }
        // /ptt alias
        if (ConfigManager.getInstance().isCommandEnabled("ptt")) {
        dispatcher.register(Commands.literal("ptt")
            .requires(cs -> cs.getEntity() instanceof ServerPlayer)
            .executes(ctx -> {
                // Delegate to the same logic
                var src = ctx.getSource();
                ServerPlayer player = (ServerPlayer) src.getEntity();
                if (player == null) return 0;
                java.util.UUID uuid = player.getUUID();
                boolean nowEnabled;
                if (ptDisabled.contains(uuid)) { ptDisabled.remove(uuid); nowEnabled = true; }
                else { ptDisabled.add(uuid); nowEnabled = false; }
                final boolean fe = nowEnabled;
                player.sendSystemMessage(MessageUtil.success(
                    fe ? "commands.neoessentials.powertooltoggle.enabled"
                       : "commands.neoessentials.powertooltoggle.disabled"));
                return 1;
            })
        );
        }
    }

    private static void registerPowertoolCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            Commands.literal(commandName)
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /powertool list - List all powertools
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult =
                            com.zerog.neoessentials.util.PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.powertool");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }

                        ServerPlayer player = permResult.getPlayer();
                        return listPowertools(player);
                    })
                )
                // /powertool remove - Remove powertool from item in hand
                .then(Commands.literal("remove")
                    .executes(ctx -> {
                        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult =
                            com.zerog.neoessentials.util.PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.powertool");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }

                        ServerPlayer player = permResult.getPlayer();
                        ItemStack heldItem = player.getMainHandItem();

                        if (heldItem.isEmpty()) {
                            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.powertool.no_item"));
                            return 0;
                        }

                        String itemId = getItemId(heldItem);
                        if (removePowertool(player.getUUID(), itemId)) {
                            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.powertool.remove.success"), false);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.powertool.remove.not_found"));
                            return 0;
                        }
                    })
                )
                // /powertool <command> - Assign command to item
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        // Validate permission
                        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult = 
                            com.zerog.neoessentials.util.PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.powertool");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        String cmd = StringArgumentType.getString(ctx, "command");
                        
                        // Check if this is a @p targeting command
                        if (cmd.startsWith("@p ")) {
                            return executeTargetCommand(ctx.getSource(), player, cmd.substring(3));
                        }
                        
                        // Check if player is holding an item
                        ItemStack heldItem = player.getMainHandItem();
                        if (heldItem.isEmpty()) {
                            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.powertool.no_item"));
                            return 0;
                        }

                        // Original powertool functionality
                        // Validate command
                        com.zerog.neoessentials.util.InputValidator.ValidationResult cmdValidation = 
                            com.zerog.neoessentials.util.InputValidator.validateCommand(cmd);
                        if (!cmdValidation.isValid()) {
                            ctx.getSource().sendFailure(MessageUtil.error(cmdValidation.getErrorMessage()));
                            return 0;
                        }
                        
                        String validCommand = cmdValidation.getValue(String.class);
                        assign(player, validCommand);
                        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.powertool.assign.success"), false);
                        return 1;
                    })
                )
        );
    }

    /** /pt — standalone alias, gated independently via its own "pt" config toggle. */
    private static void registerPtAlias(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pt")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /pt list
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult =
                            com.zerog.neoessentials.util.PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.powertool");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }

                        ServerPlayer player = permResult.getPlayer();
                        return listPowertools(player);
                    })
                )
                // /pt remove
                .then(Commands.literal("remove")
                    .executes(ctx -> {
                        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult =
                            com.zerog.neoessentials.util.PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.powertool");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }

                        ServerPlayer player = permResult.getPlayer();
                        ItemStack heldItem = player.getMainHandItem();

                        if (heldItem.isEmpty()) {
                            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.powertool.no_item"));
                            return 0;
                        }

                        String itemId = getItemId(heldItem);
                        if (removePowertool(player.getUUID(), itemId)) {
                            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.powertool.remove.success"), false);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.powertool.remove.not_found"));
                            return 0;
                        }
                    })
                )
                // /pt <command>
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        // Validate permission
                        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult = 
                            com.zerog.neoessentials.util.PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.powertool");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        String cmd = StringArgumentType.getString(ctx, "command");
                        
                        // Check if this is a @p targeting command
                        if (cmd.startsWith("@p ")) {
                            return executeTargetCommand(ctx.getSource(), player, cmd.substring(3));
                        }
                        
                        // Check if player is holding an item
                        ItemStack heldItem = player.getMainHandItem();
                        if (heldItem.isEmpty()) {
                            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.powertool.no_item"));
                            return 0;
                        }

                        // Original powertool functionality
                        // Validate command
                        com.zerog.neoessentials.util.InputValidator.ValidationResult cmdValidation = 
                            com.zerog.neoessentials.util.InputValidator.validateCommand(cmd);
                        if (!cmdValidation.isValid()) {
                            ctx.getSource().sendFailure(MessageUtil.error(cmdValidation.getErrorMessage()));
                            return 0;
                        }
                        
                        String validCommand = cmdValidation.getValue(String.class);
                        assign(player, validCommand);
                        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.powertool.assign.success"), false);
                        return 1;
                    })
                )
        );
    }

    /**
     * Get the item ID from an ItemStack.
     */
    private static String getItemId(ItemStack itemStack) {
        Identifier itemKey = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        return itemKey.toString();
    }

    /**
     * Assigns a command to the item type in the player's main hand.
     * Logs the assignment for audit trail purposes.
     * 
     * @param player The player assigning the powertool
     * @param command The command to bind to the item
     */
    public static void assign(ServerPlayer player, String command) {
        ItemStack heldItem = player.getMainHandItem();
        String itemId = getItemId(heldItem);

        POWERS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(itemId, command);

        // Log powertool assignment for audit trail
        LOGGER.info("Player {} assigned powertool command '{}' to item {}",
            player.getName().getString(), command, itemId);
    }

    /**
     * List all powertools for a player.
     */
    private static int listPowertools(ServerPlayer player) {
        Map<String, String> playerPowers = POWERS.get(player.getUUID());

        if (playerPowers == null || playerPowers.isEmpty()) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.powertool.list.empty"));
            return 0;
        }

        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.powertool.list.header"));
        for (Map.Entry<String, String> entry : playerPowers.entrySet()) {
            String itemId = entry.getKey();
            String command = entry.getValue();
            // Extract just the item name from the full ID (e.g., "minecraft:diamond_sword" -> "diamond_sword")
            String itemName = itemId.contains(":") ? itemId.substring(itemId.indexOf(":") + 1) : itemId;
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("  §e" + itemName + "§r: §7" + command));
        }

        return 1;
    }

    /**
     * Check if a player has any powertool data.
     */
    public static boolean hasPowertoolData(java.util.UUID playerUUID) {
        return POWERS.containsKey(playerUUID) && !POWERS.get(playerUUID).isEmpty();
    }

    /**
     * Get the powertool command for a specific item.
     */
    public static String getPowertoolCommand(java.util.UUID playerUUID, String itemId) {
        Map<String, String> playerPowers = POWERS.get(playerUUID);
        return playerPowers != null ? playerPowers.get(itemId) : null;
    }

    /**
     * Remove powertool command from an item.
     * @return true if a powertool was removed, false if none existed
     */
    public static boolean removePowertool(java.util.UUID playerUUID, String itemId) {
        Map<String, String> playerPowers = POWERS.get(playerUUID);
        if (playerPowers != null) {
            String removed = playerPowers.remove(itemId);
            if (playerPowers.isEmpty()) {
                POWERS.remove(playerUUID);
            }
            return removed != null;
        }
        return false;
    }

    /**
     * Execute a command targeting other players with @p selector (excluding the executor).
     */
    private static int executeTargetCommand(CommandSourceStack source, ServerPlayer executor, String command) {
        // Validate permission for targeting
        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult = 
            com.zerog.neoessentials.util.PermissionValidator.validatePermission(source, "neoessentials.command.target");
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        // Validate command
        com.zerog.neoessentials.util.InputValidator.ValidationResult cmdValidation = 
            com.zerog.neoessentials.util.InputValidator.validateCommand(command);
        if (!cmdValidation.isValid()) {
            source.sendFailure(MessageUtil.error(cmdValidation.getErrorMessage()));
            return 0;
        }

        String validCommand = cmdValidation.getValue(String.class);

        // Get all online players excluding the executor
        List<ServerPlayer> targets = new ArrayList<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(executor.getUUID())) {
                targets.add(player);
            }
        }

        if (targets.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.pt.target.no_targets"));
            return 0;
        }

        // Execute command for each target
        final int[] successCount = {0};
        for (ServerPlayer target : targets) {
            try {
                // Create command with target's name substituted
                String targetCommand = validCommand.replace("{player}", target.getName().getString());
                
                // Execute the command as the server
                source.getServer().getCommands().performPrefixedCommand(
                    source.getServer().createCommandSourceStack(),
                    targetCommand
                );
                successCount[0]++;
            } catch (Exception e) {
                // Log error but continue with other targets
                LOGGER.warn("Failed to execute target command '{}' for player {}: {}", 
                    validCommand, target.getName().getString(), e.getMessage());
            }
        }

        if (successCount[0] > 0) {
            // Log successful target command execution for audit trail
            LOGGER.info("Player {} executed target command '{}' on {}/{} players successfully",
                executor.getName().getString(), validCommand, successCount[0], targets.size());
            
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.pt.target.success", 
                successCount[0], targets.size()), false);
            return 1;
        } else {
            source.sendFailure(MessageUtil.error("commands.neoessentials.pt.target.failed"));
            return 0;
        }
    }
}
