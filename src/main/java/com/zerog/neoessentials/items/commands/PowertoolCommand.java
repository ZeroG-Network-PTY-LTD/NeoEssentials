
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

public class PowertoolCommand {
    // Server-side powertool assignments: player UUID -> slot -> command
    private static final Map<java.util.UUID, Map<Integer, String>> POWERS = new HashMap<>();
    /**
     * Register the /powertool and /pt commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("powertool")) return;
        dispatcher.register(
            Commands.literal("powertool")
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
     */
    public static void assign(ServerPlayer player, String command) {
        int slot = player.getInventory().selected;
        POWERS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(slot, command);
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
                System.err.println("Failed to execute target command '" + validCommand + "' for player " + 
                    target.getName().getString() + ": " + e.getMessage());
            }
        }

        if (successCount[0] > 0) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.pt.target.success", 
                successCount[0], targets.size()), false);
            return 1;
        } else {
            source.sendFailure(MessageUtil.error("commands.neoessentials.pt.target.failed"));
            return 0;
        }
    }
}
