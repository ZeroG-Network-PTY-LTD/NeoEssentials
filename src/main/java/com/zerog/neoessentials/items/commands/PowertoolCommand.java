
package com.zerog.neoessentials.items.commands;
import java.util.HashMap;
import java.util.Map;

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
}
