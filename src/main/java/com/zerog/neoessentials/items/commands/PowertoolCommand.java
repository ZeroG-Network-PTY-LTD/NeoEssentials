
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
 *   <li>Bind any command to an item for right-click execution</li>
 *   <li>Advanced @p targeting executes command on all other online players</li>
 *   <li>Server-side storage prevents client tampering</li>
 *   <li>Supports placeholder substitution ({player} in commands)</li>
 *   <li>Audit logging for assignments and executions</li>
 * </ul>
 */
public class PowertoolCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowertoolCommand.class);
    
    // Server-side powertool assignments: player UUID -> slot -> command
    private static final Map<java.util.UUID, Map<Integer, String>> POWERS = new HashMap<>();
    /**
     * Register the /powertool and /pt commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("powertool")) return;
        registerPowertoolCommand(dispatcher, "powertool");
        registerPowertoolCommand(dispatcher, "ptool");
    }
    
    private static void registerPowertoolCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            Commands.literal(commandName)
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
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
        dispatcher.register(
            Commands.literal("pt")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
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
     * Assigns a command to the item in the player's main hand using vanilla NBT.
     * Logs the assignment for audit trail purposes.
     * 
     * @param player The player assigning the powertool
     * @param command The command to bind to the item
     */
    public static void assign(ServerPlayer player, String command) {
        int slot = player.getInventory().selected;
        POWERS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(slot, command);
        
        // Log powertool assignment for audit trail
        LOGGER.info("Player {} assigned powertool command '{}' to slot {}", 
            player.getName().getString(), command, slot);
    }

    /**
     * Check if a player has any powertool data.
     */
    public static boolean hasPowertoolData(java.util.UUID playerUUID) {
        return POWERS.containsKey(playerUUID) && !POWERS.get(playerUUID).isEmpty();
    }

    /**
     * Get the powertool command for a specific slot.
     */
    public static String getPowertoolCommand(java.util.UUID playerUUID, int slot) {
        Map<Integer, String> playerPowers = POWERS.get(playerUUID);
        return playerPowers != null ? playerPowers.get(slot) : null;
    }

    /**
     * Remove powertool command from a slot.
     */
    public static void removePowertool(java.util.UUID playerUUID, int slot) {
        Map<Integer, String> playerPowers = POWERS.get(playerUUID);
        if (playerPowers != null) {
            playerPowers.remove(slot);
            if (playerPowers.isEmpty()) {
                POWERS.remove(playerUUID);
            }
        }
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
